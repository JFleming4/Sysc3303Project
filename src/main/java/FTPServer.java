import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.RequestMessage;
import exceptions.InvalidPacketException;
import logging.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Represents a TFTP server
 */
public class FTPServer extends Thread {
	private static final int SERVER_PORT = 69;
	private static final int BUFF_HEADER_SIZE = 516;

	private static final Logger LOG = new Logger("FTPServer");
	private DatagramSocket connection;
	private List<ServerWorker> serverWorkers;

	public FTPServer() throws SocketException {
		connection = new DatagramSocket(SERVER_PORT);
		serverWorkers = new ArrayList<>();
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
			DatagramPacket receivedPacket = new DatagramPacket(new byte[BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);

			try {
				// We want to remove any workers that have completed their task
				serverWorkers.removeIf(serverWorker -> serverWorker.getState() == State.TERMINATED);

				connection.receive(receivedPacket);

				// Create and start the worker thread that will handle the request
				LOG.logVerbose("Dispatching Server worker thread.");
				ServerWorker worker = new ServerWorker(receivedPacket);
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
		Logger.setLogLevel(Logger.LogLevel.VERBOSE);
		LOG.logQuiet("Starting Server");
		LOG.logQuiet("Current Log Level: " + Logger.getLogLevel().name());
		LOG.logVerbose("Server Port: " + SERVER_PORT);


		try {
			// Create and start the server thread
			FTPServer server = new FTPServer();
			server.start();

			boolean runServer = true;
			Scanner input = new Scanner(System.in);
			System.out.println("Type 'help' for a list of commands");

			// Prompt the user for input and check to see if they would like to shutdown the server
			while (runServer) {
				System.out.print(">> ");
				String command = input.nextLine();

				switch (command.toLowerCase())
				{
					case "stop":
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
						System.out.println("Commands:\n'stop' -> Shutdown the server\n'verbose' -> Enable verbose logging\n'quiet' -> Enable quiet logging");
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
		}
	}
}

class ServerWorker extends Thread {
	private static final Logger LOG = new Logger("ServerWorker");
	private DatagramPacket packet;
	private DatagramSocket socket;

	public ServerWorker(DatagramPacket p) throws SocketException {
		this.packet = p;
	}

	@Override
	public void run() {

		try {
			// Create the socket within the context of the thread
			socket = new DatagramSocket();

			// Parse data into a DAO that is accessible
			RequestMessage receivedMessage = RequestMessage.parseDataFromPacket(this.packet);

			LOG.logVerbose("File Name: " + receivedMessage.getFileName());
			LOG.logVerbose("Mode: " + receivedMessage.getMode());

			// Perform logic based on the type of request
			switch (receivedMessage.getMessageType())
			{
				case RRQ:
					readRequest(receivedMessage);
					break;
				case WRQ:
					writeRequest(receivedMessage);
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

		socket.close();
	}

	private void raiseError() {
		LOG.logQuiet("An error has occurred!");
	}

	/**
	 * Logic to handle a read request
	 * @throws IOException
	 */
	private void readRequest(RequestMessage message) throws IOException {
		// TODO: Implement file reading functionality
		sendData(new byte[0]);
	}

	/**
	 * Handle a write request.
	 * @param message The request message received.
	 * @throws IOException
	 */
	private void writeRequest(RequestMessage message) throws IOException {

		// According to TFTP protocol, a WRQ is acknowledged by an ACK or ERROR packet.
		// A WRQ ACK will always have a block number of zero
		sendAck(0);
	}

	/**
	 * Sends a TFTP message over the socket
	 * @param msg The message to send
	 * @throws IOException
	 */
	private void sendMessage(Message msg) throws IOException
	{
		byte[] data = msg.toByteArray();
		socket.send(new DatagramPacket(data, data.length, packet.getSocketAddress()));
	}

	/**
	 * Send block acknowledgement message
	 * @param blockNum The block number to acknowledge
	 * @throws IOException
	 */
	private void sendAck(int blockNum) throws IOException {
		sendMessage(new AckMessage(blockNum));
	}

	/**
	 * Sends an array of bytes over TFTP, splicing the data in blocks if necessary
	 * @param data The array of data
	 * @throws IOException
	 */
	private void sendData(byte[] data) throws IOException {

		// Create a sequence of data for the byte array
		List<DataMessage> messages = DataMessage.createDataMessageSequence(data);

		// Send each data block sequentially
		for(DataMessage m : messages)
			sendMessage(m);
	}
}