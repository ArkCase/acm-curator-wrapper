package com.armedia.acm.curator.wrapper.conf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

public class Cfg
{
    public static Cfg load(ReadableByteChannel c)
    {
        return Cfg.load(c, null);
    }

    public static Cfg load(ReadableByteChannel c, Charset charset)
    {
        return Cfg.load(Channels.newInputStream(c), charset);
    }

    public static Cfg load(InputStream in) throws IOException
    {
        return Cfg.load(in, null);
    }

    public static Cfg load(InputStream in, Charset charset)
    {
        if (charset == null)
        {
            charset = StandardCharsets.UTF_8;
        }
        return Cfg.load(new InputStreamReader(in, charset));
    }

    public static Cfg load(Reader r)
    {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        return new Yaml(representer).loadAs(r, Cfg.class);
    }

    private static final OperationMode DEFAULT_MODE = OperationMode.leader;

    private SessionCfg session = new SessionCfg();
    private CommandCfg command = new CommandCfg();
    private OperationMode mode = OperationMode.direct;
    private LeaderCfg leader = new LeaderCfg();
    private MutexCfg mutex = new MutexCfg();
    private BarrierCfg barrier = new BarrierCfg();

    public SessionCfg getSession()
    {
        return this.session;
    }

    public CommandCfg getCommand()
    {
        return this.command;
    }

    public OperationMode getMode()
    {
        return (this.mode != null ? this.mode : Cfg.DEFAULT_MODE);
    }

    public LeaderCfg getLeader()
    {
        return this.leader;
    }

    public MutexCfg getMutex()
    {
        return this.mutex;
    }

    public BarrierCfg getBarrier()
    {
        return this.barrier;
    }
}