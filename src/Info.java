import java.io.Serializable;

/**
 * Nice object for storing info about clients.
 * @author moses
 *
 */
public class Info implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	boolean online;
	String ip;
	int port;
	
	public Info(String ip, int port, boolean online) {
		super();
		this.online = online;
		this.ip = ip;
		this.port = port;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
