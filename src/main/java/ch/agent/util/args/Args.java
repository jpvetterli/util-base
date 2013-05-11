package ch.agent.util.args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.agent.util.UtilMsg;
import ch.agent.util.UtilMsg.U;
import ch.agent.util.file.TextFile;

/**
 * Args provides support for parameter lists and parameter files. A parameter
 * list is a sequence of name-value pairs separated by white space, with names
 * and values separated by an equal sign (which can be surrounded by white
 * space). If a name or a value includes white space or an equal sign it must be
 * enclosed in square brackets. To include a closing bracket it must be escaped
 * with a backslash. Here is an example:
 * 
 * <pre><code>
 * foo = bar [qu ux]=[[what = ever\]] foo = [2nd val]
 * </code></pre>
 * 
 * In the example, 
 * parameter "foo" has two values: "bar" and "2nd val" while parameter "qu ux" has
 * one value, "[what = ever]".
 * <p>
 * When a name is repeated the previous value is lost unless the parameter was
 * defined as a list parameter, like "foo" in the example.
 * <p>
 * Parameters can be specified in files, which are themselves specified using
 * parameters, using the notation <code>file=file-spec</code>.
 * Files can reside in the file system or on the classpath. There can be
 * multiple files and files can be nested. In parameter files, lines starting
 * with a hash sign are skipped, even inside brackets. Since line terminators are
 * handled as white space, values can be continued on multiple lines by having
 * opening and closing square brackets on multiple lines. File parameters are
 * processed immediately in the order in which they appear. The file name can be
 * followed with a semi-colon and one or more mappings. Here is an example:
 * 
 * <pre><code>
 * file = [/home/someone/parms.txt; foo=bar quux=flix]
 * </code></pre>
 * <p>
 * When mappings are present, only parameters named in the mappings ("foo" and
 * "quux" in the example) will be considered. If they are found in the file the
 * corresponding values will be assigned to parameters using names bar and flix.
 * This trick is useful when extracting specific parameters from existing
 * configuration files where names are defined by someone else.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class Args {

	private abstract class Value {
		public Value() {
		}

		public abstract void set(String value);

		public String getValue(String name) {
			throw new UtilMsg(U.U00101, name).runtimeException();
		}

		public List<String> getValues(String name) {
			throw new UtilMsg(U.U00102, name).runtimeException();
		}
	}

	private class ScalarValue extends Value {
		private String value;

		public ScalarValue(String defaultValue) {
			super();
			this.value = defaultValue;
		}

		@Override
		public String getValue(String name) {
			if (value == null)
				throw new UtilMsg(U.U00105, name).runtimeException();
			return value;
		}

		@Override
		public void set(String value) {
			this.value = value;
		}
	}

	private class ListValue extends Value {
		private List<String> values;

		public ListValue() {
			super();
			this.values = new ArrayList<String>();
		}

		@Override
		public List<String> getValues(String name) {
			return values;
		}

		@Override
		public void set(String value) {
			if (value == null)
				this.values.clear();
			else
				this.values.add(value);
		}
	}

	private class ArgsFileVisitor implements TextFile.Visitor {

		private StringBuffer buffer;
		private String separator;
		
		public ArgsFileVisitor(String separator) {
			super();
			buffer = new StringBuffer();
			this.separator = separator;
		}

		@Override
		public boolean visit(int lineNr, String line) throws Exception {
			if (!line.startsWith(COMMENT)) {
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
	 * The default name of the file parameter is simply "file".
	 */
	public static final String FILE = "file";
	/**
	 * The default mapping separator is a semicolon surrounded
	 * by zero or more white space characters.
	 */
	public static final String MAPPING_SEPARATOR = "\\s*;\\s*";
	
	private static final String SEPARATOR = " ";
	private static final String COMMENT = "#";
	private String fileParameterName;
	private String mappingSeparator;
	private Map<String, Value> args;
	private TextFile textFile; // use only one for duplicate detection to work

	/**
	 * Construct a custom <code>Args</code> object. Nulls are valid arguments
	 * and will be replaced with default values.
	 * 
	 * @param name
	 *            the name of the file parameter, or null
	 * @param sep
	 *            a regular expression used as the mapping separator, or null
	 */
	public Args(String name, String sep) {
		this.fileParameterName = (name == null ? FILE : name);
		this.mappingSeparator = (sep == null ? MAPPING_SEPARATOR : sep);
		args = new HashMap<String, Args.Value>();
		textFile = new TextFile();
	}

	/**
	 * Construct an Args object using defaults.
	 * The default values for the name of the file parameter and the mapping 
	 * separator are taken from {@link #FILE} and {@link #MAPPING_SEPARATOR}.
	 */
	public Args() {
		this(null, null);
	}

	/**
	 * Convenience method to parse parameters specified in an array. Elements
	 * are joined using a space separator into a single string and passed to
	 * {@link #parse(String)}. An exception is thrown when parsing fails.
	 * 
	 * @param args
	 *            an array of strings
	 */
	public void parse(String[] args) {
		parse(join(SEPARATOR, args));
	}

	/**
	 * Parse parameters specified in a string. To parse a file, pass a string
	 * naming the file, like this:
	 * <pre><code>
	 * file = /some/where/config.txt
	 * </code><pre>
	 * An exception is thrown when parsing fails.
	 * 
	 * @param string
	 *            a string containing a list of name-value pairs
	 */
	public void parse(String string) {
		parse(new ArgsScanner().asPairs(string));
	}
	
	/**
	 * Parse <code>List</code> of name-value pairs.
	 * 
	 * @param pairs a list of arrays of length 2 (name and value)
	 */
	private void parse(List<String[]> pairs) {
		for (String[] pair : pairs) {
			if (pair[0].equals(fileParameterName))
				parse(parseFileAndMapping(pair[1]));
			else
				put(pair[0], pair[1]);
		}
	}

	/**
	 * Define a scalar parameter and its default value. If the default value is
	 * null the parameter will be interpreted as mandatory. An exception is
	 * thrown if there is already a parameter with the same name.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @param defaultValue
	 *            the default value or null if the parameter is mandatory
	 */
	public void define(String name, String defaultValue) {
		putValue(name, new ScalarValue(defaultValue));
	}

	/**
	 * Define a mandatory scalar parameter. An exception is thrown if there is
	 * already a parameter with the same name.
	 * 
	 * @param name
	 *            the name of the parameter
	 */
	public void define(String name) {
		define(name, null);
	}

	/**
	 * Define a list parameter. A list parameter can have zero or more values.
	 * An exception is thrown if there is already a parameter with the same
	 * name.
	 * 
	 * @param name
	 *            the name of the parameter
	 */
	public void defineList(String name) {
		putValue(name, new ListValue());
	}

	/**
	 * Put a value for the named parameter. An exception is thrown if there is
	 * no parameter with this name. If the parameter is a list parameter and the
	 * value is null, all values are cleared.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	public void put(String name, String value) {
		getValue(name).set(value);
	}

	/**
	 * Return the value for the named parameter. If the parameter was not
	 * specified, the default value is returned, but if the default value is
	 * null an exception is thrown. An exception is also thrown when attempting
	 * to get the value of a list parameter with this method.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return the value specified for the parameter
	 */
	public String get(String name) {
		return getValue(name).getValue(name);
	}
	
	/**
	 * Return the value of the named parameter converted to an integer. An
	 * exception is thrown if the conversion fails.
	 * 
	 * @param name
	 *            the name of the parameter
	 * 
	 * @return an integer
	 * @see #get(String)
	 */
	public int getInt(String name) {
		try {
			return Integer.parseInt(get(name));
		} catch (NumberFormatException e) {
			throw new UtilMsg(U.U00111, name, get(name)).runtimeException(e);
		}
	}
	
	/**
	 * Return the value of the named parameter converted to a boolean. An
	 * exception is thrown if the conversion fails. Valid boolean values are
	 * <code>true</code> and <code>false</code>. These values are
	 * case-insensitive.
	 * 
	 * @param name
	 *            the name of the parameter
	 * 
	 * @return a boolean
	 * @see #get(String)
	 */
	public boolean getBoolean(String name) {
		String value = get(name).toLowerCase();
		boolean result = value.equals("true");
		if (!result && ! value.equals("false"))
			throw new UtilMsg(U.U00112, name, get(name)).runtimeException();
		return result;
	}

	/**
	 * Return the list of values for the named parameter. An exception is thrown
	 * when attempting to get the value of a scalar parameter with this method.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return a list of values
	 */
	public List<String> getList(String name) {
		return getValue(name).getValues(name);
	}

	private Value getValue(String name) {
		Value v = args.get(name);
		if (v == null)
			throw new UtilMsg(U.U00105, name).runtimeException();
		return v;
	}

	private void putValue(String name, Value value) {
		Value v = args.get(name);
		if (v != null)
			throw new UtilMsg(U.U00104, name).runtimeException();
		args.put(name, value);
	}

	private String join(String separator, String[] string) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < string.length; i++) {
			b = b.append(string[i]);
			b = b.append(separator);
		}
		if (b.length() > separator.length())
			b.setLength(b.length() - separator.length());
		return b.toString();
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
	 * 
	 * @param fileSpec
	 *            a file name possibly followed by mappings
	 * @return a list of arrays of length 2 (name and value)
	 */
	private List<String[]> parseFileAndMapping(String fileSpec) {
		String[] fm = fileSpec.split(mappingSeparator, 2);
		if (fm.length > 1)
			return parseFile(fm[0], fm[1]);
		else
			return parseFile(fm[0]);
	}
	
	private List<String[]> parseFile(String fileName) {
		ArgsFileVisitor visitor = new ArgsFileVisitor(SEPARATOR);
		textFile.read(fileName, visitor);
		return new ArgsScanner().asPairs(visitor.getContent());
	}
	
	private List<String[]> parseFile(String fileName, String mappings) {
		List<String[]> pairs = parseFile(fileName);
		Map<String, String> map = asMap(new ArgsScanner().asPairs(mappings));
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

}
