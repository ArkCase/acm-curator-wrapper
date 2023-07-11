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
package com.armedia.acm.curator.tools;

import org.yaml.snakeyaml.env.EnvScalarConstructor;

/**
 * <p>
 * This class overrides the {@link #getEnv(String)} method to support first searching for a system property (using
 * {@link System#getProperty(String)}). If this method returns <code>null</code> the default behavior is followed.
 * </p>
 *
 * @author diego.rivera@armedia.com
 *
 */
public class SysPropEnvScalarConstructor extends EnvScalarConstructor
{
    /**
     * <p>
     * Get the value of the system property or environment variable, whichever is found first (in that order).
     * </p>
     *
     * @param key
     *            - the name of the system property or environment variable
     * @return value or null if not set
     */
    @Override
    public String getEnv(String key)
    {
        // Use system properties first, since these need to be
        // explicitly set
        String v = System.getProperty(key);
        if (v != null)
        {
            return v;
        }

        // No system property matched, use the default behavior
        return super.getEnv(key);
    }
}
