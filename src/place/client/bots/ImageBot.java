package place.client.bots;

import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;

/**
 * This class extends Bot and draws any 2D array of hexadecimal color values
 * assuming that the size of the given 2D array is smaller or equal to the board
 * size of the server.
 *
 * @author Dmitry Selin
 */
public class ImageBot extends Bot
{
    /** the 2D array that represents the image to be drawn */
    public int[][] image;

    /**
     * Creates an image bot by calling the super constructor in Bot and
     * initializing image
     *
     * @param args command line arguments
     * @param image a 2D array of hexadecimal color values
     */
    public ImageBot(String[] args, int[][] image)
    {
        super(args);
        this.image = image;
    }

    /**
     * The activity that the bot performs within its main loop. The ImageBot
     * iterates through each row and column of the server board and checks the
     * image array tile for a match in color - if different color than
     * the image, then change to match.
     */
    @Override
    public void botActivity()
    {
        if (image.length <= getBoard().DIM && image[0].length <= getBoard().DIM) // Checks if the image fits inside the server board, else end connection
        {
            for (int row = 0; row < image.length; row++) // Iterates through each row in image
            {
                for (int col = 0; col < image[0].length; col++) // Iterates through each column in image
                {
                    PlaceColor imageColor = getColors()[image[row][col]]; // Creates the corresponding PlaceColor to the location in image

                    if (getModel().getStatus() == ClientModel.Status.RUNNING && // Checks if the server is still running and...
                            getBoard().getTile(row, col).getColor() != imageColor) // ...if the tile colors on image and teh server board do not match
                    {
                        getModel().changeTile(new PlaceTile(row, col, getModel().getUsername(),
                                imageColor, System.currentTimeMillis())); // Changes the tile in server board to the color in image

                        printTileChange(row, col, imageColor); // Prints the information regarding the tile change
                        pause(1000); // Sleeps for 1 second
                    }
                }
            }

            pause(1000); // In order not to overwhelm the program if the image is correct - sleep for 1 second
        }
        else
        {
            System.err.println("The specified image is larger than the dimensions of the connected server");
            getModel().endConnection();
        }
    }

    public synchronized void setImage(int[][] image) { this.image = image; }

    /**
     * The main loop of ImageBot that initializes a 2D array (image) that holds a graphic.
     * The image can be customized directly by a programmer to fit any size server. The command line arguments that
     * are expected are as so...
     *
     * args = [hostName, portNum, username]
     *
     * @param args command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 3)
            System.out.println("Usage: java ImageBot host port username");
        else
        {
            int[][] image = {
                    {8, 8, 8, 8, 8, 8, 8, 8},
                    {8, 0, 0, 8, 8, 0, 0, 8},
                    {8, 0, 0, 8, 8, 0, 0, 8},
                    {8, 8, 8, 0, 0, 8, 8, 8},
                    {8, 8, 0, 0, 0, 0, 8, 8},
                    {8, 8, 0, 0, 0, 0, 8, 8},
                    {8, 8, 0, 8, 8, 0, 8, 8},
                    {8, 8, 8, 8, 8, 8, 8, 8} };

            ImageBot bot = new ImageBot(args, image);
            bot.start(); // Starts the ImageBot on a new thread
        }
    }
}