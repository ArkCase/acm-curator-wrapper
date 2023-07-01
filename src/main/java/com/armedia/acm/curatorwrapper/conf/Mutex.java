package com.armedia.acm.curatorwrapper.conf;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mutex
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String baseMutexPath;
    private final CuratorSession connection;

    Mutex(CuratorSession connection)
    {
        this.connection = connection;
        this.baseMutexPath = String.format("%s/mutex", connection.basePath);
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

        final String mutexPath = (Configuration.isEmpty(mutexName) //
                ? this.baseMutexPath //
                : String.format("%s/%s", this.baseMutexPath, mutexName) //
        );
        final InterProcessMutex lock = new InterProcessMutex(this.connection.client, mutexPath);
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