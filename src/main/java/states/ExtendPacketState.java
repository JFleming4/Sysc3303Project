package states;

import formats.DataMessage;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class ExtendPacketState extends ForwardState {
	public static final String MODE = "EXTEND";

	private byte [] extendBytes = new byte[DataMessage.MAX_BLOCK_SIZE + 20];
	private ErrorChecker checker;
	public ExtendPacketState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) throws SocketException {
		super(socket, serverAddress);
		this.checker = checker;
		Arrays.fill( this.extendBytes, (byte) 'A' );
	}

	public ExtendPacketState(TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
		this(socket, serverAddress, null);
	}

	@Override
	public String getMode() {
		return MODE;
	}

	public void setErrorChecker(ErrorChecker checker) {
		this.checker = checker;
	}
	
	@Override
	protected void forwardPacket(DatagramPacket packet) throws IOException {
		if(checker.check(packet)) {
			LOG.logQuiet("Extending packet to more than 512 bytes.");
			LOG.logVerbose(packet);

			byte[] packetData = packet.getData();
			byte[] extendData = new byte[packet.getData().length + this.extendBytes.length];

			// append the extendBytes array after the original packet data
			System.arraycopy(packetData, 0, extendData, 0, packetData.length - 1);
			System.arraycopy(this.extendBytes, 0, extendData, packetData.length - 1, this.extendBytes.length);
			extendData[extendData.length-1] = packetData[packetData.length-1];

			super.forwardPacket(new DatagramPacket(extendData, extendData.length, packet.getSocketAddress()));
		}
		else {
			super.forwardPacket(new DatagramPacket(packet.getData(), packet.getLength(), packet.getSocketAddress()));
		}
	}
}
