package states;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import exceptions.InvalidPacketException;
import formats.Message;
import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class DuplicateState extends ForwardState {
	private ErrorChecker checker;
	
	public DuplicateState(TFTPDatagramSocket socket, InetAddress serverAddress, ErrorChecker checker) {
		super(socket, serverAddress);
		this.checker = checker;
	}
	
	public DuplicateState(TFTPDatagramSocket socket, InetAddress serverAddress) {
		this(socket, serverAddress, null);
	}
	
	public void setErrorChecker(ErrorChecker checker) {
		this.checker = checker;
	}
	
	@Override
	protected void forwardPacket(DatagramPacket packet) throws IOException {
		super.forwardPacket(packet);
		if(checker.check(packet)) {
			MessageType type = null;
			try {
				type = Message.getMessageType(packet.getData());
			} catch (InvalidPacketException e) {
				e.printStackTrace();
			}
			if(type == MessageType.RRQ || type == MessageType.WRQ) 
				super.forwardRequest(packet, serverAddress);
			else
				super.forwardPacket(packet);
		}
	}

}
