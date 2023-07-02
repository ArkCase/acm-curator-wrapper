package com.armedia.acm.curator.wrapper.conf;

public class RedirectCfg
{
    String stdin = null;
    String stdout = null;
    String stderr = null;

    public String getStdin()
    {
        return this.stdin;
    }

    public void setStdin(String stdin)
    {
        this.stdin = stdin;
    }

    public String getStdout()
    {
        return this.stdout;
    }

    public void setStdout(String stdout)
    {
        this.stdout = stdout;
    }

    public String getStderr()
    {
        return this.stderr;
    }

    public void setStderr(String stderr)
    {
        this.stderr = stderr;
    }
}