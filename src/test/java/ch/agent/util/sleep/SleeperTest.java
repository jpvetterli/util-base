package ch.agent.util.sleep;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SleeperTest {

	public static class SleeperTestClient implements SleeperClient {

		public int count = 0;
		public int limit = -1;
		
		@Override
		public boolean continueSleeping() {
			count++;
			return limit < 0 || limit > count;
		}
		
	}
	
	private SleeperTestClient testClient;
	
	@Before
	public void setUp() throws Exception {
		testClient = new SleeperTestClient();
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testSleep1() {
		try {
			Sleeper sleeper = new Sleeper();
			sleeper.setSleeperClient(testClient, 100); // 100 ms
			sleeper.setSleepUnit(100, 100); // exactly 100 ms
			sleeper.sleep(10);
			assertEquals(10, testClient.count);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSleep2() {
		try {
			Sleeper sleeper = new Sleeper();
			sleeper.setSleeperClient(testClient, 100); // 100 ms
			sleeper.setSleepUnit(100, 100); // exactly 100 ms
			testClient.count = 0;
			testClient.limit = 5;
			sleeper.sleep(10);
			assertEquals(5, testClient.count);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSleep3() {
		try {
			Sleeper sleeper = new Sleeper();
			sleeper.setSleeperClient(testClient, 100); // 100 ms
			sleeper.parse("100 - 100"); // exactly 100 ms
			testClient.count = 0;
			testClient.limit = 5;
			sleeper.sleep(10);
			assertEquals(5, testClient.count);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSleep4() {
		try {
			Sleeper sleeper = new Sleeper();
			sleeper.parse("foo"); // exactly 100 ms
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00301"));
		}
	}

	@Test
	public void testSleep5() {
		try {
			Sleeper sleeper = new Sleeper();
			sleeper.setSleeperClient(testClient); // 300 ms
			sleeper.setSleepUnit(100, 100); // exactly 100 ms
			sleeper.sleep(10);
			assertEquals(4, testClient.count); // 3 + 1 with remaining
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	
}
