package com.armedia.acm.curator.wrapper.conf;

import com.armedia.acm.curator.tools.Tools;

public class MainCfg
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
        this.session = Tools.ifNull(session, SessionCfg::new);
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
        this.wrapper = Tools.ifNull(wrapper, WrapperCfg::new);
    }
}