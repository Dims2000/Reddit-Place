package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.Observer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

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
public class PlaceServer extends Thread
{
    private PlaceBoard board;

    private ConcurrentHashMap<String, PlaceServerThread> usernames = new ConcurrentHashMap<>();

    // private StatisticsListener statListener;

    private final int PORT;

    private final int DIM;

    public PlaceServer (int port, int DIM)
    {
        PORT = port;
        this.DIM = DIM;
    }

    public synchronized PlaceBoard getBoard() { return board; }

    public synchronized void changeBoardTile (PlaceTile tile)
    {
        board.setTile(tile);
        // statListener.update(this, tile);
        updateServerThreads(tile);
    }

    public synchronized void logIn (String username, PlaceServerThread clientThread) { usernames.put(username, clientThread); }

    public synchronized void logOff (String username) { usernames.remove(username); }

    public synchronized boolean isUsernameValid (String username) { return !usernames.containsKey(username); }

    private void updateServerThreads (PlaceTile tile)
    {
        for (Observer<PlaceServer, PlaceTile> observer: usernames.values())
            observer.update(this, tile);
    }

    @Override
    public void run()
    {
        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            board = new PlaceBoard(DIM);
            // statListener = new StatisticsListener(DIM);

            while (true)
            {
                Socket client = serverSocket.accept();
                new PlaceServerThread(this, client).start();
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * The main method starts the server and spawns client threads each time a new
     * client connects.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 2)
            System.out.println("Usage: java PlaceServer port DIM");
        else
        {
            try
            {
                int port = Integer.parseInt(args[0]);
                int DIM = Integer.parseInt(args[1]);

                new PlaceServer(port, DIM).start();
            }
            catch (NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}