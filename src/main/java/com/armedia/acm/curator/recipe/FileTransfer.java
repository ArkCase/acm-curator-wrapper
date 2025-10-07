package com.armedia.acm.curator.recipe;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.armedia.acm.curator.Session;

public abstract class FileTransfer extends Recipe
{
    protected FileTransfer(Session session)
    {
        super(session);
    }

    protected FileTransfer(Session session, String name)
    {
        super(session, name);
    }

    protected boolean parseRecursive(String str)
    {
        return Strings.CI.equals("true", StringUtils.trim(StringUtils.lowerCase(str)));
    }

    public int execute(String target)
    {
        return execute(target, null);
    }

    public abstract int execute(String counterpart, String recursiveStr);
}
