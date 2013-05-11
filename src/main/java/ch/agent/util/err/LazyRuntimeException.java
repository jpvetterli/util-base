package ch.agent.util.err;

/**
 * A lazy runtime exception is just a runtime exception but with a lazy message.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class LazyRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -3732535135291247354L;
	
	private LazyMessage msg;

	public LazyRuntimeException(LazyMessage msg, Throwable cause) {
		super(null, cause);
		this.msg = msg;
	}

	@Override
	public String getMessage() {
		return msg.toString();
	}
	
}
