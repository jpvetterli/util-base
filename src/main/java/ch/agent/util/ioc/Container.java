package ch.agent.util.ioc;

import static ch.agent.util.UtilMsg.lazymsg;
import static ch.agent.util.UtilMsg.msg;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.agent.util.UtilMsg.U;
import ch.agent.util.args.Args;

/**
 * A container is used to set up and start a system of modules. The container
 * configuration uses a multilevel syntax based on {@link Args} objects. There
 * is a top-level configuration syntax for the container itself and each module
 * has its own configuration syntax, which becomes known only after the module
 * has been instantiated.
 * <p>
 * The top-level syntax consists of multiple <em>module</em> statements and a
 * single <em>config</em> statement.
 * <p>
 * 
 * <pre>
 * module=[
 *   name=<em>module-name</em> 
 *   start?=true|false 
 *   class=<em>class-name</em> 
 *   require*=<em>module-name</em>
 * ]
 * </pre>
 * 
 * There is one <em>module</em> statement for each module. Names must be unique.
 * Exactly one of them must be configured as the <em>start</em> module (the
 * parameter is optional, with a false default value). A module is an object of
 * the class specified in the statement. The class must implement the
 * {@link Module} interface. A module can require zero or more other modules.
 * The modules are initialized in a sequence which guarantees that required
 * modules are always initialized before the module requiring them (unless a
 * dependency cycle is detected).
 * <p>
 * When shutting down the container, the {@link Module#stop} methods of all
 * modules are called in the reverse order of the initialization sequence.
 * Exceptions occurring during shutdown are discarded.
 * 
 * <pre>
 * config=[
 *   module-name1=[...]
 *   module-name2=[...]
 *   ...
 * ]
 * </pre>
 * 
 * There is a single <em>config</em> parameter. Its value is a sequence of
 * statements named after the module names declared in the <em>module</em>
 * statements. There is at most one statement for each module. The syntax of the
 * module configuration is defined by the module itself.
 * 
 */
public class Container {

	final static Logger logger = LoggerFactory.getLogger(Container.class);

	public static void main(String[] args) {
		Container c =  new Container();
		int exit = 0;
		try {
			exit = c.run(args);
		} catch (Exception e) {
			e.printStackTrace();
			exit = 1;
		} finally {
			try {
				c.shutdown();
			} catch (Exception e) {
				// ignore
			}
		}
		System.exit(exit);
	}
	
	private class ModuleSpecification {
		private final String name;
		private final boolean start;
		private final String className;
		private final String[] requires; // array of module names
		private String configuration;
		private Module<?> module;
		
		public ModuleSpecification(String name, boolean start, String className, String[] requires) {
			if (name.length() == 0)
				throw new IllegalArgumentException("name empty");
			if (className.length() == 0)
				throw new IllegalArgumentException("className empty");
			for (String req : requires) {
				if (name.equals(req))
					throw new IllegalArgumentException(msg(U.C06, name));
			}
			this.name = name;
			this.start = start;
			this.className = className;
			this.requires = requires;
		}
		
		protected void create() {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Module<?>> classe = (Class<? extends Module<?>>) Class.forName(className);
				Constructor<? extends Module<?>> constructor = classe.getConstructor(String.class);
				module = (Module<?>) constructor.newInstance(name);
			} catch (Exception e) {
				throw new IllegalArgumentException(msg(U.C03, name, className), e);
			}
		}
		
	}
	
	private static final String CONFIG = "config";
	private static final String MODULE = "module";
	private static final String MODULE_NAME = "name";
	private static final String MODULE_START = "start";
	private static final String MODULE_CLASS = "class";
	private static final String MODULE_REQUIRE = "require";
	
	private Map<String, ModuleSpecification> modules; // key is module name 
	private Map<String, Args> moduleArgs; // key is module class name
	private List<String> sequence;
	private String startModule;
	
	public Container() {
		modules = new LinkedHashMap<String, ModuleSpecification>(); // keep sequence
		moduleArgs = new HashMap<String, Args>();
	}
	
	/**
	 * Get a named module.
	 * 
	 * @param name
	 *            the name of the module
	 * @return a module or null
	 * @throws NoSuchElementException
	 *             if no module with that name was specified
	 */
	public Module<?> getModule(String name) {
		ModuleSpecification s = modules.get(name);
		if (s == null)
			throw new NoSuchElementException("name");
		return s.module;
	}
	
	/**
	 * Run the application.
	 * 
	 * @param parameters
	 *            an array of command line parameters
	 * @return the exit code
	 */
	public int run(String[] parameters) throws Exception {
		long start = System.currentTimeMillis();
		logger.info("{}", lazymsg(U.C20, Arrays.toString((String[]) parameters)));
		
		parseConfiguration(parameters);
		validateConfiguration();
		computeValidSequence();
		initializeModules(sequence);
		int exitCode = modules.get(startModule).module.start();
		
		logger.info("{}", lazymsg(U.C21, dhms(System.currentTimeMillis() - start)));
		return exitCode;
	}

	public void shutdown() {
		List<String> shutDownSequence = new ArrayList<String>(sequence);
		Collections.reverse(shutDownSequence);
		for (String name : shutDownSequence) {
			try {
				modules.get(name).module.stop();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	/**
	 * Define the top level parameters.
	 * 
	 * @return a parameter object
	 */
	protected Args defineTopLevelSyntax() {
		Args args = new Args();
		args.def(CONFIG).init(""); // can be omitted
		args.defList(MODULE);
		return args;
	}
	
	/**
	 * Define the parameters of the top-level module statement.
	 * 
	 * @return a parameter object
	 */
	protected Args defineModuleStatementSyntax() {
		Args args = new Args();
		args.def(MODULE_NAME);
		args.def(MODULE_START).init("false");
		args.def(MODULE_CLASS);
		args.defList(MODULE_REQUIRE);
		return args;
	}
	
	/**
	 * Parse the command line arguments. Parse a series of <em>module</em>
	 * statements and capture the opaque value of the <em>config</em> statement
	 * (opaque meaning the value is not parsed at this point yet). Define the
	 * syntax of <em>config</em>: the parameter names are simply the module
	 * names. Capture the opaque values of all these parameters and save them in
	 * each module specification.
	 * 
	 * @param arguments
	 *            an array of strings
	 * @throws Exception
	 *             in case of errors
	 */
	protected void parseConfiguration(String[] arguments) throws Exception {
		int errors = 0;
		
		// top level parsing
		Args topSyntax = defineTopLevelSyntax();
		topSyntax.parse(arguments);
		String[] moduleStatements = topSyntax.getVal(MODULE).stringArray(); 
		String config = topSyntax.get(CONFIG);
		
		// parse each "module" statement
		Args configSyntax = new Args();
		Args moduleSyntax = defineModuleStatementSyntax();
		for (String m : moduleStatements) {
			moduleSyntax.reset();
			moduleSyntax.parse(m);
			String name = moduleSyntax.get(MODULE_NAME);
			configSyntax.def(name).init(""); // it is okay to omit the statement
			try {
				modules.put(name, new ModuleSpecification(
						name, 
						moduleSyntax.getVal(MODULE_START).booleanValue(), 
						moduleSyntax.get(MODULE_CLASS), 
						moduleSyntax.getVal(MODULE_REQUIRE).stringArray())
				);
			} catch (Exception e) {
				errors++;
				logger.error(e.getMessage());
			}
		}
		
		// parse each statement named after modules
		configSyntax.parse(config);
		for (ModuleSpecification spec : modules.values()) {
			// omitted: value is an empty strings
			spec.configuration = configSyntax.get(spec.name); 
		}
		if (errors > 0)
			throw new Exception(msg(U.C04));
	}
	
	/**
	 * Validate the module configuration. The method finds all errors before
	 * throwing an exception.
	 * 
	 * @throws IllegalArgumentException
	 *             if configuration not valid
	 * @throws Exception
	 *             in case of errors
	 */
	protected void validateConfiguration() throws Exception {
		int errors = 0;
		startModule = null;
		for (ModuleSpecification mc : modules.values()) {
			if (mc.start) {
				if (startModule != null) {
					errors++;
					logger.error(msg(U.C01, mc.name, startModule));
				} else
					startModule = mc.name;
			}
			for (String req : mc.requires) {
				if (!modules.containsKey(req)) {
					errors++;
					logger.error(msg(U.C02, req, mc.name));
				}
			}
		}
		if (startModule == null) {
			errors++;
			logger.error(msg(U.C11));
		}
		if (errors > 0)
			throw new Exception(msg(U.C04));
	}
	
	/**
	 * Determine a valid sequence in which modules can be initialized. At
	 * {@link #shutdown}, the modules are stopped in the inverse sequence.
	 * 
	 * @throws Exception
	 *             if it is impossible to compute a valid sequence
	 */
	protected void computeValidSequence() throws Exception {
		// an exception during construction is a bug because the input is clean ...
		DAG<String> dag = new DAG<String>();
		dag.add(modules.keySet().toArray(new String[modules.size()]));
		for (ModuleSpecification spec : modules.values()) {
			dag.addLinks(spec.name, spec.requires);
		}
		// ... except for a possible cycle
		try {
			sequence = dag.sort();
		} catch (Exception e) {
			throw new Exception(msg(U.C09), e);
		}
	}

	/**
	 * Initialize all modules following a sequence which guarantees that a
	 * required module is initialized before a module which requires it.
	 * 
	 * @param sequence
	 *            a valid sequence of module names
	 * @throws Exception
	 *             as soon as the initialization of a module fails
	 */
	protected void initializeModules(List<String> sequence) throws Exception {
		for (String name : sequence) {
			initializeModule(modules.get(name));
		}
	}

	/**
	 * Intialize a module. Instantiate the module object. Define the module
	 * syntax. Configure the module. Add all required module (which must already
	 * have been initialized).
	 * 
	 * @throws Exception
	 *             in case of failure
	 */
	protected void initializeModule(ModuleSpecification spec) throws Exception {
		try {
			spec.create(); // sets spec.module
			Args config = getConfigurationSyntax(spec);
			config.parse(spec.configuration);
			spec.module.configure(config);
			addRequiredModules(spec);
			logger.info(msg(U.C08, spec.name));
		} catch (Exception e) {
			throw new Exception(msg(U.C07, spec.name), e);
		}
	}

	/**
	 * Add all modules required by a module.
	 * 
	 * @param spec
	 *            specification of the requiring module
	 * @throws Exception
	 *             in case of one or more failures
	 */
	protected void addRequiredModules(ModuleSpecification spec) throws Exception {
		int errors = 0;
		for (String req : spec.requires) {
			ModuleSpecification reqSpec = modules.get(req);
			if (reqSpec.module == null)
				throw new IllegalStateException("bug found, module " + req + " required by " + spec.name + " null");
			else {
				if (!spec.module.add(modules.get(req).module)) {
					logger.error(msg(U.C05, spec.name, req));
					errors++;
				}
			}
		}
		if (errors > 0)
			throw new Exception(msg(U.C10, spec.name));
	}

	/**
	 * Get the configuration syntax object for a module. If not yet available,
	 * create it and store it in a map keyed by the module class (a module with
	 * the same class can be declared multiple times under different names with
	 * a <em>module</em> statement, but the configuration syntax is the same).
	 * 
	 * @param spec
	 *            a module specification
	 * @return a configuration syntax object
	 */
	protected Args getConfigurationSyntax(ModuleSpecification spec) {
		Args config = moduleArgs.get(spec.className);
		if (config == null) {
			config = new Args();
			spec.module.define(config);
			moduleArgs.put(spec.className, config);
		}
		return config;
	}

	/**
	 * Convert milliseconds into string with days, hours, minutes, and seconds.
	 * Leading days and hours are omitted if zero.
	 * 
	 * @param millis milliseconds
	 * @return a string representing days, hours, minutes, and seconds
	 */
	private static String dhms(long t) {
		final int MPD = 24*60*60*1000;
		long days = (t / MPD);
		int s = (int) (t - days * MPD) / 1000;
		int m = s / 60;
		int h = m / 60;
		s = s - m * 60;
		m = m - h * 60;
		String result = "";
		if (days > 0)
			result = String.format("%dd%dh%dm%ds", days, h, m, s);
		else {
			if (h > 0)
				result = String.format("%dh%dm%ds", h, m, s);
			else
				result = String.format("%dm%ds", m, s);
		}
		return result;
	}
	
}
