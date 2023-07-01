/*-
 * #%L
 * acm-config-server
 * %%
 * Copyright (C) 2019 - 2023 ArkCase LLC
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

package com.armedia.acm.curatorwrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class ConfigLocator
{

    private static final Logger LOG = LoggerFactory.getLogger(ConfigLocator.class);

    private static final String DEFAULT_CONFIG_NAME = "cluster-config.yaml";
    private static final File DEFAULT_CONFIG;
    private static final File USER_DIR;

    static
    {
        File f = new File(".");
        try
        {
            f = f.getCanonicalFile();
        }
        catch (IOException e)
        {
            // Ignore the exception
            f = f.getAbsoluteFile();
        }
        finally
        {
            USER_DIR = f;
        }
        DEFAULT_CONFIG = new File(ConfigLocator.USER_DIR, ConfigLocator.DEFAULT_CONFIG_NAME);
    }

    private static boolean isEmpty(String s)
    {
        return (s == null) || (s.length() == 0);
    }

    private static String getSysProp(String name)
    {
        return ConfigLocator.getSysProp(name, null);
    }

    private static String getSysProp(String name, String def)
    {
        String prop = String.format("arkcase.curator.%s", name);
        ConfigLocator.LOG.debug("Seeking system property [{}]", prop);
        String v = ConfigLocator.sanitize(System.getProperty(prop, def));
        ConfigLocator.LOG.debug("Value found: [{}]", v);
        return v;
    }

    private static String getEnvProp(String name)
    {
        return ConfigLocator.getEnvProp(name, null);
    }

    private static String getEnvProp(String name, String def)
    {
        String prop = String.format("ARKCASE_CLUSTER_%s", name.replace(".", "_").toUpperCase());
        ConfigLocator.LOG.debug("Seeking environment property [{}]", prop);
        String v = ConfigLocator.sanitize(System.getenv(prop));
        if (v == null)
        {
            v = def;
        }
        ConfigLocator.LOG.debug("Value found: [{}]", v);
        return ConfigLocator.sanitize(v);
    }

    private static String getDefaultProp(String name)
    {
        return ConfigLocator.getDefaultProp(name, null);
    }

    private static String getDefaultProp(String name, String def)
    {
        String v = null;

        // Priority 1: system properties
        v = ConfigLocator.getSysProp(name);
        if (v != null)
        {
            return v;
        }

        // Priority 2: environment variables
        v = ConfigLocator.getEnvProp(name);
        if (v != null)
        {
            return v;
        }

        return def;
    }

    private static String sanitize(String value)
    {
        return (value != null ? value.trim() : value);
    }

    private static final File validate(File f, boolean required) throws IOException
    {
        if (f == null)
        {
            if (required)
            {
                throw new NullPointerException("Must provide a non-null File instance");
            }
            return null;
        }

        if (!f.exists())
        {
            if (required)
            {
                throw new FileNotFoundException(f.getPath());
            }
            return null;
        }

        // We explicitly check for directories since it might be a FIFO or other
        // readable object, and we shouldn't puke out b/c of it
        if (f.isDirectory())
        {
            if (required)
            {
                throw new IOException("The file at [" + f.getPath() + "] is a directory");
            }
            return null;
        }

        if (!f.canRead())
        {
            if (required)
            {
                throw new IOException("The file at [" + f.getPath() + "] is not readable");
            }
            return null;
        }
        return f;
    }

    private static final Path validate(Path p, boolean required) throws IOException
    {
        if (p == null)
        {
            if (required)
            {
                throw new NullPointerException("Must provide a non-null Path instance");
            }
            return null;
        }

        if (!Files.exists(p))
        {
            if (required)
            {
                throw new FileNotFoundException(p.toString());
            }
            return null;
        }

        // We explicitly check for directories since it might be a FIFO or other
        // readable object, and we shouldn't puke out b/c of it
        if (Files.isDirectory(p))
        {
            if (required)
            {
                throw new IOException("The file at [" + p.toString() + "] is a directory");
            }
            return null;
        }

        if (!Files.isReadable(p))
        {
            if (required)
            {
                throw new IOException("The file at [" + p.toString() + "] is not readable");
            }
            return null;
        }
        return p;
    }

    private static final Charset sanitize(Charset encoding)
    {
        return (encoding != null) //
                ? encoding //
                : StandardCharsets.UTF_8 //
        ;
    }

    public static ConfigLocator load() throws IOException
    {
        String config = null;

        config = ConfigLocator.getDefaultProp("config");
        if (config != null)
        {
            String encoding = ConfigLocator.getDefaultProp("config.encoding");
            Charset charset = null;
            if (encoding != null)
            {
                charset = Charset.forName(encoding);
            }
            return ConfigLocator.load(config, charset);
        }

        File f = ConfigLocator.validate(ConfigLocator.DEFAULT_CONFIG, false);
        if (f != null)
        {
            return ConfigLocator.load(f);
        }

        // There's no default configurations, so just return an "empty" configuration
        return new ConfigLocator(null);
    }

    public static ConfigLocator load(String config) throws IOException
    {
        return ConfigLocator.load(config, null);
    }

    public static ConfigLocator load(String config, Charset encoding) throws IOException
    {
        Objects.requireNonNull(config, "Must provide a non-null path string to read");
        return ConfigLocator.load(new File(config), ConfigLocator.sanitize(encoding));
    }

    public static ConfigLocator load(File config) throws IOException
    {
        return ConfigLocator.load(config, null);
    }

    public static ConfigLocator load(File config, Charset encoding) throws IOException
    {
        Objects.requireNonNull(config, "Must provide a non-null File to read");
        return ConfigLocator.load(config.toPath(), encoding);
    }

    public static ConfigLocator load(Path config) throws IOException
    {
        return ConfigLocator.load(config, null);
    }

    public static ConfigLocator load(Path config, Charset encoding) throws IOException
    {
        Objects.requireNonNull(config, "Must provide a non-null Path to read");
        try (Reader r = Files.newBufferedReader(config,
                ConfigLocator.sanitize(encoding)))
        {
            return ConfigLocator.load(r);
        }
    }

    public static ConfigLocator load(InputStream config, Charset encoding) throws IOException
    {
        Objects.requireNonNull(config, "Must provide a non-null InputStream to read");
        try (Reader r = new InputStreamReader(config, ConfigLocator.sanitize(encoding)))
        {
            return ConfigLocator.load();
        }
    }

    public static ConfigLocator load(ReadableByteChannel config, Charset encoding) throws IOException
    {
        Objects.requireNonNull(config, "Must provide a non-null ReadableByteChannel to read");
        try (Reader r = Channels.newReader(config, ConfigLocator.sanitize(encoding)))
        {
            return ConfigLocator.load(r);
        }
    }

    public static ConfigLocator load(Reader config) throws IOException
    {
        return new ConfigLocator(Objects.requireNonNull(config, "Must provide a non-null ReadableByteChannel to read"));
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, Object> yaml;

    private ConfigLocator(Reader src) throws IOException
    {
        if (src == null)
        {
            this.yaml = Collections.emptyMap();
            return;
        }

        this.yaml = Collections.unmodifiableMap(new Yaml().load(src));
    }

    public Object get(String name)
    {
        return get(name, null);
    }

    public Object get(String name, String def)
    {
        Object v = resolve(name);
        if (v != null)
        {
            return v;
        }

        return ConfigLocator.getDefaultProp(name, def);
    }

    private Object resolve(String name)
    {
        return resolve(name, null);
    }

    private Object resolve(String name, Object def)
    {
        List<String> parts = new LinkedList<>();
        for (String s : name.split("\\Q.\\E"))
        {
            if (ConfigLocator.isEmpty(s))
            {
                throw new IllegalArgumentException(
                        "The property name can't begin with or end with a dot, or have two consecutive dots (" + name + ")");
            }
            parts.add(s);
        }

        Map<?, ?> ctx = this.yaml;
        Object current = ctx;
        while (true)
        {
            if (parts.isEmpty())
            {
                return current;
            }

            String s = parts.remove(0);

            if (!ctx.containsKey(s))
            {
                break;
            }

            current = ctx.get(s);
            if (current == null)
            {
                break;
            }

            if (Map.class.isInstance(current))
            {
                ctx = Map.class.cast(current);
                continue;
            }

            if (Collection.class.isInstance(current))
            {

            }

        }

        Object ret = ConfigLocator.getDefaultProp(name);
        return (ret != null) ? ret : def;
    }
}