package states;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import exceptions.InvalidPacketException;
import formats.Message;
import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class LostPacketState extends ForwardState {
	
	private ErrorChecker checker;
	
	public LostPacketState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) {
		super(socket, serverAddress);
		this.checker = checker;
	}
	
	public LostPacketState(TFTPDatagramSocket socket, InetAddress serverAddress) {
		this(socket, serverAddress, null);
	}
	
	public void setErrorChecker(ErrorChecker checker) {
		this.checker = checker;
	}

	@Override
	protected void forwardPacket(DatagramPacket packet) throws IOException {
		if (checker.check(packet)) return;
		super.forwardPacket(packet);
	}
}
