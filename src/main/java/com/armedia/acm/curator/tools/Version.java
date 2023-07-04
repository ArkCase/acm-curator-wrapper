package com.armedia.acm.curator.tools;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version>, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final Pattern PARSER = Pattern.compile("^(0|[1-9][0-9]*)[.](0|[1-9][0-9]*)[.](0|[1-9][0-9]*)$");

    public static Version parse(String str)
    {
        if (Tools.isEmpty(str))
        {
            return null;
        }
        Matcher m = Version.PARSER.matcher(str);
        if (!m.matches())
        {
            return null;
        }

        return new Version(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    private final int major;
    private final int minor;
    private final int release;

    public Version(int major)
    {
        this(major, 0, 0);
    }

    public Version(int major, int minor)
    {
        this(major, minor, 0);
    }

    public Version(int major, int minor, int release)
    {
        this.major = Math.max(0, major);
        this.minor = Math.max(0, minor);
        this.release = Math.max(0, release);
    }

    public int getMajor()
    {
        return this.major;
    }

    public int getMinor()
    {
        return this.minor;
    }

    public int getRelease()
    {
        return this.release;
    }

    @Override
    public int compareTo(Version v)
    {
        if (v == null)
        {
            return 1;
        }
        if (v == this)
        {
            return 0;
        }

        int r = 0;
        r = Integer.compare(this.major, v.major);
        if (r != 0)
        {
            return r;
        }

        r = Integer.compare(this.minor, v.minor);
        if (r != 0)
        {
            return r;
        }

        r = Integer.compare(this.release, v.release);
        if (r != 0)
        {
            return r;
        }

        return 0;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + this.major;
        result = (prime * result) + this.minor;
        result = (prime * result) + this.release;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }

        Version other = Version.class.cast(obj);
        if (this.major != other.major)
        {
            return false;
        }
        if (this.minor != other.minor)
        {
            return false;
        }
        if (this.release != other.release)
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("%d.%d.%d", this.major, this.minor, this.release);
    }
}