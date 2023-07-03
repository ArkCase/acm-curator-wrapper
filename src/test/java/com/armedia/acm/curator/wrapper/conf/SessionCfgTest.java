package com.armedia.acm.curator.wrapper.conf;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SessionCfgTest
{
    @Test
    public void testParseHostPort()
    {
    }

    @Test
    public void testConstructor()
    {
        SessionCfg cfg = new SessionCfg();
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
    }

    @Test
    public void testConnect()
    {
        SessionCfg cfg = new SessionCfg();
        Assertions.assertNull(cfg.getConnect());

        String c = "abc:123";
        cfg.setConnect(c);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertEquals(c, cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());

        cfg.setConnect(null);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
    }

    @Test
    public void testSessionTimeout()
    {
        SessionCfg cfg = new SessionCfg();
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());

        for (int i = Integer.MIN_VALUE; i < 0; i /= 10)
        {
            cfg.setSessionTimeout(i);
            Assertions.assertNull(cfg.getBasePath());
            Assertions.assertNull(cfg.getConnect());
            Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
            Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
            Assertions.assertNotNull(cfg.getRetry());
        }

        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            cfg.setSessionTimeout(i);
            Assertions.assertNull(cfg.getBasePath());
            Assertions.assertNull(cfg.getConnect());
            Assertions.assertEquals(Math.max(SessionCfg.MIN_SESSION_TIMEOUT, i), cfg.getSessionTimeout());
            Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
            Assertions.assertNotNull(cfg.getRetry());
        }

        cfg.setSessionTimeout(0);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
    }

    @Test
    public void testConnectionTimeout()
    {
        SessionCfg cfg = new SessionCfg();
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());

        for (int i = Integer.MIN_VALUE; i < 0; i /= 10)
        {
            cfg.setConnectionTimeout(i);
            Assertions.assertNull(cfg.getBasePath());
            Assertions.assertNull(cfg.getConnect());
            Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
            Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
            Assertions.assertNotNull(cfg.getRetry());
        }

        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            cfg.setConnectionTimeout(i);
            Assertions.assertNull(cfg.getBasePath());
            Assertions.assertNull(cfg.getConnect());
            Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
            Assertions.assertEquals(Math.max(SessionCfg.MIN_CONNECTION_TIMEOUT, i), cfg.getConnectionTimeout());
            Assertions.assertNotNull(cfg.getRetry());
        }

        cfg.setConnectionTimeout(0);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
    }

    @Test
    public void testBasePath()
    {
        SessionCfg cfg = new SessionCfg();
        Assertions.assertNull(cfg.getBasePath());

        String str = UUID.randomUUID().toString();
        cfg.setBasePath(str);
        Assertions.assertEquals(str, cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());

        cfg.setBasePath(null);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
    }

    @Test
    public void testRetry()
    {
        SessionCfg cfg = new SessionCfg();
        Assertions.assertNotNull(cfg.getRetry());

        RetryCfg r = cfg.getRetry();
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
        Assertions.assertSame(r, cfg.getRetry());

        RetryCfg n = new RetryCfg();
        cfg.setRetry(n);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotSame(r, cfg.getRetry());
        Assertions.assertSame(n, cfg.getRetry());

        cfg.setRetry(null);
        Assertions.assertNull(cfg.getBasePath());
        Assertions.assertNull(cfg.getConnect());
        Assertions.assertEquals(SessionCfg.DEFAULT_SESSION_TIMEOUT, cfg.getSessionTimeout());
        Assertions.assertEquals(SessionCfg.DEFAULT_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());
        Assertions.assertNotNull(cfg.getRetry());
        Assertions.assertNotSame(r, cfg.getRetry());
        Assertions.assertNotSame(n, cfg.getRetry());
    }
}