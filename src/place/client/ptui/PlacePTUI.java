package place.client.ptui;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public class PlacePTUI extends ConsoleApplication implements Observer<ClientModel, PlaceTile>
{
    private ClientModel model;

    @Override
    public void go(Scanner consoleIn, PrintWriter consoleOut)
    {
        List<String> args = getArguments(); // Command line arguments
        // By passing new String[0] to toArray() we are telling the method to return String[] instead of Object[]
        model = new ClientModel(args.toArray(new String[0]));
        model.addObserver(this);
        model.start();
    }

    @Override
    public void update(ClientModel model, PlaceTile tile)
    {
        PlaceBoard board = model.getBoard();
        board.setTile(tile);
        System.out.println(board.toString());
    }

    public static void main(String[] args)
    {
        if (args.length != 3)
            System.out.println("Usage: java PlaceClient host port username");

        launch(PlacePTUI.class, args);
    }
}
