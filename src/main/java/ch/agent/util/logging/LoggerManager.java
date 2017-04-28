package ch.agent.util.logging;

import static ch.agent.util.STRINGS.msg;

import java.lang.reflect.Method;

import ch.agent.util.STRINGS.U;

/**
 * The logger manager manages logger factories. It can also directly provide
 * loggers. When nothing special is done, the logger manager uses
 * {@link DefaultLoggerBridgeFactory} as the logger bridge factory, which
 * provides access to SLF4J. To override the default, the name of an alternative
 * factory class can be passed as a system property with the key
 * <em>LoggerBridgeFactory</em>.
 * 
 */
public class LoggerManager {

	public static final String LOGGER_BRIDGE_FACTORY = "LoggerBridgeFactory";
	private static final String INSTANCE_METHOD = "getInstance";
	
	private static LoggerBridgeFactory factory;
	
	/**
	 * Return the LoggerBridgeFactory instance.
	 * This method is synchronized.
	 * 
	 * @return the LoggerBridgeFactory instance
	 */
	public static synchronized LoggerBridgeFactory getFactory() {
		if (factory == null) {
			String className = System.getProperty(LOGGER_BRIDGE_FACTORY);
			if (className == null)
				factory = DefaultLoggerBridgeFactory.getInstance();
			else {
				try {
					Class<?> c = Class.forName(className);
					Method getI = c.getMethod(INSTANCE_METHOD);
					factory = (LoggerBridgeFactory) getI.invoke(null);
				} catch (Exception e) {
					throw new IllegalStateException(msg(U.U00300, LOGGER_BRIDGE_FACTORY, className));
				}
			}
		}
		return factory;
	}
	
	/**
	 * Constructor.
	 */
	private LoggerManager() {
	}

	/**
	 * Get a logger bridge by name.
	 * 
	 * @param name a string, non-null
	 * @return a logger bridge
	 */
	public static LoggerBridge getLogger(String name) {
		return getFactory().getLogger(name);
	}

	/**
	 * Get a logger bridge by class.
	 * 
	 * @param klass a class, non-null
	 * @return a logger bridge
	 */
	public static LoggerBridge getLogger(Class<?> klass) {
		return getFactory().getLogger(klass);
	}

	
	
}
