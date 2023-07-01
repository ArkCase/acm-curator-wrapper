package com.armedia.acm.curator.wrapper.conf;

public class ZookeeperCfg
{
    private static final int MIN_SESSION_TIMEOUT = 1;
    private static final int DEF_SESSION_TIMEOUT = 1;
    private static final int MIN_CONNECTION_TIMEOUT = 100;
    private static final int DEF_CONNECTION_TIMEOUT = 5000;
    String connect = null;
    int sessionTimeout = ZookeeperCfg.DEF_SESSION_TIMEOUT;
    int connectionTimeout = ZookeeperCfg.DEF_CONNECTION_TIMEOUT;
    String basePath = null;
    RetryCfg retry = new RetryCfg();

    public String getConnect()
    {
        return this.connect;
    }

    public long getSessionTimeout()
    {
        return this.sessionTimeout;
    }

    public String getBasePath()
    {
        return this.basePath;
    }

    public RetryCfg getRetry()
    {
        return this.retry;
    }

    public CuratorSession connect() throws InterruptedException
    {
        this.sessionTimeout = Math.max(ZookeeperCfg.MIN_SESSION_TIMEOUT, this.sessionTimeout);
        this.connectionTimeout = Math.max(ZookeeperCfg.MIN_CONNECTION_TIMEOUT, this.connectionTimeout);
        return new CuratorSession(this);
    }
}