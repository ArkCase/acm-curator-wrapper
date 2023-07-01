package com.armedia.acm.curatorwrapper.conf;

import java.time.Duration;

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

    public AutoCloseable acquire(CuratorSession connection) throws Exception
    {
        if (this.timeout <= 0)
        {
            return new Mutex(connection).acquire(this.name);
        }
        return new Mutex(connection).acquire(this.name, Duration.ofMillis(this.timeout));
    }
}