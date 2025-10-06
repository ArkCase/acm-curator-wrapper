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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.armedia.acm.curator.tools.Tools;

public class ExecCfg
{
    private String workdir = Tools.CWD.getPath();
    private Object command = null;
    private Map<String, String> env = new LinkedHashMap<>();
    private boolean cleanEnv = false;
    private RedirectCfg redirect = new RedirectCfg();

    public String getWorkdir()
    {
        return this.workdir;
    }

    public ExecCfg setWorkdir(String workdir)
    {
        this.workdir = Tools.ifEmpty(workdir, Tools.CWD::getPath);
        return this;
    }

    public Object getCommand()
    {
        return this.command;
    }

    public ExecCfg setCommand(Object command)
    {
        if ((command != null) && (!(command instanceof String) && !(command instanceof Collection) && !command.getClass().isArray()))
        {
            throw new IllegalArgumentException(
                    String.format("The command must be a string, a Collection, or an array: %s", command.getClass()));
        }
        this.command = command;
        return this;
    }

    public Map<String, String> getEnv()
    {
        return this.env;
    }

    public ExecCfg setEnv(Map<String, String> env)
    {
        this.env = Tools.ifNull(env, LinkedHashMap::new);
        return this;
    }

    public boolean isCleanEnv()
    {
        return this.cleanEnv;
    }

    public ExecCfg setCleanEnv(boolean cleanEnv)
    {
        this.cleanEnv = cleanEnv;
        return this;
    }

    public RedirectCfg getRedirect()
    {
        return this.redirect;
    }

    public ExecCfg setRedirect(RedirectCfg redirect)
    {
        this.redirect = Tools.ifNull(redirect, RedirectCfg::new);
        return this;
    }
}