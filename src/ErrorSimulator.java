import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ErrorSimulator {
	private DatagramSocket receiver;
	private DatagramPacket clientPacket;
	public ErrorSimulator() throws SocketException {
		receiver = new DatagramSocket(23);
	}
	public DatagramPacket sendReceiveServer(DatagramPacket clientPacket) throws IOException {
		DatagramPacket serverSend = new DatagramPacket(
				clientPacket.getData(), 
				clientPacket.getLength(), 
				InetAddress.getLocalHost(), 
				69);
		DatagramPacket serverReceive = new DatagramPacket(new byte[512], 512);
		DatagramSocket socket = new DatagramSocket();
		socket.send(serverSend);
		socket.receive(serverReceive);
		socket.close();
		return serverReceive;
	}
	public DatagramPacket receiveClient() throws IOException {
		clientPacket = new DatagramPacket(new byte[512], 512);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

