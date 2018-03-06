package resources;

import logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Wrapper class for File Object.
 */
public class ResourceFile extends File {

    private static final Logger LOG = new Logger("ResourceFile");

    /**
     * Creates a resource file given the full path URI.
     * This constructor should only be available to the ResourceManager (hence package-private)
     * @param path The path of the resource file
     */
    ResourceFile(Path path)
    {
        super(path.normalize().toUri());
    }

    /**
     * Writes bytes to the file (The current implementation will not overwrite files)
     * @param data The bytes to write
     * @throws IOException
     */
    public synchronized void writeBytesToFile(byte[] data) throws IOException {
        LOG.logVerbose("Writing byte array to File. File:  " + getCanonicalPath());

        if(!exists() && !createNewFile()) {
            LOG.logVerbose("File does not exist and failed to be created. (" + getCanonicalPath() + ")");
            throw new IOException("Failed to create file (" + getCanonicalPath() + ")");
        }

        if(getUsableSpace() < data.length) {
            throw new IOException("Not enough usable space");
        }

        // Append block to file
        // Use try-with-resource (auto-closes the stream when done)
        try (FileOutputStream fileOutputStream = new FileOutputStream(this, true)) {
            fileOutputStream.write(data);
        }

        LOG.logVerbose("Successfully wrote data block to file (" + getCanonicalPath() + ")");
    }

    /**
     * Read Resource file to byte array
     * @return bytes read from file
     * @throws IOException
     */
    public synchronized byte[] readFileToBytes() throws IOException {
        LOG.logVerbose("Reading File to byte array. File:  " + getCanonicalPath());

        // Try-with-resource to ensure stream gets closed
        try(FileInputStream fileInputStream = new FileInputStream(this))
        {
            long fileLength = length();
            byte[] fileBytes = new byte[(int) fileLength];
            fileInputStream.read(fileBytes, 0, (int) fileLength);
            fileInputStream.close();
            LOG.logVerbose("Successfully read file. (" + getCanonicalPath() + ")");
            return fileBytes;
        }
    }

    /**
     * Overrides the original implementation of the File object.
     * Creates any necessary parent directories. Otherwise,
     * an IOException is thrown since the file would not be able to be created.
     * @return {@link File#createNewFile()}
     * @throws IOException
     */
    @Override
    public synchronized boolean createNewFile() throws IOException {
        // Check to see if appropriate directories are created.
        // If this fails, just log the failure. (super.createNewFile() will raise the appropriate exception for us)
        if(!getParentFile().exists() && !mkdirs())
            LOG.logVerbose("Failed to create the non-existent parent directories for the resource file.");

        return super.createNewFile();
    }

    /**
     * Read Resource file to String
     * @return bytes read from file
     * @throws IOException
     */
    public synchronized String readFileToString() throws IOException {
        return new String(readFileToBytes());
    }
}
