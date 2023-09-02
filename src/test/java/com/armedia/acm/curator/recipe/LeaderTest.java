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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;

public class LeaderTest
{
    private static TestingServer SERVER = null;

    private final int acceptableWaitSecs = 15;

    @BeforeAll
    public static void beforeAll() throws Exception
    {
        if (LeaderTest.SERVER == null)
        {
            LeaderTest.SERVER = new TestingServer();
        }
    }

    @AfterAll
    public static void afterAll() throws Exception
    {
        if (LeaderTest.SERVER != null)
        {
            try
            {
                LeaderTest.SERVER.close();
            }
            finally
            {
                LeaderTest.SERVER = null;
            }
        }
    }

    final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testConstructor() throws Exception
    {
        new Leader(null);
        new Leader(null, null);

        try (Session session = new Session.Builder().build())
        {
            new Leader(session);
            new Leader(session, null);
        }
    }

    @Test
    public void testAcquire() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            Leader l = new Leader(session);
            try (AutoCloseable c = l.awaitLeadership())
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(LeaderTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            Leader l = new Leader(session);
            try (AutoCloseable c = l.awaitLeadership())
            {
                // Lock was acquired properly ...
            }
        }

        // Try multithreading to ensure we really are Leadering
        final CyclicBarrier barrier = new CyclicBarrier(4);
        Map<String, Thread> threads = new LinkedHashMap<>();
        final Map<String, AtomicLong> counters = new LinkedHashMap<>();
        final Map<String, Throwable> exceptions = new LinkedHashMap<>();
        final String name = UUID.randomUUID().toString();

        for (int i = 0; i < 3; i++)
        {
            final String key = String.format("leader-%02d", i);
            counters.put(key, new AtomicLong(0));
            threads.put(key, new Thread(key)
            {

                private Map<String, Long> getCounters()
                {
                    Map<String, Long> m = new LinkedHashMap<>();
                    for (String k : counters.keySet())
                    {
                        // Don't mess with ours!
                        if (key.equals(k))
                        {
                            continue;
                        }

                        m.put(k, counters.get(k).get());
                    }
                    return Collections.unmodifiableMap(m);
                }

                private void runTest() throws Exception
                {
                    barrier.await(LeaderTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                    try (Session session = new Session.Builder().connect(LeaderTest.SERVER.getConnectString()).build())
                    {
                        final Leader l = new Leader(session, name);
                        final AtomicLong counter = counters.get(key);
                        // We will attempt to acquire the Leader 5 times in a tight loop.
                        // We will check the other threads' counters. The must not move
                        // while we do our thing. We add a little bit of a pause (20ms)
                        // and a Thread.yield() in there to make sure the other threads
                        // have a chance to run
                        for (int i = 0; i < 5; i++)
                        {
                            try (AutoCloseable c = l.awaitLeadership())
                            {
                                Map<String, Long> startCounters = getCounters();
                                // Lock was acquired properly ... what are the other counters at?
                                for (int j = 0; j < 10; j++)
                                {
                                    counter.incrementAndGet();
                                    Thread.sleep(20);
                                    Thread.yield();

                                    Assertions.assertEquals(startCounters, getCounters());
                                }
                            }
                        }
                    }
                }

                @Override
                public void run()
                {
                    try
                    {
                        runTest();
                        barrier.await(LeaderTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                    }
                    catch (Exception e)
                    {
                        exceptions.put(key, e);
                    }
                }
            });
        }

        // Start the threads...
        for (Thread t : threads.values())
        {
            t.start();
        }
        // This will cause them all to synchronize
        barrier.await(this.acceptableWaitSecs, TimeUnit.SECONDS);

        // Now wait for them to complete
        barrier.await(this.acceptableWaitSecs, TimeUnit.SECONDS);

        // Start them, and wait for them to complete
        if (!exceptions.isEmpty())
        {
            for (String k : exceptions.keySet())
            {
                this.log.error("Thread {} failed", k, exceptions.get(k));
            }
            Assertions.fail("Multithreaded Leader test failed");
        }
    }

    @Test
    public void testAcquireDuration() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            Leader l = new Leader(session);
            try (AutoCloseable c = l.awaitLeadership(Duration.of(10, ChronoUnit.SECONDS)))
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(LeaderTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            Leader l = new Leader(session);
            try (AutoCloseable c = l.awaitLeadership(Duration.of(10, ChronoUnit.SECONDS)))
            {
                // Lock was acquired properly ...
            }
        }

        // Try multithreading to ensure we really are Leadering
        final CyclicBarrier startBarrier = new CyclicBarrier(2);
        final CyclicBarrier endBarrier = new CyclicBarrier(3);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final String name = "leader-timeout";

        // This thread is gonna hog the lock for 10 seconds
        new Thread("leader-hog")
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(LeaderTest.SERVER.getConnectString()).build())
                    {
                        final Leader l = new Leader(session, name);
                        try (AutoCloseable c = l.awaitLeadership())
                        {
                            // Signal that we're ready to keep going
                            startBarrier.await(LeaderTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                            // We're going to hold it until we're told to let it go
                            endBarrier.await(LeaderTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                        }
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    LeaderTest.this.log.error("Hog thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Wait until the lock is acquired and held by the hog
        startBarrier.await(this.acceptableWaitSecs, TimeUnit.SECONDS);

        // This thread is going to await the lock for at most 2 seconds (for a few times).
        new Thread("leader-beg")
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(LeaderTest.SERVER.getConnectString()).build())
                    {
                        final Leader l = new Leader(session, name);
                        startBarrier.await(LeaderTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                        Duration d = Duration.of(2, ChronoUnit.SECONDS);
                        for (int i = 0; i < 3; i++)
                        {
                            Instant start = Instant.now();
                            try (AutoCloseable c = l.awaitLeadership(d))
                            {
                                Assertions.fail("The beggar acquired a lock that isn't available");
                            }
                            catch (TimeoutException e)
                            {
                                // All is well! This is what we expected!! Did we wait (approximately)
                                // the requisite duration?
                                Duration wait = Duration.between(start, Instant.now());
                                Assertions.assertTrue(wait.compareTo(d) >= 0, () -> "Waited " + wait + " when we expected to wait " + d);
                            }
                        }
                    }
                    finally
                    {
                        endBarrier.await(LeaderTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    LeaderTest.this.log.error("Beg thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Now unleash the beggar
        startBarrier.await(this.acceptableWaitSecs, TimeUnit.SECONDS);

        // Wait for everyone
        endBarrier.await(this.acceptableWaitSecs, TimeUnit.SECONDS);

        Assertions.assertFalse(failed.get(), "An exception was raised by one of the threads");
    }
}
