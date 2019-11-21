package place.model;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
import place.network.PlaceRequest;
import place.network.User;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * The client side model that is used as the "M" in the MVC paradigm.  All client
 * side applications (PTUI, GUI, bots) are observers of this model.
 *
 * @author Sean Strout @ RIT CS
 */
public class ClientModel extends Thread
{
    public enum Status { RUNNING, NOT_RUNNING, FINISHED, ERROR }

    /** the actual board that holds the tiles */
    private PlaceBoard board;

    private Status status;

    private User user;

    private String username;

    /** observers of the model (PlacePTUI and PlaceGUI - the "views") */
    private List<Observer<ClientModel, PlaceTile>> observers = new LinkedList<>();

    public ClientModel(String[] args)
    {
        try
        {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            username = args[2];
            status = Status.NOT_RUNNING;
            user = new User(host, port);
        }
        catch (PlaceException e)
        {
            error(e.getMessage());
            System.exit(1);
        }
    }

    private void board()
    {
        try
        {
            PlaceRequest boardMessage = validateProtocol(user.getInputStream().readUnshared(), PlaceRequest.RequestType.BOARD);
            board = (PlaceBoard)boardMessage.getData();
        }
        catch (ClassNotFoundException e) {
            error("ClassNotFoundException: An error occurred receiving BOARD from the server");
        }
        catch (IOException e) {
            error("IOException: The connection with the server was lost when verifying BOARD");
        }
    }

    private boolean login(String username)
    {
        try
        {
            PlaceRequest<String> login = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username);
            user.getOutputStream().writeUnshared(login);

            PlaceRequest loginSuccess = validateProtocol(user.getInputStream().readUnshared(), PlaceRequest.RequestType.LOGIN_SUCCESS);

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
     * Add a new observer. of tiles. Sent only once from the server to a client directly after a successful login attempt.
     *
     * @param observer the new observer
     */
    public void addObserver(Observer<ClientModel, PlaceTile> observer) { this.observers.add(observer); }

    /**
     * Notify observers the model has changed.
     */
    private void notifyObservers(PlaceTile tile)
    {
        for (Observer<ClientModel, PlaceTile> observer: observers)
            observer.update(this, tile);
    }

    private void error(String errorMessage)
    {
        status = Status.ERROR;
        System.err.println(errorMessage);
        user.close();
    }

    public PlaceBoard getBoard() { return board; }

    public Status getStatus() { return status; }

    private PlaceRequest validateProtocol(Object request, PlaceRequest.RequestType expectedType)
    {
        if (request instanceof PlaceRequest)
        {
            PlaceRequest comm = (PlaceRequest)request;

            if (comm.getType() == expectedType)
            {
                switch (expectedType)
                {
                    case LOGIN_SUCCESS:
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
        }

        error("PlaceException: Invalid data received from the server");
        return null;
    }

    public String getUsername() { return username; }

    private void changedTile(PlaceTile tile) { notifyObservers(tile); }

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

    @Override
    public void run()
    {
        try
        {
            if (!login(username))
                throw new IOException("IOException: The username returned by the server did not match the client");

            board();
            status = Status.RUNNING;
            Object comm;

            while (status == Status.RUNNING && (comm = user.getInputStream().readUnshared()) != null)
            {
                if (!(comm instanceof PlaceRequest))
                    throw new IOException("IOException: The message sent by the server is not an instance of PlaceRequest");

                PlaceRequest protocol = (PlaceRequest)comm;

                switch (protocol.getType())
                {
                    case ERROR:
                        PlaceRequest error = validateProtocol(protocol, PlaceRequest.RequestType.ERROR);
                        String errorMessage = (String)error.getData();
                        error(errorMessage);
                        break;
                    case TILE_CHANGED:
                        PlaceRequest changedTile = validateProtocol(protocol, PlaceRequest.RequestType.TILE_CHANGED);
                        PlaceTile tile = (PlaceTile)changedTile.getData();
                        changedTile(tile);
                        break;
                    default:
                        throw new IOException("IOException: The message sent by the server is an invalid protocol");
                }
            }
        }
        catch (ClassNotFoundException e) {
            error("ClassNotFoundException: An error occurred receiving data from the server during the main loop");
        }
        catch (IOException e) {
            error(e.getMessage());
        }
        finally {
            status = Status.FINISHED;
            user.close();
        }
    }
}
