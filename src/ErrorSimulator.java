import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ErrorSimulator {
	private DatagramSocket receiver;
	private DatagramPacket clientPacket;
	private static final int SERVER_PORT = 69;
	private static final int ERROR_PORT = 23;
	// private static final int BUFF_SIZE = 512;
	private static final int BUFF_HEADER_SIZE = 516;
	public ErrorSimulator() throws SocketException {
		receiver = new DatagramSocket(ERROR_PORT);
	}
	public DatagramPacket sendReceiveServer(DatagramPacket clientPacket) throws IOException {
		DatagramPacket serverSend = new DatagramPacket(
				clientPacket.getData(), 
				clientPacket.getLength(), 
				InetAddress.getLocalHost(), 
				SERVER_PORT);
		DatagramPacket serverReceive = new DatagramPacket(new byte[BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);
		DatagramSocket socket = new DatagramSocket();
		socket.send(serverSend);
		socket.receive(serverReceive);
		socket.close();
		return serverReceive;
	}
	public DatagramPacket receiveClient() throws IOException {
		clientPacket = new DatagramPacket(new byte[BUFF_HEADER_SIZE], BUFF_HEADER_SIZE);
		receiver.receive(clientPacket);
		return clientPacket;
	}
	public void replyToClient(DatagramPacket serverResponse) throws IOException {
		DatagramPacket clientReply = new DatagramPacket(
				serverResponse.getData(), 
				serverResponse.getLength(),
				clientPacket.getSocketAddress());
		DatagramSocket socket = new DatagramSocket();
		socket.send(clientReply);
		socket.close();
	}
	public static void main(String[] args) {
		try {
			ErrorSimulator sim = new ErrorSimulator();
			while(true) {
				DatagramPacket clientMsg = sim.receiveClient();
				DatagramPacket serverMsg = sim.sendReceiveServer(clientMsg);
				sim.replyToClient(serverMsg);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

