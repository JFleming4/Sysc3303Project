SYSC 3303 Iteration 2
======================


Team contribution breakdown:
============================
Justin Fleming: 100934170
    - Handle IO Errors
Noah Segal: 100911661
    - Handle IO Errors
Derek Stride: 100955939
    - Refactor to introduce state-based behaviour
Michael Vezina: 100934579
    - UML & Timing Diagrams
Irusha Vidanamadura: 100935300
    - Testing & Integration


Setup Instructions:
===================
To run the project you need to import the source files into Eclipse and compile the project.

To start the project run the class FTPServer. Enter `help` into the command line interface to view the available options.

[ Optional Step - Error Simulator ]
Start the Error Simulator by running the class ErrorSimulator. This is REQUIRED to use the -t (test) option when sending requests from the client, otherwise the request will fail.

Next, start the client by running the class FTPClient. Again, you can enter `help` into the command line interface to view the available options.

Run the following commands to test reading and writing to the server. You can add the `-v` flag to enable verbose logging and
`-t` to enable the error simulator for that transfer (the Error Simulator must be running - see the optional step above).

read read-test.txt localhost
write write-test.txt localhost


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
    - socket/TFTPDatagramSocket.java - A wrapper around DatagramSocket for ease of use
    - states/ - State behaviour for reading, writing, input, and exiting
    - ErrorSimulator.java - The Error Simulator
    - FTPClient.java - The Java Client
    - FTPServer.java - The Java server
/src/test/java/
    - formats/ - Message Testing suite
    - parsing/ - Client Command tests
/resources/client - The directory where the client looks for reads/writes
/resources/server - The directory where the server looks for reads/writes
/resources/diagrams - The diagrams required for this iteration
