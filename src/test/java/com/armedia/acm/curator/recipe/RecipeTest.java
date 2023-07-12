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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.acm.curator.Session;

public class RecipeTest
{

    @Test
    public void testConstructor() throws Exception
    {
        new Recipe(null)
        {
        };
        new Recipe(null, null)
        {
        };

        Session.Builder builder = new Session.Builder();
        try (Session session = builder.build())
        {
            new Recipe(session, null);
            new Recipe(session);
        }
    }

    @Test
    public void testGetSession() throws Exception
    {
        Session.Builder builder = new Session.Builder();
        try (Session session = builder.build())
        {
            Recipe r = new Recipe(session, null);
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
                Recipe r = new Recipe(session, name);
                Assertions.assertSame(session, r.getSession());
                Assertions.assertEquals(name, r.getName());
            }

            Recipe r = new Recipe(session, null);
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
            Set<String> oldPaths = new HashSet<>();
            for (int i = 0; i < 5; i++)
            {
                final String name = String.format("my-test-name-%02d", i);
                final String uuid = UUID.randomUUID().toString();
                Recipe r = new Recipe(session, name)
                {
                    @Override
                    public String toString()
                    {
                        return uuid;
                    }
                };
                String path = String.format("%s/%s/%s", session.getBasePath(), r.getClass().getSimpleName().toLowerCase(), name);
                Assertions.assertSame(session, r.getSession());
                Assertions.assertEquals(name, r.getName());
                Assertions.assertEquals(path, r.getPath());
                Assertions.assertTrue(oldPaths.add(r.getPath()), "Duplicate path from simple class name: " + path);
            }

            final String uuid = UUID.randomUUID().toString();
            Recipe r = new Recipe(session, null)
            {
                @Override
                public String toString()
                {
                    return uuid;
                }
            };
            String name = r.getName();
            String path = String.format("%s/%s/%s", session.getBasePath(), r.getClass().getSimpleName().toLowerCase(), name);
            Assertions.assertSame(session, r.getSession());
            Assertions.assertEquals(name, r.getName());
            Assertions.assertEquals(path, r.getPath());
            Assertions.assertTrue(oldPaths.add(r.getPath()), "Duplicate path from simple class name: " + path);
        }
    }
}
