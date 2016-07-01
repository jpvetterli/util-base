package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.logging.LoggerBridge;
import ch.agent.util.logging.LoggerManager;

/**
 * A container is used to set up and start a system of modules. The container
 * configuration uses a multilevel syntax based on {@link Args} objects. There
 * is a top-level configuration syntax for the container itself and each module
 * has its own configuration syntax, which becomes known only after the module
 * has been instantiated.
 * <p>
 * The top-level syntax consists of multiple <em>module</em> statements, a
 * single <em>config</em> statement and a single <em>exec</em> statement.
 * <p>
 * 
 * <pre>
 * module=[
 *   name=<em>module-name</em> 
 *   class=<em>class-name</em> 
 *   requirement*=<em>module-name</em> (abbreviated require)
 *   predecessor</em>*=<em>module-name</em> (abbreviated pred)
 * ]
 * </pre>
 * 
 * There is one <em>module</em> statement for each module. Names must be unique.
 * A module is an object of the class specified in the statement. The class must
 * implement the {@link Module} interface. A module can declare zero or more
 * other modules as requirements or predecessors. The modules are initialized in
 * a sequence which guarantees that required modules and predecessors are always
 * initialized before the module requiring them (unless a dependency cycle is
 * detected). Only requirements, not predecessors, will be added to the module
 * using {@link Module#add}.
 * <p>
 * When shutting down the container, the {@link Module#shutdown} methods of all
 * modules are called in the reverse order of the initialization sequence.
 * Exceptions occurring during shutdown are discarded.
 * 
 * <pre>
 * config=[
 *   module-name=[...]
 *   module-name=[...]
 *   ...
 * ]
 * </pre>
 * 
 * There is a single <em>config</em> statement. Its value is a sequence of
 * statements named after the module names declared in the <em>module</em>
 * statements. There is at most one statement for each module. The syntax of the
 * module configuration is defined by the module itself.
 * 
 * <pre>
 * exec=[
 *   command-name=[...]
 *   command-name=[...]
 *   ...
 * ]
 * </pre>
 * 
 * There is a single <em>exec</em> statement. Its value is a sequence of
 * statements which are the command names registered by modules during module
 * initialization. The names of the commands are the preferred names given by
 * {@link Command#getName} or, if that name is already registered by another
 * module, the concatenation of the module name and the command name, with a
 * period between them. The syntax of command parameters is defined by the
 * command themselves. The container passes the value verbatim to the
 * {@link Command#execute} methods.
 * <p>
 * The {@link #run} method returns the sum of the exit codes of all commands
 * executed. Because the convention is for a command to return 0 to mean "okay",
 * the final exit code will be also be 0 if everything is "okay".
 * 
 */
public class Container implements CommandRegistry {

	final static LoggerBridge logger = LoggerManager.getLogger(Container.class);

	public static void main(String[] args) {
		Container c =  new Container();
		int exit = 0;
		try {
			c.run(args);
		} catch (Exception e) {
			e.printStackTrace();
			exit = 1;
		} finally {
			c.shutdown();
		}
		System.exit(exit);
	}
	
	private class ModuleSpecification {
		private final String name;
		private final String className;
		private final String[] req; // module names required by this module
		private final String[] pred; // module names preceding but not required
		private String configuration;
		private Module<?> module;
		
		public ModuleSpecification(String name, String className, String[] requires, String[] preceding) {
			if (name.length() == 0)
				throw new IllegalArgumentException("name empty");
			if (className.length() == 0)
				throw new IllegalArgumentException("className empty");
			Set<String> duplicates = new HashSet<String>();
			for (String req : requires) {
				if (name.equals(req))
					throw new IllegalArgumentException(msg(U.C06, name));
				if (!duplicates.add(req))
					throw new IllegalArgumentException(msg(U.C13, name, req));
			}
			for (String prec : preceding) {
				if (name.equals(prec))
					throw new IllegalArgumentException(msg(U.C06, name));
				if (!duplicates.add(prec))
					throw new IllegalArgumentException(msg(U.C13, name, prec));
			}
			
			this.name = name;
			this.className = className;
			this.req = requires;
			this.pred = preceding;
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
		
		protected String[] requirements() {
			return req;
		}
		
		protected String[] predecessors() {
			return pred;
		}

	}
	
	private static final String MODULE = "module";
	private static final String CONFIG = "config";
	private static final String EXEC = "exec";
	private static final String MODULE_NAME = "name";
	private static final String MODULE_CLASS = "class";
	private static final String MODULE_REQUIREMENT = "requirement";
	private static final String MODULE_REQUIREMENT_AKA = "require";
	private static final String MODULE_PREDECESSOR = "predecessor";
	private static final String MODULE_PREDECESSOR_AKA = "pred";
	
	private Map<String, ModuleSpecification> modules; // key is module name 
	private Map<String, Command<?>> commands; // key is the actual command name 
	private List<String> initSequence; // is null in case of cycles
	private String exec;
	private long start; // start time of the #run method
	
	public Container() {
		modules = new LinkedHashMap<String, ModuleSpecification>(); // keep sequence
		commands = new HashMap<String, Command<?>>();
	}
	
	/**
	 * Get a named module.
	 * <p>
	 * <em>This method is provided to support unit testing.</em>
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
	 * Configure and execute the modules. Any {@link Exception} during
	 * processing is caught and thrown again, after logging a termination
	 * message followed by all exception messages in the cause chain. If a stack
	 * trace is wanted, it can be produced by catching the exception thrown by
	 * {@link #run}.
	 * <p>
	 * The method does not perform the {@link #shutdown} because some
	 * applications or unit tests need to access module data after {@link #run}
	 * has returned or has been interrupted by an exception. Performing the
	 * {@link #shutdown} is therefore the responsibility of the client.
	 * 
	 * @param parameters
	 *            an array of command line parameters
	 * @throws Exception
	 *             anything can happen during execution
	 */
	public void run(String[] parameters) throws Exception {
		start = System.currentTimeMillis();
		logger.info(lazymsg(U.C20, Arrays.toString((String[]) parameters)));
		try {
			parseConfiguration(parameters);
			validateConfiguration();
			computeValidSequence();
			initializeModules();
			executeCommands(exec);
		} catch (Exception e) {
			logger.error(msg(U.C23, e.getClass().getSimpleName()));
			Throwable cause = e;
			while (cause != null) {
				logger.error(cause.getMessage());
				cause = cause == cause.getCause() ? null : cause.getCause();
			}
			throw e;
		}
	}
	
	/**
	 * Method hook for subclasses. The method is called before a module is
	 * initialized.
	 */
	public void preModuleInitialization(Module<?> module) {
	}

	/**
	 * Method hook for subclasses. The method is called after a module has been
	 * initialized and before any command is executed.
	 */
	public void postModuleInitialization(Module<?> module) {
	}
	
	/**
	 * Shutdown all modules. The sequence is the reverse of the initialization
	 * sequence.
	 */
	public void shutdown() {
		if (initSequence != null) {
			List<String> shutDownSequence = new ArrayList<String>(initSequence);
			Collections.reverse(shutDownSequence);
			for (String name : shutDownSequence) {
				try {
					modules.get(name).module.shutdown();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		logger.info(lazymsg(U.C21, dhms(System.currentTimeMillis() - start)));
	}
	
	@Override
	public String register(Command<?> command) {
		String name = command.getName();
		Command<?> existing = commands.get(name);
		if (existing == null)
			commands.put(name, command);
		else {
			String moduleName = command.getModule().getName();
			if (existing.getModule().getName().equals(moduleName))
				throw new IllegalStateException(msg(U.C12, moduleName, name));
			else {
				name = moduleName + "." + name;
				if (commands.get(name) != null)
					throw new RuntimeException("bug found " + name);
				else
					commands.put(name, command);
			}
		}
		return name;
	}

	/**
	 * Define the top level parameters.
	 * 
	 * @return a parameter object
	 */
	protected Args defineTopLevelSyntax() {
		Args args = new Args();
		args.defList(MODULE);
		args.def(CONFIG).init(""); // can be omitted
		args.def(EXEC).init(""); // can be omitted
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
		args.def(MODULE_CLASS);
		args.defList(MODULE_REQUIREMENT).aka(MODULE_REQUIREMENT_AKA);
		args.defList(MODULE_PREDECESSOR).aka(MODULE_PREDECESSOR_AKA);
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
		exec = topSyntax.get(EXEC);
		
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
						name, moduleSyntax.get(MODULE_CLASS), 
						moduleSyntax.getVal(MODULE_REQUIREMENT).stringArray(),
						moduleSyntax.getVal(MODULE_PREDECESSOR).stringArray()));
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
		for (ModuleSpecification mc : modules.values()) {
			for (String req : mc.requirements()) {
				if (!modules.containsKey(req)) {
					errors++;
					logger.error(msg(U.C02, req, mc.name));
				}
			}
			for (String req : mc.predecessors()) {
				if (!modules.containsKey(req)) {
					errors++;
					logger.error(msg(U.C02, req, mc.name));
				}
			}
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
			dag.addLinks(spec.name, spec.requirements());
			dag.addLinks(spec.name, spec.predecessors());
		}
		// ... except for a possible cycle
		try {
			initSequence = dag.sort();
		} catch (Exception e) {
			throw new Exception(msg(U.C09), e);
		}
	}

	/**
	 * Initialize all modules following a sequence which guarantees that a
	 * required module is initialized before a module which requires it.
	 * 
	 * @throws Exception
	 *             as soon as the initialization of a module fails
	 */
	protected void initializeModules() throws Exception {
		for (String name : initSequence) {
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
			spec.module.configure(spec.configuration);
			addRequiredModules(spec);
			spec.module.registerCommands(this);
			spec.module.initialize();
			logger.info(msg(U.C08, spec.name));
		} catch (Exception e) {
			throw new Exception(msg(U.C07, spec.name), e);
		}
	}

	/**
	 * Execute all commands in the exec statement.
	 * 
	 * @param exec
	 *            the value of the exec statement
	 * @throws Exception execution of commands can throw exceptions
	 */
	protected void executeCommands(String exec) throws Exception {
		Args execSyntax = new Args();
		for (String commandName : commands.keySet()) {
			execSyntax.defList(commandName); // a command can be executed 0 or more times
		}
		execSyntax.setSequenceTrackingMode(true);
		execSyntax.parse(exec);
		List<String[]> statements = execSyntax.getSequence();
		for (String[] statement : statements) {
			Command<?> command = commands.get(statement[0]);
			try {
				command.execute(statement[1]);
			} catch (Exception e) {
				throw new Exception(msg(U.C22, command.getName(), command.getModule().getName(), statement[1]), e);
			}
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
		for (String req : spec.requirements()) {
			ModuleSpecification reqSpec = modules.get(req);
			if (reqSpec.module == null)
				throw new RuntimeException("bug found, module \"" + req + "\" required by \"" + spec.name + "\" is null");
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
