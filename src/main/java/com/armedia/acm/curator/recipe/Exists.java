package com.armedia.acm.curator.recipe;

import org.apache.zookeeper.data.Stat;

import com.armedia.acm.curator.Session;

public class Exists extends Recipe
{
    public Exists(Session session)
    {
        super(session);
    }

    public Exists(Session session, String name)
    {
        super(session, name);
    }

    public int execute()
    {
        if (!isSessionEnabled())
        {
            this.log.warn("The current session is not enabled, cannot check for the existence of resources");
            return 1;
        }

        try
        {
            Stat stat = getSession().getClient().checkExists().forPath(this.name);
            return (stat != null) ? 0 : 1;
        }
        catch (Exception e)
        {
            this.log.error("Failed to check for the existence of [{}]", this.name, e);
            return 1;
        }
    }
}
