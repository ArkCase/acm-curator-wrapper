package com.armedia.acm.curator.recipe;

/*-
 * #%L
 * acm-curator-wrapper
 * %%
 * Copyright (C) 2023 - 2025 ArkCase LLC
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.armedia.acm.curator.Session;

public abstract class FileTransfer extends Recipe
{
    protected FileTransfer(Session session)
    {
        super(session);
    }

    protected FileTransfer(Session session, String name)
    {
        super(session, name);
    }

    protected boolean parseRecursive(String str)
    {
        return Strings.CI.equals("true", StringUtils.trim(StringUtils.lowerCase(str)));
    }

    public int execute(String target)
    {
        return execute(target, false);
    }

    public abstract int execute(String counterpart, boolean recursive);
}
