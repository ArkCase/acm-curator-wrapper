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

public class RetryCfg
{
    protected static final int MIN_DELAY = 100;
    protected static final int DEF_DELAY = 1000;
    protected static final int DEF_COUNT = 0;

    private int count = RetryCfg.DEF_COUNT;
    private int delay = RetryCfg.DEF_DELAY;

    public int getCount()
    {
        return (this.count = Math.max(RetryCfg.DEF_COUNT, this.count));
    }

    public void setCount(int count)
    {
        this.count = Math.max(RetryCfg.DEF_COUNT, count);
    }

    public int getDelay()
    {
        if (this.delay < 0)
        {
            this.delay = RetryCfg.DEF_DELAY;
        }
        return this.delay;
    }

    public void setDelay(int delay)
    {
        this.delay = Math.max(RetryCfg.MIN_DELAY, delay);
    }
}