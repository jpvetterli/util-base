package ch.agent.util.args;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.ArgsScanner.SymbolScanner;
import ch.agent.util.base.Misc;
import ch.agent.util.file.TextFile;
import ch.agent.util.logging.LoggerBridge;

/**
 * Args is a parser for command line arguments. It is named after the parameter
 * of the main method of Java programs, usually written like this:
 * 
 * <pre>
 * <code>
 * public static void main(String[] <b>args</b>) {
 *    // etc.
 * }
 * </code></pre>
 * 
 * Args consists of a simple language and a small set of built-in
 * operators. Application are of course free to use Args beyond parsing command
 * line arguments. Using Args consists of three steps: (1) defining parameters,
 * (2) parsing the input, and (3) extracting values of parameters.
 * 
 * <h3>Defining parameters</h3>
 * 
 * <h3>Parsing the input</h3>
 * 
 * The input is a single string. For convenience Args provides a method to parse
 * an array of strings, like the command line arguments of the main method, but
 * all elements are joined into a single string, with blanks inserted between.
 * <p>
 * The input is a succession of name-value pairs and standalone keywords. 
 * Name-value pairs have an equal sign character between them, 
 * possibly surrounded by one or more white space characters 
 * (characters for which {@link Character#isWhitespace} is true). 
 * 
 * <p>
 * EXPLAIN THAT escape have only effect in brackets and in front of $$
 * </p>
 * 
 * <h3>Extracting values of parameters</h3>
 * 
 * <h3>Built-in operators</h3>
 * 
 * Variables. "file", "if", "file with mapping".
 * 
 * 
 * <pre>
 * ==================== ***** WORK IN PROGRESS ABOVE -- OLD DOC BELOW ******** ===================
 * </pre>
 * 
 * Support for parameter lists and parameter files. A parameter list is a
 * sequence of name-value pairs separated by white space, with names and values
 * separated by an equal sign (which can be surrounded by white space). It is
 * also possible to configure <code>Args</code> to have nameless values
 * (sometimes known as positional parameters). If a name or a value includes
 * white space or an equal sign it must be enclosed in square brackets. To
 * include a closing bracket it must be escaped with a backslash. Here is an
 * example:
 * 
 * <pre>
 * <code>
 * foo = bar [qu ux]=[[what = ever\]] foo = [2nd val]
 * </code>
 * </pre>
 * 
 * In the example, parameter "foo" has two values: "bar" and "2nd val" while
 * parameter "qu ux" has one value, "[what = ever]".
 * <p>
 * When a name is repeated the previous value is lost unless the parameter was
 * defined as a list parameter, like "foo" in the example.
 * <p>
 * Name-value parameters can be specified in files, but nameless values are not
 * allowed. Files are themselves specified using parameters, using the notation
 * <code>file=file-spec</code>. Files can reside in the file system or on the
 * class path. There can be multiple files and files can be nested. In parameter
 * files, lines starting with a hash sign are skipped, even inside brackets.
 * Since line terminators are handled as white space, values can be continued on
 * multiple lines by having opening and closing square brackets on multiple
 * lines (line terminators are replaced with spaces). File parameters are
 * processed immediately in the order in which they appear. The file name can be
 * followed with a semi-colon and one or more mappings. Here is an example:
 * 
 * <pre>
 * <code>
 * file = [/home/someone/parms.txt; foo=bar quux=flix]
 * </code>
 * </pre>
 * <p>
 * When mappings are present, only parameters named in the mappings ("foo" and
 * "quux" in the example) will be considered. If they are found in the file the
 * corresponding values will be assigned to parameters using the mapping values
 * ("bar" and "flix" in the example). This trick is useful when extracting
 * specific parameters from existing configuration files where names are defined
 * by someone else.
 * <p>
 * Except in <em>loose mode</em> parameter names must have been defined before
 * parsing. Loose mode is activated by {@link #setLoose} and deactivated by
 * {@link #setStrict}.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class Args implements Iterable<String> {
	
	
	/**
	 * It is possible to configure the meta characters with a system property
	 * named <b>ArgsMetaCharacters</b>. The property is ignored unless it
	 * contains exactly 4 characters. 
	 */
	public static final String ARGS_META = "ArgsMetaCharacters";
	
	static {
		char[] chars = validateMetaCharacters(System.getProperty(ARGS_META));
		leftQuote = chars[0];
		rightQuote = chars[1];
		nameValueSeparator = chars[2];
		escape = chars[3];
	}
	
	/**
	 * Validate a string of meta characters. There must be four characters and
	 * they must be different. The sequence (defaults in parentheses) is
	 * <ol>
	 * <li>left quote ([),
	 * <li>right quote (]),
	 * <li>name-value separator (=),
	 * <li>escape (\).
	 * </ol>
	 * The characters are returned in an array in that sequence. If the input is
	 * null default meta characters are returned.
	 * 
	 * @param metaChars
	 *            a string of length 4
	 * @return an array of length 4
	 */
	public static char[] validateMetaCharacters(String metaChars) {
		if (metaChars == null)
			return new char[] { '[', ']', '=', '\\' };
		else {
			if (metaChars.length() != 4)
				throw new IllegalArgumentException(msg(U.U00164, metaChars));
			char lq = metaChars.charAt(0);
			char rq = metaChars.charAt(1);
			char nvs = metaChars.charAt(2);
			char esc = metaChars.charAt(3);
			validateMetaCharacters(lq, rq, nvs, esc);
			return new char[] { lq, rq, nvs, esc };
		}
	}
	
	/**
	 * Validate four meta characters. 
	 * The four characters must be different.
	 *
	 * @param leftQuote the left quote 
	 * @param rightQuote the right quote 
	 * @param nameValueSeparator the name-value separator
	 * @param escape the escape
	 */
	public static void validateMetaCharacters(char leftQuote, char rightQuote, char nameValueSeparator, char escape) {
		if (leftQuote == rightQuote || leftQuote == nameValueSeparator || leftQuote == escape 
			|| rightQuote == nameValueSeparator || rightQuote == escape 
			|| nameValueSeparator == escape)
			throw new IllegalArgumentException(msg(U.U00163, leftQuote, rightQuote, nameValueSeparator, escape));
	}

	private final static char leftQuote;
	private final static char rightQuote;
	private final static char nameValueSeparator;
	private final static char escape;
	
	private final static String COND = "condition";
	private final static String COND_IF_NON_EMPTY = "if";
	private final static String COND_THEN = "then";
	private final static String COND_ELSE = "else";
	
	private final static String INCLUDE = "include";
	private final static String INC_NAMES = "names";
	private final static String INC_CLASS = "extractor";
	private final static String INC_CONFIG = "extractor-parameters";
	
	/**
	 * A definition object is used to write code in method chaining style.
	 * For example, to define a parameter with two aliases and a default
	 * value, one would write:
	 * <pre><code>
	 * args.def("foo").aka("f").aka("foooo").init("bar");
	 * </code></pre>
	 * When defining a parameter, it is mandatory to use {@link #def} first.
	 */
	public class Definition {
		private Args args; 
		private String name;
		
		private Definition(Args args, String name) {
			Misc.nullIllegal(args, "args null");
			Misc.nullIllegal(name, "name null");
			this.args = args;
			this.name = name;
		}
		
		private Args args() {
			return args;
		}
		
		private String name() {
			return name;
		}
		
		/**
		 * Set an alias. An exception is thrown if the alias is already 
		 * in use for this parameter or an another one.
		 * 
		 * @param alias an alternate name for the parameter
		 * @return this definition
		 */
		public Definition aka(String alias) {
			Value v = args().internalGet(name());
			if (v == null)
				throw new IllegalArgumentException("bug: " + name());
			if (args().internalGet(alias) != null)
				throw new IllegalArgumentException(msg(U.U00104, alias));
			args().put(alias, v);
			return this;
		}

		/**
		 * Set a default value for the parameter. Only scalar parameters can
		 * have default values. A parameter with a null default value is
		 * mandatory. A parameter with a non-null default value can be omitted.
		 * 
		 * @param value
		 *            the default value of the parameter
		 * @return this definition
		 */
		public Definition init(String value) {
			Value v = args().internalGet(name());
			if (v == null)
				throw new IllegalArgumentException("bug: " + name());
			v.setDefault(value);
			return this;
		}
		
		public Definition repeatable() {
			Value v = args().internalGet(name());
			if (v == null)
				throw new IllegalArgumentException("bug: " + name());
			v.setRepeatable(true);
			return this;
		}
		
	}
	
	public class Value {
		private String canonical;
		private String value;
		private String defaultValue;
		private boolean repeatable;

		/**
		 * Constructor.
		 * 
		 * @param canonical canonical name of parameter
		 */
		public Value(String canonical) {
			this.canonical = canonical;
		}
		
		protected String getName() {
			return canonical;
		}

		public void set(String value) {
			this.value = value;
		}
		
		public void append(String value) {
			this.value = this.value == null ?
					new StringBuilder(value.length() + 2).append(leftQuote).append(value).append(rightQuote).toString() :
					new StringBuilder(this.value.length() + value.length() + 3).append(this.value).append(BLANK).append(leftQuote).append(value).append(rightQuote).toString();
		}
		
		public String getDefault() {
			return defaultValue;
		}

		public void setDefault(String value) {
			this.defaultValue = value;
		}
		
		public boolean isRepeatable() {
			return repeatable;
		}

		public void setRepeatable(boolean b) {
			this.repeatable = b;
		}

		/**
		 * Return the value as a string. Throw an exception if the value is null
		 * and no default value was defined.
		 * <p>
		 * If the value contains a single unresolved variable without any
		 * surrounding text and a default value has been defined, the method
		 * returns the default value. Except in this case an exception is thrown
		 * in the presence of any unresolved variable.
		 * 
		 * @return a string
		 */
		public String stringValue() {
			String result = value;
			if (result == null) {
				if (defaultValue == null)
					throw new IllegalArgumentException(msg(U.U00105, getName()));
				result = defaultValue;
			} else {
				List<String> parts = symScanner.split(result);
				switch (parts.size()) {
				case 0:
				case 1:
					break;
				case 2:
					if (defaultValue == null)
						throw new IllegalArgumentException(msg(U.U00107, getName(), result));
					result = defaultValue;
					break;
				default:
					throw new IllegalArgumentException(msg(U.U00106, getName(), result));
						
				}
			}
			return result;
		}
		
		/**
		 * Split value into a number of strings. Splitting is done on white
		 * space and meta characters. An <code>IllegalArgumentException</code>
		 * is thrown if the number of strings is too small or too large, as
		 * specified by two parameters. Negative parameters are ignored.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @param min
		 *            minimal number of strings (no limit if negative)
		 * @param max
		 *            maximal number of strings (no limit if negative)
		 * @return an array of strings
		 */
		public String[] stringValues(int min, int max) {
			List<String> values = getScanner().asValues(stringValue());
			checkSize(values.size(), min, max);
			return values.toArray(new String[values.size()]);
		}

		/**
		 * Split value into a number of strings. Splitting is done on white
		 * space and meta characters.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @return an array of strings
		 */
		public String[] stringValues() {
			return stringValues(-1, -1);
		}

		/**
		 * Return the value as a string. Throw an exception if the value is null
		 * and no default value was defined. This method does not check for
		 * unresolved variables.
		 * 
		 * @return a string
		 */
		public String rawValue() {
			if (value == null) {
				if (defaultValue == null)
					throw new IllegalArgumentException(msg(U.U00105, getName()));
				return defaultValue;
			}
			return value;
		}
		
		/**
		 * Split value into a number of strings. Splitting is done on white
		 * space and meta characters. An <code>IllegalArgumentException</code>
		 * is thrown if the number of strings is too small or too large, as
		 * specified by two parameters. Negative parameters are ignored. This
		 * method does not check for unresolved variables.
		 * 
		 * @param min
		 *            minimal number of strings (no limit if negative)
		 * @param max
		 *            maximal number of strings (no limit if negative)
		 * @return an array of strings
		 */
		public String[] rawValues(int min, int max) {
			List<String> values = getScanner().asValues(rawValue());
			checkSize(values.size(), min, max);
			return values.toArray(new String[values.size()]);
		}

		/**
		 * Split value into a number of strings. Splitting is done on white
		 * space and meta characters. This method does not check for unresolved
		 * variables.
		 * 
		 * @return an array of strings
		 */
		public String[] rawValues() {
			return rawValues(-1, -1);
		}

		private void checkSize(int size, int min, int max) {
			if (min > -1) {
				if (min == max) {
					if (size != min)
						throw new IllegalArgumentException(msg(U.U00109, getName(), size, min));
				} else {
					if (max > -1 && min > max)
						throw new IllegalArgumentException(msg(U.U00108, getName(), min, max));
					if (size < min)
						throw new IllegalArgumentException(msg(U.U00110, getName(), size, min));
				}
			} 
			if (max > -1) {
				if (size > max)
					throw new IllegalArgumentException(msg(U.U00111, getName(),	size, max));
			}
		}
			
		/**
		 * Return the value as a int. Throw an exception if the value cannot be
		 * converted.
		 * 
		 * @return an int
		 */
		public int intValue() {
			return asInt(stringValue(), -1);
		}

		/**
		 * Split the value into a number of strings and convert them to
		 * integers. Throw an exception if a string cannot be converted or if
		 * the number of strings does not agree with the constraints. Return the
		 * integers in an array.
		 * 
		 * @param min
		 *            the minimum number of integers (no limit if negative)
		 * @param max
		 *            the maximum number of integers (no limit if negative)
		 * @return an int array
		 */
		public int[] intValues(int min, int max) {
			String[] strings = stringValues(min, max);
			int[] ints = new int[strings.length];
			try {
				for (int i = 0; i < ints.length; i++) {
					ints[i] = asInt(strings[i], i);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(msg(U.U00117, getName(), stringValue()), e);
			}
			return ints;
		}

		/**
		 * Split the value into a number of strings and convert them to
		 * integers. Throw an exception if a string cannot be converted. Return
		 * the integers in an array.
		 * 
		 * @return an int array
		 */
		public int[] intValues() {
			return intValues(-1, -1);
		}
		
		/**
		 * Return the value as a boolean. Throw an exception if the value cannot
		 * be converted.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @return a boolean
		 */
		public boolean booleanValue() {
			return asBoolean(stringValue(), -1);
		}

		/**
		 * Split the value into a number of strings and convert them to
		 * booleans. Throw an exception if a string cannot be converted or if
		 * the number of strings does not agree with the constraints. Return the
		 * booleans in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @param min
		 *            the minimum number of booleans (no limit if negative)
		 * @param max
		 *            the maximum number of booleans (no limit if negative)
		 * @return a boolean array
		 */
		public boolean[] booleanValues(int min, int max) {
			String[] strings = stringValues(min, max);
			boolean[] booleans = new boolean[strings.length];
			try {
				for (int i = 0; i < booleans.length; i++) {
					booleans[i] = asBoolean(strings[i], i);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(msg(U.U00119, getName(), stringValue()), e);
			}
			return booleans;
		}
		
		/**
		 * Split the value into a number of strings and convert them to
		 * booleans. Throw an exception if a string cannot be converted. Return
		 * the booleans in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @return a boolean array
		 */
		public boolean[] booleanValues() {
			return booleanValues(-1,  -1);
		}

		/**
		 * Return the value as a double. Throw an exception if the value cannot
		 * be converted.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @return a double
		 */
		public double doubleValue() {
			return asDouble(stringValue(), -1);
		}
		
		/**
		 * Split the value into a number of strings and convert them to
		 * doubles. Throw an exception if a string cannot be converted or if
		 * the number of strings does not agree with the constraints. Return the
		 * doubles in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @param min
		 *            the minimum number of doubles (no limit if negative)
		 * @param max
		 *            the maximum number of doubles (no limit if negative)
		 * @return a double array
		 */
		public double[] doubleValues(int min, int max) {
			String[] strings = stringValues(min, max);
			double[] doubles = new double[strings.length];
			try {
				for (int i = 0; i < doubles.length; i++) {
					doubles[i] = asDouble(strings[i], i);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(msg(U.U00118, getName(), stringValue()), e);
			}
			return doubles;
		}
		
		/**
		 * Split the value into a number of strings and convert them to doubles.
		 * Throw an exception if a string cannot be converted. Return the
		 * doubles in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @return a double array
		 */
		public double[] doubleValues() {
			return doubleValues(-1, -1);
		}
		
		/**
		 * Return the value as an Enum constant. Throw an exception if the value
		 * cannot be converted.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @param <T>
		 *            the type of the enum value
		 * @param enumClass
		 *            the enum type class
		 * @return an Enum
		 */
		public <T extends Enum<T>> T enumValue(Class<T> enumClass) {
			return asEnum(enumClass, stringValue(), -1);
		}

		/**
		 * Split the value into a number of strings and convert them to enum
		 * constants. Throw an exception if a string cannot be converted or if
		 * the number of strings does not agree with the constraints. Return the
		 * enum constants in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
		 * 
		 * @param <T>
		 *            the type of the enum values
		 * @param enumClass
		 *            the class object of the enum type
		 * @param min
		 *            the minimum number of enum constants (no limit if
		 *            negative)
		 * @param max
		 *            the maximum number of enum constants (no limit if
		 *            negative)
		 * @return an enum double array
		 */
		public <T extends Enum<T>> T[] enumValues(Class<T> enumClass, int min, int max) {
			String[] strings = stringValues(min, max);
			@SuppressWarnings("unchecked")
			T[] enums = (T[]) new Enum<?>[strings.length];
			try {
				for (int i = 0; i < enums.length; i++) {
					enums[i] = asEnum(enumClass, strings[i], i);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(msg(U.U00120, getName(), stringValue(), enumClass.getSimpleName()), e);
			}
			return enums;
		}
		
		/**
		 * Split the value into a number of strings and convert them to enum
		 * constants. Throw an exception if a string cannot be converted. Return
		 * the enum constants in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables.
		 * 
		 * @param <T>
		 *            the type of the enum values
		 * @param enumClass
		 *            the class object of the enum type
		 * @return an enum double array
		 */
		public <T extends Enum<T>> T[] enumValues(Class<T> enumClass) {
			return enumValues(enumClass, -1, -1);
		}

		protected boolean asBoolean(String value, int index) {
			String orig = value;
			value = value.toLowerCase();
			boolean result = value.equals(TRUE);
			if (!result && !value.equals(FALSE)) {
				String name = index >= 0 ? String.format("%s[%d]", getName(), index) : getName();
				throw new IllegalArgumentException(msg(U.U00112, name, orig));
			}
			return result;
		}
		
		protected int asInt(String value, int index) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				String name = index >= 0 ? String.format("%s[%d]", getName(), index) : getName();
				throw new IllegalArgumentException(msg(U.U00114, name, value));
			}
		}
		
		protected double asDouble(String value, int index) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				String name = index >= 0 ? String.format("%s[%d]", getName(), index) : getName();
				throw new IllegalArgumentException(msg(U.U00113, name, value));
			}
		}
		
		protected <T extends Enum<T>> T asEnum(Class<T> enumClass, String value, int index) {
			try {
				return Enum.valueOf(enumClass, value);
			} catch (Exception e) {
				String name = index >= 0 ? String.format("%s[%d]", getName(), index) : getName();
				throw new IllegalArgumentException(msg(U.U00115, name, value, enumClass.getSimpleName()));
			}
		}
		
		@Override
		public String toString() {
			return stringValue();
		}

	}
	
	/**
	 * The string which is parsed as the boolean true value is "true".
	 */
	public final static String TRUE = "true";
	/**
	 * The string which is parsed as the boolean false value is "false".
	 */
	public final static String FALSE = "false";
	
	private final static String BLANK = " ";
	private final static char DOLLAR = '$';
	private final static String DOLLARS = "$$";
	
	private Map<String, Value> args;
	private Map<String, String> variables;
	private TextFile textFile; // use only one for duplicate detection to work
	private List<String[]> sequence;
	private boolean loose;
	private LoggerBridge logger;
	private ArgsScanner argsScanner;
	private SymbolScanner symScanner;
	private Map<String, Integer> symCycleDetector;
	private ArgsIncluder includer;

	/**
	 * Constructor.
	 */
	public Args() {
		args = new HashMap<String, Args.Value>();
		def(COND);
		def(INCLUDE);
		variables = new HashMap<String, String>();
		textFile = new TextFile();
		argsScanner = new ArgsScanner(leftQuote, rightQuote, nameValueSeparator, escape);
		symScanner = new SymbolScanner(DOLLAR);
		symCycleDetector = new HashMap<String, Integer>();
	}
	
	private ArgsScanner getScanner() {
		return argsScanner;
	}
	
	private Args parseIncludeArgs(String input) {
		Args a = new Args();
		a.def(""); // mandatory file name
		a.def(INC_NAMES).init("");
		a.def(INC_CLASS).init("");
		a.def(INC_CONFIG).init("");
		a.parse(input);
		return a;
	}
	
	private Args parseIfArgs(String input) {
		Args a = new Args();
		a.def(COND_IF_NON_EMPTY);
		a.def(COND_THEN);
		a.def(COND_ELSE).init("");
		a.parse(input);
		return a;
	}
	
	private ArgsIncluder getIncluder(String className) {
		ArgsIncluder inc = null;
		if (className != null) {
			try {
				inc = (ArgsIncluder) Class.forName(className).newInstance();
				inc.setTextFileReader(textFile);
			} catch (Exception e) {
				throw new IllegalArgumentException(msg(U.U00161, className), e);
			}
		} else {
			if (includer == null) {
				includer = new ArgsIncluder();
				includer.setTextFileReader(textFile);
			}
			inc = includer;
		}
		return inc;
	}
	
	/**
	 * Enable loose mode and optionally set a logger. If available the logger is
	 * used to log unresolved names at log level "debug".
	 * 
	 * @param logger
	 *            logger
	 */
	public void setLoose(LoggerBridge logger) {
		loose = true;
		this.logger = logger;
	}
	
	/**
	 * Disable loose mode and clear the logger.
	 */
	public void setStrict() {
		loose = false;
		logger = null;
	}
	
	/**
	 * Activate or deactivate the parser sequence tracking mode. This mode must
	 * be set before using {@link #parse} for the {@link #getSequence} method to
	 * return a non-null result. By default this mode is not active.
	 * 
	 * @param track
	 *            if true, sequence tracking will be activated, else it will be deactivated
	 */
	public void setSequenceTrackingMode(boolean track) {
		if (track)
			sequence = new ArrayList<String[]>();
		else
			sequence = null;
	}

	/**
	 * Get the name-value pairs in original sequence. This method returns null
	 * when sequence tracking is not active. See
	 * {@link #setSequenceTrackingMode}.
	 * 
	 * @return a list of pairs or null
	 */
	public List<String[]> getSequence() {
		return sequence;
	}
	
	/**
	 * Return an iterator over all parameter names.
	 * Parameter names are not returned in any predictable order.
	 * 
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<String> iterator() {
		return args.keySet().iterator();
	}

	/**
	 * Get the size.
	 * 
	 * @return the number of parameters defined
	 */
	public int size() {
		return args.size();
	}
	
	/**
	 * Convenience method to parse parameters specified in an array. Elements
	 * are joined using a space separator into a single string and passed to
	 * {@link #parse(String)}. An <code>IllegalArgumentException</code> is 
	 * thrown when parsing fails.
	 * 
	 * @param args
	 *            an array of strings
	 * @throws IllegalArgumentException
	 */
	public void parse(String[] args) {
		parse(Misc.join(BLANK, args));
	}

	/**
	 * Parse parameters specified in a string. To parse a file, pass a string
	 * naming the file, like this:
	 * <pre><code>
	 * file = /some/where/config.txt
	 * </code></pre>
	 * An <code>IllegalArgumentException</code> is thrown when parsing fails.
	 * 
	 * @param string
	 *            a string containing a list of name-value pairs
	 * @throws IllegalArgumentException
	 */
	public void parse(String string) {
		if (sequence != null)
			sequence.clear();
		parse(scan(string));
	}
	
	private List<String[]> scan(String string) {
		return getScanner().asValuesAndPairs(string);
	}
	
	/**
	 * Parse list of name-value pairs.
	 * 
	 * @param pairs
	 *            a list of arrays of length 1 or 2 (name and value)
	 */
	private void parse(List<String[]> pairs) {
		for (String[] pair : pairs) {
			switch (pair.length) {
			case 1:
				pair[0] = resolve(pair[0]);
				put("", pair[0]);
				break;
			case 2:
				pair[0] = resolve(pair[0]);
				pair[1] = resolve(pair[1]);
				if (pair[0].equals(COND))
					parse(scan(parseIf(pair[1])));
				else if (pair[0].equals(INCLUDE))
					parse(parseInclude(pair[1]));
				else
					put(pair[0], pair[1]);
				break;
			default:
				throw new RuntimeException("bug: " + pair.length);
			}
		}
	}

	/**
	 * Define a scalar parameter. The name cannot be null but it can be empty,
	 * in which case it is known as a <em>positional</em> parameter. When a scalar
	 * parameter is repeated, the last value wins. An
	 * <code>IllegalArgumentException</code> is thrown if there is already a
	 * parameter with the same name.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return a definition object which can be used to define aliases and set a
	 *         default value
	 * @throws IllegalArgumentException
	 */
	public Definition def(String name) {
		putValue(name, new Value(name));
		return new Definition(this, name);
	}
	
	/**
	 * Put a value for the named parameter. Except in <em>loose mode</em> an
	 * <code>IllegalArgumentException</code> is thrown if there is no parameter
	 * with this name. If the parameter is a list parameter and the value is
	 * null, all values are cleared.
	 * <p>
	 * If the name is prefixed with $ it is a substitution
	 * variable, which is defined on the fly. If an existing substitution
	 * variable is set a second time it is ignored. This allows to set
	 * substitution variables with default values in parameter files and
	 * override them on the command line <b>before</b> the file.
	 * <p>
	 * Standalone keywords and values without a name are put with this method by
	 * using the convention of using an empty name. the following heuristic is
	 * used:
	 * <ol>
	 * <li>If a scalar is defined with a name equal to the value parameter and
	 * with a default value of "false", the value of the scalar is set to true.
	 * No resolution is done.
	 * <li>Else, if a scalar or a list is defined with a empty name, the value
	 * is resolved and used for the scalar or list.
	 * </ol>
	 * 
	 * @param name
	 *            the name of the parameter
	 * @param value
	 *            the value of the parameter
	 * @throws IllegalArgumentException
	 */
	public void put(String name, String value) {
		Misc.nullIllegal(name, "name null");
		if (!putKeyword(name, value)) {
			Value v = args.get(name);
			if (v == null) {
				if (isVariable(name))
					putVariable(name, value);
				else {
					if (loose) {
						if (logger != null)
							logger.debug(lazymsg(U.U00165, name.length() == 0 ? value : name));
					} else
						throw new IllegalArgumentException(msg(U.U00103, name.length() == 0 ? value : name));
				}
			} else {
				if (v.isRepeatable())
					v.append(value);
				else
					v.set(value);
				if (sequence != null)
					sequence.add(new String[] {name, value});
			}
		}
	}
	
	private boolean putKeyword(String name, String value) {
		boolean keyword = false;
		if (name.length() == 0) {
			Value v = args.get(value); // value, not name
			if (v != null && v.getDefault().equals(FALSE)) {
				v.set(TRUE);
				keyword = true;
				if (sequence != null)
					sequence.add(new String[] {value, ""});
			}
		}
		return keyword;
	}
	
	private String resolve(String input) {
		symCycleDetector.clear();
		return resolve0(input, 0, symCycleDetector);
	}
	
	private String resolve0(String input, int level, Map<String, Integer> cycleDetector) {
		if (input == null)
			throw new IllegalArgumentException("input null");
		boolean changed = false;
		StringBuffer b = new StringBuffer();
		Iterator<String> it = symScanner.split(input).iterator();
		while (it.hasNext()) {
			String s = it.next();
			if (s == null) {
				// null is a stand-in for $$
				assert it.hasNext();
				String symbol = it.next();
				if (!checkForCycle(symbol, level, cycleDetector))
					throw new IllegalArgumentException(msg(U.U00123, input, symbol));
				String resolved = variables.get(symbol);
				if (resolved == null) {
					b.append(DOLLARS);
					b.append(symbol);
				} else {
					changed = true;
					b.append(resolved);
				}
			} else 
				b.append(s);
		}
		// important! return input object if no change
		return changed ? resolve0(b.toString(), ++level, cycleDetector) : input;
	}
	
	private boolean checkForCycle(String symbol, int level, Map<String, Integer> cycleDetector) {
		boolean pass = true;
		Integer previousLevel = cycleDetector.get(symbol);
		if (previousLevel != null && previousLevel != level)
			pass = false;
		else
			cycleDetector.put(symbol, level);
		return pass;
	}
	
	/**
	 * Return the value object for the parameter specified. An exception is
	 * thrown if the the name is unknown. For a nameless parameter, pass an
	 * empty name.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return the value object
	 */
	public Value getVal(String name) {
		Value v = args.get(name);
		if (v == null)
			throw new IllegalArgumentException(msg(U.U00103, name));
		return v;
	}
	
	/**
	 * Return value of scalar parameter as a string.
	 * This is method is shorthand for 
	 * <pre><code>
	 * getVal(name).stringValue()
	 * </code></pre>
	 * @param name name of scalar parameter
	 * @return the string value
	 */
	public String get(String name) {
		return getVal(name).stringValue();
	}
	
	/**
	 * Return value of parameter as an array of strings. This is method is
	 * shorthand for
	 * 
	 * <pre>
	 * <code>
	 * getVal(name).stringValues()
	 * </code>
	 * </pre>
	 * 
	 * @param name
	 *            name of parameter
	 * @return an array of strings
	 */
	public String[] split(String name) {
		return getVal(name).stringValues();
	}

	/**
	 * Set a local variable. If a global or local variable with the same name
	 * exists nothing is done (the principle is that <em>the first one wins</em>
	 * ). If the value contains embedded variables these are substituted before
	 * adding the variable.
	 * 
	 * @param name
	 *            the name of the variable
	 * @param value
	 *            the value of the variable
	 * @return true if the variable was added or false if a variable with the
	 *         same name already exists
	 */
	public boolean putVariable(String name, String value) {
		boolean done = false;
		symScanner.verify(name);
		name = name.substring(1);
		if (variables.get(name) == null) {
			variables.put(name, value);
			done = true;
		}
		return done;
	}
	
	private boolean isVariable(String name) {
		return name.length() > 0 && name.charAt(0) == DOLLAR;
	}

	private void putValue(String name, Value value) {
		Misc.nullIllegal(name, "name null");
		Value v = args.get(name);
		if (v != null)
			throw new IllegalArgumentException(msg(U.U00104, name));
		put(name, value);
	}
	
	private void put(String name, Value value) {
		if (isVariable(name))
			throw new IllegalArgumentException(msg(U.U00121, name));
		args.put(name, value);
	}
	
	private Value internalGet(String name) {
		return args.get(name);
	}
	
	/**
	 * Parse a specification like:
	 * 
	 * <pre>
	 * <code>
	 * non-empty=[${x}] then=[foo] else=[bar]
	 * </pre>
	 * 
	 * </code> The pseudo-variables <em>non-empty</em>, <em>then</em>, and
	 * <em>else</em> are interpreted by the system inside an <em>if</em>
	 * statement. If the value of <em>non-empty</em> resolves to not empty the
	 * method returns the unresolved value of <em>then</em> else it returns the
	 * unresolved value of <em>else</em>. If <em>else</em> is omitted it returns
	 * an empty string. The two other pseudo variables cannot be omitted.
	 * 
	 * @param text
	 *            a string containing the if specification
	 * @return either the then value or the else value or an empty value
	 */
	private String parseIf(String text) {
		String result = "";
		Args a = parseIfArgs(text);
		String ifValue = a.get(COND_IF_NON_EMPTY);
		String thenValue = a.get(COND_THEN);
		String elseValue = a.get(COND_ELSE);
		if (ifValue.length() > 0)
			result = thenValue;
		else if (!Misc.isEmpty(elseValue))
			result = elseValue;
		return result;
	}
	
	private List<String[]> parseInclude(String text) {
		ArgsIncluder argsIncluder = null;
		try {
			Args a = parseIncludeArgs(text);
			String fileName = a.get("");
			String names = a.get(INC_NAMES);
			String classe = a.get(INC_CLASS);
			String config = a.get(INC_CONFIG);
			argsIncluder = Misc.isEmpty(classe) ? getIncluder(null) : getIncluder(classe);
			Map<String, String> map = null;
			if (!Misc.isEmpty(names)) {
				map = asMap(scan(names));
			}
			return argsIncluder.include(argsScanner, fileName, map, config);
		} finally {
			argsIncluder = null;
		}
	}

	private Map<String, String> asMap(List<String[]> pairs) {
		Map<String, String> map = new HashMap<String, String>();
		for (String[] pair : pairs) {
			map.put(pair[0],  pair.length == 2 ? pair[1] : "");
		}
		return map;
	}
	
	/**
	 * Reset the state of the parser. This method must be called between calls
	 * to {@link #parse(String)} or {@link #parse(String[])} unless parsing
	 * multiple inputs incrementally. Global variables are never cleared.
	 */
	public void reset() {
		for (Value v : args.values()) {
			v.set(null);
		}
		variables.clear();
		textFile.setDuplicateDetection(true); // resets duplicate detection
	}
	
}
