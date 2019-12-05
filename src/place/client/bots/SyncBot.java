package place.client.bots;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.ClientModel;

public class SyncBot extends Bot
{
    private ImageBot pasteBot;

    private int[][] image;

    public SyncBot(String copyServerName, String copyServerPort, String pasteServerName, String pasteServerPort, String username)
    {
        super(new String[]{copyServerName, copyServerPort, username});
        pasteBot = new ImageBot(new String[]{pasteServerName, pasteServerPort, username}, image);
    }

    @Override
    public void update(ClientModel model, PlaceTile placeTile)
    {
        image[placeTile.getRow()][placeTile.getCol()] = placeTile.getColor().getNumber();
        // pasteBot.setImage(image);
    }

    private int[][] placeBoardTo2DArray (PlaceBoard board)
    {
        PlaceTile[][] tileArray = board.getBoard();
        int[][] integerArray = new int[tileArray.length][tileArray[0].length];

        for (int row = 0; row < tileArray.length; row++)
        {
            for (int col = 0; col < tileArray[0].length; col++)
                integerArray[row][col] = tileArray[row][col].getColor().getNumber();
        }

        return integerArray;
    }

    @Override
    public void run()
    {
        getModel().addObserver(this);
        getModel().start();

        image = placeBoardTo2DArray(getModel().getBoard());
        // pasteBot.setImage(image);
        pasteBot.start();
    }

    @Override
    public void botActivity() {}

    public static void main(String[] args)
    {
        SyncBot queen = new SyncBot("queen.cs.rit.edu", "50009",
                "pinkfloyd.cs.rit.edu", "50009", "SyncBot");
        queen.start();
    }
}