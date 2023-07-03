package com.armedia.acm.curator.wrapper.conf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetryCfgTest
{
    @Test
    public void testConstructor()
    {
        RetryCfg cfg = new RetryCfg();
        Assertions.assertEquals(RetryCfg.DEF_COUNT, cfg.getCount());
        Assertions.assertEquals(RetryCfg.DEF_DELAY, cfg.getDelay());
    }

    @Test
    public void testCount()
    {
        RetryCfg cfg = new RetryCfg();
        Assertions.assertEquals(RetryCfg.DEF_COUNT, cfg.getCount());

        for (int i = Integer.MIN_VALUE; i < 0; i /= 10)
        {
            cfg.setCount(i);
            Assertions.assertEquals(RetryCfg.DEF_COUNT, cfg.getCount());
            Assertions.assertEquals(RetryCfg.DEF_DELAY, cfg.getDelay());
        }

        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            cfg.setCount(i);
            Assertions.assertEquals(i, cfg.getCount());
            Assertions.assertEquals(RetryCfg.DEF_DELAY, cfg.getDelay());
        }

        cfg.setCount(0);
        Assertions.assertEquals(0, cfg.getCount());
        Assertions.assertEquals(RetryCfg.DEF_DELAY, cfg.getDelay());
    }

    @Test
    public void testDelay()
    {
        RetryCfg cfg = new RetryCfg();
        Assertions.assertEquals(RetryCfg.DEF_DELAY, cfg.getDelay());

        for (int i = Integer.MIN_VALUE; i < 0; i /= 10)
        {
            cfg.setDelay(i);
            Assertions.assertEquals(RetryCfg.DEF_COUNT, cfg.getCount());
            Assertions.assertEquals(RetryCfg.MIN_DELAY, cfg.getDelay());
        }

        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            cfg.setDelay(i);
            Assertions.assertEquals(RetryCfg.DEF_COUNT, cfg.getCount());
            Assertions.assertEquals(Math.max(RetryCfg.MIN_DELAY, i), cfg.getDelay());
        }

        cfg.setDelay(0);
        Assertions.assertEquals(RetryCfg.DEF_COUNT, cfg.getCount());
        Assertions.assertEquals(RetryCfg.MIN_DELAY, cfg.getDelay());
    }

}