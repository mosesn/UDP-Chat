import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Holds common constants and methods.
 * @author moses
 */

public class NetworkNode {

	public static final int LENGTH = 1024;
	public static final int BIG_LEN = 1024;
	public static final byte[] ACK_BUFFER = "ACK".getBytes();
	protected static final int INT_LEN = 4;
	protected static final int DE_REG = -1;
	protected static final int REG = -2;
	protected static final String DEREG_STR = "DR";
	protected static final String MESSAGE = "MSG";
	protected static final String REG_STR = "RS";
	protected final DatagramSocket socket;
	
	public NetworkNode(DatagramSocket socket) {
		super();
		this.socket = socket;
	}

	protected void ack(DatagramPacket packet) throws IOException {
		DatagramPacket ackPacket = new DatagramPacket(ACK_BUFFER, ACK_BUFFER.length, packet.getAddress(), packet.getPort());
		socket.send(ackPacket);
	}

	protected String getMessage(String[] arr, int start) {
		StringBuilder builder = new StringBuilder();
		for (int index = start; index < arr.length; index++) {
			builder.append(arr[index]);
			builder.append(" ");
		}
	
		String message = builder.toString();
		return message;
	}

	protected void acceptAck(DatagramSocket curSocket) throws IOException {
		int ackLength = ACK_BUFFER.length;
		byte[] buffer = new byte[ackLength];
		DatagramPacket packet = new DatagramPacket(buffer, ackLength);
		curSocket.receive(packet);
		if (!Arrays.equals(buffer, ACK_BUFFER)) {
			throw new SocketTimeoutException();
		}
	}

	protected void acceptAck() throws IOException {
		acceptAck(socket);
	}
	
	protected byte[] getIntByte(int num) {
		return ByteBuffer.allocate(INT_LEN).putInt(num).array();
	}

	protected void send(DatagramSocket curSocket, String message, InetSocketAddress address) throws IOException {
		byte[] arr = ByteBuffer.allocate(LENGTH).put(message.getBytes(Charset.forName("UTF-8"))).array();
		
		curSocket.send(new DatagramPacket(arr, LENGTH, address));
		acceptAck(curSocket);
	}
}
