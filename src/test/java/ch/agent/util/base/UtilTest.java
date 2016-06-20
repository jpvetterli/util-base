package ch.agent.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

public class UtilTest {
	

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void test10() {
		try {
			String[] strings = new String[]{"foo", "bar"};
			assertEquals("foo bar", ch.agent.util.base.Util.join(" ", strings));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void test20() {
		try {
			String[] strings = new String[]{"foo", "bar"};
			assertEquals("foo bar", ch.agent.util.base.Util.join(" ", Arrays.asList(strings)));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void test30() {
		try {
			assertEquals("", ch.agent.util.base.Util.join("", new String[]{}));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test40() {
		try {
			ch.agent.util.base.Util.join(null, new String[]{});
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("separator null", e.getMessage());
		}
	}
	@Test
	public void test50() {
		try {
			ch.agent.util.base.Util.join("", (String[])null);
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("items null", e.getMessage());
		}
	}

}
