package ch.agent.util.sleep;

import java.util.Random;

import ch.agent.util.UtilMsg;
import ch.agent.util.UtilMsg.U;

/**
 * A <code>Sleeper</code> sleeps. The sleep unit is a random duration selected
 * by default between 50 and 150 milliseconds, or 0.1 second in average. It is
 * possible to sleep for multiple units. For example sleeping for 10 units
 * results in an actual sleeping duration picked at random between a half-second
 * and a second and a half, or 1 second in average. The sleep unit can be
 * redefined.
 * <p>
 * If a {@link SleeperClient} is defined, it will be asked at each step whether
 * to continue sleeping. When no such client is available sleeping continues for
 * the whole random duration.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class Sleeper {
	
	private static Random random = new Random();
	private int min = 50;
	private int max = 150;
	private SleeperClient client;
	private int step;
	private boolean continueSleepingPreviousValue = true;
	
	/**
	 * The constructor.
	 */
	public Sleeper() {
	}
	
	/**
	 * Set a sleeper client to control how long to sleep. When a client is
	 * specified, the sleeper sleeps stepwise and checks with the client after each
	 * whether to continue sleeping. Specifying a non positive step has the same
	 * effect as specifying a null client.
	 * 
	 * @param client
	 *            the sleeping client which can be null
	 * @param step
	 *            the number of milliseconds to sleep between checks on the
	 *            client
	 */
	public void setSleeperClient(SleeperClient client, int step) {
		if (step < 1)
			client = null;
		this.client = client;
		this.step = step;
	}
	
	/**
	 * Set a sleeper client with a 300ms step.
	 * 
	 * @param client the sleeper client
	 */
	public void setSleeperClient(SleeperClient client) {
		setSleeperClient(client, 300);
	}
	
	/**
	 * Set the standard sleep unit. This is a range of milliseconds. The actual
	 * sleep duration is picked at random within this range. BY default, a sleep
	 * unit is 50-100ms.
	 * 
	 * @param min
	 *            the minimum number of milliseconds in a sleep unit
	 * @param max
	 *            the maximum number of milliseconds in a sleep unit
	 */
	public void setSleepUnit(int min, int max) {
		if (min < 0 || max < 0 || min > max)
			throw new IllegalArgumentException("min < 0 || max < 0 || min > max");
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Parse the string as sleep unit. The expected format is
	 * <code>min-max</code> with blanks allowed around hyphen and comma.
	 * 
	 * @param spec
	 *            a string representing a range
	 * @see #setSleepUnit(int, int)
	 */
	public void parse(String spec) {
		String[] values = spec.split("\\s*-\\s*");
		if (values.length == 2) {
			try {
				setSleepUnit(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
			} catch (Exception e) {
				throw new IllegalArgumentException(UtilMsg.msg(U.U00301, spec), e);
			}
		} else
			throw new IllegalArgumentException(UtilMsg.msg(U.U00301, spec));
	}	
	
	/**
	 * Sleep a number of sleep units while checking on {@link SleeperClient}.
	 * 
	 * @param units
	 *            a number of units
	 * @return false if an {@link InterruptedException} was caught
	 * @see #setSleepUnit(int, int)
	 */
	public boolean sleep(int units) {
		if (units > 0)
			return sleep(units * min, units * max);
		else 
			return true;
	}

	/**
	 * Sleep a number of milliseconds selected at random in the given range. An
	 * <code>IllegalArgumentException</code> is thrown if the minimum is larger
	 * than the maximum.
	 * <p>
	 * When a sleeper client is available, the method can return before the
	 * minimum of milliseconds is reached when
	 * {@link SleeperClient#continueSleeping} returns false before a step.
	 * 
	 * @param min
	 *            the minimum sleep duration
	 * @param max
	 *            the maximum sleep duration
	 * @return false if an {@link InterruptedException} was caught
	 */

	private boolean sleep(int min, int max) {
		boolean result = true;
		int millis = min;
		if (min != max)
			millis = millis + random.nextInt(max - millis);
		if (millis > 0) {
			try {
				if (client == null || step > millis)
					Thread.sleep(millis);
				else {
					int remaining = millis;
					continueSleepingPreviousValue = true;
					while (remaining > step && continueSleeping()) {
						Thread.sleep(step);
						remaining -= step;
					}
					if (remaining > 0 && continueSleeping())
						Thread.sleep(remaining);
				}
			} catch (InterruptedException e) {
				result = false;
			}
		}
		return result;
	}
	
	/* stop testing client.continueSleeping as soon as it false */
	private boolean continueSleeping() {
		// assert: client != null
		if (continueSleepingPreviousValue)
			continueSleepingPreviousValue = client.continueSleeping();
		return continueSleepingPreviousValue;
	}

}
