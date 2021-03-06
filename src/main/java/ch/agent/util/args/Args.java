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
 * Args defines parameters and parses strings and configuration files. It
 * provides a simple language and a small set of built-in operators. Here is a
 * short example:
 * 
 * <pre>
 * <code>
 * $greeting = hello $subject = world
 * config = [
 *   greet=$$greeting
 * ]
 * exec = [hello.say=[$$subject]]
 * </code>
 * </pre>
 * 
 * Put the example in file <code>hello.config</code> and run an hypothetical
 * application, coded with Args, from the command line (\ indicates continuation
 * on next line):
 * 
 * <pre>
 * <code>
 * prompt&gt; java -jar example.jar $subject= "zäme [you all]" \
 *      $greeting = Hoi include=hello.config
 * Hoi zäme
 * Hoi you all
 * </code>
 * </pre>
 * 
 * Args is named after the parameter of the main method of Java programs,
 * usually written like this:
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
 * {@link #def(String)} method; the definition can be customized using the
 * {@link Definition} object returned.
 * 
 * <li><b>Setting</b> values or <b>parsing</b> values from an input. Parameter
 * values can be set using a put method, like {@link #put(String, String)}, or
 * parsed from text using {@link #parse(String)}. Such text is written in a
 * simple but flexible specification language, described shortly.
 * 
 * <li><b>Getting</b> values of parameters. All values are basically strings
 * encapsulated in a {@link Value} object. Methods are available to extract
 * values of various types as either scalars or arrays.
 * 
 * </ol>
 * 
 * <h3>The specification language</h3>
 * 
 * A specification consists of a series of <em>name-value pairs</em> and
 * isolated values and keywords. In a name-value pair, the name identifies an
 * existing parameter definition. An isolated value is the value of a nameless
 * parameter (a parameter with an empty name). Some isolated values are known as
 * <em>keywords</em>; a keyword corresponds to a parameter defined with a
 * default value of "false". The occurrence of the parameter name as an isolated
 * value sets the value of the parameter to "true".
 * <p>
 * There are two different approaches for parameters to have multiple values.
 * The first approach is to define the parameter as <em>repeatable</em>. In this
 * case, multiple name-value pairs with the same name can be specified. The
 * second approach is to use a standard parameter and to specify all values
 * between quotes (brackets) and separated by white space. In both cases, all
 * values are accessed with a single invocation of an array getter. The two
 * approaches cannot be mixed. If a parameter is defined as repeatable,
 * specifying multiple values between quotes and separated by white space will
 * be accessed as a single value with blanks inside.
 * <p>
 * A parameter can only be used if it has been defined, but sometimes it is
 * useful to define parameters on the fly. This is possible with
 * <em>variables</em>. Variables have a name with a $ sign in front. They are
 * referenced by prefixing them with $$. When a variable reference is seen in
 * the text, it is replaced by the value of the variable if available, else it
 * is left as an unresolved variable. When accessing a parameter value which
 * contains unresolved variables, an error occurs, unless using a <em>raw</em>
 * getter.
 * <p>
 * One difference between parameters and variables is their behavior with
 * respect to repeated values. With a (non-repeatable) parameter, the last value
 * wins. For variables, the first wins. This allows to override default values
 * of variables in configuration files (see the <em>include</em> operator,
 * below) with values specified before the file is included. It is still
 * possible to modify a variable if necessary, using the reset built-in operator
 * (explained below).
 * <p>
 * The language syntax supports names and values with arbitrary content using a
 * <em>nested</em> quoting notation. A value with blanks can be passed as
 * 
 * <pre>
 * <code>
 *  name=[a value with blanks]
 * </code>
 * </pre>
 * 
 * When accessed as a scalar, the value will be the string
 * "a value with blanks". When accessed as an array, the value will be an array
 * with elements "a", "value", "with", and "blanks". Nested quotes can be used
 * when list elements contain blanks. The elements "a", "[list", "of]", and
 * "four elements" can be extracted from (notice the two escapes):
 * 
 * <pre>
 * <code>
 * list=[a \[list of\] [four elements]]
 * </code>
 * </pre>
 * 
 * Nesting can be used to powerful effect. Suppose a piece of software uses Args
 * to parse its parameters and suppose this software has multiple components
 * which also use Args for their parameters. A top-level component can define
 * its parameters without knowing any detail about the other components. It only
 * needs to define one parameter for each such component and pass along an
 * opaque value.
 * <p>
 * Quote parsing and variable resolution are distinct processing steps in Args.
 * Quote parsing is done one nesting level at a time, but variable resolution is
 * done on the full input, irrespective of quotes. This separation is useful in
 * practice but there are some caveats, like the fact that the $ and $$ prefixes
 * of variables cannot be escaped with \, as mentioned below. Variables can
 * remain unresolved until accessed by a non-raw getter. Raw getters are used to
 * get values passed to the next parsing level. Non-raw getters are used when
 * values are actually required.
 * <p>
 * When finding an isolated value, the parser resolves it for variables. If
 * resolution effectively changes the value, it is parsed again recursively.
 * This allows to insert pieces of specification code via variables. This
 * behavior does not apply to name-value pairs which are resolved but not
 * recursively parsed.
 * <p>
 * The way quoting and variable resolution interact allows to simulate
 * subroutines in the specification language, which is quite useful when a
 * specification is large.
 * <p>
 * When using nested Args, applications can pass variables from one level to the
 * next using {@link #getVariables()} and {@link #putVariable(String, String)}.
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
 * <p>
 * These can be overridden by passing a string of 5 characters as a System
 * property named <code>ArgsMetaCharacters</code> which will be interpreted in
 * the above sequence. The escape character is used to suppress the normal
 * behavior of quotes, the name-value separator, and the escape itself. It
 * cannot be used to escape the variable prefix.
 * 
 * <h3>Built-in operators</h3>
 * 
 * Args provides four built-in operators, <em>reset</em> which resets the value
 * of parameters or variables to null (if a parameter is repeatable, all its
 * values are removed), <em>condition</em> for conditional parsing,
 * <em>include</em> for file inclusion, and <em>dump</em> as a debugging aid to
 * print values of parameters and variables to standard error. Syntactically,
 * these operators are parameter names.
 * <p>
 * The syntax of the reset operator is
 * 
 * <pre>
 * <code>
 * reset = [name ...]
 * </code>
 * </pre>
 * 
 * Names are either parameter names or variable names (starting with $). The
 * dump operator has the same syntax as the reset operator.
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
 * value of "else" is used. An undefined variable is considered empty here,
 * instead of throwing an exception. The "else" part is the only one which can
 * be omitted.
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
 * must be located either in the file system or on the classpath. It is possible
 * to include multiple files and includes can be nested.
 * <p>
 * Only the file name (a nameless parameter) is mandatory. In this case, the
 * quoting brackets can usually be omitted. The "names" parameter takes zero or
 * more name-value pairs or isolated values. An isolated value specifies the
 * name of a parameter to extract from the file. The name in a name-value pair
 * also specifies the name of a parameter to extract while the value specifies a
 * translation for that name. This allows to extract parameters from
 * configuration files belonging to other applications and use them
 * automatically under a new name. The "extractor" parameter allows to specify a
 * class to use for extraction. The class must extend {@link FileIncluder},
 * provided with Args. If necessary, a parameter can be passed to that class
 * using "extractor-parameters".
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
	 * null, default meta characters are returned.
	 * 
	 * @param metaChars
	 *            a string of length 5
	 * @return an array of length 5
	 * @throws IllegalArgumentException
	 *             unless there are 5 meta characters, all different
	 */
	public static char[] validateMetaCharacters(String metaChars) {
		if (metaChars == null)
			return new char[] { '[', ']', '=', '\\', '$' };
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
	 * Validate five meta characters. The five characters must be different.
	 * 
	 * @param leftQuote
	 *            the left quote
	 * @param rightQuote
	 *            the right quote
	 * @param nameValueSeparator
	 *            the name-value separator
	 * @param escape
	 *            the escape
	 * @param dollar
	 *            the variable prefix
	 * @throws IllegalArgumentException
	 *             if any two meta characters are equal
	 */
	public static void validateMetaCharacters(char leftQuote, char rightQuote, char nameValueSeparator, char escape, char dollar) {
		if (leftQuote == rightQuote || leftQuote == nameValueSeparator || leftQuote == escape || leftQuote == dollar || rightQuote == nameValueSeparator || rightQuote == escape || rightQuote == dollar || nameValueSeparator == escape || nameValueSeparator == dollar || escape == dollar)
			throw new IllegalArgumentException(msg(U.U00163, leftQuote, rightQuote, nameValueSeparator, escape, dollar));
	}

	private final static char leftQuote;
	private final static char rightQuote;
	private final static char nameValueSeparator;
	private final static char escape;
	private final static char dollar;

	private final static String RESET = "reset";
	private final static String DUMP = "dump";
	private final static String DUMP_FORMAT = "[DUMP] %s : %s";
	private final static String DUMP_FORMAT_MISSING = "[MISS] %s";
	
	private final static String COND = "condition";
	private final static String COND_IF = "if";
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
	 * The object is returned by method {@link Args#def(String)}.
	 */
	public static class Definition {
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
		 *            a non-null alternate name for the parameter
		 * @return this definition
		 * @throws IllegalArgumentException
		 *             as described in the comment
		 */
		public Definition aka(String alias) {
			Misc.nullIllegal(alias, "alias null");
			Value v = args().internalGet(name());
			if (v == null)
				throw new RuntimeException("bug: " + name());
			if (args().internalGet(alias) != null)
				throw new IllegalArgumentException(msg(U.U00104, alias));
			args().put(alias, v);
			return this;
		}

		/**
		 * Set a default value for the parameter. A parameter with a non-null
		 * default value can be omitted. A parameter without a default value is
		 * mandatory unless it is repeatable and accessed with an array getter.
		 * 
		 * @param value
		 *            the default value of the parameter
		 * @return this definition
		 */
		public Definition init(String value) {
			Value v = args().internalGet(name());
			if (v == null)
				throw new RuntimeException("bug: " + name());
			v.setDefault(value);
			return this;
		}

		/**
		 * Make the parameter repeatable. A repeatable parameters can have zero
		 * or more values. The values are returned by array getters. See for
		 * example {@link Value#stringValues()}.
		 * 
		 * @return this definition
		 */
		public Definition repeatable() {
			Value v = args().internalGet(name());
			if (v == null)
				throw new RuntimeException("bug: " + name());
			v.setRepeatable(true);
			return this;
		}

	}

	/**
	 * A Value encapsulates a parameter value. It provides many methods to get a
	 * scalar value or an array of values of various types from a parameter.
	 * Attempting to get a parameter value which was not set results in an
	 * exception, unless a default value was defined.
	 * <p>
	 * Accessing the value of a parameter which contains one or more unresolved
	 * variables also results in an exception in most cases. The only methods
	 * which are always safe in this case are the raw getters
	 * {@link #rawValue()}, {@link #rawValues()} and
	 * {@link #rawValues(int, int)}. Another case not resulting in an exception
	 * is when the value contains a single unresolved variable without any
	 * surrounding text and a default value is available.
	 */
	public class Value {
		private String canonical;
		private String value;
		private String defaultValue;
		private boolean repeatable;

		/**
		 * Constructor.
		 * 
		 * @param canonical
		 *            canonical name of parameter, not null
		 */
		public Value(String canonical) {
			Misc.nullIllegal(canonical, "canonical null");
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
		 * @param value
		 *            a string or null
		 */
		public void set(String value) {
			this.value = value;
		}

		/**
		 * Append a value to the current value of the parameter. Appending is
		 * done in a way that allows to access the element as an array element,
		 * using for example {@link #stringValues()}.
		 * 
		 * @param value
		 *            a string
		 */
		public void append(String value) {
			this.value = this.value == null ? new StringBuilder(value.length() + 2).append(leftQuote).append(value).append(rightQuote).toString() : new StringBuilder(this.value.length() + value.length() + 3).append(this.value).append(BLANK).append(leftQuote).append(value).append(rightQuote).toString();
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
		 * @param value
		 *            a string or null
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
		 * @param b
		 *            if true the parameter will be repeatable
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
		 * Since this method is the basis for all other non-raw access methods,
		 * all of them behave similarly with respect to unresolved variables.
		 * 
		 * @return a string
		 * @throws IllegalArgumentException
		 *             as described in the comment
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @param min
		 *            minimal number of strings (no limit if negative)
		 * @param max
		 *            maximal number of strings (no limit if negative)
		 * @return an array of strings
		 * @throws IllegalArgumentException
		 *             as described in the comment
		 */
		public String[] stringValues(int min, int max) {
			List<String> values = (isRepeatable() && value == null && defaultValue == null) ? new ArrayList<String>() : getScanner().asValues(stringValue());
			checkSize(values.size(), min, max);
			return values.toArray(new String[values.size()]);
		}

		/**
		 * Split value into a number of strings. Splitting is done on white
		 * space and meta characters.
		 * <p>
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return an array of strings
		 * @throws IllegalArgumentException
		 *             as described in the comment
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
		 * @throws IllegalArgumentException
		 *             if there is no value and no default was set
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
		 * specified by two parameters, or if the value cannot be scanned
		 * successfully. Negative parameters are ignored. This method does not
		 * check for unresolved variables.
		 * 
		 * @param min
		 *            minimal number of strings (no limit if negative)
		 * @param max
		 *            maximal number of strings (no limit if negative)
		 * @return an array of strings
		 * @throws IllegalArgumentException
		 *             as described in the comment
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
		 * @throws IllegalArgumentException
		 *             on invalid input
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
					throw new IllegalArgumentException(msg(U.U00111, getName(), size, max));
			}
		}

		/**
		 * Return the value as a int. Throw an exception if the value cannot be
		 * converted.
		 * <p>
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return an int
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @param min
		 *            the minimum number of integers (no limit if negative)
		 * @param max
		 *            the maximum number of integers (no limit if negative)
		 * @return an int array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return an int array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
		 */
		public int[] intValues() {
			return intValues(-1, -1);
		}

		/**
		 * Return the value as a boolean. Throw an exception if the value cannot
		 * be converted. The only strings which can be converted to boolean are
		 * "true" and "false".
		 * <p>
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return a boolean
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @param min
		 *            the minimum number of booleans (no limit if negative)
		 * @param max
		 *            the maximum number of booleans (no limit if negative)
		 * @return a boolean array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return a boolean array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
		 */
		public boolean[] booleanValues() {
			return booleanValues(-1, -1);
		}

		/**
		 * Return the value as a double. Throw an exception if the value cannot
		 * be converted.
		 * <p>
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return a double
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
		 */
		public double doubleValue() {
			return asDouble(stringValue(), -1);
		}

		/**
		 * Split the value into a number of strings and convert them to doubles.
		 * Throw an exception if a string cannot be converted or if the number
		 * of values does not agree with the constraints. Return the doubles in
		 * an array.
		 * <p>
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @param min
		 *            the minimum number of doubles (no limit if negative)
		 * @param max
		 *            the maximum number of doubles (no limit if negative)
		 * @return a double array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @return a double array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
		 */
		public double[] doubleValues() {
			return doubleValues(-1, -1);
		}

		/**
		 * Return the value as an Enum constant. Throw an exception if the value
		 * cannot be converted.
		 * <p>
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @param <T>
		 *            the type of the enum value
		 * @param enumClass
		 *            the enum type class
		 * @return an Enum
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
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
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		 * See {@link #stringValue()} for explanations about unresolved
		 * variables.
		 * 
		 * @param <T>
		 *            the type of the enum values
		 * @param enumClass
		 *            the class object of the enum type
		 * @return an enum double array
		 * @throws IllegalArgumentException
		 *             if there is any problem with the value
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
		def(DUMP);
		def(RESET);
		def(COND);
		def(INCLUDE);
		variables = new HashMap<String, String>();
		textFile = new TextFile();
		argsScanner = new NameValueScanner(leftQuote, rightQuote, nameValueSeparator, escape);
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
		a.def(COND_IF);
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
	 * @throws IllegalArgumentException
	 *             when parsing fails
	 */
	public void parse(String[] args) {
		parse(Misc.join(BLANK, args));
	}

	/**
	 * Parse parameter values specified as name-value pairs and isolated values.
	 * To parse a file, pass a string naming the file, like this:
	 * 
	 * <pre>
	 * <code>
	 * include = /some/where/config.txt
	 * </code>
	 * </pre>
	 * 
	 * @param input
	 *            a string containing a specification
	 * @throws IllegalArgumentException
	 *             when parsing fails
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
	 * @throws IllegalArgumentException
	 *             when parsing fails
	 */
	public void parse(String input, List<String[]> collector) {
		parse(scan(input), collector);
	}

	/**
	 * Reset the state of the parser. This method must be called between calls
	 * to {@link #parse(String)} unless parsing multiple inputs incrementally.
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
				// lone value changed by resolution could be name-value, parse
				// recursively
				String resolved = resolve(pair[0]);
				if (resolved != pair[0])
					parse(scan(resolved), collector);
				else
					put("", pair[0], collector);
				break;
			case 2:
				pair[0] = resolve(pair[0]);
				if (pair[0].equals(COND)) {
					// leave parameters unresolved because of possible 'reset'
					parse(scan(parseIf(pair[1])), collector);
				} else {
					pair[1] = resolve(pair[1]);
					if (pair[0].equals(RESET))
						reset(getScanner().asValues(pair[1]));
					else if (pair[0].equals(DUMP))
						dump(getScanner().asValues(pair[1]));
					else if (pair[0].equals(INCLUDE))
						parse(parseInclude(pair[1]), collector);
					else
						put(pair[0], pair[1], collector);
				}
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
	 * @throws IllegalArgumentException
	 *             as described in the comment
	 */
	public Definition def(String name) {
		putValue(name, new Value(name));
		return new Definition(this, name);
	}

	/**
	 * Set the value of a parameter or a variable. If it is a parameter it must
	 * have been defined. If the name is prefixed with $, it names a variable,
	 * which can be defined on the fly if necessary. If the parameter is
	 * repeatable, the value will be appended, else it will be set and will
	 * replace an existing value. The value of a variable cannot be changed
	 * ("first wins").
	 * <p>
	 * Keywords and isolated values without a name are put with this method by
	 * using the convention of giving an empty name. A keyword is the name of a
	 * parameter defined with a default value of false.
	 * 
	 * @param name
	 *            the non-null name of the parameter or variable
	 * @param value
	 *            the non-null value to set, replace or append
	 * @throws IllegalArgumentException
	 *             when no such parameter, unless it is a variable
	 */
	public void put(String name, String value) {
		put(name, value, null);
	}

	/**
	 * Set the value of a parameter or a variable. If it is a parameter it must
	 * have been defined. If the name is prefixed with $, it names a variable,
	 * which can be defined on the fly if necessary. If the parameter is
	 * repeatable, the value will be appended, else it will be set and will
	 * replace a existing value. The value of a variable cannot be changed
	 * ("first wins").
	 * <p>
	 * Keywords and isolated values without a name are put with this method by
	 * using the convention of giving an empty name. A keyword is the name of a
	 * parameter defined with a default value of false.
	 * <p>
	 * The collector is used by clients to keep track of the lexical sequence of
	 * parameters.
	 * 
	 * @param name
	 *            the non-null name of the parameter or variable
	 * @param value
	 *            the non-null value to set, replace or append
	 * @param collector
	 *            a list taking names and values or null
	 * @throws IllegalArgumentException
	 *             when no such parameter, unless it is a variable
	 */
	public void put(String name, String value, List<String[]> collector) {
		Misc.nullIllegal(name, "name null");
		Misc.nullIllegal(value, "name value");
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
					collector.add(new String[] { name, value });
			}
		}
	}

	private boolean putKeyword(String name, String value, List<String[]> collector) {
		boolean keyword = false;
		if (name.length() == 0) {
			if (value.length() == 0)
				throw new IllegalArgumentException(msg(U.U00101));
			Value v = args.get(value); // value, not name
			if (v != null) {
				if (v.getDefault() != null && v.getDefault().equals(FALSE)) {
					v.set(TRUE);
					keyword = true;
					if (collector != null)
						collector.add(new String[] { value });
				} else {
					throw new IllegalArgumentException(msg(U.U00102, value));
				}
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
		StringBuilder b = new StringBuilder();
		Iterator<String> it = symScanner.split(input).iterator();
		while (it.hasNext()) {
			String s = it.next();
			if (s == null) {
				// null is a stand-in for $$
				assert it.hasNext();
				String symbol = it.next();
				String resolved = variables.get(symbol);
				if (resolved == null) {
					// rebuild the symbol using triple dollar notation (just in case)
					b.append(dollar);
					b.append(dollar);
					b.append(dollar);
					b.append(symbol);
					b.append(dollar);
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
	 * Reset a number of parameters and variables. Variable names must be
	 * prefixed with a $. Undefined parameters and variables are ignored.
	 * 
	 * @param names
	 *            a list of names of parameters and variables
	 */
	public void reset(List<String> names) {
		Misc.nullIllegal(names, "names null");
		for (String name : names) {
			if (isVariable(name)) {
				String symbol = name.substring(1);
				if (variables.containsKey(symbol))
					variables.remove(symbol);
			} else {
				Value v = args.get(name);
				if (v != null)
					v.set(null);
			}
		}
	}
	
	/**
	 * Print the value of a number of parameters and variables. Variable names must be
	 * prefixed with a $. 
	 * 
	 * @param names
	 *            a list of names of parameters and variables
	 */
	public void dump(List<String> names) {
		Misc.nullIllegal(names, "names null");
		for (String name : names) {
			if (isVariable(name)) {
				String symbol = name.substring(1);
				if (variables.containsKey(symbol))
					System.err.println(String.format(DUMP_FORMAT, name, variables.get(symbol)));
				else 
					System.err.println(String.format(DUMP_FORMAT_MISSING, name));
			} else {
				Value v = args.get(name);
				if (v == null)
					System.err.println(String.format(DUMP_FORMAT_MISSING, name));
				else {
					if (v.isRepeatable()) {
						for (String value : v.rawValues()) {
							System.err.println(String.format(DUMP_FORMAT, name, value));
						}
					} else {
						System.err.println(String.format(DUMP_FORMAT, name, v.rawValue()));
					}
				}
			}
		}
	}

	/**
	 * Return the value object for the parameter specified. An exception is
	 * thrown if the name is unknown. For a nameless parameter, pass an empty
	 * name.
	 * 
	 * @param name
	 *            the name of the parameter, not null
	 * @return the value object
	 * @throws IllegalArgumentException
	 *             when no such parameter
	 */
	public Value getVal(String name) {
		Misc.nullIllegal(name, "name null");
		Value v = args.get(name);
		if (v == null)
			throw new IllegalArgumentException(msg(U.U00103, name));
		return v;
	}

	/**
	 * Return the value of a parameter as a string. This method is shorthand for
	 * 
	 * <pre>
	 * <code>
	 * getVal(name).stringValue()
	 * </code>
	 * </pre>
	 * 
	 * @param name
	 *            non-null parameter name
	 * @return the string value
	 * @throws IllegalArgumentException
	 *             when no such parameter or when there is a problem with the
	 *             value
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
	 * @throws IllegalArgumentException
	 *             when no such parameter or when there is a problem with the
	 *             value
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
	 * Set a variable. If a variable with the same name exists, nothing is done
	 * ("first wins"). The name is verified by
	 * {@link SymbolScanner#verify(String)}.
	 * 
	 * @param name
	 *            the name of the variable
	 * @param value
	 *            the value of the variable
	 * @return true if the variable was added or false if a variable with the
	 *         same name already exists
	 * @throws IllegalArgumentException
	 *             if the name fails verification
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
	
	/**
	 * Import all variables from an another <code>Args</code> into this one. The
	 * "first wins" rule applies.
	 * 
	 * @param args
	 *            an <code>Args</code> or null
	 */
	public void putVariables(Args args) {
		if (args != null) {
			for (Map.Entry<String, String> entry : args.getVariables().entrySet()) {
				if (variables.get(entry.getKey()) == null) {
					variables.put(entry.getKey(), entry.getValue());
				}
			}
		}
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
		Misc.nullIllegal(name, "name null");
		Misc.nullIllegal(value, "value null");
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
		// parseIf text is not resolved, so resolve 'if' parameter now
		String ifValue = resolve(a.getVal(COND_IF).rawValue());
		// undefined variable would be $$FOO here, must "get" it
		a.put(COND_IF, ifValue);
		try {
			ifValue = a.get(COND_IF);
		} catch (IllegalArgumentException e) {
			ifValue = null;
		}
		String thenValue = a.getVal(COND_THEN).rawValue();
		String elseValue = a.getVal(COND_ELSE).rawValue();
		if (!Misc.isEmpty(ifValue))
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
			map.put(pair[0], pair.length == 2 ? pair[1] : "");
		}
		return map;
	}

}
