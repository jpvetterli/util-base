package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import ch.agent.util.args.Args;

public class ConfigurationTest {
	
	public static class FooModuleDefinition extends ModuleDefinition {

		private final boolean isFoo;
		
		public FooModuleDefinition(String name, boolean isFoo, String className, String[] required, String[] predecessors) {
			super(name, className, required, predecessors);
			this.isFoo = isFoo;
		}

		public boolean isFoo() {
			return isFoo;
		}
		
	}

	public static class FooModuleDefinitionBuilder<T extends FooModuleDefinition> extends ModuleDefinitionBuilder<T> {

		@SuppressWarnings("unchecked")
		@Override
		public T build(Args p) {
			return (T) new FooModuleDefinition(
					p.get(MODULE_NAME), 
					p.getVal("foo").booleanValue(),
					p.get(MODULE_CLASS), 
					p.getVal(MODULE_REQUIREMENT).stringArray(), 
					p.getVal(MODULE_PREDECESSOR).stringArray());
		}

		@Override
		public void defineSyntax(Args p) {
			super.defineSyntax(p);
			p.def("foo").init("false");
		}
	}
	
	public static class FooConfiguration<T extends FooModuleDefinition> extends Configuration<T> {

		private final String foo;
		
		public FooConfiguration(List<T> modules, String config, String exec, String foo) {
			super(modules, config, exec);
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}
		
	}
	
	public static class FooConfigurationBuilder<C extends FooConfiguration<M>, M extends FooModuleDefinition> extends ConfigurationBuilder<C, M> {

		public FooConfigurationBuilder(FooModuleDefinitionBuilder<M> builder) {
			super(builder);
		}

		@Override
		protected void defineSyntax(Args p) {
			super.defineSyntax(p);
			p.def("foo");
		}

		@SuppressWarnings("unchecked")
		@Override
		protected C build(Args p) {
			Configuration<M> c = super.build(p);
			return (C) new FooConfiguration<M>(c.getModuleDefinitions(), c.getConfiguration(), c.getExecution(), p.get("foo"));
		}
		
	}
	
	public void setUp() throws Exception {
	}

	@Test
	public void test1() {
		try {
			FooModuleDefinitionBuilder<FooModuleDefinition> mdb = new FooModuleDefinitionBuilder<FooModuleDefinition>();
			FooConfigurationBuilder<FooConfiguration<FooModuleDefinition>, FooModuleDefinition> cb = new FooConfigurationBuilder<ConfigurationTest.FooConfiguration<FooModuleDefinition>, ConfigurationTest.FooModuleDefinition>(mdb);
			String spec = 
				"module=[name=a class=aclass require=b foo=true]" + 
				"module=[name=b class=bclass foo=false]" + 
				"config=[config stuff]" + 
				"exec = [exec stuff]" + 
				"foo = [foo stuff]";
			FooConfiguration<FooModuleDefinition> c = cb.build(spec);
			assertEquals("config stuff", c.getConfiguration());
			assertEquals("exec stuff", c.getExecution());
			assertEquals("foo stuff", c.getFoo());
			Iterator<FooModuleDefinition> mdi = c.iterator();
			FooModuleDefinition mb = mdi.next();
			assertEquals("b", mb.getName());
			assertEquals(false, mb.isFoo());
			FooModuleDefinition ma = mdi.next();
			assertEquals(true, ma.isFoo());
			assertEquals(false, mdi.hasNext());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}


}
