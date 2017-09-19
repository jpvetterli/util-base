package ch.agent.util.logging;

/**
 * The default logger factory provides access to the default logger bridge.
 */
public final class DefaultLoggerBridgeFactory implements LoggerBridgeFactory {
	// final class because nothing to subclass

	public static final String DEFAULT_LOGGER_BRIDGE_SEVERITY = "DefaultLoggerBridgeSeverity";
	private static DefaultLoggerBridgeFactory factory = new DefaultLoggerBridgeFactory();
	private static int severity = 0; // log everything

	static{
		String severityProperty = System.getProperty(DEFAULT_LOGGER_BRIDGE_SEVERITY);
		if (severityProperty != null)
			try {severity = Integer.parseInt(severityProperty);} catch (Throwable t) {}
		factory = new DefaultLoggerBridgeFactory();
	}
	
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
		return new DefaultLoggerBridge(severity);
	}

	@Override
	public LoggerBridge getLogger(Class<?> klass) {
		return new DefaultLoggerBridge(severity);
	}
	
}
