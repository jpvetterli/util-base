package ch.agent.util.file;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VersioningTest {

	private TextFile textFile;
	private String dirName;
	private List<String> text;
	
	@Before
	public void setUp() throws Exception {
		textFile = new TextFile();
		dirName = tmpDir().getAbsolutePath();
		text = new ArrayList<String>();
		text.add("line 1");
		text.add("line 2");
		text.add("line 3");
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
		delete(new File(dirName));
	}
	
	@Test
	public void testVersioning() {
		try {
			Versioning v = new Versioning("%s.%02d", 3);
			String fileName = dirName + "/a.txt";
			textFile.write(fileName, false, text.iterator());
			assertTrue("1st", v.move(fileName));
			textFile.write(fileName, false, text.iterator());
			assertTrue("2nd", v.move(fileName));
			textFile.write(fileName, false, text.iterator());
			assertTrue("3d", v.move(fileName));
			textFile.write(fileName, false, text.iterator());
			assertFalse("4th", v.move(fileName));
			assertTrue("1st", new File(fileName + ".01").exists());
			assertTrue("2nd", new File(fileName + ".02").exists());
			assertTrue("3d", new File(fileName + ".03").exists());
			assertFalse("4th", new File(fileName + ".04").exists());
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}
	
	@Test
	public void testSecurity() {
		try {
			Versioning v = new Versioning("%s.%02d", 3);
			assertTrue(v.move("/nonesuch"));
			assertFalse(v.move("/etc/passwd"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
