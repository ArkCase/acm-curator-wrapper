package com.armedia.acm.curator.wrapper.conf;

import java.util.Collections;
import java.util.Map;

public class ExecCfg
{
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
}