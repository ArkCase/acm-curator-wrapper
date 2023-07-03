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

import java.util.LinkedHashMap;
import java.util.Map;

import com.armedia.acm.curator.tools.Tools;

public class ExecCfg
{
    private String workdir = null;
    private Object command = null;
    private Map<String, String> env = null;
    private boolean cleanEnv = false;
    private RedirectCfg redirect = new RedirectCfg();

    public String getWorkdir()
    {
        if (this.workdir == null)
        {
            this.workdir = Tools.CWD.getPath();
        }
        return this.workdir;
    }

    public void setWorkdir(String workdir)
    {
        this.workdir = Tools.ifEmpty(workdir, Tools.CWD::getPath);
    }

    public Object getCommand()
    {
        return this.command;
    }

    public void setCommand(Object command)
    {
        this.command = command;
    }

    public Map<String, String> getEnv()
    {
        if (this.env == null)
        {
            this.env = new LinkedHashMap<>();
        }
        return this.env;
    }

    public void setEnv(Map<String, String> env)
    {
        this.env = Tools.ifNull(env, LinkedHashMap::new);
    }

    public boolean isCleanEnv()
    {
        return this.cleanEnv;
    }

    public void setCleanEnv(boolean cleanEnv)
    {
        this.cleanEnv = cleanEnv;
    }

    public RedirectCfg getRedirect()
    {
        if (this.redirect == null)
        {
            this.redirect = new RedirectCfg();
        }
        return this.redirect;
    }

    public void setRedirect(RedirectCfg redirect)
    {
        this.redirect = Tools.ifNull(redirect, RedirectCfg::new);
    }
}