package com.armedia.acm.curator.wrapper.module;

import java.util.Objects;

public class Recipe
{
    protected final Session session;

    public Recipe(Session session)
    {
        this.session = Objects.requireNonNull(session, "Must provide a non-null Session object");
    }

    public final Session getSession()
    {
        return this.session;
    }
}