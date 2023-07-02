package com.armedia.acm.curator.wrapper;

import org.apache.commons.cli.Options;

import com.armedia.acm.curator.wrapper.conf.Cfg;
import com.armedia.acm.curator.wrapper.conf.SessionCfg;

public class Main
{
    private final Cfg cfg;

    private static final Options OPTIONS = new Options() //
            .addOption("c", "config", true, "The configuration file to use") //
    ;

    private Main(String... args)
    {
        // TODO: Find our configurations ... either from the command line arguments,
        // environment, or system properties
        this.cfg = null;
    }

    private int run() throws Exception
    {
        final SessionCfg cfg = this.cfg.getSession();
        return new Wrapper(cfg::build, this.cfg.getWrapper()).run();
    }

    public static void main(String... args) throws Exception
    {
        System.exit(new Main(args).run());
    }
}