package com.armedia.acm.curator.recipe;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.armedia.acm.curator.Session;

public class Upload extends FileTransfer
{
    public Upload(Session session)
    {
        super(session);
    }

    public Upload(Session session, String name)
    {
        super(session, name);
    }

    @Override
    public int execute(String source, String recursiveStr)
    {
        parseRecursive(recursiveStr);

        if (!isSessionEnabled())
        {
            this.log.warn("The current session is not enabled, cannot upload any resources");
            return 1;
        }

        try (InputStream in = Files.newInputStream(Path.of(source), StandardOpenOption.READ))
        {
            getSession().getClient().setData().forPath(this.name, in.readAllBytes());
            return 0;
        }
        catch (Exception e)
        {
            this.log.error("Failed to upload the contents of [{}] into [{}]", source, this.name, e);
            return 1;
        }
    }
}
