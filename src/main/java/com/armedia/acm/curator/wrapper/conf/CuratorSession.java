package com.armedia.acm.curator.wrapper.conf;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorSession implements AutoCloseable
{
    private static final String BASE_PATH = "/arkcase";
    private final Logger log = LoggerFactory.getLogger(getClass());

    CuratorFramework client = null;
    private Thread cleanup = null;
    final String basePath;
    private final Map<Integer, LeaderSelector> selectors = Collections.synchronizedMap(new TreeMap<>());

    CuratorSession(ZookeeperCfg cfg) throws InterruptedException
    {
        if ((cfg == null) || Configuration.isEmpty(cfg.connect))
        {
            this.basePath = null;
            this.log.info("No ZooKeeper configuration");
            return;
        }

        this.basePath = (Configuration.isEmpty(cfg.basePath) //
                ? CuratorSession.BASE_PATH //
                : cfg.basePath //
        );

        this.log.info("ZooKeeper connection string: [{}]", cfg.connect);

        RetryCfg retry = (cfg.retry != null ? cfg.retry : new RetryCfg());
        RetryPolicy retryPolicy = retry.asRetryPolicy();

        this.log.debug("Clustering retry policy is {}, with a delay of {}", retryPolicy.getClass().getSimpleName(),
                retry.getDelay());
        if (retry.getCount() > 0)
        {
            this.log.debug("Clustering retry count is {}", retry.getCount());
        }

        this.log.trace("Initializing the Curator client");
        this.client = CuratorFrameworkFactory.newClient(cfg.connect, cfg.sessionTimeout, cfg.connectionTimeout, retryPolicy);
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
}