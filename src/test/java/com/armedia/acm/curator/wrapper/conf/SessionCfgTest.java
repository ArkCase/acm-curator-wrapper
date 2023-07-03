/*-
 * #%L
 * acm-curator-wrapper
 * %%
 * Copyright (C) 2023 ArkCase LLC
 * %%
 * This file is part of the ArkCase software.
 *
 * If the software was purchased under a paid ArkCase license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * ArkCase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ArkCase is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
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
