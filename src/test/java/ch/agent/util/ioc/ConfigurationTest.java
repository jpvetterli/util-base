package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import ch.agent.util.args.Args;

public class ConfigurationTest {
	
	public static class FooModDef extends ModuleDefinition<Module<?>> {

		private final boolean isFoo;
		
		public FooModDef(String name, boolean isFoo, String className, String[] required, String[] predecessors, String config) {
			super(name, className, required, predecessors, config);
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
					p.getVal(MODULE_PREDECESSOR).stringArray(),
					p.get(MODULE_CONFIG));
		}

		@Override
		public void defineSyntax(Args p) {
			super.defineSyntax(p);
			p.def("foo").init("false");
		}
	}
	
	public static class FooConf<T extends FooModDef> extends Configuration<T, Module<?>> {

		private final String foo;
		
		public FooConf(List<T> modules, String exec, String foo) {
			super(modules, exec);
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
			return (C) new FooConf<D>(c.getModuleDefinitions(), c.getExecution(), p.get("foo"));
		}
		
	}
	
	private FooConfBldr<FooConf<FooModDef>, FooModDefBldr<FooModDef>, FooModDef> getBuilder() {
		FooModDefBldr<FooModDef> mdb = new FooModDefBldr<FooModDef>();
		return new FooConfBldr<FooConf<FooModDef>, FooModDefBldr<FooModDef>, FooModDef>(mdb);
	}
	
	
	public void setUp() throws Exception {
	}
	
	@Test
	public void test1() {
		try {
			String spec = 
				"module=[name=a class=aclass require=b config=[config stuff] foo=true]" + 
				"module=[name=b class=bclass foo=false]" + 
				"exec = [exec stuff]" + 
				"foo = [foo stuff]";
			FooConf<FooModDef> c = getBuilder().build(spec);
			assertEquals("exec stuff", c.getExecution());
			assertEquals("foo stuff", c.getFoo());
			List<FooModDef> definitions = c.getModuleDefinitions();
			FooModDef mb = definitions.get(0);
			assertEquals("b", mb.getName());
			assertEquals(false, mb.isFoo());
			FooModDef ma = definitions.get(1);
			assertEquals(true, ma.isFoo());
			assertEquals("config stuff", ma.getConfiguration());
			assertEquals(2, definitions.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test2() {
		try {
			String spec = 
				"module=[name=a class=aclass require=b foo=true]" + 
				"module=[name=a class=bclass foo=false]" + 
				"exec = [exec stuff]" + 
				"foo = [foo stuff]";
			getBuilder().build(spec);
			fail("exception expected");
		} catch (Exception e) {
			assertTrue("message C11 missing", e.getCause().getMessage().indexOf("cannot be defined twice") > 0);
		}
	}
	
	@Test
	public void test3() {
		try {
			String spec = 
				"module=[name=a class=aclass require=c config=[config stuff] foo=true]" + 
				"module=[name=b class=bclass require=a foo=false]" + 
				"module=[name=c class=cclass require=b foo=false]" + 
				"exec = [exec stuff]" + 
				"foo = [foo stuff]";
			getBuilder().build(spec);
			fail("exception expected");
		} catch (Exception e) {
			assertTrue("message C09 missing", e.getMessage().indexOf("no valid sequence") > 0);
		}
	}


}
