package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.UtilMsg.U;

public class ArgsTest {

	private Args args;
	private String file1;
	private String file2;
	private String file3;
	
	@Before
	public void setUp() throws Exception {
		args = new Args();
		file1 = "ArgsTest.test1";
		file2 = "ArgsTest.test2";
		file3 = "ArgsTest.test3";
	}
	
	@Test
	public void testSimpleDefine() {
		try {
			args.define("foo");
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testDuplicateDefine() {
		try {
			args.define("foo");
			args.define("foo");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00104));
		}
	}
	
	@Test
	public void testDefault() {
		try {
			args.define("foo", "bar");
			assertEquals("bar", args.get("foo"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMissing() {
		try {
			assertEquals(null, args.get("foo"));
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00103));
		}
	}
	
	@Test
	public void testMissingDefault() {
		try {
			args.define("foo");
			assertEquals(null, args.get("foo"));
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00105));
		}
	}

	@Test
	public void testListParameter() {
		try {
			args.defineList("foo");
			args.put("foo", "value1");
			args.put("foo", "value2");
			assertEquals("value2", args.getList("foo").get(1));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testListParameterException() {
		try {
			args.defineList("foo");
			args.get("foo");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00101));
		}
	}
	
	@Test
	public void testListParameterException2() {
		try {
			args.define("foo");
			args.getList("foo");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00102));
		}
	}
	
	@Test
	public void testIntegerParameter() {
		try {
			args.define("foo", "42");
			assertEquals(42,  args.getInt("foo"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testIntegerParameter2() {
		try {
			args.define("foo", "xyzzy");
			assertEquals(42,  args.getInt("foo"));
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00111));
		}
	}
	
	@Test
	public void testBoolean() {
		try {
			args.define("foo", "true");
			assertEquals(true,  args.getBoolean("foo"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testBoolean2() {
		try {
			args.define("foo", "FALSE");
			assertEquals(false,  args.getBoolean("foo"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testBoolean3() {
		try {
			args.define("foo", "XyZZy");
			args.getBoolean("foo");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00112));
		}
	}
	
	@Test
	public void testParser() {
		try {
			args.define("foo");
			args.define("b a]z");
			args.parse("foo = [b a r] [b a\\]z]=barf]");
			assertEquals("b a r", args.get("foo"));
			assertEquals("barf]", args.get("b a]z"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testParser2() {
		try {
			args.defineList("foo");
			args.define("b a]z");
			args.parse("foo = [b a r] [b a\\]z]=barf] foo=[2nd value]");
			assertEquals("b a r", args.getList("foo").get(0));
			assertEquals("2nd value", args.getList("foo").get(1));
			assertEquals("barf]", args.get("b a]z"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testParser3() {
		try {
			args.define("foo");
			args.define("x");
			args.parse("x=[[y\\]] foo = bar");
			assertEquals("[y]", args.get("x"));
			assertEquals("bar", args.get("foo"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testParser4() {
		try {
			args.defineList("foo");
			args.define("qu ux");
			args.parse("foo = bar [qu ux]=[[what = ever\\]] foo = [2nd val]");
			List<String> values = args.getList("foo");
			assertEquals("[what = ever]", args.get("qu ux"));
			assertEquals(2, values.size());
			assertEquals("bar", values.get(0));
			assertEquals("2nd val", values.get(1));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testEmptyValue() {
		try {
			args.define("foo");
			args.parse("foo = []");
			assertEquals(0, args.get("foo").length());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testArgsFile() {
		try {
			args.define("foo");
			args.define("bar");
			args.define("multi");
			args.define("name1");
			args.define("name2");
			args.parse("file = " + file1 + " name2 = val2B");
			assertEquals("foo's value", args.get("foo"));
			assertEquals("bar's value", args.get("bar"));
			assertEquals("a b c", args.get("multi"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2B", args.get("name2"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testArgsFileWithMapping() {
		try {
			args.define("bar");
			args.define("x");
			args.define("y");
			args.parse("file = [" + file2 + "; name1 = x name2=y]");
			assertEquals("val1", args.get("x"));
			assertEquals("val2", args.get("y"));
			// bar not mapped
			assertEquals("bar's value", args.get("bar"));
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00105));
		}
	}
	
	@Test
	public void testDuplicateFiles() {
		try {
			args.define("bar");
			args.define("name1");
			args.define("name2");
			args.parse("file = " + file2 + " file = " + file3);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getCause().getMessage().startsWith(U.U00209));
		}
	}
	
	@Test
	public void testLooseMode() {
		try {
			Args looseArgs = new Args(null, null, false);
			looseArgs.parse("a=1 b=2 c=3");
			int total = 0;
			for (String arg : looseArgs) {
				total += looseArgs.getInt(arg);
			}
			assertEquals(6, total);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

}
