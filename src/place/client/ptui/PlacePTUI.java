package place.client.ptui;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

public class PlacePTUI implements Observer<ClientModel, PlaceTile>
{
    private static ClientModel model;

    private static PlaceBoard board;

    public void go(String[] args)
    {
        model = new ClientModel(args);
        model.addObserver(this);
        model.run();
    }

    @Override
    public void update(ClientModel model, PlaceTile tile)
    {

    }

    public static void main(String[] args)
    {
        if (args.length != 3)
            System.out.println("Usage: java PlaceClient host port username");

        new PlacePTUI().go(args);
    }
}
