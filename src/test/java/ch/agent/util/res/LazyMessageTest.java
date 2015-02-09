package ch.agent.util.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.res.TestMessage.M;

public class LazyMessageTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testDouble() {
		assertEquals("1.52", LazyMessage.lazy("1.52").toString());
	}
	
	@Test
	public void testDouble2() {
		assertEquals("1.52", LazyMessage.lazy("{0}", 1.52).toString());
	}
	
	@Test
	public void testDouble3() {
		assertEquals("NaN", LazyMessage.lazy("{0}", Double.NaN).toString());
	}
	
	@Test
	public void testStringFormatter() {
		assertEquals("%d", LazyMessage.lazy("%d", 42d).toString());
	}

	@Test
	public void testNull() {
		Object x = null;
		assertEquals("null", LazyMessage.lazy("{0}", x).toString());
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
		assertEquals(M.M2 + " - This message has a parameter: foo.", 
				TestMessage.msg(M.M2, "foo"));
	}
	
	@Test
	public void testSimpleMessageWithTwoArgsReversed() {
		assertEquals(M.M3 + " - This message has two parameters foo and NaN (reversed).", 
				TestMessage.msg(M.M3, Double.NaN, "foo"));
	}
	
	@Test
	public void testSimpleMessageWithTwoArgsNotReversed() {
		try {
			TestMessage.msg(M.M4, "foo", Double.NaN);
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getCause().getMessage().startsWith("can't parse argument number")); 
		}
	}
	
	@Test
	public void testNullKey() {
		try {
			new TestMessage(null, null).toString();
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("key=null bundle=ch.agent.util.res.TestMessage", e.getMessage());
		}
	}
	
	@Test
	public void testNonExistentKey() {
		try {
			new TestMessage("KEY42", null).toString();
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("key=KEY42 bundle=ch.agent.util.res.TestMessage", e.getMessage());
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
		String msg = LazyMessage.lazy("x{0}x", longArg1000).toString();
		assertEquals(1002, msg.length());
		msg = LazyMessage.lazy("x{0}x", longArg1000 + longArg100).toString();
		assertEquals(1002, msg.length());
		assertTrue(msg.endsWith("[...]x"));
	}
	
}
