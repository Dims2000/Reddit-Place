package place.network;

import place.PlaceException;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A class that encapsulates all connections to a server. This class is the "controller" of the model-view-controller
 * as it facilitates the connections between the model and the server. When a server sends a message indicating that a
 * change has occurred, this class gets that connection and then can immediately forward it to the model. Likewise, a
 * model can tell this User to convey the server some information.
 * <p>
 * Last modified: 11/21/19
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
	 * The {@link ObjectInputStream} used as a gateway to reading responses from the server.
	 */
	private ObjectInputStream in;
	/**
	 * The {@link ObjectOutputStream} used as a gateway to writing to the server.
	 */
	private ObjectOutputStream out;

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
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			throw new PlaceException("Failed to connect to the server", e);
		}
	}

	/**
	 * Get the gateway that this User uses to read responses from the server. Warning: close() should NEVER be invoked on the
	 * {@link ObjectOutputStream} that is returned from this method as it is closed from within the close() method of this class.
	 *
	 * @return a BufferedReader that reads what the server sends this user
	 */
	public synchronized ObjectInputStream getInputStream() {
		return in;
	}

	/**
	 * Get the gateway that this User uses to write to the server. Warning: close() should NEVER be invoked on the
	 * {@link ObjectInputStream} that is returned from this method as it is closed from within the close() method of this class.
	 *
	 * @return a PrintStream that sends data to the server
	 */
	public synchronized ObjectOutputStream getOutputStream() {
		return out;
	}

	/**
	 * Close the {@link Socket} and I/O streams that this User holds.
	 */
	@Override
	public void close() {
		try {
			if (!socket.isClosed()) {
				if (socket != null)
					socket.close();
				if (in != null && socket != null)
					in.close();
				if (out != null && socket != null)
					out.close();
			}
		} catch (IOException e) {
			System.err.println("Attempt to close a resource more than once");
		}
	}
}
