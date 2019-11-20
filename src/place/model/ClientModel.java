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
    public enum Status { RUNNING, NOT_RUNNING, ERROR }

    /** the actual board that holds the tiles */
    private PlaceBoard board;

    private Status status;

    private User user;

    private String username;

    /** observers of the model (PlacePTUI and PlaceGUI - the "views") */
    private List<Observer<ClientModel, PlaceTile>> observers = new LinkedList<>();

    public ClientModel(String[] args)
    {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        username = args[2];
        status = Status.NOT_RUNNING;

        try {
            user = new User(host, port);
        }
        catch (PlaceException e) {
            e.getMessage();
        }

    }

    public void login(String username) throws IOException
    {
        PlaceRequest<String> login = new PlaceRequest<String>(PlaceRequest.RequestType.LOGIN, username);
        user.getOutputStream().writeUnshared(login);

        try
        {
            Object comm = user.getInputStream().readUnshared();
            PlaceRequest loginSuccess;

            if (comm instanceof PlaceRequest)
                loginSuccess = (PlaceRequest)comm;
            else
                throw new IOException();

            if (loginSuccess.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS &&
            loginSuccess.getData().equals(username))
            {
                comm = user.getInputStream().readUnshared();
                PlaceRequest boardMessage;

                if (comm instanceof PlaceRequest)
                    boardMessage = (PlaceRequest)comm;
                else
                    throw new IOException();

                board = (PlaceBoard)boardMessage.getData();
                System.out.println(board.toString());
            }

        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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

    public PlaceBoard getBoard() { return board; }

    public Status getStatus() { return status; }

    public String getUsername() { return username; }

    public void changeTile(PlaceTile tile)
    {
        try
        {
            PlaceRequest<PlaceTile> changedTile = new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile);
            user.getOutputStream().writeUnshared(changedTile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        status = Status.RUNNING;

        try
        {
            login(username);

            Object comm;

            while (status == Status.RUNNING)
            {
                while ((comm = user.getInputStream().readUnshared()) != null)
                {
                    PlaceRequest protocol = null;

                    if (comm instanceof PlaceRequest)
                        protocol = (PlaceRequest)comm;

                    assert protocol != null;
                    switch (protocol.getType())
                    {
                        case ERROR:
                            break;
                        case TILE_CHANGED:
                            notifyObservers((PlaceTile)protocol.getData());
                    }
                }
            }
        }
        catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        finally {

        }
    }
}
