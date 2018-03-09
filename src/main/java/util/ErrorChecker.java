package util;

import java.net.DatagramPacket;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.Message.MessageType;

public class ErrorChecker {
	private MessageType type;
	private int blockNum;
	private int replicator;
	public ErrorChecker(MessageType type, int blockNum, int replicator) {
		this.type = type;
		this.blockNum = blockNum;
		this.replicator = replicator;
	}
	
	public ErrorChecker(MessageType type, int blockNum) {
		this(type, blockNum, -1);
	}
	
	public ErrorChecker(MessageType type) {
		this(type, -1, -1);
	}
	
	public boolean check(DatagramPacket packet) {
		try {
			if(Message.getMessageType(packet.getData()) != type) return false;
			
			int packetBlock = -1;
			
			switch(type) {
			case RRQ:
			case WRQ:
			case ERROR:
				return true;
			case ACK:
				packetBlock = AckMessage.parseMessage(packet.getData()).getBlockNum();
				break;
			case DATA:
				packetBlock = DataMessage.parseMessage(packet.getData()).getBlockNum();
				break;
			}
			
			if (blockNum == packetBlock) {
				blockNum += replicator;
				return true;
			}	
		} catch (InvalidPacketException e) {
			e.printStackTrace();
		}
		return false;
	}	
}
