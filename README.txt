SYSC 3303 Iteration 4
======================


Team contribution breakdown:
============================
Justin Fleming: 100934170
    - Handle IO Errors
    - Error Simulator
    - ErrorSimulator Invalid OP Code
Noah Segal: 100911661
    - Handle IO Errors
    - Handle duplicate, delay, lost packets
Derek Stride: 100955939
    - Refactor to introduce state-based behaviour
    - Error Simulator
    - UML Diagrams
Michael Vezina: 100934579
    - UML & Timing Diagrams
    - Handle duplicate, delay, lost packets
    - Handle packet format errors
Irusha Vidanamadura: 100935300
    - Testing & Integration
    - UML & Timing Diagrams
    - Invalid TID packet and Extend packet changes for Error Sim


Setup Instructions:
===================
To run the project you need to import the source files into Eclipse and compile the project.

To start the project run the class FTPServer. Enter `help` into the command line interface to view the available options.

[ Optional Step - Error Simulator ]
Start the Error Simulator by running the class ErrorSimulator. This is REQUIRED to use the -t (test) option when sending requests from the client, otherwise the request will fail.

Error Simulator Commands + Example:
 - lose: (lose DATA 1 0) drops the first data packet and all subsequent packets (2, 3, 4, etc.)
 - dup: (dup DATA 1 2) duplicates the first data packet and every second subsequent packet (3, 5, 7, etc.)
 - delay: (delay DATA 1 0 69000) delays the first data packet and all subsequent packets (2, 3, 4, etc.) for 69 seconds.
 - invop: (invop data 2) Send an invalid op code when you recieve data 2
 - invtid: (invtid data 2) Send a packet invalid tid code and then the normal packet when you receive data 2
 - extend: (extend data 1 4) - Extend every 4th Data Message with fake data.
 - normal: normal operation
 - help: "I need somebody! Help! Not just anybody!" – Lennon-McCartney. (You should know what this operation does)

Next, start the client by running the class FTPClient. Again, you can enter `help` into the command line interface to view the available options.

Run the following commands to test reading and writing to the server. You can add the `-v` flag to enable verbose logging and
`-t` to enable the error simulator for that transfer (the Error Simulator must be running - see the optional step above).

read read-test.txt localhost
write write-test.txt localhost

NOTE:
    If the ACK packet on the last DATA block is lost then the receiving node (Client or Server) will fail.
    However, the session still succeeds with the sending of the ACK.
    This follows the TFTP specification and is impossible to fix due to the socket timeout being the same for both sides (Client & Server)

Error Testing:
==============
ACCESS VIOLATION:
- Remove the read and write permissions on either file in /resources/server or /resources/client
- Testing the Server:
	- 'read read-test.txt localhost'
- Testing the Client:
	- 'write write-test.txt localhost'
	
FILE NOT FOUND:
- Testing the Server:
	- Delete read-test.txt from /resources/server
	- 'read read-test.txt localhost'
- Testing the Client:
	- Delete write-test.txt from /resources/client
	- 'write write-test.txt localhost'

DISK FULL:
- Run the the code from a full disk (i.e. memory stick or small partition). Both operations will result in a disk full error
- Testing the Server:
	- 'write write-test.txt localhost'
- Testing the Client:
	- 'read read-test.txt localhost'
	
FILE ALREADY EXISTS:
- Testing the Server:
	- 'write write-test.txt localhost' (first attempt, which creates the file)
	- 'write write-test.txt localhost' (second attempt, which returns an error)
- Testing the Client:
	- 'read read-test.txt localhost' (first attempt, which creates te file)
	- 'read read-test.txt localhost' (second attempt, which returns an error)

DUPLICATE PACKET:
- ErrorSim:
	- 'dup DATA 1 2' (duplicate the first DATA packet and every second (3, 5, 7, etc.)
- Client:
	- 'read read-test.txt localhost'
	or
	- 'write write-test.txt localhost'

LOST PACKET:
- ErrorSim:
	- 'lost DATA 1 2' (loses the first DATA packet and every second (3, 5, 7, etc.)
- Client:
	- 'read read-test.txt localhost'
	or
	- 'write write-test.txt localhost'
	
DELAYED PACKET:
- ErrorSim:
	- 'delay DATA 1 2' (delays the first DATA packet and every second (3, 5, 7, etc.)
- Client:
	- 'read read-test.txt localhost'
	or
	- 'write write-test.txt localhost'

Invalid OP Code PACKET:
- ErrorSim:
    - 'invop data 2' (Forward data 2, except replace the OP Code with an invalid op code)
- Client:
    - 'read read-test.txt localhost'
    or
    - 'write write-test.txt localhost'

Invalid TID PACKET:
- ErrorSim:
    - 'invtid data 2' (Forward data 2, using another TID)
- Client:
    - 'read read-test.txt localhost'
    or
    - 'write write-test.txt localhost'

Packet DATA too large:
- ErrorSim:
    - 'extend data 2' (Extend DATA packet byte array with block == 2 with fake data)
- Client:
    - 'read read-test.txt localhost'
    or
    - 'write write-test.txt localhost'

Test File Locations:
====================
The locations for reading and writing are listed under the `resources` folder.
- Client Files: `resources/client`
- Server Fies: `resources/server`

You can place files there and try to read to the client or write to the server. 


Diagrams:
=========
Diagrams are located in the resources folder. They contain:
- UCM for Read File transfer
- UCM for Write File transfer
- UML Diagram
- Timing Diagrams for Errors


Project Structure:
==================
/src/main/java/
    - exceptions/ - Projects specific exceptions for invalid packet and invalid commands
    - formats/ - All the message types that are involved in the TFTP protocol
    - logging/ - Simple logger
    - parsing/ - FTPClient command line interface actions separated by command
    - resources/ - Contains class for file i/o
    - session/ - Sessions for receiving and transmitting
    - socket/ - Contains TFTPDatagramSocket
    - states/ - State behaviour for reading, writing, input, and exiting
    - util/ - Determine if the ErrorSim needs to alter the packets
    - ErrorSimulator.java - The Error Simulator
    - FTPClient.java - The Java Client
    - FTPServer.java - The Java server
/src/test/java/
    - formats/ - Message Testing suite
    - parsing/ - Client Command tests
    - states/ - Session Testing suite
    - util/ - Util Testing suite
/resources/client - The directory where the client looks for reads/writes
/resources/server - The directory where the server looks for reads/writes
/resources/diagrams - The diagrams required for this iteration
