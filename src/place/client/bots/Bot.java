package place.client.bots;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The parent abstract class that is extended by the other bot classes. This class has
 * similar basic behavior as the PlacePTUI class (except the board does not get printed).
 * The class does define the behavior of the bot, but instead makes the method responsible for
 * the behavior abstract. The classes that extend Bot implement the method. When any class extending
 * Bot must call start() - the execution runs concurrently on a separate thread.
 *
 * @author Dmitry Selin
 */
public abstract class Bot extends Thread implements Observer<ClientModel, PlaceTile>
{
    /** a copy of the server's PlaceBoard that gets referenced by inherited classes */
    private PlaceBoard board;

    /** the ClientModel that serves the same role as it does within PlacePTUI and PlaceGUI */
    private ClientModel model;

    /** an array of all the PlaceColors */
    private final PlaceColor[] COLORS = PlaceColor.values();

    /**
     * Creates a new bot - initializes model using the passed arguments
     *
     * args = [hostName, portNum, username]
     *
     * @param args the command line arguments in the shown format
     */
    public Bot (String[] args) { model = new ClientModel(args); }

    @Override
    public void run()
    {
        model.addObserver(this);
        model.start();

        board = model.getBoard(); // Initializes board (blocks the current thread until the server sends the board)

        while (model.getStatus() == ClientModel.Status.RUNNING) // The main loop of the Bot
            botActivity();
    }

    /**
     * A helper method that prints the most recent activity of the bot. Prints
     * out the most recent tile that the bot changed.
     *
     * @param row the row of the tile
     * @param col the column of the tile
     * @param color the color of the tile
     */
    public synchronized void printTileChange(int row, int col, PlaceColor color)
    {
        System.out.println("TILE CHANGE: row = " + row +
                " | col = " + col +
                " | color = " + color.getName() +
                " | date = " + LocalDate.now() +
                " | time = " + LocalTime.now());
    }

    /**
     * A helper method that sleeps the current thread for a given amount of time.
     *
     * @param milliseconds the number of milliseconds the thread should sleep for
     */
    public synchronized void pause(long milliseconds)
    {
        try {
            sleep(milliseconds);
        }
        catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Returns the current PlaceBoard
     *
     * @return the board
     */
    public synchronized PlaceBoard getBoard() { return board; }

    /**
     * Returns the ClientModel
     *
     * @return the model
     */
    public synchronized ClientModel getModel() { return model; }

    /**
     * Returns the array of colors
     *
     * @return COLORS array
     */
    public synchronized PlaceColor[] getColors() { return COLORS; }

    /**
     * The abstract method that determines the behavior of Bot - must be implemented by the
     * class that extends Bot.
     */
    public abstract void botActivity();

    @Override
    public synchronized void update(ClientModel model, PlaceTile placeTile) { board.setTile(placeTile); }
}