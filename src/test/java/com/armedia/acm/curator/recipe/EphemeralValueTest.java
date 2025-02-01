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

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class EphemeralValueTest
{
    private static final Random RANDOM = new Random(System.nanoTime());

    private static TestingServer SERVER = null;

    @BeforeAll
    public static void beforeAll() throws Exception
    {
        if (EphemeralValueTest.SERVER == null)
        {
            EphemeralValueTest.SERVER = new TestingServer();
        }
    }

    @AfterAll
    public static void afterAll() throws Exception
    {
        if (EphemeralValueTest.SERVER != null)
        {
            try
            {
                EphemeralValueTest.SERVER.close();
            }
            finally
            {
                EphemeralValueTest.SERVER = null;
            }
        }
    }

    final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testConstructor() throws Exception
    {
        new EphemeralValue(null);
        new EphemeralValue(null, null);

        try (Session session = new Session.Builder().build())
        {
            new EphemeralValue(session);
            new EphemeralValue(session, null);
        }
    }

    @Test
    public void testSet() throws Exception
    {
        try (Session session = new Session.Builder().build())
        {
            Assertions.assertFalse(session.isEnabled());
            Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());
            try (AutoCloseable c = ev.set(value))
            {
                // ok ...
            }
        }

        // Simple happy path
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());
            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, Tools.deserialize(session.getClient().getData().forPath(ev.getPath())));
            }
        }
        // Simple happy path
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());

            // Make sure it doesn't exist yet
            Assertions.assertThrows(NoNodeException.class, () -> session.getClient().getData().forPath(ev.getPath()));

            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, Tools.deserialize(session.getClient().getData().forPath(ev.getPath())));
            }

            // Make sure it was removed right after closing
            Assertions.assertThrows(NoNodeException.class, () -> session.getClient().getData().forPath(ev.getPath()));
        }
    }

    @Test
    public void testGet() throws Exception
    {
        // Simple happy path
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());
            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, ev.get());
            }
        }
    }

    @Test
    public void testAwaitCreate() throws Exception
    {
        // Simple happy path
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            final Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());

            // Fire off the waiter thread
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<Exception> thrown = new AtomicReference<>(null);
            final Thread thread = new Thread(() -> {
                try
                {
                    barrier.await();
                    Assertions.assertEquals(value, ev.awaitCreation());
                }
                catch (Exception e)
                {
                    thrown.set(e);
                }
            }, "test-await-create");

            thread.start();
            barrier.await();

            // We have to wait a little bit longer
            Thread.sleep(1000);

            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, ev.get());
            }

            Exception e = thrown.get();
            if (e != null)
            {
                throw e;
            }
        }

        // Wait for between 1000 and 3000 millis, and timeout
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            final Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());

            // Fire off the waiter thread
            final long millis = (EphemeralValueTest.RANDOM.nextInt(2001) + 1000);
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<Exception> thrown = new AtomicReference<>(null);
            final Thread thread = new Thread(() -> {
                try
                {
                    barrier.await();
                    Assertions.assertEquals(value, ev.awaitCreation(millis, TimeUnit.MILLISECONDS));
                }
                catch (Exception e)
                {
                    thrown.set(e);
                }
            }, "test-await-create");

            thread.start();
            barrier.await();

            // We have to wait a little bit longer
            Thread.sleep(4000);

            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, ev.get());
            }

            Exception e = thrown.get();
            Assertions.assertNotNull(e);
            Assertions.assertEquals(TimeoutException.class, e.getClass());
        }
    }

    @Test
    public void testAwaitDelete() throws Exception
    {
        // Simple happy path
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            final Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());

            // Fire off the waiter thread
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<Exception> thrown = new AtomicReference<>(null);
            final Thread thread = new Thread(() -> {
                try
                {
                    barrier.await();
                    ev.awaitDeletion();
                }
                catch (Exception e)
                {
                    thrown.set(e);
                }
            }, "test-await-create");

            thread.start();
            barrier.await();

            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, ev.get());

                // We have to wait a little bit longer
                Thread.sleep(1000);
            }

            Exception e = thrown.get();
            if (e != null)
            {
                throw e;
            }
        }

        // Wait for between 1000 and 3000 millis, and timeout
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            final Serializable value = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, value.toString());

            // Fire off the waiter thread
            final long millis = (EphemeralValueTest.RANDOM.nextInt(2001) + 1000);
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<Exception> thrown = new AtomicReference<>(null);
            final Thread thread = new Thread(() -> {
                try
                {
                    barrier.await();
                    ev.awaitDeletion(millis, TimeUnit.MILLISECONDS);
                }
                catch (Exception e)
                {
                    thrown.set(e);
                }
            }, "test-await-create");

            thread.start();
            barrier.await();

            try (AutoCloseable c = ev.set(value))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(value, ev.get());

                // We have to wait a little bit longer
                Thread.sleep(4000);
            }

            Exception e = thrown.get();
            Assertions.assertNotNull(e);
            Assertions.assertEquals(TimeoutException.class, e.getClass());
        }
    }

    @Test
    public void testAwaitUpdate() throws Exception
    {
        // Simple happy path
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            final Serializable firstValue = UUID.randomUUID();
            final Serializable secondValue = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, firstValue.toString());

            // Fire off the waiter thread
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<Exception> thrown = new AtomicReference<>(null);
            final Thread thread = new Thread(() -> {
                try
                {
                    barrier.await();
                    Assertions.assertEquals(secondValue, ev.awaitUpdate());
                }
                catch (Exception e)
                {
                    thrown.set(e);
                }
            }, "test-await-create");

            thread.start();
            barrier.await();

            try (AutoCloseable c = ev.set(firstValue))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(firstValue, ev.get());

                // We have to wait a little bit longer
                Thread.sleep(1000);

                ev.set(secondValue);

                // Verify that the node exists and has the expected value
                Assertions.assertEquals(secondValue, ev.get());
            }

            Exception e = thrown.get();
            if (e != null)
            {
                throw e;
            }
        }

        // Wait for between 1000 and 3000 millis, and timeout
        try (Session session = new Session.Builder().connect(EphemeralValueTest.SERVER.getConnectString()).build())
        {
            Assertions.assertTrue(session.isEnabled());
            final Serializable firstValue = UUID.randomUUID();
            final Serializable secondValue = UUID.randomUUID();
            EphemeralValue ev = new EphemeralValue(session, firstValue.toString());

            // Fire off the waiter thread
            final long millis = (EphemeralValueTest.RANDOM.nextInt(2001) + 1000);
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<Exception> thrown = new AtomicReference<>(null);
            final Thread thread = new Thread(() -> {
                try
                {
                    barrier.await();
                    ev.awaitDeletion(millis, TimeUnit.MILLISECONDS);
                }
                catch (Exception e)
                {
                    thrown.set(e);
                }
            }, "test-await-create");

            thread.start();
            barrier.await();

            try (AutoCloseable c = ev.set(firstValue))
            {
                // Verify that the node exists and has the expected value
                Assertions.assertEquals(firstValue, ev.get());

                // We have to wait a little bit longer
                Thread.sleep(4000);
            }

            Exception e = thrown.get();
            Assertions.assertNotNull(e);
            Assertions.assertEquals(TimeoutException.class, e.getClass());
        }
    }
}