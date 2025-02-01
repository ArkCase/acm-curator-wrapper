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
package com.armedia.acm.curator.recipe;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class Mutex extends Recipe
{
    public static final String DEFAULT_NAME = "default";

    public Mutex(Session session)
    {
        this(session, null);
    }

    public Mutex(Session session, String name)
    {
        super(session, name);
    }

    public AutoCloseable acquire() throws Exception
    {
        return acquire(null);
    }

    public AutoCloseable acquire(Duration maxWait) throws Exception
    {
        if (!isSessionEnabled())
        {
            this.log.warn("The current session is not enabled, no mutex to acquire");
            return Tools::noop;
        }

        final InterProcessMutex lock = new InterProcessMutex(getClient(), this.path);
        if ((maxWait != null) && !maxWait.isNegative() && !maxWait.isZero())
        {
            this.log.info("Acquiring the mutex at [{}] (maximum wait {})", this.path, maxWait);
            if (!lock.acquire(maxWait.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new TimeoutException(String.format("Timed out acquiring the lock [%s] (timeout = %s)", this.name, maxWait));
            }
        }
        else
        {
            this.log.info("Acquiring the mutex at [{}]", this.path);
            lock.acquire();
        }

        this.log.trace("Acquired the lock at [{}]", this.path);
        return new ItemCloser<>(lock, InterProcessMutex::release);
    }
}