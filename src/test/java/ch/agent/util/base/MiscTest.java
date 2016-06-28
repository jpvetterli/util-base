package ch.agent.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

public class MiscTest {
	

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void test10() {
		try {
			String[] strings = new String[]{"foo", "bar"};
			assertEquals("foo bar", ch.agent.util.base.Misc.join(" ", strings));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void test20() {
		try {
			String[] strings = new String[]{"foo", "bar"};
			assertEquals("foo bar", ch.agent.util.base.Misc.join(" ", Arrays.asList(strings)));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void test30() {
		try {
			assertEquals("", ch.agent.util.base.Misc.join("", new String[]{}));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test40() {
		try {
			ch.agent.util.base.Misc.join(null, new String[]{});
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("separator null", e.getMessage());
		}
	}
	@Test
	public void test50() {
		try {
			ch.agent.util.base.Misc.join("", (String[])null);
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("items null", e.getMessage());
		}
	}

	@Test
	public void testJoin1() {
		try {
			assertEquals("a-b-c", Misc.join("-", new String[]{"a", "b", "c"}));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testJoin2() {
		try {
			Collection<String> s = new ArrayList<String>();
			s.add("a");
			s.add("b");
			s.add("c");
			assertEquals("a-b-c", Misc.join("-", s));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testJoin3() {
		try {
			Collection<String> s = new ArrayList<String>();
			assertEquals("", Misc.join("-", s));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testJoin4() {
		try {
			assertEquals(null, Misc.join("-", (String[])null));
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			assertEquals("items null", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
