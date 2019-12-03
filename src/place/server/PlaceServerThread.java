package place.server;

import place.network.PlaceRequest;

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
public class PlaceServerThread extends Thread {
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
	 *
	 * @param clientServer
	 * @param client
	 */
	PlaceServerThread(PlaceServer clientServer, Socket client) {
		server = clientServer;
		this.client = client;
	}

	@Override
	public void run() {
		try (
			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())
		) {
			// Listen for LOGIN
			PlaceRequest<?> maybeLogin = (PlaceRequest<?>) in.readUnshared();
			// If it's not LOGIN...
			if (maybeLogin.getType() != PlaceRequest.RequestType.LOGIN) {
				// ...then send back ERROR
				PlaceRequest<String> didntLogin = new PlaceRequest<>(
					PlaceRequest.RequestType.ERROR,
					"Did not receive an initial LOGIN request"
				);
				out.writeUnshared(didntLogin);
			} else {
				// The first request was LOGIN
				// Now check for valid username
				String desiredUsername = (String) maybeLogin.getData();
				// If the username is invalid, send back an ERROR
				// if ()
				// 		If valid, then move on to indefinitely listening for CHANGE_TILE
				// 		Else, also send back an ERROR

				// What the client has chosen to do
				PlaceRequest<?> clientAction;
				while ((clientAction = (PlaceRequest<?>) in.readUnshared()) != null) {

				}
			}
			client.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace(); // For now
		}
	}
}
