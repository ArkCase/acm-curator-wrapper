package com.armedia.acm.curator.wrapper.conf;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.util.function.Consumer;

public class RedirectCfg
{
    private static final String NULL = "null";

    String stdin = null;
    String stdout = null;
    String stderr = null;

    public String getStdin()
    {
        return this.stdin;
    }

    public String getStdout()
    {
        return this.stdout;
    }

    public String getStderr()
    {
        return this.stderr;
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

    public void apply(ProcessBuilder pb)
    {
        if (pb == null)
        {
            return;
        }

        redirect(this.stdin, pb::redirectInput);
        redirect(this.stdout, pb::redirectOutput);
        redirect(this.stderr, pb::redirectError);
    }
}