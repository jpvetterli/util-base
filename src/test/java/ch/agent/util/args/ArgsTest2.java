package ch.agent.util.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ArgsTest2 {

	static {
		System.setProperty(Args.ARGS_META, "{}:!");
	}

	@Test
	public void testCustomMetaChars01() {
		try {
			Args args = new Args();
			args.def("a");
			args.parse("a: {x !}y z}");
			assertEquals("x }y z", args.get("a"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
}
