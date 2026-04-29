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
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
    protected static final Pattern HOSTPORT_PARSER = Pattern
            .compile("^((?:[a-z0-9][-a-z0-9]*)?[a-z0-9](?:[.](?:[a-z0-9][-a-z0-9]*)?[a-z0-9])*)(?::([1-9][0-9]*))?$",
                    Pattern.CASE_INSENSITIVE);

    public static final int MIN_SESSION_TIMEOUT = 1000;
    public static final int DEFAULT_SESSION_TIMEOUT = 15000;

    public static final int MIN_CONNECTION_TIMEOUT = 100;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public static final int DEFAULT_RETRY_COUNT = 0;
    public static final int MIN_RETRY_COUNT = 0;

    public static final int DEFAULT_RETRY_DELAY = 1000;
    public static final int MIN_RETRY_DELAY = 100;
    public static final int MAX_RETRY_DELAY = 60000;

    public static final boolean DEFAULT_WAIT_FOR_CONNECTION = true;

    protected static final String NULL_CLEANUP_KEY = "<n/a>";

    protected static final boolean validateHostPort(String hostport)
    {
        if (StringUtils.isEmpty(hostport))
        {
            return true;
        }

        return Session.HOSTPORT_PARSER.matcher(hostport).matches();
    }

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
    private final AtomicInteger cleanupKeys = new AtomicInteger();
    private final Map<Integer, AutoCloseable> cleanups = Collections.synchronizedMap(new TreeMap<>());

    private Session(Builder builder)
            throws InterruptedException
    {
        if (Tools.isEmpty(builder.connect))
        {
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

        this.log.info("ZooKeeper connection string: [{}]", builder.connect);

        retryPolicy = Tools.ifNull(retryPolicy, () -> new RetryForever(1000));

        this.log.trace("Initializing the Curator client");
        if (StringUtils.isNotBlank(builder.chroot))
        {
            this.log.trace("Ensuring the existence of the chroot path [{}]", builder.chroot);
            try (CuratorFramework chrootClient = CuratorFrameworkFactory.newClient(builder.hostConnect, sessionTimeout, connectionTimeout,
                    retryPolicy))
            {
                chrootClient.start();
                chrootClient.blockUntilConnected();
                chrootClient.createContainers(builder.chroot);
                this.log.info("Created the ZK chroot path at [{}]", builder.chroot);
            }
            catch (Exception e)
            {
                throw new RuntimeException(String.format("Failed to create the ZK chroot path [%s]", builder.chroot), e);
            }
        }
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

    public synchronized CuratorFramework getClient()
    {
        return this.client;
    }

    public synchronized boolean isEnabled()
    {
        return (this.client != null);
    }

    public Object addCleanup(AutoCloseable closeable)
    {
        if ((closeable != null) && isEnabled())
        {
            int key = this.cleanupKeys.getAndIncrement();
            this.cleanups.put(key, closeable);
            return key;
        }
        return Session.NULL_CLEANUP_KEY;
    }

    public AutoCloseable removeCleanup(Object key)
    {
        return this.cleanups.remove(key);
    }

    private synchronized void cleanup()
    {
        // Always unlock in reverse order to acquisition...
        for (Object k : new TreeSet<>(this.cleanups.keySet()).descendingSet())
        {
            AutoCloseable c = this.cleanups.get(k);
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
        private String instanceId = null;
        private String hostConnect = null;
        private String chroot = null;
        private String connect = null;

        private Integer sessionTimeout = Session.DEFAULT_SESSION_TIMEOUT;
        private Integer connectionTimeout = Session.DEFAULT_CONNECTION_TIMEOUT;

        private Integer retryDelay = Session.DEFAULT_RETRY_DELAY;
        private Integer retryCount = Session.DEFAULT_RETRY_COUNT;
        private Boolean waitForConnection = Session.DEFAULT_WAIT_FOR_CONNECTION;

        private String computeString(String name, String val)
        {
            if (StringUtils.isNotBlank(val))
            {
                return val;
            }

            val = Tools.getParameter(name);
            if (StringUtils.isNotBlank(val))
            {
                return val;
            }

            return null;
        }

        private int computeInteger(String name, Integer val, Integer min, Integer def)
        {
            if (val == null)
            {
                String str = Tools.getParameter(name);
                if (StringUtils.isBlank(str))
                {
                    return def;
                }

                try
                {
                    val = Integer.valueOf(str);
                }
                catch (NumberFormatException e)
                {
                    return def;
                }
            }

            if (val <= 0)
            {
                return def;
            }

            return Math.max(min, val);
        }

        private boolean computeBoolean(String name, Boolean val, Boolean def)
        {
            if (val == null)
            {
                String str = Tools.getParameter(name);
                if (StringUtils.isBlank(str))
                {
                    return def;
                }
                val = BooleanUtils.toBooleanObject(str);
            }
            return val;
        }

        private void computeConnectStrings(String connect, String instanceId)
        {
            // Sanitize the instance ID
            instanceId = StringUtils.defaultIfBlank(instanceId, StringUtils.EMPTY);
            if (instanceId.indexOf('/') >= 0)
            {
                throw new RuntimeException(String.format("The ArkCase instance ID may not contain any slashes (/): [%s]", instanceId));
            }
            this.instanceId = instanceId;

            connect = String.format("%s/%s", StringUtils.defaultIfBlank(connect, StringUtils.EMPTY), instanceId);

            // If we have a connect string, do we have a chroot?
            int index = connect.indexOf('/');
            this.hostConnect = connect.substring(0, index);
            if (StringUtils.isNotBlank(this.hostConnect))
            {
                // Validate that it's a CSV list of host:port pairs, and sanitize
                // The port is optional and will be defaulted to 2181. It may have
                // chroot info at the end, so account for that
                for (String s : this.hostConnect.split(","))
                {
                    if (!Session.validateHostPort(s.trim()))
                    {
                        throw new IllegalArgumentException(
                                String.format("Invalid host:port value [%s] in connection string [%s]", s, this.hostConnect));
                    }
                }
            }

            // Sanitize the chroot data
            String chroot = connect.substring(index)
                    // Consolidate multiple slashes
                    .replaceAll("/+", "/")
                    // Remove leading slashes
                    .replaceAll("^/", "")
                    // Remove trailing slashes
                    .replaceAll("/$", "")
                    // Re-add a single leading slash
                    .replaceAll("^", "/");

            // If it's a single slash, use an empty string
            if ("/".equals(chroot))
            {
                chroot = StringUtils.EMPTY;
            }
            this.chroot = chroot;
            this.connect = String.format("%s%s", this.hostConnect, this.chroot);
        }

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

        public Builder configure()
        {
            computeConnectStrings(computeString("zk.host", this.connect), computeString("arkcase.instance.id", this.instanceId));
            this.sessionTimeout = computeInteger("zk.session.timeout", this.sessionTimeout, Session.DEFAULT_SESSION_TIMEOUT,
                    Session.MIN_SESSION_TIMEOUT);
            this.connectionTimeout = computeInteger("zk.connection.timeout", this.connectionTimeout, Session.DEFAULT_CONNECTION_TIMEOUT,
                    Session.MIN_CONNECTION_TIMEOUT);
            this.retryDelay = computeInteger("zk.retry.delay", this.retryDelay, Session.DEFAULT_RETRY_DELAY,
                    Session.MIN_CONNECTION_TIMEOUT);
            this.retryCount = computeInteger("zk.retry.count", this.retryCount, Session.DEFAULT_RETRY_COUNT, Session.MIN_RETRY_COUNT);
            this.waitForConnection = computeBoolean("zk.connection.wait", this.waitForConnection, Session.DEFAULT_WAIT_FOR_CONNECTION);
            return this;
        }

        public Session build() throws InterruptedException
        {
            configure();
            return new Session(this);
        }
    }
}
