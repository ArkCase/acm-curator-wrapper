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
package com.armedia.acm.curator.module;

import java.time.Duration;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class Leader extends Recipe
{
    private static final AtomicInteger selectorCounter = new AtomicInteger(0);
    private static final Duration WAIT_FOREVER = Duration.ZERO;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String leaderName;
    private final String leaderPath;

    private Leader(Session session, String name)
    {
        super(session);
        String baseLeaderPath = String.format("%s/leader", session.getBasePath());
        this.leaderName = name;
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
        return this.session;
    }

    private AutoCloseable execute(BooleanSupplier job, Duration maxWait) throws InterruptedException, TimeoutException
    {
        this.session.assertEnabled();
        this.log.debug("Leadership node path: [{}]", this.leaderPath);
        if (maxWait == null)
        {
            maxWait = Leader.WAIT_FOREVER;
        }

        final CyclicBarrier awaitLeadership = new CyclicBarrier(2);
        final CyclicBarrier awaitCompletion = new CyclicBarrier(2);
        final int selectorKey = Leader.selectorCounter.getAndIncrement();
        final LeaderSelectorListener listener = new LeaderSelectorListenerAdapter()
        {
            @Override
            public void takeLeadership(CuratorFramework client)
            {
                try
                {
                    Leader.this.log.info("Assuming Leadership (# {})", selectorKey);
                    awaitLeadership.await();
                    Leader.this.log.info("Signalled the start of the execution, awaiting completion (# {})", selectorKey);
                    awaitCompletion.await();
                    Leader.this.log.info("Execution completed; the latch returned normally (# {})", selectorKey);
                }
                catch (BrokenBarrierException e)
                {
                    Leader.this.log.error("Broken barrier during leadership processing (# {})", selectorKey, e);
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
        final LeaderSelector selector = new LeaderSelector(this.session.getClient(), this.leaderPath, listener);
        this.log.trace("Starting the leadership selector (# {})", selectorKey);
        if (job != null)
        {
            selector.autoRequeue();
        }
        selector.start();
        this.session.addSelector(selectorKey, selector);

        AutoCloseable close = () -> {
            this.log.info("Processing completed, relinquishing leadership (selector # {})", selectorKey);
            awaitCompletion.await();
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
                this.session.removeSelector(selectorKey);
            }
        };

        // We will block in this await() invocation until leadership is acquired.
        while (true)
        {
            this.log.trace("Waiting for leadership to be attained (# {})", selectorKey);
            try
            {
                if (!maxWait.isNegative() && !maxWait.isZero())
                {
                    awaitLeadership.await(maxWait.toMillis(), TimeUnit.MILLISECONDS);
                }
                else
                {
                    awaitLeadership.await();
                }
            }
            catch (final InterruptedException e)
            {
                this.log.warn("Leadership wait interrupted (# {})!", selectorKey, e);
                Thread.interrupted();
                throw e;
            }
            catch (BrokenBarrierException e)
            {
                throw new RuntimeException(String.format("Barrier broken while awaiting leadership (# %d)", selectorKey), e);
            }
            finally
            {
                this.log.trace("The leadership wait is finished (# {})", selectorKey);
            }

            if (job == null)
            {
                return close;
            }

            boolean ret = false;
            try
            {
                ret = job.getAsBoolean();
                this.log.info("Processing completed, relinquishing leadership (selector # {})", selectorKey);
                try
                {
                    awaitCompletion.await();
                }
                catch (final InterruptedException e)
                {
                    this.log.warn("Interrupted while relinquishing leadership (# {})!", selectorKey, e);
                    Thread.interrupted();
                    throw e;
                }
                catch (BrokenBarrierException e)
                {
                    throw new RuntimeException(String.format("Barrier broken while relinquishing leadership (# %d)", selectorKey), e);
                }
            }
            finally
            {
                if (selector.hasLeadership())
                {
                    selector.interruptLeadership();
                }
            }

            if (ret == false)
            {
                Tools.closeQuietly(close);
                return null;
            }
        }
    }

    private RuntimeException unexpectedTimeout(TimeoutException e)
    {
        return new RuntimeException("Timed out while waiting forever ... ?!?!", e);
    }

    public <T> void whileLeader(Supplier<? extends T> job, Predicate<? super T> condition) throws InterruptedException, TimeoutException
    {
        try
        {
            whileLeader(job, condition, null);
        }
        catch (TimeoutException e)
        {
            throw unexpectedTimeout(e);
        }
    }

    /**
     * <p>
     * This is a more generic version of {@link #whileLeader(BooleanSupplier)} that supports any object type and any
     * predicate. As before, if the <code>job</code> is <code>null</code>, then this method will do nothing and return
     * immediately. If the <code>condition</code> is <code>null</code>, the <code>job</code> will run only once.
     * </p>
     *
     * @param <T>
     * @param job
     * @param condition
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public <T> void whileLeader(Supplier<? extends T> job, Predicate<? super T> condition, Duration maxWait)
            throws InterruptedException, TimeoutException
    {
        if (job != null)
        {
            final Predicate<? super T> c = (condition != null ? condition : (t) -> false);
            whileLeader(() -> c.test(job.get()), maxWait);
        }
    }

    public void whileLeader(Runnable job) throws InterruptedException
    {
        try
        {
            whileLeader(job, null);
        }
        catch (TimeoutException e)
        {
            throw unexpectedTimeout(e);
        }
    }

    /**
     * <p>
     * Execute the given task each time leadership is obtained. Leadership will be relinquished when the task completes,
     * and the object will re-queue itself to the leadership queue. If <code>job</code> is null, this method returns
     * immediately without doing anything. In normal scenarios, this method will never return.
     * </p>
     *
     * @param job
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void whileLeader(Runnable job, Duration maxWait) throws InterruptedException, TimeoutException
    {
        if (job != null)
        {
            execute(() -> {
                job.run();
                return true;
            }, maxWait);
        }
    }

    public void whileLeader(BooleanSupplier job) throws InterruptedException
    {
        try
        {
            whileLeader(job, null);
        }
        catch (TimeoutException e)
        {
            throw unexpectedTimeout(e);
        }
    }

    /**
     * <p>
     * Execute the given task when leadership is obtained, and as long as the given <code>job</code> returns true. When
     * the job returns <code>false</code>, the loop will be broken and this method will return. If <code>job</code> is
     * null, this method returns immediately without doing anything.
     * </p>
     *
     * @param job
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void whileLeader(BooleanSupplier job, Duration maxWait) throws InterruptedException, TimeoutException
    {
        if (job != null)
        {
            execute(job, maxWait);
        }
    }

    public AutoCloseable awaitLeadership() throws InterruptedException
    {
        try
        {
            return awaitLeadership(null);
        }
        catch (TimeoutException e)
        {
            throw unexpectedTimeout(e);
        }
    }

    /**
     * <p>
     * This method will block until leadership is attained, the given wait expires (if <code>null</code> or negative, it
     * will wait forever), or the thread is interrupted.
     * </p>
     *
     * @return an {@link AutoCloseable} which can be used to relinquish leadership when the work is done.
     * @throws InterruptedException
     */
    public AutoCloseable awaitLeadership(Duration maxWait) throws InterruptedException, TimeoutException
    {
        return execute(null, maxWait);
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

        public Leader build(Session session)
        {
            return new Leader(session, this.name);
        }
    }
}
