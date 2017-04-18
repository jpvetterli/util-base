package ch.agent.util.ioc;

import static ch.agent.util.STRINGS.lazymsg;
import static ch.agent.util.STRINGS.msg;

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
 * <p>
 * The configuration consists of:
 * <ul>
 * <li>a list of module definitions in a sequence compatible with dependency
 * constraints,
 * <li>an <em>execution</em> specification.
 * </ul>
 * 
 * @param <D>
 *            the module definition type
 * @param <M>
 *            the module type
 */
public class Configuration<D extends ModuleDefinition<M>, M extends Module<?>> {

	private static LoggerBridge logger = LoggerManager.getLogger(Configuration.class);

	private final String execution;
	private final Map<String, D> modules;

	/**
	 * Constructor. Modules must be provided in a valid dependency sequence.
	 * 
	 * @param definitions
	 *            the list of module definitions in valid initialization sequence
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
	 * Create and configure all modules. The result is a registry with all the
	 * configured modules and all commands registered by them.
	 * 
	 * @return configuration registry with all modules and commands
	 * @throws Exception
	 *             thrown by the module initialize method
	 */
	public ConfigurationRegistry<M> configure() throws Exception {
		ConfigurationRegistry<M> registry = new ConfigurationRegistry<M>();
		for (D def : getModuleDefinitions()) {
			M module = def.configure(registry);
			if (registry.getModules().put(def.getName(), module) != null)
				throw new IllegalStateException(msg(U.C55, def.getName()));
		}
		logger.debug(lazymsg(U.C18, Misc.join("\", \"", registry.getModules().keySet())));
		return registry;
	}
	
	/**
	 * Initialize all modules.
	 * 
	 * @param registry a configuration registry
	 * @throws Exception by a module initialize method
	 */
	public void initialize(ConfigurationRegistry<M> registry) throws Exception {
		for (M module : registry.getModules().values()) {
			module.initialize();
		}
	}
	
	/**
	 * Parse the execution statement of the configuration. Command
	 * specifications must have already been initialized.
	 * 
	 * @param specifications
	 *            a collection of command specifications
	 * @return a collection of executable command specifications
	 */
	public Collection<ExecutableCommandSpecification> parseCommands(Collection<CommandSpecification> specifications) {
		List<ExecutableCommandSpecification> executables = new ArrayList<ExecutableCommandSpecification>();
		Map<String, CommandSpecification> map = new HashMap<String, CommandSpecification>(specifications.size());
		Args syntax = new Args();
		syntax.setLoose(logger);
		for (CommandSpecification spec : specifications) {
			if (spec.isParameterless())
				syntax.def(spec.getName()).init(Args.FALSE);
			else
				syntax.defList(spec.getName());
			map.put(spec.getName(), spec);
		}
		syntax.setSequenceTrackingMode(true);
		try {
			if (getExecution() != null)
				syntax.parse(getExecution());
		} catch (Exception e) {
			throw new ConfigurationException(msg(U.C24), e);
		}
		for (String[] statement : syntax.getSequence()) {
			CommandSpecification spec = map.get(statement[0]);
			if (spec.isParameterless() && statement[1].length() > 0)
				throw new ConfigurationException(msg(U.C25, statement[0], statement[1]));
			executables.add(new ExecutableCommandSpecification(spec, spec.isParameterless() ? "" : statement[1]));
		}
		return executables;
	}
	
	/**
	 * Execute commands. 
	 * 
	 * @param registry
	 *            the configuration registry
	 * @param executables
	 *            a collection of executable command specifications
	 * @throws Exception
	 */
	public void executeCommands(ConfigurationRegistry<M> registry, Collection<ExecutableCommandSpecification> executables) throws Exception {
		for (ExecutableCommandSpecification spec : executables) {
			try {
				registry.getModules().get(spec.getModule()).execute(spec.getCommand(), spec.getParameters());
			} catch (EscapeException e) {
				throw e;
			} catch (Exception e) {
				throw new Exception(msg(U.C22, spec.getCommand(), spec.getModule(), spec.getParameters()), e);
			}
		}
	}
	
	/**
	 * Shutdown all modules in the registry. The sequence is the reverse from
	 * the initialization sequence.
	 * 
	 * @param registry
	 *            a configuration registry
	 */
	public void shutdown(ConfigurationRegistry<M> registry) {
		List<M> list = new ArrayList<M>(registry.getModules().values());
		int i = list.size();
		while (--i >= 0) {
			try {
				M module = list.get(i);
				module.shutdown();
			} catch (Exception e) {
				// ignore
			}
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
	 * Return the <em>execution</em> specification. The execution specification
	 * is an opaque block of text which contains instructions on module commands
	 * to be executed after all modules have been initialized. The instruction
	 * for a given command can be extracted using the command name. The details
	 * of how to perform this extraction is not the responsibility of this
	 * object.
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
	 * @param module
	 *            names of zero or more top modules to include in the
	 *            sub-configuration
	 * @return a configuration
	 */
	@SuppressWarnings("unchecked")
	public <T extends Configuration<D,M>> T extract(String... module) {
		Set<String> scope = new HashSet<String>(); 
		for (String name : module) {
			D def = getModuleDefinition(name);
			if (def == null)
				throw new NoSuchElementException(name);
			add(def, scope);
		}
		List<D> extract = new ArrayList<D>(scope.size());
		for (D def : getModuleDefinitions()) {
			if (scope.contains(def.getName()))
				extract.add(def);
		}
		return (T) new Configuration<D,M>(extract, null);
	}
	
	private void add(D def, Set<String> scope) {
		// no risk of stack overflow because original configuration has no cycle
		scope.add(def.getName());
		for (String pre : def.getPrerequisites()) {
			add(getModuleDefinition(pre), scope);
		}
	}


}
