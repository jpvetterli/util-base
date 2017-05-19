package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.STRINGS.U;

public class ArgsScannerTest2 {

	private NameValueScanner scanner;

	@Before
	public void setUp() throws Exception {
		scanner = new NameValueScanner();
	}

	@Test
	public void testBracket01() {
		try {
			scanner.asValuesAndPairs("a ] b");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00154));
		}
	}
	
	@Test
	public void testBracket02() {
		try {
			scanner.asValuesAndPairs("[a] ] b");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00154));
		}
	}
	
	@Test
	public void testBracket03() {
		try {
			scanner.asValuesAndPairs("[a ] b]");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00154));
		}
	}

	@Test
	public void testBracket06() {
		try {
			scanner.asValuesAndPairs("[\\a\\b");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00155));
		}
	}
	
	@Test
	public void testBracket07() {
		try {
			scanner.asValuesAndPairs("[[a]");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00155));
		}
	}
	
	@Test
	public void testBracket10() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("a[ ]b");
			assertEquals(3, result.size());
			assertEquals("a", result.get(0)[0]);
			assertEquals(" ", result.get(1)[0]);
			assertEquals("b", result.get(2)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testBracket11() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[a ]b");
			assertEquals("a ", result.get(0)[0]);
			assertEquals("b", result.get(1)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testBracket15() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[ a b] = [ x y z ]  ");
			assertEquals(1, result.size());
			assertEquals(" a b", result.get(0)[0]);
			assertEquals(" x y z ", result.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testBracket16() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("[ a b] = [[ x y z ]]  ");
			assertEquals(1, result.size());
			assertEquals(" a b", result.get(0)[0]);
			assertEquals("[ x y z ]", result.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testEqual01() {
		try {
			scanner.asValuesAndPairs("= x");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00156));
		}
	}
	
	@Test
	public void testEqual02() {
		try {
			scanner.asValuesAndPairs("a=b =x");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00156));
		}
	}
	
	@Test
	public void testEscape01() {
		try {
			scanner.asValuesAndPairs("\\");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00153));
		}
	}
	
	@Test
	public void testEscape02() {
		try {
			scanner.asValuesAndPairs("foo=[bar baf] \\");
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00153));
		}
	}
	
	@Test
	public void testEscape03() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("ab\\c");
			assertEquals("ab\\c", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testEscape04() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("\\abc");
			assertEquals("\\abc", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testEscape05() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("ab\\\\c");
			assertEquals("ab\\c", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testEscape06() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("ab\\ c");
			assertEquals("ab c", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testEscape07() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("ab\\\tc");
			assertEquals("ab\tc", result.get(0)[0]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testEscape10() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("a=[b\\] c d]");
			assertEquals(1, result.size());
			assertEquals("a", result.get(0)[0]);
			assertEquals("b] c d", result.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testEscape11() {
		try {
			List<String[]> result = scanner.asValuesAndPairs("a\\= = [b\\] c d]");
			assertEquals(1, result.size());
			assertEquals("a=", result.get(0)[0]);
			assertEquals("b] c d", result.get(0)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	
}
