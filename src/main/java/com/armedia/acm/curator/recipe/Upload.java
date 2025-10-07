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
package com.armedia.acm.curator.recipe;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.armedia.acm.curator.Session;

public class Upload extends FileTransfer
{
    public Upload(Session session)
    {
        super(session);
    }

    public Upload(Session session, String name)
    {
        super(session, name);
    }

    @Override
    public int execute(String source, boolean recursive)
    {
        if (!isSessionEnabled())
        {
            this.log.warn("The current session is not enabled, cannot upload any resources");
            return 1;
        }

        try (InputStream in = Files.newInputStream(Path.of(source), StandardOpenOption.READ))
        {
            getSession().getClient().setData().idempotent().forPath(this.path, in.readAllBytes());
            return 0;
        }
        catch (Exception e)
        {
            this.log.error("Failed to upload the contents of [{}] into [{}]", source, this.name, e);
            return 1;
        }
    }
}
