package resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import logging.Logger;

import java.io.IOException;

public class Configuration {
    private static final Logger LOG = new Logger("Configuration");
    private static final String CONFIG_NAME = "config.json";
    private static final String CONFIG_DIR = "config";
    public static final Configuration GLOBAL_CONFIG = loadConfiguration(CONFIG_NAME);

    public final int SERVER_PORT;
    public final int SIMULATOR_PORT;
    public final boolean DEBUG_MODE;
    public final String CLIENT_RESOURCE_DIR;
    public final String SERVER_RESOURCE_DIR;

    public Configuration()
    {
        // Set all default values
        DEBUG_MODE = false;
        SERVER_PORT = 69;
        SIMULATOR_PORT = 23;
        CLIENT_RESOURCE_DIR = "client";
        SERVER_RESOURCE_DIR = "server";
    }

    /**
     * Load configuration from file
     * @param configName The file name in the config resource directory
     * @return The Configuration object
     */
    public static Configuration loadConfiguration(String configName)
    {
        try {
            ResourceManager manager = new ResourceManager(CONFIG_DIR);
            Gson gson = new GsonBuilder().create();

            // Read and de-serialize JSON file into configuration object
            String contents = manager.readFileToString(configName);
            return gson.fromJson(contents, Configuration.class);
        }
        catch (IOException | JsonSyntaxException e)
        {
            LOG.logQuiet("Failed to load application configuration file. Defaults will be used. (" + e.getLocalizedMessage() + ")");
        }

        return new Configuration();
    }
}
