package com.armedia.acm.curator.wrapper.conf;

import java.io.File;

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

    public void apply(ProcessBuilder pb)
    {
        // Always do this
        pb.inheritIO();
        if (this.stdin != null)
        {
            if (RedirectCfg.NULL.equalsIgnoreCase(this.stdin))
            {
                pb.redirectInput(java.lang.ProcessBuilder.Redirect.DISCARD);
            }
            else
            {
                pb.redirectInput(new File(this.stdin));
            }
        }
        if (this.stdout != null)
        {
            if (RedirectCfg.NULL.equalsIgnoreCase(this.stdout))
            {
                pb.redirectOutput(java.lang.ProcessBuilder.Redirect.DISCARD);
            }
            else
            {
                pb.redirectOutput(new File(this.stdout));
            }
        }
        if (this.stderr != null)
        {
            if (RedirectCfg.NULL.equalsIgnoreCase(this.stderr))
            {
                pb.redirectError(java.lang.ProcessBuilder.Redirect.DISCARD);
            }
            else
            {
                pb.redirectError(new File(this.stderr));
            }
        }
    }
}