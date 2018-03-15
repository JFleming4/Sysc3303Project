package states;

import formats.AckMessage;
import formats.DataMessage;
import formats.Message.MessageType;
import formats.RequestMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.Arrays;

import static resources.Configuration.GLOBAL_CONFIG;

public class ExtendPacketStateTest {
	private ExtendPacketState state;
	private TFTPDatagramSocket socket;
	private ErrorChecker checker;
	private InetAddress serverAddress;
	private InetSocketAddress serverSocketAddress;
	
	@Before
	public void setup() {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outStream));
		socket = Mockito.mock(TFTPDatagramSocket.class);
		try {
			serverAddress = InetAddress.getByName(StateTestConfig.SERVER_HOST);
			serverSocketAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), GLOBAL_CONFIG.SERVER_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			state = new ExtendPacketState(socket, serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	@After
	public void tearDown() {
		System.setOut(System.out);
	}


	@Test
	public void testExtendedAck() {
		try {
			checker = new ErrorChecker(MessageType.ACK, 1);
			byte[] testDataBytes = new AckMessage(1).toByteArray();
			testExtendedPacket(checker, testDataBytes);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testExtendedData() {
		try {
			checker = new ErrorChecker(MessageType.DATA, 1);
			byte[] testDataBytes = new DataMessage(1, new String("TESTDATA").getBytes()).toByteArray();
			testExtendedPacket(checker, testDataBytes);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testExtendedRequest() {
		try {
			checker = new ErrorChecker(MessageType.RRQ);
			byte[] testDataBytes = new RequestMessage(MessageType.RRQ, RequestMessage.DEFAULT_MODE).toByteArray();
			testExtendedPacket(checker, testDataBytes);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}


	public void testExtendedPacket(ErrorChecker errorChecker, byte[] testDataBytes) {
		try {
			state.setErrorChecker(errorChecker);
			DatagramPacket expectedPacket = new DatagramPacket(testDataBytes, testDataBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
					.thenReturn(expectedPacket)
					.thenThrow(new RuntimeException("TEST EXCEPTION"));


			byte [] extendBytes = new byte[DataMessage.MAX_BLOCK_SIZE + 20];
			Arrays.fill(extendBytes,(byte) 'A');
			// append the extendBytes array after the original packet data
			byte [] expectedBytes = new byte[testDataBytes.length + DataMessage.MAX_BLOCK_SIZE + 20];
			System.arraycopy(testDataBytes, 0, expectedBytes, 0, testDataBytes.length-1);
			System.arraycopy(extendBytes, 0, expectedBytes, testDataBytes.length-1, extendBytes.length);
			expectedBytes[expectedBytes.length-1] = testDataBytes[testDataBytes.length-1];

			state.setServerWorkerPort(3000);
			state.setClientAddress(serverSocketAddress);
			state.execute();


			DatagramPacketByteMatcher matcher = new DatagramPacketByteMatcher(new DatagramPacket(expectedBytes, expectedBytes.length, serverSocketAddress));

			Mockito.verify(socket, Mockito.times(0)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
			Mockito.verify(socket, Mockito.times(1)).forwardPacket(Mockito.argThat(matcher), Mockito.eq(serverAddress), Mockito.eq(3000));
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}
}






class DatagramPacketByteMatcher implements ArgumentMatcher<DatagramPacket> {
	private DatagramPacket packet;

	public DatagramPacketByteMatcher(DatagramPacket packet) {
		this.packet = packet;
	}

	@Override
	public boolean matches(DatagramPacket p) {
		return Arrays.equals(p.getData(),this.packet.getData());
	}
}
