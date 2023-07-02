package com.armedia.acm.curator.wrapper;

import com.armedia.acm.curator.wrapper.conf.Cfg;
import com.armedia.acm.curator.wrapper.module.Session;

public class Main
{
    private final Cfg cfg;

    private Main(String... args)
    {
        // TODO: Find our configurations ... either from the command line arguments,
        // environment, or system properties
        this.cfg = null;
    }

    private Session newSession() throws InterruptedException
    {
        return new Session(this.cfg.getSession());
    }

    private int run() throws Exception
    {
        return new Wrapper(this::newSession, this.cfg.getWrapper()).run();
    }

    public static void main(String... args) throws Exception
    {
        System.exit(new Main(args).run());
    }
}