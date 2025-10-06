package com.armedia.acm.curator.recipe;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.armedia.acm.curator.Session;

public class Download extends FileTransfer
{
    public Download(Session session)
    {
        super(session);
    }

    public Download(Session session, String name)
    {
        super(session, name);
    }

    @Override
    public int execute(String target, String recursiveStr)
    {
        parseRecursive(recursiveStr);

        if (!isSessionEnabled())
        {
            this.log.warn("The current session is not enabled, cannot download resources");
            return 1;
        }

        try
        {
            byte[] data = getSession().getClient().getData().forPath(this.name);
            FileUtils.writeByteArrayToFile(new File(target), data);
            return 0;
        }
        catch (Exception e)
        {
            this.log.error("Failed to download the contents of [{}] into [{}]", this.name, target, e);
            return 1;
        }
    }
}
