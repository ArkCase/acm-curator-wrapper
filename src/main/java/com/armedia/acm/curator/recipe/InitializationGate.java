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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.CheckedBiFunction;

public class InitializationGate extends Recipe
{
    public static abstract class Initializer
    {
        private final String version;

        public Initializer(String version)
        {
            this.version = Objects.requireNonNull(version, "Must provide a non-null version for the initializer");
        }

        public final String getVersion()
        {
            return this.version;
        }

        public abstract Map<String, String> initialize(String current, Map<String, String> extraData) throws Exception;
    }

    public static class FunctionalInitializer extends Initializer
    {
        private final CheckedBiFunction<String, Map<String, String>, Map<String, String>> initializer;

        public FunctionalInitializer(String version, CheckedBiFunction<String, Map<String, String>, Map<String, String>> initializer)
        {
            super(version);
            this.initializer = initializer;
        }

        @Override
        public final Map<String, String> initialize(String current, Map<String, String> extraData) throws Exception
        {
            return (this.initializer != null) //
                    ? this.initializer.applyChecked(current, extraData) //
                    : null //
            ;
        }
    }

    private static final InitializationInfo NULL_INFO = new InitializationInfo(null, null, null, null);

    public static class InitializationInfo implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private final String version;
        private final Instant started;
        private final Duration duration;
        private final Map<String, String> extraData;

        private InitializationInfo(byte[] data)
        {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data)))
            {
                this.version = String.class.cast(in.readObject());
                this.started = Instant.class.cast(in.readObject());
                this.duration = Duration.class.cast(in.readObject());
                @SuppressWarnings("unchecked")
                Map<String, String> extraData = (Map<String, String>) in.readObject();
                this.extraData = extraData;
            }
            catch (IOException | ClassNotFoundException e)
            {
                throw new RuntimeException("Unexpected exception caught", e);
            }
        }

        private InitializationInfo(String version, Instant started, Duration duration, Map<String, String> extraData)
        {
            this.version = version;
            if (version != null)
            {
                this.started = started;
                this.duration = duration;
                this.extraData = extraData;
            }
            else
            {
                this.started = null;
                this.duration = null;
                this.extraData = null;
            }
        }

        public String getVersion()
        {
            return this.version;
        }

        public Instant getStarted()
        {
            return this.started;
        }

        public Duration getDuration()
        {
            return this.duration;
        }

        public Map<String, String> getExtraData()
        {
            return this.extraData;
        }

        private byte[] encode()
        {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                try (ObjectOutputStream out = new ObjectOutputStream(baos))
                {
                    out.writeObject(this.version);
                    out.writeObject(this.started);
                    out.writeObject(this.duration);
                    out.writeObject(this.extraData);
                }
                return baos.toByteArray();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unexpected exception caught", e);
            }
        }

        private boolean needsUpdate(String v)
        {
            // If *they* don't have a version or it's the same as ours, no update
            if ((v == null) || Objects.equals(this.version, v))
            {
                return false;
            }

            // If *we* don't have a version but they do, for sure we need an update
            if (this.version == null)
            {
                return true;
            }

            // We both have versions? Compare them for realsies...
            ComparableVersion a = new ComparableVersion(v);
            ComparableVersion b = new ComparableVersion(this.version);
            return a.compareTo(b) > 0;
        }

        @Override
        public String toString()
        {
            if (this.version == null)
            {
                return "InitializationInfo [<none>]";
            }
            return String.format("InitializationInfo [version=%s, started=%s, duration=%s]", this.version, this.started, this.duration);
        }
    }

    private static String toHexString(byte[] arr)
    {
        if (arr == null)
        {
            return null;
        }
        if (arr.length == 0)
        {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        for (byte b : arr)
        {
            buf.append(String.format("%02x", b & 0x000000FF));
        }
        return buf.toString();
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Mutex mutex;

    public InitializationGate(Session session)
    {
        this(session, null);
    }

    public InitializationGate(Session session, String name)
    {
        super(session, name);
        this.mutex = new Mutex(session, String.format("initializer-%s", this.name));
    }

    public Mutex getMutex()
    {
        return this.mutex;
    }

    public boolean initialize(Initializer initializer) throws Exception
    {
        return initialize(initializer, null);
    }

    public boolean initialize(Initializer initializer, Duration maxWait) throws Exception
    {
        if (initializer == null)
        {
            return false;
        }

        this.session.assertEnabled();

        String incoming = initializer.getVersion();
        this.log.info("Attempting to initialize for [{}] on version {}", this.name, incoming);
        InitializationInfo existing = getInitializationInfo();
        this.log.info("Existing version info before mutex lock      : {}", existing);
        if (existing.needsUpdate(incoming))
        {
            try (AutoCloseable lock = this.mutex.acquire(maxWait))
            {
                existing = getInitializationInfo();
                this.log.info("Actual existing version info after mutex lock: {}", existing);
                if (existing.needsUpdate(incoming))
                {
                    this.log.info("Incoming version {} is newer than the existing version {}", incoming,
                            existing.version);

                    this.log.info("Starting the initialization");
                    final Instant start = Instant.now();
                    Map<String, String> extraData = initializer.initialize(existing.version, existing.extraData);

                    // Mark the initialization as successful
                    final Duration duration = Duration.between(start, Instant.now());
                    InitializationGate.this.log.info("Initialization completed ({}), storing the persistent marker", duration);
                    setInitializationInfo(incoming, start, duration, extraData);
                    return true;
                }
            }
        }
        return false;
    }

    public InitializationInfo getInitializationInfo() throws Exception
    {
        try
        {
            byte[] data = this.session.getClient().getData().forPath(this.path);
            if (this.log.isDebugEnabled())
            {
                this.log.debug("Data loaded from path [{}] = [{}]", this.path, InitializationGate.toHexString(data));
            }
            return new InitializationInfo(data);
        }
        catch (NoNodeException e)
        {
            return InitializationGate.NULL_INFO;
        }
    }

    protected void setInitializationInfo(String version, Instant start, Duration duration, Map<String, String> extraData)
            throws IOException, Exception
    {
        InitializationInfo info = new InitializationInfo(version, start, duration, extraData);
        this.log.debug("Encoding the data from {}", info);
        byte[] data = info.encode();
        if (this.log.isDebugEnabled())
        {
            this.log.debug("Data encoded as [{}]", InitializationGate.toHexString(data));
        }
        try
        {
            this.session.getClient().setData().forPath(this.path, data);
        }
        catch (NoNodeException e)
        {
            this.session.getClient().create().creatingParentContainersIfNeeded().forPath(this.path, data);
        }
    }
}