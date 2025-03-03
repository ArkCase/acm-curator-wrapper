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

import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock.ReadLock;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock.WriteLock;

import com.armedia.acm.curator.Session;

public class ReadWriteLock extends Recipe
{
    public static final String DEFAULT_NAME = "default";

    public class Read implements AutoCloseable
    {
        private final InterProcessReadWriteLock rwLock;
        public final ReadLock lock;

        private Read(Duration maxWait) throws Exception
        {
            if (!isSessionEnabled())
            {
                ReadWriteLock.this.log.debug("The current session is not enabled - read locking functionality is not available");
                this.lock = null;
                this.rwLock = null;
                return;
            }

            this.rwLock = new InterProcessReadWriteLock(getClient(), ReadWriteLock.this.path);

            this.lock = this.rwLock.readLock();
            if ((maxWait != null) && !maxWait.isNegative() && !maxWait.isZero())
            {
                ReadWriteLock.this.log.info("Acquiring the read lock at [{}] (maximum wait {})", ReadWriteLock.this.path, maxWait);
                if (!this.lock.acquire(maxWait.toMillis(), TimeUnit.MILLISECONDS))
                {
                    throw new TimeoutException(
                            String.format("Timed out acquiring the read lock [%s] (timeout = %s)", ReadWriteLock.this.name, maxWait));
                }
            }
            else
            {
                ReadWriteLock.this.log.info("Acquiring the read lock at [{}]", ReadWriteLock.this.path);
                this.lock.acquire();
            }

            ReadWriteLock.this.log.trace("Acquired the read lock at [{}]", ReadWriteLock.this.path);
        }

        public Write upgrade() throws Exception
        {
            return upgrade(null);
        }

        public Write upgrade(Duration maxWait) throws Exception
        {
            close();
            return new Write(maxWait, this);
        }

        @Override
        public void close() throws Exception
        {
            ReadWriteLock.this.log.trace("Releasing the read lock at [{}]", ReadWriteLock.this.path);
            if (this.lock != null)
            {
                this.lock.release();
            }
        }
    }

    public class Write implements AutoCloseable
    {
        private final InterProcessReadWriteLock rwLock;
        private final WriteLock lock;
        private final Read read;

        private Write(Duration maxWait) throws Exception
        {
            this(maxWait, null);
        }

        private Write(Duration maxWait, Read read) throws Exception
        {
            if (!isSessionEnabled())
            {
                ReadWriteLock.this.log.debug("The current session is not enabled - write locking functionality is not available");
                this.rwLock = null;
                this.lock = null;
                this.read = null;
                return;
            }

            this.read = read;
            this.rwLock = (read != null //
                    ? read.rwLock //
                    : new InterProcessReadWriteLock(getClient(),
                            ReadWriteLock.this.path) //
            );

            this.lock = this.rwLock.writeLock();
            if ((maxWait != null) && !maxWait.isNegative() && !maxWait.isZero())
            {
                ReadWriteLock.this.log.info("Acquiring the write lock at [{}] (maximum wait {})", ReadWriteLock.this.path, maxWait);
                if (!this.lock.acquire(maxWait.toMillis(), TimeUnit.MILLISECONDS))
                {
                    throw new TimeoutException(
                            String.format("Timed out acquiring the write lock [%s] (timeout = %s)", ReadWriteLock.this.name, maxWait));
                }
            }
            else
            {
                ReadWriteLock.this.log.info("Acquiring the write lock at [{}]", ReadWriteLock.this.path);
                this.lock.acquire();
            }

            ReadWriteLock.this.log.trace("Acquired the write lock at [{}]", ReadWriteLock.this.path);
        }

        @Override
        public void close() throws Exception
        {
            if (this.read != null)
            {
                ReadWriteLock.this.log.trace("Re-acquiring the read lock at [{}]", ReadWriteLock.this.path);
                this.read.lock.acquire();
            }
            ReadWriteLock.this.log.trace("Releasing the write lock at [{}]", ReadWriteLock.this.path);
            if (this.lock != null)
            {
                this.lock.release();
            }
        }
    }

    public ReadWriteLock(Session session)
    {
        this(session, null);
    }

    public ReadWriteLock(Session session, String name)
    {
        super(session, name);
    }

    public Read read() throws Exception
    {
        return read(null);
    }

    public Read read(Duration maxWait) throws Exception
    {
        return new Read(maxWait);
    }

    public Write write() throws Exception
    {
        return write(null);
    }

    public Write write(Duration maxWait) throws Exception
    {
        return new Write(maxWait);
    }
}