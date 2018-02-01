SYSC 3303 Iteration 1

To run the project you need to import the source files into eclipse and compile the project.

To start the project run the class FTPServer, you can enter `help` into the command line interface to view the available options.

Next, start the client by running the class FTPClient, again you can enter `help` into the command line interface to view the available options.

Run the following commands to test reading and writing to the server. You can add the `-v` flag to enable verbose logging and
`-t` to enable the error simulator for that transfer.

read read-test.txt localhost
write write-test.txt localhost

The locations for reading and writing are listed under the `resources` folder.
- `resources/client` for files on the client
- `resources/server` for files on the server

You can place files there and trying reading to the client folder or writing to the server folder. 
