package ch.agent.util.args;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.agent.util.file.TextFile;

/**
 * The file includer reads parameters from a file in the file system or on the
 * classpath. If an external {@link TextFile} is used for accessing files,
 * inclusion cycles are detected. Lines with a # as the first non-whitespace
 * characters are skipped as comments. A <em>simple</em> mode is provided, which
 * skips all lines not containing a = character. The {@link Args}
 * <em>include</em> operator can be instructed to use a custom file includer,
 * which needs to extend this class.
 */
public class FileIncluder {

	private static final String SEPARATOR = " ";
	private static final String COMMENT = "#";
	private static final String EQUAL = "=";

	private class ArgsFileVisitor implements TextFile.Visitor {

		private StringBuilder buffer;
		private boolean skipIfNoEqual;

		public ArgsFileVisitor(boolean skipIfNoEqual) {
			super();
			this.skipIfNoEqual = skipIfNoEqual;
			buffer = new StringBuilder();
		}

		@Override
		public boolean visit(int lineNr, String line) throws Exception {
			if (!line.trim().startsWith(COMMENT)) {
				if (!skipIfNoEqual || line.indexOf(EQUAL) >= 0) {
					buffer.append(line);
					buffer.append(SEPARATOR);
				}
			}
			return false;
		}

		public String getContent() {
			return buffer.toString();
		}

	}

	private TextFile textFile; // use only one for duplicate detection to work

	/**
	 * Constructor.
	 */
	public FileIncluder() {
	}

	/**
	 * Set the text file reader to use. Using the same reader when there are
	 * recursive includes makes it possible to detect cycles. If this method is
	 * not used or if a null reader is passed, a new reader is created each time
	 * one is needed.
	 * 
	 * @param textFile
	 *            a text file reader
	 */
	public void setTextFileReader(TextFile textFile) {
		this.textFile = textFile;
	}

	/**
	 * Return the content of a file as a list of name-value pairs or isolated
	 * values. The second parameter is a map where keys are the names to
	 * include. If the corresponding value is an non-empty string it is used to
	 * translate the name. Only names present in the map will be returned.
	 * Mapping can be disabled by passing a null map.
	 * <p>
	 * Passing the keyword <em>simple</em> as additional configuration enables a
	 * mode where all lines not containing an equal sign are skipped.
	 * 
	 * @param scanner
	 *            the scanner to use
	 * @param fileName
	 *            the file to include
	 * @param names
	 *            the names to include, with optional translation
	 * @param configuration
	 *            additional configuration
	 * @return a list of String arrays of length 1 or 2
	 */
	public List<String[]> include(NameValueScanner scanner, String fileName, Map<String, String> names, String configuration) {
		boolean skipIfNotEqual = configuration == null ? true : configuration.equals("simple");
		List<String[]> scanned = include(scanner, fileName, skipIfNotEqual);
		if (names != null) {
			Iterator<String[]> it = scanned.iterator();
			while (it.hasNext()) {
				String[] pair = it.next();
				String mapping = names.get(pair[0]);
				if (mapping == null)
					it.remove(); // remove if no key (or value null)
				else if (mapping.length() > 0)
					pair[0] = mapping; // rename if empty
			}
		}
		return scanned;
	}

	/**
	 * Return the content of a file as a list of name-value pairs or
	 * isolated values.
	 * 
	 * @param scanner
	 *            the scanner to use
	 * @param fileName
	 *            the file to include
	 * @return a list of String arrays of length 1 or 2
	 * @throws IllegalArgumentException
	 *             on failure to read the included file
	 */
	public List<String[]> include(NameValueScanner scanner, String fileName) {
		return include(scanner, fileName, false);
	}

	/**
	 * Return the content of a file as a list of name-value pairs or
	 * isolated values.
	 * 
	 * @param scanner
	 *            the scanner to use
	 * @param fileName
	 *            the file to include
	 * @param skipIfNotEqual
	 *            if true skip lines not containing an equal sign
	 * 
	 * @return a list of String arrays of length 1 or 2
	 * @throws IllegalArgumentException
	 *             on failure to read the included file
	 */
	protected List<String[]> include(NameValueScanner scanner, String fileName, boolean skipIfNotEqual) {
		ArgsFileVisitor visitor = new ArgsFileVisitor(skipIfNotEqual);
		if (textFile == null)
			textFile = new TextFile();
		textFile.read(fileName, visitor);
		return scanner.asValuesAndPairs(visitor.getContent());
	}

}
