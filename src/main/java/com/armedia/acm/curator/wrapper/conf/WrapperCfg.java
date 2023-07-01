package com.armedia.acm.curator.wrapper.conf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.armedia.acm.curator.wrapper.tools.Tools;

public class WrapperCfg
{
    private static final OperationMode DEFAULT_MODE = OperationMode.leader;
    private static final Map<String, Object> NO_PARAMS = Collections.emptyMap();

    private OperationMode mode = WrapperCfg.DEFAULT_MODE;
    private String name = null;
    private long timeout = 0;
    private Map<String, Object> params = WrapperCfg.NO_PARAMS;
    private ExecCfg exec = new ExecCfg();

    public OperationMode getMode()
    {
        return Tools.coalesce(this.mode, WrapperCfg.DEFAULT_MODE);
    }

    public void setMode(OperationMode mode)
    {
        this.mode = Tools.coalesce(mode, WrapperCfg.DEFAULT_MODE);
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getTimeout()
    {
        if (this.timeout < 0)
        {
            this.timeout = 0;
        }
        return this.timeout;
    }

    public void setTimeout(long timeout)
    {
        this.timeout = Math.max(0, timeout);
    }

    public Map<String, Object> getParams()
    {
        if (this.params == null)
        {
            this.params = new LinkedHashMap<>();
        }
        return this.params;
    }

    public void setParams(Map<String, Object> params)
    {
        this.params = Tools.ifNull(params, LinkedHashMap::new);
    }

    public ExecCfg getExec()
    {
        if (this.exec == null)
        {
            this.exec = new ExecCfg();
        }
        return this.exec;
    }

    public void setExec(ExecCfg exec)
    {
        this.exec = Tools.ifNull(this.exec, ExecCfg::new);
    }
}