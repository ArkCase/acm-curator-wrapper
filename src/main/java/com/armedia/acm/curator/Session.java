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
package com.armedia.acm.curator;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryForever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.tools.Tools;

public class Session implements AutoCloseable
{
    public static final int MIN_SESSION_TIMEOUT = 1000;
    public static final int DEFAULT_SESSION_TIMEOUT = 15000;

    public static final int MIN_CONNECTION_TIMEOUT = 100;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public static final int DEFAULT_RETRY_COUNT = 0;
    public static final int MIN_RETRY_COUNT = 0;

    public static final int DEFAULT_RETRY_DELAY = 1000;
    public static final int MIN_RETRY_DELAY = 100;
    public static final int MAX_RETRY_DELAY = 60000;

    public static final String DEFAULT_BASE_PATH = "/arkcase";

    public static final boolean DEFAULT_WAIT_FOR_CONNECTION = true;

    protected static final String NULL_CLEANUP_KEY = "<n/a>";

    private static int sanitizeValue(int value, int def, int min)
    {
        if (value <= 0)
        {
            return def;
        }
        return Math.max(min, value);
    }

    public static int sanitizeRetryDelay(int retryDelay)
    {
        return Session.sanitizeValue(retryDelay, Session.DEFAULT_RETRY_DELAY, Session.MIN_RETRY_DELAY);
    }

    public static int sanitizeRetryCount(int retryCount)
    {
        return Session.sanitizeValue(retryCount, Session.DEFAULT_RETRY_COUNT, 0);
    }

    public static int sanitizeSessionTimeout(int sessionTimeout)
    {
        return Session.sanitizeValue(sessionTimeout, Session.DEFAULT_SESSION_TIMEOUT, Session.MIN_SESSION_TIMEOUT);
    }

    public static int sanitizeConnectionTimeout(int connectionTimeout)
    {
        return Session.sanitizeValue(connectionTimeout, Session.DEFAULT_CONNECTION_TIMEOUT, Session.MIN_CONNECTION_TIMEOUT);
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private CuratorFramework client = null;
    private Thread cleanup = null;
    private final String basePath;
    private final AtomicInteger selectorKeys = new AtomicInteger();
    private final Map<Integer, Closeable> selectors = Collections.synchronizedMap(new TreeMap<>());

    private Session(Builder builder)
            throws InterruptedException
    {
        if (Tools.isEmpty(builder.connect))
        {
            this.basePath = null;
            this.log.info("No ZooKeeper configuration");
            return;
        }

        int sessionTimeout = builder.sessionTimeout;
        int connectionTimeout = builder.connectionTimeout;

        RetryPolicy retryPolicy = null;
        if (builder.retryCount <= 0)
        {
            retryPolicy = new RetryForever(builder.retryDelay);
            this.log.debug("Clustering retry is infinite");
        }
        else
        {
            this.log.debug("Clustering retry count is {}", builder.retryCount);
            retryPolicy = new ExponentialBackoffRetry(builder.retryDelay, builder.retryCount);
        }

        this.log.debug("Clustering retry policy is {}, with a delay of {}", retryPolicy.getClass().getSimpleName(),
                builder.retryDelay);

        this.basePath = builder.basePath;

        this.log.info("ZooKeeper connection string: [{}]", builder.connect);

        retryPolicy = Tools.ifNull(retryPolicy, () -> new RetryForever(1000));

        this.log.trace("Initializing the Curator client");
        this.client = CuratorFrameworkFactory.newClient(builder.connect, sessionTimeout, connectionTimeout, retryPolicy);
        this.log.info("Starting the Curator client");
        this.client.start();
        if (builder.waitForConnection)
        {
            this.client.blockUntilConnected();
        }
        this.cleanup = new Thread(this::cleanup, "ZookeeperConnection-Cleanup");
        this.cleanup.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(this.cleanup);
    }

    public String getBasePath()
    {
        return this.basePath;
    }

    public synchronized CuratorFramework getClient()
    {
        return this.client;
    }

    public void assertEnabled()
    {
        if (!isEnabled())
        {
            throw new IllegalStateException("Zookeeper is not enabled");
        }
    }

    public synchronized boolean isEnabled()
    {
        return (this.client != null);
    }

    public Object addCleanup(Closeable closeable)
    {
        if ((closeable != null) && isEnabled())
        {
            int key = this.selectorKeys.getAndIncrement();
            this.selectors.put(key, closeable);
            return key;
        }
        else
        {
            return Session.NULL_CLEANUP_KEY;
        }
    }

    public Closeable removeCleanup(Object key)
    {
        return this.selectors.remove(key);
    }

    private synchronized void cleanup()
    {
        // Always unlock in reverse order to acquisition...
        for (Object k : new TreeSet<>(this.selectors.keySet()).descendingSet())
        {
            Closeable c = this.selectors.get(k);
            this.log.warn("Emergency cleanup: closing out leadership selector # {}", k);
            try
            {
                c.close();
            }
            catch (Exception e)
            {
                this.log.warn("Exception caught during cleanup of leadership selector # {}", k, e);
            }
        }

        if (this.client.getState() != CuratorFrameworkState.STOPPED)
        {
            try
            {
                this.client.close();
            }
            catch (Exception e)
            {
                this.log.warn("Exception caught while closing the main client", e);
            }
        }
    }

    @Override
    public synchronized void close()
    {
        if (this.client != null)
        {
            try
            {
                cleanup();
            }
            finally
            {
                if (this.cleanup != null)
                {
                    Runtime.getRuntime().removeShutdownHook(this.cleanup);
                }
                this.cleanup = null;
            }
        }
    }

    public static class Builder
    {

        private String connect = null;
        private int sessionTimeout = Session.DEFAULT_SESSION_TIMEOUT;
        private int connectionTimeout = Session.DEFAULT_CONNECTION_TIMEOUT;
        private String basePath = Session.DEFAULT_BASE_PATH;

        private int retryDelay = Session.DEFAULT_RETRY_DELAY;
        private int retryCount = Session.DEFAULT_RETRY_COUNT;

        private boolean waitForConnection = Session.DEFAULT_WAIT_FOR_CONNECTION;

        public String connect()
        {
            return this.connect;
        }

        public Builder connect(String connect)
        {
            this.connect = connect;
            return this;
        }

        public int sessionTimeout()
        {
            return this.sessionTimeout;
        }

        public Builder sessionTimeout(int sessionTimeout)
        {
            this.sessionTimeout = Session.sanitizeSessionTimeout(sessionTimeout);
            return this;
        }

        public int connectionTimeout()
        {
            return this.connectionTimeout;
        }

        public Builder connectionTimeout(int connectionTimeout)
        {
            this.connectionTimeout = Session.sanitizeConnectionTimeout(connectionTimeout);
            return this;
        }

        public String basePath()
        {
            return this.basePath;
        }

        public Builder basePath(String basePath)
        {
            this.basePath = Tools.isEmpty(basePath) //
                    ? Session.DEFAULT_BASE_PATH //
                    : basePath //
            ;
            return this;
        }

        public int retryCount()
        {
            return this.retryCount;
        }

        public Builder retryCount(int retryCount)
        {
            this.retryCount = Session.sanitizeValue(retryCount, Session.DEFAULT_RETRY_COUNT, Session.MIN_RETRY_COUNT);
            return this;
        }

        public int retryDelay()
        {
            return this.retryDelay;
        }

        public Builder retryDelay(int retryDelay)
        {
            this.retryDelay = Math.min(Session.MAX_RETRY_DELAY,
                    Session.sanitizeValue(retryDelay, Session.DEFAULT_RETRY_DELAY, Session.MIN_RETRY_DELAY));
            return this;
        }

        public boolean waitForConnection()
        {
            return this.waitForConnection;
        }

        public Builder waitForConnection(Boolean wait)
        {
            this.waitForConnection = Tools.ifNull(wait, () -> Session.DEFAULT_WAIT_FOR_CONNECTION);
            return this;
        }

        public Session build() throws InterruptedException
        {
            return new Session(this);
        }
    }
}
