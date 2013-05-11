package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.UtilMsg.U;

public class ArgsScannerTest {

	private ArgsScanner scanner;
	
	@Before
	public void setUp() throws Exception {
		scanner = new ArgsScanner();
	}

	@Test
	public void testOneString() {
		assertEquals("foo", scanner.tokenize("foo").get(0));
	}
	
	@Test
	public void testOneString2() {
		assertEquals("foo", scanner.tokenize("foo ").get(0));
	}
	
	@Test
	public void testTwoStrings() {
		List<String> result = scanner.tokenize("foo bar");
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
	}
	
	@Test
	public void testTwoStrings2() {
		List<String> result = scanner.tokenize("	foo 	 bar");
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
	}
	
	@Test
	public void testEmptyString() {
		assertEquals(0, scanner.tokenize("").size());
	}
	
	@Test
	public void testEmptyString2() {
		assertEquals(0, scanner.tokenize("   	").size());
	}

	@Test
	public void testNullString() {
		try {
			assertEquals(0, scanner.tokenize(null).size());
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
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair2() {
		try {
			List<String[]> pairs = scanner.asPairs("[f [o[ o] = bar");
			assertEquals("f [o[ o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair3() {
		try {
			List<String[]> pairs = scanner.asPairs("[f [o\\] o] = bar");
			assertEquals("f [o] o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair4() {
		try {
			List<String[]> pairs = scanner.asPairs("[f =[o\\] o] = b\\ar");
			assertEquals("f =[o] o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair5() {
		try {
			List<String[]> pairs = scanner
					.asPairs("[f =[o\\] o] = b\\ar [f =[o\\] o] = b\\ar");
			assertEquals("f =[o] o", pairs.get(0)[0]);
			assertEquals("b\\ar", pairs.get(0)[1]);
			assertEquals("f =[o] o", pairs.get(1)[0]);
			assertEquals("b\\ar", pairs.get(1)[1]);
		} catch (Exception e) {
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
			List<String[]> pairs = scanner.asPairs("f[o]o = bar");
			assertEquals("f[o]o", pairs.get(0)[0]);
			assertEquals("bar", pairs.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testNameValuePair8() {
		try {
			scanner.asPairs("[f =[o\\] o] = b\\ar [w h a t] b\\ar");
			fail("expected en exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00107));
		}
	}
	
	@Test
	public void testNameValuePair9() {
		try {
			scanner.asPairs("[f =[o\\] o] = b\\ar [w h a t] =");
			fail("expected en exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00107));
		}
	}
	
	@Test
	public void testNameValuePair10() {
		try {
			scanner.asPairs("[f =[o\\] o] = b\\ar [w h a t]");
			fail("expected en exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00107));
		}
	}

}
