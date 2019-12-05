package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.Observer;
import place.network.PlaceRequest;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * TODO: Documentation
 *
 * @author Joey Territo
 * @since 12/3/19
 */
public class PlaceServerThread extends Thread implements Observer<PlaceServer, PlaceTile>, Closeable {
	/**
	 * The PlaceServer this client's thread is running on
	 */
	private PlaceServer server;
	/**
	 * TODO: Documentation
	 */
	private Socket client;
	/**
	 * TODO: Documentation
	 */
	private PlaceBoard board;
	/**
	 * TODO: Documentation
	 */
	private String username;
	/**
	 * TODO: Documentation
	 */
	private ObjectInputStream in;
	/**
	 * TODO: Documentation
	 */
	private ObjectOutputStream out;
	/**
	 * TODO: Documentation
	 */
	private Status status;

	/**
	 * TODO: Documentation
	 */
	enum Status {
		RUNNING,
		ERROR,
	}

	/**
	 * TODO: Documentation
	 *
	 * @param clientServer
	 * @param client
	 */
	public PlaceServerThread(PlaceServer clientServer, Socket client) {
		server = clientServer;
		this.client = client;
		board = clientServer.getBoard();
		username = "";
		status = Status.RUNNING;

		try {
			out = new ObjectOutputStream(client.getOutputStream());
			in = new ObjectInputStream(client.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try (this) {
			// Listen for LOGIN
			PlaceRequest<?> maybeLogin = (PlaceRequest<?>) in.readUnshared();
			try {
				username = (String) maybeLogin.getData();
			} catch (ClassCastException e) {
				username = "";
			}
			// If it's not LOGIN...
			if (maybeLogin.getType() != PlaceRequest.RequestType.LOGIN) {
				// ...then send back ERROR
				PlaceRequest<String> didntLogin = new PlaceRequest<>(
					PlaceRequest.RequestType.ERROR,
					"Did not receive an initial LOGIN request"
				);
				out.writeUnshared(didntLogin);
			} else if (!server.isUsernameValid((String) maybeLogin.getData())) {
				/* The first request was LOGIN, but the username was invalid
				   Since the username is invalid, send back an ERROR */
				PlaceRequest<String> usernameTaken = new PlaceRequest<>(
					PlaceRequest.RequestType.ERROR,
					String.format("A user with the username %s is already logged in", username)
				);
				out.writeUnshared(usernameTaken);
			} else {
				// Login was successful
				PlaceRequest<String> loginSuccessful = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN_SUCCESS, username);
				out.writeUnshared(loginSuccessful);
				// The board is sent only once directly after a successful login attempt
				PlaceRequest<PlaceBoard> initialBoard = new PlaceRequest<>(PlaceRequest.RequestType.BOARD, board);
				out.writeUnshared(initialBoard);
				// Tell the main server about a new username
				server.logIn(username, this);
				// Display the username and IP address of a client when they login
				System.out.printf("%s (%s) has entered the chat\n", username, client.getInetAddress());
				// Move on to indefinitely listening for CHANGE_TILE
				PlaceRequest<?> maybeChangeTile;
				while (status == Status.RUNNING && (maybeChangeTile = (PlaceRequest<?>) in.readUnshared()) != null) {
					// Whenever a CHANGE_TILE request is received, change the requested tile
					if (maybeChangeTile.getType() == PlaceRequest.RequestType.CHANGE_TILE) {
						PlaceTile tileToChange = (PlaceTile) maybeChangeTile.getData();
						// When a tile change comes in, it should be recorded by the server with a timestamp of the current time
						tileToChange.setTime(System.currentTimeMillis());
						server.changeBoardTile(tileToChange);

						PlaceRequest<PlaceTile> tileChanged = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tileToChange);
						out.writeUnshared(tileChanged);
						sleep(500);
					}
				}
			}
		} catch (Exception ignored) {
		}
		/* Detect when the client closes the connection
		Tell the main server that the username that this client was using is now available */
		server.logOff(username);
		// Display a message when a user logs off
		System.out.printf("%s (%s) has left the chat\n", username, client.getInetAddress());
	}

	@Override
	public void update(PlaceServer placeServer, PlaceTile placeTile) {
		board.setTile(placeTile);

		try {
			if (!client.isClosed() && !client.isOutputShutdown())
				out.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, placeTile));
			else
				status = Status.ERROR;
		} catch (IOException e) {
			// Thread dies
			try {
				close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (client != null && !client.isClosed())
			client.close();
		if (in != null && client != null && !client.isClosed())
			in.close();
		if (out != null && client != null && !client.isClosed())
			out.close();
	}
}
