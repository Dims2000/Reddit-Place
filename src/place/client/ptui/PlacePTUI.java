package place.client.ptui;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.io.PrintWriter;
import java.util.Scanner;

public class PlacePTUI extends ConsoleApplication implements Observer<ClientModel, PlaceTile>
{
    private PlaceBoard board;

    private ClientModel model;

    private final PlaceColor[] COLORS = PlaceColor.values();

    @Override
    public void go(Scanner consoleIn, PrintWriter consoleOut)
    {
        model = new ClientModel(getArguments().toArray(String[]::new));
        model.addObserver(this);
        model.start();

        board = model.getBoard();
        System.out.println(board.toString());

        while (model.getStatus() == ClientModel.Status.RUNNING)
        {
            consoleOut.print("Change tile: row col color? ");
            consoleOut.flush();
            int[] tileValues = validateTileChange(consoleIn.nextLine());

            if (tileValues != null)
            {
                int row = tileValues[0]; int col = tileValues[1]; int color = tileValues[2];

                PlaceTile tile = new PlaceTile(row, col, model.getUsername(), COLORS[color], System.currentTimeMillis());
                board.setTile(tile);
                model.changeTile(tile);

                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        consoleIn.close();
        consoleOut.close();
    }

    public int[] validateTileChange(String command)
    {
        String[] components = command.split(" ");

        try
        {
            if (components.length == 3)
            {
                int row = Integer.parseInt(components[0]);
                int col = Integer.parseInt(components[1]);
                int color = Integer.parseInt(components[2],16);

                if ((row < board.DIM && row >= 0) && (col < board.DIM && col >= 0) && (color < COLORS.length && color >= 0))
                        return new int[]{row, col, color};
            }
            else if (components.length == 1)
            {
                int connectionTerminate = Integer.parseInt(components[0]);

                if (connectionTerminate == -1)
                    model.endConnection();
            }
        }
        catch (NumberFormatException ignored) {}

        return null;
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

        launch(PlacePTUI.class, args);
    }
}
