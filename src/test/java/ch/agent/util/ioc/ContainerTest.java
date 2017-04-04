package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.args.Args;
import ch.agent.util.logging.LoggerBridge;
import ch.agent.util.logging.LoggerManager;

public class ContainerTest {
	
	public static class A {
		
		void set(String s) {
		}
		
	}
	
	public static class B {
	
		List<String> records = new ArrayList<String>();
		
		String tag = "not set";
		
		void set(String s) {
			String text = "B#set " + s + " and tag=" + tag;
			record(text);
		}
		
		void changeTag(String tag) {
			this.tag = tag;
		}
		
		void record(String s) {
			records.add(s);
		}
		List<String> getRecords() {
			return records;
		}

	}

	public static class AModule extends AbstractModule<A>  implements Module<A> {

		B b;
		
		public AModule(String name) {
			super(name);
		}

		@Override
		public A getObject() {
			return new A();
		}

		@Override
		public boolean add(Module<?> module) {
			b = (B) module.getObject();
			return true;
		}

		@Override
		public void initialize() {
			b.set("This is module \"" + getName() + "\" starting");
			b.changeTag("xyzzy");
		}

		@Override
		public void registerCommands(ConfigurationRegistry<?> registry) {
			add(new Command<A>() {
				@Override
				public String getName() {
					return "changeTag";
				}
				@Override
				public void execute(String parameters) {
					b.changeTag(parameters);
				}
			});
			add(new Command<A>() {
				@Override
				public String getName() {
					return "set";
				}
				@Override
				public void execute(String parameters) {
					b.set(parameters);
				}
			});
			super.registerCommands(registry);
		}

		@Override
		public void shutdown() {
			b.set("This is module \"" + getName() + "\" stopping");
		}
		
	}
	
	public static class DModule extends AbstractModule<Object>  implements Module<Object> {

		public DModule(String name) {
			super(name);
			addCommands();
		}

		private void addCommands() {
			add(new Command<Object>() {
				@Override
				public String getName() {
					return "command";
				}
				@Override
				public void execute(String parameters) {}
			});
			add(new Command<Object>() {
				@Override
				public String getName() {
					return "command";
				}
				@Override
					public void execute(String parameters) {}
				});
		}
		
		@Override
		public Object getObject() {
			return null;
		}

	}

	public static class BModule extends AbstractModule<B>  implements Module<B> {

		B b;
		
		public BModule(String name) {
			super(name);
			b = new B();
		}

		@Override
		public B getObject() {
			return b;
		}

		@Override
		public void defineParameters(Args config) {
			config.def("tag").init("default");
		}

		@Override
		public void configure(Args config) {
			super.configure(config);
			b.changeTag(config.get("tag"));
		}
		
	}

	/**
	 * A module without an underlying object.
	 */
	public static class CModule extends AbstractModule<Object>  implements Module<Object> {
		
		final static LoggerBridge logger = LoggerManager.getLogger(CModule.class);
		
		public CModule(String name) {
			super(name);
			addCommands();
		}
		
		private void addCommands() {
			final Module<Object> m = this;
			add(
				new Command<Object>() {
				@Override
				public String getName() {
					return "echo";
				}
				@Override
				public void execute(String parameters) {
					logger.debug("* (this is command " + getName() + " in module " + m.getName() + ")");
					logger.info("* " + parameters);
				}
			});
		}

		@Override
		public Object getObject() {
			return null;
		}
		
	}

	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test10() {
		Container c = new Container();
		try {
			c.run(new String[]{
					String.format("module=[name = a class=%s require=b]", AModule.class.getName()),
					String.format("module=[name = b class=%s config=[tag=[This tag was modified.]]]", BModule.class.getName()),
			});
			c.shutdown();
			List<String> texts = ((B) c.getModule("b").getObject()).getRecords();
			assertEquals("B#set This is module \"a\" stopping and tag=xyzzy", texts.get(1));
			assertEquals("B#set This is module \"a\" starting and tag=This tag was modified.", texts.get(0));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void test11() {
		Container c = new Container();
		try {
			c.run(new String[]{
					String.format("module=[name = a class=%s require=b]", AModule.class.getName()),
					String.format("module=[name = b class=%s config=[tag=[This tag was modified.]]]", BModule.class.getName()),
					"exec=[a.set=[exec1] a.changeTag=[exec2] a.set=[exec3]]"
			});
			c.shutdown();
			List<String> texts = ((B) c.getModule("b").getObject()).getRecords();
			assertEquals("B#set This is module \"a\" starting and tag=This tag was modified.", texts.get(0));
			assertEquals("B#set exec1 and tag=xyzzy", texts.get(1));
			assertEquals("B#set exec3 and tag=exec2", texts.get(2));
			assertEquals("B#set This is module \"a\" stopping and tag=exec2", texts.get(3));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test12() {
		Container c = new Container();
		try {
			c.run(new String[]{
					String.format("module=[name = a class=%s require=b]", AModule.class.getName()),
					String.format("module=[name = b class=%s config=[tag=[This tag was modified.]]]", BModule.class.getName()),
					String.format("module=[name = c class=%s]", CModule.class.getName()),
					"exec=[a.set=[exec1] a.changeTag=[exec2] a.set=[exec3] c.echo=[hello world]]"
			});
			c.shutdown();
			List<String> texts = ((B) c.getModule("b").getObject()).getRecords();
			assertEquals("B#set This is module \"a\" starting and tag=This tag was modified.", texts.get(0));
			assertEquals("B#set exec1 and tag=xyzzy", texts.get(1));
			assertEquals("B#set exec3 and tag=exec2", texts.get(2));
			assertEquals("B#set This is module \"a\" stopping and tag=exec2", texts.get(3));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test13() {
		Container c = new Container();
		try {
			c.run(new String[]{
					String.format("module=[name = x class=%s]", DModule.class.getName())
			});
			c.shutdown();
			fail("exception expected");
		} catch (Exception e) {
			assertTrue("message C14 ?",  e.getCause().getCause().getMessage().indexOf("Duplicate command") > 0);
		}
	}
	
	@Test
	public void test14() {
		Container c = new Container();
		try {
			c.run(new String[]{
					String.format("module=[name = [] class=%s]", DModule.class.getName())
			});
			c.shutdown();
			fail("exception expected");
		} catch (Exception e) {
			assertTrue("message C03 ?",  e.getMessage().startsWith("C03"));
		}
	}

	@Test
	public void test20() {
		Container c = new Container();
		try {
			c.run(new String[]{});
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		} finally {
			c.shutdown();
		}
	}
	
	@Test
	public void test21() {
		Container c = new Container();
		try {
			c.run(new String[]{"module=[name = foo class=foo]"});
			fail("exception expected");
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue("message C03 ?", e.getMessage().startsWith("C03"));
		} finally {
			c.shutdown();
		}
	}

	@Test
	public void test22() {
		Container c = new Container();
		try {
			c.run(new String[]{"module=[name = foo class=java.lang.String]"});
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("C03"));
		} finally {
			c.shutdown();
		}
	}

	@Test
	public void test23() {
		Container c = new Container();
		try {
			c.run(new String[]{
					"module=[name = foo class=java.lang.String require=bar]",
					"module=[name = bar class=java.lang.String require=foo]"
			});
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("C09"));
		} finally {
			c.shutdown();
		}
	}
	
}
