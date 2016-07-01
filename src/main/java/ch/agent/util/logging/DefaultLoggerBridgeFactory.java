package ch.agent.util.logging;

import org.slf4j.LoggerFactory;

/**
 * The default logger factory provides access to the SLF4J logger factory.
 */
public final class DefaultLoggerBridgeFactory implements LoggerBridgeFactory {
	// final class because nothing to subclass

	private static DefaultLoggerBridgeFactory factory = new DefaultLoggerBridgeFactory();
	
	/**
	 * Return the DefaultLoggerFactory instance.
	 * @return the DefaultLoggerFactory instance
	 */
	public static DefaultLoggerBridgeFactory getInstance() {
		return factory;
	}
	
	private DefaultLoggerBridgeFactory() {
	}

	@Override
	public LoggerBridge getLogger(String name) {
		return new SLF4JLoggerBridge(LoggerFactory.getLogger(name));
	}

	@Override
	public LoggerBridge getLogger(Class<?> klass) {
		return new SLF4JLoggerBridge(LoggerFactory.getLogger(klass));
	}
	
}
