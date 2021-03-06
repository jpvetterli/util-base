package ch.agent.util.base;

import java.util.ResourceBundle;

import ch.agent.util.base.LazyString;

public class TestMessage extends LazyString {

	/**
	 * Message symbols.
	 */
	public class M {
		public static final String M1 = "M1";
		public static final String M2 = "M2";
		public static final String M3 = "M3";
		public static final String M4 = "M4";
	}
	
	private static final String BUNDLE_NAME = ch.agent.util.base.TestMessage.class.getName();
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	public TestMessage(String key, String pattern, Object... args) {
		super(key, BUNDLE_NAME, BUNDLE, pattern, args);
	}
	
	// short-hand for new TestMessage(...).toString()
	public static String msg(String key, Object... args) {
		return new TestMessage(key, "", args).toString();
	}

}
