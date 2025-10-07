package com.armedia.acm.curator.recipe;

/*-
 * #%L
 * acm-curator-wrapper
 * %%
 * Copyright (C) 2023 - 2025 ArkCase LLC
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

import org.apache.zookeeper.data.Stat;

import com.armedia.acm.curator.Session;

public class Exists extends Recipe
{
    public Exists(Session session)
    {
        super(session);
    }

    public Exists(Session session, String name)
    {
        super(session, name);
    }

    public int execute()
    {
        if (!isSessionEnabled())
        {
            this.log.warn("The current session is not enabled, cannot check for the existence of resources");
            return 1;
        }

        try
        {
            Stat stat = getSession().getClient().checkExists().forPath(this.name);
            return (stat != null) ? 0 : 1;
        }
        catch (Exception e)
        {
            this.log.error("Failed to check for the existence of [{}]", this.name, e);
            return 1;
        }
    }
}
