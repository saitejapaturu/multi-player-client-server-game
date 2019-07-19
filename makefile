JFLAGS = -g
JC = javac
JVM= java
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Client.java \
	ClientThread.java \
	Game.java \
	Server.java \
	ServerThread.java 

default: compile

compile:
	$(JC) *.java

server: compile
	$(JVM) Server

client: compile 
	$(JVM) Client

clean:
	$(RM) *.class