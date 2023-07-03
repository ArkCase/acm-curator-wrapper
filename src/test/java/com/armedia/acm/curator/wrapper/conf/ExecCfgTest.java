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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.acm.curator.tools.Tools;

public class ExecCfgTest
{
    @Test
    public void testConstructor()
    {
        ExecCfg cfg = new ExecCfg();
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
    }

    @Test
    public void testWorkdir()
    {
        ExecCfg cfg = new ExecCfg();
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        cfg.setWorkdir("/");
        Assertions.assertEquals(cfg.getWorkdir(), "/");
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
        cfg.setWorkdir("");
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
        cfg.setWorkdir("/");
        Assertions.assertEquals(cfg.getWorkdir(), "/");
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
        cfg.setWorkdir(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
    }

    @Test
    public void testCommand()
    {
        ExecCfg cfg = new ExecCfg();
        Assertions.assertNull(cfg.getCommand());

        cfg.setCommand(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertNull(cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        String str = UUID.randomUUID().toString();
        cfg.setCommand(str);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(str, cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        cfg.setCommand(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertNull(cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        Object[] arr = new Object[10];
        for (int i = 0; i < arr.length; i++)
        {
            arr[i] = new Object();
        }
        cfg.setCommand(arr);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertArrayEquals(arr, (Object[]) cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        cfg.setCommand(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertNull(cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        List<?> list = Arrays.asList(arr);
        cfg.setCommand(list);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(list, cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        cfg.setCommand(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertNull(cfg.getCommand());
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        try
        {
            cfg.setCommand(new Object());
            Assertions.fail("Did not fail when setting the wrong object type");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    @Test
    public void testEnv()
    {
        ExecCfg cfg = new ExecCfg();
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());

        Map<String, String> oldEnv = cfg.getEnv();
        cfg.setEnv(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertNotSame(oldEnv, cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        Map<String, String> m = new TreeMap<>();
        Map<String, String> n = new TreeMap<>();
        for (int i = 0; i < 10; i++)
        {
            String k = String.valueOf(i);
            String v = UUID.randomUUID().toString();
            m.put(k, v);
            n.put(k, v);
        }
        cfg.setEnv(m);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertSame(m, cfg.getEnv());
        Assertions.assertEquals(n, m);
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
    }

    @Test
    public void testCleanEnv()
    {
        ExecCfg cfg = new ExecCfg();
        Assertions.assertFalse(cfg.isCleanEnv());

        cfg.setCleanEnv(true);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertTrue(cfg.isCleanEnv());

        cfg.setCleanEnv(false);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
    }

    @Test
    public void testRedirect()
    {
        ExecCfg cfg = new ExecCfg();

        RedirectCfg r = cfg.getRedirect();
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(r);
        Assertions.assertFalse(cfg.isCleanEnv());

        RedirectCfg n = new RedirectCfg();
        cfg.setRedirect(n);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotSame(r, cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());

        cfg.setRedirect(null);
        Assertions.assertEquals(cfg.getWorkdir(), Tools.CWD.getPath());
        Assertions.assertEquals(cfg.getCommand(), null);
        Assertions.assertNotNull(cfg.getEnv());
        Assertions.assertTrue(cfg.getEnv().isEmpty());
        Assertions.assertNotNull(cfg.getRedirect());
        Assertions.assertNotSame(r, cfg.getRedirect());
        Assertions.assertNotSame(n, cfg.getRedirect());
        Assertions.assertFalse(cfg.isCleanEnv());
    }
}
