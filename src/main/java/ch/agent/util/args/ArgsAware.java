package ch.agent.util.args;

import java.util.List;

/**
 * Classes which can be configured using {@link Args} implement this interface.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public interface ArgsAware {

	public static final String ARG_TYPE_STRING = "string";
	public static final String ARG_TYPE_LIST = "list";
	public static final String ARG_TYPE_INT = "integer";
	public static final String ARG_TYPE_BOOLEAN = "boolean";
	
	/**
	 * Define all parameters.
	 * <p>
	 * <b>Important:</b> Subclasses overriding this method are expected to call
	 * the base class method. The code should be written in such a way that it
	 * is not important whether the base method is called first or last.
	 * 
	 * @param args
	 *            the {@link Args} object used for defining parameters
	 */
	void register(Args args);

	/**
	 * This method is called when parameter values become available. Some
	 * applications call the method more than once, allowing to change the
	 * configuration of a running system.
	 * <p>
	 * <b>Important:</b> Subclasses overriding this method are expected to call
	 * the base class method. The code should be written in such a way that it
	 * is not important whether the base method is called first or last.
	 * 
	 * @param args
	 *            the {@link Args} object with parameters and values
	 */
	void updated(Args args);
	
	/**
	 * This method appends zero or more elements to the usage list. The idea is
	 * to call the base class method and then to add one element for each
	 * parameter. An element must indicate the name and the type of the parameter
	 * and its default value if any. The absence of a default value means the 
	 * parameter is mandatory. 
	 * 
	 * @param usage
	 *            a list of texts describing parameters
	 */
	void usage(List<String> usage);
	
}
