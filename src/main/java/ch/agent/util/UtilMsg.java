package ch.agent.util;

import java.util.ResourceBundle;

import ch.agent.util.err.LazyMessage;

/**
 * UtilMsg provides messages for the package's exceptions.
 */
public class UtilMsg extends LazyMessage {

	/**
	 * Message symbols.
	 */
	public class U {
		public static final String U00101 = "U00101";
		public static final String U00102 = "U00102";
		public static final String U00103 = "U00103";
		public static final String U00104 = "U00104";
		public static final String U00105 = "U00105";
		public static final String U00106 = "U00106";
		public static final String U00107 = "U00107";
		public static final String U00111 = "U00111";
		public static final String U00112 = "U00112";
		public static final String U00201 = "U00201";
		public static final String U00202 = "U00202";
		public static final String U00205 = "U00205";
		public static final String U00206 = "U00206";
		public static final String U00207 = "U00207";
		public static final String U00208 = "U00208";
		public static final String U00209 = "U00209";
	}
	
	private static final String BUNDLE_NAME = ch.agent.util.UtilMsg.class.getName();
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public UtilMsg(String key, Object... args) {
		super(key, BUNDLE_NAME, BUNDLE, args);
	}

}
