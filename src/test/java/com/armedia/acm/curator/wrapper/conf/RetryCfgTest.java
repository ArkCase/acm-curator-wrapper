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
