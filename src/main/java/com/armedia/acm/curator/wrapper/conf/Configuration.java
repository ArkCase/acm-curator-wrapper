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

public class Configuration
{
    public static Configuration load(ReadableByteChannel c)
    {
        return Configuration.load(c, null);
    }

    public static Configuration load(ReadableByteChannel c, Charset charset)
    {
        return Configuration.load(Channels.newInputStream(c), charset);
    }

    public static Configuration load(InputStream in) throws IOException
    {
        return Configuration.load(in, null);
    }

    public static Configuration load(InputStream in, Charset charset)
    {
        if (charset == null)
        {
            charset = StandardCharsets.UTF_8;
        }
        return Configuration.load(new InputStreamReader(in, charset));
    }

    public static Configuration load(Reader r)
    {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        return new Yaml(representer).loadAs(r, Configuration.class);
    }

    private static final OperationMode DEFAULT_MODE = OperationMode.leader;

    private CuratorSessionCfg session = new CuratorSessionCfg();
    private CommandCfg command = new CommandCfg();
    private OperationMode mode = OperationMode.direct;
    private LeaderCfg leader = new LeaderCfg();
    private MutexCfg mutex = new MutexCfg();
    private BarrierCfg barrier = new BarrierCfg();

    public CuratorSessionCfg getSession()
    {
        return this.session;
    }

    public CommandCfg getCommand()
    {
        return this.command;
    }

    public OperationMode getMode()
    {
        return (this.mode != null ? this.mode : Configuration.DEFAULT_MODE);
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