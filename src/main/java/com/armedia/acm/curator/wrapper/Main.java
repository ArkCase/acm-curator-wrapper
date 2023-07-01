package com.armedia.acm.curator.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Consumer;

/*-
 * #%L
 * acm-config-server
 * %%
 * Copyright (C) 2019 ArkCase LLC
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.conf.Cfg;
import com.armedia.acm.curator.wrapper.conf.ExecCfg;
import com.armedia.acm.curator.wrapper.conf.OperationMode;
import com.armedia.acm.curator.wrapper.conf.RedirectCfg;
import com.armedia.acm.curator.wrapper.conf.WrapperCfg;
import com.armedia.acm.curator.wrapper.module.Leader;
import com.armedia.acm.curator.wrapper.module.Mutex;
import com.armedia.acm.curator.wrapper.module.Session;

public class Main
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Cfg cfg;

    private Main(String... args)
    {
        // TODO: Find our configurations ... either from the command line arguments,
        // environment, or system properties
        this.cfg = null;
    }

    private AutoCloseable createWrapper(WrapperCfg cfg, Session session) throws Exception
    {
        switch (cfg.getMode())
        {
        case leader:
            this.log.info("Creating a leadership selector");
            return new Leader(session, cfg.getName()).awaitLeadership();

        case mutex:
            this.log.info("Creating a mutex lock");
            return new Mutex(session, cfg.getName()).acquire();

        default:
            this.log.info("No-op wrapper created");
            return this::noop;
        }
    }

    private void noop()
    {
        // Specifically do nothing...
    }

    private void redirect(String path, Consumer<Redirect> tgt)
    {
        Redirect redirect = Redirect.INHERIT;
        if (RedirectCfg.NULL.equalsIgnoreCase(path))
        {
            redirect = Redirect.DISCARD;
        }
        else if (path != null)
        {
            redirect = Redirect.from(new File(path));
        }
        tgt.accept(redirect);
    }

    public void applyRedirects(RedirectCfg cfg, ProcessBuilder pb)
    {
        if ((pb == null) || (cfg == null))
        {
            return;
        }

        redirect(cfg.getStdin(), pb::redirectInput);
        redirect(cfg.getStdout(), pb::redirectOutput);
        redirect(cfg.getStderr(), pb::redirectError);
    }

    private int run(ExecCfg cfg) throws Exception
    {
        final Object command = cfg.getCommand();
        Objects.requireNonNull(command);
        File workdir = new File(".");
        if (cfg.getWorkdir() != null)
        {
            workdir = new File(cfg.getWorkdir());
            try
            {
                workdir = workdir.getCanonicalFile();
            }
            catch (IOException e)
            {
                workdir = workdir.getAbsoluteFile();
            }
            finally
            {
                if (!workdir.exists() || !workdir.isDirectory())
                {
                    throw new FileNotFoundException("The working directory [" + workdir + "] doesn't exist or isn't a directory");
                }
            }
        }

        final List<String> cmd;
        Objects.requireNonNull(command, "Must provide a non-null exec value");
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
            StringTokenizer tok = new StringTokenizer(command.toString());
            cmd = new ArrayList<>(tok.countTokens());
            while (tok.hasMoreTokens())
            {
                cmd.add(tok.nextToken());
            }
        }

        if (cmd.isEmpty())
        {
            throw new IllegalStateException("The command array must contain at least one value");
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

        this.log.debug("Launching the process command {}", pb.command());
        this.log.trace("Using the environment:\n{}", pb.environment());

        return pb.start().waitFor();
    }

    public int run() throws Exception
    {
        WrapperCfg wrapperCfg = this.cfg.getWrapper();
        ExecCfg cmd = wrapperCfg.getExec();
        if (wrapperCfg.getMode() == OperationMode.direct)
        {
            // We're not working any of our magic... just execute the wrapped command
            return run(cmd);
        }

        try (Session session = new Session(this.cfg))
        {
            if (!session.isEnabled())
            {
                // Still not working our magic b/c we have no clustering configuration
                this.log.info("Running in standalone mode");
                return run(cmd);
            }

            // This is the new, "clusterable" code path
            this.log.info("Running in clustered mode");
            try (AutoCloseable c = createWrapper(wrapperCfg, session))
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

    public static void main(String... args) throws Exception
    {
        System.exit(new Main(args).run());
    }
}