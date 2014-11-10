package ch.agent.util;

import java.util.ResourceBundle;

import ch.agent.util.res.LazyMessage;

/**
 * Error messages for base utilities.
 */
public class UtilMsg extends LazyMessage {

	/**
	 * Message symbols. The symbols correspond to keys in the
	 * {@link ResourceBundle} with base name <code>ch.agent.util.UtilMsg</code>.
	 */
	public class U {
		public static final String U00101 = "U00101";
		public static final String U00102 = "U00102";
		public static final String U00103 = "U00103";
		public static final String U00104 = "U00104";
		public static final String U00105 = "U00105";
		public static final String U00106 = "U00106";
		public static final String U00107 = "U00107";
		public static final String U00108 = "U00108";
		public static final String U00109 = "U00109";
		public static final String U00110 = "U00110";
		public static final String U00111 = "U00111";
		public static final String U00112 = "U00112";
		public static final String U00113 = "U00113";
		public static final String U00114 = "U00114";
		public static final String U00130 = "U00130";
		public static final String U00156 = "U00156";
		public static final String U00157 = "U00157";
		public static final String U00158 = "U00158";
		public static final String U00159 = "U00159";
		public static final String U00163 = "U00163";
		public static final String U00164 = "U00164";
		public static final String U00201 = "U00201";
		public static final String U00202 = "U00202";
		public static final String U00205 = "U00205";
		public static final String U00207 = "U00207";
		public static final String U00208 = "U00208";
		public static final String U00209 = "U00209";
		public static final String U00301 = "U00301";
	}

	private static final String BUNDLE_NAME = ch.agent.util.UtilMsg.class.getName();
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	/**
	 * Construct a lazy message.
	 * 
	 * @param key message key
	 * @param args message arguments
	 */
	public UtilMsg(String key, Object... args) {
		super(key, BUNDLE_NAME, BUNDLE, "", args);
	}
	
	/**
	 * Short hand for 
	 * <pre><code>new UtilMsg(key, args).toString()</code></pre>
	 * 
	 * @param key message key
	 * @param args message arguments
	 * @return the message resolved to a string
	 */
	public static String msg(String key, Object... args) {
		return new UtilMsg(key, args).toString();
	}

}
