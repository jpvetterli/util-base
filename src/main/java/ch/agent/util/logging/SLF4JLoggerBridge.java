package ch.agent.util.logging;

import org.slf4j.Logger;

import ch.agent.util.base.LazyString;

/**
 * SLF4J implementation of logger bridge.
 *
 */
public class SLF4JLoggerBridge implements LoggerBridge {

	private Logger logger;
	
	public SLF4JLoggerBridge(Logger logger) {
		this.logger = logger;
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}
	
	@Override
	public void trace(LazyString msg) {
		logger.trace("{}", msg);
	}

	@Override
	public void trace(String msg) {
		logger.trace(msg);
	}

	@Override
	public void debug(LazyString msg) {
		logger.debug("{}", msg);
	}

	@Override
	public void debug(String msg) {
		logger.debug(msg);
	}

	@Override
	public void info(LazyString msg) {
		logger.info("{}", msg);
	}

	@Override
	public void info(String msg) {
		logger.info(msg);
	}

	@Override
	public void warn(LazyString msg) {
		logger.warn("{}", msg);
	}

	@Override
	public void warn(String msg) {
		logger.warn(msg);
	}

	@Override
	public void error(LazyString msg) {
		logger.error("{}", msg);
	}

	@Override
	public void error(String msg) {
		logger.error(msg);
	}

	@Override
	public void error(LazyString msg, Throwable t) {
		logger.error(msg.toString(), t);
	}

	@Override
	public void error(String msg, Throwable t) {
		logger.error(msg, t);
	}

}
