package ch.agent.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
	
	@Test
	public void testJoin5() {
		try {
			Collection<String> s = new ArrayList<String>();
			s.add("a");
			s.add("b");
			s.add("c");
			assertEquals("[1, 2, 3]", Misc.join("[", ", ", "]", new Integer[]{1, 2, 3}));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	
	@Test
	public void testSplit01() {
		try {
			String[] parts = Misc.split("a:b", ":", 2);
			assertEquals(2, parts.length);
			assertEquals("a", parts[0]);
			assertEquals("b", parts[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testSplit02() {
		try {
			String[] parts = Misc.split("a :   b", "\\s*:\\s*", 2);
			assertEquals(2, parts.length);
			assertEquals("a", parts[0]);
			assertEquals("b", parts[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testSplit03() {
		try {
			String[] parts = Misc.split("a --   b", "--", 2);
			assertEquals(2, parts.length);
			assertEquals("a ", parts[0]);
			assertEquals("   b", parts[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testSplit04() {
		try {
			String[] parts = Misc.split("[a1,b2, c3, d4]", "\\s*,\\s*", 4);
			assertEquals(4, parts.length);
			assertEquals("[a1", parts[0]);
			assertEquals("b2", parts[1]);
			assertEquals("c3", parts[2]);
			assertEquals("d4]", parts[3]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testSplit05() {
		try {
			Misc.split("[a1,b2, c3, d4]", "\\s*,\\s*", 5);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			assertEquals("4!=5", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit06() {
		try {
			String[] parts = Misc.split("[a1,b2, c3, d4]", "\\s*,\\s*", 0);
			assertEquals(0, parts.length);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testSplit07() {
		try {
			String[] parts = Misc.split("[a1,b2, c3, d4]", "\\s*,\\s*", 1);
			assertEquals(1, parts.length);
			assertEquals("[a1,b2, c3, d4]", parts[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit08() {
		try {
			String[] parts = Misc.split("","-", -1);
			assertEquals(0, parts.length);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSplit09() {
		try {
			String[] parts = Misc.split("","-", 1);
			assertEquals(1, parts.length);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testSplit10() {
		try {
			Misc.split("", "\\s+", 2);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			assertEquals("1!=2", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testTruncate01() {
		try {
			assertEquals("hopla", Misc.truncate("hopla", 0, null));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testTruncate02() {
		try {
			assertEquals(null, Misc.truncate(null, 0, null));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testTruncate03() {
		try {
			assertEquals("0123456789", Misc.truncate("0123456789", 10, null));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testTruncate04() {
		try {
			assertEquals("0123456789...", Misc.truncate("0123456789A", 10, null));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testTruncate05() {
		try {
			assertEquals("0123456789[...]", Misc.truncate("0123456789A", 10, "[...]"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testTruncate06() {
		try {
			String ellipsis = "[...]";
			assertEquals("01234[...]", Misc.truncate("0123456789A", 10 - ellipsis.length(), ellipsis));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	private static final String SENTENCE = "As an example, this sentence would be returned as (position=35, length=50):";
	@Test
	public void testMark01() {
		try {
			String marked = Misc.mark(SENTENCE, 35,  81); 
			assertEquals("As an example, this sentence would [-->]be returned as (position=35, length=50):", marked);
			assertEquals(80, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testMark01a() {
		try {
			String marked = Misc.mark(SENTENCE, 74,  80); 
			assertEquals("As an example, this sentence would be returned as (position=35, length=50)[-->]:", marked);
			assertEquals(80, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testMark02() {
		try {
			String marked = Misc.mark(SENTENCE, 35,  80); 
			assertEquals("As an example, this sentence would [-->]be returned as (position=35, length=50):", marked);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testMark03() {
		try {
			String marked = Misc.mark(SENTENCE, 35,  79); 
			assertEquals("As an example, this sentence would [-->]be returned as (position=35, lengt[...]", marked);
			assertEquals(79, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testMark04() {
		try {
			String marked = Misc.mark(SENTENCE, 35,  75); 
			assertEquals("As an example, this sentence would [-->]be returned as (position=35, l[...]", marked);
			assertEquals(75, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testMark04a() {
		try {
			String marked = Misc.mark(SENTENCE, 51,  75); 
			assertEquals("As an example, this [...]ould be returned as ([-->]position=35, length=50):", marked);
			assertEquals(75, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMark05() {
		try {
			String marked = Misc.mark(SENTENCE, 35,  65); 
			assertEquals("As an example, t[...] sentence would [-->]be returned as (po[...]", marked);
			assertEquals(65, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testMark05a() {
		try {
			String marked = Misc.mark(SENTENCE, 75,  65); 
			assertEquals("As an example, this sentenc[...]as (position=35, length=50):[-->]", marked);
			assertEquals(65, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMark06() {
		try {
			String marked = Misc.mark(SENTENCE+SENTENCE+SENTENCE+SENTENCE, 155,  65); 
			assertEquals("As an example, t[...]length=50):As an[-->] example, this sen[...]", marked);
			assertEquals(65, marked.length()); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNotClose01() {
		try {
			assertTrue(Misc.notClose(1, 2));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNotClose02() {
		try {
			assertFalse(Misc.notClose(1, 1));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNotClose03() {
		try {
			assertTrue(Misc.notClose(1, 1+1e-9, 1e-10));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNotClose04() {
		try {
			assertFalse(Misc.notClose(1, 1+1e-11, 1e-10));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
