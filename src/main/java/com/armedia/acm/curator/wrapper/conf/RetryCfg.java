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
