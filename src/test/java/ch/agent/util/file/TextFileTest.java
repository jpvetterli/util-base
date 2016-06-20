package ch.agent.util.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.agent.util.STRINGS.U;

public class TextFileTest {

	private TextFile textFile;
	private String fileName;
	private String dirName;
	private String classPathFileName;
	private List<String> text;
	
	@Before
	public void setUp() throws Exception {
		textFile = new TextFile();
		fileName = tmpFile().getAbsolutePath();
		dirName = tmpDir().getAbsolutePath();
		classPathFileName = "TextFileTest.test";
		text = new ArrayList<String>();
		text.add("line 1");
		text.add("line 2");
		text.add("line 3");
	}

	private File tmpFile() throws IOException {
		File f = File.createTempFile("testf-", ".txt");
		return f;
	}
	
	private File tmpDir() throws IOException {
		File tmpdir = new File(System.getProperty("java.io.tmpdir"));
		int i = 0;
		File tempDir = null;
		while (true) {
			i++;
			Double ran = Math.random() * 1000000;
			String dir = "testd-" + ran.intValue();
			tempDir = new File(tmpdir, dir);
			if (tempDir.mkdir())
				break;
			else {
				if (i > 1000) 
					throw new RuntimeException(
						String.format("Can't create directory %s in %s (%d attempts)", 
						dir, tmpdir, i));
			}
		}
		return tempDir;
	}
	
	// verbatim from http://stackoverflow.com/questions/779519/delete-files-recursively-in-java
	private void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}
		
	@After
	public void tearDown() throws Exception {
		new File(fileName).delete();
		delete(new File(dirName));
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
	public void testWriteRead2() {
		try {
			textFile.write(fileName, false, text.iterator());
			List<String> text2 = textFile.read(fileName);
			assertEquals(text.size(), text2.size());
			assertEquals(text.get(0), text2.get(0));
			assertEquals(text.get(1), text2.get(1));
			assertEquals(text.get(2), text2.get(2));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testWriteRead3() {
		try {
			String fileName = File.createTempFile("test", null).getPath();
			textFile.write(fileName, false, "foo");
			List<String> text = textFile.read(fileName);
			assertEquals("foo", text.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	@Test
	public void testWriteRead4() {
		try {
			String fileName = File.createTempFile("test", null).getPath();
			textFile.write(fileName, false, "");
			List<String> text = textFile.read(fileName);
			assertEquals(1, text.size());
			assertEquals("", text.get(0));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testWriteReadAndAppend() {
		try {
			textFile.setDuplicateDetection(false);
			String fileName = File.createTempFile("test", null).getPath();
			textFile.write(fileName, false, (String) null);
			List<String> text = textFile.read(fileName);
			assertEquals(0, text.size());
			textFile.write(fileName, true, new String[]{"line 1", "line 2"});
			text = textFile.read(fileName);
			assertEquals(2, text.size());
			assertEquals("line 1", text.get(0));
			assertEquals("line 2", text.get(1));
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
	
	@Test
	public void testRelativeOutputFile() {
		try {
			textFile.write("a.txt", false, text.iterator());
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00205));
		}
	}
	
	@Test
	public void testRelativeOutputFile2() {
		try {
			textFile.write("b/a.txt", false, text.iterator());
			fail("expected an exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith(U.U00205));
		}
	}
	
	@Test
	public void testOutputFileCreateDirectory() {
		try {
			textFile.write(dirName + "/a.txt", false, text.iterator());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testOutputFileCreateDirectory2() {
		try {
			textFile.write(dirName + "/a/b/c/d/e.txt", false, text.iterator());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

	@Test
	public void testOutputFileInRoot() {
		try {
			textFile.write("/a.txt", false, text.iterator());
			fail("expected an exception");
		} catch (FileNotFoundException e) {
			assertTrue(e.getMessage().equals("/a.txt (Permission denied)"));
		} catch (Exception e) {
			fail("unexpected exception");
		}
	}


}
