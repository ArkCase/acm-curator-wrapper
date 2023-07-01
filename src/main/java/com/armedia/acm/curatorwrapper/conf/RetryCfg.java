package com.armedia.acm.curatorwrapper.conf;

import org.apache.curator.RetryPolicy;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryForever;

public class RetryCfg
{
    private static final int MIN_DELAY = 100;
    private static final int DEF_DELAY = 1000;
    private static final int MAX_DELAY = 60000;
    private static final int DEF_COUNT = 0;
    private int count = RetryCfg.DEF_COUNT;
    private int delay = RetryCfg.DEF_DELAY;

    public int getCount()
    {
        return this.count;
    }

    public long getDelay()
    {
        return this.delay;
    }

    public RetryPolicy asRetryPolicy()
    {
        final int delay = Math.max(RetryCfg.MIN_DELAY, Math.min(RetryCfg.MAX_DELAY, this.delay));
        if (this.count <= 0)
        {
            return new RetryForever(delay);
        }
        return new ExponentialBackoffRetry(delay, this.count);
    }
}