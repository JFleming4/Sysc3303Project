package states;

import static resources.Configuration.GLOBAL_CONFIG;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import exceptions.InvalidPacketException;
import formats.*;
import formats.Message.MessageType;
import formats.ErrorMessage.ErrorType;

import logging.Logger;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class ReadState extends State {
    private static final int SOCKET_TIMEOUT = 5000;

    private TFTPDatagramSocket socket;
    private ResourceManager resourceManager;
    private SocketAddress serverAddress;
    private String filename;

    private static final Logger LOG = new Logger("FTPClient - Read");
    private int expBlockNum;
    private boolean isConsentual;

    public ReadState(SocketAddress serverAddress, String filename, boolean isVerbose) throws SocketException, IOException {
        this(serverAddress, filename, isVerbose, new TFTPDatagramSocket(), new ResourceManager(GLOBAL_CONFIG.CLIENT_RESOURCE_DIR));
    }


    public ReadState(SocketAddress serverAddress, String filename, boolean isVerbose, TFTPDatagramSocket socket, ResourceManager resourceManager) throws SocketException {
        this.serverAddress = serverAddress;
        this.filename = filename;

        this.socket = socket;
        this.socket.setSoTimeout(SOCKET_TIMEOUT);
        this.resourceManager = resourceManager;
        if (isVerbose)
            Logger.setLogLevel(Logger.LogLevel.VERBOSE);
        else
            Logger.setLogLevel(Logger.LogLevel.QUIET);
    }

    @Override
    public State execute() {

        expBlockNum = -1;
        isConsentual = true;
        try {
            // Create the resource manager, handle IOException if failed to get resource directory

            socket.setSoTimeout(SOCKET_TIMEOUT);

            // Check if the file already exists. If so, notify Client about overwrite
            if (resourceManager.fileExists(filename))
            {
                LOG.logQuiet("File (" + filename + ") already exists on the Client");
                LOG.logQuiet("Please enter another command");
                return new InputState();
            }

            // Set up + Send RRQ Message
            RequestMessage rrqMessage = new RequestMessage(MessageType.RRQ ,filename);
            socket.sendMessage(rrqMessage, serverAddress);
            LOG.logQuiet("---- Begin File Transaction ---");
            LOG.logQuiet("Read request sent!");
            LOG.logQuiet("Waiting to receive data");

            while (isConsentual) {

                try {
                    // Receive read data from server
                    DatagramPacket recv = socket.receivePacket();


                    // Handle message type (make this better)
                    switch (Message.getMessageType(recv.getData())) {
                        case DATA:
                            isConsentual = handleDataMessage(recv);
                            break;

                        case ERROR:
                            handleErrorMessage(recv);
                            isConsentual = false;
                            break;

                        default:
                            isConsentual = false;
                            break;
                    }

                } catch (IOException | InvalidPacketException iopE) {
                    iopE.printStackTrace();
                }
            }
        } catch(UnknownHostException uHE) {
            LOG.logQuiet("Error: Unknown Host Entered");
        } catch(IOException ioE) {
            LOG.logVerbose("An IOException has occurred: " + ioE.getLocalizedMessage());
            ioE.printStackTrace();
        } finally {
            socket.close();
        }

        return new InputState();
    }


    /**
     *
     * @param recv blah
     */
    private boolean handleDataMessage(DatagramPacket recv) throws InvalidPacketException, IOException
    {
        boolean shouldContinue = true;

        DataMessage dataMessage = DataMessage.parseMessageFromPacket(recv);
        LOG.logVerbose("Received data block: " + dataMessage.getBlockNum());

        // Initialize block number to one sent from server
        if(expBlockNum == -1)
            expBlockNum = dataMessage.getBlockNum();

        // Confirm expected block number
        if(dataMessage.getBlockNum() != expBlockNum)
            throw new Error("Unexpected Block Number");

        // Attempt to write to file
        try {
            resourceManager.writeBytesToFile(filename, dataMessage.getData());
        } catch (IOException ioE) {
            if (ioE.getMessage().contains("Not enough usable space"))
            {
                LOG.logQuiet("Not enough usable disk space");
                ErrorMessage msg = new ErrorMessage(ErrorType.DISK_FULL, "Not enough free space on disk");
                socket.sendMessage(msg, this.serverAddress);
                shouldContinue = false;
            } else {
                ioE.printStackTrace();
            }
        }

        // Send ACK and increment block num
        AckMessage ackMsg = new AckMessage(expBlockNum++);
        socket.sendMessage(ackMsg, recv.getSocketAddress());
        LOG.logVerbose("Sent Ack for block: " + ackMsg.getBlockNum());

        // Check if this was the last block
        if(dataMessage.isFinalBlock()) {
            LOG.logVerbose("End of read file reached");
            LOG.logQuiet("Successfully received file");
            LOG.logQuiet("---- End File Transaction ---");
            shouldContinue = false;
        }

        return shouldContinue;
    }

    /**
     *
     * @param recv
     * @return
     * @throws InvalidPacketException
     */
    private void handleErrorMessage(DatagramPacket recv) throws InvalidPacketException
    {
        ErrorMessage errorMessage = ErrorMessage.parseMessageFromPacket(recv);
        LOG.logVerbose("Received error (" + errorMessage.getErrorType()
                + ") with message: " + errorMessage.getMessage());

        // Determine the Type of the ErrorMessage
        switch (errorMessage.getErrorType()) {
            case FILE_NOT_FOUND:
                System.out.println("Sorry, but " + filename + " doesn't exist on the Server");
                break;

            case ACCESS_VIOLATION:
                // Justin's code
                break;

            default:
                break;
        }
    }
}
