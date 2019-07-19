import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

/**
 * Gets a pool of 3 serverThreads from server
 * Generates random number for the game.
 * Notifies when to start the game and wait for other players
 * Announces result to client.
 */
public class Game implements Runnable
{
    private final String DASH_LINE = "\n----------------------------------------\n";
    // Message to start Game.
    private final String GAME_RULES = "Guess a number between 0-9.\n" +
                                        "Try to guess the number generated in 4 tries.\n" +
                                        "If you want to quit the game during guessing, enter: e.\n";
    private final String GAME_START_MESSAGE = DASH_LINE + "Random number has been generated. Game has begun.\n"
                                                + GAME_RULES;
    //Message when game session ends.
    private final String SESSION_END = "Game Session has been ended.";
    // Message to announce results to client.
    private final String RESULT_ANNOUNCEMENT = SESSION_END + DASH_LINE + "Results: ";

    private final int MAX_CLIENTS = 3;
    private final int MAX_GUESS_RANGE = 9;
    private final int MIN_GUESS_RANGE = 0;

    //Server to which clients were connected.
    private Server server;

    // To maintain a list of all clients.
    private ArrayList<ServerThread> serverThreads = new ArrayList<ServerThread>();

    // To keep track to disconnected clients.
    private int serverThreadCounter;

    // ANSWER to be guessed by clients.
    private final int ANSWER;

    // Gets server from server and generates random number for the game session.
    public Game(Server server)
    {
        this.server = server;
        this.ANSWER = new Random().nextInt(MAX_GUESS_RANGE - MIN_GUESS_RANGE + 1) + MIN_GUESS_RANGE;
    }

    @Override
    public void run()
    {
        try
        {
            // Sleeps until until 3 clients joined the game.
            while (server.getQueue().size() < MAX_CLIENTS)
            {
                Thread.sleep(10);
            }

            // The first 3 clients in the queue are added.
            for (int i = 0; i < MAX_CLIENTS; i++)
            {
                serverThreads.add(server.getQueue().get(i));
            }
            serverThreadCounter = serverThreads.size();

            // Start looking for clients for next game session on server
            server.resetQueue();

            // Wake up clients so that they can start the game
            for (ServerThread serverThread : serverThreads)
            {
                synchronized(serverThread)
                {
                    serverThread.setGame(this);
                    serverThread.wake();
                }
            }

            // Waits till all clients have finished their game, from playerFinished method
            synchronized(this)
            {
                wait();
            }

            // Wake up clients when all clients have finished the game
            for (ServerThread serverThread : serverThreads)
            {
                synchronized(serverThread)
                {
                    serverThread.wake();
                }
            }

            // Waits until all clients got the end results from getResults method
            while (serverThreadCounter > 0)
            {
                Thread.sleep(100);
            }

            System.out.println(SESSION_END);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    // Gets results from serverThread and notifies client.
    public void getResults(InputStream inputStream, OutputStream outputStream) throws IOException
    {
        Server.sendOutput(outputStream, inputStream, RESULT_ANNOUNCEMENT);
        for (ServerThread serverThread : serverThreads)
        {
            Server.sendOutput(outputStream, inputStream, serverThread.sendResult());
        }

        synchronized(this)
        {
            serverThreadCounter--;
        }
    }


    // When either of the clients finish the game, then this method is called.
    // The thread is woken when all 3 clients finish the game
    public void playerFinished()
    {
        synchronized(this)
        {
            serverThreadCounter--;
            if (serverThreadCounter == 0)
            {
                notify();
                serverThreadCounter = serverThreads.size();
            }
        }
    }

    // Returns Welcome message to introduce other players to client.
    public String welcomePlayers()
    {
        String playerlist = "";
        int counter = serverThreads.size() - 1;
        for (ServerThread serverThread : serverThreads)
        {
            playerlist += serverThread.getClientName();
            if (counter > 0)
            {
                playerlist += ", ";
                counter--;
            }
        }
        return GAME_START_MESSAGE + "\nPlayers: "+ playerlist;
    }

    // Returns ANSWER to client to perform calculations.
    public int getAnswer()
    {
        return this.ANSWER;
    }

}