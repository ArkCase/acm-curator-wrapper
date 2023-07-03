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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.acm.curator.Session;
import com.armedia.acm.curator.tools.Tools;

public class SessionCfg
{
    public static final String DEFAULT_PORT = String.valueOf(2181);
    public static final int MIN_SESSION_TIMEOUT = 1000;
    public static final int DEFAULT_SESSION_TIMEOUT = 15000;
    public static final int MIN_CONNECTION_TIMEOUT = 100;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    protected static final Pattern HOSTPORT_PARSER = Pattern
            .compile("^((?:[a-z0-9][-a-z0-9]*)?[a-z0-9](?:[.](?:[a-z0-9][-a-z0-9]*)?[a-z0-9])*)(?::([1-9][0-9]*))?$",
                    Pattern.CASE_INSENSITIVE);

    protected static final String parseHostPort(String hostport)
    {
        if ((hostport == null) || Tools.isEmpty(hostport))
        {
            return null;
        }

        Matcher m = SessionCfg.HOSTPORT_PARSER.matcher(hostport);
        if (!m.matches())
        {
            throw new IllegalArgumentException(String.format("Invalid host:port string: [%s]", hostport));
        }

        String host = m.group(1);
        String port = Tools.ifEmpty(m.group(2), SessionCfg.DEFAULT_PORT);
        return String.format("%s:%s", host, port);
    }

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
        if (!Tools.isEmpty(connect))
        {
            // Validate that it's a CSV list of host:port pairs, and sanitize
            // The port is optional and will be defaulted to 2181
            StringBuilder sb = new StringBuilder(connect.length());
            for (String s : connect.split(","))
            {
                s = SessionCfg.parseHostPort(s.trim());
                if (s == null)
                {
                    continue;
                }

                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(s);
            }
            connect = (Tools.isEmpty(sb) ? null : sb.toString());
        }
        else
        {
            connect = null;
        }
        this.connect = connect;
    }

    public int getSessionTimeout()
    {
        if (this.sessionTimeout <= 0)
        {
            this.sessionTimeout = SessionCfg.DEFAULT_SESSION_TIMEOUT;
        }
        else if (this.sessionTimeout < SessionCfg.MIN_SESSION_TIMEOUT)
        {
            this.sessionTimeout = SessionCfg.MIN_SESSION_TIMEOUT;
        }
        return this.sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout)
    {
        if (sessionTimeout <= 0)
        {
            sessionTimeout = SessionCfg.DEFAULT_SESSION_TIMEOUT;
        }
        this.sessionTimeout = Math.max(SessionCfg.MIN_SESSION_TIMEOUT, sessionTimeout);
    }

    public int getConnectionTimeout()
    {
        if (this.connectionTimeout <= 0)
        {
            this.connectionTimeout = SessionCfg.DEFAULT_CONNECTION_TIMEOUT;
        }
        else if (this.connectionTimeout < SessionCfg.MIN_CONNECTION_TIMEOUT)
        {
            this.connectionTimeout = SessionCfg.MIN_CONNECTION_TIMEOUT;
        }
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        if (connectionTimeout <= 0)
        {
            connectionTimeout = SessionCfg.DEFAULT_CONNECTION_TIMEOUT;
        }
        this.connectionTimeout = Math.max(SessionCfg.MIN_CONNECTION_TIMEOUT, connectionTimeout);
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
