package place.server;

import place.PlaceColor;
import place.PlaceTile;
import place.model.Observer;

import java.util.HashMap;

/**
 * This class runs on a separate thread and listens for any tile changes that occur within
 * PlaceServer and takes note of these changes. Statistics get drawn up after PlaceServer is
 * shut down as to what happened during the run of the server.
 *
 * @author Dmitry Selin
 * @since 12-3-2019
 * @modified 12-8-2019
 */
public class StatisticsListener implements Observer<PlaceServer, PlaceTile>
{
    /** the total number of tile placed */
    private int tilesPlaced;

    /** a HashMap of colors used: key = color name, value = number of tiles placed with the corresponding color */
    private HashMap<String, Integer> colorsUsed = new HashMap<>();

    /** a HashMap of how popular each row of the server was: key = row, value = number of tiles placed on that row */
    private HashMap<Integer, Integer> rowPopularity = new HashMap<>();

    /**
     * a HashMap of the popularity of each column of the server: key = column,
     * value = number of tiles placed on that column
     */
    private HashMap<Integer, Integer> columnPopularity = new HashMap<>();

    /** the time that the StatisticsListener was created */
    private long initialTime;

    /**
     * Creates a new Statistics Listener that initializes each field.
     *
     * @param boardDimension the dimension of the PlaceBoard used by the PlaceServer
     */
    public StatisticsListener (int boardDimension)
    {
        initialTime = System.currentTimeMillis();
        tilesPlaced = 0;

        for (PlaceColor color : PlaceColor.values()) // Key = ColorName | value = Number times this color was used
            colorsUsed.put(color.getName(), 0);

        // Key = Row | value = Number times a tile was placed on this row
        for (int row = 0; row < boardDimension; row++)
            rowPopularity.put(row, 0);

        // Key = Column | value = Number times a tile was placed on this column
        for (int col = 0; col < boardDimension; col++)
            columnPopularity.put(col, 0);
    }

    /**
     * Simply computes the data received from the server and prints out the statistics
     */
    public void getStats ()
    {
        long endTime = System.currentTimeMillis(); // Sets a time that the server closed
        double minutesElapsed = (double)(endTime - initialTime)/60000; // Computes the number of minutes that elapsed

        System.out.println("\n-----STATS-----");
        System.out.println("\nTOTAL TILES PLACED --- " + tilesPlaced);
        System.out.println("TILES PLACED PER MINUTE --- " + (tilesPlaced/minutesElapsed));
        System.out.println("\nCOLOR POPULARITY:\n");

        if (tilesPlaced == 0) // If not tiles were placed - avoid a divide by 0 error
            tilesPlaced++;

        // Prints the percentage use of each color: -ColorName --- ###%
        for (String colorName : colorsUsed.keySet())
            System.out.println("-" + colorName.toUpperCase() + " --- " +
                    ((double)colorsUsed.get(colorName) / tilesPlaced) * 100 + "%");

        int mostPopularRow = 0;
        int leastPopularRow = 0;

        // Determines the most and least popular row for each run
        for (int row : rowPopularity.keySet())
        {
            if (rowPopularity.get(mostPopularRow) < rowPopularity.get(row))
                mostPopularRow = row;

            if (rowPopularity.get(leastPopularRow) > rowPopularity.get(row))
                leastPopularRow = row;
        }

        int mostPopularCol = 0;
        int leastPopularCol = 0;

        // Determines the most and least popular column for each run
        for (int col : columnPopularity.keySet())
        {
            if (columnPopularity.get(mostPopularCol) < columnPopularity.get(col))
                mostPopularCol = col;

            if (columnPopularity.get(leastPopularCol) > columnPopularity.get(col))
                leastPopularCol = col;
        }

        System.out.println("\nMOST POPULAR TILE --- (" + mostPopularRow + ", " + mostPopularCol + ")");
        System.out.println("LEAST POPULAR TILE --- (" + leastPopularRow + ", " + leastPopularCol + ")");
    }

    /**
     * Overrides the method inside Observer: updates each data structure accordingly in
     * accordance to the tile that was changed.
     *
     * @param placeServer the server where the tile change occurred
     * @param tile the tile that was changed
     */
    @Override
    public void update(PlaceServer placeServer, PlaceTile tile)
    {
        tilesPlaced++;

        // Adds 1 to to each each HashMap's respective values according to tile's color, row, and column
        colorsUsed.put(tile.getColor().getName(), colorsUsed.get(tile.getColor().getName()) + 1);
        rowPopularity.put(tile.getRow(), rowPopularity.get(tile.getRow()) + 1);
        columnPopularity.put(tile.getCol(), columnPopularity.get(tile.getCol()) + 1);
    }
}
