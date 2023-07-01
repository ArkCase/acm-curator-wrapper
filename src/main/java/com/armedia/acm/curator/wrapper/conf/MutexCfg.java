package com.armedia.acm.curator.wrapper.conf;

public class MutexCfg
{
    String name = null;
    int timeout = 0;

    public String getName()
    {
        return this.name;
    }

    public long getTimeout()
    {
        return this.timeout;
    }
}