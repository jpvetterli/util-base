package ch.agent.util.logging;

import ch.agent.util.base.LazyString;

/**
 * Default logger bridge. Always prints all messages to standard error.
 * Messages are prefixed with a capital letter indicating the logging level.
 */
public class DefaultLoggerBridge implements LoggerBridge {

	
	public DefaultLoggerBridge() {
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}
	
	@Override
	public void trace(LazyString msg) {
		System.err.println("T " + msg.toString());
	}

	@Override
	public void trace(String msg) {
		System.err.println("T " + msg);
	}

	@Override
	public void debug(LazyString msg) {
		System.err.println("D " + msg.toString());
	}

	@Override
	public void debug(String msg) {
		System.err.println("D " + msg);
	}

	@Override
	public void info(LazyString msg) {
		System.err.println("I " + msg.toString());
	}

	@Override
	public void info(String msg) {
		System.err.println("I " + msg);
	}

	@Override
	public void warn(LazyString msg) {
		System.err.println("W " + msg.toString());
	}

	@Override
	public void warn(String msg) {
		System.err.println("W " + msg);
	}

	@Override
	public void error(LazyString msg) {
		System.err.println("E " + msg.toString());
	}

	@Override
	public void error(String msg) {
		System.err.println("E " + msg);
	}

	@Override
	public void error(LazyString msg, Throwable t) {
		System.err.println("E " + msg.toString());
		t.printStackTrace(System.err);
	}

	@Override
	public void error(String msg, Throwable t) {
		System.err.println("E " + msg);
		t.printStackTrace(System.err);
	}

}
