package ch.agent.util.res;

import static org.junit.Assert.assertEquals;
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
	public void testSimpleMessage() {
		assertTrue(new TestMessage(M.M1).toString().startsWith(M.M1));
	}
	
	@Test
	public void testSimpleMessageWithArgs() {
		assertEquals(M.M2 + " - This message has a parameter: foo.", 
				new TestMessage(M.M2, "foo").toString());
	}
	
	@Test
	public void testSimpleMessageWithTwoArgsReversed() {
		assertEquals(M.M3 + " - This message has two parameters foo and null (reversed).", 
				new TestMessage(M.M3, null, "foo").toString());
	}
	
	@Test
	public void testNullKey() {
		try {
			new TestMessage(null).toString();
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("key=null bundle=ch.agent.util.res.TestMessage", e.getMessage());
		}
	}
	
	@Test
	public void testNonExistentKey() {
		try {
			new TestMessage("KEY42").toString();
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
