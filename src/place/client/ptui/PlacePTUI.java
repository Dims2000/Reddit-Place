package place.client.ptui;

import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.util.Scanner;

public class PlacePTUI implements Observer<ClientModel, PlaceTile>
{
    private ClientModel model;

    private final PlaceColor[] COLORS = PlaceColor.values();

    public void go(String[] args)
    {
        model = new ClientModel(args);
        model.addObserver(this);
        model.start();

        Scanner input = new Scanner(System.in);

        while (model.getStatus() == ClientModel.Status.RUNNING)
        {
            System.out.print("Change tile: row col color? ");

            String[] components = input.nextLine().split(" ");

            if (components.length > 1 && Integer.parseInt(components[0]) == -1)
                break;

            while (components.length != 3)
                components = input.nextLine().split(" ");

            int row = Integer.parseInt(components[0]);
            int col = Integer.parseInt(components[1]);
            int color = Integer.parseInt(components[2],16);

            PlaceTile tile = new PlaceTile(row, col, model.getUsername(), COLORS[color]);
            model.changeTile(tile);

            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(ClientModel model, PlaceTile tile)
    {
        model.getBoard().setTile(tile);
        System.out.println(model.getBoard().toString());
    }

    public static void main(String[] args)
    {
        if (args.length != 3)
            System.out.println("Usage: java PlaceClient host port username");

        new PlacePTUI().go(args);
    }
}
