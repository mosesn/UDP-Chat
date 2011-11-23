public class Message {
	private final String message;
	private final String senderNick;

	private final long time;

	/**
	 * Nice object for holding messages, doesn't need to stored recipient nick, that's remembered elsewhere
	 * @param message
	 * @param sender
	 * @param l
	 */
	public Message(String message, String sender, long l) {
		super();
		this.message = message;
		this.senderNick = sender;
		this.time = l;
	}
	
	public long getTime() {
		return time;
	}
	
	public String getMessage() {
		return message;
	}

	public String getSender() {
		return senderNick;
	}
}
