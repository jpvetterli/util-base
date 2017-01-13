package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import ch.agent.util.args.Args;

public class ConfigurationTest {
	
	public static class FooModDef extends ModuleDefinition<Module<?>> {

		private final boolean isFoo;
		
		public FooModDef(String name, boolean isFoo, String className, String[] required, String[] predecessors) {
			super(name, className, required, predecessors);
			this.isFoo = isFoo;
		}

		public boolean isFoo() {
			return isFoo;
		}
		
	}

	public static class FooModDefBldr<D extends FooModDef> extends ModuleDefinitionBuilder<D, Module<?>> {

		@SuppressWarnings("unchecked")
		@Override
		public D build(Args p) {
			return (D) new FooModDef(
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
	
	public static class FooConf<T extends FooModDef> extends Configuration<T, Module<?>> {

		private final String foo;
		
		public FooConf(List<T> modules, String config, String exec, String foo) {
			super(modules, config, exec);
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}
		
	}
	
	public static class FooConfBldr<C extends FooConf<D>, B extends FooModDefBldr<D>, D extends FooModDef> extends ConfigurationBuilder<C, B, D, Module<?>> {

		public FooConfBldr(B builder) {
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
			Configuration<D, Module<?>> c = super.build(p);
			return (C) new FooConf<D>(c.getModuleDefinitions(), c.getConfiguration(), c.getExecution(), p.get("foo"));
		}
		
	}
	
	public void setUp() throws Exception {
	}

	@Test
	public void test1() {
		try {
			FooModDefBldr<FooModDef> mdb = new FooModDefBldr<FooModDef>();
			FooConfBldr<FooConf<FooModDef>, FooModDefBldr<FooModDef>, FooModDef> cb = 
					new FooConfBldr<FooConf<FooModDef>, FooModDefBldr<FooModDef>, FooModDef>(mdb);
			String spec = 
				"module=[name=a class=aclass require=b foo=true]" + 
				"module=[name=b class=bclass foo=false]" + 
				"config=[config stuff]" + 
				"exec = [exec stuff]" + 
				"foo = [foo stuff]";
			FooConf<FooModDef> c = cb.build(spec);
			assertEquals("config stuff", c.getConfiguration());
			assertEquals("exec stuff", c.getExecution());
			assertEquals("foo stuff", c.getFoo());
			Iterator<FooModDef> mdi = c.iterator();
			FooModDef mb = mdi.next();
			assertEquals("b", mb.getName());
			assertEquals(false, mb.isFoo());
			FooModDef ma = mdi.next();
			assertEquals(true, ma.isFoo());
			assertEquals(false, mdi.hasNext());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}


}