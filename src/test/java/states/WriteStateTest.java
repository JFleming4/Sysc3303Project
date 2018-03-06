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

public class WriteStateTest {
	private TFTPDatagramSocket socket;
	private ResourceManager resourceManager;
    private InetSocketAddress serverAddress;
    private InetSocketAddress connectionManagerSocketAddress;
    private InOrder inOrder;
    private ResourceFile mockedFile;
    private File mockedParentFile;

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

        inOrder = Mockito.inOrder(socket);

        try {
            // Set up mocked file
            Mockito.when(mockedFile.exists()).thenReturn(StateTestConfig.WRITE_FILE_EXISTS);
            Mockito.when(mockedFile.createNewFile()).thenReturn(StateTestConfig.CREATE_NEW_FILE_RETURN);
            Mockito.when(mockedFile.getParentFile()).thenReturn(mockedParentFile);
            Mockito.when(mockedFile.canRead()).thenReturn(StateTestConfig.WRITE_FILE_CAN_READ);
            Mockito.when(mockedFile.getName()).thenReturn(StateTestConfig.FILENAME);
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
    public void WriteLessThan512BytesSuccess() {
        this.RunWriteTestByFileLength(256);
    }

    @Test
    public void Write512BytesSuccess() {
        this.RunWriteTestByFileLength(512);
    }

    @Test
    public void Write1024BytesSuccess() {
        this.RunWriteTestByFileLength(1024);
    }

    @Test
    public void WriteMoreThan1024BytesSuccess() {
        this.RunWriteTestByFileLength(-1);
    }

    @Test
    public void FileNotFoundErrorOnClient() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outStream));

        try {
            byte[] expectedWRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();
            Mockito.when(mockedFile.exists()).thenReturn(false);

            // Execute function
            new WriteState(serverAddress, resourceManager, StateTestConfig.FILENAME, false, socket).execute();
            Mockito.verify(socket, Mockito.times(0)).send( new DatagramPacket(expectedWRQBytes, expectedWRQBytes.length, serverAddress));
            Mockito.verify(socket, Mockito.times(0)).receive();
            Mockito.verify(mockedFile, Mockito.times(0)).readFileToBytes();

            // Ensure Message is displayed to the user
            Assert.assertTrue("File Not Found User Message Not Found", outStream.toString().contains("The file "+ StateTestConfig.FILENAME + " does not exist (or is not a file)"));
        }
         catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        System.setOut(System.out);
    }


    @Test
    public void FileAlreadyExistsOnServerError() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outStream));

        try {
            ArgumentCaptor<RequestMessage> requestArgument = ArgumentCaptor.forClass(RequestMessage.class);

            byte[] expectedWRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();
            String expectedErrorMessage = "File (" + StateTestConfig.FILENAME + ") already exists on the Server.";
            byte[] mockResponseErrorBytes = new ErrorMessage(ErrorMessage.ErrorType.FILE_EXISTS, expectedErrorMessage).toByteArray();

            Mockito.when(socket.receive()).thenReturn(new DatagramPacket(mockResponseErrorBytes, mockResponseErrorBytes.length, connectionManagerSocketAddress));

            Mockito.when(mockedFile.exists()).thenReturn(true);
            Mockito.when(mockedFile.readFileToBytes()).thenReturn(StateTestConfig.FILENAME.getBytes());

            // Execute function
            new WriteState(serverAddress, resourceManager, StateTestConfig.FILENAME, true, socket).execute();

            inOrder.verify(socket).sendMessage(requestArgument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Write Request Does Not Match", new String(expectedWRQBytes), new String(requestArgument.getValue().toByteArray()));

            Mockito.verify(socket, Mockito.times(1)).receive();

            // Ensure Message is displayed to the user
            Assert.assertTrue("File Not Found User Message Not Found", outStream.toString().contains(expectedErrorMessage));
        }
         catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        System.setOut(System.out);
    }

    @Test
    public void DiskFullOnServerWhenSendingLastDATAMessageError() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outStream));

        try {
            ArgumentCaptor<RequestMessage> requestArgument = ArgumentCaptor.forClass(RequestMessage.class);
            ArgumentCaptor<DataMessage> dataArgument = ArgumentCaptor.forClass(DataMessage.class);

            //Disk Full Error Message
            String expectedErrorMessage = "Not enough free space";
            byte[] mockResponseErrorBytes = new ErrorMessage(ErrorType.DISK_FULL, expectedErrorMessage).toByteArray();

            // Create mock packet sequence with half the message and then the disk full error message
            Mockito.when(resourceManager.fileExists(StateTestConfig.FILENAME)).thenReturn(true);
            Mockito.when(resourceManager.readFileToBytes(StateTestConfig.FILENAME)).thenReturn(StateTestConfig.FILE_STRING.getBytes());


            // build first response as the ack with block 0
            AckMessage mockResponseAckInitial = new AckMessage(0);
            OngoingStubbing<DatagramPacket> mockResponseBuilder = Mockito.when(socket.receivePacket())
                    .thenReturn( new DatagramPacket(mockResponseAckInitial.toByteArray(), mockResponseAckInitial.toByteArray().length, connectionManagerSocketAddress));

            // build rest of the ack responses for everything but last data block
            List<DataMessage> mockedDataSequence = DataMessage.createDataMessageSequence(StateTestConfig.FILE_STRING.getBytes());
            for (int i = 0; i < mockedDataSequence.size() - 1; i++) {
                AckMessage mockResponseAck = new AckMessage(mockedDataSequence.get(i).getBlockNum());
                mockResponseBuilder.thenReturn(new DatagramPacket(mockResponseAck.toByteArray(), mockResponseAck.toByteArray().length, connectionManagerSocketAddress));
            }
            //return the disk full error last
            mockResponseBuilder.thenReturn(new DatagramPacket(mockResponseErrorBytes, mockResponseErrorBytes.length, connectionManagerSocketAddress));

            // Execute function
            new WriteState(serverAddress, resourceManager, StateTestConfig.FILENAME, false, socket).execute();

            // verify resourceManager was called
            Mockito.verify(resourceManager).readFileToBytes((StateTestConfig.FILENAME));

            // Verify number of requests received
            Mockito.verify(socket, Mockito.times(mockedDataSequence.size() + 1)).receivePacket();

            // Verify first sent request is a WRQ
            inOrder.verify(socket).sendMessage(requestArgument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Write Request Does Not Match",
                    new String(new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray()),
                    new String(requestArgument.getValue().toByteArray())
            );

            // Verify sent request is an DATA Message with same block number
            for(int i = 0; i < mockedDataSequence.size() - 1; i++) {
                inOrder.verify(socket).sendMessage(dataArgument.capture(), Mockito.eq(connectionManagerSocketAddress));
                Assert.assertEquals(
                        "Expected DATA Message with Block " + mockedDataSequence.get(i).getBlockNum() + " Does Not Match",
                        mockedDataSequence.get(i).getBlockNum(),
                        dataArgument.getValue().getBlockNum()
                );
            }

            // Ensure Message is displayed to the user
            Assert.assertTrue("Disk Full User Message Not Found", outStream.toString().contains(expectedErrorMessage));
        }
         catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        System.setOut(System.out);
    }

    public void RunWriteTestByFileLength(int length) {
        try {
            ArgumentCaptor<RequestMessage> requestArgument = ArgumentCaptor.forClass(RequestMessage.class);
            ArgumentCaptor<DataMessage> dataArgument = ArgumentCaptor.forClass(DataMessage.class);

            // Expected Request for write file transfer
            byte[] expectedWRQBytes = new RequestMessage(MessageType.WRQ, StateTestConfig.FILENAME).toByteArray();

            // Create mock File Data messages
            String mockFile = length != -1 ? StateTestConfig.FILE_STRING.substring(0, length - 1) : StateTestConfig.FILE_STRING;
            Mockito.when(mockedFile.readFileToBytes()).thenReturn(mockFile.getBytes());
            Mockito.when(mockedFile.exists()).thenReturn(true);


            // Create response Ack Messages from the data message sequence on receivePacket

            List<DataMessage> mockedDataSequence = DataMessage.createDataMessageSequence(mockFile.getBytes());
            // build first response as the ack with block 0
            AckMessage mockResponseAckInitial = new AckMessage(0);
            AckMessage mockResponseAck = new AckMessage(mockedDataSequence.get(0).getBlockNum());
            OngoingStubbing<DatagramPacket> mockResponseBuilder = Mockito.when(socket.receive())
                    .thenReturn( new DatagramPacket(mockResponseAckInitial.toByteArray(), mockResponseAckInitial.toByteArray().length, connectionManagerSocketAddress));


            //Mockito.when(DataMessage.createDataMessageSequence())
            // build rest of the responses
            for (DataMessage dataMessage : mockedDataSequence) {
                mockResponseAck = new AckMessage(dataMessage.getBlockNum());
                mockResponseBuilder.thenReturn(new DatagramPacket(mockResponseAck.toByteArray(), mockResponseAck.toByteArray().length, connectionManagerSocketAddress));
            }

            // Execute function
            new WriteState(serverAddress, resourceManager, StateTestConfig.FILENAME, false, socket).execute();

            // Verify number of requests received
            Mockito.verify(socket, Mockito.times(mockedDataSequence.size() + 1)).receive();

            // Verify first sent request is a WRQ
            inOrder.verify(socket).sendMessage(requestArgument.capture(), Mockito.eq(serverAddress));
            Assert.assertEquals("Created Write Request Does Not Match", new String(expectedWRQBytes), new String(requestArgument.getValue().toByteArray()));

            // Verify sent request is an DATA Message with same block number
            for(DataMessage dataMessage : mockedDataSequence) {
                inOrder.verify(socket).sendMessage(dataArgument.capture(), Mockito.eq(connectionManagerSocketAddress));
                Assert.assertEquals(
                        "Expected DATA Message with Block " + dataMessage.getBlockNum() + " Does Not Match",
                        dataMessage.getBlockNum(),
                        dataArgument.getValue().getBlockNum()
                );
            }

            // verify resourceManager was called
            Mockito.verify(mockedFile).readFileToBytes();

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
