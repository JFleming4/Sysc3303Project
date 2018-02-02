SYSC 3303 Iteration 1
=====================

Team contribution breakdown:
=====================
Justin Fleming: 100934170
    - FTPServer Iteration 0
Noah Segal: 100911661
    - UCM and UML Diagrams
Derek Stride: 100955939
    - FTPClient Iteration 0 + Testing
    - Simple Logger
Michael Vezina: 100934579
    - Message Classes and Testing Suite
    - Refactoring FTPServer and TFTPDatagramSocket
Irusha Vidanamadura: 100935300
    - Steady-state read/write data transfer and File I/O
    - TFTPDatagramSocket wrapper

Setup Instructions:
=====================
To run the project you need to import the source files into eclipse and compile the project.

To start the project run the class FTPServer, you can enter `help` into the command line interface to view the available options.

Next, start the client by running the class FTPClient, again you can enter `help` into the command line interface to view the available options.

Run the following commands to test reading and writing to the server. You can add the `-v` flag to enable verbose logging and
`-t` to enable the error simulator for that transfer.

read read-test.txt localhost
write write-test.txt localhost


Test File Locations:
=====================
The locations for reading and writing are listed under the `resources` folder.
- `resources/client` for files on the client
- `resources/server` for files on the server

You can place files there and trying reading to the client folder or writing to the server folder. 


Diagrams:
=====================
The diagrams will be in /src/main/java/resources folder. They will contain
- UCM for Read File transfer
- UCM for Write File transfer
- UML Diagram

Project Structure
=====================
/src/main/java/
    - FTPClient.java - The Java Client
    - FTPServer.java - The Java server
    - components/ErrorSimulator.java - The Error Simulator created by Client
    - exceptions/ - projects specific exceptions for invalid packet and invalid commands
    - formats/ - All the message types that are involved in the TFTP protocol
    - logging/ - Simple logger
    - parsing/ - FTPClient command line interface actions separated by command
    - resources/client - The director where the client looks for reads/writes
    - resources/server - The director where the server looks for reads/writes
    - resources/diagrams - The Diagrams needed for iteration 1
    - socket/TFTPDatagramSocket.java - A wrapper around DatagramSocket for ease of use
/src/test/java/
    - formats/ - Message Testing suite
    - parsing/ - Client Command tests