package ch.agent.util.args;

import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;
import ch.agent.util.file.TextFile;

/**
 * Args is a parser for command line arguments. It provides a simple language
 * and a small set of built-in operators. It is named after the parameter of the
 * main method of Java programs, usually written like this:
 * 
 * <pre>
 * <code>
 * public static void main(String[] <b>args</b>) {
 *    // etc.
 * }
 * </code>
 * </pre>
 * 
 * Using the parser consists of three steps:
 * <ol>
 * <li><b>Defining</b> parameters. All parameters are defined with the
 * {@link #def} method; the definition can be customized using the
 * {@link Definition} object returned.
 * 
 * <li><b>Setting</b> values or parsing values from an input. Parameter values
 * can be set using {@link #put} methods or parsed from text. Such text is
 * written in a simple but flexible specification language, described shortly
 * 
 * <li><b>Getting</b> values of parameters. All values are basically strings
 * encapsulated in a {@link Value} object. Methods are available to extract
 * values of various types and as individual values or as arrays.
 * 
 * </ol>
 * 
 * <h3>The specification language</h3>
 * 
 * A specification consists of a series of <em>name-value pairs</em> and
 * isolated values and keywords. In a name-value pair, the name corresponds to
 * the a parameter definition. An isolated value is the value of a nameless
 * parameter. To have multiple isolated values, the nameless parameter must have
 * been defined as repeatable. Some isolated values are known as
 * <em>keywords</em>; a keyword corresponds to a parameter defined with a
 * default value of "false". The occurrence of the parameter name as an isolated
 * value sets the value of the parameter to "true".
 * <p>
 * A parameter can only be used if it has been defined. Sometimes it is useful
 * to define parameters on the fly and this is possible with <em>variables</em>.
 * Variables have a name with a $ sign in front. They are referenced by
 * prefixing them with $$. When a variable reference is seen in the text it is
 * replaced by the value of the variable if available, else it is left as is.
 * This is called an unresolved variable. When accessing a parameter value which
 * contains unresolved variables, an error occurs, unless using special care.
 * <p>
 * An other important difference between parameters and variables is their
 * behavior with respect to repeated values. With a (non-repeatable) parameter,
 * the last value wins. For variables, the first wins. This allows to override
 * default values of variables in configuration files (see the include operator,
 * below) with values specified before the file is included.
 * <p>
 * The language syntax supports names and values with arbitrary content using a
 * <em>nested</em> quoting notation. Suppose a piece of software uses Args to
 * parse its parameters and suppose this software has multiple components which
 * also use Args for their parameters. A top-level component can define its
 * parameters without knowing any detail about the other components. It only
 * needs to define a parameter for each such component to take opaque values and
 * pass them along without bothering about unresolved variables. This feature
 * allows to simulate subroutines in the specification language, which is quite
 * useful when a specification is large.
 * <p>
 * When finding an isolated value, the parser resolves it for variables. If
 * resolution effectively changes the value, it is parsed again recursively.
 * This allows to insert pieces of specification code via variables. This
 * behavior does not apply to name-value pairs which are resolved but not
 * recursively parsed.
 * <p>
 * When using nested Args, applications can pass variables from one level to the
 * next using {@link #getVariables} and {@link #putVariable}.
 * <p>
 * Details about the syntax are available from the documentation of
 * {@link NameValueScanner} and {@link SymbolScanner}. The specification
 * language uses five meta characters (defaults in parentheses):
 * <ul>
 * <li>the opening quote ([),
 * <li>the closing quote (]),
 * <li>the name-value separator (=),
 * <li>the escape (\),
 * <li>the variable prefix ($).
 * </ul>
 * These can be overridden by passing a string of 5 characters as a System
 * property which will be interpreted in the above sequence. The property is
 * <code>ArgsMetaCharacters</code>.
 * 
 * <h3>Built-in operators</h3>
 * 
 * Args provides two built-in operators, <em>condition</em> for conditional
 * parsing and <em>include</em> for file inclusion. Syntactically, these
 * operators are parameter names.
 * <p>
 * The complete syntax of the condition operator is
 * 
 * <pre>
 * <code>
 * condition = [if=[...] then=[...] else=[...]]
 * </code>
 * </pre>
 * 
 * When the value of "if" is non-empty, the value of "then" is used, else the
 * value of "else" is used. The "else" part is the only one which can be
 * omitted.
 * 
 * <p>
 * The complete syntax of the include operator is
 * 
 * <pre>
 * <code>
 * include = [<em>filename</em> names=[...] extractor=[...] extractor-parameters=[...]]
 * </code>
 * </pre>
 * 
 * The operator includes the contents of a file in the parser input. The file
 * must be located either in the file system or the classpath. It is possible to
 * include multiple files and includes can be nested.
 * 
 * Only the file name (a nameless value) is mandatory. In this case, the quoting
 * brackets can usually be omitted. The names parameter take zero or more
 * name-value pairs or isolated values. And isolated value gives the name of a
 * parameter to extract from the file. The name in a name-value pair also gives
 * the name of a parameter to extract and the value specifies a translation for
 * that name. This allows to extract parameters from the configuration files of
 * other applications and use them automatically under a name of our own
 * choosing. The extractor parameter allows to specify a class to use for the
 * extraction. The class must extend the {@link FileIncluder}, provided with
 * Args. If necessary, a parameter can be passed to that class using
 * extractor-parameters.
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
		dollar = chars[4];
	}
	
	/**
	 * Validate a string of meta characters. There must be five characters and
	 * they must be different. The sequence (defaults in parentheses) is
	 * <ol>
	 * <li>left quote ([),
	 * <li>right quote (]),
	 * <li>name-value separator (=),
	 * <li>escape (\).
	 * <li>variable prefix ($).
	 * </ol>
	 * The characters are returned in an array in that sequence. If the input is
	 * null default meta characters are returned.
	 * 
	 * @param metaChars
	 *            a string of length 5
	 * @return an array of length 5
	 */
	public static char[] validateMetaCharacters(String metaChars) {
		if (metaChars == null)
			return new char[] { '[', ']', '=', '\\', '$'};
		else {
			if (metaChars.length() != 5)
				throw new IllegalArgumentException(msg(U.U00164, metaChars));
			char lq = metaChars.charAt(0);
			char rq = metaChars.charAt(1);
			char nvs = metaChars.charAt(2);
			char esc = metaChars.charAt(3);
			char dol = metaChars.charAt(4);
			validateMetaCharacters(lq, rq, nvs, esc, dol);
			return new char[] { lq, rq, nvs, esc, dol };
		}
	}
	
	/**
	 * Validate five meta characters. 
	 * The five characters must be different.
	 *
	 * @param leftQuote the left quote 
	 * @param rightQuote the right quote 
	 * @param nameValueSeparator the name-value separator
	 * @param escape the escape
	 * @param dollar the variable prefix
	 */
	public static void validateMetaCharacters(char leftQuote, char rightQuote, char nameValueSeparator, char escape, char dollar) {
		if (leftQuote == rightQuote || leftQuote == nameValueSeparator || leftQuote == escape || leftQuote == dollar
			|| rightQuote == nameValueSeparator || rightQuote == escape || rightQuote == dollar
			|| nameValueSeparator == escape || nameValueSeparator == dollar
			|| escape == dollar)
			throw new IllegalArgumentException(msg(U.U00163, leftQuote, rightQuote, nameValueSeparator, escape, dollar));
	}

	private final static char leftQuote;
	private final static char rightQuote;
	private final static char nameValueSeparator;
	private final static char escape;
	private final static char dollar;
	
	private final static String COND = "condition";
	private final static String COND_IF_NON_EMPTY = "if";
	private final static String COND_THEN = "then";
	private final static String COND_ELSE = "else";
	
	private final static String INCLUDE = "include";
	private final static String INC_NAMES = "names";
	private final static String INC_CLASS = "extractor";
	private final static String INC_CONFIG = "extractor-parameters";
	
	/**
	 * A Definition object allows to customize a parameter. It supports the
	 * method chaining coding style. For example, to define a parameter with two
	 * aliases and a default value, one would write:
	 * 
	 * <pre>
	 * <code>
	 * args.def("foo").aka("f").aka("foooo").init("bar");
	 * </code>
	 * </pre>
	 * 
	 * The object is returned by method {@link #def} first.
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
		 * Set an alias. An exception is thrown if the alias is already in use
		 * for this parameter or an another one.
		 * 
		 * @param alias
		 *            an alternate name for the parameter
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
		 * Set a default value for the parameter. A parameter with a non-null
		 * default value can be omitted. A parameter without a default value is
		 * mandatory unless it is repeatable.
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
		
		/**
		 * Make the parameter as repeatable. A repeatable can have zero or more
		 * values. The values are returned in an array. See for example
		 * {@link Value#stringValues}.
		 * 
		 * @return the definition
		 */
		public Definition repeatable() {
			Value v = args().internalGet(name());
			if (v == null)
				throw new IllegalArgumentException("bug: " + name());
			v.setRepeatable(true);
			return this;
		}
		
	}
	
	/**
	 * A Value encapsulates a parameter value. It provides many methods to get a
	 * value or values of various types from a parameter. Attempting to get a
	 * parameter value which was not set results in an exception, unless a
	 * default value was defined.
	 * <p>
	 * Accessing the value of a parameter which contains one or more unresolved
	 * variables also results in an exception in most cases. The only methods
	 * which are always safe in this case are {@link #rawValue} and the two
	 * {@link Value#rawValues} methods. Another case not resulting in an
	 * exception is when the value contains a single unresolved variable without
	 * any surrounding text and a default value is available.
	 */
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
		
		/**
		 * Get the canonical name of the parameter.
		 * 
		 * @return a string
		 */
		protected String getName() {
			return canonical;
		}

		/**
		 * Set the value of the parameter.
		 * 
		 * @param value a string or null
		 */
		public void set(String value) {
			this.value = value;
		}

		/**
		 * Append a value to the current value of the parameter. Appending is
		 * done in a way that allows to access the element as an array element
		 * with for example {@link #stringValues}.
		 * 
		 * @param value
		 *            a string
		 */
		public void append(String value) {
			this.value = this.value == null ?
					new StringBuilder(value.length() + 2).append(leftQuote).append(value).append(rightQuote).toString() :
					new StringBuilder(this.value.length() + value.length() + 3).append(this.value).append(BLANK).append(leftQuote).append(value).append(rightQuote).toString();
		}
		
		/**
		 * Get the default value.
		 * 
		 * @return a string or null
		 */
		public String getDefault() {
			return defaultValue;
		}

		/**
		 * Set the default value.
		 * 
		 * @param value a string or null
		 */
		public void setDefault(String value) {
			this.defaultValue = value;
		}
		
		/**
		 * Test if a parameter is repeatable.
		 * 
		 * @return true if the parameter is repeatable
		 */
		public boolean isRepeatable() {
			return repeatable;
		}

		/**
		 * Set a parameter as repeatable.
		 * 
		 * @param b if true the parameter will be repeatable
		 */
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
		 * <p>
		 * Since this method is the basis for all other access methods, all of 
		 * them behave similarly with respect to unresolved variables.
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
					List<String> unresolved = new ArrayList<String>();
					Iterator<String> it = parts.iterator();
					while (it.hasNext()) {
						if (it.next() == null)
							unresolved.add(it.next());
					}
					throw new IllegalArgumentException(msg(U.U00106, getName(), Misc.join(", ", unresolved), result));
						
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
		 * If there is no value and no default value was set, an exception is
		 * thrown unless the parameter is repeatable, in which case the method
		 * returns an empty array, constraints permitting.
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
			List<String> values = 
				(isRepeatable() && value == null && defaultValue == null) ? 
				new ArrayList<String>() : getScanner().asValues(stringValue());
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
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables.
		 * 
		 * @return an int
		 */
		public int intValue() {
			return asInt(stringValue(), -1);
		}

		/**
		 * Split the value into a number of strings and convert them to
		 * integers. Throw an exception if a string cannot be converted or if
		 * the number of values does not agree with the constraints. Return the
		 * integers in an array.
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables.
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
		 * <p>
		 * See {@link #stringValue} for explanations about unresolved variables. 
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
		 * the number of values does not agree with the constraints. Return the
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
		 * the number of values does not agree with the constraints. Return the
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
		 * the number of values does not agree with the constraints. Return the
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
	
	private Map<String, Value> args;
	private Map<String, String> variables;
	private TextFile textFile; // use only one for duplicate detection to work
	private NameValueScanner argsScanner;
	private SymbolScanner symScanner;
	private Map<String, Integer> symCycleDetector;
	private FileIncluder includer;

	/**
	 * Constructor.
	 */
	public Args() {
		args = new HashMap<String, Args.Value>();
		def(COND);
		def(INCLUDE);
		variables = new HashMap<String, String>();
		textFile = new TextFile();
		argsScanner = new NameValueScanner(leftQuote, rightQuote, nameValueSeparator, escape, dollar);
		symScanner = new SymbolScanner(dollar);
		symCycleDetector = new HashMap<String, Integer>();
	}
	
	private NameValueScanner getScanner() {
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
	
	private FileIncluder getIncluder(String className) {
		FileIncluder inc = null;
		if (className != null) {
			try {
				inc = (FileIncluder) Class.forName(className).newInstance();
				inc.setTextFileReader(textFile);
			} catch (Exception e) {
				throw new IllegalArgumentException(msg(U.U00161, className), e);
			}
		} else {
			if (includer == null) {
				includer = new FileIncluder();
				includer.setTextFileReader(textFile);
			}
			inc = includer;
		}
		return inc;
	}
	
	/**
	 * Return an iterator over all parameter names. Parameter names are not
	 * returned in any predictable order.
	 * 
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<String> iterator() {
		return args.keySet().iterator();
	}

	/**
	 * Get the number of parameters.
	 * 
	 * @return the number of parameters defined
	 */
	public int size() {
		return args.size();
	}
	
	/**
	 * Convenience method for parsing parameters specified in an array. Elements
	 * are joined using a space separator into a single string and passed to
	 * {@link #parse(String)}.
	 * 
	 * @param args
	 *            an array of strings
	 */
	public void parse(String[] args) {
		parse(Misc.join(BLANK, args));
	}

	/**
	 * Parse parameter values specified as name-value pairs and isolated
	 * values. To parse a file, pass a string naming the file, like this:
	 * 
	 * <pre>
	 * <code>
	 * include = /some/where/config.txt
	 * </code>
	 * </pre>
	 * 
	 * @param input
	 *            a string containing a specification
	 */
	public void parse(String input) {
		parse(scan(input), null);
	}

	/**
	 * Parse a string containing a specification and collect parameters in
	 * sequence. Name-values are collected in arrays of length 2 and keywords in
	 * arrays of length 1.
	 * 
	 * @param input
	 *            a string
	 * @param collector
	 *            a list taking names and values or null
	 */
	public void parse(String input, List<String[]> collector) {
		parse(scan(input), collector);
	}

	/**
	 * Reset the state of the parser. This method must be called between calls
	 * to {@link #parse} unless parsing multiple inputs incrementally.
	 */
	public void reset() {
		for (Value v : args.values()) {
			v.set(null);
		}
		variables.clear();
		textFile.setDuplicateDetection(true); // resets duplicate detection
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
	private void parse(List<String[]> pairs, List<String[]> collector) {
		for (String[] pair : pairs) {
			switch (pair.length) {
			case 1:
				// lone value changed by resolution could be name-value, parse recursively 
				String resolved = resolve(pair[0]);
				if (resolved != pair[0])
					parse(scan(resolved), collector);
				else 
					put("", pair[0], collector);
				break;
			case 2:
				pair[0] = resolve(pair[0]);
				pair[1] = resolve(pair[1]);
				if (pair[0].equals(COND))
					parse(scan(parseIf(pair[1])), collector);
				else if (pair[0].equals(INCLUDE))
					parse(parseInclude(pair[1]), collector);
				else
					put(pair[0], pair[1], collector);
				break;
			default:
				throw new RuntimeException("bug: " + pair.length);
			}
		}
	}

	/**
	 * Define a parameter. The name cannot be null but it can be empty, in which
	 * case it will be possible to specify isolated values. When a
	 * non-repeatable parameter is repeated, the last value wins. An exception
	 * is thrown if there is already a parameter with the same name.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return a definition object which can be used to customize the parameter
	 */
	public Definition def(String name) {
		putValue(name, new Value(name));
		return new Definition(this, name);
	}
	
	/**
	 * Set the value of a parameter or a variable. If it is a parameter it must
	 * have been defined. If the name is prefixed with $, it names a variable,
	 * which can be defined on the fly if
	 * necessary. If the parameter is repeatable, the value will be appended,
	 * else it will be set and will replace a existing value. The value of
	 * variable cannot be changed ("first wins").
	 * <p>
	 * Keywords and isolated values without a name are put with this method by
	 * using the convention of giving an empty name. A keyword is the name of
	 * a parameter defined with a default value of false. 
 	 * 
	 * @param name
	 *            the name of the parameter or variable
	 * @param value
	 *            the value to set, replace or append
	 */
	public void put(String name, String value) {
		put(name, value, null);
	}
	
	/**
	 * Set the value of a parameter or a variable. If it is a parameter it must
	 * have been defined. If the name is prefixed with $, it names a variable,
	 * which can be defined on the fly if necessary. If the parameter is
	 * repeatable, the value will be appended, else it will be set and will
	 * replace a existing value. The value of variable cannot be changed
	 * ("first wins").
	 * <p>
	 * Keywords and isolated values without a name are put with this method by
	 * using the convention of giving an empty name. A keyword is the name of a
	 * parameter defined with a default value of false.
	 * <p>
	 * The collector is used by clients to keep track of the lexical sequence
	 * of parameters.
	 * 
	 * @param name
	 *            the name of the parameter or variable
	 * @param value
	 *            the value to set, replace or append
	 * @param collector
	 *            a list taking names and values or null
	 */
	public void put(String name, String value, List<String[]> collector) {
		Misc.nullIllegal(name, "name null");
		if (!putKeyword(name, value, collector)) {
			Value v = args.get(name);
			if (v == null) {
				if (isVariable(name))
					putVariable(name, value);
				else
					throw new IllegalArgumentException(msg(U.U00103, name.length() == 0 ? value : name));
			} else {
				if (v.isRepeatable())
					v.append(value);
				else
					v.set(value);
				if (collector != null)
					collector.add(new String[] {name, value});
			}
		}
	}
	
	private boolean putKeyword(String name, String value, List<String[]> collector) {
		boolean keyword = false;
		if (name.length() == 0) {
			Value v = args.get(value); // value, not name
			if (v != null && v.getDefault().equals(FALSE)) {
				v.set(TRUE);
				keyword = true;
				if (collector != null)
					collector.add(new String[] {value});
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
				String resolved = variables.get(symbol);
				if (resolved == null) {
					b.append(dollar);
					b.append(dollar);
					b.append(symbol);
				} else {
					if (!checkForCycle(symbol, level, cycleDetector))
						throw new IllegalArgumentException(msg(U.U00123, input, symbol));
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
	 * Return the value of a parameter as a string. This method is shorthand
	 * for
	 * 
	 * <pre>
	 * <code>
	 * getVal(name).stringValue()
	 * </code>
	 * </pre>
	 * 
	 * @param name
	 *            parameter name
	 * @return the string value
	 */
	public String get(String name) {
		return getVal(name).stringValue();
	}
	
	/**
	 * Return value of parameter as an array of strings. This method is
	 * shorthand for
	 * 
	 * <pre>
	 * <code>
	 * getVal(name).stringValues()
	 * </code>
	 * </pre>
	 * 
	 * @param name
	 *            parameter name
	 * @return an array of strings
	 */
	public String[] split(String name) {
		return getVal(name).stringValues();
	}

	/**
	 * Get a copy of all variables.
	 * 
	 * @return a name-value map
	 */
	public Map<String, String> getVariables() {
		Map<String, String> result = new HashMap<String, String>();
		result.putAll(variables);
		return result;
	}
	
	/**
	 * Set a variable. If a variable with the same name
	 * exists nothing is done ("first wins"). 
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
		return name.length() > 0 && name.charAt(0) == dollar;
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
		FileIncluder argsIncluder = null;
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
		
}
