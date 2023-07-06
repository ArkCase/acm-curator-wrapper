package com.armedia.acm.curator;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.armedia.acm.curator.tools.Tools;

public class SessionTest
{
    private static TestingServer SERVER = null;

    @BeforeAll
    public static void beforeAll() throws Exception
    {
        if (SessionTest.SERVER == null)
        {
            SessionTest.SERVER = new TestingServer();
        }
    }

    @AfterAll
    public static void afterAll() throws Exception
    {
        if (SessionTest.SERVER != null)
        {
            try
            {
                SessionTest.SERVER.close();
            }
            finally
            {
                SessionTest.SERVER = null;
            }
        }
    }

    @Test
    public void testBuilder()
    {
        Session.Builder builder = new Session.Builder();

        for (int i = Integer.MIN_VALUE; i < 0; i /= 2)
        {
            builder.connectionTimeout(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.connectionTimeout(0);
        Assertions.assertNull(builder.connect(), "0");
        Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), "0");
        Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), "0");
        Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), "0");
        Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), "0");
        for (int i = Integer.MAX_VALUE; i > 0; i /= 2)
        {
            builder.connectionTimeout(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Math.max(Session.MIN_CONNECTION_TIMEOUT, i), builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.connectionTimeout(0);

        for (int i = Integer.MIN_VALUE; i < 0; i /= 2)
        {
            builder.sessionTimeout(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.sessionTimeout(0);
        Assertions.assertNull(builder.connect(), "0");
        Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), "0");
        Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), "0");
        Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), "0");
        Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), "0");
        for (int i = Integer.MAX_VALUE; i > 0; i /= 2)
        {
            builder.sessionTimeout(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Math.max(Session.MIN_SESSION_TIMEOUT, i), builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.sessionTimeout(0);

        for (int i = Integer.MIN_VALUE; i < 0; i /= 2)
        {
            builder.retryCount(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.retryCount(0);
        Assertions.assertNull(builder.connect(), "0");
        Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), "0");
        Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), "0");
        Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), "0");
        Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), "0");
        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            builder.retryCount(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(i, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.retryCount(0);

        for (int i = Integer.MIN_VALUE; i < 0; i /= 2)
        {
            builder.retryDelay(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.retryDelay(0);
        Assertions.assertNull(builder.connect(), "0");
        Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), "0");
        Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), "0");
        Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), "0");
        Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), "0");
        Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), "0");
        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            builder.retryDelay(i);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Math.max(Session.MIN_RETRY_DELAY, Math.min(Session.MAX_RETRY_DELAY, i)), builder.retryDelay(),
                    () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.retryDelay(0);

        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            String p = String.format("/a/b/%d", i);
            builder.basePath(p);
            final int I = i;
            Assertions.assertNull(builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(p, builder.basePath(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
        }
        builder.basePath(null);

        for (int i = 0; i < 2; i++)
        {
            boolean w = ((i % 2) == 0);
            builder.waitForConnection(w);
            Assertions.assertNull(builder.connect(), () -> "w = " + w);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "w = " + w);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "w = " + w);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "w = " + w);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "w = " + w);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "w = " + w);
            Assertions.assertEquals(w, builder.waitForConnection(), () -> "w = " + w);
        }
        builder.waitForConnection(null);
        Assertions.assertNull(builder.connect());
        Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout());
        Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout());
        Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount());
        Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay());
        Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath());
        Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection());

        for (int i = Integer.MAX_VALUE; i > 0; i /= 10)
        {
            String c = String.format("/a/b/%d", i);
            builder.connect(c);
            final int I = i;
            Assertions.assertEquals(c, builder.connect(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection(), () -> "i = " + I);
            Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath(), () -> "i = " + I);
        }
        builder.connect(null);
        Assertions.assertNull(builder.connect());
        Assertions.assertEquals(Session.DEFAULT_CONNECTION_TIMEOUT, builder.connectionTimeout());
        Assertions.assertEquals(Session.DEFAULT_SESSION_TIMEOUT, builder.sessionTimeout());
        Assertions.assertEquals(Session.DEFAULT_RETRY_COUNT, builder.retryCount());
        Assertions.assertEquals(Session.DEFAULT_RETRY_DELAY, builder.retryDelay());
        Assertions.assertEquals(Session.DEFAULT_BASE_PATH, builder.basePath());
        Assertions.assertEquals(Session.DEFAULT_WAIT_FOR_CONNECTION, builder.waitForConnection());
    }

    @Test
    public void testConstructor() throws Exception
    {
        Session.Builder builder = new Session.Builder();

        try (Session session = builder.build())
        {
            Assertions.assertNull(session.getBasePath());
            Assertions.assertFalse(session.isEnabled());
            Assertions.assertNull(session.getClient());
        }

        builder.connect(SessionTest.SERVER.getConnectString());
        try (Session session = builder.build())
        {
            Assertions.assertEquals(builder.basePath(), session.getBasePath());
            Assertions.assertTrue(session.isEnabled());
            Assertions.assertNotNull(session.getClient());
        }
    }

    @Test
    public void testGetBasePath() throws Exception
    {
        Session.Builder builder = new Session.Builder();
        builder.connect(SessionTest.SERVER.getConnectString());

        for (int i = 0; i < 10; i++)
        {
            String p = "/a/b/c/" + i;
            builder.basePath(p);
            try (Session session = builder.build())
            {
                Assertions.assertEquals(p, session.getBasePath());
                Assertions.assertTrue(session.isEnabled());
                Assertions.assertNotNull(session.getClient());
            }
        }
    }

    @Test
    public void testAssertEnabled() throws Exception
    {
        Session.Builder builder = new Session.Builder();

        try (Session session = builder.build())
        {
            Assertions.assertFalse(session.isEnabled());
            session.assertEnabled();
            Assertions.fail("Did not fail when the session was disabled");
        }
        catch (IllegalStateException e)
        {
            // All's well
        }

        builder.connect(SessionTest.SERVER.getConnectString());
        try (Session session = builder.build())
        {
            Assertions.assertTrue(session.isEnabled());
            session.assertEnabled();
            // All's well
        }
        catch (IllegalStateException e)
        {
            Assertions.fail("Failed when a good connection existed");
        }
    }

    @Test
    public void testAddCleanup() throws Exception
    {
        Session.Builder builder = new Session.Builder();

        try (Session session = builder.build())
        {
            Assertions.assertEquals(Session.NULL_CLEANUP_KEY, session.addCleanup(Tools::noop));
            Assertions.assertEquals(Session.NULL_CLEANUP_KEY, session.addCleanup(null));
        }

        builder.connect(SessionTest.SERVER.getConnectString());
        try (Session session = builder.build())
        {
            Object key = session.addCleanup(Tools::noop);
            Assertions.assertNotNull(key);
            Assertions.assertNotEquals(Session.NULL_CLEANUP_KEY, key);
            Assertions.assertTrue(Integer.class.isInstance(key));
            Assertions.assertEquals(Session.NULL_CLEANUP_KEY, session.addCleanup(null));
        }
    }

    @Test
    public void testRemoveCleanup() throws Exception
    {
        Session.Builder builder = new Session.Builder();

        try (Session session = builder.build())
        {
            Object key = session.addCleanup(Tools::noop);
            Assertions.assertEquals(Session.NULL_CLEANUP_KEY, session.addCleanup(Tools::noop));
            Assertions.assertNull(session.removeCleanup(key));
        }

        builder.connect(SessionTest.SERVER.getConnectString());
        try (Session session = builder.build())
        {
            for (int i = 0; i < 10; i++)
            {
                Closeable c = () -> {
                };

                Object key = session.addCleanup(c);
                Assertions.assertNotNull(key);
                Assertions.assertNotEquals(Session.NULL_CLEANUP_KEY, key);
                Assertions.assertTrue(Integer.class.isInstance(key));
                Assertions.assertSame(c, session.removeCleanup(key));
            }
        }
    }

    @Test
    public void testClose() throws Exception
    {
        Session.Builder builder = new Session.Builder();

        try (Session session = builder.build())
        {
            Object key = session.addCleanup(Tools::noop);
            Assertions.assertEquals(Session.NULL_CLEANUP_KEY, session.addCleanup(Tools::noop));
            Assertions.assertNull(session.removeCleanup(key));
        }

        builder.connect(SessionTest.SERVER.getConnectString());
        final LinkedList<Closeable> l = new LinkedList<>();
        try (Session session = builder.build())
        {
            for (int i = 0; i < 10; i++)
            {
                final int I = i;
                Closeable c = new Closeable()
                {
                    @Override
                    public String toString()
                    {
                        return String.format("Closeable # %d", I);
                    }

                    @Override
                    public void close() throws IOException
                    {
                        // Ensure this element is the current last element in the list
                        System.out.printf(toString());
                        Closeable other = l.removeLast();
                        Assertions.assertSame(other, this);
                        if (other != this)
                        {
                            other.close();
                            close();
                            Assertions.fail("Object mismatch on closing order");
                        }
                    }
                };

                session.addCleanup(c);
                l.add(c);
            }
        }
    }
}