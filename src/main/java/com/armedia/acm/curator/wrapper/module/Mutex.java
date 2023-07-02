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
package com.armedia.acm.curator.wrapper.module;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.tools.Tools;

public class Mutex extends Recipe
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String mutexName;
    private final String mutexPath;

    private Mutex(Session session, String name)
    {
        super(session);
        String baseMutexPath = String.format("%s/mutex", session.getBasePath());
        this.mutexName = name;
        this.mutexPath = (Tools.isEmpty(this.mutexName) //
                ? baseMutexPath //
                : String.format("%s/%s", baseMutexPath, this.mutexName) //
        );
    }

    public String getMutexName()
    {
        return this.mutexName;
    }

    public String getMutexPath()
    {
        return this.mutexPath;
    }

    public Session getConnection()
    {
        return this.session;
    }

    public AutoCloseable acquire() throws Exception
    {
        return acquire(null, null);
    }

    public AutoCloseable acquire(Duration maxWait) throws Exception
    {
        return acquire(null, maxWait);
    }

    public AutoCloseable acquire(String mutexName) throws Exception
    {
        return acquire(mutexName, null);
    }

    public AutoCloseable acquire(String mutexName, Duration maxWait) throws Exception
    {
        this.session.assertEnabled();

        final InterProcessMutex lock = new InterProcessMutex(this.session.getClient(), this.mutexPath);
        if ((maxWait != null) && !maxWait.isNegative() && !maxWait.isZero())
        {
            if (!lock.acquire(maxWait.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new IllegalStateException(String.format("Timed out acquiring the lock [%s] (timeout = %s)", mutexName, maxWait));
            }
        }
        else
        {
            lock.acquire();
        }

        this.log.trace("Acquired the lock [{}]", mutexName);
        return () -> {
            this.log.trace("Releasing the lock [{}]", mutexName);
            lock.release();
        };
    }

    public static class Builder
    {
        private String name;

        public String name()
        {
            return this.name;
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public Mutex build(Session session)
        {
            return new Mutex(session, this.name);
        }
    }
}
