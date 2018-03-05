package states;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import formats.*;
import formats.ErrorMessage.ErrorType;
import formats.Message.MessageType;
import resources.ResourceFile;
import resources.ResourceManager;
import socket.TFTPDatagramSocket;

public class ReadStateTest {
	private TFTPDatagramSocket socket;
	private ResourceManager resourceManager;
	private ResourceFile mockedFile;
    private File mockedParentFile;
    private InetSocketAddress serverAddress;
    private InetSocketAddress connectionManagerSocketAddress;
    private InOrder inOrder;

	@Before
	public void setUp() {
	    socket = Mockito.mock(TFTPDatagramSocket.class);
	    resourceManager = Mockito.mock(ResourceManager.class);
	    mockedFile = Mockito.mock(ResourceFile.class);
        mockedParentFile = Mockito.mock(File.class);

        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), 69);
            connectionManagerSocketAddress = new InetSocketAddress(InetAddress.getByName(StateTestConfig.SERVER_HOST), 1069);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        // Mock mechanism to capture arguments in order
        inOrder = Mockito.inOrder(socket, mockedFile);

        try {
            // Set up mocked file
            Mockito.when(mockedFile.exists()).thenReturn(StateTestConfig.READ_FILE_EXISTS);
            Mockito.when(mockedFile.getParentFile()).thenReturn(mockedParentFile);
            Mockito.when(mockedFile.canWrite()).thenReturn(StateTestConfig.READ_FILE_CAN_WRITE);
            Mockito.when(mockedFile.createNewFile()).thenReturn(StateTestConfig.CREATE_NEW_FILE_RETURN);
            Mockito.when(mockedFile.getUsableSpace()).thenReturn(StateTestConfig.USABLE_SPACE);
            Mockito.when(mockedFile.isFile()).thenReturn(StateTestConfig.IS_FILE);

            // Set up mocked parent file
            Mockito.when(mockedParentFile.exists()).thenReturn(StateTestConfig.PARENT_DIRECTORY_EXISTS);

            // Set up resource manager mock
            Mockito.when(resourceManager.getFile(StateTestConfig.FILENAME)).thenReturn(mockedFile);
            Mockito.when(resourceManager.isValidResource(StateTestConfig.FILENAME)).thenReturn(StateTestConfig.IS_VALID_RESOURCE);
        }catch (IOException ioE)
        {
            Assert.fail(ioE.getMessage());
        }
	}

	@Test
    public void ReadLessThan512BytesSuccess() {
        this.RunReadTestByFileLength(256);
    }

	@Test
	public void Read512BytesSuccess() {
	    this.RunReadTestByFileLength(512);
	}

    @Test
    public void Read1024BytesSuccess() {
        this.RunReadTestByFileLength(1024);
    }

	@Test
    public void ReadMoreThan1024BytesSuccess() {
        this.RunReadTestByFileLength(-1);
    }


	@Test
    public void FileNotFoundOnServerError() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outStream));

        try {
            ArgumentCaptor<RequestMessage> requestArgument = ArgumentCaptor.forClass(RequestMessage.class);

            byte[] expectedRRQBytes = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();
            String expectedErrorMessage = StateTestConfig.FILENAME + " Not Found";

            byte[] mockResponseErrorBytes = new ErrorMessage(ErrorMessage.ErrorType.FILE_NOT_FOUND, "Requested File: " + StateTestConfig.FILENAME + " Not Found").toByteArray();
            Mockito.when(socket.receive()).thenReturn(new DatagramPacket(mockResponseErrorBytes, mockResponseErrorBytes.length, connectionManagerSocketAddress));

            // Execute function
            new ReadState(serverAddress, resourceManager, StateTestConfig.FILENAME, false, socket).execute();

            inOrder.verify(socket).sendMessage(requestArgument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Read Request Does Not Match", new String(expectedRRQBytes), new String(requestArgument.getValue().toByteArray()));

            Mockito.verify(socket, Mockito.times(1)).receive();
            Mockito.verify(mockedFile, Mockito.times(0)).writeBytesToFile(Mockito.any(byte[].class));

            // Ensure Message is displayed to the user
            Assert.assertTrue("File Not Found User Message Not Found", outStream.toString().contains(expectedErrorMessage));
        }
         catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        System.setOut(System.out);
    }

    @Test
    public void FilePermissionsErrorFail() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outStream));

        try {
            ArgumentCaptor<RequestMessage> requestArgument = ArgumentCaptor.forClass(RequestMessage.class);

            byte[] expectedRRQBytes = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME).toByteArray();
            String expectedErrorMessage = "You do not have the correct permissions for this file";
            byte[] mockResponseErrorBytes  = new ErrorMessage(ErrorType.ACCESS_VIOLATION, expectedErrorMessage).toByteArray();

            Mockito.when(socket.receive()).thenReturn(new DatagramPacket(mockResponseErrorBytes, mockResponseErrorBytes.length, connectionManagerSocketAddress));
            Mockito.when(mockedFile.exists()).thenReturn(false);

            // Execute function
            new ReadState(serverAddress, resourceManager, StateTestConfig.FILENAME, true, socket).execute();

            inOrder.verify(socket).sendMessage(requestArgument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Read Request Does Not Match", new String(expectedRRQBytes), new String(requestArgument.getValue().toByteArray()));

            Mockito.verify(socket, Mockito.times(1)).receive();

            // Ensure Message is displayed to the user
            Assert.assertTrue("Permission Error User Message Not Found", outStream.toString().contains(expectedErrorMessage));
        }
         catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        System.setOut(System.out);
    }

    public void RunReadTestByFileLength(int length) {
        try {
            ArgumentCaptor<RequestMessage> requestArgument = ArgumentCaptor.forClass(RequestMessage.class);
            ArgumentCaptor<AckMessage> ackArgument = ArgumentCaptor.forClass(AckMessage.class);

            // Create mock File Data messages
            String mockFile = length != -1 ? StateTestConfig.FILE_STRING.substring(0, length - 1) : StateTestConfig.FILE_STRING;
            List<DataMessage> mockedDataSequence = DataMessage.createDataMessageSequence(mockFile.getBytes());

            // Mocked and expected Messages to be passed in 1 block read file transfer
            RequestMessage expectedRRQ = new RequestMessage(MessageType.RRQ, StateTestConfig.FILENAME);

            // Return the mock response on receivePacket
            OngoingStubbing<DatagramPacket> mockResponseBuilder = Mockito.when(socket.receive())
                    .thenReturn(new DatagramPacket(mockedDataSequence.get(0).toByteArray(), mockedDataSequence.get(0).toByteArray().length, connectionManagerSocketAddress));

            for(int i = 1; i < mockedDataSequence.size(); i++) {
                mockResponseBuilder.thenReturn(new DatagramPacket(mockedDataSequence.get(i).toByteArray(), mockedDataSequence.get(i).toByteArray().length, connectionManagerSocketAddress));
            }

            // Execute function
            ReadState readState = new ReadState(serverAddress, resourceManager, StateTestConfig.FILENAME, false, socket);
            readState.execute();


            // Verify number of requests received
            Mockito.verify(socket, Mockito.times(mockedDataSequence.size())).receive();

            // Verify first sent request is a RRRQ
            inOrder.verify(socket).sendMessage(requestArgument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Read Request Does Not Match", new String(expectedRRQ.toByteArray()),
                    new String(requestArgument.getValue().toByteArray()));

            // Verify second sent request is an ACK with same block number
            for(DataMessage dataMessage: mockedDataSequence) {
                inOrder.verify(mockedFile).writeBytesToFile(dataMessage.getData());
                inOrder.verify(socket).sendMessage(ackArgument.capture(), Mockito.eq(connectionManagerSocketAddress));
                Assert.assertEquals(
                        "Expected ACK Message with Block " + dataMessage.getBlockNum() + " Does Not Match",
                        dataMessage.getBlockNum(), ackArgument.getValue().getBlockNum()
                );
            }

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
