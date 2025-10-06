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

public class RedirectCfg
{
    String stdin = null;
    String stdout = null;
    String stderr = null;

    public String getStdin()
    {
        return this.stdin;
    }

    public RedirectCfg setStdin(String stdin)
    {
        this.stdin = stdin;
        return this;
    }

    public String getStdout()
    {
        return this.stdout;
    }

    public RedirectCfg setStdout(String stdout)
    {
        this.stdout = stdout;
        return this;
    }

    public String getStderr()
    {
        return this.stderr;
    }

    public RedirectCfg setStderr(String stderr)
    {
        this.stderr = stderr;
        return this;
    }
}
