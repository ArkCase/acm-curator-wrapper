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

public class ReadWriteLockTest
{
    private static TestingServer SERVER = null;

    @BeforeAll
    public static void beforeAll() throws Exception
    {
        if (ReadWriteLockTest.SERVER == null)
        {
            ReadWriteLockTest.SERVER = new TestingServer();
        }
    }

    @AfterAll
    public static void afterAll() throws Exception
    {
        if (ReadWriteLockTest.SERVER != null)
        {
            try
            {
                ReadWriteLockTest.SERVER.close();
            }
            finally
            {
                ReadWriteLockTest.SERVER = null;
            }
        }
    }

    final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testConstructor() throws Exception
    {
        try
        {
            new ReadWriteLock(null);
            Assertions.fail("Did not fail with a null session");
        }
        catch (NullPointerException e)
        {
            // All is well
        }

        try
        {
            new ReadWriteLock(null, null);
            Assertions.fail("Did not fail with a null session");
        }
        catch (NullPointerException e)
        {
            // All is well
        }

        try (Session session = new Session.Builder().build())
        {
            new ReadWriteLock(session);
            new ReadWriteLock(session, null);
        }
    }

    @Test
    public void testAcquireWrite() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.write())
            {
                Assertions.fail("Did not fail with a disabled session");
            }
            catch (IllegalStateException e)
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.write())
            {
                // Lock was acquired properly ...
            }
        }

        // Try multithreading to ensure we really are ReadWriteLocking
        final CyclicBarrier barrier = new CyclicBarrier(4);
        Map<String, Thread> threads = new LinkedHashMap<>();
        final Map<String, AtomicLong> counters = new LinkedHashMap<>();
        final Map<String, Throwable> exceptions = new LinkedHashMap<>();
        final String name = UUID.randomUUID().toString();

        for (int i = 0; i < 3; i++)
        {
            final String key = String.format("%02d", i);
            counters.put(key, new AtomicLong(0));
            threads.put(key, new Thread()
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
                    barrier.await();
                    try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
                    {
                        final ReadWriteLock rw = new ReadWriteLock(session, name);
                        final AtomicLong counter = counters.get(key);
                        // We will attempt to acquire the ReadWriteLock 5 times in a tight loop.
                        // We will check the other threads' counters. The must not move
                        // while we do our thing. We add a little bit of a pause (20ms)
                        // and a Thread.yield() in there to make sure the other threads
                        // have a chance to run
                        for (int i = 0; i < 5; i++)
                        {
                            try (AutoCloseable c = rw.write())
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
                        barrier.await();
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
        barrier.await();

        // Now wait for them to complete
        barrier.await();

        // Start them, and wait for them to complete
        if (!exceptions.isEmpty())
        {
            for (String k : exceptions.keySet())
            {
                this.log.error("Thread {} failed", k, exceptions.get(k));
            }
            Assertions.fail("Multithreaded ReadWriteLock test failed");
        }
    }

    @Test
    public void testAcquireWriteDuration() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.write(Duration.of(10, ChronoUnit.SECONDS)))
            {
                Assertions.fail("Did not fail with a disabled session");
            }
            catch (IllegalStateException e)
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.write(Duration.of(10, ChronoUnit.SECONDS)))
            {
                // Lock was acquired properly ...
            }
        }

        // Try multithreading to ensure we really are ReadWriteLocking
        final CyclicBarrier startBarrier = new CyclicBarrier(2);
        final CyclicBarrier endBarrier = new CyclicBarrier(3);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final String name = UUID.randomUUID().toString();

        // This thread is gonna hog the lock for 10 seconds
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
                    {
                        final ReadWriteLock rw = new ReadWriteLock(session, name);
                        try (AutoCloseable c = rw.write())
                        {
                            // Signal that we're ready to keep going
                            startBarrier.await();
                            // We're going to hold it until we're told to let it go
                            endBarrier.await();
                        }
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    ReadWriteLockTest.this.log.error("Hog thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Wait until the lock is acquired and held by the hog
        startBarrier.await();

        // This thread is going to await the lock for at most 2 seconds (for a few times).
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
                    {
                        final ReadWriteLock rw = new ReadWriteLock(session, name);
                        startBarrier.await();
                        Duration d = Duration.of(2, ChronoUnit.SECONDS);
                        for (int i = 0; i < 3; i++)
                        {
                            Instant start = Instant.now();
                            try (AutoCloseable c = rw.write(d))
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
                        endBarrier.await();
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    ReadWriteLockTest.this.log.error("Beg thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Now unleash the beggar
        startBarrier.await();

        // Wait for everyone
        endBarrier.await();

        Assertions.assertFalse(failed.get(), "An exception was raised by one of the threads");
    }

    @Test
    public void testAcquireRead() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.read())
            {
                Assertions.fail("Did not fail with a disabled session");
            }
            catch (IllegalStateException e)
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.read())
            {
                // Lock was acquired properly ...
            }
        }

        // Try multithreading to ensure we really are ReadWriteLocking
        final CyclicBarrier barrier = new CyclicBarrier(4);
        Map<String, Thread> threads = new LinkedHashMap<>();
        final Map<String, Throwable> exceptions = new LinkedHashMap<>();
        final String name = UUID.randomUUID().toString();

        for (int i = 0; i < 3; i++)
        {
            final String key = String.format("%02d", i);
            threads.put(key, new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        barrier.await();
                        try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
                        {
                            final ReadWriteLock rw = new ReadWriteLock(session, name);
                            // We will attempt to acquire the read lock 5 times in a tight loop.
                            // We will check the other threads' counters. The must also move
                            // while we do our thing. We add a little bit of a pause (20ms)
                            // and a Thread.yield() in there to make sure the other threads
                            // have a chance to run
                            for (int i = 0; i < 5; i++)
                            {
                                try (AutoCloseable c = rw.read())
                                {
                                    barrier.await(5, TimeUnit.SECONDS);
                                }
                            }
                        }
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
        barrier.await();

        // Now wait for them to acquire the lock
        barrier.await(10, TimeUnit.SECONDS);

        // Start them, and wait for them to complete
        if (!exceptions.isEmpty())
        {
            for (String k : exceptions.keySet())
            {
                this.log.error("Thread {} failed", k, exceptions.get(k));
            }
            Assertions.fail("Multithreaded ReadWriteLock test failed");
        }
    }

    @Test
    public void testAcquireReadDuration() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.read(Duration.of(10, ChronoUnit.SECONDS)))
            {
                Assertions.fail("Did not fail with a disabled session");
            }
            catch (IllegalStateException e)
            {
                // All is well
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (AutoCloseable c = rw.read(Duration.of(10, ChronoUnit.SECONDS)))
            {
                // Lock was acquired properly ...
            }
        }

        // Try multithreading to ensure we really are ReadWriteLocking
        final CyclicBarrier startBarrier = new CyclicBarrier(2);
        final CyclicBarrier endBarrier = new CyclicBarrier(3);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final String name = UUID.randomUUID().toString();

        // This thread is gonna hog the lock for 10 seconds
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
                    {
                        final ReadWriteLock rw = new ReadWriteLock(session, name);
                        try (AutoCloseable c = rw.write())
                        {
                            // Signal that we're ready to keep going
                            startBarrier.await();
                            // We're going to hold it until we're told to let it go
                            endBarrier.await();
                        }
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    ReadWriteLockTest.this.log.error("Hog thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Wait until the lock is acquired and held by the hog
        startBarrier.await();

        // This thread is going to await the lock for at most 2 seconds (for a few times).
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
                    {
                        final ReadWriteLock rw = new ReadWriteLock(session, name);
                        startBarrier.await();
                        Duration d = Duration.of(2, ChronoUnit.SECONDS);
                        for (int i = 0; i < 3; i++)
                        {
                            Instant start = Instant.now();
                            try (AutoCloseable c = rw.read(d))
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
                        endBarrier.await();
                    }
                }
                catch (Exception e)
                {
                    failed.set(true);
                    ReadWriteLockTest.this.log.error("Beg thread caught an exception, the test is invalid", e);
                }
            }
        }.start();

        // Now unleash the beggar
        startBarrier.await();

        // Wait for everyone
        endBarrier.await();

        Assertions.assertFalse(failed.get(), "An exception was raised by one of the threads");
    }

    @Test
    public void testUpgrade() throws Exception
    {
        // Simple happy path
        try (Session session = new Session.Builder().connect(ReadWriteLockTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            ReadWriteLock rw = new ReadWriteLock(session);
            try (ReadWriteLock.Read r = rw.read(Duration.of(10, ChronoUnit.SECONDS)))
            {
                try (ReadWriteLock.Write w = r.upgrade(Duration.of(100, ChronoUnit.MILLIS)))
                {
                    // Lock was upgraded ...
                }
            }
        }
    }
}