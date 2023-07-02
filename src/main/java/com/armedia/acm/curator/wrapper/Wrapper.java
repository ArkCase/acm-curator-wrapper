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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.conf.ExecCfg;
import com.armedia.acm.curator.wrapper.conf.OperationMode;
import com.armedia.acm.curator.wrapper.conf.RedirectCfg;
import com.armedia.acm.curator.wrapper.conf.WrapperCfg;
import com.armedia.acm.curator.wrapper.module.Leader;
import com.armedia.acm.curator.wrapper.module.Mutex;
import com.armedia.acm.curator.wrapper.module.Session;
import com.armedia.acm.curator.wrapper.tools.CheckedSupplier;
import com.armedia.acm.curator.wrapper.tools.Tools;

public class Wrapper
{
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
            return new Leader(session, this.cfg.getName()).awaitLeadership();

        case mutex:
            this.log.info("Creating a mutex lock");
            return new Mutex(session, this.cfg.getName()).acquire();

        default:
            this.log.info("No-op wrapper created");
            return Tools::noop;
        }
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