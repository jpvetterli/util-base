package ch.agent.util.ioc;

import ch.agent.util.args.Args;

/**
 * A module is an adapter allowing an underlying object to be configured and
 * manipulated in a standard way. Modules are created and managed by a
 * {@link Container}. The container expects a module constructor to have a
 * single parameter: the module name.
 * 
 * @param <T>
 *            the type of the underlying object
 */
public interface Module<T> {

	/**
	 * Get the module name.
	 * 
	 * @return a string
	 */
	String getName();
	
	/**
	 * Define configuration parameters.
	 * 
	 * @param config
	 *            the configuration object
	 */
	void define(Args config);

	/**
	 * Configure the module. This method is called before {@link #add}. If the
	 * method throws an exception, the exception's message is logged by the
	 * container as an error, and after all modules have been parameterized, the
	 * container throws an exception.
	 * <p>
	 * This method can be called only once.
	 * 
	 * @param config
	 *            the configuration
	 * @throws Exception
	 *             if there is an error
	 */
	void configure(Args config) throws Exception;
	
	/**
	 * Add a required module. This method is called for all modules, as required
	 * by the configuration, after {@link #configure} is called.
	 * 
	 * @param module
	 *            a module, already parameterized
	 * @return true if the module is accepted else false
	 */
	boolean add(Module<?> module);
	
	/**
	 * Get the underlying object implementing the module.
	 * 
	 * @return an object
	 * @throws IllegalStateException
	 *             if the object is not yet available
	 */
	T getObject();
	
	/**
	 * Start execution of the underlying object implementing the module. The
	 * application configuration declares one and only one module as the module
	 * to be started when the configuration is ready. 
	 * <p>
	 * This method can be called only once.
	 * 
	 * @return an exit code
	 */
	int start();
	
	/**
	 * Stop execution of the underlying object implementing the module.
	 * <p>
	 * This method can be called only once. It is possible that the method will
	 * be called even after the module has thrown an exception.
	 * 
	 * @return return true if stopping is supported, else false
	 */
	boolean stop();
	
	/**
	 * Remove a required module. This method is called by the container when
	 * dynamically removing a module. The container will not remove the module
	 * when this method returns false.
	 * 
	 * @param module
	 *            a module, already parameterized
	 * @return true if removal accepted else false
	 */
	boolean remove(Module<?> module);
	
	/**
	 * Update the module configuration. This method is called by the container
	 * when dynamically changing the configuration and can be called at any time
	 * after {@link #start}. If the method throws an
	 * {@link IllegalArgumentException}, the exception's message is logged as an
	 * error by the container, and processing continue. For any other exception,
	 * the message is logged, and the container throws an exception. The method
	 * returns false when updating the configuration is not supported.
	 * 
	 * @param config
	 *            the configuration
	 * @return true if updating is supported, else false
	 * @throws Exception
	 *             if there is an error
	 */
	boolean update(Args config) throws Exception;

}
