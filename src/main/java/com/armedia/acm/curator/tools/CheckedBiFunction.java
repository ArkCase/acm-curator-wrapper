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

import java.util.function.BiFunction;

@FunctionalInterface
public interface CheckedBiFunction<A, B, R> extends BiFunction<A, B, R>
{
    public R applyChecked(A a, B b) throws Exception;

    @Override
    public default R apply(A a, B b)
    {
        try
        {
            return applyChecked(a, b);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}