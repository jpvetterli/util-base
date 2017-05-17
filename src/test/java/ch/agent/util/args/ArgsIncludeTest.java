package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ArgsIncludeTest {

	@Test
	public void testInclude01() {
		try {
			Args args = new Args(null);
			args.def("a");
			args.def("name1");
			args.def("name2");
			args.def("bar");
			args.def("");
			args.parse("a = b include=ArgsTest.test4");
			assertEquals("b", args.get("a"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2", args.get("name2"));
			assertEquals("bar's value", args.get("bar"));
			assertEquals("keyword", args.get(""));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testInclude02() {
		try {
			Args args = new Args(null);
			args.def("a");
			args.def("foo1");
			args.def("foo2");
			args.parse("a = b include=[names=[name1=foo1 name2=foo2] ArgsTest.test4]");
			assertEquals("b", args.get("a"));
			assertEquals("val1", args.get("foo1"));
			assertEquals("val2", args.get("foo2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testInclude03() {
		try {
			Args args = new Args(null);
			args.def("a");
			args.def("foo1");
			args.def("name2");
			args.parse("a = b include=[names=[name1=foo1 name2] ArgsTest.test4]");
			assertEquals("b", args.get("a"));
			assertEquals("val1", args.get("foo1"));
			assertEquals("val2", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testInclude04() {
		try {
			Args args = new Args(null);
			args.def("a");
			args.def("foo1");
			args.def("name2");
			args.def("");
			args.parse("a = b include=[names=[name1=foo1 name2 keyword=Schlüsselwort] ArgsTest.test4]");
			assertEquals("b", args.get("a"));
			assertEquals("val1", args.get("foo1"));
			assertEquals("val2", args.get("name2"));
			assertEquals("Schlüsselwort", args.get(""));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testInclude05() {
		try {
			Args args = new Args(null);
			args.def("a");
			args.def("foo1");
			args.def("name2");
			args.def("").init("DEFAULT");
			args.parse("a = b include=[names=[name1=foo1 name2 keyword=Schlüsselwort] ArgsTest.test4 extractor-parameters=simple]");
			assertEquals("b", args.get("a"));
			assertEquals("val1", args.get("foo1"));
			assertEquals("val2", args.get("name2"));
			assertEquals("DEFAULT", args.get(""));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
