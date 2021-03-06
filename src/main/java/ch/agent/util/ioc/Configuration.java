package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import ch.agent.util.STRINGS.U;
import ch.agent.util.args.Args;
import ch.agent.util.base.Misc;
import ch.agent.util.logging.LoggerBridge;
import ch.agent.util.logging.LoggerManager;

/**
 * A configuration is an immutable object encapsulating the information
 * necessary to set up and launch a system of modules.
 * 
 * @param <D>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class Configuration<D extends ModuleDefinition<M>, M extends Module<?>> implements Serializable {

	private static final long serialVersionUID = -8582798552915184875L;

	/**
	 * Callback interface for method
	 * {@link Configuration#extract(ExtractFilter, String...)}.
	 */
	public static interface ExtractFilter {

		/**
		 * Test if a module definition must be included in the extraction.
		 * 
		 * @param def
		 *            a module definition
		 * @return true to include else false
		 */
		boolean include(ModuleDefinition<?> def);
	}

	private static LoggerBridge logger = LoggerManager.getLogger(Configuration.class);

	private final String execution;
	private final Map<String, D> modules;

	/**
	 * Constructor. Module definitions must be in a valid dependency
	 * sequence. A valid sequence is guaranteed when using 
	 * {@link ConfigurationBuilder}.
	 * 
	 * @param definitions
	 *            the list of module definitions in valid initialization
	 *            sequence
	 * @param execution
	 *            the execution specification or null
	 */
	public Configuration(List<D> definitions, String execution) {
		this.execution = Misc.isEmpty(execution) ? null : execution;
		this.modules = new LinkedHashMap<String, D>();
		for (D def : definitions) {
			this.modules.put(def.getName(), def);
		}
	}

	/**
	 * Create all modules. The result is a registry with all modules.
	 * 
	 * @return configuration registry with all modules
	 * @throws IllegalArgumentException
	 *             if two modules have the same name
	 */
	public ConfigurationRegistry<M> create() {
		ConfigurationRegistry<M> registry = new ConfigurationRegistry<M>();
		for (D def : getModuleDefinitions()) {
			M module = def.create();
			if (registry.getModules().put(def.getName(), module) != null)
				throw new IllegalArgumentException(msg(U.C55, def.getName()));
		}
		logger.debug(lazymsg(U.C30, Misc.join("\", \"", registry.getModules().keySet())));
		return registry;
	}

	/**
	 * Configure all modules in the registry. See
	 * {@link ModuleDefinition#configure(Module, ConfigurationRegistry)}
	 * and {@link Module#configure(String)}.
	 * 
	 * @param registry
	 *            configuration registry
	 * @throws IllegalArgumentException
	 *             if there is a configuration failure
	 */
	public void configure(ConfigurationRegistry<M> registry) {
		for (M module : registry.getModules().values()) {
			getModuleDefinition(module.getName()).configure(module, registry);
		}
		logger.debug(lazymsg(U.C31, Misc.join("\", \"", registry.getModules().keySet())));
	}

	/**
	 * Initialize all modules in the registry.
	 * See {@link Module#initialize()}.
	 * 
	 * @param registry
	 *            a configuration registry
	 * @throws Exception
	 *             if a module initialization method fails
	 */
	public void initialize(ConfigurationRegistry<M> registry) throws Exception {
		for (M module : registry.getModules().values()) {
			module.initialize();
		}
		logger.debug(lazymsg(U.C32, Misc.join("\", \"", registry.getModules().keySet())));
	}

	/**
	 * Parse the execution statement of the configuration in the context of
	 * command specifications. On the execution statement, see
	 * {@link #getExecution()}.
	 * 
	 * @param specifications
	 *            a collection of command specifications
	 * @return a collection of executable command specifications
	 * @throws IllegalArgumentException
	 *             if there is parsing failure
	 */
	public Collection<ExecutableCommandSpecification> parseCommands(Collection<CommandSpecification> specifications) {
		List<ExecutableCommandSpecification> executables = new ArrayList<ExecutableCommandSpecification>();
		Map<String, CommandSpecification> map = new HashMap<String, CommandSpecification>(specifications.size());
		Args syntax = new Args();
		for (CommandSpecification spec : specifications) {
			if (spec.isParameterless())
				syntax.def(spec.getName()).init(Args.FALSE);
			else
				syntax.def(spec.getName()).repeatable().init("");
			map.put(spec.getName(), spec);
		}
		List<String[]> sequence = new ArrayList<String[]>();
		try {
			if (getExecution() != null)
				syntax.parse(getExecution(), sequence);
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(U.C24), e);
		}
		for (String[] statement : sequence) {
			CommandSpecification spec = map.get(statement[0]);
			if (spec.isParameterless() && statement.length > 1)
				throw new IllegalArgumentException(msg(U.C25, statement[0], statement[1]));
			executables.add(new ExecutableCommandSpecification(spec, statement.length == 2 ? statement[1] : ""));
		}
		return executables;
	}

	/**
	 * Execute commands. Executable command specifications are provided by
	 * {@link #parseCommands(Collection)}.
	 * 
	 * @param registry
	 *            the configuration registry
	 * @param executables
	 *            a collection of executable command specifications
	 * @throws Exception
	 *             when a command fails
	 */
	public void executeCommands(ConfigurationRegistry<M> registry, Collection<ExecutableCommandSpecification> executables) throws Exception {
		for (ExecutableCommandSpecification spec : executables) {
			try {
				registry.getModules().get(spec.getModule()).execute(spec.getCommand(), spec.getParameters());
			} catch (Exception e) {
				throw new Exception(msg(U.C22, spec.getCommand(), spec.getModule(), spec.getParameters()), e);
			}
		}
	}

	/**
	 * Shutdown all modules in the registry. The shutdown sequence is the
	 * reverse of the initialization sequence. See {@link Module#shutdown()}.
	 * 
	 * @param registry
	 *            a configuration registry
	 */
	public void shutdown(ConfigurationRegistry<M> registry) {
		Collection<M> coll = registry.getModules().values();
		Module<?>[] array = coll.toArray(new Module<?>[coll.size()]);
		for (int i = array.length - 1; i >= 0; i--) {
			array[i].shutdown();
		}
	}

	/**
	 * Get a copy of all module names in valid initialization sequence.
	 * 
	 * @return a list of module names
	 */
	public List<String> getModuleNames() {
		return new ArrayList<String>(modules.keySet());
	}

	/**
	 * Get a copy of all module definitions in valid initialization sequence.
	 * 
	 * @return a list of module definitions
	 */
	public List<D> getModuleDefinitions() {
		return new ArrayList<D>(modules.values());
	}

	/**
	 * Return the number of module definitions.
	 * 
	 * @return a non-negative number
	 */
	public int getModuleCount() {
		return modules.size();
	}

	/**
	 * Get a module definition.
	 * 
	 * @param name
	 *            the module name
	 * @return the module specification or null if none such
	 */
	public D getModuleDefinition(String name) {
		return modules.get(name);
	}

	/**
	 * Return the execution specification. The execution specification is an
	 * opaque block of text which contains instructions on module commands to be
	 * executed after all modules have been initialized. The instruction for a
	 * given command can be extracted using the command name. The details of how
	 * to perform this extraction is not the responsibility of this object.
	 * 
	 * @return the execution string or null
	 */
	public String getExecution() {
		return execution;
	}

	/**
	 * Extract sub-configuration for a selection of modules. The
	 * sub-configuration includes the modules and all their direct and indirect
	 * prerequisites.
	 * <p>
	 * The execution specification, which belongs to the top-level
	 * configuration, is not included.
	 * 
	 * @param <T>
	 *            the type of configuration returned
	 * @param filter
	 *            extraction filter or null
	 * @param module
	 *            names of zero or more top modules to include in the
	 *            sub-configuration
	 * @return a configuration
	 * @throws NoSuchElementException
	 *             when a module is unknown
	 */
	@SuppressWarnings("unchecked")
	public <T extends Configuration<D, M>> T extract(ExtractFilter filter, String... module) {
		Set<String> scope = new HashSet<String>();
		for (String name : module) {
			D def = getModuleDefinition(name);
			if (def == null)
				throw new NoSuchElementException(name);
			add(def, scope);
		}
		List<D> extract = new ArrayList<D>(scope.size());
		for (D def : getModuleDefinitions()) {
			if (scope.contains(def.getName()) && (filter == null || filter.include(def)))
				extract.add(def);
		}
		return (T) new Configuration<D, M>(extract, null);
	}

	private void add(D def, Set<String> scope) {
		// no risk of stack overflow because original configuration has no cycle
		scope.add(def.getName());
		for (String pre : def.getPrerequisites()) {
			add(getModuleDefinition(pre), scope);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((execution == null) ? 0 : execution.hashCode());
		result = prime * result + ((modules == null) ? 0 : modules.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		Configuration other = (Configuration) obj;
		if (execution == null) {
			if (other.execution != null)
				return false;
		} else if (!execution.equals(other.execution))
			return false;
		if (modules == null) {
			if (other.modules != null)
				return false;
		} else if (!modules.equals(other.modules))
			return false;
		return true;
	}

}
