package ch.agent.util.err;

/**
 * A lazy exception is just an exception but with a lazy message.
 * 
 * @author Jean-Paul Vetterli
 *
 */
public class LazyException extends Exception {
	
	private static final long serialVersionUID = -3613077402129657017L;

	private LazyMessage msg;

	public LazyException(LazyMessage msg, Throwable cause) {
		super(null, cause);
		this.msg = msg;
	}

	@Override
	public String getMessage() {
		return msg.toString();
	}

}
