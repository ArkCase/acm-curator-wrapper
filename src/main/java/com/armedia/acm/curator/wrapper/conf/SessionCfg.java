package com.armedia.acm.curator.wrapper.conf;

public class SessionCfg
{
    private String connect = null;
    private int sessionTimeout = 0;
    private int connectionTimeout = 0;
    private String basePath = null;
    private RetryCfg retry = new RetryCfg();

    public String getConnect()
    {
        return this.connect;
    }

    public int getSessionTimeout()
    {
        return this.sessionTimeout;
    }

    public int getConnectionTimeout()
    {
        return this.connectionTimeout;
    }

    public String getBasePath()
    {
        return this.basePath;
    }

    public RetryCfg getRetry()
    {
        return this.retry;
    }
}