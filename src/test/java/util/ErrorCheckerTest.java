package util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;

import org.junit.Before;
import org.junit.Test;

import exceptions.InvalidPacketException;
import formats.AckMessage;
import formats.DataMessage;
import formats.ErrorMessage;
import formats.ErrorMessage.ErrorType;
import formats.Message.MessageType;
import formats.RequestMessage;

public class ErrorCheckerTest {
	private DatagramPacket ack1, ack2, rrq, wrq, data1, data2, error;

	
	@Before
	public void setup() {
		try
		{
			byte [] ack1Byte = new AckMessage(1).toByteArray();
			byte [] ack2Byte = new AckMessage(2).toByteArray();
			byte [] rrqByte = new RequestMessage(MessageType.RRQ, "Beer.txt").toByteArray();
			byte [] wrqByte = new RequestMessage(MessageType.WRQ, "Wine.txt").toByteArray();
			byte [] data1Byte = new DataMessage(1, new byte[] { 1 }).toByteArray();
			byte [] data2Byte = new DataMessage(2, new byte[] { 1 }).toByteArray();
			byte [] errorByte = new ErrorMessage(ErrorType.ACCESS_VIOLATION, "Don't touch my whiskey").toByteArray();
			ack1 = new DatagramPacket(ack1Byte, ack1Byte.length);
			ack2 = new DatagramPacket(ack2Byte, ack2Byte.length);
			rrq = new DatagramPacket(rrqByte, rrqByte.length);
			wrq = new DatagramPacket(wrqByte, wrqByte.length);
			data1 = new DatagramPacket(data1Byte, data1Byte.length);
			data2 = new DatagramPacket(data2Byte, data2Byte.length);
			error = new DatagramPacket(errorByte, errorByte.length);
		}  catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCheckAck() {
		ErrorChecker checker = new ErrorChecker(MessageType.ACK, 1);
		assertTrue(checker.check(ack1));
		assertFalse(checker.check(ack2));
		assertFalse(checker.check(data1));
		assertFalse(checker.check(data2));
		assertFalse(checker.check(error));
		assertFalse(checker.check(rrq));
		assertFalse(checker.check(wrq));
	}
	
	@Test
	public void testCheckManyAck() {
		ErrorChecker checker = new ErrorChecker(MessageType.ACK, 1, 3);
		byte[] ack4Byte = null;
		try {
			ack4Byte = new AckMessage(4).toByteArray();
		} catch (IOException e) {
			fail();
		}
		DatagramPacket ack4 = new DatagramPacket(ack4Byte, ack4Byte.length);
		assertTrue(checker.check(ack1));
		assertFalse(checker.check(ack2));
		assertTrue(checker.check(ack4));
	}
	
	@Test
	public void testCheckData() {
		ErrorChecker checker = new ErrorChecker(MessageType.DATA, 1);
		assertFalse(checker.check(ack1));
		assertFalse(checker.check(ack2));
		assertTrue(checker.check(data1));
		assertFalse(checker.check(data2));
		assertFalse(checker.check(error));
		assertFalse(checker.check(rrq));
		assertFalse(checker.check(wrq));
	}
	
	@Test
	public void testCheckRRQ() {
		ErrorChecker checker = new ErrorChecker(MessageType.RRQ);
		assertFalse(checker.check(ack1));
		assertFalse(checker.check(ack2));
		assertFalse(checker.check(data1));
		assertFalse(checker.check(data2));
		assertFalse(checker.check(error));
		assertTrue(checker.check(rrq));
		assertFalse(checker.check(wrq));
	}
	
	@Test
	public void testCheckWRQ() {
		ErrorChecker checker = new ErrorChecker(MessageType.WRQ);
		assertFalse(checker.check(ack1));
		assertFalse(checker.check(ack2));
		assertFalse(checker.check(data1));
		assertFalse(checker.check(data2));
		assertFalse(checker.check(error));
		assertFalse(checker.check(rrq));
		assertTrue(checker.check(wrq));
	}
	
	@Test
	public void testCheckError() {
		ErrorChecker checker = new ErrorChecker(MessageType.ERROR);
		assertFalse(checker.check(ack1));
		assertFalse(checker.check(ack2));
		assertFalse(checker.check(data1));
		assertFalse(checker.check(data2));
		assertTrue(checker.check(error));
		assertFalse(checker.check(rrq));
		assertFalse(checker.check(wrq));
	}
	

}
