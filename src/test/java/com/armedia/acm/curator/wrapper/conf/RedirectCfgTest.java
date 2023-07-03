package com.armedia.acm.curator.wrapper.conf;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedirectCfgTest
{

    @Test
    public void testConstructor()
    {
        RedirectCfg cfg = new RedirectCfg();
        Assertions.assertNull(cfg.getStdin());
        Assertions.assertNull(cfg.getStdout());
        Assertions.assertNull(cfg.getStderr());
    }

    @Test
    public void testStdin()
    {
        RedirectCfg cfg = new RedirectCfg();
        Assertions.assertNull(cfg.getStdin());

        String str = UUID.randomUUID().toString();
        cfg.setStdin(str);
        Assertions.assertEquals(str, cfg.getStdin());
        Assertions.assertNull(cfg.getStdout());
        Assertions.assertNull(cfg.getStderr());

        cfg.setStdin(null);
        Assertions.assertNull(cfg.getStdin());
        Assertions.assertNull(cfg.getStdout());
        Assertions.assertNull(cfg.getStderr());
    }

    @Test
    public void testStdout()
    {
        RedirectCfg cfg = new RedirectCfg();
        Assertions.assertNull(cfg.getStdout());

        String str = UUID.randomUUID().toString();
        cfg.setStdout(str);
        Assertions.assertNull(cfg.getStdin());
        Assertions.assertEquals(str, cfg.getStdout());
        Assertions.assertNull(cfg.getStderr());

        cfg.setStdout(null);
        Assertions.assertNull(cfg.getStdin());
        Assertions.assertNull(cfg.getStdout());
        Assertions.assertNull(cfg.getStderr());
    }

    @Test
    public void testStderr()
    {
        RedirectCfg cfg = new RedirectCfg();
        Assertions.assertNull(cfg.getStderr());

        String str = UUID.randomUUID().toString();
        cfg.setStderr(str);
        Assertions.assertNull(cfg.getStdin());
        Assertions.assertNull(cfg.getStdout());
        Assertions.assertEquals(str, cfg.getStderr());

        cfg.setStderr(null);
        Assertions.assertNull(cfg.getStdin());
        Assertions.assertNull(cfg.getStdout());
        Assertions.assertNull(cfg.getStderr());
    }
}