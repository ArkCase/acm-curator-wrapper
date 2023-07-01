package com.armedia.acm.curator.wrapper.module;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.conf.LeaderCfg;
import com.armedia.acm.curator.wrapper.tools.Tools;

public class Leader
{
    private static final AtomicInteger selectorCounter = new AtomicInteger(0);
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String leaderName;
    private final String leaderPath;
    private final Session connection;

    public Leader(LeaderCfg cfg, Session connection)
    {
        this.connection = connection;
        String baseLeaderPath = String.format("%s/leader", connection.getBasePath());
        this.leaderName = cfg.getName();
        this.leaderPath = (Tools.isEmpty(this.leaderName) //
                ? baseLeaderPath //
                : String.format("%s/%s", baseLeaderPath, this.leaderName) //
        );
    }

    public String getLeaderName()
    {
        return this.leaderName;
    }

    public String getLeaderPath()
    {
        return this.leaderPath;
    }

    public Session getConnection()
    {
        return this.connection;
    }

    public AutoCloseable acquire() throws InterruptedException
    {
        this.connection.assertEnabled();
        this.log.debug("Leadership node path: [{}]", this.leaderPath);

        final CountDownLatch awaitLeadership = new CountDownLatch(1);
        final CountDownLatch awaitCompletion = new CountDownLatch(1);
        final int selectorKey = Leader.selectorCounter.getAndIncrement();
        final LeaderSelectorListener listener = new LeaderSelectorListenerAdapter()
        {
            @Override
            public void takeLeadership(CuratorFramework client)
            {
                Leader.this.log.info("Assuming Leadership (# {})", selectorKey);
                awaitLeadership.countDown();
                try
                {
                    Leader.this.log.info("Signalled the start of the execution, awaiting completion (# {})", selectorKey);
                    awaitCompletion.await();
                    Leader.this.log.info("Execution completed; the latch returned normally (# {})", selectorKey);
                }
                catch (InterruptedException e)
                {
                    Thread.interrupted();
                    Leader.this.log.error("Interrupted waiting for execution to complete (# {})", selectorKey);
                }
                finally
                {
                    Leader.this.log.trace("Relinquishing leadership (# {})", selectorKey);
                }
            }
        };

        this.log.trace("Creating a new leadership selector");
        final LeaderSelector selector = new LeaderSelector(this.connection.getClient(), this.leaderPath, listener);
        this.log.trace("Starting the leadership selector (# {})", selectorKey);
        selector.start();
        this.connection.addSelector(selectorKey, selector);

        // We will block in this await() invocation until leadership is acquired.
        this.log.trace("Waiting for leadership to be attained (# {})", selectorKey);
        try
        {
            awaitLeadership.await();
        }
        catch (final InterruptedException e)
        {
            this.log.warn("Leadership wait interrupted (# {})!", selectorKey, e);
            Thread.interrupted();
            throw e;
        }
        finally
        {
            this.log.trace("The leadership wait is finished (# {})", selectorKey);
        }

        return () -> {
            this.log.info("Processing completed, relinquishing leadership (selector # {})", selectorKey);
            awaitCompletion.countDown();
            try
            {
                selector.close();
            }
            catch (Exception e)
            {
                this.log.warn("Exception caught closing down leadership selector # {}", selectorKey, e);
            }
            finally
            {
                this.connection.removeSelector(selectorKey);
            }
        };
    }
}