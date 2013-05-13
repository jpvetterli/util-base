package ch.agent.util.res;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A lazy message is a text with parameters which delays work. Parameters
 * are resolved and the text formatted only when actually needed. Subclasses
 * manage a resource bundle, which can be kept hidden from client code.
 * 
 * 
 * <p>
 * The following example shows a way to use <code>LazyMessage</code>.
 * <p>
 * This is a Java class FooMsg:
 * 
 * <pre>
 * <code>
 * package org.example.bar;
 * 
 * import ch.agent.util.err.LazyMessage;
 * 
 * public class FooMsg extends LazyMessage {
 * 	
 * 	// Message symbols
 * 	public class M {
 * 		public static final String M00101 = "M00101";
 * 		public static final String M00102 = "M00102";
 * 	}
 * 	
 * 	private static final String BUNDLE_NAME = org.example.bar.FooMsg.class.getName();
 * 	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
 * 
 * 	public FooMsg(String key, Object... args) {
 * 		super(key, BUNDLE_NAME, BUNDLE, args);
 * 	}
 * 	
 * 	// short-hand for new FooMsg(...).toString()
 * 	public static String msg(String key, Object... args) {
 * 		return new FooMsg(key, args).toString();
 * 	}
 * }
 * </code>
 * </pre>
 * 
 * This is properties file FooMsg.properties:
 * 
 * <pre>
 * <code> M00101=This is a message.
 * M00102=This is a message with two parameters: {0} and {1}.
 * </code>
 * </pre>
 * 
 * This is code using messages:
 * 
 * <pre>
 * <code> ...
 * import org.example.bar.FooMsg;
 * import org.example.bar.FooMsg.M;
 * ...
 * throw new IllegalArgumentException(FooMsg.msg(M.M00102, "xyzzy", 42));
 * ...
 * </code>
 * </pre>
 * 
 * To Eclipse users:
 * <p>
 * Assuming the resource file is on the source class path, leaving the mouse
 * pointer on a message symbol (like M.M00102 in the last code snippet above)
 * will display the message text in a tooltip.
 * <p>
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
	public LazyMessage(String key, String bundleName, ResourceBundle bundle,
			Object... args) {
		this.bundle = bundle;
		this.bundleName = bundleName;
		this.key = key;
		this.args = args;
	}

	private String findMessage(ResourceBundle bundle, String key, Object[] args) {
		return formatMessage(bundle.getString(String.valueOf(key)), args);
	}

	private String formatMessage(String rawMessage, Object... args) {
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
				message = String.format(FORMAT, key,
						findMessage(bundle, key, args));
			} catch (Exception e) {
				throw new RuntimeException(String.format("key=%s bundle=%s",
						key, bundleName), e);
			}
		}
		return message;
	}

}
