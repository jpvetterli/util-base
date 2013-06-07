package ch.agent.util.res;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A lazy message is a text with optional parameters. Parameters follow
 * {@link MessageFormat} conventions. They are resolved and the text is
 * formatted only when actually needed. Subclasses manage a resource bundle,
 * which can be kept hidden from client code.
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
public class LazyMessage {

	private static String DEFAULT_PATTERN = "%s - %s"; 
	
	private ResourceBundle bundle;
	private String bundleName;
	private String key;
	private Object[] args;
	private String pattern;
	private String text;

	/**
	 * Construct a lazy message. The actual message text is only created if and
	 * when needed. Depending on the pattern specified, the message key can be
	 * included in the message text. By default, key and message body are joined
	 * with a hyphen. Here is an example with key <q>M042</q>:
	 * 
	 * <pre>
	 * <code>
	 * M042 - This is message forty-two.
	 * </code>
	 * </pre>
	 * <p>
	 * The behavior is specified using the <code>pattern</code> parameter.
	 * The pattern is simply a format specification as in {@link String#format}.
	 * When null, the key is not inserted. When the pattern is empty,
	 * <q>%s&nbsp;-&nbsp;%s</q> is used as the built-in default.
	 * <p>
	 * When the <code>bundle</code> parameter is null, the <code>key</code>
	 * parameter is interpreted as the message text.
	 *  
	 * @param key
	 *            a String identifying the message
	 * @param bundleName
	 *            the name of the bundle (used in meta exception messages)
	 * @param bundle
	 *            a {@link ResourceBundle} containing the wanted text
	 * @param pattern
	 *            if true the message will be prefixed with the key
	 * @param args
	 *            zero of more arguments
	 */
	public LazyMessage(String key, String bundleName, ResourceBundle bundle,
			String pattern, Object... args) {
		this.bundle = bundle;
		this.bundleName = bundleName;
		this.key = key;
		this.args = args;
		this.pattern = (pattern != null && pattern.length() == 0) 
				? DEFAULT_PATTERN : pattern;
	}
	
	/**
	 * Construct a lazy message. The actual message text is only created if and
	 * when needed.
	 *  
	 * @param message
	 *            the message text
	 * @param args
	 *            zero of more arguments
	 */
	public LazyMessage(String message, Object... args) {
		this(message, null, null, null, args);
	}

	private String format(String rawMessage, Object... args) {
		if (args.length == 0)
			return rawMessage;
		// MessageFormat.format() does not handle Double.NaNs
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Double)
				args[i] = args[i].toString();
		}
		return new MessageFormat(rawMessage).format(args);
	}

	private String getText() {
		return bundle == null ? key : bundle.getString(key);
	}
	
	/**
	 * Resolve and format the message into a string. The method retrieves the
	 * text from the resource bundle using the key, formats and replaces
	 * arguments, and inserts the key into the message if requested. Null arguments
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
		if (text == null) {
			try {
				text = format(getText(), args);
				if (pattern != null)
					text = String.format(pattern, key, text);
			} catch (Exception e) {
				throw new RuntimeException(
						String.format("key=%s bundle=%s", key, bundleName), e);
			}
		}
		return text;
	}

}
