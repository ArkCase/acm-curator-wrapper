package com.armedia.acm.curator.wrapper.conf;

public class LeaderCfg
{
    String name = null;
    boolean requeue = false;

    public String getName()
    {
        return this.name;
    }

    public boolean isRequeue()
    {
        return this.requeue;
    }
}