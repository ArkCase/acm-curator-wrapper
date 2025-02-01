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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.apache.curator.framework.recipes.watch.PersistentWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class EphemeralValue extends Recipe
{
    public static final String DEFAULT_NAME = "default";

    public EphemeralValue(Session session)
    {
        this(session, null);
    }

    public EphemeralValue(Session session, String name)
    {
        super(session, name);
    }

    public boolean exists() throws Exception
    {
        if (!isSessionEnabled())
        {
            this.log.debug("The current session is not enabled, the value does not exist");
            return false;
        }

        return (getClient().checkExists().forPath(this.path) != null);
    }

    public AutoCloseable set(Serializable value) throws Exception
    {
        return set(value, -1, null);
    }

    public AutoCloseable set(Serializable value, long wait, TimeUnit unit) throws Exception
    {
        if (!isSessionEnabled())
        {
            this.log.debug("The current session is not enabled, the value does not exist");
            return Tools::noop;
        }

        final byte[] data = Tools.serialize(value);
        final PersistentNode node = new PersistentNode(getClient(), CreateMode.EPHEMERAL, false, this.path, data, true);

        this.log.debug("Set the ephemeral value at [{}] ({} bytes)", this.path, data.length);
        node.start();

        if ((wait < 0) || (unit == null))
        {
            wait = Long.MAX_VALUE;
            unit = TimeUnit.DAYS;
        }
        node.waitForInitialCreate(wait, unit);
        return new ItemCloser<>(node, AutoCloseable::close);
    }

    public Serializable get() throws Exception
    {
        if (!isSessionEnabled())
        {
            this.log.debug("The current session is not enabled, the value does not exist");
            return null;
        }

        try
        {
            return Tools.deserialize(getClient().getData().forPath(this.path));
        }
        catch (NoNodeException e)
        {
            return null;
        }
    }

    protected Serializable await(final EventType eventType, boolean retrieve, long wait, TimeUnit unit) throws Exception
    {
        if (!isSessionEnabled())
        {
            this.log.debug("The current session is not enabled, cannot await");
            return null;
        }

        if ((wait < 0) || (unit == null))
        {
            wait = Long.MAX_VALUE;
            unit = TimeUnit.DAYS;
        }
        try (PersistentWatcher w = new PersistentWatcher(getClient(), this.path, false))
        {
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final Listenable<Watcher> listenable = w.getListenable();
            Watcher watcher = (event) -> {
                if (event.getType() == eventType)
                {
                    try
                    {
                        barrier.await();
                    }
                    catch (InterruptedException | BrokenBarrierException e)
                    {
                        throw new RuntimeException(
                                String.format("Barrier broken or wait interrupted while awaiting the event %s", eventType.name()));
                    }
                }
            };
            listenable.addListener(watcher);
            w.start();
            try
            {
                if ((wait < 0) || (unit == null))
                {
                    barrier.await();
                }
                else
                {
                    barrier.await(wait, unit);
                }
            }
            finally
            {
                listenable.removeListener(watcher);
            }

            return (retrieve ? get() : null);
        }
    }

    public Serializable awaitCreation() throws Exception
    {
        return awaitCreation(0, null);
    }

    public Serializable awaitCreation(long wait, TimeUnit unit) throws Exception
    {
        return await(EventType.NodeCreated, true, wait, unit);
    }

    public Serializable awaitUpdate() throws Exception
    {
        return awaitUpdate(0, null);
    }

    public Serializable awaitUpdate(long wait, TimeUnit unit) throws Exception
    {
        return await(EventType.NodeDataChanged, true, wait, unit);
    }

    public void awaitDeletion() throws Exception
    {
        awaitCreation(0, null);
    }

    public void awaitDeletion(long wait, TimeUnit unit) throws Exception
    {
        await(EventType.NodeDeleted, false, wait, unit);
    }
}