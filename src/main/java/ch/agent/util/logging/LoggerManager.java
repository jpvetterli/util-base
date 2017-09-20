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
 * to SLF4J is achieved with the <code>ch.agent.util.logging.SLF4JLoggerBridgeFactory</code>, 
 * which must be on the classpath.
 */
public class LoggerManager {

	/**
	 * The System property key for naming the class of a non-default logger
	 * bridge factory.
	 */
	public static final String LOGGER_BRIDGE_FACTORY = "LoggerBridgeFactory";
	private static final String INSTANCE_METHOD = "getInstance";
	
	private final static LoggerBridgeFactory factory = factory(); 
	
	private static LoggerBridgeFactory factory() {
		String className = null;
		try {
			className = System.getProperty(LOGGER_BRIDGE_FACTORY);
			if (className == null)
				return DefaultLoggerBridgeFactory.getInstance();
			else {
				Class<?> c = Class.forName(className);
				Method getI = c.getMethod(INSTANCE_METHOD);
				return (LoggerBridgeFactory) getI.invoke(null);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.U00300, LOGGER_BRIDGE_FACTORY, className), e);
		}
	}
	
	/**
	 * Return the LoggerBridgeFactory instance.
	 * 
	 * @return the LoggerBridgeFactory instance
	 * @throws IllegalArgumentException in case of failure to load a logger factory
	 */
	private static LoggerBridgeFactory getFactory() {
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
