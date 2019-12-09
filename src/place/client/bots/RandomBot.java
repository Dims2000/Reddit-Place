package place.client.bots;

import place.PlaceColor;
import place.PlaceTile;

import java.util.Random;

/**
 * The following bot places a tile in a random location on the server board.
 * The tile is of a random color within PlaceColor, and the RandomBot sleeps for
 * a random number of time from 1 to 10 seconds each time it place a tile.
 *
 * @author Dmitry Selin
 */
public class RandomBot extends Bot
{
    /**
     * Creates a new RandomBot by calling the Bot constructor
     *
     * @param args command line arguments
     */
    public RandomBot(String[] args) { super(args); }

    /**
     * The main activity method of RandomBot that places a random color tile in a random
     * location on the server PlaceBoard, and sleeps for a random period of time between 1-10 seconds
     */
    @Override
    public void botActivity()
    {
        // Initializes the random row, random column, and random color
        int row = new Random().nextInt(getBoard().DIM);
        int col = new Random().nextInt(getBoard().DIM);
        PlaceColor color = getColors()[new Random().nextInt(getColors().length)];

        // Changes a random tile on the server board based on the values initialized for row, col, and color
        getModel().changeTile(new PlaceTile(row, col, getModel().getUsername(), color, System.currentTimeMillis()));
        printTileChange(row, col, color); // Prints the information regarding the most recent tile change

        // pause(1000 * new Random().nextInt(10) + 1); // Sleeps for 1-10 seconds
        pause(500);
    }

    /**
     * The main method of RandomBot that creates a RandomBot based on the command line
     * arguments and runs it. The command line arguments must be in the form...
     * <p>
     * args = [hostName, portNum, username]
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        switch (args.length) {
            case 3: {
                RandomBot bot = new RandomBot(args);
                bot.start();
            }
            case 2: {
                for (int i = 0; i < 20; i++) {
                    String username = String.format("Random Bot %d", i);
                    RandomBot bot = new RandomBot(new String[]{args[0], args[1], username});
                    bot.start();
                }
            }
            default:
                System.out.println("Usage: java RandomBot host port username");
        }
    }
}
