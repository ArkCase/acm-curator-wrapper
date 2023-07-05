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