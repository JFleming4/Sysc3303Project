package states;

import formats.AckMessage;
import formats.DataMessage;
import formats.Message.MessageType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import socket.TFTPDatagramSocket;
import util.ErrorChecker;

import java.io.IOException;
import java.net.*;

import static resources.Configuration.GLOBAL_CONFIG;

public class InvalidTIDStateTest {
	private InvalidTIDState state;
	private TFTPDatagramSocket socket;
	private TFTPDatagramSocket invalidTIDSocket;
	private ErrorChecker checker;
	private InetAddress serverAddress;
	private InetSocketAddress serverSocketAddress;

	@Before
	public void setup() {
		socket = Mockito.mock(TFTPDatagramSocket.class);
		invalidTIDSocket = Mockito.mock(TFTPDatagramSocket.class);
		try {
			serverAddress = InetAddress.getByName(StateTestConfig.SERVER_HOST);
			serverSocketAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), GLOBAL_CONFIG.SERVER_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			state = new InvalidTIDState(socket, serverAddress);
			state.setInvalidTIDSocket(invalidTIDSocket);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testInvalidTIDAck() {
		try {
			checker = new ErrorChecker(MessageType.ACK, 1);
			byte[] testDataBytes = new AckMessage(1).toByteArray();
			testInvalidTIDPacket(checker, testDataBytes);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testInvalidTIDData() {
		try {
			checker = new ErrorChecker(MessageType.DATA, 1);
			byte[] testDataBytes = new DataMessage(1, StateTestConfig.FILE_STRING.substring(0, 500).getBytes()).toByteArray();
			testInvalidTIDPacket(checker, testDataBytes);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	public void testInvalidTIDPacket(ErrorChecker errorChecker, byte[] testDataBytes) {
		try {
			state.setErrorChecker(errorChecker);

			DatagramPacket expectedPacket = new DatagramPacket(testDataBytes, testDataBytes.length, serverSocketAddress);
			Mockito.when(socket.receive())
					.thenReturn(expectedPacket)
					.thenThrow(new RuntimeException("TEST EXCEPTION"));

			state.setServerWorkerPort(3000);
			state.setClientAddress(serverSocketAddress);
			state.execute();

			Mockito.verify(socket, Mockito.times(1)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(3000));
			Mockito.verify(invalidTIDSocket, Mockito.times(1)).forwardPacket(Mockito.eq(expectedPacket), Mockito.eq(serverAddress), Mockito.eq(GLOBAL_CONFIG.SERVER_PORT));
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}
}
