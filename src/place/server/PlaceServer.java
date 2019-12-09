package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.Observer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
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
 * @author Dmitry Selin
 * @since 12-3-2019
 * @modified 12-8-2019
 */
public class PlaceServer extends Thread
{
    /** the main PlaceBoard of the server */
    private PlaceBoard board;

    /**
     * The HashMap of active users where the key is the username
     * and the value is the PlaceServerThread of the user
     */
    private ConcurrentHashMap<String, PlaceServerThread> usernames = new ConcurrentHashMap<>();

    /** a listener thread that listens for any tile changes and records for statistics */
    private StatisticsListener statListener;

    /** the ServerSocket that client sockets connect to */
    private ServerSocket serverSocket;

    /** the port of the serverSocket */
    private final int PORT;

    /** the dimensions of board */
    private final int DIM;

    /**
     * Creates a new PlaceServer: initializes the port and board dimensions
     * of the new server
     *
     * @param port the port that clients must use to connect to the server
     * @param DIM the dimensions of board
     */
    public PlaceServer (int port, int DIM)
    {
        PORT = port;
        this.DIM = DIM;
    }

    /**
     * Returns the current PlaceBoard (used by PlaceServerThread)
     *
     * @return the PlaceBoard
     */
    public PlaceBoard getBoard() { return board; }

    /**
     * Changes a tile in board. This method also updates statListener and
     * pushes the recent tile change to the other user threads.
     *
     * @param tile the tile that was changed
     */
    public synchronized void changeBoardTile (PlaceTile tile)
    {
        board.setTile(tile);
        statListener.update(this, tile);
        updateServerThreads(tile);
    }

    /**
     * Utilized by PlaceServerThread. Adds the user and its respective thread to the
     * HashMap, usernames.
     *
     * @param username the username of the user
     * @param clientThread the PlaceServerThread that the respective user is running on
     */
    public synchronized void logIn (String username, PlaceServerThread clientThread) { usernames.put(username, clientThread); }

    /**
     * Utilized by PlaceServerThread. Removes a username from usernames. This method
     * is used when a user leaves the server
     *
     * @param username the username of the user
     */
    public synchronized void logOff (String username) { usernames.remove(username); }

    /**
     * Utilized by PlaceServerThread. Checks if the username already exists within usernames
     *
     * @param username the username of the client
     * @return if username exists within usernames
     */
    public boolean isUsernameValid (String username) { return !usernames.containsKey(username); }

    /**
     * This helper method updates all observer PlaceServerThread objects of a tile change that
     * occurred within board.
     *
     * @param tile the PlaceTile that was changed
     */
    private void updateServerThreads (PlaceTile tile)
    {
        for (Observer<PlaceServer, PlaceTile> observer: usernames.values())
            observer.update(this, tile);
    }

    /**
     * Closes the server. This method is called by ServerStopListener and closes all
     * PlaceServerThread threads that are running before closing serverSocket.
     */
    public void closeServer()
    {
        try
        {
            for (PlaceServerThread client : usernames.values())
                client.serverClosed();

            serverSocket.close();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * The main method of PlaceServer. This method contains the main loop of the server
     * where clients connect and become split into separate threads that communicate with
     * each other using the methods in PlaceServer.
     */
    @Override
    public void run()
    {
        /* Creates and starts a ServerStopListener - this object runs on a separate
        thread and waits for user input so that the server can be shut down gracefully */
        ServerStopListener stopListener = new ServerStopListener(this);
        stopListener.start();

        try
        {
            serverSocket = new ServerSocket(PORT);
            board = new PlaceBoard(DIM);
            statListener = new StatisticsListener(DIM);

            while (stopListener.isServerRunning()) // The main loop (runs until user initiates shutdown)
            {
                Socket client = serverSocket.accept(); // Blocked waiting for a client socket connection

                // This creates and starts a new thread that represents an individual client connection
                new PlaceServerThread(this, client).start();
            }

            // Sleeps for 1 second while PlaceServerThread objects finish running
            Thread.sleep(1000);
        }
        catch (IOException | InterruptedException e)
        {
            // Only display an error message if the server was not shut down voluntarily
            if (stopListener.isServerRunning())
                System.err.println(e.getMessage());
        }

        statListener.getStats(); // Display the stats for the run of the server
    }

    /**
     * The main method simply starts the server on a separate thread.
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

                new PlaceServer(port, DIM).start(); // Creates and starts the server
            }
            catch (NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * This internal static class runs on a separate thread and constantly checks for possible
     * user input. If the user hits ENTER while the server is running, a graceful shutdown of
     * the server will commence.
     */
    static class ServerStopListener extends Thread
    {
        /** a boolean that represents if the server is running (true) or not (false) */
        private boolean serverRunning;

        /** the server */
        private PlaceServer server;

        /**
         * Creates a new ServerStopListener: initializes serverRunning and server
         *
         * @param server the PlaceServer that this thread aims to shut down
         */
        public ServerStopListener (PlaceServer server)
        {
            serverRunning = true;
            this.server = server;
        }

        /**
         * Returns the serverRunning boolean (if the server is running or not).
         *
         * @return is the server currently running
         */
        public boolean isServerRunning() { return serverRunning; }

        /**
         * The main method of ServerStopListener that constantly checks for user
         * input in order to shut down the server successfully.
         */
        @Override
        public void run()
        {
            Scanner input = new Scanner(System.in); // The Scanner that gets standard input

            while (serverRunning)
            {
                // If the user hits the RETURN or ENTER key - the server will be shut down
                if (input.nextLine().equals(""))
                {
                    serverRunning = false;
                    server.closeServer();
                }
            }
        }
    }
}