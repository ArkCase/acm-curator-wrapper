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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Version;

public class InitializationGateTest
{
    private static TestingServer SERVER = null;

    private final int acceptableWaitSecs = 15;

    @BeforeAll
    public static void beforeAll() throws Exception
    {
        if (InitializationGateTest.SERVER == null)
        {
            InitializationGateTest.SERVER = new TestingServer();
        }
    }

    @AfterAll
    public static void afterAll() throws Exception
    {
        if (InitializationGateTest.SERVER != null)
        {
            try
            {
                InitializationGateTest.SERVER.close();
            }
            finally
            {
                InitializationGateTest.SERVER = null;
            }
        }
    }

    final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testConstructor() throws Exception
    {
        try
        {
            new InitializationGate(null);
            Assertions.fail("Did not fail with a null session");
        }
        catch (NullPointerException e)
        {
            // All is well
        }

        try
        {
            new InitializationGate(null, null);
            Assertions.fail("Did not fail with a null session");
        }
        catch (NullPointerException e)
        {
            // All is well
        }

        try (Session session = new Session.Builder().build())
        {
            new InitializationGate(session, null);
        }
    }

    @Test
    public void testInitialize() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            InitializationGate ig = new InitializationGate(session);
            final AtomicBoolean initialized = new AtomicBoolean();
            final InitializationGate.Initializer initializer = new InitializationGate.Initializer(Version.parse("1.0.0"))
            {
                @Override
                public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                {
                    initialized.set(true);
                    return null;
                }
            };

            try
            {
                ig.initialize(initializer);
                Assertions.fail("Did not fail with a null session");
            }
            catch (IllegalStateException e)
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(InitializationGateTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            InitializationGate ig = new InitializationGate(session);
            final AtomicInteger counter = new AtomicInteger();
            InitializationGate.Initializer initializer = new InitializationGate.Initializer(Version.parse("1.0.0"))
            {
                @Override
                public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                {
                    counter.incrementAndGet();
                    return null;
                }
            };

            boolean called = ig.initialize(initializer);
            Assertions.assertTrue(called, "Failed to execute the initializer block");
            Assertions.assertEquals(1, counter.get(), "Failed to execute the initailizer block");

            called = ig.initialize(initializer);
            Assertions.assertFalse(called, "initialize() returned TRUE even after initialization was executed");
            Assertions.assertEquals(1, counter.get(), "Counter was changed even when initializer code wasn't executed");
        }

        // Try multithreading to ensure we really are Leadering
        final CyclicBarrier barrier = new CyclicBarrier(4);
        Map<String, Thread> threads = new LinkedHashMap<>();
        final Set<String> callers = Collections.synchronizedSet(new LinkedHashSet<>());
        final Map<String, Throwable> exceptions = Collections.synchronizedMap(new LinkedHashMap<>());
        final String name = UUID.randomUUID().toString();

        final Version version = Version.parse("1.0.1");
        for (int i = 0; i < 3; i++)
        {
            final String key = String.format("init-%02d", i);
            threads.put(key, new Thread(key)
            {

                private final InitializationGate.Initializer initializer = new InitializationGate.Initializer(version)
                {
                    @Override
                    public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                    {
                        callers.add(key);
                        return null;
                    }

                };

                @Override
                public void run()
                {
                    try
                    {
                        barrier.await(InitializationGateTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                        try (Session session = new Session.Builder().connect(InitializationGateTest.SERVER.getConnectString()).build())
                        {
                            final InitializationGate ig = new InitializationGate(session, name);
                            for (int i = 0; i < 5; i++)
                            {
                                ig.initialize(this.initializer);
                            }
                        }
                        barrier.await(InitializationGateTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
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
        Assertions.assertFalse(callers.isEmpty(), "No callers detected");
        Assertions.assertEquals(1, callers.size(), () -> "More than one caller detected: " + callers);
    }

    @Test
    public void testInitializeDuration() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            InitializationGate ig = new InitializationGate(session);
            final AtomicBoolean initialized = new AtomicBoolean();
            final InitializationGate.Initializer initializer = new InitializationGate.Initializer(Version.parse("1.0.0"))
            {
                @Override
                public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                {
                    initialized.set(true);
                    return null;
                }
            };

            try
            {
                ig.initialize(initializer, Duration.of(10, ChronoUnit.SECONDS));
                Assertions.fail("Did not fail with a null session");
            }
            catch (IllegalStateException e)
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(InitializationGateTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            InitializationGate ig = new InitializationGate(session);
            final AtomicInteger counter = new AtomicInteger();
            InitializationGate.Initializer initializer = new InitializationGate.Initializer(Version.parse("1.0.0"))
            {
                @Override
                public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                {
                    counter.incrementAndGet();
                    return null;
                }
            };

            boolean called = ig.initialize(initializer, Duration.of(2, ChronoUnit.SECONDS));
            Assertions.assertTrue(called, "Failed to execute the initializer block");
            Assertions.assertEquals(1, counter.get(), "Failed to execute the initailizer block");

            called = ig.initialize(initializer, Duration.of(2, ChronoUnit.SECONDS));
            Assertions.assertFalse(called, "initialize() returned TRUE even after initialization was executed");
            Assertions.assertEquals(1, counter.get(), "Counter was changed even when initializer code wasn't executed");
        }

        // Try multithreading to ensure we really are Leadering
        final CyclicBarrier startBarrier = new CyclicBarrier(2);
        final CyclicBarrier endBarrier = new CyclicBarrier(3);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final String name = "init-gate-timeout";

        // This thread is gonna hog the lock for 10 seconds
        new Thread("init-hog")
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(InitializationGateTest.SERVER.getConnectString()).build())
                    {
                        final InitializationGate ig = new InitializationGate(session, name);
                        ig.initialize(new InitializationGate.Initializer(Version.parse("1.0.0"))
                        {
                            @Override
                            public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                            {
                                // Signal that we're ready to keep going
                                startBarrier.await(InitializationGateTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                                // We're going to hold it until we're told to let it go
                                endBarrier.await(InitializationGateTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                                return null;
                            }
                        });
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    InitializationGateTest.this.log.error("Hog thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Wait until the lock is acquired and held by the hog
        startBarrier.await(this.acceptableWaitSecs, TimeUnit.SECONDS);

        // This thread is going to await the lock for at most 2 seconds (for a few times).
        new Thread("init-beg")
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(InitializationGateTest.SERVER.getConnectString()).build())
                    {
                        final InitializationGate ig = new InitializationGate(session, name);
                        startBarrier.await(InitializationGateTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                        for (int i = 0; i < 3; i++)
                        {
                            try
                            {
                                ig.initialize(new InitializationGate.Initializer(Version.parse("1.0.0"))
                                {
                                    @Override
                                    public Map<String, String> initialize(Version current, Map<String, String> extraData) throws Exception
                                    {
                                        Assertions.fail("Acquired an initializer lock that isn't available");
                                        return null;
                                    }
                                }, Duration.of(2, ChronoUnit.SECONDS));
                                Assertions.fail("Did not time out waiting for the initializer lock");
                            }
                            catch (TimeoutException e)
                            {
                                // All is well
                            }
                        }
                    }
                    finally
                    {
                        endBarrier.await(InitializationGateTest.this.acceptableWaitSecs, TimeUnit.SECONDS);
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    InitializationGateTest.this.log.error("Hog thread caught an exception, the test is invalid", e);
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
