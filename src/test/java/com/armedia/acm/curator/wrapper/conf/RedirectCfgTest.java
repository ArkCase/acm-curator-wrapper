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
