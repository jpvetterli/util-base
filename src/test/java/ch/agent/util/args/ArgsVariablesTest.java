package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.STRINGS.U;

public class ArgsVariablesTest {

	private static final boolean DEBUG = false;

	private static void assertMessage(Throwable e, String prefix) {
		assertEquals(prefix, e.getMessage().substring(0, 6));
	}

	private Args args;

	@Before
	public void setUp() throws Exception {
		args = new Args();
	}

	@Test
	public void testVars01() {
		try {
			args.def("foo");
			args.parse("$a=b $c=$$a foo=$$c");
			assertEquals("b", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVars02() {
		try {
			args.def("foo");
			args.parse("$a=525 $c=$$a foo=$$c");
			assertEquals(525, args.getVal("foo").intValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars03() {
		try {
			args.def("foo");
			args.parse("$a=true $c=$$a foo=$$c");
			assertEquals(Boolean.TRUE, args.getVal("foo").booleanValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars04() {
		try {
			args.def("foo").repeatable();
			args.parse("$a=1 $b=2 foo=$$a foo=$$b");
			int[] values = args.getVal("foo").intValues();
			assertEquals(1, values[0]);
			assertEquals(2, values[1]);
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars05() {
		try {
			args.def("foo");
			args.parse("$a=b $c=$$a foo=[ $$c ]");
			assertEquals(" b ", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars06() {
		try {
			args.def("foo");
			args.parse("$a=b $c=[ $$a ] foo=[ x$$cx ]");
			assertEquals(" x$$cx ", args.getVal("foo").rawValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars06a() {
		try {
			args.def("foo");
			args.parse("$a=b $c=[ $$a ] foo=[x$$$c$x]");
			assertEquals("x b x", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars07() {
		try {
			// escaping has no effect on variables
			args.def("foo");
			args.parse("$a=b $c=\\$$a foo=$$c");
			assertEquals("\\b", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars08() {
		try {
			// escaping has no effect on variables
			args.def("foo");
			args.parse("$a=b $c=$$a foo=\\$$c");
			assertEquals("\\b", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVars09() {
		try {
			args.def("$foo");
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00121));
		}
	}
	@Test
	public void testVars10() {
		try {
			// unresolved variable
			args.def("foo");
			args.put("foo", "$$bar");
			assertEquals("$$bar", args.getVal("foo").rawValue());
		} catch (Exception e) {
			fail("unexpected exception");
			assertTrue(e.getMessage().startsWith(U.U00122));
		}
	}
	@Test
	public void testVars10a() {
		try {
			args.def("foo");
			args.put("foo", "$$ bar");
			assertEquals("$$ bar", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars10b() {
		try {
			args.def("foo");
			args.put("foo", "$$");
			assertEquals("$$", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars11() {
		try {
			// test "the first wins"
			args.def("foo");
			args.parse("$a=b $a=B $c=$$a foo=$$c");
			assertEquals("b", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVars12() {
		try {
			args.def("module");
			args.parse("$VAR=varvalue module=[name=a class=x pred=$$VAR " + 
						"conf=[svc = $$VAR] count=2]");
			assertEquals("name=a class=x pred=varvalue " + 
						"conf=[svc = varvalue] count=2", args.get("module"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVars12a() {
		try {
			args.def("a");
			args.parse("a=[$$x $$x]");
			assertEquals("$$x $$x", args.getVal("a").rawValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVars12b() {
		try {
			args.def("a");
			args.parse("$x=bb a=[$$x $$x]");
			assertEquals("bb bb", args.get("a"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testVars13() {
		
		try {
			args.def("conf");
			args.parse("$VAR=varvalue conf=[svc = $$VAR]");
			assertEquals("svc = varvalue", args.get("conf"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testVariables1() {
		try {
			args.def("foo");
			args.parse("$HOP = hop $YET = another foo = [$$HOP la boum]");
			assertEquals("hop la boum", args.get("foo"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVariables1A() {
		// symbol cannot be empty
		try {
			args.def("foo");
			args.parse("$ = hop foo = [$$[] la boum]");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00124);
		}
	}
	
	@Test
	public void testVariables1B() {
		// symbol cannot be empty
		try {
			args.def("foo");
			args.parse("$EMPTY = hop foo = [$$[] la boum]");
			assertEquals("$$[] la boum", args.get("foo"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVariables2() {
		try {
			// first wins
			args.putVariable("$HOP", "hop");
			boolean result = args.putVariable("$HOP", "hophop");
			args.def("foo");
			args.parse("foo = [$$HOP la boum]");
			assertEquals("hop la boum", args.get("foo"));
			assertFalse(result);
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testInfLoop1() {
		try {
			// infinite recursion?
			args.putVariable("$HOP", "$$HOP");
			args.def("foo");
			args.parse("foo = [$$HOP la boum]");
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00123);
		}
	}
	
	@Test
	public void testInfLoop2() {
		try {
			// infinite recursion?
			args.putVariable("$TIC", "$$TAC");
			args.putVariable("$TAC", "$$TIC");
			args.def("foo");
			args.parse("foo = [$$TIC la boum]");
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00123);
		}
	}
	
	@Test
	public void testBadVariable1() {
		try {
			args.putVariable("", "val0");
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00126);
		}
	}
	@Test
	public void testBadVariable2() {
		try {
			args.putVariable("$", "val0");
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00124);
		}
	}
	
	@Test
	public void testBadVariable3() {
		try {
			args.putVariable("$foo$", "val0");
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00125);
		}
	}

	@Test
	public void testBadVariable4() {
		try {
			args.putVariable("$$foo", "val0");
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00125);
		}
	}
	
	@Test
	public void testBadVariable5() {
		try {
			// this one is good
			args.putVariable("$foo", "val0");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testLateResolution01() {
		try {
			args.def("foo");
			args.def("bar");
			args.parse("$x=1 bar=$$x foo=$$y");
			assertEquals("$$y", args.getVal("foo").rawValue());
			assertEquals("1", args.get("bar"));
			assertEquals(1, args.getVal("bar").intValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testLateResolution02() {
		try {
			args.def("foo");
			args.parse("foo=$$y");
			assertEquals("$$y", args.get("foo"));
			assertEquals(1, args.getVal("foo").intValue());
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00107);
		}
	}
	
	@Test
	public void testLateResolution02a() {
		try {
			args.def("foo");
			args.parse("foo=$$y");
			assertEquals("$$y", args.get("foo"));
			assertEquals("anything", args.getVal("foo").stringValue());
			fail("exception expected");
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			assertMessage(e, U.U00107);
		}
	}

	@Test
	public void testLateResolution03() {
		try {
			args.def("foo").init("1");
			args.parse("foo=$$y");
			assertEquals("$$y", args.getVal("foo").rawValue());
			assertEquals("1", args.getVal("foo").stringValue());
			assertEquals(1, args.getVal("foo").intValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testSubroutines1() {
		// no escaping of substitution variables
		try {
			args.def("foox");
			args.def("fooa");
			args.parse(
				"$BODY = [arg1=\\$$ARG1 arg2=\\$$ARG2] " + 
				"foox=[$ARG1=x $ARG2=y $$BODY] " + 
				"fooa=[$ARG1=a $ARG2=b $$BODY]");
			assertEquals("$ARG1=x $ARG2=y arg1=\\$$ARG1 arg2=\\$$ARG2", args.getVal("foox").rawValue());
			assertEquals("$ARG1=a $ARG2=b arg1=\\$$ARG1 arg2=\\$$ARG2", args.getVal("fooa").rawValue());

			Args args2 = new Args();
			args2.def("arg1");
			args2.def("arg2");
			args2.parse(args.getVal("foox").rawValue());
			assertEquals("\\x", args2.get("arg1"));
			assertEquals("\\y", args2.get("arg2"));
			
			// without reset, the values are still x and y because "the first wins"
			args2.parse(args.getVal("fooa").rawValue());
			assertEquals("\\x", args2.get("arg1"));
			assertEquals("\\y", args2.get("arg2"));

			// after reset, all $variables have disappeared
			args2.reset();
			args2.parse(args.getVal("fooa").rawValue());
			assertEquals("\\a", args2.get("arg1"));
			assertEquals("\\b", args2.get("arg2"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSubroutines2() {
		try {
			args.def("foox");
			args.def("fooa");
			args.parse(
				"$BODY = [arg1=$$ARG1 arg2=$$ARG2] " + 
				"foox=[$ARG1=x $ARG2=y $$BODY] " + 
				"fooa=[$ARG1=a $ARG2=b $$BODY]");
			assertEquals("$ARG1=x $ARG2=y arg1=$$ARG1 arg2=$$ARG2", args.getVal("foox").rawValue());
			assertEquals("$ARG1=a $ARG2=b arg1=$$ARG1 arg2=$$ARG2", args.getVal("fooa").rawValue());

			Args args2 = new Args();
			args2.def("arg1");
			args2.def("arg2");
			args2.parse(args.getVal("foox").rawValue());
			assertEquals("x", args2.get("arg1"));
			assertEquals("y", args2.get("arg2"));
			
			// without reset, the values are still x and y because "the first wins"
			args2.parse(args.getVal("fooa").rawValue());
			assertEquals("x", args2.get("arg1"));
			assertEquals("y", args2.get("arg2"));

			// after reset, all $variables have disappeared
			args2.reset();
			args2.parse(args.getVal("fooa").rawValue());
			assertEquals("a", args2.get("arg1"));
			assertEquals("b", args2.get("arg2"));
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}

}