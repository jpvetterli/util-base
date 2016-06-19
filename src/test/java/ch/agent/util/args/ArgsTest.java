package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.UtilMsg.U;

public class ArgsTest {

	private static void assertMessage(Throwable e, String prefix) {
		assertEquals(prefix, e.getMessage().substring(0, 6));
	}
	
	private Args args;
	private String file1;
	private String file2;
	private String file3;
	
	@Before
	public void setUp() throws Exception {
		args = new Args();
		file1 = "ArgsTest.test1";
		file2 = "ArgsTest.test2";
		file3 = "ArgsTest.test3";
	}
	
	@Test
	public void testSimpleDefine() {
		try {
			args.def("foo");
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testDuplicateDefine() {
		try {
			args.def("foo");
			args.def("foo");
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00104);
		}
	}
	
	@Test
	public void testDefault() {
		try {
			args.def("foo").init("bar");
			assertEquals("bar", args.getVal("foo").stringValue());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testMissing() {
		try {
			assertEquals(null, args.getVal("foo").stringValue());
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00103);
		}
	}
	
	@Test
	public void testMissingDefault() {
		try {
			args.def("foo");
			assertEquals(null, args.getVal("foo").stringValue());
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00105);
		}
	}

	@Test
	public void testListParameter() {
		try {
			args.defList("foo");
			args.put("foo", "value1");
			args.put("foo", "value2");
			assertEquals("value2", args.getVal("foo").minSize(2).stringArray()[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testListParameterException() {
		try {
			args.defList("foo");
			args.getVal("foo").stringValue();
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00101);
		}
	}
	
	@Test
	public void testListParameterException2() {
		try {
			args.def("foo");
			args.getVal("foo").stringArray();
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00102);
		}
	}
	
	@Test
	public void testIntegerParameter() {
		try {
			args.def("foo").init("42");
			assertEquals(42,  args.getVal("foo").intValue());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testIntegerParameter2() {
		try {
			args.def("foo").init("xyzzy");
			assertEquals(42,  args.getVal("foo").intValue());
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00114);
		}
	}
	
	@Test
	public void testBoolean() {
		try {
			args.def("foo").init("true");
			assertEquals(true,  args.getVal("foo").booleanValue());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testBoolean2() {
		try {
			args.def("foo").init("FALSE");
			assertEquals(false,  args.getVal("foo").booleanValue());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testBoolean3() {
		try {
			args.def("foo").init("XyZZy");
			args.getVal("foo").booleanValue();
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00112);
		}
	}
	
	@Test
	public void testDouble1() {
		try {
			args.def("foo").init("4.5");
			args.getVal("foo").doubleValue();
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testDouble2() {
		try {
			args.def("foo").init("hop");
			args.getVal("foo").doubleValue();
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00113);
		}
	}
	
	@Test
	public void testDouble3() {
		try {
			args.defList("foo").aka("F");
			args.put("F", "4.5");
			args.put("F", "0.5");
			assertEquals(0.5, args.getVal("foo").size(5, 10).doubleArray()[1], 10e-10);
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00109);
		}
	}
	
	@Test
	public void testDouble4() {
		try {
			args.defList("foo");
			args.put("foo", "4.5");
			args.put("foo", "0.5");
			assertEquals(0.5, args.getVal("foo").minSize(3).doubleArray()[1], 10e-10);
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00110);
		}
	}

	@Test
	public void testDouble5() {
		try {
			args.defList("foo");
			args.put("foo", "4.5");
			args.put("foo", "0.5");
			assertEquals(0.5, args.getVal("foo").doubleArray()[1], 10e-10);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
			assertMessage(e, U.U00113);
		}
	}
	
	@Test
	public void testParser() {
		try {
			args.def("foo");
			args.def("b a]z");
			args.setSequenceTrackingMode(true);
			args.parse("foo = [b a r] [b a\\]z]=barf]");
			assertEquals("b a r", args.get("foo"));
			assertEquals("barf]", args.get("b a]z"));
			List<String[]> sequence = args.getSequence();
			assertEquals("foo", sequence.get(0)[0]);
			assertEquals("b a]z", sequence.get(1)[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testParser2() {
		try {
			args.defList("foo");
			args.def("b a]z");
			args.setSequenceTrackingMode(true);
			args.parse("foo = [b a r] [b a\\]z]=barf] foo=[2nd value]");
			assertEquals("b a r", args.getVal("foo").stringArray()[0]);
			assertEquals("2nd value", args.getVal("foo").stringArray()[1]);
			assertEquals("barf]", args.getVal("b a]z").stringValue());
			List<String[]> sequence = args.getSequence();
			assertEquals("foo", sequence.get(0)[0]);
			assertEquals("b a r", sequence.get(0)[1]);
			assertEquals("b a]z", sequence.get(1)[0]);
			assertEquals("barf]", sequence.get(1)[1]);
			assertEquals("foo", sequence.get(2)[0]);
			assertEquals("2nd value", sequence.get(2)[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testParser3() {
		try {
			args.def("foo");
			args.def("x");
			args.parse("x=[[y]] foo = bar");
			assertEquals("[y]", args.getVal("x") + "");
			assertEquals("bar", args.getVal("foo") + "");
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testParser4() {
		try {
			args.defList("foo");
			args.def("qu ux");
			args.parse("foo = bar [qu ux]=[\\[what = ever\\]] foo = [2nd val]");
			String[] values = args.getVal("foo").stringArray();
			assertEquals("[what = ever]", args.getVal("qu ux").stringValue());
			assertEquals(2, values.length);
			assertEquals("bar", values[0]);
			assertEquals("2nd val", values[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNameless1() {
		try {
			args.defList("");
			args.def("qu ux");
			args.parse("bar [qu ux]=[[what = ever]] [2nd val]");
			String[] values = args.getVal("").stringArray();
			assertEquals("[what = ever]", args.getVal("qu ux").stringValue());
			assertEquals(2, values.length);
			assertEquals("bar", values[0]);
			assertEquals("2nd val", values[1]);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testNameless2() {
		try {
			args.defList("");
			args.parse("bar baf");
			String[] values = args.getVal("").stringArray();
			assertEquals(2, values.length);
			args.parse("bar baf");
			values = args.getVal("").stringArray();
			assertEquals(4, values.length);
			args.reset();
			args.parse("bar baf");
			values = args.getVal("").stringArray();
			assertEquals(2, values.length);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testEmptyValue() {
		try {
			args.def("foo");
			args.parse("foo = []");
			assertEquals(0, args.getVal("foo").stringValue().length());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testArgsFile() {
		try {
			args.def("foo");
			args.def("bar");
			args.def("multi");
			args.def("name1");
			args.def("name2");
			args.parse("file = " + file1 + " name2 = val2B");
			assertEquals("foo's value", args.get("foo"));
			assertEquals("bar's value", args.get("bar"));
			assertEquals("a b c", args.get("multi"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2B", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testArgsFileWithVariable1() {
		try {
			args.def("name0");
			args.def("name1");
			args.def("name2");
			args.parse("file = ArgsTest.fileE");
			assertEquals("val0", args.get("name0"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testArgsFileWithVariable2() {
		try {
			args.def("name0");
			args.def("name1");
			args.def("name2");
			args.parse("$VAR-SET-IN-LEVEL1=val2 file = ArgsTest.fileD");
			assertEquals("val0", args.get("name0"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testArgsFileWithVariable3() {
		try {
			args.def("name0");
			args.def("name1");
			args.def("name2");
			args.parse("$VAR-SET-IN-LEVEL1=val2 $FILE=ArgsTest.fileF file = ArgsTest.fileC");
			assertEquals("val0", args.get("name0"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testArgsFileWithVariable4() {
		try {
			args.def("name0");
			args.def("name1");
			args.def("name2");
			args.parse("$VAR-SET-IN-LEVEL1=val2 $FILE=ArgsTest.fileD file = ${FILE}");
			assertEquals("val0", args.get("name0"));
			assertEquals("val1", args.get("name1"));
			assertEquals("val2", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testArgsFileWithMapping() {
		try {
			args.def("bar");
			args.def("x");
			args.def("y");
			args.parse("file = [" + file2 + "; name1 = x name2=y]");
			assertEquals("val1", args.get("x"));
			assertEquals("val2", args.get("y"));
			// bar not mapped
			assertEquals("bar's value", args.getVal("bar") + "");
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00105);
		}
	}
	
	@Test
	public void testDuplicateFiles() {
		try {
			args.def("bar");
			args.def("name1");
			args.def("name2");
			args.parse("file = " + file2 + " file = " + file3);
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e.getCause(), U.U00209);
		}
	}

	@Test
	public void testAlias1() {
		try {
			args.def("bar").aka("b");
			args.def("name1");
			args.def("name2");
			args.parse("b= [this is bar's value]");
			assertEquals("this is bar's value", args.getVal("b").stringValue());
			assertEquals("this is bar's value", args.getVal("bar").stringValue());
			assertTrue(args.getVal("b").stringValue() == args.getVal("bar").stringValue());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testAlias2() {
		try {
			args.def(null).aka("b");
			fail("expected an exception");
		} catch (Exception e) {
			assertEquals("name null", e.getMessage());
		}
	}
	
	@Test
	public void testAlias3() {
		try {
			args.def("bar");
			args.def("b").aka("bar");
			fail("expected an exception");
		} catch (Exception e) {
			assertMessage(e, U.U00104);
		}
	}
	
	@Test
	public void testAlias4() {
		try {
			args.def("bar").init("this is bar's default value").aka("b");
			assertEquals("this is bar's default value", args.getVal("b").stringValue());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testAlias5() {
		try {
			args.defList("").aka("pos");
			args.parse("foo bar baf");
			assertEquals(3, args.getVal("pos").stringArray().length);
			args.reset();
			args.parse("pos=foo pos = bar baf");
			assertEquals(3, args.getVal("").stringArray().length);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testPositional1() {
		try {
			args.defList("");
			args.def("name1").init("value");
			args.def("name2");
			args.parse("name2=x foo bar baf");
			assertEquals(3, args.getVal("").stringArray().length);
			assertEquals("value", args.get("name1"));
			assertEquals("x", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testPositional2() {
		try {
			args.def("").init("");
			args.def("name1").init("value");
			args.def("name2");
			args.parse("name2=x foo");
			assertEquals("value", args.get("name1"));
			assertEquals("x", args.get("name2"));
			assertEquals("foo", args.get(""));
			args.reset();
			args.parse("name2=x");
			assertEquals("", args.get(""));
			assertEquals("x", args.get("name2"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testVars1() {
		try {
			args.def("foo");
			args.parse("$a=b $c=${a} foo=${c}");
			assertEquals("b", args.get("foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars2() {
		try {
			args.def("foo");
			args.parse("$a=525 $c=${a} foo=${c}");
			assertEquals(525, args.getVal("foo").intValue());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars3() {
		try {
			args.def("foo");
			args.parse("$a=true $c=${a} foo=${c}");
			assertEquals(Boolean.TRUE, args.getVal("foo").booleanValue());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars4() {
		try {
			args.defList("foo");
			args.parse("$a=1 $b=2 foo=${a} foo=${b}");
			int[] values = args.getVal("foo").intArray();
			assertEquals(1, values[0]);
			assertEquals(2, values[1]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars5() {
		try {
			args.def("foo");
			args.parse("$a=b $c=${a} foo=[ ${c} ]");
			assertEquals(" b ", args.get("foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars6() {
		try {
			args.def("foo");
			args.parse("$a=b $c=[ ${a} ] foo=[ x${c}x ]");
			assertEquals(" x b x ", args.get("foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars7() {
		try {
			args.def("foo");
			args.parse("$a=b $c=\\${a} foo=${c}");
			assertEquals("${a}", args.get("foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	@Test
	public void testVars8() {
		try {
			args.def("foo");
			args.parse("$a=b $c=${a} foo=\\${c}");
			assertEquals("${c}", args.get("foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVars9() {
		try {
			args.def("$foo");
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00121"));
		}
	}
	@Test
	public void testVars10() {
		try {
			args.def("foo");
			args.put("foo", "${bar}");
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00122"));
		}
	}
	@Test
	public void testVars11() {
		try {
			// test "the first wins"
			args.def("foo");
			args.parse("$a=b $a=B $c=${a} foo=${c}");
			assertEquals("b", args.get("foo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	public enum Good {
		good1, good2
	}
	public enum Bad {
		good1
	}

	@Test
	public void testEnum1() {
		try {
			args.def("foo");
			args.put("foo", "good2");
			assertEquals(Good.good2, args.getVal("foo").enumValue(Good.good1));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testEnum2() {
		try {
			args.def("foo");
			args.put("foo", "good3");
			assertEquals(Good.good2, args.getVal("foo").enumValue(Good.good1));
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00115"));
		}
	}
	@Test
	public void testEnum3() {
		try {
			args.defList("foo");
			args.put("foo", "good1");
			args.put("foo", "good2");
			Enum<?>[] res = args.getVal("foo").enumArray(Good.good1);
			assertEquals(Good.good1, res[0]);
			assertEquals(Good.good2, res[1]);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().startsWith("U00115"));
		}
	}
	@Test
	public void testEnum4() {
		try {
			args.defList("foo");
			args.put("foo", "good2");
			args.put("foo", "good3");
			Enum<?>[] res = args.getVal("foo").enumArray(Good.good1);
			assertEquals(Good.good1, res[0]);
			assertEquals(Good.good2, res[1]);
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00115"));
		}
	}
	@Test
	public void testEnum5() {
		try {
			args.def("foo");
			args.put("foo", "good2");
			assertEquals(Good.good2, args.getVal("foo").enumValue(Bad.good1));
			fail("exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("U00115"));
		}
	}
	@Test
	public void testEnum6() {
		try {
			args.def("foo");
			args.put("foo", "good1");
			assertNotEquals(Good.good1, args.getVal("foo").enumValue(Bad.good1));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testVariables1() {
		try {
			args.def("foo");
			args.parse("$HOP = hop $YET = another foo = [${HOP} la boum]");
			assertEquals("hop la boum", args.get("foo"));
			Map<String, String>vars = args.getVariables();
			assertEquals("hop", vars.get("HOP"));
			assertEquals("another", vars.get("YET"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testVariables2() {
		try {
			args.putVariable("HOP", "hop");
			boolean result = args.putVariable("HOP", "hophop");
			args.def("foo");
			args.parse("foo = [${HOP} la boum]");
			assertEquals("hop la boum", args.get("foo"));
			assertFalse(result);
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}


}
