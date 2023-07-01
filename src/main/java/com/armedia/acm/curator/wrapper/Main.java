package com.armedia.acm.curator.wrapper;

/*-
 * #%L
 * acm-config-server
 * %%
 * Copyright (C) 2019 ArkCase LLC
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.acm.curator.wrapper.conf.CommandCfg;
import com.armedia.acm.curator.wrapper.conf.Configuration;
import com.armedia.acm.curator.wrapper.conf.OperationMode;
import com.armedia.acm.curator.wrapper.module.CuratorSession;
import com.armedia.acm.curator.wrapper.module.Leader;
import com.armedia.acm.curator.wrapper.module.Mutex;

public class Main
{

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Configuration cfg;

    private Main(String... args)
    {
        // TODO: Find our configurations ... either from the command line arguments,
        // environment, or system properties
        this.cfg = null;
    }

    private AutoCloseable createWrapper(Configuration cfg, CuratorSession session) throws Exception
    {
        switch (cfg.getMode())
        {
        case leader:
            this.log.info("Creating a leadership selector");
            return new Leader(cfg.getLeader(), session).acquire();

        case mutex:
            this.log.info("Creating a mutex lock");
            return new Mutex(cfg.getMutex(), session).acquire();

        default:
            this.log.info("No-op wrapper created");
            return this::noop;
        }
    }

    private void noop()
    {
        // Specifically do nothing...
    }

    public int run() throws Exception
    {
        CommandCfg cmd = this.cfg.getCommand();
        if (this.cfg.getMode() == OperationMode.direct)
        {
            // We're not working any of our magic... just execute the wrapped command
            return cmd.run();
        }

        try (CuratorSession session = new CuratorSession(this.cfg))
        {
            if (!session.isEnabled())
            {
                // Still not working our magic b/c we have no clustering configuration
                this.log.info("Running in standalone mode");
                return cmd.run();
            }

            // This is the new, "clusterable" code path
            this.log.info("Running in clustered mode");
            try (AutoCloseable c = createWrapper(this.cfg, session))
            {
                return cmd.run();
            }
            catch (Exception e)
            {
                this.log.error("Exception caught during wrapping or command execution", e);
                return 1;
            }
        }
    }

    public static void main(String... args) throws Exception
    {
        System.exit(new Main(args).run());
    }
}