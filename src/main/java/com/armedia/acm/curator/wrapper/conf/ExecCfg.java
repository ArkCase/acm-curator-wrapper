package com.armedia.acm.curator.wrapper.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecCfg
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String workdir = null;
    private Object command = null;
    private Map<String, String> env = Collections.emptyMap();
    private boolean cleanEnv = false;
    private RedirectCfg redirect = new RedirectCfg();

    public String getWorkdir()
    {
        return this.workdir;
    }

    public Object getCommand()
    {
        return this.command;
    }

    public Map<String, String> getEnv()
    {
        return this.env;
    }

    public boolean isCleanEnv()
    {
        return this.cleanEnv;
    }

    public RedirectCfg getRedirect()
    {
        return this.redirect;
    }

    public int run() throws Exception
    {
        Objects.requireNonNull(this.command);
        File workdir = new File(".");
        if (this.workdir != null)
        {
            workdir = new File(this.workdir);
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
        Objects.requireNonNull(this.command, "Must provide a non-null exec value");
        if (Collection.class.isInstance(this.command))
        {
            Collection<?> c = Collection.class.cast(this.command);
            cmd = new ArrayList<>(c.size());
            c.forEach((v) -> cmd.add(String.valueOf(v)));
        }
        else if (this.command.getClass().isArray())
        {
            Object[] arr = (Object[]) this.command;
            cmd = new ArrayList<>(arr.length);
            for (Object o : arr)
            {
                cmd.add(String.valueOf(o));
            }
        }
        else
        {
            StringTokenizer tok = new StringTokenizer(this.command.toString());
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
        this.redirect.apply(pb);

        Map<String, String> env = pb.environment();
        if (this.cleanEnv)
        {
            env.clear();
        }

        if (this.env != null)
        {
            env.putAll(this.env);
        }

        this.log.debug("Launching the process command {}", pb.command());
        this.log.trace("Using the environment:\n{}", pb.environment());

        return pb.start().waitFor();
    }
}