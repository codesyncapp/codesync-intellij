package org.intellij.sdk.codesync.configuration;

public class ConfigurationFactory {
    final static private String env = "env";
    final static private String dev = "dev";
    final static private String prod = "prod";
    final static private String test = "test";

    public static Configuration getConfiguration() {
        String value = System.getenv(env);
        if (value == null) {
            value = prod;
        }
        return getConfiguration(value);
    }

    public static Configuration getConfiguration(String env) {
        switch (env) {
            case dev:
                return DevConfiguration.getInstance();
            case prod:
                return ProdConfiguration.getInstance();
            case test:
                return TestConfiguration.getInstance();
            default:
                return ProdConfiguration.getInstance();
        }
    }
}
