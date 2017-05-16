package ch.agent.util;

import java.util.ResourceBundle;

import ch.agent.util.base.LazyString;

/**
 * Error messages for base utilities.
 */
public class STRINGS extends LazyString {

	/**
	 * Message symbols. The symbols correspond to keys in the
	 * {@link ResourceBundle} with base name <code>ch.agent.util.UtilMsg</code>.
	 */
	public class U {
		public static final String U00103 = "U00103";
		public static final String U00104 = "U00104";
		public static final String U00105 = "U00105";
		public static final String U00108 = "U00108";
		public static final String U00109 = "U00109";
		public static final String U00110 = "U00110";
		public static final String U00111 = "U00111";
		public static final String U00112 = "U00112";
		public static final String U00113 = "U00113";
		public static final String U00114 = "U00114";
		public static final String U00115 = "U00115";
		public static final String U00117 = "U00117";
		public static final String U00118 = "U00118";
		public static final String U00119 = "U00119";
		public static final String U00120 = "U00120";
		public static final String U00121 = "U00121";
		public static final String U00122 = "U00122";
		public static final String U00130 = "U00130";
		public static final String U00132 = "U00132";
		public static final String U00133 = "U00133";
		public static final String U00134 = "U00134";
		public static final String U00153 = "U00153";
		public static final String U00154 = "U00154";
		public static final String U00155 = "U00155";
		public static final String U00156 = "U00156";
		public static final String U00158 = "U00158";
		public static final String U00159 = "U00159";
		public static final String U00160 = "U00160";
		public static final String U00161 = "U00161";
		public static final String U00162 = "U00162";
		public static final String U00163 = "U00163";
		public static final String U00164 = "U00164";
		public static final String U00165 = "U00165";
		public static final String U00201 = "U00201";
		public static final String U00202 = "U00202";
		public static final String U00205 = "U00205";
		public static final String U00207 = "U00207";
		public static final String U00208 = "U00208";
		public static final String U00209 = "U00209";
		
		public static final String U00300 = "U00300";
		
		public static final String C03 = "C03";
		public static final String C06 = "C06";
		public static final String C09 = "C09";
		public static final String C11 = "C11";
		public static final String C12 = "C12";
		public static final String C13 = "C13";
		public static final String C14 = "C14";
		public static final String C15 = "C15";
		public static final String C16 = "C16";
		public static final String C17 = "C17";
		public static final String C19 = "C19";
		public static final String C20 = "C20";
		public static final String C21 = "C21";
		public static final String C22 = "C22";
		public static final String C23 = "C23";
		public static final String C24 = "C24";
		public static final String C25 = "C25";
		public static final String C30 = "C30";
		public static final String C31 = "C31";
		public static final String C32 = "C32";
		public static final String C50 = "C50";
		public static final String C51 = "C51";
		public static final String C52 = "C52";
		public static final String C53 = "C53";
		public static final String C54 = "C54";
		public static final String C55 = "C55";
		public static final String C56 = "C56";
		public static final String C61 = "C61";
		public static final String C62 = "C62";
		public static final String C63 = "C63";

	}

	private static final String BUNDLE_NAME = STRINGS.class.getName();
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	/**
	 * Construct a lazy message.
	 * 
	 * @param key message key
	 * @param args message arguments
	 */
	public STRINGS(String key, Object... args) {
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
		return new STRINGS(key, args).toString();
	}

	public static LazyString lazymsg(String key, Object... args) {
		return new STRINGS(key, args);
	}

}
