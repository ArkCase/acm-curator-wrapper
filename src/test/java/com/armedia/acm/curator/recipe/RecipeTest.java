package com.armedia.acm.curator.recipe;

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
            new Recipe(null)
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
            new Recipe(session)
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
            Recipe r = new Recipe(session)
            {
            };
            Assertions.assertSame(session, r.getSession());
        }
    }
}