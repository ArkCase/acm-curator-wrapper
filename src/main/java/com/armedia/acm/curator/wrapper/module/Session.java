package com.armedia.acm.curator.wrapper.module;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.retry.RetryForever;
import org.apache.zookeeper.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.conf.Cfg;
import com.armedia.acm.curator.wrapper.conf.SessionCfg;
import com.armedia.acm.curator.wrapper.conf.RetryCfg;
import com.armedia.acm.curator.wrapper.tools.Tools;

public class Session implements AutoCloseable
{
    private static final int MIN_SESSION_TIMEOUT = 1;
    private static final int MIN_CONNECTION_TIMEOUT = 100;

    private static final String ROOT_PATH = "/arkcase";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private CuratorFramework client = null;
    private Thread cleanup = null;
    private final String basePath;
    private final Map<Integer, LeaderSelector> selectors = Collections.synchronizedMap(new TreeMap<>());

    public Session(Cfg configuration)
            throws InterruptedException
    {
        SessionCfg cfg = Tools.ifNull(configuration.getSession(), SessionCfg::new);

        if (Tools.isEmpty(cfg.getConnect()))
        {
            this.basePath = null;
            this.log.info("No ZooKeeper configuration");
            return;
        }

        int sessionTimeout = Math.max(Session.MIN_SESSION_TIMEOUT, cfg.getSessionTimeout());
        int connectionTimeout = Math.max(Session.MIN_CONNECTION_TIMEOUT, cfg.getConnectionTimeout());

        RetryCfg retry = Tools.ifNull(cfg.getRetry(), RetryCfg::new);
        RetryPolicy retryPolicy = retry.asRetryPolicy();

        this.log.debug("Clustering retry policy is {}, with a delay of {}", retryPolicy.getClass().getSimpleName(),
                retry.getDelay());
        if (retry.getCount() > 0)
        {
            this.log.debug("Clustering retry count is {}", retry.getCount());
        }

        this.basePath = (StringUtils.isEmpty(cfg.getBasePath()) //
                ? Session.ROOT_PATH //
                : cfg.getBasePath() //
        );

        this.log.info("ZooKeeper connection string: [{}]", cfg.getConnect());

        retryPolicy = Tools.ifNull(retryPolicy, () -> new RetryForever(1000));

        this.log.trace("Initializing the Curator client");
        this.client = CuratorFrameworkFactory.newClient(cfg.getConnect(), sessionTimeout, connectionTimeout, retryPolicy);
        this.log.info("Starting the Curator client");
        this.client.start();
        this.client.blockUntilConnected();
        this.cleanup = new Thread(this::cleanup, "ZookeeperConnection-Cleanup");
        this.cleanup.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(this.cleanup);
    }

    public String getBasePath()
    {
        return this.basePath;
    }

    synchronized CuratorFramework getClient()
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
}