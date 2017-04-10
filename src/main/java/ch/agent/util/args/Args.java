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
 * parameter "qu ux" has one value, "[what = ever]". It is possible to change
 * the meta characters dynamically at run time, like this:
 * 
 * <pre>
 * <code>
 * Tokenizer.MetaCharacters = '':\ foo: bar 'qu ux':'[what = ever]' foo: '2nd val'
 * </code>
 * </pre>
 * 
 * The special parameter <code>Tokenizer.MetaCharacters</code> takes a value
 * with exactly four characters in predefined order: bracket open, bracket
 * close, name-value separator, escape character. In the example, names and
 * values are the same as previously but there is no need for escaping the "]"
 * of "[what = ever]".
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
 * When mappings are present, only parameters named in the mappings (
 * <q>foo</q> and
 * <q>quux</q> in the example) will be considered. If they are found in the file
 * the corresponding values will be assigned to parameters using the mapping
 * values (
 * <q>bar</q> and
 * <q>flix</q> in the example). This trick is useful when extracting specific
 * parameters from existing configuration files where names are defined by
 * someone else.
 * <p>
 * Except in <em>loose mode</em> parameter names must have been defined before
 * parsing. Loose mode is activated by {@link #setLoose} and deactivated by
 * {@link #setStrict}.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class Args implements Iterable<String> {

	public final static char EQ = '=';
	public final static String VAR_PREFIX = "$";
	public final static String TRUE = "true";
	public final static String FALSE = "false";
	
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
		 * @param alias
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
		 * mandatory.
		 * 
		 * @param value
		 * @return this definition
		 */
		public Definition init(String value) {
			Value v = args().internalGet(name());
			if (v == null)
				throw new IllegalArgumentException("bug: " + name());
			v.setDefault(value);
			return this;
		}
		
	}
	
	public abstract class Value {
		private String canonical;
		
		/**
		 * Constructor.
		 * 
		 * @param canonical canonical name of parameter
		 */
		public Value(String canonical) {
			this.canonical = canonical;
		}
		
		public abstract boolean isScalar();
		
		protected String getName() {
			return canonical;
		}

		public abstract void set(String value);
		
		public String getDefault() {
			return null;
		}

		public void setDefault(String value) {
			throw new IllegalStateException(msg(U.U00106, getName()));
		}

		/**
		 * Check the number of values. Throw an exception if the number of values does not match
		 * the constraint.
		 * 
		 * @param size
		 *            size constraint
		 * 
		 * @return this value
		 */
		public Value size(int size) {
			return size(size, size);
		}
		
		/**
		 * Check the number of values. Throw an exception if the size does not match
		 * the constraint.
		 * 
		 * @param minSize minimum size
		 * @param maxSize maximum size
		 * 
		 * @return this value 
		 */
		public Value size(int minSize, int maxSize) { 
			throw new IllegalStateException(msg(U.U00107, getName()));
		}
		
		/**
		 * Check the number of values. Throw an exception if the size does not match
		 * the constraint.
		 * 
		 * @param size maximum size
		 * 
		 * @return this value 
		 */
		public Value maxSize(int size) { 
			throw new IllegalStateException(msg(U.U00107, getName()));
		}
		
		/**
		 * Check the number of values. Throw an exception if the size does not match
		 * the constraint.
		 * 
		 * @param size minimum size
		 * 
		 * @return this value 
		 */
		public Value minSize(int size) { 
			throw new IllegalStateException(msg(U.U00107, getName()));
		}

		/**
		 * Return the value as a string. Throw an exception if the value is not
		 * scalar.
		 * 
		 * @return a string
		 */
		public String stringValue() {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}		

		/**
		 * Return the value split into a string array. Throw an exception if the
		 * value is not scalar.
		 * 
		 * @param separator
		 *            a string specifying the separator pattern, not null
		 * @param count
		 *            the number of elements, negative for any
		 * @return an string array
		 */
		public String[] stringSplit(String separator, int count) {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}

		/**
		 * Return the value as a string array. Throw an exception if the value is 
		 * scalar. 
		 * 
		 * @return a string array
		 */
		public String[] stringArray() {
			throw new IllegalArgumentException(msg(U.U00102, getName()));
		}
		
		/**
		 * Return the value as a int. Throw an exception if the value is not
		 * scalar or if the value cannot be converted.
		 * 
		 * @return an int
		 */
		public int intValue() {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}		

		/**
		 * Return the value split into an int array. Throw an exception if the
		 * value is not scalar or if the value cannot be converted.
		 * 
		 * @param separator
		 *            a string specifying the separator pattern, not null
		 * @param count
		 *            the number of elements, negative for any
		 * @return an int array
		 */
		public int[] intSplit(String separator, int count) {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}

		/**
		 * Return the value as an int array. Throw an exception if the value is 
		 * scalar or if any element cannot be converted.
		 * 
		 * @return an int array
		 */
		public int[] intArray() {
			throw new IllegalArgumentException(msg(U.U00102, getName()));
		}	
		
		/**
		 * Return the value as an Enum. Throw an exception if the value is not
		 * scalar or if the value cannot be converted.
		 * 
		 * @return an Enum 
		 */
		public Enum<?> enumValue(Enum<?> example) {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}
		
		/**
		 * Return the value as an Enum array. Throw an exception if the value is 
		 * scalar or if any element cannot be converted.
		 * 
		 * @return an Enum array
		 */
		public Enum<?>[] enumArray(Enum<?> example) {
			throw new IllegalArgumentException(msg(U.U00102, getName()));
		}
		
		/**
		 * Return the value as a boolean. Throw an exception if the value is not
		 * scalar or if the value cannot be converted.
		 * 
		 * @return a boolean
		 */
		public boolean booleanValue() {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}		

		/**
		 * Return the value split into a boolean array. Throw an exception if the
		 * value is not scalar or if the value cannot be converted.
		 * 
		 * @param separator
		 *            a string specifying the separator pattern, not null
		 * @param count
		 *            the number of elements, negative for any
		 * @return a boolean array
		 */
		public boolean[] booleanSplit(String separator, int count) {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}

		/**
		 * Return the value as a boolean array. Throw an exception if the value is 
		 * scalar or if any element cannot be converted.
		 * 
		 * @return a boolean array
		 */
		public boolean[] booleanArray() {
			throw new IllegalArgumentException(msg(U.U00102, getName()));
		}		

		/**
		 * Return the value as a double. Throw an exception if the value is not
		 * scalar or if the value cannot be converted.
		 * 
		 * @return a double
		 */
		public double doubleValue() {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
		}		

		/**
		 * Return the value as a double array. Throw an exception if the value is 
		 * scalar or if any element cannot be converted.
		 * 
		 * @return a double array
		 */
		public double[] doubleArray() {
			throw new IllegalArgumentException(msg(U.U00102, getName()));
		}
		
		/**
		 * Return the value split into a double array. Throw an exception if the
		 * value is not scalar or if the value cannot be converted.
		 * 
		 * @param separator
		 *            a string specifying the separator pattern, not null
		 * @param count
		 *            the number of elements, negative for any
		 * @return a double array
		 */
		public double[] doubleSplit(String separator, int count) {
			throw new IllegalArgumentException(msg(U.U00101, getName()));
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
		
		@SuppressWarnings("unchecked")
		protected Enum<?> asEnum(Enum<?> example, String value, int index) {
			try {
				return Enum.valueOf(example.getClass(), value);
			} catch (Exception e) {
				String name = index >= 0 ? String.format("%s[%d]", getName(), index) : getName();
				throw new IllegalArgumentException(msg(U.U00115, name, value, example.getClass().getSimpleName()));
			}
		}

	}

	private class ScalarValue extends Value {
		private String value;
		private String defaultValue;

		public ScalarValue(String canonical) {
			super(canonical);
		}
		
		@Override
		public boolean isScalar() {
			return true;
		}

		@Override
		public String getDefault() {
			return defaultValue;
		}

		@Override
		public void setDefault(String value) {
			this.defaultValue = value;
		}

		@Override
		public void set(String value) {
			this.value = value;
		}

		@Override
		public String stringValue() {
			if (value == null) {
				if (defaultValue == null)
					throw new IllegalArgumentException(msg(U.U00105, getName()));
				return defaultValue;
			}
			return value;
		}
		
		@Override
		public String[] stringSplit(String separator, int count) {
			String s = stringValue();
			try {
				return Misc.split(s, separator, count);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(msg(U.U00116, getName(), s, count, separator));
			}
		}

		@Override
		public int intValue() {
			return asInt(stringValue(), -1);
		}

		@Override
		public int[] intSplit(String separator, int count) {
			String[] parts = stringSplit(separator, count);
			int[] result = new int[parts.length];
			try {
				for (int i = 0; i < result.length; i++) {
					result[i] = Integer.parseInt(parts[i]);
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(msg(U.U00117, getName(), stringValue(), count, separator));
			}
			return result;
		}

		@Override
		public boolean booleanValue() {
			return asBoolean(stringValue(), -1);
		}
		
		@Override
		public boolean[] booleanSplit(String separator, int count) {
			String[] parts = stringSplit(separator, count);
			boolean[] result = new boolean[parts.length];
			try {
				for (int i = 0; i < result.length; i++) {
					result[i] = asBoolean(parts[i], -1);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(msg(U.U00119, getName(), stringValue(), count, separator));
			}
			return result;
		}



		@Override
		public double doubleValue() {
			return asDouble(stringValue(), -1);
		}
		
		@Override
		public double[] doubleSplit(String separator, int count) {
			String[] parts = stringSplit(separator, count);
			double[] result = new double[parts.length];
			try {
				for (int i = 0; i < result.length; i++) {
					result[i] = Double.parseDouble(parts[i]);
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(msg(U.U00118, getName(), stringValue(), count, separator));
			}
			return result;
		}

		@Override
		public Enum<?> enumValue(Enum<?> example) {
			return asEnum(example, stringValue(), -1);
		}

		@Override
		public String toString() {
			return stringValue();
		}
		
	}

	private class ListValue extends Value {
		private List<String> values;

		public ListValue(String canonical) {
			super(canonical);
			this.values = new ArrayList<String>();
		}

		@Override
		public boolean isScalar() {
			return false;
		}

		@Override
		public void set(String value) {
			if (value == null)
				this.values.clear();
			else
				this.values.add(value);
		}

		@Override
		public Value size(int minSize, int maxSize) {
			if (minSize < 0 || maxSize < 0)
				throw new IllegalArgumentException("minSize < 0 or maxSize < 0");
			if (minSize == maxSize) {
				if (minSize > -1 && values.size() != minSize)
					throw new IllegalArgumentException(msg(U.U00108, getName(), 
							values.size(), minSize));
			} else {
				if (values.size() < minSize || values.size() > maxSize)
					throw new IllegalArgumentException(msg(U.U00109, getName(), 
							values.size(), minSize, maxSize));
			}
			return this;
		}
		
		@Override
		public Value minSize(int size) {
			if (size < 0)
				throw new IllegalArgumentException("size < 0");
			if (values.size() < size)
				throw new IllegalArgumentException(msg(U.U00110, getName(), 
						values.size(), size));
			return this;
		}
		
		@Override
		public Value maxSize(int size) {
			if (size < 0)
				throw new IllegalArgumentException("size < 0");
			if (values.size() > size)
				throw new IllegalArgumentException(msg(U.U00111, getName(), 
						values.size(), size));
			return this;
		}

		@Override
		public String[] stringArray() {
			return values.toArray(new String[values.size()]);
		}

		@Override
		public int[] intArray() {
			int[] result = new int[values.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = asInt(values.get(i), i);
			}
			return result;
		}

		@Override
		public boolean[] booleanArray() {
			boolean[] result = new boolean[values.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = asBoolean(values.get(i), i);
			}
			return result;
		}

		@Override
		public double[] doubleArray() {
			double[] result = new double[values.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = asDouble(values.get(i), i);
			}
			return result;
		}

		@Override
		public Enum<?>[] enumArray(Enum<?> example) {
			Enum<?>[] result = new Enum<?>[values.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = asEnum(example, values.get(i), i);
			}
			return result;
		}
		
	}

	private class ArgsFileVisitor implements TextFile.Visitor {

		private StringBuffer buffer;
		private boolean simple;
		private String separator;
		
		public ArgsFileVisitor(boolean simple, String separator) {
			super();
			buffer = new StringBuffer();
			this.simple = simple;
			this.separator = separator;
		}

		@Override
		public boolean visit(int lineNr, String line) throws Exception {
			if (!line.trim().startsWith(COMMENT)) {
				if (simple) {
					if (line.indexOf(EQ) >= 0)
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
	private final boolean keywords;
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
	 * @param keywords
	 *            if true support also keywords, else name-values only
	 * @param fileName
	 *            the name of the "file" parameter, or null
	 * @param ifGrammar
	 *            an array with 4 strings for the "if-nonempty-then-else" grammar
	 * @param suffix
	 *            the suffix used to request simple parsing
	 * @param sep
	 *            a regular expression used as the mapping separator, or null
	 */
	public Args(boolean keywords, String fileName, String[] ifGrammar, String suffix, String sep) {
		this.keywords = keywords;
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
		this(true, null, null, null, null);
	}
	
	/**
	 * Construct an Args object using defaults.The default values for the name
	 * of the file parameter, the suffix, and the mapping separator are taken
	 * from {@link #FILE}, {@link Args#FILE_SIMPLE_SUFFIX} and
	 * {@link #MAPPING_SEPARATOR}.
	 * 
	 * @param keywords
	 *            if true support also keywords, else name-values only
	 */
	public Args(boolean keywords) {
		this(keywords, null, null, null, null);
	}
	
	private ArgsScanner getScanner() {
		return new ArgsScanner('[', ']', EQ, '\\');
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
		parse(getScanner().asValuesAndPairs(string, !keywords));
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
	 * in which case it is known as a <q>positional</q> parameter. When a scalar
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
		putValue(name, new ScalarValue(name));
		return new Definition(this, name);
	}
	
	/**
	 * Define a list parameter. The name cannot be null but it can be empty, in
	 * which case it is known as a <q>positional</q> parameter. When a list
	 * parameter is repeated all values are returned. An
	 * <code>IllegalArgumentException</code> is thrown if there is already a
	 * parameter with the same name.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return a definition object which can be used to define aliases
	 * @throws IllegalArgumentException
	 */
	public Definition defList(String name) {
		putValue(name, new ListValue(name));
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
			if (v != null && v.isScalar() && v.getDefault().equals(FALSE)) {
				v.set(TRUE);
				keyword = true;
				if (sequence != null)
					sequence.add(new String[] {value, ""});
			}
		}
		return keyword;
	}

	private String resolve(String value) {
		String[] parts = value.split("\\$\\{");
		StringBuilder s = new StringBuilder(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			int len = s.length();
			if (len > 0 && s.charAt(len - 1) == '\\') {
				// \$ is an escape
				s.deleteCharAt(len - 1);
				s.append("${" + parts[i]);
			} else {
				String[] nameRemainder = parts[i].split("}", 2);
				if (nameRemainder.length == 1)
					s.append("${" + nameRemainder[0]);
				else {
					
					String resolved = vars.get(nameRemainder[0]);
					if (resolved == null)
						throw new IllegalArgumentException(msg(U.U00122, value, nameRemainder[0]));
					else
						s.append(resolved);
					s.append(nameRemainder[1]);
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
			Map<String, String> map = asMap(getScanner().asValuesAndPairs(text, false));
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
		ArgsFileVisitor visitor = new ArgsFileVisitor(simple, SEPARATOR);
		textFile.read(fileName, visitor);
		return getScanner().asValuesAndPairs(visitor.getContent(), !keywords);
	}
	
	private List<String[]> parseFile(boolean simple, String fileName, String mappings) throws IOException {
		List<String[]> pairs = parseFile(simple, fileName);
		Map<String, String> map = asMap(getScanner().asValuesAndPairs(mappings, false));
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
