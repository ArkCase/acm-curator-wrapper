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

import com.armedia.acm.curator.wrapper.module.Session;
import com.armedia.acm.curator.wrapper.tools.Tools;

public class SessionCfg
{
    private String connect = null;
    private int sessionTimeout = 0;
    private int connectionTimeout = 0;
    private String basePath = null;
    private RetryCfg retry = new RetryCfg();

    public String getConnect()
    {
        return this.connect;
    }

    public void setConnect(String connect)
    {
        this.connect = connect;
    }

    public int getSessionTimeout()
    {
        if (this.sessionTimeout < 0)
        {
            this.sessionTimeout = 0;
        }
        return this.sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout)
    {
        this.sessionTimeout = Math.max(0, sessionTimeout);
    }

    public int getConnectionTimeout()
    {
        if (this.connectionTimeout < 0)
        {
            this.connectionTimeout = 0;
        }
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = Math.max(0, connectionTimeout);
    }

    public String getBasePath()
    {
        return this.basePath;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public RetryCfg getRetry()
    {
        if (this.retry == null)
        {
            this.retry = new RetryCfg();
        }
        return this.retry;
    }

    public void setRetry(RetryCfg retry)
    {
        this.retry = Tools.ifNull(retry, RetryCfg::new);
    }

    public Session build() throws InterruptedException
    {
        // This helps ensure we have a value
        RetryCfg retry = getRetry();
        return new Session.Builder() //
                .connect(this.connect) //
                .sessionTimeout(this.sessionTimeout) //
                .connectionTimeout(this.connectionTimeout) //
                .basePath(this.basePath) //
                .retryCount(retry.getCount()) //
                .retryDelay(retry.getDelay()) //
                .build() //
        ;
    }
}
