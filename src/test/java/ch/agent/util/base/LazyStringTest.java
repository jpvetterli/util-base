package ch.agent.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DecimalFormat;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.base.LazyString;
import ch.agent.util.base.TestMessage.M;

public class LazyStringTest {

	public static LazyString lazy(String text, Object... args) {
		return new LazyString(text, null, null, null, args);
	}
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void test0() {
		// NaNs are printed as a question mark (actually \uFFFD) 
		// on a calm day, look into java.text.spi.DecimalFormatSymbolsProvider
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals("1.52", df.format(1.52));
		assertEquals("\uFFFD", df.format(Double.NaN));
	}
	
	@Test
	public void testDouble() {
		assertEquals("1.52", lazy("1.52").toString());
	}
	
	@Test
	public void testDouble2() {
		assertEquals("1.52", lazy("{0}", 1.52).toString());
	}
	@Test
	public void testDouble3() {
		try {
			assertEquals("\uFFFD", lazy("{0,number,#.##}", Double.NaN).toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception");
		}
	}
	@Test
	public void testDouble4() {
		assertEquals("0.33", lazy("{0,number,#.##}", ((double)1/3)).toString());
	}
	@Test
	public void testDouble5() {
		assertEquals("1.00", lazy("{0,number,0.00}", ((double)1.000000000001)).toString());
	}
	@Test
	public void testDouble6() {
		assertEquals("1", lazy("{0,number,#.##}", ((double)1.000000000001)).toString());
	}

	@Test
	public void testStringFormatter() {
		assertEquals("%d", lazy("%d", 42d).toString());
	}

	@Test
	public void testNull() {
		Object x = null;
		assertEquals("null", lazy("{0}", x).toString());
	}
	
	@Test
	public void testSimpleMessage() {
		assertTrue(new TestMessage(M.M1, "").toString().startsWith(M.M1));
	}
	
	@Test
	public void testSimpleMessage2() {
		assertFalse(new TestMessage(M.M1, null).toString().startsWith(M.M1));
	}
	
	@Test
	public void testSimpleMessageWithArgs() {
		assertEquals(M.M2 + "     This message has a parameter: foo.", 
				TestMessage.msg(M.M2, "foo"));
	}
	
	@Test
	public void testSimpleMessageWithTwoArgsReversed() {
		try {
		assertEquals(M.M3 + "     This message has two parameters foo and \uFFFD (reversed).", 
				TestMessage.msg(M.M3, Double.NaN, "foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception");
		}
	}
	
	@Test
	public void testSimpleMessageWithTwoArgsNotReversed() {
		try {
			String text = TestMessage.msg(M.M4, "foo", Double.NaN);
			assertEquals("FAILURE (LazyString) key=M4 text=null bundle=ch.agent.util.base.TestMessage", text); 
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNullKey() {
		try {
			new TestMessage(null, null).toString();
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("key null", e.getMessage());
		}
	}
	
	@Test
	public void testNonExistentKey() {
		try {
			String text = new TestMessage("KEY42", null).toString();
			assertEquals("FAILURE (LazyString) key=KEY42 text=null bundle=ch.agent.util.base.TestMessage", text); 
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testException() {
		try {
			throw new Exception(TestMessage.msg(M.M1));
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(M.M1));
		}
	}
	
	@Test
	public void testRuntimeException() {
		try {
			throw new RuntimeException(TestMessage.msg(M.M1), new IllegalArgumentException("foo"));
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(M.M1));
			assertEquals("foo", e.getCause().getMessage());
		}
	}

	@Test
	public void testTooLongArg() {
		// LazyMessage truncates message arguments longer than 1000
		String longArg100 = "";
		String longArg1000 = "";
		for (int i = 0; i < 10; i++)
			longArg100 = longArg100 + "0123456789";
		for (int i = 0; i < 10; i++)
			longArg1000 = longArg1000 + longArg100;
		assertEquals(1000, longArg1000.length());
		String msg = lazy("x{0}x", longArg1000).toString();
		assertEquals(1002, msg.length());
		msg = lazy("x{0}x", longArg1000 + longArg100).toString();
		assertEquals(1002, msg.length());
		assertTrue(msg.endsWith("[...]x"));
	}
	
}
