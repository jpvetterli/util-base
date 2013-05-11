package ch.agent.util.err;

import java.util.ResourceBundle;

public class TestMessage extends LazyMessage {

	/**
	 * Message symbols.
	 */
	public class M {
		public static final String M1 = "M1";
		public static final String M2 = "M2";
		public static final String M3 = "M3";
	}
	
	private static final String BUNDLE_NAME = ch.agent.util.err.TestMessage.class.getName();
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	public TestMessage(String key, Object... args) {
		super(key, BUNDLE_NAME, BUNDLE, args);
	}

}
