package resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceManager {
	private static final String RESOURCE_DIR = "resources";
	private Path directory;


	/**
	 * Initialize Resource Directory
	 * @param directoryName
	 */
	public ResourceManager(String directoryName) {
		directory = Paths.get(System.getProperty("user.dir"), RESOURCE_DIR, directoryName);
		if(Files.notExists(directory)) {
			try {
				if(!directory.toFile().mkdirs()) throw new IOException("Directory Structure not created");
			}
			catch(IOException e) {
				e.printStackTrace();
			}
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
	    FileInputStream fileInputStream = new FileInputStream(file);
	    long fileLength = file.length();
	    byte [] fileBytes = new byte [(int) fileLength];
	    fileInputStream.read(fileBytes, 0, (int) fileLength);
	    fileInputStream.close();
	    return fileBytes;
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
		if(!file.exists()) {
			if(!file.createNewFile()) throw new IOException("Cannot Create File");
		}
		FileOutputStream  fileOutputStream = new FileOutputStream(file, true);
		try {
			fileOutputStream.write(data);
		} finally {
			fileOutputStream.close();
		}
	}

}
