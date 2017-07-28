package ch.agent.util.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Miscellaneous utility methods. All methods are static and the object is never
 * instantiated.
 */
public class Misc {

	// do not instantiate, all methods are static
	private Misc() {
	}

	/**
	 * Ensure a reference is not null. The description passed in the second
	 * argument is usually very short (like "foo null"), as it should only help
	 * the developer identify the object when there are many.
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
	 * Join elements of an array of items into a string using zero or more
	 * separators. Elements are turned into strings using {@link #toString}.
	 * 
	 * @param <T>
	 *            the type of collection items
	 * @param separator
	 *            a non-null separator
	 * @param items
	 *            an non-null collection of items
	 * @return a string
	 */
	public static <T> String join(String separator, T[] items) {
		nullIllegal(items, "items null");
		return join("", separator, "", Arrays.asList(items));
	}

	/**
	 * Join elements of an array of items into a string using a prefix, zero or
	 * more separators, and a suffix. Elements are turned into strings using
	 * {@link #toString}.
	 * 
	 * @param <T>
	 *            the type of collection items
	 * @param prefix
	 *            a non-null prefix
	 * @param separator
	 *            a non-null separator
	 * @param suffix
	 *            a non-null suffix
	 * @param items
	 *            an non-null collection of items
	 * @return a string
	 */
	public static <T> String join(String prefix, String separator, String suffix, T[] items) {
		nullIllegal(items, "items null");
		return join(prefix, separator, suffix, Arrays.asList(items));
	}

	/**
	 * Join elements of a collection of items into a string using a separator.
	 * Elements are turned into strings using {@link #toString}.
	 * 
	 * @param <T>
	 *            the type of collection items
	 * @param separator
	 *            a non-null separator
	 * @param items
	 *            an non-null collection of items
	 * @return a string
	 */
	public static <T> String join(String separator, Collection<T> items) {
		nullIllegal(items, "items null");
		return join("", separator, "", items);
	}

	/**
	 * Join elements of a collection of items into a string using a prefix, zero
	 * or more separators, and a suffix. Elements are turned into strings using
	 * {@link #toString}.
	 * 
	 * @param <T>
	 *            the type of collection items
	 * @param prefix
	 *            a non-null prefix
	 * @param separator
	 *            a non-null separator
	 * @param suffix
	 *            a non-null suffix
	 * @param items
	 *            an non-null collection of items
	 * @return a string
	 */
	public static <T> String join(String prefix, String separator, String suffix, Collection<T> items) {
		nullIllegal(prefix, "prefix null");
		nullIllegal(separator, "separator null");
		nullIllegal(suffix, "suffix null");
		nullIllegal(items, "items null");
		StringBuilder b = new StringBuilder();
		b.append(prefix);
		Iterator<T> it = items.iterator();
		if (it.hasNext())
			b.append(it.next().toString());
		while (it.hasNext()) {
			b = b.append(separator);
			b = b.append(it.next().toString());
		}
		b.append(suffix);
		return b.toString();
	}

	/**
	 * Split a string into a given number of parts. If <code>count</code> is
	 * negative there is no restriction on the number of parts. If it is zero,
	 * the method returns an empty array. If it is 1 the method returns an array
	 * with the input as single element. If it is larger than 1, it must be
	 * possible to split the input into exactly <code>count</code> parts. When
	 * this is not the case the method throws an
	 * {@link IllegalArgumentException} with a terse message indicating the
	 * actual number of parts and the count.
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
	 * @throws IllegalArgumentException
	 *             as described in the comment
	 */
	public static String[] split(String input, String separator, int count) {
		Misc.nullIllegal(input, "input null");
		Misc.nullIllegal(separator, "separator null");
		String[] parts = null;
		if (count == 0)
			parts = new String[0];
		else if (count == 1)
			parts = new String[] { input };
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
	 *            input string or null
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
	 * Mark a position in a string. The method inserts » in front of the
	 * position. It returns a portion of input of the requested length with the
	 * position and marker visible. When the input is truncated, ellipsis are
	 * inserted where appropriate. A null input or a position outside the input
	 * produce an exception (last valid position = length of input). A length
	 * less than 30 is ignored and the minimum of 30 is used instead.
	 * <p>
	 * As an example, this sentence would be returned as (position=35,
	 * length=50):
	 * 
	 * <pre>
	 * <code>
	 * As an exampl[...]tence would [--&gt;]be returned[...]
	 * </code>
	 * </pre>
	 * 
	 * @param input
	 *            a string
	 * @param position
	 *            a position within the string, first position is 0
	 * @param length
	 *            the length of the result
	 * @return the truncated input with a marker and one or two ellipsis added
	 */
	public static String mark(String input, int position, int length) {
		final String MARK = "[-->]"; 
		final String ELLIPSIS = "[...]"; 
		final int MARK_LEN = MARK.length();
		final int ELLIPSIS_LEN = ELLIPSIS.length();
		
		if (input == null)
			throw new IllegalArgumentException("input null");
		if (position < 0 || position > input.length())
			throw new IllegalArgumentException("position outside input");
		
		String half1 = input.substring(0,  position);
		String half2 = input.substring(position);
		length = length < 30 ? 30 : length;
		int currentLength = input.length() + MARK_LEN;
		StringBuilder b = new StringBuilder();
		int excess = currentLength - length;
		if (excess <= 0) {
			b.append(half1);
			b.append(MARK);
			b.append(half2);
		} else if (excess <= ELLIPSIS_LEN) {
			if (position > currentLength/2) {
				// ellipsis in the middle of first half
				int effectiveLength = half1.length() - ELLIPSIS_LEN - MARK_LEN;
				int mid = effectiveLength / 2;
				int remainder = effectiveLength - mid;
				b.append(half1.substring(0,  mid));
				b.append(ELLIPSIS);
				b.append(half1.substring(half1.length() - remainder));
				b.append(MARK);
				b.append(half2);
			} else {
				// ellipsis at end
				b.append(half1);
				b.append(MARK);
				b.append(half2);
				b.setLength(length - ELLIPSIS_LEN);
				b.append(ELLIPSIS);
			}
		} else {
			int effectiveLength = length - 2 * ELLIPSIS_LEN - MARK_LEN;
			int chunkLength = effectiveLength / 3;
			if (chunkLength >= half2.length()) {
				// ellipsis in the first half 
				effectiveLength = length - ELLIPSIS_LEN - MARK_LEN - half2.length();
				chunkLength = effectiveLength / 2;
				int remainder = effectiveLength - chunkLength;
				b.append(half1.substring(0, chunkLength));
				b.append(ELLIPSIS);
				b.append(half1.substring(half1.length() - remainder));
				b.append(MARK);
				b.append(half2);
			} else {
				// ellipsis in the first half and at end of second half
				int remainder = effectiveLength - 2 * chunkLength;
				b.append(half1.substring(0, chunkLength));
				b.append(ELLIPSIS);
				b.append(half1.substring(half1.length() - chunkLength));
				b.append(MARK);
				b.append(half2.substring(0, remainder));
				b.append(ELLIPSIS);
			}
		}
		return b.toString();
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
		final int MPD = 24 * 60 * 60 * 1000;
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

	/**
	 * Test if the absolute difference between two numbers is larger than 1e-10.
	 * 
	 * @param d1
	 *            a number
	 * @param d2
	 *            another number
	 * @return true if the two numbers are not close
	 */
	public static boolean notClose(double d1, double d2) {
		double EPSILON = 1e-10;
		return notClose(d1, d2, EPSILON);
	}

	/**
	 * Test if the absolute difference between two numbers is larger than a
	 * threshold.
	 * 
	 * @param d1
	 *            a number
	 * @param d2
	 *            another number
	 * @param threshold
	 *            a very small number
	 * @return true if the first two numbers are not close
	 */
	public static boolean notClose(double d1, double d2, double threshold) {
		return Math.abs(d1 - d2) > threshold;
	}

}
