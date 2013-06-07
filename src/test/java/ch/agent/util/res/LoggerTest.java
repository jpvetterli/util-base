package ch.agent.util.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.agent.util.file.TextFile;

public class LoggerTest {

	protected final Log log = LogFactory.getLog(LoggerTest.class);
	protected static File logFile;
	
	@Before
	public void setUp() throws Exception {
		// initialize logger using code because we need a temporary file
		if (logFile != null)
			return;
		logFile = File.createTempFile("log-", ".log");
		FileAppender a = new FileAppender();
		a.setFile(logFile.getAbsolutePath());
		a.setLayout(new PatternLayout("[%p] %m%n"));
		//a.setThreshold(Level.DEBUG);
		a.activateOptions();
		Logger.getRootLogger().addAppender(a);
	}
	
	@After
	public void tearDown() {
		if (logFile != null)
			logFile.delete();
	}

	@Test
	public void testSimpleMessageWithTwoArgsReversed() {
		try {
			/*
			 * Test support for the java.text.MessageFormat syntax.
			 * Note (7.6.2013): SLF4J does not support it. So, use JCL+log4j.
			 */
			log.info(new LazyMessage("#2={1} #1={0}", Double.NaN, "foo"));
			TextFile f = new TextFile();
			List<String> lines = f.read(logFile.getAbsolutePath());
			assertEquals(1, lines.size());
			assertEquals("[INFO] #2=foo #1=NaN", lines.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
}
