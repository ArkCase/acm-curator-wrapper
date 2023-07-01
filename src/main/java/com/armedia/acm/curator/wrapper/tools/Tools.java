package com.armedia.acm.curator.wrapper.tools;

import java.util.function.Supplier;

public class Tools
{

    public static boolean isEmpty(CharSequence str)
    {
        return ((str == null) || (str.length() == 0));
    }

    public static <T> T coalesce(@SuppressWarnings("unchecked") T... values)
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
}