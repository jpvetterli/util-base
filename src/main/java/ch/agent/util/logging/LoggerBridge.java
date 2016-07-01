package ch.agent.util.logging;

import ch.agent.util.base.LazyString;

/**
 * A logger bridge is just a logger. Using a logger bridge instead of simply
 * using the underlying logger avoids having to deal with different APIs in some
 * situations.
 * <p>
 * As an example, the Akka actor system provides its own logging API on top of
 * SLF4J. When logging from Akka-aware classes, using the Akka API for logging
 * is a good idea, because of performance. On the other hand, when classes in
 * the same system are not aware of the Akka context, they need to log directly
 * to the SLF4J API. The APIs differ a little and the code to get a logger from
 * a factory is also different. Using a logger bridge resolves the issue, and it
 * is possible to switch to another underlying logger without touching the
 * source.
 * <p>
 * Sometimes a log level is not available in the underlying implementation. When
 * this is the case, the next higher level available is used and the log message
 * is prefixed with the desired level in upper case followed by a blank. For
 * example, supposing the <em>trace</em> level is not supported, a
 * <em>trace</em> message is logged as a <em>debug</em> message with a prefix of
 * <q>TRACE</q>.
 * <p>
 * The <em>lazy string</em> version of the logging methods are provided for
 * dealing with expensive messages. The performance cost of a lazy string is
 * assumed only when {@code #toString} is used on it, which occurs only
 * when (if) the message is actually logged, depending on the logging level.
 */
public interface LoggerBridge {

	/**
	 * Test the trace logging level.
	 * 
	 * @return true if a trace message would be logged
	 */
	public boolean isTraceEnabled();

	/**
	 * Test the debug logging level.
	 * 
	 * @return true if a debug message would be logged
	 */
	public boolean isDebugEnabled();

	/**
	 * Test the info logging level.
	 * 
	 * @return true if an info message would be logged
	 */
	public boolean isInfoEnabled();

	/**
	 * Test the warn logging level.
	 * 
	 * @return true if a warn message would be logged
	 */
	public boolean isWarnEnabled();

	/**
	 * Test the error logging level.
	 * 
	 * @return true if an error message would be logged
	 */
	public boolean isErrorEnabled();

	/**
	 * Log a lazy string at the trace logging level.
	 * 
	 * @param msg
	 *            a lazy string
	 */
	public void trace(LazyString msg);

	/**
	 * Log a string at the trace logging level.
	 * 
	 * @param msg
	 *            a string
	 */
	public void trace(String msg);

	/**
	 * Log a lazy string at the debug logging level.
	 * 
	 * @param msg
	 *            a lazy string
	 */
	public void debug(LazyString msg);

	/**
	 * Log a string at the debug logging level.
	 * 
	 * @param msg
	 *            a string
	 */
	public void debug(String msg);

	/**
	 * Log a lazy string at the info logging level.
	 * 
	 * @param msg
	 *            a lazy string
	 */
	public void info(LazyString msg);

	/**
	 * Log a string at the info logging level.
	 * 
	 * @param msg
	 *            a string
	 */
	public void info(String msg);

	/**
	 * Log a lazy string at the warn logging level.
	 * 
	 * @param msg
	 *            a lazy string
	 */
	public void warn(LazyString msg);

	/**
	 * Log a string at the warn logging level.
	 * 
	 * @param msg
	 *            a string
	 */
	public void warn(String msg);

	/**
	 * Log a lazy string at the error logging level.
	 * 
	 * @param msg
	 *            a lazy string
	 */
	public void error(LazyString msg);

	/**
	 * Log a string at the error logging level.
	 * 
	 * @param msg
	 *            a string
	 */
	public void error(String msg);

	/**
	 * Log a lazy string at the error logging level. Also log the exception
	 * causing the error.
	 * 
	 * @param msg
	 *            a lazy string
	 * @param t
	 *            the cause of the error
	 */
	public void error(LazyString msg, Throwable t);

	/**
	 * Log a string at the error logging level. Also log the exception causing
	 * the error.
	 * 
	 * @param msg
	 *            a string
	 * @param t
	 *            the cause of the error
	 */
	public void error(String msg, Throwable t);

}
