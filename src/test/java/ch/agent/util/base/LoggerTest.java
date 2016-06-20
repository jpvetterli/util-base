package ch.agent.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import ch.agent.util.base.LazyString;
import ch.agent.util.file.TextFile;

public class LoggerTest {

	private static int toStringCount;
	
	private class TestMessage extends LazyString {
		public TestMessage(String message, Object... args) {
			super(message, null, null, null, args);
		}
		@Override
		public String toString() {
			toStringCount++;
			return super.toString();
		}
	}
	
	/**
	 * This tests support for the java.text.MessageFormat syntax. This format is
	 * very useful in multilingual contexts, where an important requirement is
	 * to modify the order of parameters when translating message texts.
	 * <p>
	 * SLF4J does not support this while JCL does.
	 * <p>
	 * The test in this project assumes JCL with log4j. There is a variation of
	 * this test in project util-web which uses the combination JCL-over-SLF4J
	 * with logback. Testing different combinations in different projects makes
	 * life easier because the mere presence of some jars affect how loggers
	 * behave.
	 * <p>
	 * The 3d assertion tests that parameter are handled lazily by counting
	 * calls of {@link TestMessage#toString}.
	 */
	@Test
	public void testSimpleMessageWithTwoArgsReversed() {
		File logFile = null;
		try {
			logFile = File.createTempFile("log-", ".latog");
			Log log = setUpLog4J(logFile);
			log.info(new TestMessage("#2={1} #1={0}", Double.NaN, "foo"));
			log.debug(new TestMessage("#2={1} #1={0}", Double.NaN, "foo"));
			TextFile f = new TextFile();
			List<String> lines = f.read(logFile.getAbsolutePath());
			assertEquals(1, lines.size());
			assertEquals("[INFO] #2=foo #1=\uFFFD", lines.get(0));
			assertEquals(1, toStringCount); // see note above 
		} catch (Exception e) {
			fail("unexpected exception");
		} finally {
			logFile.delete();
		}
	}
	
	private Log setUpLog4J(File f) throws Exception {
		FileAppender a = new FileAppender();
		a.setFile(f.getAbsolutePath());
		a.setLayout(new PatternLayout("[%p] %m%n"));
		a.setThreshold(Level.INFO);
		a.activateOptions();
		Logger.getRootLogger().addAppender(a);
		return LogFactory.getLog(LoggerTest.class);
	}

}
