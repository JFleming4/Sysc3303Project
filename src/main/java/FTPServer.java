import exceptions.InvalidPacketException;
import exceptions.SessionException;
import formats.ErrorMessage;
import formats.RequestMessage;
import formats.ErrorMessage.ErrorType;

import logging.Logger;
import resources.ResourceFile;
import resources.ResourceManager;
import session.ISessionHandler;
import session.ReceiveSession;
import session.TFTPSession;
import session.TransmitSession;
import socket.TFTPDatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import java.util.*;

import static resources.Configuration.GLOBAL_CONFIG;

/**
 * Represents a TFTP server
 */

public class FTPServer extends Thread {
    private static final Logger LOG = new Logger("FTPServer");
    private TFTPDatagramSocket connection;
    private List<ServerWorker> serverWorkers;
    private long currentWorkerId;

    public FTPServer() throws SocketException {
        connection = new TFTPDatagramSocket(GLOBAL_CONFIG.SERVER_PORT);
        serverWorkers = new ArrayList<>();
        currentWorkerId = 1;
    }

    /**
     * @return the server address
     */
    public InetAddress getAddress() {
        return connection.getLocalAddress();
    }

    /**
     * @return the server port
     */
    public int getPort() {
        return this.connection.getLocalPort();
    }

    /**
     * Closes the server connection, waits for all worker threads to finish
     */
    public void stopServer() {

        // Close the server socket
        connection.close();

        // Remove any completed workers
        serverWorkers.removeIf(serverWorker -> serverWorker.getState() == State.TERMINATED);

        LOG.logQuiet("Waiting for worker threads to complete.");
        LOG.logVerbose("Number of active worker threads: " + serverWorkers.size());

        // We want to join all worker threads, so that we can make sure they complete all of their tasks
        // before shutting down the server
        for (ServerWorker worker : serverWorkers) {

            try {
                // We must wait for the worker to finish the task
                if (worker.getState() != State.TERMINATED)
                    worker.join();

                LOG.logVerbose("Worker " + worker.getName() + " has successfully finished");

            } catch (InterruptedException iE) {
                iE.printStackTrace();
            }
        }
    }

    /**
     * Runs the server thread. Waits for an incoming request and dispatches a ServerWorker thread to
     * process the request
     */
    @Override
    public void run() {
        while (!connection.isClosed()) {
            try {
                // We want to remove any workers that have completed their task
                serverWorkers.removeIf(serverWorker -> serverWorker.getState() == State.TERMINATED);

                DatagramPacket receivedPacket = connection.receive();

                // Create and start the worker thread that will handle the request
                LOG.logVerbose("Dispatching Server worker thread.");
                ServerWorker worker = new ServerWorker(currentWorkerId++, receivedPacket);
                worker.start();

                // Add worker thread to our listing
                this.serverWorkers.add(worker);

            } catch (SocketException sE) {
                // If the socket was just closed, do not print the stack trace
                if (!connection.isClosed())
                    sE.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {

        // Set VERBOSE on debug mode
        if (GLOBAL_CONFIG.DEBUG_MODE)
            Logger.setLogLevel(Logger.LogLevel.VERBOSE);

        LOG.logQuiet("Starting Server");
        LOG.logQuiet("Current Log Level: " + Logger.getLogLevel().name());

        try (Scanner input = new Scanner(System.in)) {
            // Create and start the server thread
            FTPServer server = new FTPServer();
            LOG.logVerbose("Server Listening at: " + server.getAddress() + ":" + server.getPort());
            server.start();

            boolean runServer = true;
            System.out.println("Type 'help' for a list of commands");

            // Prompt the user for input and check to see if they would like to shutdown the server
            while (runServer) {
                System.out.print(">> ");
                String command = input.nextLine();
                if (command.trim().isEmpty())
                    continue;

                switch (command.toLowerCase()) {
                    case "exit":
                        runServer = false;
                        server.stopServer();

                        // Join the server thread to wait until it is finished
                        server.join();
                        break;
                    case "verbose":
                        System.out.println("Logging has been set to verbose");
                        Logger.setLogLevel(Logger.LogLevel.VERBOSE);
                        break;
                    case "quiet":
                        System.out.println("Logging has been set to quiet");
                        Logger.setLogLevel(Logger.LogLevel.QUIET);
                        break;
                    case "help":
                        System.out.println("Commands:\n'exit' -> Shutdown the server\n'verbose' -> Enable verbose logging\n'quiet' -> Enable quiet logging");
                        break;
                    default:
                        System.out.println("'" + command + "' is not a valid command.");
                        System.out.println("Type 'help' for a list of commands");
                        break;
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (InterruptedException | NoSuchElementException iE) {
            // No need to worry about this
        }
    }
}

class ServerWorker extends Thread implements ISessionHandler {
    private static final Logger LOG = new Logger("ServerWorker");
    private TFTPDatagramSocket socket;
    private DatagramPacket packet;
    private ResourceManager resourceManager;

    public ServerWorker(long workerId, DatagramPacket p) throws IOException {
        // Include Worker ID in Log Tag
        LOG.setComponentName("ServerWorker-" + workerId);

        this.packet = p;
        this.resourceManager = new ResourceManager(GLOBAL_CONFIG.SERVER_RESOURCE_DIR);
        LOG.logVerbose("Resource Path for request: " + this.resourceManager.getFullPath());
    }

    @Override
    public void run() {

        try {
            try {
                // Create the socket within the context of the thread
                socket = new TFTPDatagramSocket();

                // Parse data into a DAO that is accessible
                RequestMessage receivedMessage = RequestMessage.parseMessage(this.packet);

                // Check to see if the file is a valid resource file
                if (!resourceManager.isValidResource(receivedMessage.getFileName())) {
                    // We must let the client know that the requested file is not a valid resource
                    raiseError(new ErrorMessage(ErrorType.ACCESS_VIOLATION, "The requested file '" + receivedMessage.getFileName() + "' is not a valid resource file"));
                    return;
                }


                LOG.logVerbose("Client Information: " + this.packet.getSocketAddress().toString());
                LOG.logVerbose("File Name: " + receivedMessage.getFileName());
                LOG.logVerbose("Full File Path: " + resourceManager.getFile(receivedMessage.getFileName()).getAbsolutePath());
                LOG.logVerbose("Mode: " + receivedMessage.getMode());

                // Perform logic based on the type of request
                switch (receivedMessage.getMessageType()) {
                    case RRQ:
                        LOG.logQuiet("Received Read Request");
                        readRequest(receivedMessage);
                        LOG.logQuiet("Successfully handled RRQ");
                        break;
                    case WRQ:
                        LOG.logQuiet("Received Write Request");
                        writeRequest(receivedMessage);
                        LOG.logQuiet("Successfully handled WRQ");
                        break;
                    default:
                        break;
                }

            } catch (InvalidPacketException iPE) {
                LOG.logQuiet("There was an error while parsing the received packet");
                LOG.logVerbose(iPE.getMessage());

                // Raise an error if there was a problem parsing the packet
                raiseError(new ErrorMessage(ErrorType.ILLEGAL_OPERATION, "Wrong Message Type. Expecting Request Message (RRQ or WRQ)."));
            } catch (SocketException se) {
                LOG.logQuiet("There was a SocketException when handling the Request. No ERROR message will be sent to the client.");
                LOG.logQuiet("The ServerWorker failed to handle the request because of a socket message.");
                LOG.logVerbose(se.getLocalizedMessage());
            } catch (IOException ioE) {
                LOG.logQuiet("There was an IOException while handling the request packet. Attempting to send ERROR to client.");
                LOG.logVerbose(ioE.getMessage());

                // Raise an error if there was a problem parsing the packet
                raiseError(new ErrorMessage(ErrorType.NOT_DEFINED, "IOException Occurred: " + ioE.getLocalizedMessage()));
            }
        } catch (IOException ioE) {
            LOG.logQuiet("There was an IOException while raising an ERROR. The client will not be notified of this exception.");
            LOG.logVerbose(ioE.getMessage());
        } finally {
            LOG.logVerbose("Shutting down this instance of ServerWorker.");
            socket.close();
        }
    }

    /**
     * Logs and sends an error to the client
     *
     * @param errorMessage The Error to Log/Send
     * @throws IOException
     */
    private void raiseError(ErrorMessage errorMessage) throws IOException {
        LOG.logQuiet("An error has been raised! Sending an ERROR message to the client.");
        LOG.logQuiet(errorMessage);
        socket.sendMessage(errorMessage, packet.getSocketAddress());
    }

    /**
     * Logic to handle a read request
     * Sends an ErrorMessage to the Client if the file does not exist
     *
     * @throws IOException
     */
    private void readRequest(RequestMessage message) throws IOException {
        new TransmitSession(this, message, packet.getSocketAddress());
    }

    /**
     * Handle a write request.
     *
     * @param message The request message received.
     * @throws IOException
     */
    private void writeRequest(RequestMessage message) throws IOException {
        new ReceiveSession(this, message, packet.getSocketAddress());
    }


    @Override
    public ResourceManager getSessionResourceManager() {
        return ServerWorker.this.resourceManager;
    }

    @Override
    public TFTPDatagramSocket getSessionTFTPSocket() {
        return ServerWorker.this.socket;
    }

    /**
     * Handles any session errors that occur. Since this class is the handler
     * for both a Transmit and Receive session, we must be able to differentiate between
     * the two when handling error messages
     *
     * @param session The TFTPSession where the error occurred.
     * @param message The ERROR message representing the error that occurred.
     * @throws IOException
     * @throws SessionException
     */
    @Override
    public void sessionErrorOccurred(TFTPSession session, ErrorMessage message) throws IOException, SessionException {
        if (session instanceof ReceiveSession)
            this.sessionErrorOccurred((ReceiveSession) session, message);
        else if (session instanceof TransmitSession)
            this.sessionErrorOccurred((TransmitSession) session, message);
        else {
            LOG.logQuiet("Unknown TFTP session. Stopping session.");
            throw new SessionException();
        }
    }

    /**
     * Occurs when the TFTP Session receives an error from the client.
     *
     * @param session The TFTPSession where the error was received.
     * @param message The ERROR message representing the error that was received by the destination.
     */
    @Override
    public void sessionErrorReceived(TFTPSession session, ErrorMessage message) {
        // Received an error from the client
        LOG.logQuiet("Received Error Packet from Client. Stopping Session.");
        LOG.logQuiet("Error Received: ");
        LOG.logQuiet(message);

        // Special handling for when a receive session receives an error (delete partial file)
        if (session instanceof ReceiveSession)
            this.sessionErrorReceived((ReceiveSession) session);
    }

    /**
     * Occurs when an error is received during a TFTP receive session.
     * Removes the partial (corrupt) file due to TFTP errors.
     *
     * @param session The ReceiveSession
     */
    private void sessionErrorReceived(ReceiveSession session) {
        // Remove the file, as it is incomplete
        ResourceFile file = session.getResourceFile();

        LOG.logQuiet("Failed to receive correct file.");

        // Remove file
        if (file.delete())
            LOG.logQuiet("Deleted partial file: " + file.getAbsolutePath());
        else
            LOG.logQuiet("Failed to delete partial file: " + file.getAbsolutePath());
    }

    /**
     * Gets called when a TransmitSession encounters an Error.
     *
     * @param session The TransmitSession that encountered an error
     * @param message The ErrorMessage describing all details
     * @throws IOException
     * @throws SessionException
     */
    private void sessionErrorOccurred(TransmitSession session, ErrorMessage message) throws IOException, SessionException {
        switch (message.getErrorType()) {
            case FILE_EXISTS:
                // Don't do anything. File exists is normal behaviour (We need to transmit from this file)
                break;
            default:
                LOG.logVerbose("Transmit Error Occurred: " + message.getMessage());
                session.raiseError(message);
        }
    }

    /**
     * Gets called when a ReceiveSession encounters an Error.
     *
     * @param session The ReceiveSession that encountered an error
     * @param message The ErrorMessage describing all details
     * @throws IOException
     * @throws SessionException
     */
    private void sessionErrorOccurred(ReceiveSession session, ErrorMessage message) throws IOException, SessionException {

        switch (message.getErrorType()) {
            case FILE_NOT_FOUND:
                // Create the file if it is not found.
                LOG.logVerbose("Creating new resource file for WRQ from client: " + session.getResourceFile().getAbsolutePath());
                session.getResourceFile().createNewFile();
                break;
            default:
                LOG.logVerbose("Receive Error Occurred: " + message.getMessage());
                session.raiseError(message);
                break;
        }
    }
}
