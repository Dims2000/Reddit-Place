package place.client.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;

import java.util.List;

public class PlaceGUI extends Application implements Observer<ClientModel, PlaceTile> {
	/**
	 * The model that this GUI client uses
	 */
	private ClientModel model;

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

		for (int row = 1; row <= model.getBoard().DIM; row++)
			for (int col = 1; col <= model.getBoard().DIM; col++)
				tiles.add(new Rectangle(50, 50), col, row);

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
			// TODO: Set the ToggleButton's color

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
