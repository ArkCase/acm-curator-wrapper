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
import com.armedia.acm.curator.recipe.InitializationGate;
import com.armedia.acm.curator.recipe.InitializationGate.FunctionalInitializer;
import com.armedia.acm.curator.recipe.InitializationGate.Initializer;
import com.armedia.acm.curator.recipe.Leader;
import com.armedia.acm.curator.recipe.Mutex;
import com.armedia.acm.curator.tools.CheckedSupplier;
import com.armedia.acm.curator.tools.Tools;
import com.armedia.acm.curator.tools.Version;
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

    private String getParameter(String name)
    {
        String value = null;

        value = Tools.toString(this.cfg.getParam().get(name));
        if (!Tools.isEmpty(value))
        {
            this.log.trace("Found the parameter [{}] with value [{}]", name, value);
            return value;
        }

        final String sysProp = String.format("wrapper.param.%s", name);

        value = System.getProperty(sysProp);
        if (!Tools.isEmpty(value))
        {
            this.log.trace("Found the system property {} with value [{}]", sysProp, value);
            return value;
        }

        String envVar = sysProp.replace('.', '_').toUpperCase();
        value = System.getenv(envVar);
        if (!Tools.isEmpty(value))
        {
            this.log.trace("Found the environment variable {} with value [{}]", envVar, value);
            return value;
        }

        this.log.trace("No sysprop or envvar found for {} or {}", sysProp, envVar);
        return null;
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

    private int runWrappedCommand(ExecCfg cmd) throws Exception
    {
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

            final Duration maxWait = (this.cfg.getTimeout() > 0) //
                    ? Duration.ofMillis(this.cfg.getTimeout()) //
                    : null //
            ;

            switch (this.cfg.getMode())
            {
            case leader:
                this.log.info("Creating a leadership selector");
                Leader leader = new Leader(session, this.cfg.getName());
                try (AutoCloseable l = leader.awaitLeadership(maxWait))
                {
                    return run(cmd);
                }

            case mutex:
                this.log.info("Creating a mutex lock");
                Mutex mutex = new Mutex(session, this.cfg.getName());
                try (AutoCloseable m = mutex.acquire(maxWait))
                {
                    return run(cmd);
                }

            case init:
                this.log.info("Creating an initializer gate");
                InitializationGate init = new InitializationGate(session, this.cfg.getName());
                final String version = getParameter("version");
                if (Tools.isEmpty(version))
                {
                    this.log.error("Must provide a non-null, non-empty value for wrapper.param.version");
                    return 1;
                }

                final String marker = getParameter("marker");
                final String markerStdErr = getParameter("markerStdErr");
                final Initializer initializer = new FunctionalInitializer(Version.parse(version), (v, e) -> {

                    if (!Tools.isEmpty(marker))
                    {
                        boolean stdErr = (!Tools.isEmpty(markerStdErr) && Boolean.valueOf(markerStdErr));
                        // We've been asked to spit out a marker before running the initializer command,
                        // so do just that. We spit it out on its very own line, on the selected stream
                        // (i.e. we support using STDOUT or STDERR, per the caller's preference).
                        (stdErr ? System.err : System.out).printf("Initializer lock acquired:%n%s%n", marker);
                    }

                    int rc = run(cmd);
                    if (rc == 0)
                    {
                        return null;
                    }

                    // The command failed, so communicate it upwards...
                    throw new Exception(String.format("The command exited with a non-0 status: %d", rc));
                });
                init.initialize(initializer, maxWait);
                return 0;

            default:
                this.log.info("No algorithm for wrapper type {}", this.cfg.getMode());
                return run(cmd);
            }
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

        try
        {
            return runWrappedCommand(cmd);
        }
        catch (Exception e)
        {
            this.log.error("Exception caught during wrapping or command execution", e);
            return 1;
        }

    }

}
