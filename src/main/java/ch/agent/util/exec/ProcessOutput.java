package ch.agent.util.exec;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

/**
 * An extension of {@link ByteArrayOutputStream} with a size limit.
 * 
 * @author Jean-Paul
 * 
 */
public class ProcessOutput extends ByteArrayOutputStream {

	private int max;
	
	/**
	 * Constructor.
	 * 
	 * @param size
	 *            positive size limit and initial size
	 * @throws IllegalArgumentException
	 *             if size is negative
	 */
	public ProcessOutput(int size) {
		super(size);
		this.max = size;
	}

	/**
	 * Constructor.
	 * 
	 * @param initSize
	 *            positive initial size
	 * @param maxSize
	 *            positive size limit
	 * @throws IllegalArgumentException
	 *             if a size is negative
	 */
	public ProcessOutput(int initSize, int maxSize) {
		super(initSize);
		if (max < 0)
			throw new IllegalArgumentException("maxSize negative");
		this.max = maxSize;
	}

	/**
	 * Return the size limit.
	 * 
	 * @return a p
	 */
	public int getSizeLimit() {
		return max;
	}
	
	@Override
	public synchronized void write(int b) {
		if (size() < max)
			super.write(b);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) {
		int r = max - size();
		if (r >= len)
			super.write(b, off, len);
		else if (r > 0)
			super.write(b, off, r);
	}
	
	/**
	 * Return the output as an array of lines. The method rescans the whole
	 * output on each invocation.
	 * 
	 * @return an array of strings, possibly empty, never null
	 */
	public String[] toLines() {
		if (size() == 0)
			return new String[0];
		else {
			Pattern p = Pattern.compile("\r?\n\r?");
			return p.split(toString());
		}
	}
	
}
