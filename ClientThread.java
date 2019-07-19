import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Gets socket from client class.
 * Runs the game from server.
 */
public class ClientThread extends Thread

{
    private Socket socket;

    // A list of game states, used to check if the state of the game
    // There are 3 game states,
    // R - Register where client user name
    // G - Guess where client guesses the number
    // GO - Game Over, either if client used 4 tries or Won the game.
    private final String STATE[] = {"R","G", "GO", "SA"};
    // To send confirmation to continue to server
    private final String CONTINUE_MESSAGE = "c";

    //Message when server asks to wait.
    private final String STAYALIVE_MESSAGE = "Still Waiting for Other Players.";

    // When message to send is too short.
    private final String MESSAGE_SHORT = "Message to server is too short. Try again.";
    // When message to send is too long.
    private final String MESSAGE_LONG = "Message to server is too long. Try again.";

    //Maximum length of message to send to server.
    private final int MAX_OUTPUT = 25;

    public ClientThread(Socket socket)
    {
        //setting thread as user interface thread
        this.setDaemon(true);
        this.socket = socket;
    }

    //Runs the client thread concurrently
    public void run()
    {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        Scanner scanner = null;

        try
        {
            inputStream = socket.getInputStream();      //Gets inputStream from Server to read from server.

            outputStream = socket.getOutputStream();    //Gets outputStream from Server to write to server.

            scanner = new Scanner(System.in);           // Scanner for user input

            String serverInput, clientOutput;           // Strings to store serverInput and Client output.

            byte [] buffer = new byte[1024];


            while(true)
            {
                // Get input from server
                inputStream.read(buffer);
                serverInput = new String(buffer).replace("\0","");
                buffer = new byte[1024];

                // If the input is one of the STATE Commands
                // Loops according to the state.
                // Check if the game is over
                if (serverInput.equals(STATE[2]))   // If it is GO - GameOver
                {
                    break;
                }
                else if (serverInput.equals(STATE[3]))
                {
                    System.out.println(STAYALIVE_MESSAGE);  // If it is SA - Stay Alive
                    clientOutput = CONTINUE_MESSAGE;
                }
                else if (serverInput.equals(STATE[0]) || serverInput.equals(STATE[1])) // If it is R-register, G-Guess
                {
                    // Gets input from user either to register or to guess the number.
                    while(true)
                    {
                        clientOutput = scanner.nextLine();

                        // Check if the input is in range.
                        if(clientOutput.length() < 1)
                        {
                            System.out.println(MESSAGE_SHORT);
                        }
                        else if(clientOutput.length() > MAX_OUTPUT)
                        {
                          System.out.println(MESSAGE_LONG);
                        }
                        // If the input is within range, continue.
                        else
                        {
                            break;
                        }
                    }
                }

                // If the input is one NOT of the STATE Commands
                // Prints the input to user and sends confirmation to continue to SERVER.
                else
                {
                    System.out.println(serverInput);
                    clientOutput = CONTINUE_MESSAGE;
                }

                // Writes either the username, guess number or continue confirmation to server.
                outputStream.write(clientOutput.getBytes());
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        //Closing streams and connections
        finally
        {
            try
            {
                //Closing input-stream
                scanner.close();
                inputStream.close();

                //Closing output-stream
                outputStream.close();

                //Close connection
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
