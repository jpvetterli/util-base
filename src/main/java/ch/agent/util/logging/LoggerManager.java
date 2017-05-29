package ch.agent.util.logging;

import static ch.agent.util.STRINGS.msg;

import java.lang.reflect.Method;

import ch.agent.util.STRINGS.U;

/**
 * The logger manager manages logger factories. By default, it uses
 * {@link DefaultLoggerBridgeFactory} as the logger bridge factory, which is a
 * minimal implementation writing to {@link System#err}. To override the
 * default, the name of an alternative factory class must be passed as a system
 * property with the key <code>LoggerBridgeFactory</code>. For example logging
 * to SLF4J is achieved with the factory class
 * <code>ch.agent.util.logging.SLF4JLoggerBridgeFactory</code> available when
 * version <code>x.y.z</code> of the library
 * <code>util-base-slf4j-x.y.z.jar</code> is on the classpath.
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
	 * @throws IllegalArgumentException in case of failure to load a logger factory
	 */
	private static synchronized LoggerBridgeFactory getFactory() {
		if (factory == null) {
			String className = null;
			try {
				className = System.getProperty(LOGGER_BRIDGE_FACTORY);
				if (className == null)
					factory = DefaultLoggerBridgeFactory.getInstance();
				else {
					Class<?> c = Class.forName(className);
					Method getI = c.getMethod(INSTANCE_METHOD);
					factory = (LoggerBridgeFactory) getI.invoke(null);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(msg(U.U00300, LOGGER_BRIDGE_FACTORY, className), e);
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
	 * @throws IllegalArgumentException in case of failure to get a logger 
	 */
	public static LoggerBridge getLogger(String name) {
		try {
			return getFactory().getLogger(name);
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.U00303, name), e);
		}
	}

	/**
	 * Get a logger bridge by class.
	 * 
	 * @param klass a class, non-null
	 * @return a logger bridge
	 * @throws IllegalArgumentException in case of failure to get a logger 
	 */
	public static LoggerBridge getLogger(Class<?> klass) {
		try {
			return getFactory().getLogger(klass);
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.U00304, klass == null ? null : klass.getName()), e);
		}
	}
	
}
