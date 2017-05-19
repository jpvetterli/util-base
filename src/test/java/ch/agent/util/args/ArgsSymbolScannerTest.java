package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.args.ArgsScanner.SymbolScanner;

public class ArgsSymbolScannerTest {

	private static final boolean DEBUG = false;

	private SymbolScanner scanner;

	@Before
	public void setUp() throws Exception {
		scanner = new SymbolScanner('$');
	}

	@Test
	public void testSplit01() {
		try {
			List<String> output = scanner.split("abc$$def ");
			assertEquals("abc", output.get(0));
			assertEquals(null, output.get(1));
			assertEquals("def", output.get(2));
			assertEquals(" ", output.get(3));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit02() {
		try {
			List<String> output = scanner.split("abc$$def");
			assertEquals("abc", output.get(0));
			assertEquals(null, output.get(1));
			assertEquals("def", output.get(2));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit03() {
		try {
			List<String> output = scanner.split("$$$$");
			assertEquals(1, output.size());
			assertEquals("$$$$", output.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit04() {
		try {
			// because of $$$x$ trick the next one cannot be parsed as $$/null/x
			List<String> output = scanner.split("$$$$x");
			assertEquals(1, output.size());
			assertEquals("$$$$x", output.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit05() {
		try {
			List<String> output = scanner.split("$$$ ");
			assertEquals("$$$ ", output.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit10() {
		// $$$x$ ==> $$ x uses 2 dollars to bound the symbol
		try {
			List<String> output = scanner.split("ab$$$x$cd");
			assertEquals(4, output.size());
			assertEquals("ab", output.get(0));
			assertEquals(null, output.get(1));
			assertEquals("x", output.get(2));
			assertEquals("cd", output.get(3));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testSplit11() {
		try {
			List<String> output = scanner.split("ab$$$x$cd xyzzy $$foo[bar] ");
			assertEquals(7, output.size());
			assertEquals("ab", output.get(0));
			assertEquals(null, output.get(1));
			assertEquals("x", output.get(2));
			assertEquals("cd xyzzy ", output.get(3));
			assertEquals(null, output.get(4));
			assertEquals("foo", output.get(5));
			assertEquals("[bar] ", output.get(6));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit15() {
		try {
			List<String> output = scanner.split("a bb c 124 [yes]");
			assertEquals(1, output.size());
			assertEquals("a bb c 124 [yes]", output.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit16() {
		try {
			List<String> output = scanner.split("$$one");
			assertEquals(2, output.size());
			assertEquals(null, output.get(0));
			assertEquals("one", output.get(1));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

}
