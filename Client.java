import java.io.IOException;
import java.net.Socket;

/**
 * client class creates another thread using ClientThread to handle game
 */

public class Client
{
    /**
     * Runs client.
     * Gets IP address of the host
     * Connects to port 61616 of the server, which server class is also using.
     * Connects to it.
     * Sends the socket to ClientThread class for gameplay
     */

    public static void main(String[] args)
    {
        Socket socket = null;

        try
        {
            socket = getSocket();

            // Creats a new thread to handle file reading
            ClientThread thread = new ClientThread((socket));   //Sends socket to ClientThread  to play game

            thread.run();   //Runs the client function instead of creating a new thread.
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Gets IP address of the host
     * Creats a socket using the address and port 61616 which is also used by the server
     * Returns the socket
     */
    public static Socket getSocket() throws IOException
    {
        // Server runs at this address
        String address = "netprog1.csit.rmit.edu.au";                   // The same address is used by server

        //DEBUG
        // To test on local machiene.
        //String local_address = "localhost";

        Socket socket = new Socket(address, 61616);          // Socket created using address and port 61616

        //DEBUG
        System.out.println("Client created at : " + address);     // Testing address
        return socket;
    }
}
