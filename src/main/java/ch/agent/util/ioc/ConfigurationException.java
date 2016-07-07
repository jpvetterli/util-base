package ch.agent.util.ioc;

/**
 * Unchecked exception thrown when configuration fails for any reason.
 *
 */
@SuppressWarnings("serial")
public class ConfigurationException extends RuntimeException {

	/**
	 * Constructor.
	 * 
	 * @param message a string 
	 */
	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param message a string
	 * @param cause an exception or null
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
