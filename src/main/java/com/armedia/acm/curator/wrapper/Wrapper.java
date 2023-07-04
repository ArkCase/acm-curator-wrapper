/*-
 * #%L
 * acm-curator-wrapper
 * %%
 * Copyright (C) 2023 ArkCase LLC
 * %%
 * This file is part of the ArkCase software.
 *
 * If the software was purchased under a paid ArkCase license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * ArkCase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ArkCase is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package com.armedia.acm.curator.wrapper;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.recipe.Leader;
import com.armedia.acm.curator.recipe.Mutex;
import com.armedia.acm.curator.tools.CheckedSupplier;
import com.armedia.acm.curator.tools.Tools;
import com.armedia.acm.curator.wrapper.conf.ExecCfg;
import com.armedia.acm.curator.wrapper.conf.OperationMode;
import com.armedia.acm.curator.wrapper.conf.RedirectCfg;
import com.armedia.acm.curator.wrapper.conf.WrapperCfg;

public class Wrapper
{
    private static final String NULL = "null";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final WrapperCfg cfg;
    private final CheckedSupplier<Session> session;

    public Wrapper(CheckedSupplier<Session> session, WrapperCfg cfg)
    {
        this.session = Objects.requireNonNull(session, "Must provide a non-null Session supplier");
        this.cfg = Tools.ifNull(cfg, WrapperCfg::new);
    }

    private AutoCloseable createWrapper(Session session) throws Exception
    {
        switch (this.cfg.getMode())
        {
        case leader:
            this.log.info("Creating a leadership selector");
            Leader leader = new Leader(session, this.cfg.getName());
            if (this.cfg.getTimeout() > 0)
            {
                return leader.awaitLeadership(Duration.ofMillis(this.cfg.getTimeout()));
            }
            return leader.awaitLeadership() //
            ;

        case mutex:
            this.log.info("Creating a mutex lock");
            Mutex mutex = new Mutex(session, this.cfg.getName());
            if (this.cfg.getTimeout() > 0)
            {
                return mutex.acquire(Duration.ofMillis(this.cfg.getTimeout()));
            }
            return mutex.acquire() //
            ;

        default:
            this.log.info("No-op wrapper created");
            return Tools::noop;
        }
    }

    private void redirect(boolean from, String path, Consumer<Redirect> tgt)
    {
        Redirect redirect = Redirect.INHERIT;
        if (Wrapper.NULL.equalsIgnoreCase(path))
        {
            // Enable this when we move to Java 11
            // redirect = Redirect.DISCARD;

            // This only exists for Java 8
            redirect = null;
        }
        else if (path != null)
        {
            final File f = new File(path);
            redirect = (from ? Redirect.from(f) : Redirect.to(f));
        }
        tgt.accept(redirect);
    }

    public void applyRedirects(RedirectCfg cfg, ProcessBuilder pb)
    {
        if ((pb == null) || (cfg == null))
        {
            return;
        }

        redirect(true, cfg.getStdin(), pb::redirectInput);
        redirect(false, cfg.getStdout(), pb::redirectOutput);
        redirect(false, cfg.getStderr(), pb::redirectError);
    }

    private int run(ExecCfg cfg)
    {
        final Object command = cfg.getCommand();
        if (command == null)
        {
            this.log.warn("No command was given to wrap, returning an exit status of 1");
            return 1;
        }

        File workdir = Tools.CWD;
        if (cfg.getWorkdir() != null)
        {
            workdir = Tools.canonicalize(new File(cfg.getWorkdir()));
            if (!workdir.exists() || !workdir.isDirectory())
            {
                this.log.error("The working directory [{}] doesn't exist or isn't a directory", workdir);
                return 1;
            }
        }

        final List<String> cmd;
        if (Collection.class.isInstance(command))
        {
            Collection<?> c = Collection.class.cast(command);
            cmd = new ArrayList<>(c.size());
            c.forEach((v) -> cmd.add(String.valueOf(v)));
        }
        else if (command.getClass().isArray())
        {
            Object[] arr = (Object[]) command;
            cmd = new ArrayList<>(arr.length);
            for (Object o : arr)
            {
                cmd.add(String.valueOf(o));
            }
        }
        else
        {
            // TODO: we need to support a shell-safe tokenizer here that supports quotes for
            // parameters that require spaces
            StringTokenizer tok = new StringTokenizer(command.toString());
            cmd = new ArrayList<>(tok.countTokens());
            while (tok.hasMoreTokens())
            {
                cmd.add(tok.nextToken());
            }
        }

        if (cmd.isEmpty())
        {
            this.log.error("The command is empty, cannot continue");
            return 1;
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir);
        applyRedirects(cfg.getRedirect(), pb);

        Map<String, String> env = pb.environment();
        if (cfg.isCleanEnv())
        {
            env.clear();
        }

        if (cfg.getEnv() != null)
        {
            env.putAll(cfg.getEnv());
        }

        this.log.info("Launching the wrapped command {}", pb.command());
        this.log.trace("Using the environment:\n{}", pb.environment());

        try
        {
            return pb.start().waitFor();
        }
        catch (IOException e)
        {
            this.log.error("IOException caught running the command", e);
            return 1;
        }
        catch (InterruptedException e)
        {
            this.log.error("Interrupted waiting for the command to complete", e);
            return 1;
        }
    }

    public int run() throws Exception
    {
        ExecCfg cmd = this.cfg.getExec();
        if (this.cfg.getMode() == OperationMode.direct)
        {
            // We're not working any of our magic... just execute the wrapped command
            return run(cmd);
        }

        try (Session session = this.session.get())
        {
            if (!session.isEnabled())
            {
                // Still not working our magic b/c we have no clustering configuration
                this.log.info("Running in standalone mode");
                return run(cmd);
            }

            // This is the new, "clusterable" code path
            this.log.info("Running in clustered mode");
            try (AutoCloseable c = createWrapper(session))
            {
                return run(cmd);
            }
            catch (Exception e)
            {
                this.log.error("Exception caught during wrapping or command execution", e);
                return 1;
            }
        }
    }

}
