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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ch.agent.util.UtilMsg;
import ch.agent.util.UtilMsg.U;

/**
 * TextFile provides support for reading and writing text files.
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
		 * Take one line of text. Returns true to skip remaining lines.
		 * 
		 * @param lineNr
		 *            the line number
		 * @param line
		 *            the text, with line separator already removed
		 * @return true to request skipping remaining lines
		 * @throws Exception
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
	 * detect cycles. Setting the mode on or off resets the detector to its
	 * initial state.
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
	 * resource on the classpath. An exception is thrown if the file cannot be
	 * read or if the visitor throws an exception.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param visitor
	 *            a visitor taking lines of text
	 */
	public void read(String fileName, Visitor visitor) {
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
				throw new UtilMsg(U.U00202, fh.getName(), lineNr).runtimeException(e);
			else
				throw new UtilMsg(U.U00201, fh.getName(), lineNr).runtimeException(e);
		}
	}

	/**
	 * Write a series of lines to a file. The file is created if it does not
	 * exist, as is the file's directory (but not the directory's directory).
	 * Lines are terminated by the platform's line separator. An exception is
	 * thrown if the file cannot be written.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param append if true append to existing file, else overwrite 
	 * @param lines
	 *            an iterator supplying lines of text
	 */
	public void write(String fileName, boolean append, Iterator<String> lines) {
		Output out = openOutput(fileName, append);
		OutputStreamWriter w = new OutputStreamWriter(out.getStream(), charset);
		int lineNr = 0;
		try {
			String sep = System.getProperty("line.separator");
			while(lines.hasNext()) {
				lineNr++;
				w.write(lines.next());
				w.write(sep);
			}
			w.close();
		} catch (IOException e) {
			throw new UtilMsg(U.U00207, out.getName(), lineNr).runtimeException(e);
		}
	}

	/**
	 * Prepares an input object. If found neither in the file system nor on the
	 * classpath an exception is thrown. If duplicate detection mode is active and
	 * the file has already been seen an exception is thrown.
	 * 
	 * @param fileName the name of the file
	 * @return an input object
	 */
	private Input openInput(String fileName) {
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
				throw new UtilMsg(U.U00208, file.getAbsolutePath()).runtimeException(e);
		}
		if (duplicates != null) {
			if (!duplicates.add(in.getName()))
				throw new UtilMsg(U.U00209, in.getName()).runtimeException();
		}
		return in;
	}
	
	/**
	 * Prepares an output object. If the file does not exist, it is
	 * created. If the directory does not exist, it is created (but not the
	 * directory's directory.)
	 * <p>
	 * 
	 * @param fileName
	 *            the name of the file
	 * @param append
	 *            if true, the stream is opened in append mode
	 * @return an output object
	 */
	private Output openOutput(String fileName, boolean append) {
		File file = new File(fileName);
		File dir = file.getParentFile();
		if (dir == null) {
			throw new UtilMsg(U.U00205, file.getAbsolutePath()).runtimeException();
		}
		dir.mkdir();
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file.getAbsolutePath(), append);
		} catch (FileNotFoundException e) {
			throw new UtilMsg(U.U00206, file.getAbsolutePath()).runtimeException(e);
		}
		return new Output(file.getAbsolutePath(), fos);
	}

}
