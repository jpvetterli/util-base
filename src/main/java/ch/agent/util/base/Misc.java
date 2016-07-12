package ch.agent.util.base;

import java.util.Collection;
import java.util.Iterator;


/**
 * Miscellaneous utility methods.
 *
 */
public class Misc {

	/**
	 * Ensure a reference is not null. The description passed in the second
	 * argument is usually very short (like "foo null"), as it should only help
	 * the developer identify the object which was null, in case more than one
	 * object could be involved.
	 * 
	 * @param reference
	 *            an object
	 * @param description
	 *            a message to pass to the exception
	 * @throws IllegalArgumentException
	 *             if the reference is null
	 */
	public static void nullIllegal(Object reference, String description) {
		if (reference == null)
			throw new IllegalArgumentException(description);
	}
	
	/**
	 * Test if a string is null or empty.
	 * 
	 * @param string
	 *            a string
	 * @return true if the string is null or if its length is 0
	 */
	public static boolean isEmpty(String string) {
		return string == null || string.length() == 0;
	}
	
	/**
	 * Join elements of an array of items into a string using a separator.
	 * Elements are turned into a string using {@link #toString}.
	 * 
	 * @param separator
	 *            a non-null separator
	 * @param items
	 *            an non-null array of items
	 * @return a string
	 */
	public static <T>String join(String separator, T[] items) {
		nullIllegal(separator, "separator null");
		nullIllegal(items, "items null");
		StringBuilder b = new StringBuilder();
		if (items.length > 0)
			b.append(items[0].toString());
		for (int i = 1; i < items.length; i++) {
			b = b.append(separator);
			b = b.append(items[i].toString());
		}
		return b.toString();
	}
	
	/**
	 * Join elements of a collection of items into a string using a separator.
	 * Elements are turned into a string using {@link #toString}.
	 * 
	 * @param separator
	 *            a non-null separator
	 * @param items
	 *            an non-null collection of items
	 * @return a string
	 */
	public static <T>String join(String separator, Collection<T> items) {
		nullIllegal(separator, "separator null");
		nullIllegal(items, "items null");
		StringBuilder b = new StringBuilder();
		Iterator<T> it = items.iterator();
		if (it.hasNext())
			b.append(it.next().toString());
		while(it.hasNext()) {
			b = b.append(separator);
			b = b.append(it.next().toString());
		}
		return b.toString();
	}

	/**
	 * Split a string into a given number of parts. If {@code count} is negative
	 * there is no restriction on the number of parts. If it is zero, the method
	 * returns an empty array. If it is 1 the method returns an array with the
	 * input as single element. If it is larger than 1, it must be possible to
	 * split the input into exactly {@code count} parts. When this is not the
	 * case the method throws an {@link IllegalArgumentException} with a terse
	 * message indicating the actual number of parts and the count.
	 * <p>
	 * To make usage easier, when the input is empty and count is negative, the
	 * result is an empty array.
	 * 
	 * @param input
	 *            the string to split, not null
	 * @param separator
	 *            a string specifying the separator pattern, not null
	 * @param count
	 *            the number of parts
	 * @return the array of parts split from the input
	 */
	public static String[] split(String input, String separator, int count) {
		Misc.nullIllegal(input, "input null");
		Misc.nullIllegal(separator, "separator null");
		String[] parts = null; 
		if (count == 0)
			parts = new String[0];
		else if (count == 1)
			parts = new String[]{input};
		else {
			parts = input.split(separator);
			if (count > 0) {
				if (parts.length != count)
					throw new IllegalArgumentException(parts.length + "!=" + count);
			} else {
				// more convenient for Args#stringSplit and #intSplit
				if (parts.length == 1 && parts[0].length() == 0)
					parts = new String[0];
			}
		}
		return parts;
	}
	
	/**
	 * Return a substring of the input. If the input is longer than a given
	 * length, remove excess characters, and append an ellipsis. If the length
	 * is specified as 0 or less, a length of 100 will be applied. A null input
	 * is returned as is. If the ellipsis is null, a sequence of 3 periods is
	 * used. The length of the ellipsis is <em>not</em> counted in the length
	 * specification.
	 * 
	 * @param input
	 *            input string
	 * @param length
	 *            the total length not to be exceeded, including a 3-character
	 *            ellipsis
	 * @param ellipsis
	 *            the ellipsis or null for "..."
	 * @return a string
	 */
	public static String truncate(String input, int length, String ellipsis) {
		if (input != null) {
			length = length < 1 ? 100 : length;
			int currentLength = input.length();
			if (currentLength > length)
				input = input.substring(0, length) + (ellipsis == null ? "..." : ellipsis);
		}
		return input;
	}
	
	/**
	 * Convert milliseconds into string with days, hours, minutes, and seconds.
	 * Leading days and hours are omitted if zero.
	 * 
	 * @param t
	 *            milliseconds
	 * @return a string representing days, hours, minutes, and seconds
	 */
	public static String dhms(long t) {
		final int MPD = 24*60*60*1000;
		long days = (t / MPD);
		int s = (int) (t - days * MPD) / 1000;
		int m = s / 60;
		int h = m / 60;
		s = s - m * 60;
		m = m - h * 60;
		String result = "";
		if (days > 0)
			result = String.format("%dd%dh%dm%ds", days, h, m, s);
		else {
			if (h > 0)
				result = String.format("%dh%dm%ds", h, m, s);
			else
				result = String.format("%dm%ds", m, s);
		}
		return result;
	}

}
