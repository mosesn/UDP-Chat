import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;


/**
 * Handles all of the server's logic.
 * @author moses
 *
 */
public class Server extends NetworkNode{
	private final Map<String, Info> nickTable;
	private final Multimap<String, Message> nickMessageLog;
	
	public Server(int port) throws SocketException{
		super(new DatagramSocket(port));
		nickTable = Maps.newHashMap();
		this.nickMessageLog = ArrayListMultimap.create();
	}

	private void broadcast(byte[] message) throws IOException{
		byte[] sendMessage = ByteBuffer.allocate(LENGTH).put(message).array();
		for (Entry<String, Info> entry: nickTable.entrySet()) {
			if (entry.getValue().online) {
				String ip = entry.getValue().getIp();
				int port = entry.getValue().getPort();
				socket.send(new DatagramPacket(sendMessage, LENGTH, new InetSocketAddress(ip, port)));
				acceptAck();
			}
		}
		System.out.println("BROADCAST");
	}

	/**
	 * Lets the server start to listen for messages.
	 * @throws IOException
	 */
	public void listen() throws IOException {
		System.out.println("Listening.");
		while (true) {
			boolean isGood = receive();
			if (!isGood) {
				return;
			}
		}
	}

	private boolean receive() {
		DatagramPacket packet = new DatagramPacket(new byte[LENGTH], LENGTH);
		try {
			socket.receive(packet);
			parse(packet);
		}
		catch (IOException e) {
			return false;
		}
		
		return true;
	}

	private void parse(DatagramPacket packet) throws IOException {
		String str = new String(packet.getData(), Charset.forName("UTF-8"));
		String[] arr = str.trim().split(" ");
		if (arr.length < 1) {
			throw new IllegalArgumentException("Wrong number of arguments");
		}
		
		String type = arr[0];
		
		String address = packet.getAddress().getHostAddress();
		if (type.equals(REG_STR)) {

			if (arr.length != 3) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}

			String nick = arr[1];
			int port = Integer.parseInt(arr[2]);

			if (nickTable.containsKey(nick) && nickTable.get(nick).isOnline()) {
				return;
			}
			ack(packet);
			sendTable(nick, packet);
			nickTable.put(nick, new Info(address, port, true));
			sendBacklog(nick);
			broadcastRegister(nick, address, port);
		}
		else if (type.equals(DEREG_STR)) {
			ack(packet);
			if (arr.length != 2) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}
			String nick = arr[1];
			
			nickTable.get(nick).setOnline(false);
			broadcastDeregister(nick);
		}
		else if (type.equals(MESSAGE)){
			ack(packet);
			String sender = arr[1];
			String receiver = arr[2];
			if (arr.length < 3) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}
			String message = getMessage(arr, 3);
			nickMessageLog.put(receiver, new Message(message, sender, new Date().getTime()));
		}
		else {
			ack(packet);
			throw new IllegalStateException("not a valid state.");
		}
	}

	private void sendTable(String nick, DatagramPacket packet) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(byteStream);
		try {
			stream.writeObject(nickTable);
		}
		catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		stream.close();
		
		byte[] arr = byteStream.toByteArray();
		byte[] sendArr = ByteBuffer.allocate(LENGTH).put(arr).array();
		byteStream.close();
		socket.send(new DatagramPacket(sendArr,LENGTH, packet.getAddress(), packet.getPort()));
		acceptAck(socket);
	}

	private void broadcastDeregister(String nick) throws IOException {
		String message = new StringBuilder()
		.append(DEREG_STR)
		.append(" ")
		.append(nick).toString();
		broadcast(message.getBytes(Charset.forName("UTF-8")));
	}

	private void broadcastRegister(String nick, String address, int port) throws IOException {
		String message = new StringBuilder()
		.append(REG_STR)
		.append(" ")
		.append(nick)
		.append(" ")
		.append(address)
		.append(" ")
		.append(port).toString();
		broadcast(message.getBytes(Charset.forName("UTF-8")));
	}

	/**
	 * Sends the backlog of messages.
	 * @param nick
	 * @throws IOException
	 */
	private void sendBacklog(String nick) throws IOException {
		System.out.println("Sending . . .");
		InetSocketAddress netAddr = new InetSocketAddress(nickTable.get(nick).getIp(), nickTable.get(nick).getPort());
		
		if (nickMessageLog.containsKey(nick)) {
			for (Message msg : nickMessageLog.get(nick)) {
					send(socket, MESSAGE + " " + msg.getSender() + " " + nick + " "  + msg.getTime() + " " + msg.getMessage(), netAddr);
			}
		}
		send(socket, MESSAGE, netAddr);
		nickMessageLog.removeAll(nick);
	}

}
