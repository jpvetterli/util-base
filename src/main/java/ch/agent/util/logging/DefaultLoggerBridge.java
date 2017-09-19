package ch.agent.util.logging;


import ch.agent.util.base.LazyString;

/**
 * Default logger bridge. Prints messages to standard error. Messages are
 * prefixed with a capital letter indicating the logging severity. A severity
 * level is set when creating the logger bridge. Logging of messages less severe
 * than this level is disabled. The severity levels 
 * are 
 * <ul>
 * <li>0 (or less) log all messages</li>
 * <li>1 log "trace", "debug", "info", "warn" and "error" messages</li>
 * <li>2 log "debug", "info", "warn" and "error" messages</li>
 * <li>3 log "info", "warn" and "error" messages</li>
 * <li>4 log only "warn" and "error" messages</li>
 * <li>5 log only "error" messages</li>
 * <li>6 (or more) do not log any message</li>
 * </ul>
 */
public class DefaultLoggerBridge implements LoggerBridge {

	private static final int TRACE = 1;
	private static final int DEBUG = 2;
	private static final int INFO = 3;
	private static final int WARN = 4;
	private static final int ERROR = 5;
	
	private final int severity;
	
	/**
	 * Constructor. Messages less severe than the argument will be ignored.
	 * 
	 * @param severity
	 *            a value between 0 and 6
	 */
	public DefaultLoggerBridge(int severity) {
		this.severity = severity;
	}
	

	@Override
	public boolean isTraceEnabled() {
		return severity <= TRACE;
	}

	@Override
	public boolean isDebugEnabled() {
		return severity <= DEBUG;
	}

	@Override
	public boolean isInfoEnabled() {
		return severity <= INFO;
	}

	@Override
	public boolean isWarnEnabled() {
		return severity <= WARN;
	}

	@Override
	public boolean isErrorEnabled() {
		return severity <= ERROR;
	}
	
	@Override
	public void trace(LazyString msg) {
		if (isTraceEnabled())
			System.err.println("T " + msg.toString());
	}

	@Override
	public void trace(String msg) {
		if (isTraceEnabled())
			System.err.println("T " + msg);
	}

	@Override
	public void debug(LazyString msg) {
		if (isDebugEnabled())
			System.err.println("D " + msg.toString());
	}

	@Override
	public void debug(String msg) {
		if (isDebugEnabled())
			System.err.println("D " + msg);
	}

	@Override
	public void info(LazyString msg) {
		if (isInfoEnabled())
			System.err.println("I " + msg.toString());
	}

	@Override
	public void info(String msg) {
		if (isInfoEnabled())
			System.err.println("I " + msg);
	}

	@Override
	public void warn(LazyString msg) {
		if (isWarnEnabled())
			System.err.println("W " + msg.toString());
	}

	@Override
	public void warn(String msg) {
		if (isWarnEnabled())
			System.err.println("W " + msg);
	}

	@Override
	public void error(LazyString msg) {
		if (isErrorEnabled())
			System.err.println("E " + msg.toString());
	}

	@Override
	public void error(String msg) {
		if (isErrorEnabled())
			System.err.println("E " + msg);
	}

	@Override
	public void error(LazyString msg, Throwable t) {
		if (isErrorEnabled()) {
			System.err.println("E " + msg.toString());
			t.printStackTrace(System.err);
		}
	}

	@Override
	public void error(String msg, Throwable t) {
		if (isErrorEnabled()) {
			System.err.println("E " + msg);
			t.printStackTrace(System.err);
		}
	}

}
