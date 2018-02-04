build-maven:
	mvn package
run-server:
	java -cp target/classes FTPServer
run-sim:
	java -cp target/classes ErrorSimulator
run-client:
	java -cp target/classes FTPClient

