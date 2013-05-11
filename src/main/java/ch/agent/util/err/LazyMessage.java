package ch.agent.util.err;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A lazy message is a text with parameters. Parameters are resolved and the
 * text formatted only when actually needed. Subclasses manage a resource
 * bundle, which can be kept hidden from client code.
 * <p>
 * Methods are available for creating exceptions taking
 * lazy messages. Applications requiring their own exception types should
 * consider subclassing {@link LazyException} or {@link LazyRuntimeException}
 * and override the {@link #exception()} or {@link #runtimeException()}
 * methods.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public abstract class LazyMessage {
	
	private static final String FORMAT = "%s - %s";

	private ResourceBundle bundle;
	private String bundleName;
	private String key;
	private Object[] args;
	private String message;

	/**
	 * Construct a lazy message. The actual message text is only created if and
	 * when needed.
	 * 
	 * @param key
	 *            a String identifying the text
	 * @param bundleName
	 *            the name of the bundle, used in meta exception message
	 * 
	 * @param bundle
	 *            a {@link ResourceBundle} containing the wanted text
	 * @param args
	 *            zero of more arguments
	 */
	public LazyMessage(String key, String bundleName, ResourceBundle bundle, Object... args) {
		this.bundle = bundle;
		this.bundleName = bundleName;
		this.key = key;
		this.args = args;
	}
	
	private String findMessage(ResourceBundle bundle, String key, Object[] args) {
		return formatMessage(bundle.getString(String.valueOf(key)), args);
	}

	private String formatMessage(String rawMessage, Object ... args) {
		if (args.length == 0)
			return rawMessage;
		// MessageFormat.format() does ugly Double.NaNs so use toString()
		String[] s = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				s[i] = "null";
			else
				s[i] = args[i].toString();
		}
		return new MessageFormat(rawMessage).format(s);
	}

	/**
	 * Return an exception which with this message.
	 * 
	 * @return an exception
	 */
	public Exception exception() {
		return new LazyException(this, null);
	}
	
	/**
	 * Return an exception which with this message.
	 * 
	 * @param cause the causing exception
	 * @return an exception
	 */
	public Exception exception(Throwable cause) {
		return new LazyException(this, cause);
	}
	
	/**
	 * Return a runtime exception which with this message.
	 * 
	 * @return a runtime exception
	 */
	public RuntimeException runtimeException() {
		return new LazyRuntimeException(this, null);
	}
	
	/**
	 * Return a runtime exception which with this message.
	 * 
	 * @param cause the causing exception
	 * @return a runtime exception
	 */
	public RuntimeException runtimeException(Throwable cause) {
		return new LazyRuntimeException(this, cause);
	}

	/**
	 * Resolve and format the message into a string. The method retrieves the
	 * text from the resource bundle using the key, formats and replaces
	 * arguments, and prefixes the result with the message key. Null arguments
	 * are supported. If anything gets in the way, making it impossible to
	 * prepare the message, a runtime exception is thrown with the relevant
	 * cause and with a message displaying the key and the base name of the
	 * resource bundle.
	 * <p>
	 * 
	 * @return the formatted message text
	 * @throws RuntimeException
	 */
	@Override
	public String toString() {
		if (message == null) {
			try {
				message = String.format(FORMAT, key, findMessage(bundle, key, args));
			} catch (Exception e) {
				throw new RuntimeException(
						String.format("key=%s bundle=%s", key, bundleName), e);
			}
		}
		return message;
	}
	
}
