package ch.agent.util.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.agent.util.UtilMsg.U;

public class TextFileTest {

	private TextFile textFile;
	private String fileName;
	private String classPathFileName;
	private List<String> text;
	
	@Before
	public void setUp() throws Exception {
		textFile = new TextFile();
		fileName = File.createTempFile("test", ".txt").getAbsolutePath();
		classPathFileName = "TextFileTest.test";
		text = new ArrayList<String>();
		text.add("line 1");
		text.add("line 2");
		text.add("line 3");
	}

	@After
	public void tearDown() throws Exception {
		new File(fileName).delete();
	}
	
	@Test
	public void testWrite() {
		try {
			textFile.write(fileName, false, text.iterator());
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testWriteRead() {
		try {
			textFile.write(fileName, false, text.iterator());
			final List<String> text2 = new ArrayList<String>();
			textFile.read(fileName, new TextFile.Visitor() {
				@Override
				public boolean visit(int lineNr, String line) throws Exception {
					text2.add(line);
					return false;
				}
			});
			assertEquals(text.size(), text2.size());
			assertEquals(text.get(0), text2.get(0));
			assertEquals(text.get(1), text2.get(1));
			assertEquals(text.get(2), text2.get(2));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testFileNotFound() {
		try {
			textFile.read("/foo/bar", new TextFile.Visitor() {
				public boolean visit(int lineNr, String line) throws Exception {
					return true;
				}
			});
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00208));
		}
	}
	
	@Test
	public void testDuplicateFiles() {
		try {
			textFile.write(fileName, false, text.iterator());
			textFile.read(fileName, new TextFile.Visitor() {
				public boolean visit(int lineNr, String line) throws Exception {
					return true;
				}
			});
			textFile.read(fileName, new TextFile.Visitor() {
				public boolean visit(int lineNr, String line) throws Exception {
					return true;
				}
			});
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00209));
		}
	}
	
	@Test
	public void testClassPathFile() {
		try {
			final List<String> text2 = new ArrayList<String>();
			textFile.read(classPathFileName, new TextFile.Visitor() {
				@Override
				public boolean visit(int lineNr, String line) throws Exception {
					text2.add(line);
					return false;
				}
			});
			assertEquals(4, text2.size());
			assertEquals("this is line 4", text2.get(3));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	

}
