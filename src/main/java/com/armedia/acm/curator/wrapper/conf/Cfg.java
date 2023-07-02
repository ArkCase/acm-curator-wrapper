package com.armedia.acm.curator.wrapper.conf;

import com.armedia.acm.curator.wrapper.tools.Tools;

public class Cfg
{
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