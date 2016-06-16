package ch.agent.util.ioc;

import static ch.agent.util.UtilMsg.lazymsg;
import static ch.agent.util.UtilMsg.msg;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.agent.util.UtilMsg.U;
import ch.agent.util.args.Args;

/**
 * A container is used to configure and start a system of modules. The container
 * itself is configured using an {@link Args}-style configuration file with
 * multiple <em>module</em> statements and a single <em>config</em> statement.
 * <p>
 * 
 * <pre>
 * module=[name=<em>module-name</em> start?=true|false class=<em>class-name</em> require*=<em>module-name</em>]
 * </pre>
 * 
 * There is one <em>module</em> statement for each module. Exactly one of them
 * must be configured as the <em>start</em> module (the parameter is optional
 * and false by default). A module is an object of the class with the name
 * specified in the statement. The class must implement the {@link Module}
 * interface. A module can require zero or more other modules. The modules are
 * created immediately in their order of definition.
 * <p>
 * Modules are processed in the order of their declarations. When shutting down
 * the container, the {@link Module#stop} method of all modules are called.
 * Exceptions occurring in stop are discarded.
 * 
 * <pre>
 * config=[
 *   module-name1=[...]
 *   module-name2=[...]
 *   ...
 * ]
 * </pre>
 * 
 * There is a single <em>config</em> parameter. Its value is parsed when all
 * modules have been instantiated and parameters defined. Inside <em>config</em>
 * the names on the left of the equal signs are the module names previously
 * given in the module statements. A given module can appear multiple times
 * inside <em>config</em> only if it is not a <em>required</em> module.
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
	
	private class ModuleConfiguration {
		private final String name;
		private final boolean start;
		private final String className;
		private final String[] requires; // array of module names
		private Collection<String> requiredBy;
		private Module<?> module;
		
		public ModuleConfiguration(String name, boolean start, String className, String[] requires) {
			super();
			this.name = name;
			this.start = start;
			this.className = className;
			this.requires = requires;
			this.requiredBy = new ArrayList<String>();
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
	
	private Map<String, ModuleConfiguration> modules; // key is module name 
	private Map<String, Args> moduleArgs; // key is module class name
	private String config;
	private String startModule;
	
	public Container() {
		modules = new LinkedHashMap<String, ModuleConfiguration>(); // keep sequence
		moduleArgs = new HashMap<String, Args>();
	}
	
	/**
	 * Run the application.
	 * 
	 * @param parameters
	 *            an array of command line parameters
	 * @return the exit code
	 */
	public int run(String[] parameters) {
		long start = System.currentTimeMillis();
		logger.info("{}", lazymsg(U.C20, Arrays.toString((String[]) parameters)));
		
		parseConfiguration(parameters);
		validateConfiguration();
		instantiateModules();
		parameterize(defineModuleArgs());
		addRequiredModules();
		int exitCode = modules.get(startModule).module.start();
		
		logger.info("{}", lazymsg(U.C21, dhms(System.currentTimeMillis() - start)));
		return exitCode;
	}

	public void shutdown() {
		for (ModuleConfiguration mc : modules.values()) {
			try {
				mc.module.stop();
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
		args.def(CONFIG);
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
	 * Parse the command line arguments.
	 * 
	 * @param arguments
	 *            an array of strings
	 */
	protected void parseConfiguration(String[] arguments) {
		Args args = defineTopLevelSyntax();
		args.parse(arguments);
		String[] moduleStatements = args.getVal(MODULE).stringArray(); 
		config = args.get(CONFIG);
		
		args = defineModuleStatementSyntax();
		for (String m : moduleStatements) {
			args.reset();
			args.parse(m);
			String name = args.get(MODULE_NAME);
			modules.put(name, 
				new ModuleConfiguration(name, 
					args.getVal(MODULE_START).booleanValue(),
					args.get(MODULE_CLASS),
					args.getVal(MODULE_REQUIRE).stringArray()
				)
			);
		}
	}
	
	/**
	 * Validate the module configuration. The method finds all errors before
	 * throwing an exception.
	 * 
	 * @throws IllegalArgumentException
	 *             if configuration not valid
	 */
	protected void validateConfiguration() {
		int errors = 0;
		startModule = null;
		for (ModuleConfiguration mc : modules.values()) {
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
				modules.get(req).requiredBy.add(mc.name);
			}
		}
		if (errors > 0)
			throw new IllegalArgumentException(msg(U.C04));
	}
	
	/**
	 * Create the modules.
	 * 
	 * @throws IllegalArgumentException
	 *             on first constructor failure
	 */
	protected void instantiateModules() {
		for (ModuleConfiguration mc : modules.values()) {
			mc.create();
		}
	}
	
	/**
	 * Add required modules.
	 */
	protected void addRequiredModules() {
		int errors = 0;
		for (ModuleConfiguration mc : modules.values()) {
			for (String req : mc.requires) {
				if (!mc.module.add(modules.get(req).module)) {
					logger.error(msg(U.C05, mc.name, req));
					errors++;
				}
			}
		}
		if (errors > 0)
			throw new IllegalArgumentException(msg(U.C04));
	}

	/**
	 * Define the module names as parameters. The name is defined as a list
	 * parameter if it is required by no other module, else it is defined as a
	 * simple parameter (because it is not clear which instance would be passed
	 * to the requirer.
	 * 
	 * @return a parameter object
	 */
	protected Args defineModuleArgs() {
		Args args = new Args();
		for (ModuleConfiguration mc : modules.values()) {
			if (!moduleArgs.containsKey(mc.className)) {
				if (mc.requiredBy.size() == 0)
					args.def(mc.name);
				else
					args.defList(mc.name);
				Args ma = new Args();
				mc.module.define(ma);
				moduleArgs.put(mc.className, ma);
			}
		}
		return args;
	}
	
	/**
	 * Parameterize all modules.
	 * 
	 * @param args the parameter object
	 */
	protected void parameterize(Args args) {
		int errors = 0;
		args.parse(config);
		for (ModuleConfiguration mc : modules.values()) {
			Args moduleArgs = this.moduleArgs.get(mc.className);
			moduleArgs.reset();
			if (mc.requiredBy.size() == 0) {
				if (!parameterize(mc.module, moduleArgs, args.get(mc.name)))
					errors++;
			} else {
				String[] moduleConfig = args.getVal(mc.name).stringArray();
				for (String s : moduleConfig) {
					if (!parameterize(mc.module, moduleArgs, s))
						errors++;
					moduleArgs.reset();
				}
			}
		}
		if (errors > 0)
			throw new IllegalArgumentException(msg(U.C04));
	}
	
	/**
	 * Parse the module configuration and parameterize the module.
	 * 
	 * @param module the module
	 * @param moduleArgs the parameter object
	 * @param config the configuration string
	 * @return true unless an exception was caught
	 */
	private boolean parameterize(Module<?> module, Args moduleArgs, String config) {
		boolean done = true;
		try {
			moduleArgs.parse(config);
			module.configure(moduleArgs);
		} catch (Exception e) {
			logger.error(e.getMessage());
			done = false;
		}
		return done;
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
