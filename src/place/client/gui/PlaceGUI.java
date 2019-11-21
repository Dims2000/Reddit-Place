package place.client.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.util.EnumSet;
import java.util.List;

public class PlaceGUI extends Application implements Observer<ClientModel, PlaceTile> {
	/**
	 * The model that this GUI client uses
	 */
	private ClientModel model;
	/**
	 * A set of colors that are so dark that any text overlayed on top of them should be displayed white.
	 */
	private static final EnumSet<PlaceColor> DARK_COLORS = EnumSet.of(
		PlaceColor.BLACK,
		PlaceColor.MAROON,
		PlaceColor.RED,
		PlaceColor.OLIVE,
		PlaceColor.GREEN,
		PlaceColor.TEAL,
		PlaceColor.NAVY,
		PlaceColor.BLUE,
		PlaceColor.PURPLE,
		PlaceColor.FUCHSIA
	);

	@Override
	public void init() {
		List<String> args = getParameters().getRaw();
		model = new ClientModel(args.toArray(String[]::new));
		model.addObserver(this);
		model.start();
	}

    @Override
    public void start(Stage primaryStage) throws Exception {
		/*
		 * The Scene will be divided into 2 sections:
		 * (1) The upper section will be a GridPane of Rectangles representing the Place board.
		 * (2) The lower section will be a ToggleGroup of ToggleButtons representing the list of colors that the user
		 * can click on.
		 *
		 * A BorderPane is well-suited for this.
		 * */
		BorderPane rootNode = new BorderPane();
		// Section (1)
		rootNode.setCenter(makePlaceBoard());
		// Section (2)
		HBox colorControls = makeButtonRow();
		rootNode.setBottom(colorControls);

		Scene mainScene = new Scene(rootNode);
		primaryStage.setScene(mainScene);
		primaryStage.setTitle(
			String.format("Place: %s", model.getUsername())
		);
        primaryStage.show();
	}

	/**
	 * A utility method for generating a {@link GridPane} of {@link Rectangle}s to represent the Place board that
	 * takes up most of the window.
	 *
	 * @return a GridPane of Rectangles
	 */
	private GridPane makePlaceBoard() {
		GridPane tiles = new GridPane();

		PlaceBoard board = model.getBoard();

		for (int row = 1; row <= board.DIM; row++) {
			for (int col = 1; col <= board.DIM; col++) {
				// A visual representation of the tile
				Rectangle guiTile = new Rectangle(50, 50);
				// The actual tile the Rectangle will represent
				PlaceTile tileData = board.getTile(row - 1, col - 1);

				// Set the rectangle to be the tile's color
				PlaceColor tileColor = tileData.getColor();
				guiTile.setFill(Color.web(tileColor.getName()));
				// Add a tooltip to each rectangle
				String coordinate = String.format("(%d, %d)", tileData.getRow(), tileData.getCol());
				String tileOwner = tileData.getOwner();
				String timestamp = String.format(
					"%te/%tm/%ty\n%tl:%tM:%tS",
					tileData.getTime(),
					tileData.getTime(),
					tileData.getTime(),
					tileData.getTime(),
					tileData.getTime(),
					tileData.getTime()
				);

				Tooltip t = new Tooltip(
					String.format("%s\n%s\n%s", coordinate, tileOwner, timestamp)
				);
				t.setShowDelay(Duration.millis(500));
				Tooltip.install(guiTile, t);

				tiles.add(guiTile, col, row);
			}
		}

		return tiles;
	}

	/**
	 * A utility method for generating an {@link HBox} of {@link ToggleButton}s to represent the row of
	 * buttons at the bottom of the screen that the user can click on to select a color.
	 *
	 * @return an HBox of ToggleButtons
	 */
	private HBox makeButtonRow() {
		HBox buttons = new HBox();
		ToggleGroup group = new ToggleGroup();
		// Create a ToggleButton for every PlaceColor
		for (PlaceColor color : PlaceColor.values()) {
			ToggleButton button = new ToggleButton(color.toString());
			// Set the ToggleButton to be part of the same group so that only one at a time can be "activated"
			button.setToggleGroup(group);
			// Button styling
			// TODO: Make the button more visible that it is clicked
			// Set the ToggleButton's color
			button.setBackground(
				new Background(
					new BackgroundFill(
						Color.web(color.getName()),
						CornerRadii.EMPTY,
						null
					)
				)
			);
			// If the color is dark (black text is hard to see), then make text white
			if (DARK_COLORS.contains(color))
				button.setTextFill(Color.WHITE);
			// Add the ToggleButton to the HBox
			buttons.getChildren().add(button);
		}
		return buttons;
	}

    @Override
    public void update(ClientModel model, PlaceTile tile) {
		// TODO: implement PlaceGUI.update
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceGUI host port username");
            System.exit(-1);
        } else {
            Application.launch(args);
        }
    }
}
