package place.model;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
import place.network.PlaceRequest;
import place.network.User;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * The client side model that is used as the "M" in the MVC paradigm.  All client
 * side applications (PTUI, GUI, bots) are observers of this model.
 *
 * @author Sean Strout @ RIT CS
 * @author Dmitry Selin
 */
public class ClientModel extends Thread
{
    /**
     * The possible statuses of the client
     *
     * RUNNING: The model is running properly within the main loop
     *
     * READY: The model is ready to be run
     *
     * FINISHED: The main loop has finished properly and all connections and streams
     *           have been closed. Set to this status when endConnection() is called.
     *
     * ERROR: An error occurred during runtime
     */
    public enum Status { RUNNING, READY, FINISHED, ERROR }

    /** the actual board that holds the tiles */
    private PlaceBoard board;

    /** the status of the client */
    private Status status;

    /** the User class that represents the client-server connection */
    private User user;

    /** the chosen username of the client */
    private String username;

    /** observers of the model (PlacePTUI and PlaceGUI - the "views") */
    private List<Observer<ClientModel, PlaceTile>> observers = new LinkedList<>();

    /**
     * Returns the PlaceBoard received from the server. This method blocks the thread that
     * accesses it until the board() method has finished executing without any errors or until
     * dismissViewThread() is called. This method may return null if the message received in invalid.
     *
     * @return the PlaceBoard sent by the server or null
     */
    public synchronized PlaceBoard getBoard()
    {
        if (board == null)
        {
            try {
                wait();
            }
            catch (InterruptedException e) {
                error("InterruptedException: An error occurred when waiting to receive BOARD from server");
            }
        }

        return board;
    }

    /**
     * Returns the status of the model
     *
     * @return the status
     */
    public Status getStatus() { return status; }

    /**
     * Returns the username of the model
     *
     * @return the username
     */
    public String getUsername() { return username; }

    /**
     * Add a new observer. of tiles. Sent only once from the server to a
     * client directly after a successful login attempt.
     *
     * @param observer the new observer
     */
    public void addObserver(Observer<ClientModel, PlaceTile> observer) { this.observers.add(observer); }

    /** Notify observers the model has changed. */
    private void notifyObservers(PlaceTile tile)
    {
        for (Observer<ClientModel, PlaceTile> observer: observers)
            observer.update(this, tile);
    }

    /** Severs the connection with the server and ends the ClientModel successfully */
    public void endConnection()
    {
        status = Status.FINISHED;
        user.close();
    }

    /** Releases the potential view thread waiting within the getBoard() method */
    private synchronized void dismissViewThread() { notify(); }

    /**
     * Creates a ClientModel. Splits args into respective variables and creates
     * the corresponding fields. Adds an observer to the new ClientModel.
     *
     * args = [host, port, username]
     *
     * @param args an array of command line arguments passed by the client's view class
     * @param observer the new observer
     */
    public ClientModel(String[] args, Observer<ClientModel, PlaceTile> observer)
    {
        try
        {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            username = args[2];
            user = new User(host, port);
            addObserver(observer);

            status = Status.READY;
        }
        catch (PlaceException e) // Catches a PlaceException thrown by the User constructor...
        {
            error(e.getMessage());
            System.exit(1); // ...and terminates the program
        }
    }

    /**
     * This helper methods initializes board by receiving the RequestType.BOARD protocol
     * from the server. The method notifies the client view thread waiting in the getBoard() method.
     */
    private synchronized void board()
    {
        try
        {
            //Receives the expected BOARD PlaceRequest from the server and validates that it
            PlaceRequest boardMessage = validateProtocol(user.getInputStream().readUnshared());

            //Verifies that boardMessage is not null and of type BOARD
            if (boardMessage != null && boardMessage.getType() == PlaceRequest.RequestType.BOARD)
                board = (PlaceBoard)boardMessage.getData();

            //Wakes up the view thread within getBoard()
            notify();
        }
        catch (ClassNotFoundException e) {
            error("ClassNotFoundException: An error occurred receiving BOARD from the server");
        }
        catch (IOException e) {
            error("IOException: The connection with the server was lost when verifying BOARD");
        }
    }

    /**
     * This private helper method represents the login (or initial) portion of
     * initiating a connection with the server
     *
     *
     * @param username the username of the client model
     * @return if the server was able to successfully receive LOGIN_SUCCESS and verify that the usernames matched
     */
    private boolean login(String username)
    {
        try
        {
            // Creates the LOGIN PlaceRequest: login = [type = RequestType.LOGIN, data = username]
            PlaceRequest<String> login = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username);
            user.getOutputStream().writeUnshared(login); // Sends login to the server

            // Accepts the incoming message from the server and validates it
            // Expected message: loginSuccess = [type = RequestType.LOGIN_SUCCESS, data = username]
            PlaceRequest loginSuccess = validateProtocol(user.getInputStream().readUnshared());

            //Only returns true if no errors were thrown, loginSuccess is formatted as expected, and usernames match
            if (loginSuccess != null && loginSuccess.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS)
                return loginSuccess.getData().equals(username);
        }
        catch (ClassNotFoundException e) {
            error("ClassNotFoundException: An error occurred receiving LOGIN_SUCCESSFUL from the server");
        }
        catch (IOException e) {
            error("IOException: The connection with the server was lost when verifying LOGIN");
        }

        return false;
    }

    /**
     * This method is only called when an error occurs or if the server sends an ERROR message.
     * The method sets status to ERROR, prints errorMessage and closes the connections within user.
     *
     * @param errorMessage a String that explains the error that occurred
     */
    private void error(String errorMessage)
    {
        status = Status.ERROR;
        System.err.println(errorMessage);
        user.close();
    }

    /**
     * This method is called when ClientModel receives a TILE_CHANGED message from the server.
     * It updates board and notifies the observers of the change within the state of the model.
     *
     * @param tile the PlaceTile that was modified inside board
     */
    private void changedTile(PlaceTile tile)
    {
        board.setTile(tile);
        notifyObservers(tile);
    }

    /**
     * Represents the CHANGE_TILE PlaceRequest: sends tile in a correctly
     * formatted PlaceRequest to the server
     *
     * @param tile the PlaceTile that was changed by the client
     */
    public void changeTile(PlaceTile tile)
    {
        try
        {
            PlaceRequest<PlaceTile> changedTile = new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile);
            user.getOutputStream().writeUnshared(changedTile);
        }
        catch (IOException e) {
            error("IOException: An error occurred sending CHANGE_TILE to server");
        }
    }

    /**
     * Validates that the incoming protocol (request) from the server is in the
     * correct PlaceRequest object form and the data being sent is in the correct form.
     *
     * @param request the serialized object received from the server
     * @return the PlaceRequest object sent by the server (if formatted correctly) or null (if formatted incorrectly)
     */
    private PlaceRequest validateProtocol(Object request)
    {
        if (request instanceof PlaceRequest) // Checks if the object is in the form: PlaceRequest
        {
            PlaceRequest comm = (PlaceRequest)request; // Casts PlaceRequest onto request

            switch (comm.getType()) // Checks if the data is in the correct form for what is expected of each RequestType
            {
                case LOGIN_SUCCESS:
                case ERROR:
                    if (comm.getData() instanceof String)
                        return comm;
                    break;
                case BOARD:
                    if (comm.getData() instanceof PlaceBoard)
                        return comm;
                    break;
                case TILE_CHANGED:
                    if (comm.getData() instanceof PlaceTile)
                        return comm;
            }
        }

        // If the conditions above are not satisfied - close connections and return null
        error("PlaceException: Invalid data received from the server");
        return null;
    }

    /**
     * The main method of ClientModel that contains the main loop that receives protocol messages
     * from the server, initiates the login process, and closes the connection with the server (if necessary)
     */
    @Override
    public void run()
    {
        status = Status.RUNNING; // Once this method is run: status becomes RUNNING

        try
        {
            if (!login(username)) // If the login process fails - throw an IOException
                throw new IOException("IOException: The username returned by the server did not match the client");

            board(); // Gets the board from the server

            while (status == Status.RUNNING) // The main loop
            {
                // Validates the protocol coming from the server
                PlaceRequest protocol = validateProtocol(user.getInputStream().readUnshared());

                if (protocol != null) // If protocol does return null, status would have already changed to ERROR
                {
                    switch (protocol.getType())
                    {
                        case ERROR:
                            String errorMessage = (String)protocol.getData();
                            error(errorMessage);
                            break;
                        case TILE_CHANGED:
                            PlaceTile tile = (PlaceTile)protocol.getData();
                            changedTile(tile);
                            break;
                        default: // If the message received was of an incorrect type - throw an IOException
                            throw new IOException("IOException: The message sent by the server is an invalid protocol");
                    }
                }
            }
        }
        catch (ClassNotFoundException e)
        {
            error("ClassNotFoundException: An error occurred receiving " +
                    "data from the server during the main loop");
        }
        catch (IOException e)
        {
            if (status != Status.FINISHED) // If the program did not exit by setting status to FINISHED (by calling
                error(e.getMessage());     // endConnection()), then call error()
        }
        finally // Dismisses a possible view thread and closes all connections
        {
            dismissViewThread();
            user.close();
        }
    }
}