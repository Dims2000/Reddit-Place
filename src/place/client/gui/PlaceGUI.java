package place.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
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

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * A graphical user interface to the Place application. The GUI consists of a board of colored squares representing
 * the stats of the place board, and a row of buttons at the bottom that the user can click on to select which color
 * they want to paint the Place board with. This class encapsulates all the logic to create such a window.
 *
 * @author Joey Territo
 */
public class PlaceGUI extends Application implements Observer<ClientModel, PlaceTile> {
	/**
	 * The model that this GUI client uses
	 */
	private ClientModel model;
	/**
	 * A 2D matrix of {@link Rectangle}s that this GUI keeps track of so that an individual
	 * rectangle's state can easily be changed
	 */
	private Rectangle[][] tileGrid;
	/**
	 * A {@link ToggleGroup} that keeps track of all the bottoms at the bottom of the window to see which one is
	 * "activated."
	 */
	private ToggleGroup colorControlsGroup = new ToggleGroup();

	/**
	 * The scaling multiplier when zooming in
	 */
	private static final double SCALE_DELTA = 1.05;
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

	/**
	 * A class that tiles in a PlaceGUI use to respond to when they are clicked on. This is equivalent to creating a
	 * lambda to pass to setOnMouseClicked(), but creating a separate class allows the makePlaceBoard() method to not
	 * get unnecessarily large (in terms of lines of code). This class must keep track of the coordinates of the
	 * rectangle for which it is handling {@link MouseEvent}s.
	 */
	class MouseClickedEventHandler implements EventHandler<MouseEvent> {
		/**
		 * The row in the {@link PlaceBoard} that the rectangle that this MouseClickedEventHandler handles MouseEvents
		 * for resides in.
		 */
		private int row;
		/**
		 * The column in the {@link PlaceBoard} that the rectangle that this MouseClockedEventHandler handles MouseEvents
		 * for resides in.
		 */
		private int column;

		/**
		 * Create a new MouseClickedEventHandler for a rectangle at coordinates (row, col) in the PlaceBoard.
		 *
		 * @param row the row of the PlaceBoard that the rectangle is in
		 * @param col the column of the PlaceBoard that the rectangle is in
		 */
		MouseClickedEventHandler(int row, int col) {
			this.row = row;
			this.column = col;
		}

		@Override
		public void handle(MouseEvent mouseEvent) {
			// Change the color of the tile that was clicked on
			Platform.runLater(() -> {
				try {
					PlaceTile newState = new PlaceTile(
						row,
						column,
						model.getUsername(),
						(PlaceColor) colorControlsGroup.getSelectedToggle().getUserData(),
						Instant.now().toEpochMilli()
					);
					model.changeTile(newState);
				} catch (NullPointerException e) {
					// If the user does not select a color at first then a NullPointerException is thrown
					Alert errorMessage = new Alert(
						Alert.AlertType.ERROR,
						"You must pick a color to paint the canvas with first."
					);
					errorMessage.show();
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
	}

	@Override
	public void init() {
		List<String> args = getParameters().getRaw();
		model = new ClientModel(args.toArray(String[]::new));
		model.addObserver(this);
		model.start();
		// Block until board is gotten
		while (model.getBoard() == null) ;
	}

	@Override
	public void start(Stage primaryStage) {
		// Initialize the tileGrid with the dimensions of the board gotten from the server
		PlaceBoard board = model.getBoard();
		tileGrid = new Rectangle[board.DIM][board.DIM];
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
		GridPane placeBoard = makePlaceBoard();
		rootNode.setCenter(placeBoard);
		// Section (2)
		HBox colorControls = makeButtonRow(placeBoard);
		rootNode.setBottom(colorControls);
		BorderPane.setMargin(colorControls, new Insets(10, 0, 0, 0));

		Scene mainScene = new Scene(rootNode);
		primaryStage.setScene(mainScene);
		primaryStage.setTitle(
			String.format("Place: %s", model.getUsername())
		);
		// End connection when the window is closed
		primaryStage.setOnCloseRequest(e -> model.endConnection());
		primaryStage.show();
	}

	/**
	 * A utility method for generating a {@link GridPane} of {@link Rectangle}s to represent the Place board that
	 * takes up most of the window.
	 *
	 * @return a GridPane of Rectangles
	 */
	private GridPane makePlaceBoard() {
		final double MIN_SCALE = 0.1;
		final double MAX_SCALE = 10;

		GridPane tiles = new GridPane();

		// Make GridPane scrollable
		tiles.setOnScroll(scrollEvent -> {
			double scale = tiles.getScaleY();
			double oldScale = scale;

			if (scrollEvent.getDeltaY() < 0)
				scale /= SCALE_DELTA;
			else
				scale *= SCALE_DELTA;

			scale = clamp(scale, MIN_SCALE, MAX_SCALE);

			double f = (scale / oldScale) - 1;

			double dx = (scrollEvent.getSceneX() - (tiles.getBoundsInParent().getWidth() / 2 + tiles.getBoundsInParent().getMinX()));
			double dy = (scrollEvent.getSceneY() - (tiles.getBoundsInParent().getHeight() / 2 + tiles.getBoundsInParent().getMinY()));

			tiles.setScaleX(scale);
			tiles.setScaleY(scale);

			tiles.setTranslateX(tiles.getTranslateX() - (f * dx));
			tiles.setTranslateY(tiles.getTranslateY() - (f * dy));
			scrollEvent.consume();
		});

		// Add tiles to the GridPane
		PlaceBoard board = model.getBoard();

		for (int row = 1; row <= board.DIM; row++) {
			for (int col = 1; col <= board.DIM; col++) {
				// A visual representation of the tile
				Rectangle guiTile = new Rectangle(50, 50);
				tileGrid[row - 1][col - 1] = guiTile;
				// Add a click listener to the rectangle so that it can do model.changeTile() when clicked on

				// The coordinates of the rectangle being clicked on
				final int r = row - 1;
				final int c = col - 1;

				guiTile.setOnMouseClicked(new MouseClickedEventHandler(r, c));
				// The actual tile the Rectangle will represent
				PlaceTile tileData = board.getTile(r, c);

				// Set the rectangle to be the tile's color
				PlaceColor tileColor = tileData.getColor();
				guiTile.setFill(placeColor2JavaFXColor(tileColor));
				// Add a tooltip to each rectangle
				Tooltip t = makeTooltip(tileData);
				Tooltip.install(guiTile, t);

				tiles.add(guiTile, col, row);
			}
		}

		return tiles;
	}

	/**
	 * A utility method for generating an {@link HBox} of {@link ToggleButton}s to represent the row of
	 * buttons at the bottom of the screen that the user can click on to select a color. The last button, home, rescales
	 * the GridPane to the default settings in case the user zooms in or out too far.
	 *
	 * @param viewport the GridPane that the home button will adjust the zoom level of
	 * @return an HBox of ToggleButtons
	 */
	private HBox makeButtonRow(GridPane viewport) {
		HBox buttons = new HBox();
		// Create a ToggleButton for every PlaceColor
		for (PlaceColor color : PlaceColor.values()) {
			ToggleButton button = new ToggleButton(color.toString());
			// Set the ToggleButton to be part of the same group so that only one at a time can be "activated"
			button.setToggleGroup(colorControlsGroup);
			// Button styling
			// Set the ToggleButton's color
			button.setBackground(
				new Background(
					new BackgroundFill(
						placeColor2JavaFXColor(color),
						CornerRadii.EMPTY,
						null
					)
				)
			);
			// Make the black button the default selection
			if (color == PlaceColor.BLACK)
				colorControlsGroup.selectToggle(button);
			// Tell the ToggleButton what color it is
			button.setUserData(color);
			// If the color is dark (black text is hard to see), then make text white
			if (DARK_COLORS.contains(color))
				button.setTextFill(Color.WHITE);
			// Add the ToggleButton to the HBox
			buttons.getChildren().add(button);
		}
		// Add a button to reset the viewport
		Button etPhoneHome = new Button("Home");
		etPhoneHome.setOnAction(e -> {
			viewport.setScaleX(1.0);
			viewport.setScaleY(1.0);

			viewport.setTranslateX(0.0);
			viewport.setTranslateY(0.0);
		});

		buttons.getChildren().add(etPhoneHome);

		return buttons;
	}

	/**
	 * Create a tooltip for a certain {@link PlaceTile}. A tooltip will contain the following information:
	 * <ul>
	 *     <li>The tile's coordinate (row, col)</li>
	 *     <li>The current owner of the tile</li>
	 *     <li>The time the tile was changed in the format D/M/Y HH:MM:SS</li>
	 *     <li>The tile color</li>
	 * </ul>
	 *
	 * @param tileData the tile with the information to create the tooltip about
	 * @return a tooltip containing information about the given tile
	 */
	private Tooltip makeTooltip(PlaceTile tileData) {
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
		t.setGraphic(new Rectangle(30, 30, placeColor2JavaFXColor(tileData.getColor())));
		t.setContentDisplay(ContentDisplay.LEFT);

		return t;
	}

	@Override
	public void update(ClientModel model, PlaceTile tile) {
		// Notify the board that a tile has changed
		model.getBoard().setTile(tile);
		// And now change the GUI to reflect this change
		int row = tile.getRow();
		int col = tile.getCol();

		// NOTICE: I have no idea if I need Platform.runLater
		Platform.runLater(() -> {
			Rectangle square = tileGrid[row][col];
			square.setFill(placeColor2JavaFXColor(tile.getColor()));
			Tooltip.install(square, makeTooltip(tile));
		});
	}

	/**
	 * Convert a {@link PlaceColor} to a {@link Color}.
	 *
	 * @param placeColor the PlaceColor to convert to the JavaFX color
	 * @return the JavaFX color
	 */
	private static Color placeColor2JavaFXColor(PlaceColor placeColor) {
		return Color.web(placeColor.getName());
	}

	/**
	 * Ensure a value is within a certain range. This method will return the given value, x, unless it is out of bounds,
	 * in which case one of two things can happen:
	 * <ol>
	 *     <li>if the value is below the lower bound, then the lower bound is returned</li>
	 *     <li>if the value is above the upper bound, then the upper bound is returned</li>
	 * </ol>
	 *
	 * @param x   the value to return
	 * @param min the lower bound of the range
	 * @param max the upper bound of the range
	 * @return the value, unless it is out of bounds
	 */
	private static double clamp(double x, double min, double max) {
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
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
