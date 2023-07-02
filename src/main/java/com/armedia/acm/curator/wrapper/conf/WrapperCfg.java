/*-
 * #%L
 * acm-curator-wrapper
 * %%
 * Copyright (C) 2023 ArkCase LLC
 * %%
 * This file is part of the ArkCase software.
 *
 * If the software was purchased under a paid ArkCase license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * ArkCase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ArkCase is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package com.armedia.acm.curator.wrapper.conf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.armedia.acm.curator.tools.Tools;

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
