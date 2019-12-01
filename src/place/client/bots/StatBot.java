package place.client.bots;

import place.PlaceColor;
import place.PlaceTile;

import java.util.Random;

/**
 * This bot place only one color randomly throughout the server board.
 * The number of tiles that StatBot places is finite, and once all the tiles
 * have been placed - stats for the bot are displayed mainly: the number of tiles
 * placed, the number of the same color tiles not board, and the percentage
 * of the server board covered by the StatBot color.
 *
 * @author Dmitry Selin
 */
public class StatBot extends Bot
{
    /** the number of tiles that the bot has left to place */
    private int tilesLeft;

    /** the total number of tiles that StatBot places */
    private final int TILES;

    /** the only color that StatBot places */
    private final PlaceColor COLOR;

    /**
     * Creates a new StatBot - initializes model using the passed arguments
     *
     * args = [hostName, portNum, username]
     *
     * @param args the command line arguments in the shown format
     * @param color the color that gets placed by StatBot
     * @param tiles the number of tiles that StatBot places
     */
    public StatBot(String[] args, PlaceColor color, int tiles)
    {
        super(args);
        COLOR = color;
        TILES = tiles;
        tilesLeft = TILES;
    }

    /**
     * The main activity method of StatBot. The bot continues placing tiles randomly until no more
     * tiles are left to place, then it displays the stats for its run and ends the connection with the server.
     */
    @Override
    public void botActivity()
    {
        if (tilesLeft > 0)
        {
            // Initializes random row and column values
            int row = new Random().nextInt(getBoard().DIM + 1);
            int col = new Random().nextInt(getBoard().DIM + 1);

            // Changes the tile in the server board based on row, col, and COLOR
            getModel().changeTile(new PlaceTile(row, col, getModel().getUsername(), COLOR, System.currentTimeMillis()));
            printTileChange(row, col, COLOR);

            pause(2000); // Sleeps for 2 seconds
            tilesLeft--;
        }
        else
        {
            getModel().endConnection();
            getStats();
        }
    }

    /**
     * Prints the stats corresponding stats for the StatBot run in the form...
     *
     * ---Stats---
     * TILES PLACED --- ###
     * {COLOR} TILES ON BOARD --- ###
     * PERCENTAGE OF THE BOARD COVERED --- ###%
     */
    private void getStats()
    {
        System.out.println("\n---STATS---");
        System.out.println("TILES PLACED --- " + TILES);

        int colorTiles = 0;

        // Determines the number of COLOR tiles on board by iterating through each row and column
        for (int row = 0; row < getBoard().DIM; row++)
        {
            for (int col = 0; col < getBoard().DIM; col++)
            {
                if (getBoard().getTile(row, col).getColor() == COLOR)
                    colorTiles++;
            }
        }

        System.out.println(COLOR.getName().toUpperCase() + " TILES ON BOARD --- " + colorTiles);

        // Determines the percentage of the board covered by this equation: percentage = 100 * ({COLOR} tiles / total tiles)
        System.out.println("PERCENTAGE OF BOARD COVERED --- " + 100 * (colorTiles / Math.pow(getBoard().DIM, 2)) + "%\n");
    }

    /**
     * The main method of StatBot that creates and runs a single StatBot based on
     * the entered command line arguments. The arguments must be in this format...
     *
     * args = [hostName, portNum, username, colorName, numOfTiles]
     *
     * @param args command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 5)
            System.out.println("Usage: java StatBot host port username color tiles");
        else
        {
            // Parses args, creates a StatBot, and runs the bot
            String host = args[0];
            String port = args[1];
            String username = args[2];
            String color = args[3].toUpperCase();
            int tiles = Integer.parseInt(args[4]);

            StatBot bot = new StatBot(new String[]{host, port, username}, PlaceColor.valueOf(color), tiles);
            bot.start();
        }
    }
}