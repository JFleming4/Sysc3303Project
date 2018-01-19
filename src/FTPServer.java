import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;
//import java.util.ArrayList;
//import java.util.List;

public class FTPServer {
	private DatagramSocket connection;

	public FTPServer() throws SocketException {
		connection = new DatagramSocket(69);
	}

	public void start() throws SocketException {
		ServerReceiver receiver = new ServerReceiver(connection);
		receiver.start();
	}

	public void stop() {
		connection.close();
	}

	class ServerReceiver extends Thread {
		private DatagramSocket receiver;
		private DatagramPacket receivedPacket;

		// private List<ServerWorker> workers;
		public ServerReceiver(DatagramSocket socket) throws SocketException {
			receiver = socket;
			// workers = new ArrayList<ServerWorker>();
		}

		public void run() {
			while (!receiver.isClosed()) {
				receivedPacket = new DatagramPacket(new byte[512], 512);
				try {
					receiver.receive(receivedPacket);
					ServerWorker worker = new ServerWorker(receivedPacket);
					// workers.add(worker);
					worker.start();
				} catch (IOException e) {
					if (!receiver.isClosed()) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	class ServerWorker extends Thread {
		private DatagramPacket packet;
		private DatagramSocket socket;

		public ServerWorker(DatagramPacket p) throws SocketException {
			this.packet = p;
			socket = new DatagramSocket();
		}

		public void run() {
			if (packet.getData()[0] != 0) {
				raiseError();
			}
			try {
				switch (packet.getData()[1]) {
				case 1:
					readRequest();
					break;
				case 2:
					writeRequest();
					break;
				case 3:
				case 4:
					raiseError();
					break;
				}
			} catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
			socket.close();
		}

		private void raiseError() {

		}

		private void readRequest() throws IOException {
			int blockNum = 1;
			sendData(blockNum, new byte[0]);
		}

		private void writeRequest() throws IOException {
			int blockNum = 0;
			sendAck(blockNum);
		}

		private void sendAck(int blockNum) throws IOException {
			byte data[] = new byte[4];
			data[0] = 0;
			data[1] = 4;
			data[2] = (byte) (blockNum / 256);
			data[3] = (byte) (blockNum % 256);
			socket.send(new DatagramPacket(data, 4, packet.getSocketAddress()));
		}

		private void sendData(int blockNum, byte[] data) throws IOException {
			byte packetData[] = new byte[4 + data.length];
			data[0] = 0;
			data[1] = 3;
			data[2] = (byte) (blockNum / 256);
			data[3] = (byte) (blockNum % 256);
			for (int i = 0; i < data.length; i++) {
				packetData[5 + i] = data[i];
			}
			socket.send(new DatagramPacket(packetData, packetData.length, packet.getSocketAddress()));
		}

	}

	public static void main(String[] args) {
		System.out.println("Starting Server");
		FTPServer server;
		try {
			server = new FTPServer();
			server.start();
			boolean on = true;
			Scanner input = new Scanner(System.in);
			while (on) {
				System.out.println("Input 'stop' to shutdown");
				String command = input.nextLine();
				if (command.toLowerCase().equals("stop")) {
					on = false;
					server.stop();
				}
			}
			input.close();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
