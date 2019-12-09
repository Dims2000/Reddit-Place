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
 * Since the {@link PlaceServer} is multi-threaded, each client that must connect to it runs on its own thread. This class
 * encapsulates the logic needed for a the server to interact with a client, including implementing the "server's half"
 * of the Place application protocol, namely LOGIN_SUCCESS, BOARD, TILE_CHANGED, and ERROR.
 * <p>
 * Notice this class implements {@link Closeable}. Each client thread has its own connection to the client that must be
 * closed. This is done automatically in this class' {@link PlaceServerThread#run} method.
 * <p>
 * Last-modified: 12/8/19
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
	 * The connection to the client this {@code PlaceServerThread} is handling
	 */
	private Socket client;
	/**
	 * A {@link PlaceBoard} that this thread uses to keep track of its own board state
	 */
	private PlaceBoard board;
	/**
	 * The name that the client wants to connect to the server with
	 */
	private String username;
	/**
	 * The gateway for sending requests to the client
	 */
	private ObjectInputStream in;
	/**
	 * The gateway for reading in requests from the client
	 */
	private ObjectOutputStream out;
	/**
	 * A flag to keep track of whether or not an error has occurred
	 */
	private Status status;

	/**
	 * A type to represent the two states of this thread: either running (an error has not yet occurred) or error (an
	 * error has occurred)
	 */
	enum Status {
		RUNNING,
		CLOSED,
		ERROR,
	}

	/**
	 * Create a new {@code PlaceServerThread}. Note this this does not <em>spawn</em> the thread, i.e. just creating
	 * the thread doesn't do much.
	 *
	 * @param clientServer which server this thread is running on
	 * @param client the client to connect to
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

	/**
	 * The main logic of this thread. A client thread:
	 * <ul>
	 *     <li>Listens for the client to LOGIN</li>
	 *     <li>Ensures the client logged in with a valid (not taken) username</li>
	 *     <li>Sends the client LOGIN_SUCCESS followed by BOARD</li>
	 *     <li>Listens for and handles CHANGE_TILE requests</li>
	 *     <li>Disconnects the client when the client chooses to log off</li>
	 * </ul>
	 */
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
			} else if (!server.isUsernameValid(username)) {
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

	/**
	 * Ends the connection between the server and the client by sending an error message to
	 * the client and changing the status to CLOSED
	 *
	 * @throws IOException if out.writeUnshared() throws an IOException
	 */
	public void serverClosed () throws IOException
	{
		status = Status.CLOSED;
		out.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Server Closed"));
	}

	/**
	 * The method that is called when this thread is alerted about a changed tile. It updates the state of this PlaceBoard
	 * and then sends TILE_CHANGED to the client.
	 *
	 * @param placeServer the server that this thread is running on
	 * @param placeTile   the tile that changed
	 */
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

	/**
	 * Closes all connections to the client, including the socket and object input/output streams.
	 *
	 * @throws IOException if an exception occurred while closing the Socket, ObjectInputStream, or ObjectOutputStream
	 */
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
