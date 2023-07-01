package com.armedia.acm.curator.wrapper.module;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.conf.MutexCfg;
import com.armedia.acm.curator.wrapper.tools.Tools;

public class Mutex
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String mutexName;
    private final String mutexPath;
    private final Session connection;

    public Mutex(MutexCfg cfg, Session connection)
    {
        this.connection = connection;
        String baseMutexPath = String.format("%s/mutex", connection.getBasePath());
        this.mutexName = cfg.getName();
        this.mutexPath = (Tools.isEmpty(this.mutexName) //
                ? baseMutexPath //
                : String.format("%s/%s", baseMutexPath, this.mutexName) //
        );
    }

    public String getMutexName()
    {
        return this.mutexName;
    }

    public String getMutexPath()
    {
        return this.mutexPath;
    }

    public Session getConnection()
    {
        return this.connection;
    }

    public AutoCloseable acquire() throws Exception
    {
        return acquire(null, null);
    }

    public AutoCloseable acquire(Duration maxWait) throws Exception
    {
        return acquire(null, maxWait);
    }

    public AutoCloseable acquire(String mutexName) throws Exception
    {
        return acquire(mutexName, null);
    }

    public AutoCloseable acquire(String mutexName, Duration maxWait) throws Exception
    {
        this.connection.assertEnabled();

        final InterProcessMutex lock = new InterProcessMutex(this.connection.getClient(), this.mutexPath);
        if ((maxWait != null) && !maxWait.isNegative() && !maxWait.isZero())
        {
            if (!lock.acquire(maxWait.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new IllegalStateException(String.format("Timed out acquiring the lock [%s] (timeout = %s)", mutexName, maxWait));
            }
        }
        else
        {
            lock.acquire();
        }

        this.log.trace("Acquired the lock [{}]", mutexName);
        return () -> {
            this.log.trace("Releasing the lock [{}]", mutexName);
            lock.release();
        };
    }
}