package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;
import ch.agent.util.logging.LoggerBridge;

/**
 * The tool box provides stateless methods useful for implementing containers.
 * 
 * @param <C>
 *            the configuration type
 * @param <B>
 *            the module definition builder type
 * @param <D>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class ContainerToolBox<C extends Configuration<D, M>, B extends ModuleDefinitionBuilder<D, M>, D extends ModuleDefinition<M>, M extends Module<?>> {

	/**
	 * A helper class to keep track of the life cycle of a module.
	 *
	 * @param <M> the module type
	 */
	public static class ManagedModule<D,M> {
		private final D definition;
		private final M module;
		private boolean initialized;
		private boolean shutdown;

		public ManagedModule(D definition, M module) {
			super();
			this.definition = definition;
			this.module = module;
		}
		
		public D getModuleDefinition() {
			return definition;
		}

		public M getModule() {
			return module;
		}

		public boolean isInitialized() {
			return initialized;
		}
		
		public void setInitialized() {
			this.initialized = true;
		}
		
		public boolean isShutdown() {
			return shutdown;
		}
		
		public void setShutdown() {
			this.shutdown = true;
		}

		@Override
		public String toString() {
			return getModule().toString();
		}
		
	}
	
	/**
	 * Command registry used during module initialization.
	 *
	 */
	public static class SimpleCommandRegistry implements CommandRegistry {

		private Map<String, Command<?>> commands; // key is the actual command name 
		
		public SimpleCommandRegistry() {
			commands = new HashMap<String, Command<?>>();
		}

		@Override
		public void register(Command<?> command) {
			String name = command.getFullName();
			Command<?> existing = commands.get(name);
			if (existing != null)
				throw new ConfigurationException(msg(U.C12, command.getModule().getName(), command.getFullName()));
			commands.put(name, command);
		}
	
		@Override
		public Map<String, Command<?>> getCommands() {
			return commands;
		}
		
	}
	
	private LoggerBridge logger;
	
 	/**
	 * Constructor.
	 * 
	 * @param logger a logger or null
	 */
	public ContainerToolBox(LoggerBridge logger) {
		this.logger = logger;
	}
	
	/**
	 * Shutdown all modules. The shutdown sequence is the reverse from
	 * the initialization sequence.
	 * 
	 * @param modules
	 *            map of managed modules in th
	 */
	public void shutdown(Map<String, ManagedModule<D,M>> modules) {
		List<M> list = asList(modules);
		int i = modules.size();
		while (--i >= 0) {
			try {
				M module = list.get(i);
				module.shutdown();
				modules.get(module.getName()).setShutdown();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	/**
	 * Create and configure all modules.
	 * <p>
	 * The method creates a little {@link Args} language to parse the
	 * <em>configuration</em> specification. In this language, parameter names
	 * are the module names, their expected value is a string, and they can be
	 * omitted. As an example, if there are three modules A, B, and C, the
	 * following configuration will be parsed successfully:
	 * 
	 * <pre>
	 * <code>
	 * configuration=[
	 *   B = [<em>configuration details for module B</em>]
	 *   A = [<em>configuration details for module A</em>]
	 *   # C is omitted
	 * ]
	 * </code>
	 * </pre>
	 * 
	 * @param configuration
	 *            the configuration object
	 * @return a sequential map of managed of modules in valid sequence
	 * @throws Exception
	 *             as soon as creating or configuring a module fails
	 */
	public Map<String, ManagedModule<D, M>> configureModules(C configuration) throws Exception {
		Map<String, ManagedModule<D, M>> modules = new LinkedHashMap<String, ManagedModule<D, M>>();
		Args moduleConfig = new Args();
		for (D module : configuration.getModuleDefinitions()) {
			moduleConfig.def(module.getName()).init(""); // it is okay to omit the statement
		}
		moduleConfig.parse(configuration.getConfiguration());
		
		for (D spec : configuration.getModuleDefinitions()) {
			try {
				M m = spec.create();
				m.configure(moduleConfig.get(spec.getName()));
				modules.put(spec.getName(), new ManagedModule<D,M>(spec, m));
			} catch (Exception e) {
				throw new ConfigurationException(msg(U.C14, spec.getName()), e);
			}
		}
		return modules;
	}

	/**
	 * Initialize a module. Initialization consists of the following steps:
	 * <ol>
	 * <li>Required modules are added with {@link Module#add}.
	 * <li>Module commands are registered with{@link Module#registerCommands}.
	 * <li>The module is initialized with {@link Module#initialize}.
	 * </ol>
	 * 
	 * IMPORTANT: required modules must already have been configured.
	 * 
	 * @param module
	 *            the module
	 * @param modules
	 *            map of managed modules keyed by module name
	 * @param registry
	 *            the command registry to use for registering module commands
	 * @throws Exception
	 *             in case of failure
	 */
	public void initializeModule(M module, Map<String, ManagedModule<D,M>> modules, CommandRegistry registry) throws Exception {
		if (modules.get(module.getName()).isInitialized())
			throw new IllegalStateException(String.format("module \"%s\" already initialized", module.getName()));
		try {
			addRequiredModules(module, modules);
			module.registerCommands(registry);
			if (!module.initialize()) {
				if (logger != null)
					logger.warn(lazymsg(U.C25, module.getName()));
			}
		} catch (EscapeException e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(msg(U.C07, module.getName()), e);
		}
		if (logger != null)
			logger.debug(lazymsg(U.C18, module.getName()));
	}

	/**
	 * Execute all commands in the <em>execution</em> specification.
	 * <p>
	 * The method creates a little {@link Args} language to parse the
	 * <em>execution</em> specification. In this language, parameter names are
	 * the command names (possibly prefixed for uniqueness by the module name),
	 * their expected value is a string, and they are repeatable (and can be
	 * omitted). As an example, if there are three commands A.X, B.X, and Y, the
	 * following configuration will be parsed successfully:module
	 * 
	 * <pre>
	 * <code>
	 * execution=[
	 *   A.X = [<em>execution details for command A.X</em>]
	 *   A.X = [<em>possibly different execution details for command A.X</em>]
	 *   Y = [<em>execution details for command Y</em>]
	 *   # B.X is omitted
	 * ]
	 * </code>
	 * </pre>
	 * 
	 * @param configuration
	 *            the non-null configuration object
	 * @param registry
	 *            a non-null command registry
	 * @throws Exception
	 *             execution of commands can throw exceptions
	 */
	public void executeCommands(C configuration, CommandRegistry registry) throws Exception {
		Args execSyntax = new Args();
		Map<String, Command<?>> commands = registry.getCommands();
		for (String commandName : commands.keySet()) {
			execSyntax.defList(commandName); // a command can be executed 0 or more times
		}
		execSyntax.setSequenceTrackingMode(true);
		
		try {
			execSyntax.parse(configuration.getExecution());
		} catch (Exception e) {
			throw new ConfigurationException(msg(U.C24), e);
		}
		
		List<String[]> statements = execSyntax.getSequence();
		for (String[] statement : statements) {
			Command<?> command = commands.get(statement[0]);
			try {
				if (!command.execute(statement[1])) {
					if (logger != null)
						logger.warn(lazymsg(U.C26, command.getName(), command.getModule().getName(), statement[1]));
				}
			} catch (EscapeException e) {
				throw e;
			} catch (Exception e) {
				throw new Exception(msg(U.C22, command.getName(), command.getModule().getName(), statement[1]), e);
			}
		}
	}
	
	/**
	 * Add all modules required by a module. The container must guarantee that
	 * all modules declared as requirement or predecessor have already been
	 * created and configured, and that they have been initialized.
	 * 
	 * @param requiring
	 *            the requiring module
	 * @param modules
	 *            map of managed modules keyed by module name
	 * @throws Exception
	 *             in case of one or more failures
	 * @throws IllegalStateException
	 *             if required module missing or not initialized
	 */
	public void addRequiredModules(M requiring, Map<String, ManagedModule<D,M>> modules) throws Exception {
		ModuleDefinition<M> def =  modules.get(requiring.getName()).getModuleDefinition();
		ensureModulesInitialized(requiring.getName(), def.getPrerequisites(), modules);
		List<String> problematic = new ArrayList<String>();
		// requirements are a subset of prerequisites, so no need to check everything again
		for (String name : def.getRequirements()) {
			ManagedModule<D,M> mm = modules.get(name);
			if (!requiring.add(mm.getModule()))
				problematic.add(name);
		}
		if (problematic.size() > 0)
			throw new ConfigurationException(msg(U.C17, requiring.getName(), Misc.join("\", \"", problematic)));
	}
	
	public void ensureAllModulesInitialized(Map<String, ManagedModule<D,M>> modules) {
		// ensure all modules initialized
		List<String> errors = new ArrayList<String>();
		for (ManagedModule<D,M> mm : modules.values()) {
			if (!mm.isInitialized())
				errors.add(mm.getModule().getName());
		}
		if (errors.size() > 0)
			throw new IllegalStateException("modules not initialized: " + Misc.join("\", \"", errors));
	}
	
	/**
	 * Ensure that the named modules have been initialized.
	 * 
	 * @param requiring
	 *            the requiring module
	 * @param names
	 *            names of modules to check
	 * @param modules
	 *            map of managed modules
	 */
	public void ensureModulesInitialized(String requiring, String[] names, Map<String, ManagedModule<D, M>> modules) {
		// ensure named modules initialized
		List<String> errors = new ArrayList<String>();
		for (String name : names) {
			if (!modules.get(name).isInitialized())
				errors.add(name);
		}
		if (errors.size() > 0)
			throw new IllegalStateException(String.format("module \"%s\" needs the following modules to be initialized: \"%s\"", requiring, Misc.join("\", \"", errors)));
	}
	
	/**
	 * Ensure that the named modules have been shut down.
	 * 
	 * @param requiring
	 *            the requiring module
	 * @param names
	 *            names of modules to check
	 * @param modules
	 *            map of managed modules
	 */
	public void ensureModulesShutdown(String requiring, Iterable<String> names, Map<String, ManagedModule<D, M>> modules) {
		// ensure named modules initialized
		List<String> errors = new ArrayList<String>();
		for (String name : names) {
			if (!modules.get(name).isShutdown())
				errors.add(name);
		}
		if (errors.size() > 0)
			throw new IllegalStateException(String.format("module \"%s\" needs the following modules to be shutdown: \"%s\"", requiring, Misc.join("\", \"", errors)));
	}

	/**
	 * Extract module list from map of managed modules. The list maintains a
	 * sequence compatible with dependency constraints. Return an empty list on
	 * null input.
	 * 
	 * @param modules
	 *            a map of managed modules keyed by module name
	 * @return a list of modules in valid sequence
	 */
	public List<M> asList(Map<String, ManagedModule<D,M>> modules) {
		Misc.nullIllegal(modules, "modules null");
		List<M> result = new ArrayList<M>(modules.size());
		for (ManagedModule<D,M> mm : modules.values()) {
			result.add(mm.getModule());
		}
		return result;
	}
	
}
