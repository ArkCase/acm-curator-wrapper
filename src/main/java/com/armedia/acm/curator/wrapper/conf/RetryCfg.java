package com.armedia.acm.curator.wrapper.conf;

import com.armedia.acm.curator.wrapper.tools.Tools;

public class RetryCfg
{
    private static final Integer DEF_DELAY = 1000;
    private static final Integer DEF_COUNT = 0;

    private Integer count = null;
    private Integer delay = null;

    public int getCount()
    {
        if (this.count == null)
        {
            this.count = RetryCfg.DEF_COUNT;
        }
        return this.count;
    }

    public void setCount(Integer count)
    {
        this.count = Tools.coalesce(count, RetryCfg.DEF_COUNT);
    }

    public int getDelay()
    {
        if (this.delay == null)
        {
            this.delay = RetryCfg.DEF_DELAY;
        }
        return this.delay;
    }

    public void setDelay(Integer delay)
    {
        this.delay = Tools.coalesce(delay, RetryCfg.DEF_DELAY);
    }
}