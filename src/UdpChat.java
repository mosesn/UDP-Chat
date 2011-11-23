import java.io.IOException;
import java.net.SocketException;
/**
 * Just drives.
 * @author moses
 *
 */

public class UdpChat {

	/**
	 * Driver method
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Too short.");
			return;
		}
		
		String mode = args[0];
		if (mode.equals("-c")) {
			if (args.length < 5) {
				System.out.println("Too short.");
				return;
			}
			String nick = args[1];
			String serverIp = args[2];
			
			String strServerPort = args[3];
			String strClientPort = args[4];

			int serverPort = 0;
			int clientPort = 0;
			
			try {
				serverPort = Integer.parseInt(strServerPort);
				clientPort = Integer.parseInt(strClientPort);
			}
			catch (Exception e){
				System.out.println("Port numbers should be integers!");
			}
			
			Client client;
			try {
				client = new Client(nick, serverIp, serverPort, clientPort);
				client.register();
				client.shell();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		else if (mode.equals("-s")) {
			if (args.length < 2) {
				System.out.println("Too short.");
				return;
			}
			
			String strPort = args[1];
			
			int port = 0;
			
			try {
				port = Integer.parseInt(strPort);
			}
			catch (Exception e) {
				System.out.println("Port numbers should be integers!");
			}
			

			try {
				Server server = new Server(port);
				server.listen();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("What are you doing.");
		}
	}

}
