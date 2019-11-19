package place.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The Place server is run on the command line as:
 *
 * $ java PlaceServer port DIM
 *
 * Where port is the port number of the host and DIM is the square dimension
 * of the board.
 *
 * @author Sean Strout @ RIT CS
 */
public class PlaceServer {
    /**
     * The main method starts the server and spawns client threads each time a new
     * client connects.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 2) {
            System.out.println("Usage: java PlaceServer port DIM");
        }
        else
        {
            try
            {
                ServerSocket socket = new ServerSocket(Integer.parseInt(args[0]));
                Socket client = socket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                String line;

                while ((line = in.readLine()) != null)
                {
                    System.out.println(line);
                    out.println(line);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}