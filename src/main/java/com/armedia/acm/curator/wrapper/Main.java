package com.armedia.acm.curator.wrapper;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import com.armedia.acm.curator.wrapper.conf.Cfg;
import com.armedia.acm.curator.wrapper.conf.SessionCfg;
import com.armedia.acm.curator.wrapper.tools.Tools;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String CONF_SYSPROP = "arkcase.curator.wrapper.conf";
    private static final String CONF_ENVVAR = Main.CONF_SYSPROP.replace('.', '_').toUpperCase();
    private static final File DEFAULT_CONFIG;
    static
    {
        File f = new File(".");
        try
        {
            f = f.getCanonicalFile();
        }
        catch (Exception e)
        {
            // ignore
        }
        finally
        {
            f = f.getAbsoluteFile();
        }
        Main.LOG.trace("Default config: [{}]", f);
        DEFAULT_CONFIG = f;
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
            CommandLine cmdLine = new GnuParser().parse(Main.OPTIONS, args);

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
                    : Main.getDefaultConfig() //
            );
            final Reader cfgReader;
            if ("-".equals(cfgStr))
            {
                // Reading from stdin
                Main.LOG.info("Reading the configuration from stdin (charset = {})", charset.name());
                cfgReader = new InputStreamReader(System.in, charset);
            }
            else
            {
                // Reading from the referenced file/stream
                Main.LOG.info("Reading the configuration from the file at [{}] (charset = {})", cfgStr, charset.name());
                cfgReader = Files.newBufferedReader(new File(cfgStr).toPath(), charset);
            }

            Main.LOG.debug("Parsing the configuration YAML");
            final Cfg cfg;
            try (Reader r = cfgReader)
            {
                Representer representer = new Representer();
                representer.getPropertyUtils().setSkipMissingProperties(true);
                cfg = new Yaml(representer).loadAs(r, Cfg.class);
            }
            Main.LOG.trace("Configuration parsed");

            Main.LOG.debug("Launching the main loop");
            final SessionCfg session = cfg.getSession();
            return new Wrapper(session::build, cfg.getWrapper()).run();
        }
        catch (Exception e)
        {
            Main.usage();
            throw e;
        }
    }

    private static void usage()
    {
        new HelpFormatter().printHelp("wrapper", Main.OPTIONS, true);
    }

    public static void main(String... args) throws Exception
    {
        System.exit(Main.run());
    }
}