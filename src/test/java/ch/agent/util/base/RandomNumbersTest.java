package ch.agent.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.base.RandomNumbers.RandomBurst;
import ch.agent.util.base.RandomNumbers.Uniform;
import ch.agent.util.base.RandomNumbers.Uniform2;

public class RandomNumbersTest {
	

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void test1() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(3,  3);
			rb.setSmallIncrement(1, 1);
			rb.setLargeIncrement(10, 10);
			rb.setBase(0);
			
			assertEquals(10, rb.next());
			assertEquals(11, rb.next());
			assertEquals(12, rb.next());
			assertEquals(22, rb.next());
			assertEquals(23, rb.next());
			assertEquals(24, rb.next());
			assertEquals(34, rb.next());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test2() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(3,  3);
			rb.setSmallIncrement(1, 1);
			rb.setLargeIncrement(10, 10);
			assertEquals(10, rb.next());
			assertEquals(11, rb.next());
			assertEquals(12, rb.next());
			assertEquals(22, rb.next());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test3() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(3,  3);
			rb.setSmallIncrement(1, 1);
			rb.setLargeIncrement(10, 10);
			rb.setBase(-1);
			long n = rb.next();
			assertTrue(n > 1000000);
			assertEquals(n + 1, rb.next());
			assertEquals(n + 2, rb.next());
			assertEquals(n + 12, rb.next());
			assertEquals(n + 13, rb.next());
			assertEquals(n + 14, rb.next());
			assertEquals(n + 24, rb.next());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}


	@Test
	public void test4() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(3,  3);
			rb.setSmallIncrement(0, 0);
			rb.setLargeIncrement(10, 10);
			rb.setBase(0);
			assertEquals(10, rb.next());
			assertEquals(20, rb.next());
			assertEquals(30, rb.next());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test5() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(0,  0);
			rb.setSmallIncrement(1, 1);
			rb.setLargeIncrement(10, 10);
			rb.setBase(0);
			assertEquals(0, rb.next());
			assertEquals(0, rb.next());
			assertEquals(0, rb.next());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test6() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(1,  1);
			rb.setSmallIncrement(1, 1);
			rb.setLargeIncrement(10, 10);
			rb.setBase(0);
			assertEquals(10, rb.next());
			assertEquals(20, rb.next());
			assertEquals(30, rb.next());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test8() {
		try {
			RandomBurst rb = new RandomBurst();
			rb.setBurstSize(3,  3);
			rb.setSmallIncrement(1, 1);
			rb.setLargeIncrement(10, 10);
			rb.setBase(0);
			assertTrue(rb.test(0));
			assertFalse(rb.test(0));
			assertFalse(rb.test(9));
			assertTrue(rb.test(10));
			assertTrue(rb.test(11));
			assertTrue(rb.test(42)); // 12
			assertFalse(rb.test(21));
			assertTrue(rb.test(22));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test20() {
		try {
			// bug found: test(-1) right after setBase(-1) always returns true 
			int N = 100;
			int count = 0;
			RandomBurst rb = new RandomBurst();
			for (int i = 0; i < N; i++) {
				rb.setBase(-1);
				if (rb.test(-1))
					count++;
			}
			assertTrue(count == N);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test100() {
		try {
			Uniform uni = new Uniform(-20, -15);
			assertEquals("[-20, -15]", uni.toString());
			int[] counters = new int[6];
			for (int i = 0; i < 6000; i++) {
				int n = uni.next();
				counters[n + 20]++;
			}
			for (int n : counters) {
				assertTrue(n > 900);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void test102() {
		try {
			Uniform uni = new Uniform(-20, -20);
			assertEquals("[-20, -20]", uni.toString());
			for (int i = 0; i < 10; i++) {
				assertEquals(-20, uni.next());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void test200() {
		try {
			Uniform2 uni = new Uniform2(50, 100, 50, 100, 0, 199);
			assertEquals("[50, 100] [0, 199] low=50 high=100", uni.toString());
			int[] counters = new int[200];
			for (int i = 0; i < 20000; i++) {
				int n = uni.next();
				counters[n]++;
			}
			for (int i = 0; i < counters.length; i++) {
				if (i > 50 && i < 100) {
					assertTrue(counters[i] > 250);
				} else
					assertTrue(counters[i] < 25);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
