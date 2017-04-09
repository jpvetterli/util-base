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
			List<String[]> pairs = scanner.asPairs("foo=bar");
			assertEquals("foo", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testSimpleNameValuePair2() {
		try {
			List<String[]> pairs = scanner.asPairs("foo = bar");
			assertEquals("foo", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair() {
		try {
			List<String[]> pairs = scanner.asPairs("[f o o] = bar");
			assertEquals("f o o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair2() {
		try {
			List<String[]> pairs = scanner.asPairs("[f \\[o\\[ o] = bar");
			assertEquals("f [o[ o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testNameValuePair2a() {
		try {
			scanner.asPairs("[f [o[ o] = bar");
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00157"));
		}
	}

	@Test
	public void testNameValuePair3() {
		try {
			List<String[]> pairs = scanner.asPairs("[f [o] o] = bar");
			assertEquals("f [o] o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testNameValuePair3a() {
		try {
			List<String[]> pairs = scanner.asPairs("[f [[o]] o] = bar");
			assertEquals("f [[o]] o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair4() {
		try {
			List<String[]> pairs = scanner.asPairs("[f =[o] o] = b\\ar");
			assertEquals("f =[o] o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testNameValuePair4a() {
		try {
			List<String[]> pairs = scanner.asPairs("[f =\\[o o] = b\\ar");
			assertEquals("f =[o o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair5() {
		try {
			List<String[]> pairs = scanner
					.asPairs("[f =[o] o] = b\\ar [f =[o] o] = b\\ar");
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
	public void testNameValuePair6() {
		try {
			List<String[]> pairs = scanner.asPairs("f[o]o = bar");
			assertEquals("f[o]o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair7() {
		try {
			List<String[]> pairs = scanner.asPairs("f[[o]]o = bar");
			assertEquals("f[[o]]o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair8() {
		try {
			scanner.asPairs("[f =[o\\] o] = b\\ar [w h a t] b\\ar");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00157));
		}
	}
	
	@Test
	public void testNameValuePair9() {
		try {
			scanner.asPairs("[f =[o] o] = b\\ar [w h a t] =");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00159));
		}
	}
	
	@Test
	public void testNameValuePair10() {
		try {
			scanner.asPairs("[f =[o\\] o] = b\\ar [w h a t]");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00157));
		}
	}
	
	@Test
	public void testValuePairsMixed1() {
		List<String[]> result = scanner.asValuesAndPairs("x foo=bar hop = la", false);
		assertEquals("x", result.get(0)[0]);
		assertEquals("la", result.get(2)[1]);
	}
	@Test
	public void testNameValuePairsMixed2() {
		List<String[]> result = scanner.asValuesAndPairs("foo=[ bar ] x hop =la", false);
		assertEquals(" bar ", result.get(0)[1]);
		assertEquals("la", result.get(2)[1]);
	}
	@Test
	public void testNameValuePairsMixed3() {
		List<String[]> result = scanner.asValuesAndPairs(" foo [ = ] bar[hop]=la x", false);
		assertEquals(" = ", result.get(1)[0]);
		assertEquals("x", result.get(3)[0]);
	}
	@Test
	public void testNameValuePairsMixed4() {
		List<String[]> result = scanner.asValuesAndPairs(" foo = bar x hop=la y", false);
		assertEquals("x", result.get(1)[0]);
		assertEquals("y", result.get(3)[0]);
	}
	
	@Test
	public void testNameValuePairsMixed5() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("foo = bar x", true);
			assertEquals("x", result.get(1)[0]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00157));
		}
	}
	
	@Test
	public void testNameValuePairsMixed7() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("=abc foo = bar x", true);
			assertEquals("x", result.get(1)[0]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00158));
		}
	}
	
	@Test
	public void testNameValuePairsMixed8() {
		List<String[]> result = scanner.asValuesAndPairs("one=1 foo two = 2 three= 3 bar baz four=4 done ", false);
		assertEquals(8, result.size());
		assertEquals("1", result.get(0)[1]);
		assertEquals("1", result.get(0)[1]);
		assertEquals("four", result.get(6)[0]);
		assertEquals("4", result.get(6)[1]);
		assertEquals("done", result.get(7)[0]);
	}
	
	@Test
	public void testMetaChars1() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("OOPS [f =[o] o] = [b\\ar [w h a t] b\\ar]");
			assertEquals("f =[o] o", result.get(1)[0]);
			assertEquals("b\\ar [w h a t] b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testMetaChars1a() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("OOPS [f =\\[o\\] o] = [b\\ar [w h a t] b\\ar]");
			assertEquals("f =[o] o", result.get(1)[0]);
			assertEquals("b\\ar [w h a t] b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testMetaChars2() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("Tokenizer.MetaCharacters=():\\ OOPS (f =(o) o) : (b\\ar (w h a t) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testMetaChars2a() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("Tokenizer.MetaCharacters=():\\ OOPS (f =\\(o\\) o) : (b\\ar (w h a t) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars3() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("Tokenizer.MetaCharacters=toolong OOPS (f =(o\\) o) : (b\\ar (w h a t\\) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00164));
		}
	}

	@Test
	public void testMetaChars4() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("Tokenizer.MetaCharacters=xxxx OOPS (f =(o\\) o) : (b\\ar (w h a t\\) b\\ar)");
			assertEquals("f =(o) o", result.get(1)[0]);
			assertEquals("b\\ar (w h a t) b\\ar", result.get(1)[1]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00163));
		}
	}
	
	@Test
	public void testMetaChars5() {
		try {
			scanner.asValuesAndPairs("Tokenizer.MetaCharacters='':\\ OOPS 'f =\\'o\\' o' : 'b\\ar \\'w h a t\\' b\\ar'");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00163));
		}
	}

	@Test
	public void testMetaChars7() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[[=]]\\");
			assertEquals("[=]", result.get(0)[0]);
			assertEquals("\\", result.get(1)[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMetaChars8() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[\\[=\\\\]]");
			assertEquals("[=\\", result.get(0)[0]);
			assertEquals("]", result.get(1)[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMetaChars9() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[[]=\\\\] a=b");
			assertEquals("[]=\\", result.get(0)[0]); // instead: "[]=\\a\\] a=b"
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMetaChars10() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("Tokenizer.MetaCharacters=[[]=\\\\] OOPS [f =[o] o] = [b\\ar [w h a t] b\\ar]");
			assertEquals("f =[o] o", result.get(1)[0]);
			assertEquals("b\\ar [w h a t] b\\ar", result.get(1)[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars11() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("\\42[\\]42\\\\");
			assertEquals("\\42[\\]42\\\\", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

}
