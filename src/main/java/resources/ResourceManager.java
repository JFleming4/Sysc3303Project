package resources;

import logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceManager {
	private static final Logger LOG = new Logger("ResourceManager");
	private static final String RESOURCE_DIR = "resources";
	private Path directory;


	/**
	 * Initialize Resource Directory
	 * @param directoryName
	 */
	public ResourceManager(String directoryName) throws IOException{
		directory = Paths.get(System.getProperty("user.dir"), RESOURCE_DIR, directoryName);
		LOG.logVerbose("Resource Manager created with directory " + getFullPath());

		if(Files.notExists(directory) && !directory.toFile().mkdirs()) {
			LOG.logQuiet("The directory '" + getFullPath() + "' does not exist and failed to be created.");
			throw new IOException("Failed to create the directory " + getFullPath());
		}
	}

	/**
	 *
	 * @param filename The name of the file to read from
	 * @return bytes read from file
	 * @throws IOException
	 */
	public synchronized byte[] readFileToBytes(String filename) throws IOException {
		File file = Paths.get(directory.toString(), filename).toFile();
		LOG.logVerbose("Reading File to byte array. File:  " + file.getCanonicalPath());
	    FileInputStream fileInputStream = new FileInputStream(file);
	    long fileLength = file.length();
	    byte [] fileBytes = new byte [(int) fileLength];
	    fileInputStream.read(fileBytes, 0, (int) fileLength);
	    fileInputStream.close();
		LOG.logVerbose("Successfully read file. (" + file.getCanonicalPath() + ")");
	    return fileBytes;
	}

	/**
	 *
	 * @param filename The name of the file to read from
	 * @return bytes read from file
	 * @throws IOException
	 */
	public synchronized String readFileToString(String filename) throws IOException {
		return new String(readFileToBytes(filename));
	}

    /**
     * @return the full path of the resource directory.
     */
	public synchronized String getFullPath()
	{
		return directory.toAbsolutePath().toString();
	}

	/**
	 * Check if a file exists in the resource directory
	 * @param filename name of file to check
	 * @return boolean
	 */
	public synchronized boolean fileExists(String filename) {
	    return Paths.get(directory.toString(), filename).toFile().exists();
	}

	/**
	 * Writes bytes to the file (The current implementation will not overwrite files)
	 * @param filename The name of the file
	 * @param data The bytes to write
	 * @throws IOException
	 */
	public synchronized void writeBytesToFile(String filename, byte[] data) throws IOException {
		File file = Paths.get(directory.toString(), filename).toFile();
		LOG.logVerbose("Writing byte array to File. File:  " + file.getCanonicalPath());

		if(!file.exists() && !file.createNewFile()) {
			LOG.logVerbose("File does not exist and failed to be created. (" + file.getCanonicalPath() + ")");
			throw new IOException("Failed to create file (" + file.getCanonicalPath() + ")");
		}

		// Append block to file
		FileOutputStream  fileOutputStream = new FileOutputStream(file, true);
		try {
			fileOutputStream.write(data);
		} finally {
			fileOutputStream.close();
		}
		LOG.logVerbose("Successfully wrote data block to file (" + file.getCanonicalPath() + ")");
	}

}
