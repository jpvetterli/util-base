package ch.agent.util.ioc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DAGTest {
	
	private DAG<String> sdag;
	private DAG<Integer> idag;

	@Before
	public void setUp() throws Exception {
		// this is the example in https://en.wikipedia.org/wiki/Topological_sorting
		sdag = new DAG<String>();
		sdag.add("5", "7", "3", "11", "8", "2", "9", "10");
		sdag.addLinks("5", "11");
		sdag.addLinks("7", "11", "8");
		sdag.addLinks("3", "8", "10");
		sdag.addLinks("11", "2", "9", "10");
		sdag.addLinks("8", "9");
		
		idag = new DAG<Integer>();
		idag.add(5, 7, 3, 11, 8, 2, 9, 10);
		idag.addLinks(5, 11);
		idag.addLinks(7, 11, 8);
		idag.addLinks(3, 8, 10);
		idag.addLinks(11, 2, 9, 10);
		idag.addLinks(8, 9);

	}

	@Test
	public void test10() {
		try {
			List<String> sorted = sdag.sort();
			assertEquals("[2, 9, 10, 11, 5, 8, 7, 3]", sorted.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test20() {
		try {
			sdag.addLinks("9", "3");
			sdag.sort();
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("cycle: 9", e.getMessage());
		}
		
	}

	@Test
	public void test30() {
		try {
			sdag.sort();
			List<String> sorted = sdag.sort(); // 2nd time
			assertEquals("[2, 9, 10, 11, 5, 8, 7, 3]", sorted.toString());
			Collections.reverse(sorted); // just playing
			assertEquals("[3, 7, 8, 5, 11, 10, 9, 2]", sorted.toString());
			Collections.reverse(sorted); // you never know...
			assertEquals("[2, 9, 10, 11, 5, 8, 7, 3]", sorted.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test110() {
		try {
			List<Integer> sorted = idag.sort();
			assertEquals("[2, 9, 10, 11, 5, 8, 7, 3]", sorted.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test120() {
		try {
			idag.addLinks(9, 3);
			idag.sort();
			fail("exception expected");
		} catch (Exception e) {
			assertEquals("cycle: 9", e.getMessage());
		}
		
	}

	@Test
	public void test130() {
		try {
			idag.sort();
			List<Integer> sorted = idag.sort(); // 2nd time
			assertEquals("[2, 9, 10, 11, 5, 8, 7, 3]", sorted.toString());
			Collections.reverse(sorted); // just playing
			assertEquals("[3, 7, 8, 5, 11, 10, 9, 2]", sorted.toString());
			Collections.reverse(sorted); // you never know...
			assertEquals("[2, 9, 10, 11, 5, 8, 7, 3]", sorted.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
