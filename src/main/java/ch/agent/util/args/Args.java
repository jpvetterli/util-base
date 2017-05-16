package ch.agent.util.args;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.agent.util.STRINGS.U;
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
		String metaChars = System.getProperty(ARGS_META);
		if (metaChars != null) {
			char[] chars = validate(metaChars);
			leftQuote = chars[0];
			rightQuote = chars[1];
			nameValueSeparator = chars[2];
			escape = chars[3];
		} else {
			leftQuote = '[';
			rightQuote = ']';
			nameValueSeparator = '=';
			escape = '\\';
		}
	}
	
	/**
	 * Validate a string of meta characters. There must be four characters and
	 * they must be different. The sequence is
	 * <ol>
	 * <li>left quote,
	 * <li>right quote,
	 * <li>name-value separator,
	 * <li>escape.
	 * </ol>
	 * The characters are returned in an array in that sequence.
	 * 
	 * @param metaChars
	 *            a string of length 4
	 * @return an array of length 4
	 */
	public static char[] validate(String metaChars) {
		if (metaChars.length() != 4)
			throw new IllegalArgumentException(msg(U.U00164, metaChars));
		char lq = metaChars.charAt(0);
		char rq = metaChars.charAt(1);
		char nvs = metaChars.charAt(2);
		char esc = metaChars.charAt(3);
		if (lq == rq || lq == nvs || lq == esc || rq == nvs || rq == esc || nvs == esc)
			throw new IllegalArgumentException(msg(U.U00163, lq, rq, nvs, esc));
		return new char[]{lq, rq, nvs, esc};
	}
	
	/**
	 * There are four meta characters (default in parentheses):
	 * <ol>
	 * <li>left quote ([)
	 * <li>right quote (])
	 * <li>name-value separator (=)
	 * <li>escape (\)
	 * </ol>
	 * For nested quotes to function the left and right quotes must be
	 * different.
	 */
	private final static char leftQuote;
	private final static char rightQuote;
	private final static char nameValueSeparator;
	private final static char escape;
	
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
					new StringBuilder(this.value.length() + value.length() + 3).append(this.value).append(SEPARATOR).append(leftQuote).append(value).append(rightQuote).toString();
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
		 * 
		 * @return a string
		 */
		public String stringValue() {
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
		 * specified by two parameters. Negative parameters are ignored.
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
		 * 
		 * @return an array of strings
		 */
		public String[] stringValues() {
			return stringValues(-1, -1);
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
		 * 
		 * @return a boolean array
		 */
		public boolean[] booleanValues() {
			return booleanValues(-1,  -1);
		}

		/**
		 * Return the value as a double. Throw an exception if the value cannot
		 * be converted.
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
		 * 
		 * @return a double array
		 */
		public double[] doubleValues() {
			return doubleValues(-1, -1);
		}
		
		/**
		 * Return the value as an Enum constant. Throw an exception if the value
		 * cannot be converted.
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

	private class ArgsFileVisitor implements TextFile.Visitor {

		private StringBuffer buffer;
		private boolean simple;
		private String separator;
		private char equals;
		
		public ArgsFileVisitor(boolean simple, String separator, char equals) {
			super();
			buffer = new StringBuffer();
			this.simple = simple;
			this.separator = separator;
			this.equals = equals;
		}

		@Override
		public boolean visit(int lineNr, String line) throws Exception {
			if (!line.trim().startsWith(COMMENT)) {
				if (simple) {
					if (line.indexOf(equals) >= 0)
						buffer.append(line);
				} else 
					buffer.append(line);
				buffer.append(separator);
			}
			return false;
		}
		
		public String getContent() {
			return buffer.toString();
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
	
	private final static String VAR_PREFIX = "$";
	
	/**
	 * The default name of the if parameter is simply "if".
	 */
	public static final String IF = "if";
	public static final String IF_NON_EMPTY = "non-empty";
	public static final String IF_THEN = "then";
	public static final String IF_ELSE = "else";
	
	/**
	 * The default name of the file parameter is simply "file".
	 */
	public static final String FILE = "file";
	
	/**
	 * The default suffix after FILE to force "simple" parsing. Simple parsing
	 * excludes all lines which don't look like simple name-value pairs.
	 */
	public static final String FILE_SIMPLE_SUFFIX = "*";
	/**
	 * The default mapping separator is a semicolon surrounded
	 * by zero or more white space characters.
	 */
	public static final String MAPPING_SEPARATOR = "\\s*;\\s*";
	
	private static final String SEPARATOR = " ";
	private static final String COMMENT = "#";
	private final String fileParameterName;
	private final String ifName;
	private final String ifNonEmptyName;
	private final String ifThenName;
	private final String ifElseName;
	private String simpleFileParameterName;
	private String mappingSeparator;
	private Map<String, Value> args;
	private Map<String, String> vars;
	private TextFile textFile; // use only one for duplicate detection to work
	private List<String[]> sequence;
	private boolean loose;
	private LoggerBridge logger;

	/**
	 * Construct a custom <code>Args</code> object. Nulls are valid arguments
	 * and will be replaced with default values. <code>Args</code> is either in
	 * <em>strict mode</em> or in <em>loose mode</em>.
	 * 
	 * @param fileName
	 *            the name of the "file" parameter, or null
	 * @param ifGrammar
	 *            an array with 4 strings for the "if-nonempty-then-else" grammar
	 * @param suffix
	 *            the suffix used to request simple parsing
	 * @param sep
	 *            a regular expression used as the mapping separator, or null
	 */
	public Args(String fileName, String[] ifGrammar, String suffix, String sep) {
		this.fileParameterName = (fileName == null ? FILE : fileName);
		if (ifGrammar != null) {
			if (ifGrammar.length != 4)
				throw new IllegalArgumentException("ifGrammar.length != 4");
			this.ifName = ifGrammar[0];
			this.ifNonEmptyName = ifGrammar[1];
			this.ifThenName = ifGrammar[2];
			this.ifElseName = ifGrammar[3];
		} else {
			this.ifName = IF;
			this.ifNonEmptyName = IF_NON_EMPTY;
			this.ifThenName = IF_THEN;
			this.ifElseName = IF_ELSE;
		}
		this.simpleFileParameterName = (suffix == null ? 
				fileParameterName + FILE_SIMPLE_SUFFIX : fileParameterName + suffix);
		this.mappingSeparator = (sep == null ? MAPPING_SEPARATOR : sep);
		args = new HashMap<String, Args.Value>();
		vars = new HashMap<String, String>();
		textFile = new TextFile();
	}
	
	/**
	 * Construct an Args object using defaults. Keywords are not supported. The
	 * default values for the name of the file parameter, the suffix, and the
	 * mapping separator are taken from {@link #FILE},
	 * {@link Args#FILE_SIMPLE_SUFFIX} and {@link #MAPPING_SEPARATOR}.
	 */
	public Args() {
		this(null, null, null, null);
	}
	
	private ArgsScanner getScanner() {
		return new ArgsScanner(leftQuote, rightQuote, nameValueSeparator, escape);
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
		parse(Misc.join(SEPARATOR, args));
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
	 * Parse <code>List</code> of name-value pairs.
	 * 
	 * @param pairs
	 *            a list of arrays of length 2 (name and value)
	 */
	private void parse(List<String[]> pairs) {
		for (String[] pair : pairs) {
			switch (pair.length) {
			case 1:
				// resolve ${FOO} which can be anything, multiple name-value pairs, etc.
				String resolved = resolve(pair[0]);
				if (!resolved.equals(pair[0]))
					parse(scan(resolved));
				else
					put("", pair[0]);
				break;
			case 2:
				if (pair[0].equals(fileParameterName))
					parse(parseFileAndMapping(false, pair[0], resolve(pair[1])));
				else if (pair[0].equals(simpleFileParameterName))
					parse(parseFileAndMapping(true, pair[0], resolve(pair[1])));
				else if (pair[0].equals(ifName))
					parse(parseIf(pair[1]));
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
	 * If the name is prefixed with {@link Args#VAR_PREFIX} it is a substitution
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
				if (name.startsWith(VAR_PREFIX)) {
					String variable = name.substring(VAR_PREFIX.length());
					putVariable(variable, value);
				} else {
					if (loose) {
						if (logger != null)
							logger.debug(lazymsg(U.U00165, name.length() == 0 ? value : name));
					} else
						throw new IllegalArgumentException(msg(U.U00103, name.length() == 0 ? value : name));
				}
			} else {
				String resolved = resolve(value);
				if (v.isRepeatable())
					v.append(resolved);
				else
					v.set(resolved);
				if (sequence != null)
					sequence.add(new String[] {name, resolved});
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
	
	private boolean removeEscape(StringBuilder s) {
		int len = s.length();
		boolean isEscape = len > 0 && s.charAt(len - 1) == escape;
		if (isEscape)
			s.deleteCharAt(len - 1);
		return isEscape;
	}
	
	private String resolve(String value) {
		StringBuilder s = new StringBuilder();
		while (value.length() > 0) {
			int prefix = value.indexOf("$$");
			if (prefix < 0) {
				s.append(value);
				value = "";
			} else if (prefix >= 0) {
				s.append(value.substring(0,  prefix));
				value = value.substring(prefix + 2);
				if (removeEscape(s)) {
					s.append("$$");
				} else if (value.length() == 0) {
					s.append("$$");
				} else if (Character.isWhitespace(value.charAt(0))) {
					s.append("$$");
				} else {
					String[] nextStringAndRemainder = getScanner().immediateString(value);
					if (nextStringAndRemainder[0] == null) {
						// probably a name-value separator
						s.append("$$");
					} else {
						// possibly a variable
						String resolved = vars.get(nextStringAndRemainder[0]);
						if (resolved == null)
							throw new IllegalArgumentException(msg(U.U00122, value, nextStringAndRemainder[0]));
						else
							s.append(resolved);
						value = nextStringAndRemainder[1];
					}
				}
			}
		}
		return s.toString();
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
	 * Return a copy of all variables. Variables are arguments prefixed with a 
	 * special prefix (by default a dollar sign) which do not need be defined.
	 * 
	 * @return a map containing all variables 
	 */
	public Map<String, String> getVariables() {
		Map<String, String> result = new HashMap<String, String>();
		for (Map.Entry<String, String> e : vars.entrySet()) {
			result.put(e.getKey(), e.getValue());
		}
		return result;
	}
	
	/**
	 * Add a variable. If a variable with the same name exists nothing is done
	 * (the principle is that <em>the first one wins</em>). If the value
	 * contains embedded variables these are substituted before adding the
	 * variable.
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
		if (vars.get(name) == null) {
			vars.put(name, resolve(value));
			done = true;
		}
		return done;
	}
	
	private void putValue(String name, Value value) {
		Misc.nullIllegal(name, "name null");
		Value v = args.get(name);
		if (v != null)
			throw new IllegalArgumentException(msg(U.U00104, name));
		put(name, value);
	}
	
	private void put(String name, Value value) {
		if (name.startsWith(VAR_PREFIX))
			throw new IllegalArgumentException(msg(U.U00121, name, VAR_PREFIX));
		args.put(name, value);
	}

	private Value internalGet(String name) {
		return args.get(name);
	}
	
	/**
	 * Return a list of name-value pairs from a text file. Lines starting with a
	 * hash are skipped.
	 * <p>
	 * The file specification consists of a file name optionally followed by a
	 * mapping separator ({@link #MAPPING_SEPARATOR}) and zero or more mappings,
	 * which are simply name-value pairs. When such mappings are present, only
	 * the names found in the mapping will be extracted from the file and the
	 * corresponding values will be used to rename the pairs.
	 * An <code>IllegalArgumentException</code> will be thrown if anything 
	 * goes wrong while parsing the file specification. Some of these exceptions
	 * are wrapped <code>IOException</code>s.
	 * 
	 * @param simple request simple parsing 
	 * @param fileParameterName the name of the parameter (typically, "file" or "file*")
	 * @param fileSpec
	 *            a file name possibly followed by mappings
	 * @return a list of arrays of length 2 (name and value)
	 * @throws IllegalArgumentException
	 */
	private List<String[]> parseFileAndMapping(boolean simple, String fileParameterName, String fileSpec) {
		String[] fm = fileSpec.split(mappingSeparator, 2);
		try {
			if (fm.length > 1)
				return parseFile(simple, fm[0], fm[1]);
			else
				return parseFile(simple, fm[0]);
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.U00130, fileParameterName, fileSpec), e);
		}
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
		try {
			Map<String, String> map = asMap(scan(text));
			String nonEmptyValue = map.get(ifNonEmptyName);
			String thenValue = map.get(ifThenName);
			String elseValue = map.get(ifElseName);
			if (nonEmptyValue == null) 
				throw new IllegalArgumentException(msg(U.U00133, ifNonEmptyName));
			if (thenValue == null) 
				throw new IllegalArgumentException(msg(U.U00133, ifThenName));
			if (map.size() != (elseValue == null ? 2 : 3)) 
				throw new IllegalArgumentException(msg(U.U00134, ifNonEmptyName, ifThenName, ifElseName));
			String resolved = resolve(nonEmptyValue);
			if (resolved.length() > 0)
				result = thenValue;
			else if (elseValue != null)
				result = elseValue;
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.U00132, ifName, text), e);
		}
		return result;
	}
	
	private List<String[]> parseFile(boolean simple, String fileName) throws IOException {
		
		int wip;
//		must get = from the scanner
//		I am creating new scanners all the time
//		it's not necessary since the tokenizer is reset
//		it's not like if it has to be reentrant
//		or?
		
		int the_way; //  to go is to pass characters to scanner but fast (no check)
		ArgsFileVisitor visitor = new ArgsFileVisitor(simple, SEPARATOR, nameValueSeparator);
		textFile.read(fileName, visitor);
		return scan(visitor.getContent());
	}
	
	private List<String[]> parseFile(boolean simple, String fileName, String mappings) throws IOException {
		List<String[]> pairs = parseFile(simple, fileName);
		Map<String, String> map = asMap(scan(mappings));
		Iterator<String[]> it = pairs.iterator();
		while(it.hasNext()) {
			String[] pair = it.next();
			String mapping = map.get(pair[0]);
			if (mapping == null)
				it.remove();
			else
				pair[0] = mapping;
		}
		return pairs;
	}
	
	private Map<String, String> asMap(List<String[]> pairs) {
		Map<String, String> map = new HashMap<String, String>();
		for (String[] pair : pairs) {
			map.put(pair[0],  pair[1]);
		}
		return map;
	}
	
	/**
	 * Reset the state of the parser. This method must be called between calls
	 * to {@link #parse(String)} or {@link #parse(String[])} unless parsing
	 * multiple inputs incrementally.
	 */
	public void reset() {
		for (Value v : args.values()) {
			v.set(null);
		}
		vars.clear();
	}
	
}
