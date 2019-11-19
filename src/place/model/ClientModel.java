package place.model;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

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
    public enum Status { ERROR, LOGIN_SUCCESSFUL, BOARD, TILE_CHANGED }

    /** the actual board that holds the tiles */
    private PlaceBoard board;

    private Socket socket;

    private ObjectInputStream in;

    private ObjectOutputStream out;

    /** observers of the model (PlacePTUI and PlaceGUI - the "views") */
    private List<Observer<ClientModel, PlaceTile>> observers = new LinkedList<>();

    public ClientModel(String[] args)
    {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];

        try
        {
            socket = new Socket(host, port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            login(username);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void login(String username) throws IOException
    {
        System.out.println("I have sent the LOGIN message");

        PlaceRequest<String> login = new PlaceRequest<String>(PlaceRequest.RequestType.LOGIN, username);
        out.writeUnshared(login);
    }

    /**
     * Add a new observer. of tiles. Sent only once from the server to a client directly after a successful login attempt.
     *
     * @param observer the new observer
     */
    public void addObserver(Observer<ClientModel, PlaceTile> observer) {
        this.observers.add(observer);
    }

    /**
     * Notify observers the model has changed.
     */
    private void notifyObservers(PlaceTile tile)
    {
        for (Observer<ClientModel, PlaceTile> observer: observers)
            observer.update(this, tile);
    }

    @Override
    public void run()
    {
        try
        {
            System.out.println(in.readUnshared().toString());
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
