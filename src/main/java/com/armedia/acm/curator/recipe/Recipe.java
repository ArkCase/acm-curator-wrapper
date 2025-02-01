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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class Recipe
{
    private static String sanitize(String name)
    {
        name = name.trim();
        if (Tools.isEmpty(name))
        {
            throw new IllegalArgumentException("Must provide a non-blank string (i.e. more than just spaces)");
        }
        return name;
    }

    protected class ItemCloser<T> implements AutoCloseable
    {
        final Object cleanupKey;
        final T item;
        final FailableConsumer<T, Exception> closer;
        final String label;
        final AtomicBoolean closed = new AtomicBoolean(false);

        protected ItemCloser(T item, FailableConsumer<T, Exception> closer)
        {
            this.cleanupKey = addCleanup(this);
            Recipe.this.log.trace("Cleanup key is [{}]", this.cleanupKey);
            this.item = item;
            this.closer = closer;
            this.label = item.getClass().getSimpleName();
        }

        @Override
        public void close() throws Exception
        {
            if (this.closed.compareAndSet(false, true))
            {
                try
                {
                    Recipe.this.log.trace("Releasing the {} at [{}]", this.label, Recipe.this.path);
                    this.closer.accept(this.item);
                }
                finally
                {
                    removeCleanup(this.cleanupKey);
                }
            }
        }
    }

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Session session;
    protected final String name;
    protected final String path;

    protected Recipe(Session session)
    {
        this(session, null);
    }

    protected Recipe(Session session, String name)
    {
        this.session = session;
        String root = String.format("%s/%s", (session != null ? session.getBasePath() : Session.DEFAULT_BASE_PATH),
                getClass().getSimpleName().toLowerCase());
        if (Tools.isEmpty(name))
        {
            this.name = UUID.randomUUID().toString();
        }
        else
        {
            this.name = Recipe.sanitize(name);
        }
        this.path = String.format("%s/%s", root, this.name);
    }

    protected final boolean isSessionEnabled()
    {
        return (this.session != null) && this.session.isEnabled();
    }

    protected final CuratorFramework getClient()
    {
        return (this.session != null ? this.session.getClient() : null);
    }

    protected final Object addCleanup(AutoCloseable cleanup)
    {
        if (cleanup != null)
        {
            return this.session.addCleanup(cleanup);
        }
        return null;
    }

    protected final AutoCloseable removeCleanup(Object key)
    {
        if (key != null)
        {
            return this.session.removeCleanup(key);
        }
        return null;
    }

    public final Session getSession()
    {
        return this.session;
    }

    public final String getName()
    {
        return this.name;
    }

    public final String getPath()
    {
        return this.path;
    }
}