import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Scanner;

import com.google.common.collect.Maps;

/**
 * Handles all of the client's logic and behavior.
 * @author moses
 */

public class Client extends NetworkNode {

	private String nick;
	private String serverIp;
	private int serverPort;
	private int clientPort;
	private final Scanner scanner;
	private Map<String, Info> nickTable;
	private DatagramSocket sendSocket;

	/**
	 * Constructor for the client
	 * @param nick
	 * @param serverIp
	 * @param serverPort
	 * @param clientPort
	 * @throws SocketException
	 */
	public Client(String nick, String serverIp, int serverPort, int clientPort)
			throws SocketException {
		super(new DatagramSocket(clientPort));
		this.sendSocket = new DatagramSocket();
		this.sendSocket.setSoTimeout(500);
		this.nick = nick;
		this.serverIp = serverIp;
		this.serverPort = serverPort;
		this.clientPort = clientPort;
		this.scanner = new Scanner(System.in);
		this.nickTable = Maps.newHashMap();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + clientPort;
		result = prime * result + ((nick == null) ? 0 : nick.hashCode());
		result = prime * result
				+ ((serverIp == null) ? 0 : serverIp.hashCode());
		result = prime * result + serverPort;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Client other = (Client) obj;
		if (clientPort != other.clientPort)
			return false;
		if (nick == null) {
			if (other.nick != null)
				return false;
		} else if (!nick.equals(other.nick))
			return false;
		if (serverIp == null) {
			if (other.serverIp != null)
				return false;
		} else if (!serverIp.equals(other.serverIp))
			return false;
		if (serverPort != other.serverPort)
			return false;
		return true;
	}

	/**
	 * Starts the shell.
	 * @throws IOException
	 */
	public void shell() throws IOException {
		boolean finished = false;

		while (!finished) {
			System.out.print(">>>");
			String command = scanner.nextLine();

			try {
				if (!run(command)) {
					finished = true;
				}
			}
			catch (IllegalArgumentException e) {
				System.out.println("Not a valid command.");
			}
		}
	}

	/**
	 * Makes the listener thread.
	 */
	private void spinUpListener() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listen();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}
	

	/**
	 * Receives messages and handles output
	 * @throws IOException on failure
	 */
	protected boolean receive() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[LENGTH], LENGTH);
		try {
			socket.receive(packet);
			parse(packet);
			ack(packet);
			System.out.print(">>>");
		}
		catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Figures out what kind of message is being sent.  Protocol:
	 * prefix: type, the rest of the message is arguments (separated by " ")
	 * @param packet
	 */
	private void parse(DatagramPacket packet) {
		String str = new String(packet.getData(), Charset.forName("UTF-8"));
		String[] arr = str.trim().split(" ");
		if (arr.length < 1) {
			throw new IllegalArgumentException("Wrong number of arguments");
		}
		
		String type = arr[0];
		
		if (type.equals(REG_STR)) {
			if (arr.length != 4) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}

			String nick = arr[1];
			String addr = arr[2];
			int port = Integer.parseInt(arr[3]);
			updateNickTable(nick, new Info(addr, port, true));
			System.out.println(" [Client table updated.]");
		}
		else if (type.equals(DEREG_STR)) {
			if (arr.length != 2) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}
			String nick = arr[1];
			
			deregNick(nick);
			System.out.println(" [Client table updated.]");
		}
		else if (type.equals(MESSAGE)){
			if (arr.length > 1) {
				String sender = arr[1];
				long time = Long.parseLong(arr[3]);
				
				if (arr.length < 4) {
					throw new IllegalArgumentException("Wrong number of arguments");
				}
				
				String message = getMessage(arr, 4);
				if (time != 0) {
					System.out.println("You have messages!\n>>>");
					System.out.println(sender+ ": " + time + " " +message);
				}
				else {
					System.out.println(sender+ ": " +message);
				}
			}
			else {
				System.out.println();
			}
		}
		else {
			System.out.println(type);
			throw new IllegalStateException("not a valid state.");
		}
	}

	protected void listen() throws IOException {
		while (true) {
			if (!receive()){
				break;
			}
		}
	}

	private boolean run(String command) throws SocketException, IOException {
		String[] arr = command.trim().split(" ");
		if (arr.length < 1) {
			throw new IllegalArgumentException("Command must be nontrivial.");
		}
		
		if (arr[0].equals("send")) {
			if (arr.length <= 2) {
				throw new IllegalArgumentException("Not enough arguments");
			}

			String otherNick = arr[1];
			String message = getMessage(arr, 2);

			try {
				send(MESSAGE + " " + nick + " " + otherNick + " 0 " + message, otherNick);
				System.out.println(">>> [Message received by " + otherNick  + ".]");
			} catch (IOException e) {
				try {
					System.out.println(">>> [No ACK from " + otherNick + ", message sent to server.");
					sendToServer(message, otherNick);
					System.out.println(">>> [Messages received by the server and saved.");
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		} else if (arr[0].equals("dereg")) {
			if (arr.length != 2) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}

			String otherNick = arr[1];
			
			boolean acked = false;
			for (int x = 0; x < 5; x++) {
				try {
					deregister(otherNick);
					acked = true;
					System.out.println("[You are Offline.  Bye.]");
					break;
				}
				catch (SocketTimeoutException e) {
					//keep going
				}
			}
			
			if (!acked) {
				System.out.println(">>> [Server not responding.]");
				System.out.println(">>> [Exiting.]");
				return false;
			}
		}
		else if (arr[0].equals("reg")) {
			if (arr.length != 2) {
				throw new IllegalArgumentException("Wrong number of arguments");
			}
			
			nick = arr[1];
			register();
		}
		else {
			throw new IllegalArgumentException("Not a valid command.");
		}

		return true;
	}

	private void sendToServer(String message, String otherNick)
			throws SocketException, IOException {
		InetSocketAddress address = getServerAddress();
		String modifiedMessage = MESSAGE + " " + nick + " " + otherNick + " " + message;
		for (int attempt = 0; attempt < 5; attempt++) {
			try {
				send(sendSocket, modifiedMessage, address);
				return;
			} catch (SocketTimeoutException e) {
				//do nothing
			}
		}
		System.out.println(">>> [Server not responding.]");
		
	}

	private void deregister(String otherNick) throws SocketException,
			IOException {
		String message = new StringBuilder()
		.append(DEREG_STR)
		.append(" ")
		.append(otherNick).toString();

		byte[] arr = (message.getBytes(Charset.forName("UTF-8")));

		byte[] sendArr = ByteBuffer.allocate(LENGTH).put(arr).array();
		
		sendSocket.send(new DatagramPacket(sendArr, LENGTH, getServerAddress()));
		acceptAck(sendSocket);
	}


	/**
	 * Register registers the client with the server.
	 * Suppresses unchecked warning for casting the object to an int.  This is the only type of object it can receive.
	 * @throws SocketException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void register() throws SocketException, IOException {
		spinUpListener();

		String str = new StringBuilder()
			.append(REG_STR)
			.append(" ")
			.append(nick)
			.append(" ")
			.append(clientPort).toString();

		byte[] arr = ByteBuffer.allocate(LENGTH).put(str.getBytes(Charset.forName("UTF-8"))).array();
		
		sendSocket.send(new DatagramPacket(arr, LENGTH, getServerAddress()));
		acceptAck(sendSocket);
		
		DatagramPacket packet = new DatagramPacket(arr, LENGTH);
		sendSocket.receive(packet);
		ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
		ObjectInputStream stream = new ObjectInputStream(byteStream);
		try {
			nickTable = (Map<String, Info>) stream.readObject();
			ack(packet);
			System.out.println(">>>[Welcome, You are registered.]");
		}
		catch (Exception e) {
			//Serializability problems lie here
			e.printStackTrace();
		}
	}

	private Info getNickInfo(String otherNick) {
		return nickTable.get(otherNick);
	}

	private InetSocketAddress getServerAddress() {
		return new InetSocketAddress(serverIp, serverPort);
	}

	private void updateNickTable(String nick, Info info) {
		nickTable.put(nick, info);
	}
	
	private void deregNick(String nick) {
		if (nickTable.containsKey(nick)) {
			nickTable.get(nick).setOnline(false);
		}
	}

	protected void send(String message, String otherNick) throws IOException {
		Info info = getNickInfo(otherNick);
	
		if (info == null) {
			throw new IllegalArgumentException("Not a valid nickname");
		}
		
		if (info.isOnline()) {
			InetSocketAddress address = new InetSocketAddress(info.getIp(),
					info.getPort());
			send(sendSocket, message, address);
		} else {
			throw new IOException("Not online");
		}
	}
}
