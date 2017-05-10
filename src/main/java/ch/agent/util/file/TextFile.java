package ch.agent.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ch.agent.util.STRINGS;
import ch.agent.util.STRINGS.U;

/**
 * Support for reading and writing text files.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class TextFile {

	private Charset charset;
	private Set<String> duplicates;
	
	/**
	 * A visitor is used to process lines read from a text file.
	 */
	public interface Visitor {
		
		/**
		 * Take one line of text. Returns true to signal intention of skipping 
		 * all remaining lines. Can throw a checked exception.
		 * 
		 * @param lineNr
		 *            the line number
		 * @param line
		 *            the text, with line separator already removed
		 * @return true to request skipping remaining lines
		 * @throws Exception checked exception from the implementation
		 */
		boolean visit(int lineNr, String line) throws Exception;
	}
	
	private class Input {
		private String name;
		private InputStream stream;

		public Input(String name, InputStream inputStream) {
			super();
			this.name = name;
			this.stream = inputStream;
		}

		public String getName() {
			return name;
		}

		public InputStream getStream() {
			return stream;
		}
	}
	
	private class Output {
		private String name;
		private OutputStream stream;

		public Output(String name, OutputStream outputStream) {
			super();
			this.name = name;
			this.stream = outputStream;
		}

		public String getName() {
			return name;
		}

		public OutputStream getStream() {
			return stream;
		}
	}

	private class SimpleVisitor implements Visitor {

		private List<String> lines;
		
		public SimpleVisitor() {
			super();
			lines = new ArrayList<String>();
		}

		@Override
		public boolean visit(int lineNr, String line) throws Exception {
			lines.add(line);
			return false;
		}
		
		public List<String> getLines() {
			return lines;
		}
	}

	
	/**
	 * Construct a <code>TextFile</code> using a specific character set.
	 * 
	 * @param charset the character set
	 */
	public TextFile(Charset charset) {
		super();
		this.charset = charset;
		setDuplicateDetection(true);
	}
	
	/**
	 * Construct a <code>TextFile</code> using the default character set.
	 */
	public TextFile() {
		this(Charset.defaultCharset());
	}

	/**
	 * Set duplicate detection mode. When the mode is active, an exception is
	 * thrown when attempting to read a file more than once. This allows to
	 * detect cycles in some applications. Setting the mode on or off resets the
	 * detector to its initial state.
	 * <p>
	 * A new <code>TextFile</code> object has duplicate detection mode on.
	 * 
	 * @param on
	 *            if true duplicates will be detected
	 */
	public void setDuplicateDetection(boolean on) {
		if (on) {
			if (duplicates == null)
				duplicates = new HashSet<String>(); 
			else
				duplicates.clear();
		} else
			duplicates = null;
	}
	
	/**
	 * Read a series of lines from a file. Lines are passed to the caller via a
	 * callback mechanism. The file can reside in the file system or as a
	 * resource on the class path. An <code>IOException</code> is thrown if the
	 * file cannot be read or if the visitor throws an exception.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param visitor
	 *            a visitor taking lines of text
	 * @throws IOException
	 */
	public void read(String fileName, Visitor visitor) throws IOException {
		int lineNr = 0;
		String line = null;
		Input fh = openInput(fileName);
		try {
			BufferedReader r = new BufferedReader(
					new InputStreamReader(fh.getStream(), charset));
			while (true) {
				lineNr++;
				line = r.readLine();
				if (line == null) {
					lineNr--;
					break;
				}
				if (visitor.visit(lineNr, line))
					break;
			}
			r.close();
		} catch (Exception e) {
			if (lineNr > 0)
				throw new IOException(STRINGS.msg(U.U00202, fh.getName(), lineNr), e);
			else
				throw new IOException(STRINGS.msg(U.U00201, fh.getName()), e);
		} finally {
			fh.stream.close();
		}
	}

	/**
	 * Read a series of lines from a file. Lines are returned to the caller as a
	 * list containing of lines of text from the file. Line terminators are
	 * removed. The file can reside in the file system or as a resource on the
	 * class path. An <code>IOException</code> is thrown if the file cannot be
	 * read.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @return all lines of text from the file as a list
	 * @throws IOException
	 */
	public List<String> read(String fileName) throws IOException {
		SimpleVisitor v = new SimpleVisitor();
		read(fileName, v);
		return v.getLines();
	}
	
	/**
	 * Write a series of lines to a file. The file is created if it does not
	 * exist, as is the file's directory (but not the directory's directory).
	 * Lines are terminated by the platform's line separator. An
	 * <code>IOException</code> is thrown if the file cannot be written. If the
	 * iterator is null, an empty file is created if one does not exist.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param append
	 *            if true append to existing file, else overwrite
	 * @param lines
	 *            an iterator supplying lines of text or null
	 * @throws IOException
	 */
	public void write(String fileName, boolean append, Iterator<String> lines) throws IOException {
		Output out = openOutput(fileName, append);
		int lineNr = 0;
		try {
			OutputStreamWriter w = new OutputStreamWriter(out.getStream(), charset);
			String sep = System.getProperty("line.separator");
			if (lines != null) {
				while (lines.hasNext()) {
					lineNr++;
					w.write(lines.next());
					w.write(sep);
				}
			}
			w.close();
		} catch (IOException e) {
			throw new IOException(STRINGS.msg(U.U00207, out.getName(), lineNr), e);
		} finally {
			out.stream.close();
		}
	}
	
	/**
	 * Write an array of strings to a file. The file is created if it does not
	 * exist, as is the file's directory (but not the directory's directory).
	 * Lines are terminated by the platform's line separator. An
	 * <code>IOException</code> is thrown if the file cannot be written. If the
	 * array is null, an empty file is created if one does not exist.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param append
	 *            if true append to existing file
	 * @param lines
	 *            an array of strings or null
	 * @throws IOException
	 */
	public void write(String fileName, boolean append, String[] lines) throws IOException {
		write(fileName, append, lines == null ? null : Arrays.asList(lines).iterator());
	}
	
	/**
	 * Write a string to a file. The file is created if it does not exist, as is
	 * the file's directory (but not the directory's directory). Lines are
	 * terminated by the platform's line separator. An <code>IOException</code>
	 * is thrown if the file cannot be written. If the string is null, an empty
	 * file is created if one does not exist.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param append
	 *            if true append to existing file
	 * @param string
	 *            the string to write
	 * @throws IOException when writing to the file fails
	 */
	public void write(String fileName, boolean append, String string) throws IOException {
		write(fileName, append, string == null ? null : new String[]{string});
	}

	/**
	 * Prepares an input object. If found neither in the file system nor on the
	 * class path a <code>FileNotFoundException</code> is thrown. If duplicate
	 * detection mode is active and the file has already been seen a
	 * <code>FileNotFoundException</code> is thrown.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @return an input object
	 * @throws FileNotFoundException
	 */
	private Input openInput(String fileName) throws FileNotFoundException {
		Input in = null;
		File file = new File(fileName);
		try {
			FileInputStream fis = new FileInputStream(file);
			in = new Input(file.getAbsolutePath(), fis);
		} catch (FileNotFoundException e) {
			InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
			if (is != null)
				in = new Input(fileName, is);
			else
				throw new FileNotFoundException(STRINGS.msg(U.U00208, file.getAbsolutePath()));
		}
		if (duplicates != null) {
			if (!duplicates.add(in.getName()))
				throw new FileNotFoundException(STRINGS.msg(U.U00209, in.getName()));
		}
		return in;
	}
	
	/**
	 * Prepares an output object. If the file does not exist, it is created. If
	 * the directory does not exist, it is created. A
	 * <code>FileNotFoundException</code> is thrown when the file cannot be
	 * created.
	 * <p>
	 * The method will not open a file specified with a relative path. 
	 * <p>
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param append
	 *            if true, the stream is opened in append mode
	 * @return an output object
	 * @throws FileNotFoundException
	 */
	private Output openOutput(String fileName, boolean append) throws FileNotFoundException {
		File file = new File(fileName);
		if (!file.isAbsolute()) {
			throw new FileNotFoundException(STRINGS.msg(U.U00205, fileName));
		}
		File dir = file.getParentFile();
		dir.mkdirs();
		return new Output(file.getAbsolutePath(), 
				new FileOutputStream(file.getAbsolutePath(), append));
	}

}
