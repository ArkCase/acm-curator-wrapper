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
package com.armedia.acm.curator.wrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.help.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.env.EnvScalarConstructor;
import org.yaml.snakeyaml.representer.Representer;

import com.armedia.acm.curator.tools.SysPropEnvScalarConstructor;
import com.armedia.acm.curator.tools.Tools;
import com.armedia.acm.curator.wrapper.conf.SessionCfg;
import com.armedia.acm.curator.wrapper.conf.WrapperCfg;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String CONF_SYSPROP = "arkcase.curator.wrapper.conf";
    private static final String CONF_ENVVAR = Main.CONF_SYSPROP.replace('.', '_').toUpperCase();
    private static final File DEFAULT_CONFIG;
    static
    {
        DEFAULT_CONFIG = new File(Tools.CWD, "curator-wrapper.yaml");
        Main.LOG.trace("Default config: [{}]", Main.DEFAULT_CONFIG);
    }

    public static class Cfg
    {
        private SessionCfg session = new SessionCfg();
        private WrapperCfg wrapper = new WrapperCfg();

        public SessionCfg getSession()
        {
            if (this.session == null)
            {
                this.session = new SessionCfg();
            }
            return this.session;
        }

        public void setSession(SessionCfg session)
        {
            this.session = Tools.ifNull(session, SessionCfg::new);
        }

        public WrapperCfg getWrapper()
        {
            if (this.wrapper == null)
            {
                this.wrapper = new WrapperCfg();
            }
            return this.wrapper;
        }

        public void setWrapper(WrapperCfg wrapper)
        {
            this.wrapper = Tools.ifNull(wrapper, WrapperCfg::new);
        }
    }

    private static final Options OPTIONS = new Options() //
            .addOption("c", "config", true, "The configuration file to use") //
            .addOption("e", "encoding", true, "The encoding the configuration file or stream is in (default is UTF-8)") //
            .addOption("h", "help", true, "Descibe the tool's usage") //
    ;

    private static String getDefaultConfig()
    {
        // The configuration file will either be (in order of precedence):
        // Pointed to by the arkcase.curator.wrapper.conf sysprop
        // Pointed to by the ARKCASE_CURATOR_WRAPPER_CONF envvar
        // The file curator-wrapper.yaml in the current directory

        String cfgStr = null;

        cfgStr = System.getProperty(Main.CONF_SYSPROP);
        if (!Tools.isEmpty(cfgStr))
        {
            Main.LOG.trace("Found the configuration system property with value [{}]", cfgStr);
            return cfgStr;
        }

        cfgStr = System.getenv(Main.CONF_ENVVAR);
        if (!Tools.isEmpty(cfgStr))
        {
            Main.LOG.trace("Found the configuration environment variable with value [{}]", cfgStr);
            return cfgStr;
        }

        Main.LOG.trace("No sysprop or envvar found, using the default configuration at [{}]", Main.DEFAULT_CONFIG.getPath());
        return Main.DEFAULT_CONFIG.getPath();
    }

    private static int run(String... args) throws Exception
    {
        Main.LOG.trace("Parsing the command line: {}", Arrays.toString(args));
        try
        {
            CommandLine cmdLine = new DefaultParser().parse(Main.OPTIONS, args);

            if (cmdLine.hasOption('h'))
            {
                Main.usage();
                return 1;
            }

            Charset charset = StandardCharsets.UTF_8;
            if (cmdLine.hasOption('e'))
            {
                try
                {
                    String cs = cmdLine.getOptionValue('e');
                    Main.LOG.debug("Attempting to use charset [{}]", cs);
                    charset = Charset.forName(cs);
                }
                catch (Exception e)
                {
                    Main.usage();
                    Main.LOG.error("Failed to identify the required charset", e);
                    return 1;
                }
            }

            String cfgStr = (cmdLine.hasOption('c') //
                    ? cmdLine.getOptionValue('c') //
                    : null //
            );
            final Reader cfgReader;
            if ("-".equals(cfgStr))
            {
                // Reading from stdin
                Main.LOG.info("Reading the configuration from stdin (charset = {})", charset.name());
                cfgReader = new InputStreamReader(System.in, charset);
            }
            else if (cfgStr != null)
            {
                // Reading from the referenced file/stream
                Main.LOG.info("Reading the configuration from the file at [{}] (charset = {})", cfgStr, charset.name());
                cfgReader = Files.newBufferedReader(new File(cfgStr).toPath(), charset);
            }
            else
            {
                cfgStr = Main.getDefaultConfig();
                File f = new File(cfgStr);
                if (f.exists())
                {
                    Main.LOG.info("Reading the configuration from the file at [{}] (charset = {})", cfgStr, charset.name());
                    cfgReader = Files.newBufferedReader(new File(cfgStr).toPath(), charset);
                }
                else
                {
                    Main.LOG.info("No configuration detected, using defaults");
                    cfgReader = null;
                }
            }

            final Cfg cfg;
            if (cfgReader != null)
            {
                Main.LOG.debug("Parsing the configuration YAML");
                try (Reader r = cfgReader)
                {
                    final Constructor constructor = new SysPropEnvScalarConstructor();
                    Representer representer = new Representer(new DumperOptions());
                    representer.getPropertyUtils().setSkipMissingProperties(true);
                    Yaml yaml = new Yaml(constructor, representer, new DumperOptions());
                    yaml.addImplicitResolver(EnvScalarConstructor.ENV_TAG, EnvScalarConstructor.ENV_FORMAT, "$");
                    cfg = yaml.loadAs(r, Cfg.class);
                }
                Main.LOG.trace("Configuration parsed");
            }
            else
            {
                cfg = new Cfg();
            }

            Main.LOG.debug("Launching the main loop");
            final SessionCfg session = cfg.getSession();
            final Instant start = Instant.now();
            int ret = new Wrapper(session::build, cfg.getWrapper()).run();
            Main.LOG.info("Command exited with status {} after {}", ret, Duration.between(start, Instant.now()));
            return ret;
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    private static void usage() throws IOException
    {
        HelpFormatter.builder().get().printHelp("wrapper", "A simple Curator API Wrapper", Main.OPTIONS, "", false);
    }

    public static void main(String... args) throws Exception
    {
        System.exit(Main.run(args));
    }
}
