package ch.agent.util.logging;

/**
 * Interface for all logger bridge factories. For the motivation, read the class
 * comment of {@link LoggerBridge}.
 * 
 */
public interface LoggerBridgeFactory {

	/**
	 * Get a logger bridge by name.
	 * 
	 * @param name a string, non null
	 * @return a logger bridge
	 */
	LoggerBridge getLogger(String name);
	
	/**
	 * Get a logger bridge by class.
	 * 
	 * @param klass a class, non null
	 * @return a logger bridge
	 */
	LoggerBridge getLogger(Class<?> klass);
	
}
