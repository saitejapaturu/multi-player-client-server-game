import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server class handles a pool of ServerThread to handle game play
 */
public class Server
{
    /**
     * Runs server.
     * Connects to port 61616 of the server, which Client class is also using.
     * Connects to it.
     * When client connects sends the socket to ServerThread class for gameplay.
     * When there are 3 clients, starts the game.
     */

    // A list of clients
    private ArrayList<ServerThread> queue = new ArrayList<ServerThread>();
    private final static int BUFFERSIZE = 1024;
    // Length of players each game
    private final static int GAME_SIZE = 3;

    // Message to register Client
    private final String REGISTER_MESSAGE = "Register your username: (Maximum 25 characters)";
    private final String REGISTER_COMMAND = "R"; // command to tell client to register.
    private final static String SERVER_START_MESSAGE = "Server is running.\n";

    public static void main(String [] args)
    {
        //Creating a threadPool of 15 threads to save resource.
        ExecutorService executor = Executors.newFixedThreadPool(15);
        Server server = new Server();

        //Try to run game.
        server.resetQueue();

        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try
        {
            serverSocket = new ServerSocket(61616);       //Create a serversocket which binds to the server port

            // When server starts prints to server screen
            System.out.println(SERVER_START_MESSAGE);

            // Accepts clients and adds to thread pool.
            while(true)
            {
                // When client requests to connect, acccepts the connection and the socket returned will be stored.
                clientSocket = serverSocket.accept();           // Create a connection between server and client

                // Create a thread, which sends clientSocket to ServerThread class for game
                executor.execute(new ServerThread(clientSocket, server));  // Run the thread using threadPool
            }

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    // Adds the client into game queue.
    public void addToQueue(ServerThread serverThread)
    {
        synchronized (this.queue)
        {
            queue.add(serverThread);
        }
    }

    // Gets the queue of serverThreds
    public ArrayList<ServerThread> getQueue()
    {
        synchronized (this.queue)
        {
            return queue;
        }
    }

    // Clears the queue for next game
    public void resetQueue()
    {
        // Clear the queue.
        if (queue.size() >= GAME_SIZE)
        {
            for (int i = 0; i < GAME_SIZE; i++)
            {
                queue.remove(0);
            }
        }
        new Thread(new Game(this)).start();
    }

    // Register Client to Server
    public void registerClient(InputStream inputStream, OutputStream outputStream, ServerThread serverThread)
    {
        byte[] buffer = new byte[BUFFERSIZE];

        try
        {
            // Register client to Server
            sendOutput(outputStream, inputStream, REGISTER_MESSAGE);
            outputStream.write(REGISTER_COMMAND.getBytes());  // Register command
            inputStream.read(buffer);

            serverThread.setClientName(new String(buffer).replace("\0", ""));
            sendOutput(outputStream,inputStream, (serverThread.getClientName() + " has been registered." +
                    "\nWaiting for " + (GAME_SIZE - (queue.size() + 1)) + " more players to start the game.\n"));

            // Add them to waiting queue
            addToQueue(serverThread);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Sends output message to client and waits for the continue message.
    public static void sendOutput(OutputStream outputStream, InputStream inputStream, String output) throws IOException
    {
        byte [] buffer = new byte[BUFFERSIZE];

        outputStream.write(output.getBytes());
        inputStream.read(buffer);
    }
}
