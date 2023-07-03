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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryForever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.tools.Tools;

public class Session implements AutoCloseable
{
    private static final int MIN_DELAY = 100;
    private static final int MAX_DELAY = 60000;

    private static final int MIN_SESSION_TIMEOUT = 1;
    private static final int MIN_CONNECTION_TIMEOUT = 100;

    private static final String ROOT_PATH = "/arkcase";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private CuratorFramework client = null;
    private Thread cleanup = null;
    private final String basePath;
    private final Map<Integer, LeaderSelector> selectors = Collections.synchronizedMap(new TreeMap<>());

    private Session(Builder cfg)
            throws InterruptedException
    {
        if (Tools.isEmpty(cfg.connect))
        {
            this.basePath = null;
            this.log.info("No ZooKeeper configuration");
            return;
        }

        int sessionTimeout = Math.max(Session.MIN_SESSION_TIMEOUT, cfg.sessionTimeout);
        int connectionTimeout = Math.max(Session.MIN_CONNECTION_TIMEOUT, cfg.connectionTimeout);

        RetryPolicy retryPolicy = null;
        final int delay = Math.max(Session.MIN_DELAY, Math.min(Session.MAX_DELAY, cfg.retryDelay));
        if (cfg.retryCount <= 0)
        {
            retryPolicy = new RetryForever(delay);
            this.log.debug("Clustering retry is infinite");
        }
        else
        {
            this.log.debug("Clustering retry count is {}", cfg.retryCount);
            retryPolicy = new ExponentialBackoffRetry(delay, cfg.retryCount);
        }

        this.log.debug("Clustering retry policy is {}, with a delay of {}", retryPolicy.getClass().getSimpleName(),
                cfg.retryDelay);

        this.basePath = (Tools.isEmpty(cfg.basePath) //
                ? Session.ROOT_PATH //
                : cfg.basePath //
        );

        this.log.info("ZooKeeper connection string: [{}]", cfg.connect);

        retryPolicy = Tools.ifNull(retryPolicy, () -> new RetryForever(1000));

        this.log.trace("Initializing the Curator client");
        this.client = CuratorFrameworkFactory.newClient(cfg.connect, sessionTimeout, connectionTimeout, retryPolicy);
        this.log.info("Starting the Curator client");
        this.client.start();
        if (cfg.waitForConnection)
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

    public void addSelector(int key, LeaderSelector selector)
    {
        if (selector != null)
        {
            this.selectors.put(key, selector);
        }
    }

    public LeaderSelector removeSelector(int key)
    {
        return this.selectors.remove(key);
    }

    private synchronized void cleanup()
    {
        for (Integer i : this.selectors.keySet())
        {
            LeaderSelector l = this.selectors.get(i);
            this.log.warn("Emergency cleanup: closing out leadership selector # {}", i);
            try
            {
                l.close();
            }
            catch (Exception e)
            {
                this.log.warn("Exception caught during cleanup of leadership selector # {}", i, e);
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
        private static final Integer DEF_RETRY_DELAY = 1000;
        private static final Integer DEF_RETRY_COUNT = 0;

        private String connect = null;
        private int sessionTimeout = 0;
        private int connectionTimeout = 0;
        private String basePath = null;

        private int retryDelay = Builder.DEF_RETRY_DELAY;
        private int retryCount = Builder.DEF_RETRY_COUNT;

        private boolean waitForConnection = true;

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
            if (this.sessionTimeout < 0)
            {
                this.sessionTimeout = 0;
            }
            return this.sessionTimeout;
        }

        public Builder sessionTimeout(int sessionTimeout)
        {
            this.sessionTimeout = Math.max(0, sessionTimeout);
            return this;
        }

        public int connectionTimeout()
        {
            if (this.connectionTimeout < 0)
            {
                this.connectionTimeout = 0;
            }
            return this.connectionTimeout;
        }

        public Builder connectionTimeout(int connectionTimeout)
        {
            this.connectionTimeout = Math.max(0, connectionTimeout);
            return this;
        }

        public String basePath()
        {
            return this.basePath;
        }

        public Builder basePath(String basePath)
        {
            this.basePath = basePath;
            return this;
        }

        public int retryCount()
        {
            return this.retryCount;
        }

        public Builder retryCount(int retryCount)
        {
            if (retryCount <= 0)
            {
                retryCount = Builder.DEF_RETRY_COUNT;
            }
            this.retryCount = retryCount;
            return this;
        }

        public int retryDelay()
        {
            return this.retryDelay;
        }

        public Builder retryDelay(int retryDelay)
        {
            if (retryDelay <= 0)
            {
                retryDelay = Builder.DEF_RETRY_DELAY;
            }
            this.retryDelay = retryDelay;
            return this;
        }

        public boolean waitForConnection()
        {
            return this.waitForConnection;
        }

        public Builder waitForConnection(boolean wait)
        {
            this.waitForConnection = wait;
            return this;
        }

        public Session build() throws InterruptedException
        {
            return new Session(this);
        }
    }
}
