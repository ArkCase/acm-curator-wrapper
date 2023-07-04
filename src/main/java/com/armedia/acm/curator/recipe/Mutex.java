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

import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class Mutex extends Recipe
{
    public static final String DEFAULT_MUTEX = "default";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final String path;

    public Mutex(Session session)
    {
        this(session, null);
    }

    public Mutex(Session session, String name)
    {
        super(session);
        String root = String.format("%s/mutex", session.getBasePath());
        this.name = Tools.ifEmpty(name, Mutex.DEFAULT_MUTEX);
        this.path = String.format("%s/%s", root, this.name);
    }

    public String getName()
    {
        return this.name;
    }

    public String getMutexPath()
    {
        return this.path;
    }

    public AutoCloseable acquire() throws Exception
    {
        return acquire(null);
    }

    public AutoCloseable acquire(Duration maxWait) throws Exception
    {
        this.session.assertEnabled();

        final InterProcessMutex lock = new InterProcessMutex(this.session.getClient(), this.path);
        if ((maxWait != null) && !maxWait.isNegative() && !maxWait.isZero())
        {
            this.log.info("Acquiring the mutex at [{}] (maximum wait {})", this.path, maxWait);
            if (!lock.acquire(maxWait.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new IllegalStateException(String.format("Timed out acquiring the lock [%s] (timeout = %s)", this.name, maxWait));
            }
        }
        else
        {
            this.log.info("Acquiring the mutex at [{}]", this.path);
            lock.acquire();
        }

        this.log.trace("Acquired the lock at [{}]", this.path);
        return () -> {
            this.log.trace("Releasing the lock at [{}]", this.path);
            lock.release();
        };
    }
}