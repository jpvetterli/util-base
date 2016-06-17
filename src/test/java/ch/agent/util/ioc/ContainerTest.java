package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.args.Args;

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
		public int start() {
			b.set("This is module \"" + getName() + "\" starting");
			b.changeTag("xyzz");
			return 0;
		}

		@Override
		public boolean stop() {
			b.set("This is module \"" + getName() + "\" stopping");
			return true;
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
		public void define(Args config) {
			config.def("tag").init("default");
		}

		@Override
		public void configure(Args config) throws Exception {
			b.changeTag(config.get("tag"));
		}
		
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test10() {
		try {
			Container c = new Container();
			c.run(new String[]{
					"module=[name = a class=ch.agent.util.ioc.ContainerTest$AModule start=true require=b]",
					"module=[name = b class=ch.agent.util.ioc.ContainerTest$BModule]",
					"config=[b=[tag=[This tag was modified.]]]"
					
			});
			c.shutdown();
			List<String> texts = ((B) c.getModule("b").getObject()).getRecords();
			assertEquals("B#set This is module \"a\" stopping and tag=xyzz", texts.get(1));
			assertEquals("B#set This is module \"a\" starting and tag=This tag was modified.", texts.get(0));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test20() {
		try {
			Container c = new Container();
			c.run(new String[]{
			});
			c.shutdown();
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("C04"));
		}
	}
	
	@Test
	public void test21() {
		try {
			Container c = new Container();
			c.run(new String[]{
					"module=[name = foo class=foo start=true]"
			});
			c.shutdown();
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("C07"));
		}
	}

	@Test
	public void test22() {
		try {
			Container c = new Container();
			c.run(new String[]{
					"module=[name = foo class=java.lang.String start=true]"
			});
			c.shutdown();
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("C07"));
		}
	}

	@Test
	public void test23() {
		try {
			Container c = new Container();
			c.run(new String[]{
					"module=[name = foo class=java.lang.String start=true require=bar]",
					"module=[name = bar class=java.lang.String require=foo]"
			});
			c.shutdown();
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("C09"));
		}
	}

}
