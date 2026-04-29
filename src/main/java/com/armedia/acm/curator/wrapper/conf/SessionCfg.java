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

import org.apache.commons.lang3.StringUtils;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class SessionCfg
{

    private String connect = null;
    private String instanceId = null;
    private int sessionTimeout = Session.sanitizeSessionTimeout(0);
    private int connectionTimeout = Session.sanitizeConnectionTimeout(0);
    private RetryCfg retry = new RetryCfg();

    public String getConnect()
    {
        return this.connect;
    }

    public void setConnect(String connect)
    {
        this.connect = connect;
    }

    public String getInstanceId()
    {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId)
    {
        this.instanceId = StringUtils.defaultIfBlank(instanceId, StringUtils.EMPTY);
    }

    public int getSessionTimeout()
    {
        return this.sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout)
    {
        this.sessionTimeout = Session.sanitizeSessionTimeout(sessionTimeout);
    }

    public int getConnectionTimeout()
    {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = Session.sanitizeConnectionTimeout(connectionTimeout);
    }

    public RetryCfg getRetry()
    {
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
                .retryCount(retry.getCount()) //
                .retryDelay(retry.getDelay()) //
                .build() //
        ;
    }
}
