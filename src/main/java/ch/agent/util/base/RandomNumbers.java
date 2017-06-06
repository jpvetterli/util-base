package ch.agent.util.base;

import java.util.Random;

/**
 * Various random number generators.
 * 
 */
public class RandomNumbers {

	private static Random RANDOM = new Random();

	private RandomNumbers() {
	}

	/**
	 * A random number generator generating values in a given range.
	 * 
	 */
	public static class Uniform {
		private final int base;
		private final int limit;

		/**
		 * Constructor.
		 * 
		 * @param min
		 *            the minimal value to generate
		 * @param max
		 *            the maximal value to generate, cannot be smaller than min
		 * @throws IllegalArgumentException
		 *             if max &lt; min
		 */
		public Uniform(int min, int max) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.base = min;
			this.limit = max - min + 1;
		}

		/**
		 * Return the next random value selected from the range specified in the
		 * constructor.
		 * 
		 * @return an integer
		 */
		public int next() {
			return limit == 1 ? base : base + RANDOM.nextInt(limit);
		}

		@Override
		public String toString() {
			return String.format("[%d, %d]", base, limit + base - 1);
		}

	}

	/**
	 * A random number generator combining two uniforms. The generator uses two
	 * uniforms, plus a low and a high limit within the range of the first
	 * uniform. To generate a random number, a value is selected from the first
	 * uniform. If the value is within the limits, it is used as the result, else 
	 * a value is selected from the second uniform and used as the result.
	 */
	public static class Uniform2 {
		private Uniform uni1;
		private Uniform uni2;
		private final int low;
		private final int high;

		/**
		 * Constructor.
		 * 
		 * @param min
		 *            the minimum value of the first range
		 * @param max
		 *            the maximum value of the first range, cannot be smaller
		 *            than min
		 * @param low
		 *            the low value, cannot be outside the first range
		 * @param high
		 *            the high value, cannot be outside the first range
		 * @param min2
		 *            the minimum value of the first range, cannot be larger
		 *            than min
		 * @param max2
		 *            the maximum value of the first range, cannot be smaller
		 *            than max or min2
		 * @throws IllegalArgumentException
		 *             if a parameter constraint is violated
		 */
		public Uniform2(int min, int max, int low, int high, int min2, int max2) {
			uni1 = new Uniform(min, max);
			uni2 = new Uniform(min2, max2);
			if (high < low)
				throw new IllegalArgumentException("high < low");
			if (low < min || low > max)
				throw new IllegalArgumentException("low not in [min, max]");
			if (high < min || high > max)
				throw new IllegalArgumentException("high not in [min, max]");
			if (min2 > min)
				throw new IllegalArgumentException("min2 > min");
			if (max2 < max)
				throw new IllegalArgumentException("max2 < max");
			this.high = high;
			this.low = low;
		}

		/**
		 * Return the next random value.
		 * 
		 * @return an integer
		 */
		public int next() {
			int n = uni1.next();
			return n <= low || n >= high ? uni2.next() : n;
		}

		@Override
		public String toString() {
			return String.format("%s %s low=%d high=%d", uni1, uni2, low, high);
		}

	}

	/**
	 * A random number generator producing bursts of non-decreasing positive
	 * numbers.
	 * <p>
	 * When these numbers are interpreted as milliseconds from January 1970, the
	 * generator can be used to simulate seemingly random bursts of activity.
	 * <p>
	 * The basic properties of the generator are:
	 * <ul>
	 * <li>The <b>burst size</b>. The number of numbers in a burst.
	 * <li>The <b>small increments</b>. The difference between two numbers in a
	 * burst.
	 * <li>The <b>large increments</b>. The difference between the last number
	 * in a burst and the first number in the next burst.
	 * <li>The <b>base</b>. The first number.
	 * </ul>
	 * The first three properties are not numbers but ranges of a uniform random
	 * variable. For example a burst size of 1-5 specifies a random value in
	 * that range; an increment of 5000-10000 specifies a random value in that
	 * range (between 5 and 10 seconds when interpreting the numbers as
	 * milliseconds).
	 */
	public static class RandomBurst {

		private long currentNr = 0; // last value generated
		private int burstCount = 0; // decrements to 0 indicating end of burst
		private Uniform randomLargeIncrement;
		private Uniform randomSmallIncrement;
		private Uniform randomBurstSize;

		/**
		 * Constructor.
		 */
		public RandomBurst() {
			super();
			randomLargeIncrement = new Uniform(10 * 60 * 1000, 100 * 60 * 1000); // 10-100
																					// minutes
			randomSmallIncrement = new Uniform(3 * 1000, 20 * 1000); // 3-20
																		// seconds
			randomBurstSize = new Uniform(1, 5); // 1-5 events in a burst
		}

		/**
		 * Set the base. The default value is zero. If the argument is negative,
		 * the base will be computed on first access using
		 * <code>System.currentTimeMillis()</code>, the number of milliseconds
		 * since January 1970 in the UTC domain at the time of invocation. Note
		 * that if the first use of {@link #test(long)} is also done with a
		 * negative value, the first test will always return true. To avoid this
		 * effect, make a test with a negative value and discard the result
		 * before making actual tests.
		 * 
		 * @param base
		 *            a number
		 */
		public void setBase(long base) {
			this.currentNr = base;
		}

		/**
		 * Set the range for the number of events during a burst. The actual
		 * number will be chosen at random in the range specified. The default
		 * values are 1 and 5.
		 * <p>
		 * Passing two zero values disables generation of random numbers. In
		 * such a case {@link #next()} will always return the current number and
		 * {@link #test(long)} will always return false.
		 * 
		 * @param min
		 *            the minimum number of events, not negative
		 * @param max
		 *            the maximum number of events, not smaller than min
		 * @throws IllegalArgumentException
		 *             if max &lt; min
		 */
		public void setBurstSize(int min, int max) {
			randomBurstSize = max == 0 ? null : new Uniform(min, max);
		}

		/**
		 * Set the range for the increment between two numbers during a burst.
		 * The actual increment will be chosen at random in the range specified.
		 * When interpreting the parameters as milliseconds, the default values
		 * correspond to 3 and 20 seconds. Passing two zero values has the same
		 * effect as specifying a burst size of 1.
		 * 
		 * @param min
		 *            the minimum increment, not negative
		 * @param max
		 *            the maximum increment, not smaller than min
		 * @throws IllegalArgumentException
		 *             if max &lt; min
		 */
		public void setSmallIncrement(int min, int max) {
			randomSmallIncrement = max == 0 ? null : new Uniform(min, max);
		}

		/**
		 * Set the range for the increment between two bursts. The actual
		 * increment will be chosen at random in the range specified. When
		 * interpreting the parameters as milliseconds, the default values
		 * correspond to 10 and 100 minutes.
		 * 
		 * @param min
		 *            the minimum increment
		 * @param max
		 *            the maximum increment
		 * @throws IllegalArgumentException
		 *             if max &lt; min
		 */
		public void setLargeIncrement(int min, int max) {
			randomLargeIncrement = new Uniform(min, max);
		}

		/**
		 * Return the next number in a non-decreasing sequence of positive
		 * random numbers. The first number returned is not smaller than the one
		 * last set with {@link #setBase(long)} and it has a large increment
		 * from the base. It is followed by a random number of numbers with
		 * small increments. The next number has a large increment. And so on.
		 * If zero values were specified for small increments, there will be
		 * only large increments.
		 * <p>
		 * When random number generation is disabled (because of a zero burst
		 * size was specified), the method returns the unchanged current number.
		 * 
		 * @return a positive random number not smaller than the previous one or
		 *         the base if just set
		 */
		public long next() {
			if (currentNr < 0)
				currentNr = System.currentTimeMillis();
			if (!isDisabled()) {
				if (burstCount == 0 || noSmallIncr()) {
					currentNr += randomLargeIncrement.next();
					burstCount = randomBurstSize.next();
				} else
					currentNr += randomSmallIncrement.next();
				burstCount--;
			}
			return currentNr;
		}

		/**
		 * Test if the argument is larger than or equal to the current random
		 * number. If the argument is negative, the test value will be computed
		 * using <code>System.currentTimeMillis()</code>, the number of
		 * milliseconds since January 1970 in the UTC domain. Read the
		 * documentation of the {@link #setBase(long)} method to understand what
		 * happens when using a negative value for both {@link #setBase(long)}
		 * and {@link #test(long)}.
		 * <p>
		 * When test is true, a new random number is generated.
		 * <p>
		 * When random number generation is disabled, the method returns false.
		 * 
		 * @param testValue
		 *            a number
		 * @return false if the argument is smaller than the current random
		 *         number or if random numbers are disabled
		 */
		public boolean test(long testValue) {
			boolean result = false;
			if (!isDisabled()) {
				if (currentNr < 0)
					currentNr = System.currentTimeMillis();
				if (testValue < 0)
					testValue = System.currentTimeMillis();
				if (testValue >= currentNr) {
					result = true;
					next();
				}
			}
			return result;
		}

		private boolean isDisabled() {
			return randomBurstSize == null;
		}

		private boolean noSmallIncr() {
			return randomSmallIncrement == null;
		}

		@Override
		public String toString() {
			return isDisabled() ? "burst size: 0 (disabled)" : String.format("burst size: %s small increment: %s large increment: %s", randomBurstSize, randomSmallIncrement, randomLargeIncrement);
		}

	}

}
