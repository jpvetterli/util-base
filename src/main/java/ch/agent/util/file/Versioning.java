package ch.agent.util.file;

import java.io.File;

/**
 * A versioning object moves files out of the way.
 * 
 * @author Jean-Paul
 *
 */
public class Versioning {

	private String pattern;
	private int limit;
	
	/**
	 * Constructor for a versioning object. 
	 * 
	 * @param pattern the pattern to use when renaming
	 * @param limit the maximum number of versions
	 */
	public Versioning(String pattern, int limit) {
		super();
		this.pattern = pattern;
		this.limit = limit;
	}

	/**
	 * Constructor for a versioning object with pattern "%s.%02d" and a most 99
	 * versions. File "foo.bar" will be renamed using the first available name
	 * in the sequence foo.bar.01, ..., foo.bar.99.
	 */
	public Versioning() {
		this("%s.%02d", 99);
	}
	
	/**
	 * Rename a file. Note that the method returns true when the file does not
	 * exist. The idea is to return true if the way is clear.
	 * 
	 * @param file
	 *            a file
	 * @return false if the file exists but could not be moved else returns true
	 */
	public boolean move(File file) {
		if (!file.exists())
			return true;
		boolean done = false;
		for (int i = 1; i <= limit; i++) {
			File newFile = new File(String.format(pattern, file.getAbsolutePath(), i));
			if (newFile.exists())
				continue;
			done = file.renameTo(newFile);
			if (done)
				break;
		}
		return done;	
	}
	
	/**
	 * Rename a file. Note that the method returns true when the file does not
	 * exist.
	 * 
	 * @param file
	 *            a file name
	 * @return false if the file exists but could not be moved else returns true
	 */
	public boolean move(String file) {
		return move(new File(file));
	}

}
