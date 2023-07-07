package com.armedia.acm.curator.recipe;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.acm.curator.Session;

public class RecipeTest
{

    @Test
    public void testConstructor() throws Exception
    {
        try
        {
            new Recipe(null, null)
            {
            };
            Assertions.fail("Did not fail with a null parameter");
        }
        catch (NullPointerException e)
        {
            // all is well
        }

        Session.Builder builder = new Session.Builder();
        try (Session session = builder.build())
        {
            new Recipe(session, null)
            {
            };
        }
    }

    @Test
    public void testGetSession() throws Exception
    {
        Session.Builder builder = new Session.Builder();
        try (Session session = builder.build())
        {
            Recipe r = new Recipe(session, null)
            {
            };
            Assertions.assertSame(session, r.getSession());
        }
    }

    @Test
    public void testGetName() throws Exception
    {
        Session.Builder builder = new Session.Builder();
        try (Session session = builder.build())
        {
            for (int i = 0; i < 5; i++)
            {
                String name = String.format("my-test-name-%02d", i);
                Recipe r = new Recipe(session, name)
                {
                };
                Assertions.assertSame(session, r.getSession());
                Assertions.assertEquals(name, r.getName());
            }

            Recipe r = new Recipe(session, null)
            {
            };
            Assertions.assertSame(session, r.getSession());
            String name = r.getName();
            Assertions.assertNotNull(UUID.fromString(name));
        }
    }

    @Test
    public void testGetPath() throws Exception
    {
        Session.Builder builder = new Session.Builder();
        try (Session session = builder.build())
        {
            for (int i = 0; i < 5; i++)
            {
                String name = String.format("my-test-name-%02d", i);
                Recipe r = new Recipe(session, name)
                {
                };
                String path = String.format("%s/%s/%s", session.getBasePath(), r.getClass().getSimpleName().toLowerCase(), name);
                Assertions.assertSame(session, r.getSession());
                Assertions.assertEquals(name, r.getName());
                Assertions.assertEquals(path, r.getPath());
            }

            Recipe r = new Recipe(session, null)
            {
            };
            String name = r.getName();
            String path = String.format("%s/%s/%s", session.getBasePath(), r.getClass().getSimpleName().toLowerCase(), name);
            Assertions.assertSame(session, r.getSession());
            Assertions.assertEquals(name, r.getName());
            Assertions.assertEquals(path, r.getPath());
        }
    }
}