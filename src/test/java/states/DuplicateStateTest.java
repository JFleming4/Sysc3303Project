package states;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import formats.AckMessage;
import formats.DataMessage;
import formats.Message;
import formats.RequestMessage;
import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class DuplicateStateTest {
//  lose a packet; 2 : delay a packet, 3 : duplicate a
	// packet.
	private DuplicateState state;
	private TFTPDatagramSocket socket;
	private ErrorChecker checker;
	private InetAddress serverAddress;
	private InetSocketAddress connectionManagerSocketAddress;
	
	@Before
	public void setup() {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outStream));
		socket = Mockito.mock(TFTPDatagramSocket.class);
		try {
			serverAddress = InetAddress.getByName(StateTestConfig.SERVER_HOST);
			connectionManagerSocketAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), 1069);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = new DuplicateState(socket, serverAddress);
	}
	@After
	public void tearDown() {
		System.setOut(System.out);
	}
	@Test
	public void testDuplicateRRQ() {
		try {
			checker = new ErrorChecker(MessageType.RRQ);
			state.setErrorChecker(checker);
			byte[] expectedRRQBytes = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();
			byte[] expectedWRQBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();
			DatagramPacket expectedPacket = new DatagramPacket(expectedRRQBytes, expectedRRQBytes.length, connectionManagerSocketAddress);
			DatagramPacket serverResponse = new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, connectionManagerSocketAddress);
			Mockito.when(socket.receivePacket())
				.thenReturn(expectedPacket)
				.thenReturn(serverResponse)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.execute();
			Mockito.verify(socket, Mockito.times(2)).forwardPacket(expectedPacket, serverAddress, 69);
					
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testDuplicateWRQ() {
		try {
			checker = new ErrorChecker(MessageType.WRQ);
			state.setErrorChecker(checker);
			byte[] expectedRRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();
			byte[] expectedWRQBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();
			DatagramPacket expectedPacket = new DatagramPacket(expectedRRQBytes, expectedRRQBytes.length, connectionManagerSocketAddress);
			DatagramPacket serverResponse = new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, connectionManagerSocketAddress);
			Mockito.when(socket.receivePacket())
				.thenReturn(expectedPacket)
				.thenReturn(serverResponse)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.execute();
			Mockito.verify(socket, Mockito.times(2)).forwardPacket(expectedPacket, serverAddress, 69);
					
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testDuplicateData() {
		checker = new ErrorChecker(MessageType.DATA);
		state.setErrorChecker(checker);
		fail();
	}
	
	@Test
	public void testDuplicateACK() {
		checker = new ErrorChecker(MessageType.ACK);
		state.setErrorChecker(checker);
		fail();
	}

}
