package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;

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
	public void testVars15() {
		try {
			args.parse("$a=a $b=b $c = c");
			Args args2 = new Args();
			args2.def("foo").repeatable();
			args2.putVariables(args);
			args2.parse("$c=x $d=d foo=$$a foo=$$b foo=$$c foo=$$d");
			assertEquals("a", args2.split("foo")[0]);
			assertEquals("b", args2.split("foo")[1]);
			assertEquals("c", args2.split("foo")[2]);
			assertEquals("d", args2.split("foo")[3]);
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
	public void testVariables3() {
		try {
			// last wins: use reset
			args.putVariable("$HOP", "hop");
			args.parse("reset=$HOP");
			boolean result = args.putVariable("$HOP", "hophop");
			args.def("foo");
			args.parse("foo = [$$HOP la boum]");
			assertEquals("hophop la boum", args.get("foo"));
			assertTrue(result);
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
	public void testInfLoop3() {
		// bug in util-ioc-akka ServiceTest#test4: unresolved variables
		try {
			args.def("x");
			
			// this does not loop:
			args.parse("$RESOLVED=resolved x=[a=$$UNRESOLVED b=x]");
			assertEquals("a=$$UNRESOLVED b=x", args.getVal("x").rawValue());
			args.reset(); 
	
			// this does not loop:
			args.parse("$RESOLVED=resolved x=[a=$$UNRESOLVED b=$$UNRESOLVED2]");
			assertEquals("a=$$UNRESOLVED b=$$UNRESOLVED2", args.getVal("x").rawValue());
			args.reset(); 
			
			// this loops:
			args.parse("$RESOLVED=resolved x=[a=$$UNRESOLVED b=$$RESOLVED]"); 
			assertEquals("a=$$$UNRESOLVED$ b=resolved", args.getVal("x").rawValue());
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			fail("unexpected exception");
		}
	}

	
	@Test
	public void testInfLoop3_ServiceTest_test4_a() {
		// version with non-empty $SVC-REQ
		try {
			String[] spec = new String[]{
				"$SVC-AC=CaseChangerServiceActor" ,
				"$SVC-BODY=[name = $$SVC-NAME $$SVC-REQ actor-class=$$SVC-AC]", 
				"module=[$SVC-NAME=CCS1 $SVC-REQ=[require=FOO] $$SVC-BODY]", 
			};
			
			args.def("module").repeatable();
			args.parse(spec);
			String expect = "$SVC-NAME=CCS1 $SVC-REQ=[require=FOO] name = $$$SVC-NAME$ $$$SVC-REQ$ actor-class=CaseChangerServiceActor";
			assertEquals(expect, args.getVal("module").rawValues()[0]);

			Args args2 = new Args();
			args2.def("name");
			args2.def("require").repeatable();
			args2.def("actor-class");
			args2.parse(expect);
			assertEquals("CCS1", args2.get("name"));
			assertEquals("FOO", args2.split("require")[0]);
			assertEquals("CaseChangerServiceActor", args2.get("actor-class"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testInfLoop3_ServiceTest_test4_b() {
		// version with empty $SVC-REQ
		try {
			String[] spec = new String[]{
				"$SVC-AC=CaseChangerServiceActor" ,
				"$SVC-BODY=[name = $$SVC-NAME $$SVC-REQ actor-class=$$SVC-AC]", 
				"module=[$SVC-NAME=CCS1 $SVC-REQ=[] $$SVC-BODY]", 
			};
			
			args.def("module").repeatable();
			args.parse(spec);
			String expect = "$SVC-NAME=CCS1 $SVC-REQ=[] name = $$$SVC-NAME$ $$$SVC-REQ$ actor-class=CaseChangerServiceActor";
			assertEquals(expect, args.getVal("module").rawValues()[0]);

			Args args2 = new Args();
			args2.def("name");
			args2.def("require").repeatable();
			args2.def("actor-class");
			args2.parse(expect);
			assertEquals("CCS1", args2.get("name"));
			assertEquals(0, args2.split("require").length);
			assertEquals("CaseChangerServiceActor", args2.get("actor-class"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
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
	
	@Test
	public void testSubroutines3() {
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
			
			// instead of general reset, reset only where necessary
			args2.parse("reset=[$ARG1 $ARG2]");
			
			args2.parse(args.getVal("fooa").rawValue());
			assertEquals("a", args2.get("arg1"));
			assertEquals("b", args2.get("arg2"));

		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	private void print (String comment, Map<String, String> dict) {
		System.out.println(Misc.isEmpty(comment) ? "(no comment)" : comment);
		for (String key : dict.keySet())
			System.out.println(key + " : " + dict.get(key));
	}
	
	@Test
	public void testSubroutines4a() {
		try {
			args.parse("$MACRO=[$TEXT=[<<<$$SYMBOL>>>]]"); 
			args.parse("$SYMBOL=1 $$MACRO");
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
			// reset relevant variables before modifying
			args.parse("reset=[$SYMBOL $TEXT] $SYMBOL=2 $$MACRO");
			assertEquals("<<<2>>>", args.getVariables().get("TEXT"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSubroutines4b() {
		try {
			// yes, $TEXT must be reset too
			args.parse("$MACRO=[$TEXT=[<<<$$SYMBOL>>>]]"); 
			args.parse("$SYMBOL=1 $$MACRO");
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
			args.parse("reset=[$SYMBOL] $SYMBOL=2 $$MACRO");
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testSubroutines5a() {
		try {
			args.parse("$MACRO=[a=[b=x+$$SYMBOL+y] $TEXT=[<<<$$SYMBOL>>>]]"); 
			args.def("a");
			args.parse("$SYMBOL=1 $$MACRO");
			if (DEBUG) {
				print("*** after parsing \"$SYMBOL=1 $$MACRO\" ***", args.getVariables());
				System.out.println("a : " + args.getVal("a").rawValue());
			}
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
			assertEquals("b=x+1+y", args.getVal("a").rawValue());
			assertEquals("b=x+1+y", args.get("a"));
			args.parse("reset=[$SYMBOL $TEXT] $SYMBOL=2 $$MACRO");
			if (DEBUG) {
				print("*** after parsing \"$SYMBOL=2 $$MACRO\" ***", args.getVariables());
				System.out.println("a : " + args.getVal("a").rawValue());
			}
			assertEquals("<<<2>>>", args.getVariables().get("TEXT"));
			assertEquals("b=x+2+y", args.getVal("a").rawValue());
			assertEquals("b=x+2+y", args.get("a"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSubroutines5b() {
		try {
			args.parse("$MACRO=[a=[b=x+$$SYMBOL+y] $TEXT=[<<<$$SYMBOL>>>]]"); 
			args.def("a").repeatable();
			args.parse("$SYMBOL=1 $$MACRO");
			if (DEBUG) {
				System.err.println("*** after parsing \"$SYMBOL=1 $$MACRO\" ***");
				args.parse("dump=[$SYMBOL $TEXT $MACRO a]");
			}
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
			assertEquals("[b=x+1+y]", args.getVal("a").rawValue());
			assertEquals("[b=x+1+y]", args.get("a"));
			args.parse("reset=[$SYMBOL $TEXT] $SYMBOL=2 $$MACRO");
			if (DEBUG) {
				System.err.println("*** after parsing \"$SYMBOL=2 $$MACRO\" ***");
				args.parse("dump=[$SYMBOL $TEXT $MACRO a]");
			}
			assertEquals("<<<2>>>", args.getVariables().get("TEXT"));
			assertEquals("[b=x+1+y] [b=x+2+y]", args.getVal("a").rawValue());
			assertEquals("[b=x+1+y] [b=x+2+y]", args.get("a"));
			
			// nota bene: when a is repeatable, previous value is not replaced but appended to...
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSubroutines5c() {
		try {
			args.parse("$MACRO=[reset=a a=[b=x+$$SYMBOL+y] $TEXT=[<<<$$SYMBOL>>>]]"); 
			args.def("a").repeatable();
			args.parse("$SYMBOL=1 $$MACRO");
			if (DEBUG) {
				print("*** after parsing \"$SYMBOL=1 $$MACRO\" ***", args.getVariables());
				System.out.println("a : " + args.getVal("a").rawValue());
			}
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
			assertEquals("[b=x+1+y]", args.getVal("a").rawValue());
			assertEquals("[b=x+1+y]", args.get("a"));
			args.parse("reset=[$SYMBOL $TEXT] $SYMBOL=2 $$MACRO");
			if (DEBUG) {
				print("*** after parsing \"$SYMBOL=2 $$MACRO\" ***", args.getVariables());
				System.out.println("a : " + args.getVal("a").rawValue());
			}
			assertEquals("<<<2>>>", args.getVariables().get("TEXT"));
			assertEquals("[b=x+2+y]", args.getVal("a").rawValue());
			assertEquals("[b=x+2+y]", args.get("a"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSubroutines5d() {
		try {
			args.parse("$MACRO=[reset=a a=[b=x+$$SYMBOL+y] $TEXT=[<<<$$SYMBOL>>>]]"); 
			args.def("a").repeatable();
			args.parse("$SYMBOL=1 $$MACRO");
			if (DEBUG) {
				print("*** after parsing \"$SYMBOL=1 $$MACRO\" ***", args.getVariables());
				System.out.println("a : " + args.getVal("a").rawValue());
			}
			assertEquals("<<<1>>>", args.getVariables().get("TEXT"));
			assertEquals("[b=x+1+y]", args.getVal("a").rawValue());
			assertEquals("[b=x+1+y]", args.get("a"));
			args.parse("reset=[$SYMBOL $TEXT] $SYMBOL=2 $$MACRO");
			if (DEBUG) {
				print("*** after parsing \"$SYMBOL=2 $$MACRO\" ***", args.getVariables());
				System.out.println("a : " + args.getVal("a").rawValue());
			}
			assertEquals("<<<2>>>", args.getVariables().get("TEXT"));
			assertEquals("[b=x+2+y]", args.getVal("a").rawValue());
			assertEquals("[b=x+2+y]", args.get("a"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testConditionBug1() {
		try {
			args.parse("$MACRO=[$TEXT=[<$$SYMBOL>]]"); 
			
			args.parse("$SYMBOL=1 $$MACRO");
			if (DEBUG) {
				System.err.println("*** $SYMBOL=1 ***");
				args.parse("dump=[$SYMBOL $TEXT]");
			}
			assertEquals("<1>", args.getVariables().get("TEXT"));
			
			// something was wrong with conditional, final TEXT was <1>, not <2>
			
			args.parse( ""
				+ "dump=[] " 
				+ "$TRUE=42 condition=[if=[$$TRUE] then=[" 
				+ "  reset=[$SYMBOL $TEXT] " 
				+ "  $SYMBOL=2 $$MACRO " 
				+ "]]"
			);
			if (DEBUG) {
				System.err.println("*** $SYMBOL=2 ***");
				args.parse("dump=[$SYMBOL $TEXT]");
			}
			assertEquals("<2>", args.getVariables().get("TEXT"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testCondition2() {
		try {
			args.parse("$MACRO=[$TEXT=[<$$SYMBOL>]]"); 
			
			args.parse("$SYMBOL=1 $$MACRO");
			if (DEBUG) {
				System.err.println("*** $SYMBOL=1 ***");
				args.parse("dump=[$SYMBOL $TEXT]");
			}
			assertEquals("<1>", args.getVariables().get("TEXT"));
			
			// something was wrong with conditional, final TEXT was <1>, not <2>
			
			args.parse( ""
				+ "dump=[] " 
				+ "condition=[if=[$$MISSING] then=[] else=[" 
				+ "  reset=[$SYMBOL $TEXT] " 
				+ "  $SYMBOL=2 $$MACRO " 
				+ "]]"
			);
			if (DEBUG) {
				System.err.println("*** $SYMBOL=2 ***");
				args.parse("dump=[$SYMBOL $TEXT]");
			}
			assertEquals("<2>", args.getVariables().get("TEXT"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSubroutinesSPECIAL_1() {
		try {
			args.def("a").repeatable();
			args.def("module").repeatable();
			args.parse(
					"$MAVS-MACRO=[ reset=[a module] " + 
					"  a=[b1=x+$$SYMBOL+y] " + 
					"  a=[b2=x+$$SYMBOL+y] " + 
					"  $LOCAL-MAVS-URL = [chart-$$$SYMBOL$MAVS.svg] " + 
					"  $$MAVS-URL = $$LOCAL-MAVS-URL " + 
					"  $$MAVS-TEXT = [<b style=\"font-size: 150%\">$$SYMBOL</b><br/>" + 
					"  <span style=\"color: $$COLOR-1\">&bullet;</span>$$$MAVS-WINDOW-XS$,]" +  
					"  module = [name=MAVS-XS+$$SYMBOL $MEAN=MAVS-XS $WINDOW=$$MAVS-WINDOW-XS $$MEAN-MACRO]" + 
					"  module = [name=MAVS-L+$$SYMBOL $MEAN=MAVS-L $WINDOW=$$MAVS-WINDOW-L $$MEAN-MACRO]" + 
					"]"
			); 
			if (DEBUG) print("*** after parsing $MAVS-MACRO ***", args.getVariables());
			args.parse("$MAVS-URL=$MAVS-URL-1 $MAVS-TEXT=$MAVS-TEXT-1 $SYMBOL=XX1 $$MAVS-MACRO");
			if (DEBUG) {
				print("*** call 1 ... ***", args.getVariables());
				System.out.println("a : " + Arrays.toString(args.getVal("a").rawValues()));
				System.out.println("module : " + Arrays.toString(args.getVal("module").rawValues()));
			}
			
			args.parse("reset=[$SYMBOL $LOCAL-MAVS-URL $MAVS-URL $MAVS-TEXT] $MAVS-URL=$MAVS-URL-2 $MAVS-TEXT=$MAVS-TEXT-2 $SYMBOL=ZZ2 $$MAVS-MACRO");
			if (DEBUG) {
				print("*** call 2 ... ***", args.getVariables());
				System.out.println("a : " + Arrays.toString(args.getVal("a").rawValues()));
				System.out.println("module : " + Arrays.toString(args.getVal("module").rawValues()));
			}
			assertEquals("[name=MAVS-XS+ZZ2 $MEAN=MAVS-XS $WINDOW=$$$MAVS-WINDOW-XS$ $$$MEAN-MACRO$] [name=MAVS-L+ZZ2 $MEAN=MAVS-L $WINDOW=$$$MAVS-WINDOW-L$ $$$MEAN-MACRO$]", args.getVal("module").rawValue());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	
}
