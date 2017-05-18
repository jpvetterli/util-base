package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.STRINGS.U;

public class ArgsScannerTest {

	private ArgsScanner scanner;

	@Before
	public void setUp() throws Exception {
		scanner = new ArgsScanner();
	}

	@Test
	public void testOneString() {
		assertEquals("foo", scanner.asValuesAndPairs("foo").get(0)[0]);
	}

	@Test
	public void testOneString2() {
		assertEquals("foo", scanner.asValuesAndPairs("foo ").get(0)[0]);
	}

	@Test
	public void testTwoStrings() {
		List<String[]> result = scanner.asValuesAndPairs("foo bar");
		assertEquals("foo", result.get(0)[0]);
		assertEquals("bar", result.get(1)[0]);
	}

	@Test
	public void testTwoStrings2() {
		List<String[]> result = scanner.asValuesAndPairs("	foo 	 bar");
		assertEquals("foo", result.get(0)[0]);
		assertEquals("bar", result.get(1)[0]);
	}

	@Test
	public void testTwoStrings3() {
		List<String[]> result = scanner.asValuesAndPairs("foo [bar baf]");
		assertEquals("foo", result.get(0)[0]);
		assertEquals("bar baf", result.get(1)[0]);
	}

	@Test
	public void testAsValues0() {
		try {
			scanner.asValues(null);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			
		}
	}
	
	@Test
	public void testAsValues1() {
		List<String> result = scanner.asValues("foo [bar baf]");
		assertEquals("foo", result.get(0));
		assertEquals("bar baf", result.get(1));
	}

	@Test
	public void testAsValues2() {
		List<String> result = scanner.asValues("foo = [bar baf]");
		assertEquals("foo", result.get(0));
		assertEquals("=", result.get(1));
		assertEquals("bar baf", result.get(2));
	}

	@Test
	public void testAsValues3() {
		List<String> result = scanner.asValues("foo=[bar baf]");
		assertEquals("foo", result.get(0));
		assertEquals("=", result.get(1));
		assertEquals("bar baf", result.get(2));
	}

	@Test
	public void testAsValues4() {
		List<String> result = scanner.asValues("foo [=bar baf]");
		assertEquals("foo", result.get(0));
		assertEquals("=bar baf", result.get(1));
	}

	@Test
	public void testAsValues5() {
		List<String> result = scanner.asValues("foo [bar baf][a b c]");
		assertEquals("foo", result.get(0));
		assertEquals("bar baf", result.get(1));
		assertEquals("a b c", result.get(2));
	}

	@Test
	public void testAsValues6() {
		List<String> result = scanner.asValues("[foo] [bar baf][a b c]");
		assertEquals("foo", result.get(0));
		assertEquals("bar baf", result.get(1));
		assertEquals("a b c", result.get(2));
	}
	
	@Test
	public void testAsValues7() {
		List<String> result = scanner.asValues("[foo][bar][baf]");
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
		assertEquals("baf", result.get(2));
	}
	
	@Test
	public void testAsValues8() {
		List<String> result = scanner.asValues("[foo]bar[baf]");
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
		assertEquals("baf", result.get(2));
	}

	@Test
	public void testEmptyString() {
		assertEquals(0, scanner.asValuesAndPairs("").size());
	}

	@Test
	public void testEmptyString2() {
		assertEquals(0, scanner.asValuesAndPairs("   	").size());
	}

	@Test
	public void testNullString() {
		try {
			assertEquals(0, scanner.asValuesAndPairs(null).size());
			fail("expected an exception");
		} catch (Exception e) {
			assertEquals(IllegalArgumentException.class, e.getClass());
		}
	}

	@Test
	public void testSimpleNameValuePair() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("foo=bar");
			assertEquals("foo", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testSimpleNameValuePair2() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("foo = bar");
			assertEquals("foo", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair01() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f o o] = bar");
			assertEquals("f o o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair02() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f \\[o\\[ o] = bar");
			assertEquals("f [o[ o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair02a() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[f [o[ o]]] = bar");
			assertEquals(1, result.size());
			assertEquals("f [o[ o]]", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testNameValuePair02b() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[f [o[ o] = bar]]");
			assertEquals(1, result.size());
			assertEquals("f [o[ o] = bar]", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair03() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f [o] o] = bar");
			assertEquals("f [o] o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair03a() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f [[o]] o] = bar");
			assertEquals("f [[o]] o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair04() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f =[o] o] = b\\ar");
			assertEquals("f =[o] o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair04a() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f =\\[o o] = b\\ar");
			assertEquals("f =[o o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair05() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("[f =[o] o] = b\\ar [f =[o] o] = b\\ar");
			assertEquals("f =[o] o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
			assertEquals("f =[o] o", pairs.get(1)[0]);
			assertEquals("b\\ar", pairs.get(1)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair06() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("f[o]o = bar");
			assertEquals(3, pairs.size());
			assertEquals("f", pairs.get(0)[0]);
			assertEquals("o", pairs.get(1)[0]);
			assertEquals("o", pairs.get(2)[0]);
			assertEquals("bar", pairs.get(2)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair07() {
		try {
			List<String[]> pairs = scanner.asValuesAndPairs("f[[o]]o = bar");
			assertEquals("f", pairs.get(0)[0]);
			assertEquals("[o]", pairs.get(1)[0]);
			assertEquals("o", pairs.get(2)[0]);
			assertEquals("bar", pairs.get(2)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair08() {
		try {
			scanner.asValuesAndPairs("[f =[o\\] o] = b\\ar [w h a t] b\\ar");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00155));
		}
	}

	@Test
	public void testNameValuePair09() {
		try {
			scanner.asValuesAndPairs("[f =[o] o] = b\\ar [w h a t] =");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00159));
		}
	}

	@Test
	public void testNameValuePair10() {
		try {
			scanner.asValuesAndPairs("[f =[o\\] o] = b\\ar [w h a t]");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00155));
		}
	}
	
	@Test
	public void testValuePairsMixed1() {
		List<String[]> result = scanner.asValuesAndPairs("x foo=bar hop = la");
		assertEquals("x", result.get(0)[0]);
		assertEquals("la", result.get(2)[1]);
	}

	@Test
	public void testNameValuePairsMixed2() {
		List<String[]> result = scanner.asValuesAndPairs("foo=[ bar ] x hop =la");
		assertEquals(" bar ", result.get(0)[1]);
		assertEquals("la", result.get(2)[1]);
	}

	@Test
	public void testNameValuePairsMixed3() {
		List<String[]> result = scanner.asValuesAndPairs(" foo [ = ] bar[hop]=la x");
		assertEquals("foo", result.get(0)[0]);
		assertEquals(" = ", result.get(1)[0]);
		assertEquals("bar", result.get(2)[0]);
		assertEquals("hop", result.get(3)[0]);
		assertEquals("la", result.get(3)[1]);
		assertEquals("x", result.get(4)[0]);
	}

	@Test
	public void testNameValuePairsMixed4() {
		List<String[]> result = scanner.asValuesAndPairs(" foo = bar x hop=la y");
		assertEquals("x", result.get(1)[0]);
		assertEquals("y", result.get(3)[0]);
	}

	@Test
	public void testNameValuePairsMixed5() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("foo = bar x");
			assertEquals("foo", result.get(0)[0]);
			assertEquals("bar", result.get(0)[1]);
			assertEquals("x", result.get(1)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePairsMixed7() {
		try {
			scanner.asValuesAndPairs("=abc foo = bar x");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00156));
		}
	}

	@Test
	public void testNameValuePairsMixed8() {
		List<String[]> result = scanner.asValuesAndPairs("one=1 foo two = 2 three= 3 bar baz four=4 done ");
		assertEquals(8, result.size());
		assertEquals("1", result.get(0)[1]);
		assertEquals("1", result.get(0)[1]);
		assertEquals("four", result.get(6)[0]);
		assertEquals("4", result.get(6)[1]);
		assertEquals("done", result.get(7)[0]);
	}

	@Test
	public void testMetaChars01() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("OOPS [f =[o] o] = [b\\ar [w h a t] b\\ar]");
			assertEquals(2, result.size());
			assertEquals("OOPS", result.get(0)[0]);
			assertEquals("f =[o] o", result.get(1)[0]);
			assertEquals("b\\ar [w h a t] b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars01a() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("OOPS [f =\\[o\\] o] = [b\\ar [w h a t] b\\ar]");
			assertEquals(2, result.size());
			assertEquals("OOPS", result.get(0)[0]);
			assertEquals("f =[o] o", result.get(1)[0]);
			assertEquals("b\\ar [w h a t] b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars02() {
		try {
			ArgsScanner customScanner = new ArgsScanner('(', ')', ':', '\\');
			List<String[]> result = customScanner.asValuesAndPairs("OOPS (f =(o) o) : (b\\ar (w h a t) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars02a() {
		try {
			ArgsScanner customScanner = new ArgsScanner('(', ')', ':', '\\');
			List<String[]> result = customScanner.asValuesAndPairs("OOPS (f =\\(o\\) o) : (b\\ar (w h a t) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars04() {
		try {
			ArgsScanner customScanner = new ArgsScanner('x', 'x', 'x', 'x');
			List<String[]> result = customScanner.asValuesAndPairs("Tokenizer.MetaCharacters=xxxx OOPS (f =(o\\) o) : (b\\ar (w h a t\\) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00163));
		}
	}

	@Test
	public void testMetaChars05() {
		try {
			ArgsScanner customScanner = new ArgsScanner('\'', '\'', ':', '\\');
			customScanner.asValuesAndPairs("a: 'x y z'");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00163));
		}
	}
	
	@Test
	public void testMetaChars07() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[[=]]\\\\");
			assertEquals("[=]", result.get(0)[0]);
			assertEquals("\\", result.get(1)[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars08() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[\\[=\\\\]\\]");
			assertEquals("[=\\", result.get(0)[0]);
			assertEquals("]", result.get(1)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars09() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[[]=\\\\] a=b");
			assertEquals("[]=\\", result.get(0)[0]); // instead: "[]=\\a\\] a=b"
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars11() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("\\42[\\]42\\\\]");
			assertEquals(2, result.size());
			assertEquals("\\42", result.get(0)[0]);
			assertEquals("]42\\", result.get(1)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

}
