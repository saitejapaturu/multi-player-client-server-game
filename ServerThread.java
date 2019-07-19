import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Gets client's socket from server class.
 * Gets guesses from client and sends messages regarding game back to client.
 * Game class is used to know when to start the game, wait for other players.
 */
public class ServerThread implements Runnable
{

    private Socket clientSocket;                // Client socket
    private String clientName;                  // Client's username
    private int guessCounter = 0;               // Counts the numbers guessed
    private Server server;                      // Server
    private Game game;                          // Game lobby

    private int answer;                         // The answer to win the game, get from Game lobby

    // IO
    private InputStream inputStream;            // Gets inputStream from Client to read from client.
    private OutputStream outputStream;          // Gets outputStream from Client to write to client.

    private boolean clientWon = false;          // To check the state if client won.
    private boolean hold;                       // To check if client needs to be on hold
    private boolean timedout = false;           // To check if client has timedout

    //Final variables
    private final int MAX_GUESSES = 4;          // Maximum number of guesses
    private final int MIN_GUESS_RANGE = 0;      // The lowest integer allowed to guess
    private final int MAX_GUESS_RANGE = 9;      // The highest integer allowed to guess
    private final int BUFFER = 1024;            // Buffer size
    private final int STAY_ALIVE_INTERVAL = 20; // How often to send Stay Alive messages for Client.
    private final int TIMEOUT_INTERVAL = 30;    // How long to wait for the client before timing out.

    //Stable Messages to Client

    // Message for Invalid Guesses.
    private final String INVALID_GUESS_MESSAGE = "Invalid Number! Please enter an integer between 0 - 9";
    // Message if the guess was lower than the answer
    private final String GUESS_LOWER_THAN_ANSWER_MESSAGE = "The number Guessed is smaller than the Answer.";
    // Message if the guess was higher than the answer
    private final String GUESS_HIGHER_THAN_ANSWER_MESSAGE = "The number Guessed is bigger than the Answer.";
    // Message if the guess was the answer.
    private final String CORRECT_GUESS = "The Guess is correct! Congratulations";
    // Message for client to decide to play again
    private final String PLAYAGAIN_MESSAGE = "Enter 'p' to play again or 'q' to quit.";
    // Message for client to wait for other players
    private final String WAIT_MESSAGE = "Waiting for other Players.";
    // Message for client to wait for other players to finish the game.
    private final String WAIT_TO_FINISH_MESSAGE = "Waiting for other players to finish the game to get results.";

    // Commands to check if the client wants to continue playing or quit.
    private final String PLAY = "p";
    private final String QUIT = "q";
    //Command to quit Guessing
    private final String EXIT = "e";


    // Blank line between turns
    private final String BLANK_LINE = "\n";

    //Game states
    //There are 3 game states,
    // R - Register where client user name
    // G - Guess where client guesses the number
    // GO - Game Over, either if client used 4 tries or Won the game.
    // SA - Stay Alive
    private final String STATE[] = {"R","G", "GO", "SA"};

    //Gets client's socket and server from server class
    // Assigns the parameters and input and output streams
    public ServerThread(Socket ClientSocket, Server server)
    {
        this.clientSocket = ClientSocket;
        this.server = server;
        try
        {
            inputStream = clientSocket.getInputStream();
            outputStream = clientSocket.getOutputStream();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //Runs the server thread concurrently
    @Override
    public void run()
    {
        //DEBUG
        //When a new client joins server
        System.out.println("New Client joined.\n" + Thread.activeCount() + " threads are running on server.\n");

        //Registers this thread with Server
        server.registerClient(inputStream, outputStream, this);

        try
        {
            do
            {
                // Wait for more players to join the game.
                stayAlive();

                // Starts game
                startGame(game);

                // Notifies game about this client finishing the game
                game.playerFinished();

                // Checks if timed out.
                if(timedout)
                {
                    return;
                }

                // Wait for other clients to finish the game
                stayAlive();

                // Print results of each player in game.
                game.getResults(inputStream, outputStream);
            } while (playAgain(server));                    // Loops client chose to quit.

            // Final Stage, Game Over - GO
            // Sends message to client that the game finished.
            outputStream.write(STATE[2].getBytes());    //Game over.
            clientSocket.close();

        }
        catch (SocketTimeoutException e)    // If socket timeout.
        {
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Gets answer from game
    // Sends appropriate messages to client to start playing, guess number and wait for other players.
    private void startGame(Game game)
    {
        this.answer = game.getAnswer();     // Get's the required answer from game.

        try
        {
            String clientInput, outputMessage;

            // Step 1, start the game
            // Starts the game.

            // If client is idle for 30 seconds, the connection is terminated and client lost the game.
            clientSocket.setSoTimeout(TIMEOUT_INTERVAL*1000);

            //Sends welcome message to client
            Server.sendOutput(outputStream, inputStream, game.welcomePlayers());

            // Allows the client to guess until the tries are over or Client won.
            while (guessCounter < MAX_GUESSES && !clientWon)
            {
                //Sends message to tell the client to proceed guessing and indicates the no of guesses left.
                // Message to tell the Client to proceed guessing.
                String prcoeedToGuessMessage = clientName + ": You have " + (MAX_GUESSES - guessCounter)
                        + " guesses left." +("\n" + clientName
                        + ": Proceed with Guess no " + (guessCounter+1));

                Server.sendOutput(outputStream, inputStream, (BLANK_LINE + prcoeedToGuessMessage));

                clientInput = getInput(outputStream, inputStream, STATE[1]);    // G - Guess state

                //DEBUG info to server
                System.out.println("Guess received from user:" + this.clientName + " is :" + clientInput);

                if(clientInput.equals(EXIT))
                {
                    Server.sendOutput(outputStream, inputStream, WAIT_TO_FINISH_MESSAGE);
                    return;
                }

                // Checks the validity
                outputMessage = guessValidity(clientInput);

                //If it is a valid guess but not answer, then increments the guess counter.
                if (outputMessage.equals(GUESS_LOWER_THAN_ANSWER_MESSAGE) ||
                        outputMessage.equals(GUESS_HIGHER_THAN_ANSWER_MESSAGE))
                {
                    guessCounter++;
                }

                //If the guess was correct then ends the loop.
                else if(outputMessage.equals(CORRECT_GUESS))
                {
                    clientWon = true;
                }

                // Sends appropriate message to Client.

                Server.sendOutput(outputStream, inputStream, outputMessage);
            }
            Server.sendOutput(outputStream, inputStream, WAIT_TO_FINISH_MESSAGE);
        }

        catch (SocketTimeoutException e)    // If client doesn't guess for more than 30 seconds
        {
            System.out.println(this.clientName + " was idle for a long time and timed out.");
            timedout = true;
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Asks client if he wants to play again. returns true if we wants to play
    private boolean playAgain(Server server) throws IOException
    {
        String input;

        while(true)
        {
            Server.sendOutput(outputStream, inputStream, PLAYAGAIN_MESSAGE);
            input = getInput( outputStream,  inputStream, STATE[1]); // G - Guess state

            if(input.equals(PLAY))
            {
                resetServerThread();
                server.addToQueue(this);
                Server.sendOutput(outputStream, inputStream, WAIT_MESSAGE);
                return true;
            }
            else if(input.equals(QUIT))
            {
                return false;
            }
        }
    }

    // Game uses this method to send the result of this client.
    public String sendResult()
    {
        if(clientWon)
        {
            return (this.clientName + " won with " + (this.MAX_GUESSES - this.guessCounter) + " guesses remaining.");
        }
        else
        {
            return (this.clientName + " lost.");
        }
    }

    // Resets the thread for a fresh game.
    private void resetServerThread()
    {
        this.guessCounter = 0;
        this.clientWon = false;
        this.game = null;
    }

    /*
    *   Takes guess provided by the client.
    *   If the guess is not a integer or out of range returns Invalid message.
    *   If the guess is in range but less than or grater than answer then returns appropriate message.
    *   If the guess is correct, returns Win message.
    */
    private String guessValidity(String guess)
    {
        int guessNumber;

        // Covert Guess to string, if not a string return invalid message.
        try
        {
            guessNumber = Integer.parseInt(guess);
        }
        catch (NumberFormatException e)
        {
            return INVALID_GUESS_MESSAGE;
        }

        // Check if out of range.
        if (guessNumber < MIN_GUESS_RANGE || guessNumber > MAX_GUESS_RANGE)
        {
            return INVALID_GUESS_MESSAGE;
        }

        // Check if answer
        else if (guessNumber == answer)
        {
            return CORRECT_GUESS;
        }

        // Check if less than answer
        else if (guessNumber < answer)
        {
            return GUESS_LOWER_THAN_ANSWER_MESSAGE;
        }

        // Check if greater than answer
        else
        {
            return GUESS_HIGHER_THAN_ANSWER_MESSAGE;
        }
    }


    // Sends State command to Client, indicating current state. Get's input from client and returns input.
    private String getInput(OutputStream outputStream, InputStream inputStream, String state) throws IOException
    {
        byte [] buffer = new byte[BUFFER];
        String input_from_client = null;

        outputStream.write(state.getBytes());
        inputStream.read(buffer);

        //Convert to string
        input_from_client = new String(buffer).replace("\0","");

        return input_from_client;
    }

    //  Sends stay alaive message to client to notify to wait for other clients.
    private void stayAlive()
    {
        try
        {
            synchronized (this)
            {
                hold = true;
                while (hold)
                {
                    wait(STAY_ALIVE_INTERVAL*1000);
                    Server.sendOutput(outputStream,inputStream, STATE[3]);  // Stay alive
                }
            }
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        catch(SocketTimeoutException e)
        {
            return;
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    // Wakes the client from waiting for players and starts the game.
    // It ends the Stay alive method by making hold false.
    public void wake()
    {
        synchronized(this)
        {
            hold = false;
            notify();
        }
    }

    //  Sets client name
    public void setClientName(String clientName)
    {
        this.clientName = clientName;
    }

    //  Gets client name
    public String getClientName()
    {
        return this.clientName;
    }

    //  Sets game
    public void setGame(Game game)
    {
        this.game = game;
    }
}


