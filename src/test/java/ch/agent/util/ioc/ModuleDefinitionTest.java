package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.agent.util.args.Args;

public class ModuleDefinitionTest {
	
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

	public static class FooBarModuleDefinition extends FooModuleDefinition {

		private final boolean isBar;
		
		public FooBarModuleDefinition(String name, boolean isFoo, boolean isBar, String className, String[] required, String[] predecessors) {
			super(name, isFoo, className, required, predecessors);
			this.isBar = isBar;
		}

		public boolean isBar() {
			return isBar;
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
	
	public static class FooBarModuleDefinitionBuilder<T extends FooBarModuleDefinition> extends FooModuleDefinitionBuilder<T> {

		@SuppressWarnings("unchecked")
		@Override
		public T build(Args p) {
			FooModuleDefinition md = super.build(p);
			return (T) new FooBarModuleDefinition(
					md.getName(),
					md.isFoo(),
					p.getVal("bar").booleanValue(),
					md.getClassName(), 
					md.getRequirements(), 
					md.getPredecessors());
		}
		
		@Override
		public void defineSyntax(Args p) {
			super.defineSyntax(p);
			p.def("bar").init("false");
		}
		
	}
	
	public void setUp() throws Exception {
	}

	@Test
	public void test1() {
		ModuleDefinitionBuilder<FooModuleDefinition> mdb = new FooModuleDefinitionBuilder<FooModuleDefinition>();
		FooModuleDefinition md = mdb.build("name=foo class=bar foo=true require=x require=y pred=a");
		assertEquals("foo", md.getName());
		assertEquals(true, md.isFoo());
		assertEquals("x", md.getRequirements()[0]);
		assertEquals("y", md.getRequirements()[1]);
		assertEquals("a", md.getPredecessors()[0]);
	}

	@Test
	public void test2() {
		ModuleDefinitionBuilder<FooBarModuleDefinition> mdb = new FooBarModuleDefinitionBuilder<FooBarModuleDefinition>();
		FooBarModuleDefinition md = mdb.build("bar=true name=foo class=bar require=x require=y pred=a");
		assertEquals("foo", md.getName());
		assertEquals(false, md.isFoo());
		assertEquals(true, md.isBar());
		assertEquals("x", md.getRequirements()[0]);
		assertEquals("y", md.getRequirements()[1]);
		assertEquals("a", md.getPredecessors()[0]);
	}

}
