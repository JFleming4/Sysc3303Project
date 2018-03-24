package parsing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import states.*;
import util.ErrorChecker;

public class Parser {
	public static Command parse(String[] tokens) throws IOException {
		Command cmd;
		switch(tokens[0].toUpperCase()) {
		case "READ":
			cmd = new ReadCommand(tokens);
			break;
		case "WRITE":
			cmd = new WriteCommand(tokens);
			break;
		case "EXIT":
			cmd = new ExitCommand(tokens);
			break;
		case "HELP":
			cmd = new HelpCommand(tokens);
			break;
		default:
			System.out.println("'" + tokens[0] + "' is not a valid command.");
			cmd = new HelpCommand(tokens);
			break;
		}
		return cmd;
	}
	
	public static ForwardState parseStateInformation(String[] tokens, TFTPDatagramSocket socket, InetAddress serverAddress) throws SocketException {
		ForwardState state = null;

		switch (tokens[0].toUpperCase()) {
		case DelayPacketState.MODE:
			if(!checkInvalidNumberParams(tokens, true)) break;
			state = new DelayPacketState(
					socket,
					serverAddress,
					getChecker(subList(tokens, 1, tokens.length - 2)),
					Long.parseLong(tokens[tokens.length - 1]));
			break;
		case LostPacketState.MODE:
			if(!checkInvalidNumberParams(tokens, false)) break;
			state = new LostPacketState(
					socket,
					serverAddress,
					getChecker(subList(tokens, 1, tokens.length - 1)));
			break;
		case DuplicateState.MODE:
			if(!checkInvalidNumberParams(tokens, false)) break;
			state = new DuplicateState(
					socket,
					serverAddress,
					getChecker(subList(tokens, 1, tokens.length - 1)));
			break;
		case InvalidOpCodeState.MODE:
			if(!checkInvalidNumberParams(tokens, false)) break;
			state = new InvalidOpCodeState(
					socket, 
					serverAddress, 
					getChecker(subList(tokens, 1, tokens.length -1)));
			break;
		case ExtendPacketState.MODE:
			if(!checkInvalidNumberParams(tokens, false)) break;
			state = new ExtendPacketState(
					socket,
					serverAddress,
					getChecker(subList(tokens, 1, tokens.length - 1)));
			break;
		case InvalidTIDState.MODE:
			if(!checkInvalidNumberParams(tokens, false)) break;
			state = new InvalidTIDState(
					socket,
					serverAddress,
					getInvalidTIDChecker(subList(tokens, 1, tokens.length - 1)));
			break;
		case ForwardState.MODE:
			state = new ForwardState(socket, serverAddress);
			break;
		default:
			System.out.println("'" + tokens[0] + "' is not a valid state.");
			System.out.println("Type 'help' for a list of commands");
			break;
		}
		return state;
	}
	
	private static boolean checkInvalidNumberParams(String[] tokens, boolean delay) {
		int rqLen;
		int otherLen;
		if(delay) {
			rqLen = 3;
			otherLen = 4;
		} else {
			rqLen = 2;
			otherLen = 3;
		}
		if(tokens.length < rqLen) {
			displayInvalidNumberParams(tokens[0]);
			return false;
		}
		if(!tokens[1].toLowerCase().equals("wrq") && !tokens[1].toLowerCase().equals("rrq")) {
			if(tokens.length < otherLen) {
				displayInvalidNumberParams(tokens[0]);
				return false;
			}
		}
		return true;
	}
	private static void displayInvalidNumberParams(String command) {
		System.out.println("'" + command + "' was not provided with the correct amount of paramaters");
	}

	private static ErrorChecker getInvalidTIDChecker(String[] tokens) {
		String typeMatch = tokens[0].toUpperCase();
		if ( !typeMatch.equals(MessageType.DATA.toString()) && !typeMatch.equals(MessageType.ACK.toString())) {
			throw new ParseException("Can only perform Invalid TID State on DATA and ACK Packets");
		}
		return getChecker(tokens);
	}
	
	private static ErrorChecker getChecker(String[] tokens) {
		MessageType type;
		int blockNum = 0;
		int replication = -1;
		
		if (tokens.length > 1) blockNum = Integer.parseInt(tokens[1]);
		if (tokens.length > 2) replication = Integer.parseInt(tokens[2]);
		
		switch (tokens[0].toUpperCase()) {
		case "ACK":
			type = MessageType.ACK;
			break;
		case "DATA":
			type = MessageType.DATA;
			break;
		case "RRQ":
			return new ErrorChecker(MessageType.RRQ);
		case "WRQ":
			return new ErrorChecker(MessageType.WRQ);
		default:
			throw new ParseException("Invalid MessageType '" + tokens[0] + "'");
		}
		
		return new ErrorChecker(type, blockNum, replication);
	}
	
	private static String[] subList(String[] tokens, int startIndex, int endIndex) {
		String[] tmp = new String[1 + endIndex - startIndex];
		for (int i = 0; i <= endIndex - startIndex; i++)
			tmp[i] = tokens[i + startIndex];
		return tmp;
	}
}

class ParseException extends RuntimeException {
	public ParseException(String msg) {
		super(msg);
	}
}
