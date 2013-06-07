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
		assertEquals("1.52", new LazyMessage("1.52").toString());
	}
	
	@Test
	public void testDouble2() {
		assertEquals("1.52", new LazyMessage("{0}", 1.52).toString());
	}
	
	@Test
	public void testDouble3() {
		assertEquals("NaN", new LazyMessage("{0}", Double.NaN).toString());
	}
	
	@Test
	public void testNull() {
		Object x = null;
		assertEquals("null", new LazyMessage("{0}", x).toString());
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

}
