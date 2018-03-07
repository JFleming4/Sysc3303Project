package states;

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
import formats.RequestMessage;
import formats.Message.MessageType;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

public class LostPacketStateTest {
	private LostPacketState state;
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
		state = new LostPacketState(socket, serverAddress);
	}
	@After
	public void tearDown() {
		System.setOut(System.out);
	}
	@Test
	public void testLostRRQ() {
		try {
			checker = new ErrorChecker(MessageType.RRQ);
			state.setErrorChecker(checker);
			byte[] expectedRRQBytes = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedRRQBytes, expectedRRQBytes.length, connectionManagerSocketAddress);

			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.execute();
			
			Mockito.verify(socket, Mockito.times(0)).forwardPacket(expectedPacket, serverAddress, 69);		
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testLostWRQ() {
		try {
			checker = new ErrorChecker(MessageType.WRQ);
			state.setErrorChecker(checker);
			byte[] expectedWRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, connectionManagerSocketAddress);

			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.execute();
			Mockito.verify(socket, Mockito.times(0)).forwardPacket(expectedPacket, serverAddress, 69);
					
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testLostData() {
		try {
			checker = new ErrorChecker(MessageType.DATA, 1);
			state.setErrorChecker(checker);
			byte[] expectedDataBytes = new DataMessage(1, new byte[] { 0 }).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedDataBytes, expectedDataBytes.length, connectionManagerSocketAddress);

			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));
			
			state.setServerWorkerPort(3000);
			state.setClientAddress(connectionManagerSocketAddress);
			state.execute();
			
			Mockito.verify(socket, Mockito.times(0)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}
	
	@Test
	public void testLostACK() {
		try {
			checker = new ErrorChecker(MessageType.ACK, 1);
			state.setErrorChecker(checker);

			byte[] expectedACKBytes = new AckMessage(1).toByteArray();

			DatagramPacket expectedPacket = new DatagramPacket(expectedACKBytes, expectedACKBytes.length, connectionManagerSocketAddress);
			Mockito.when(socket.receive())
				.thenReturn(expectedPacket)
				.thenThrow(new RuntimeException("TEST EXCEPTION"));

			state.setServerWorkerPort(3000);
			state.setClientAddress(connectionManagerSocketAddress);
			state.execute();

			Mockito.verify(socket, Mockito.times(0)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));		
		} catch (IOException e) {
            Assert.fail(e.getMessage());
        }
	}

}
