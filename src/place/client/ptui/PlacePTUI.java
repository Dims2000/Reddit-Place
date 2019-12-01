package place.client.ptui;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * This class represents the Plain-Text User Interface for the client end of
 * Place. The design of this class is intentionally created to imitate the design of
 * how a JavaFX application is designed. PlacePTUI gets updated by ClientModel and sends
 * input to the server via the main method inside go().
 *
 * @author Dmitry Selin
 */
public class PlacePTUI extends ConsoleApplication implements Observer<ClientModel, PlaceTile>
{
    /** the PlaceBoard that holds the various PlaceTiles */
    private PlaceBoard board;

    /** the model of the PTUI - is responsible to handling all except visualizing the board and receiving user input */
    private ClientModel model;

    /** all the various PlaceColors in an array format */
    private final PlaceColor[] COLORS = PlaceColor.values();

    /**
     * The primary method of PlacePTUI that creates and starts the model, receives user input and sends it
     * to the model, and sleeps for the required time each time the model receives a user-inputted PlaceTile.
     * This method continues running until an error occurs or the user inputs -1 into the console.
     *
     * @param consoleIn  the source of the user input
     * @param consoleOut the destination where text output should be printed
     */
    @Override
    public void go(Scanner consoleIn, PrintWriter consoleOut)
    {
        model = new ClientModel(getArguments().toArray(String[]::new)); // Creates the model...
        model.addObserver(this);
        model.start(); // ...and starts it

        board = model.getBoard(); // Receives the board from the model (is blocked until notified)

        if (board != null)
        {
            System.out.println(board.toString()); // Prints the initial board

            while (model.getStatus() == ClientModel.Status.RUNNING) // This is the main loop of the class
            {
                consoleOut.print("Change tile: row col color? ");
                consoleOut.flush();

                int[] tileValues = validateTileChange(consoleIn.nextLine()); // Receives user input and validates it

                if (tileValues != null)
                {
                    // Parses the user input into each respective component of a PlaceTile
                    int row = tileValues[0]; int col = tileValues[1]; int color = tileValues[2];

                    // Creates the PlaceTile from the user input and updates the client board
                    PlaceTile tile = new PlaceTile(row, col, model.getUsername(), COLORS[color], System.currentTimeMillis());
                    model.changeTile(tile);

                    try {
                        Thread.sleep(500); // Sleeps for the required 500 milliseconds
                    }
                    catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }

        // Closes both input and output streams
        consoleIn.close();
        consoleOut.close();
    }

    /**
     * This private helper method validates if the user input (command) contains valid inputs
     * to create a PlaceTile. The string, command, is expected to be in the form...
     *
     * command = (int)row (int)col (int)color
     *
     * @param command the raw string from the console input
     * @return the parsed user input in the form [row, column, color] or null if command was invalid
     */
    private int[] validateTileChange(String command)
    {
        String[] components = command.split(" "); // Splits the command into its components by parsing by spaces

        try
        {
            if (components.length == 3) // Checks for the correct length
            {
                int row = Integer.parseInt(components[0]);
                int col = Integer.parseInt(components[1]);
                int color = Integer.parseInt(components[2],16); // Radix of 16 ensures that color is a hexadecimal

                // Validates the the values entered are valid in context to the board and the COLORS array
                if ((row < board.DIM && row >= 0) && (col < board.DIM && col >= 0) && (color < COLORS.length && color >= 0))
                        return new int[]{row, col, color};
            }
            else if (components.length == 1) // If the user entered one value, it could be -1 (which is a sentinel)
            {
                int connectionTerminate = Integer.parseInt(components[0]);

                if (connectionTerminate == -1)
                    model.endConnection();
            }
        }
        catch (NumberFormatException ignored) {} // Always catch a NumberFormatException if the values entered are not Integers

        return null;
    }

    /**
     * This method is called by the model to update a tile. This method is called when
     * the server broadcasts that a tile was changed by one of the clients.
     *
     * @param model the class that handles incoming and outgoing data from the server
     * @param tile the PlaceTile that was altered
     */
    @Override
    public void update(ClientModel model, PlaceTile tile)
    {
        board.setTile(tile);
        System.out.println(board.toString());
    }

    /**
     * The main method of PlacePTUI that simply checks if the number of command line
     * arguments is correct and then launches the go() method. The command line arguments
     * are expected to be in the form...
     *
     * args = [hostName, portNum, username]
     *
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 3)
            System.out.println("Usage: java PlacePTUI host port username");
        else
            launch(PlacePTUI.class, args);
    }
}