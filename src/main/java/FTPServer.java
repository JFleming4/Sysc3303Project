import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.RequestMessage;
import logging.Logger;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static resources.Configuration.GLOBAL_CONFIG;

/**
 * Represents a TFTP server
 */

public class FTPServer extends Thread {
	private static final Logger LOG = new Logger("FTPServer");
	private DatagramSocket connection;
	private List<ServerWorker> serverWorkers;
	private long currentWorkerId;

	public FTPServer() throws SocketException {
		connection = new DatagramSocket(GLOBAL_CONFIG.SERVER_PORT);
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
	public int getPort()
	{
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
	public void run()
	{
		while(!connection.isClosed())
		{
			DatagramPacket receivedPacket = new DatagramPacket(new byte[Message.MAX_PACKET_SIZE], Message.MAX_PACKET_SIZE);

			try {
				// We want to remove any workers that have completed their task
				serverWorkers.removeIf(serverWorker -> serverWorker.getState() == State.TERMINATED);

				connection.receive(receivedPacket);

				// Create and start the worker thread that will handle the request
				LOG.logVerbose("Dispatching Server worker thread.");
				ServerWorker worker = new ServerWorker(currentWorkerId++, receivedPacket);
				worker.start();

				// Add worker thread to our listing
				this.serverWorkers.add(worker);

			}
			catch (SocketException sE)
			{
				// If the socket was just closed, do not print the stack trace
				if(!connection.isClosed())
					sE.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public static void main(String[] args) {

		// Set VERBOSE on debug mode
		if(GLOBAL_CONFIG.DEBUG_MODE)
			Logger.setLogLevel(Logger.LogLevel.VERBOSE);

		LOG.logQuiet("Starting Server");
		LOG.logQuiet("Current Log Level: " + Logger.getLogLevel().name());

		try {
			// Create and start the server thread
			FTPServer server = new FTPServer();
			LOG.logVerbose("Server Listening at: " + server.getAddress() +":" + server.getPort());
			server.start();

			boolean runServer = true;
			Scanner input = new Scanner(System.in);
			System.out.println("Type 'help' for a list of commands");

			// Prompt the user for input and check to see if they would like to shutdown the server
			while (runServer) {
				System.out.print(">> ");
				String command = input.nextLine();
				if(command.trim().isEmpty())
				    continue;

				switch (command.toLowerCase())
				{
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

			input.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (InterruptedException iE) {
			// No need to worry about this
		} catch (NoSuchElementException nSEE) {
		    // Scanner throws this when a line is empty and the application quits
            // No need to worry about this
        }
	}
}

class ServerWorker extends Thread {
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
			// Create the socket within the context of the thread
			socket = new TFTPDatagramSocket();

			// Parse data into a DAO that is accessible
			RequestMessage receivedMessage = RequestMessage.parseMessageFromPacket(this.packet);
			LOG.logVerbose("Client Information: " + this.packet.getSocketAddress().toString());
			LOG.logVerbose("File Name: " + receivedMessage.getFileName());
			LOG.logVerbose("Mode: " + receivedMessage.getMode());

			// Perform logic based on the type of request
			switch (receivedMessage.getMessageType())
			{
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
					raiseError();
					break;
			}

		}
		catch (InvalidPacketException iPE)
		{
			LOG.logVerbose("There was an error while parsing the received packet");
			LOG.logVerbose(iPE.getMessage());

			// Raise an error if there was a problem parsing the packet
			raiseError();
		}
		catch (IOException ioE)
		{
			ioE.printStackTrace();
		}
		finally
		{
			LOG.logVerbose("Shutting down this instance of ServerWorker.");
			socket.close();
		}
	}

	private void raiseError() {
		LOG.logQuiet("An error has occurred!");
	}

	/**
	 * Logic to handle a read request
	 * @throws IOException
	 */
	private void readRequest(RequestMessage message) throws IOException {
		try {
		    byte [] fileBytes = resourceManager.readFileToBytes(message.getFileName());
		    sendDataBlock(fileBytes);
		} catch (FileNotFoundException e) {
			LOG.logQuiet("Requested File: '" + message.getFileName() + "' Not Found");
			throw new IOException("File not found exception thrown", e);
		}
	}

	/**
	 * Handle a write request.
	 * @param message The request message received.
	 * @throws IOException
	 */
	private void writeRequest(RequestMessage message) throws IOException {
		// According to TFTP protocol, a WRQ is acknowledged by an ACK or ERROR packet.
		// A WRQ ACK will always have a block number of zero
        int expBlockNum = 0;
        String fileName = message.getFileName();

        // WRQ Ack
        AckMessage ackMessage = new AckMessage(expBlockNum++);
		socket.sendMessage(ackMessage, packet.getSocketAddress());
        LOG.logVerbose("WRQ Acknowledgment sent.");
        LOG.logVerbose("Waiting for data messages.");

		for(;;) {

            try {

                // Wait for more data
                DatagramPacket recv = socket.receivePacket();
                DataMessage dataMessage = DataMessage.parseMessageFromPacket(recv);
                LOG.logVerbose("Received Data block. Block Number: " + dataMessage.getBlockNum() + ", Block Size: "
                        + dataMessage.getDataSize() + ", Final Block: " + dataMessage.isFinalBlock());

                // Check to see if received block number is the expected block number
                if(dataMessage.getBlockNum() != expBlockNum)
                    throw new IOException("Unexpected Block Number");

                // Write data to file
                resourceManager.writeBytesToFile(fileName, dataMessage.getData());

                // Update Block number and send
                AckMessage ackMsg = new AckMessage(expBlockNum++);
                socket.sendMessage(ackMsg, recv.getSocketAddress());

                LOG.logVerbose("Sending Data Ack. Block Number: " + ackMsg.getBlockNum());

                // Check to see if its the last data block
                if(dataMessage.isFinalBlock()) {
                    LOG.logVerbose("End of read file reached");
                    break;
                }
            } catch (InvalidPacketException e) {
                e.printStackTrace();
                throw new IOException("Packet Exception occurred", e);
            }
        }
	}



    /**
     * Sends an array of bytes over TFTP, splicing the data in blocks if necessary
     * @param data The array of data
     * @throws IOException
     */
    private void sendDataBlock(byte[] data) throws IOException {

        // Create a sequence of data for the byte array
        List<DataMessage> messages = DataMessage.createDataMessageSequence(data);
        LOG.logVerbose("Sending " + messages.size() + " Data messages to " + packet.getSocketAddress());

        try {
            // Send each data block sequentially
            for(DataMessage m : messages) {

                LOG.logVerbose("Block: " + m.getBlockNum() + ", Data size: " + m.getDataSize() + ", Final Block: " + m.isFinalBlock());

                // Send Data block message
                socket.sendMessage(m, packet.getSocketAddress());

                // Wait for Ack message before continuing
                DatagramPacket packet = socket.receivePacket();
                AckMessage ack = AckMessage.parseMessageFromPacket(packet);
                LOG.logVerbose("Ack Received: " + ack.getBlockNum());

                if(ack.getBlockNum() != m.getBlockNum()) {
                    throw new InvalidPacketException("Invalid Block Number");
                }
            }
        } catch (InvalidPacketException e) {
            e.printStackTrace();
            throw new IOException("Failed to parse Ack Message", e);
        }
    }
}
