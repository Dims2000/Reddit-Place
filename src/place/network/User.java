package place.network;

import place.PlaceException;

import java.io.*;
import java.net.Socket;

/**
 * A class that encapsulates all connections to a server. This class is the "controller" of the model-view-controller
 * as it facilitates the connections between the model and the server. When a server sends a message indicating that a
 * change has occurred, this class gets that connection and then can immediately forward it to the model. Likewise, a
 * model can tell this User to convey the server some information.
 *
 * @author Joey Territo
 * @since 11/19/19
 */
public class User implements Closeable {
	/**
	 * The {@link Socket} used to communicate with the Place server.
	 */
	private Socket socket;
	/**
	 * The {@link BufferedReader} used as a gateway to reading responses from the server.
	 */
	private BufferedReader in;
	/**
	 * The {@link PrintStream} used as a gateway to writing to the server.
	 */
	private PrintStream out;

	/**
	 * Create a new User by attempting to connect to a server with a given host name and port number. The {@link Socket}
	 * and I/O streams that this User holds are not closed until this object is closed.
	 *
	 * @param host the name of the host to connect to
	 * @param port the port number to connect to
	 * @throws PlaceException if the connection failed
	 */
	public User(String host, int port) throws PlaceException {
		try {
			socket = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintStream(socket.getOutputStream(), true);
		} catch (IOException e) {
			throw new PlaceException("Failed to connect to the server", e);
		}
	}

	/**
	 * Get the gateway that this User uses to read responses from the server. Warning: close() should NEVER be invoked on the
	 * {@link BufferedReader} that is returned from this method as it is closed from within the close() method of this class.
	 *
	 * @return a BufferedReader that reads what the server sends this user
	 */
	public BufferedReader getInputStream() {
		return in;
	}

	/**
	 * Get the gateway that this User uses to write to the server. Warning: close() should NEVER be invoked on the
	 * {@link PrintStream} that is returned from this method as it is closed from within the close() method of this class.
	 *
	 * @return a PrintStream that sends data to the server
	 */
	public PrintStream getOutputStream() {
		return out;
	}

	/**
	 * Close the {@link Socket} and I/O streams that this User holds.
	 *
	 * @throws IOException if an I/O error occurs while closing the aforementioned resources
	 */
	@Override
	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (in != null)
			in.close();
		if (out != null)
			out.close();
	}
}
