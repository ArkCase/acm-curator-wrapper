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

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WrapperCfgTest
{
    @Test
    public void testConstructor()
    {
        WrapperCfg cfg = new WrapperCfg();
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());
    }

    @Test
    public void testMode()
    {
        WrapperCfg cfg = new WrapperCfg();
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());

        for (OperationMode m : OperationMode.values())
        {
            cfg.setMode(m);
            Assertions.assertSame(m, cfg.getMode());
            Assertions.assertNull(cfg.getName());
            Assertions.assertNotNull(cfg.getParams());
            Assertions.assertTrue(cfg.getParams().isEmpty());
            Assertions.assertNotNull(cfg.getExec());
            Assertions.assertEquals(0L, cfg.getTimeout());
        }

        cfg.setMode(null);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());
    }

    @Test
    public void testName()
    {
        WrapperCfg cfg = new WrapperCfg();
        Assertions.assertNull(cfg.getName());

        String str = UUID.randomUUID().toString();
        cfg.setName(str);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertEquals(str, cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());

        cfg.setName(null);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());
    }

    @Test
    public void testTimeout()
    {
        WrapperCfg cfg = new WrapperCfg();
        Assertions.assertEquals(0L, cfg.getTimeout());

        for (long i = Long.MIN_VALUE; i < 0; i /= 10)
        {
            cfg.setTimeout(i);
            Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
            Assertions.assertNull(cfg.getName());
            Assertions.assertNotNull(cfg.getParams());
            Assertions.assertTrue(cfg.getParams().isEmpty());
            Assertions.assertNotNull(cfg.getExec());
            Assertions.assertEquals(0L, cfg.getTimeout());
        }

        for (long i = Long.MAX_VALUE; i > 0; i /= 10)
        {
            cfg.setTimeout(i);
            Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
            Assertions.assertNull(cfg.getName());
            Assertions.assertNotNull(cfg.getParams());
            Assertions.assertTrue(cfg.getParams().isEmpty());
            Assertions.assertNotNull(cfg.getExec());
            Assertions.assertEquals(i, cfg.getTimeout());
        }

        cfg.setTimeout(0L);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());
    }

    @Test
    public void testParams()
    {
        WrapperCfg cfg = new WrapperCfg();
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());

        Map<String, ?> p = cfg.getParams();
        Map<String, Object> m = new TreeMap<>();
        Map<String, Object> n = new TreeMap<>();
        for (int i = 0; i < 10; i++)
        {
            String k = String.valueOf(i);
            String v = UUID.randomUUID().toString();
            m.put(k, v);
            n.put(k, v);
        }

        cfg.setParams(m);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotSame(p, cfg.getParams());
        Assertions.assertSame(m, cfg.getParams());
        Assertions.assertFalse(cfg.getParams().isEmpty());
        Assertions.assertEquals(n, m);
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());

        cfg.setParams(null);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotSame(p, cfg.getParams());
        Assertions.assertNotSame(m, cfg.getParams());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());
    }

    @Test
    public void testExec()
    {
        WrapperCfg cfg = new WrapperCfg();
        Assertions.assertNotNull(cfg.getExec());

        ExecCfg e = cfg.getExec();
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertSame(e, cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());

        ExecCfg n = new ExecCfg();
        cfg.setExec(n);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotSame(e, cfg.getExec());
        Assertions.assertSame(n, cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());

        cfg.setExec(null);
        Assertions.assertSame(WrapperCfg.DEFAULT_MODE, cfg.getMode());
        Assertions.assertNull(cfg.getName());
        Assertions.assertNotNull(cfg.getParams());
        Assertions.assertTrue(cfg.getParams().isEmpty());
        Assertions.assertNotNull(cfg.getExec());
        Assertions.assertNotSame(e, cfg.getExec());
        Assertions.assertNotSame(n, cfg.getExec());
        Assertions.assertEquals(0L, cfg.getTimeout());
    }
}
