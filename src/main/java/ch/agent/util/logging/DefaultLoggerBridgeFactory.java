package ch.agent.util.logging;


/**
 * The default logger factory provides access to the SLF4J logger factory.
 */
public final class DefaultLoggerBridgeFactory implements LoggerBridgeFactory {
	// final class because nothing to subclass

	private static DefaultLoggerBridgeFactory factory = new DefaultLoggerBridgeFactory();
	
	/**
	 * Get the factory singleton.
	 * 
	 * @return the DefaultLoggerBridgeFactory instance
	 */
	public static DefaultLoggerBridgeFactory getInstance() {
		return factory;
	}
	
	private DefaultLoggerBridgeFactory() {
	}

	@Override
	public LoggerBridge getLogger(String name) {
		return new DefaultLoggerBridge();
	}

	@Override
	public LoggerBridge getLogger(Class<?> klass) {
		return new DefaultLoggerBridge();
	}
	
}
