package resources;

import exceptions.ResourceException;
import logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
	private static final Logger LOG = new Logger("ResourceManager");
	private static final String RESOURCE_DIR = "resources";
	private Path directory;
	private Map<Path, ResourceFile> resourceFileMap;

	/**
	 * Initialize Resource Directory
	 * @param directoryName The name of the resource directory
	 */
	public ResourceManager(String directoryName) throws IOException{
		resourceFileMap = new HashMap<>();
		directory = Paths.get(System.getProperty("user.dir"), RESOURCE_DIR, directoryName);
		LOG.logVerbose("Resource Manager created with directory " + getFullPath());

		if(Files.notExists(directory) && !directory.toFile().mkdirs()) {
			LOG.logQuiet("The directory '" + getFullPath() + "' does not exist and failed to be created.");
			throw new IOException("Failed to create the directory " + getFullPath());
		}
	}

    /**
     * @return the full path of the resource directory.
     */
	public synchronized String getFullPath()
	{
		return directory.toAbsolutePath().toString();
	}

	public synchronized boolean isValidResource(String fileName)
	{
		Path resourcePath = Paths.get(directory.toString(), fileName).normalize();

		// Check to make sure the resource path is contained within
		// the resource directory (ex: user didn't type '../' as file name)
		return resourcePath.startsWith(directory);
	}

	/**
	 * Gets the ResourceFile given the fileName.
	 * ResourceFile objects are cached, and mapped to a full path so that ResourceFile objects only
	 * need to be resolved once
	 *
	 * @param fileName The file name of the resource
	 * @return The ResourceFile object representing the resource file
	 * @throws IOException If the given filename resolves to a directory outside of the resource directory
	 * (usually caused by a filename starting with '../')
	 */
    public synchronized ResourceFile getFile(String fileName) throws ResourceException {
		Path resourcePath = Paths.get(directory.toString(), fileName).normalize();

		ResourceFile file = resourceFileMap.get(resourcePath);

		// Check to make sure the resource path is contained within
		// the resource directory (ex: user didn't type '../' as file name)
		if(!isValidResource(fileName))
			throw new ResourceException("The given filename '" + fileName + "' resolves to outside the resource directory");

		if(file == null) {
			file = new ResourceFile(resourcePath);
			resourceFileMap.put(resourcePath, file);
		}

		return file;
    }
}
