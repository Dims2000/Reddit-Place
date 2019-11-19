package place.client.ptui;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

public class PlacePTUI implements Observer<ClientModel, PlaceTile>
{
    private ClientModel model;

    private PlaceBoard board;

    public void go(String[] args)
    {
        model = new ClientModel(args);
        model.addObserver(this);
        model.start();
    }

    @Override
    public void update(ClientModel model, PlaceTile tile)
    {
        board.setTile(tile);
        System.out.println(board.toString());
    }

    public static void main(String[] args)
    {
        if (args.length != 3)
            System.out.println("Usage: java PlaceClient host port username");

        new PlacePTUI().go(args);
    }
}
