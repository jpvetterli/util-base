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
		assertEquals("bar[baf]", result.get(1));
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
			List<String[]> pairs = scanner.asPairs("[f =[o] o] = b\\ar [f =[o] o] = b\\ar");
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
		assertEquals(" = ", result.get(1)[0]);
		assertEquals("x", result.get(3)[0]);
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
			List<String[]> result = scanner.asPairs("foo = bar x");
			assertEquals("x", result.get(1)[0]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00157));
		}
	}

	@Test
	public void testNameValuePairsMixed7() {
		try {
			List<String[]> result = scanner.asPairs("=abc foo = bar x");
			assertEquals("x", result.get(1)[0]);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00158));
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

	@Test
	public void testMetaChars12() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("a[ ]b");
			assertEquals("a[", result.get(0)[0]);
			assertEquals("]b", result.get(1)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testMetaChars13() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[a ]b");
			assertEquals("a ", result.get(0)[0]);
			assertEquals("b", result.get(1)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testImmediate1() {
		try {
			scanner.immediateString(null);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testImmediate2() {
		try {
			String[] result = scanner.immediateString("");
			assertEquals(2, result.length);
			assertEquals(null, result[0]);
			assertEquals(null, result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testImmediate3() {
		try {
			String[] result = scanner.immediateString("abc d etc.");
			assertEquals(2, result.length);
			assertEquals("abc", result[0]);
			assertEquals(" d etc.", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate3a() {
		try {
			String[] result = scanner.immediateString("abc");
			assertEquals(2, result.length);
			assertEquals("abc", result[0]);
			assertEquals("", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate3b() {
		try {
			String[] result = scanner.immediateString("abc ");
			assertEquals(2, result.length);
			assertEquals("abc", result[0]);
			assertEquals(" ", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testImmediate4() {
		try {
			String[] result = scanner.immediateString("= abc d");
			assertEquals(2, result.length);
			assertEquals(null, result[0]);
			assertEquals(null, result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate5() {
		try {
			String[] result = scanner.immediateString("[ abc ] d etc.");
			assertEquals(2, result.length);
			assertEquals(" abc ", result[0]);
			assertEquals(" d etc.", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate5a() {
		try {
			String[] result = scanner.immediateString("[a]d");
			assertEquals(2, result.length);
			assertEquals("a", result[0]);
			assertEquals("d", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate6() {
		try {
			String[] result = scanner.immediateString("[abc]d etc.");
			assertEquals(2, result.length);
			assertEquals("abc", result[0]);
			assertEquals("d etc.", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate7() {
		try {
			String[] result = scanner.immediateString("\\[abc]d etc.");
			assertEquals(2, result.length);
			assertEquals("\\[abc]d", result[0]);
			assertEquals(" etc.", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate7a() {
		try {
			String[] result = scanner.immediateString("[\\[abc\\]d] etc.");
			assertEquals(2, result.length);
			assertEquals("[abc]d", result[0]);
			assertEquals(" etc.", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testImmediate8() {
		try {
			String[] result = scanner.immediateString("[abc\\]] d etc.");
			assertEquals(2, result.length);
			assertEquals("abc]", result[0]);
			assertEquals(" d etc.", result[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}


}
