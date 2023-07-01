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

import com.armedia.acm.curator.wrapper.tools.Tools;

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

    private SessionCfg session = new SessionCfg();
    private WrapperCfg wrapper = new WrapperCfg();

    public SessionCfg getSession()
    {
        if (this.session == null)
        {
            this.session = new SessionCfg();
        }
        return this.session;

    }

    public void setSession(SessionCfg session)
    {
        this.session = Tools.ifNull(this.session, SessionCfg::new);
    }

    public WrapperCfg getWrapper()
    {
        if (this.wrapper == null)
        {
            this.wrapper = new WrapperCfg();
        }
        return this.wrapper;
    }

    public void setWrapper(WrapperCfg wrapper)
    {
        this.wrapper = Tools.ifNull(this.wrapper, WrapperCfg::new);
    }
}