package com.armedia.acm.curator.wrapper.conf;

import com.armedia.acm.curator.wrapper.module.Session;
import com.armedia.acm.curator.wrapper.tools.Tools;

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

    public void setConnect(String connect)
    {
        this.connect = connect;
    }

    public int getSessionTimeout()
    {
        if (this.sessionTimeout < 0)
        {
            this.sessionTimeout = 0;
        }
        return this.sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout)
    {
        this.sessionTimeout = Math.max(0, sessionTimeout);
    }

    public int getConnectionTimeout()
    {
        if (this.connectionTimeout < 0)
        {
            this.connectionTimeout = 0;
        }
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = Math.max(0, connectionTimeout);
    }

    public String getBasePath()
    {
        return this.basePath;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public RetryCfg getRetry()
    {
        if (this.retry == null)
        {
            this.retry = new RetryCfg();
        }
        return this.retry;
    }

    public void setRetry(RetryCfg retry)
    {
        this.retry = Tools.ifNull(retry, RetryCfg::new);
    }

    public Session build() throws InterruptedException
    {
        // This helps ensure we have a value
        RetryCfg retry = getRetry();
        return new Session.Builder() //
                .connect(this.connect) //
                .sessionTimeout(this.sessionTimeout) //
                .connectionTimeout(this.connectionTimeout) //
                .basePath(this.basePath) //
                .retryCount(retry.getCount()) //
                .retryDelay(retry.getDelay()) //
                .build() //
        ;
    }
}