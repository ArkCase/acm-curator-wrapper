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
package com.armedia.acm.curator.tools;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class Tools
{
    public static final File CWD = Tools.canonicalize(new File("."));

    public static File canonicalize(File f)
    {
        if (f == null)
        {
            return null;
        }
        try
        {
            f = f.getCanonicalFile();
        }
        catch (IOException e)
        {
            // Ignore
        }
        finally
        {
            f = f.getAbsoluteFile();
        }
        return f;
    }

    public static boolean isEmpty(CharSequence str)
    {
        return ((str == null) || (str.length() == 0));
    }

    @SafeVarargs
    public static <T> T coalesce(T... values)
    {
        for (T t : values)
        {
            if (t != null)
            {
                return t;
            }
        }
        return null;
    }

    public static <T extends CharSequence> T ifEmpty(T t, Supplier<? extends T> s)
    {
        if (!Tools.isEmpty(t))
        {
            return t;
        }
        if (s != null)
        {
            return s.get();
        }
        return null;
    }

    public static <T> T ifNull(T t, Supplier<? extends T> s)
    {
        if (t != null)
        {
            return t;
        }
        if (s != null)
        {
            return s.get();
        }
        return null;
    }

    public static Throwable closeQuietly(AutoCloseable c)
    {
        if (c != null)
        {
            try
            {
                c.close();
            }
            catch (Throwable t)
            {
                return t;
            }
        }
        return null;
    }

    public static <T> T identity(T t)
    {
        return t;
    }

    public static <T> void noop(T t)
    {
        // Do nothing...
    }

    public static void noop()
    {
        // DO nothing
    }
}
