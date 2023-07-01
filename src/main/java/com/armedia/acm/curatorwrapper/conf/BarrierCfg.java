package com.armedia.acm.curatorwrapper.conf;

public class BarrierCfg
{
    String name = null;
    long timeout = 0;

    public String getName()
    {
        return this.name;
    }

    public long getTimeout()
    {
        return this.timeout;
    }
}