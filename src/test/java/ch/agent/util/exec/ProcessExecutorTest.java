package ch.agent.util.exec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

public class ProcessExecutorTest {

	public static class Inverter implements ProcessExecutor.Visitor {
		byte[] bb;
		int position = 0;
		public Inverter() {
			super();
			bb = new byte[100];
		}
		@Override
		public void visit(byte b) {
			if (position == bb.length) {
				int newlen = bb.length < 10000 ? bb.length * 2 : (int) Math.floor(bb.length * 1.5);
				bb = Arrays.copyOf(bb,  newlen);
			}
			bb[position++] = b;
		}
		
		/**
		 * Return the current state of the inverter as a string.
		 * 
		 * @param charset the character set to use or null for the platform default
		 * @return a string
		 */
		public String get(Charset charset) {
			String s = charset == null ? new String(bb, 0, position) : new String(bb, 0, position, charset);
			StringBuffer b = new StringBuffer(s.length());
			for (int i = 0; i < s.length(); i++) {
				b.insert(0, s.charAt(i));
			}
			return b.toString();
		}
	}
	
	public static class Echo implements ProcessExecutor.Feed {
		private byte[] data;
		private int next;
		public Echo(String data, Charset charset) {
			super();
			if (charset == null)
				this.data = data.getBytes();
			else
				this.data = data.getBytes(charset);
		}
		@Override
		public byte get() throws NoSuchElementException {
			if (next >= data.length)
				throw new NoSuchElementException();
			else
				return data[next++];
		}
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGoodCommand1() {
		ProcessExecutor exec = new ProcessExecutor();
		ProcessOutput out = new ProcessOutput(100);
		ProcessOutput err = new ProcessOutput(100);
		exec.redirectOutput(out);
		exec.redirectError(err);
		int code = exec.execute(new ProcessBuilder("echo", "hello"));
		assertEquals(0, code);
		assertEquals(1,  out.toLines().length);
		assertEquals(0,  err.toLines().length);
	}
	
	@Test
	public void testGoodCommand2() {
		ProcessExecutor exec = new ProcessExecutor();
		ProcessOutput out = new ProcessOutput(100);
		ProcessOutput err = new ProcessOutput(100);
		exec.redirectOutput(out);
		exec.redirectError(err);
		int code = exec.execute(new ProcessBuilder("echo", "-n", "hello"));
		assertEquals(0, code);
		assertEquals(1,  out.toLines().length);
		assertEquals(0,  err.toLines().length);
	}
	
	@Test
	public void testGoodCommand3() {
		ProcessExecutor exec = new ProcessExecutor();
		ProcessOutput out = new ProcessOutput(100);
		exec.redirectOutput(out);
		exec.execute(new ProcessBuilder("echo", "hello"));
		assertEquals(6, out.size());
	}
	
	@Test
	public void testGoodCommand4() {
		ProcessExecutor exec = new ProcessExecutor();
		ProcessOutput out = new ProcessOutput(100);
		exec.redirectOutput(out);
		exec.execute(new ProcessBuilder("echo", "-n", "hello"));
		assertEquals(5, out.size());
	}
	
	@Test
	public void testBadCommand1() {
		ProcessExecutor exec = new ProcessExecutor();
		ProcessOutput out = new ProcessOutput(100);
		ProcessOutput err = new ProcessOutput(100);
		exec.redirectOutput(out);
		exec.redirectError(err);
		int code = exec.execute(new ProcessBuilder("cat", "/nonesuch"));
		assertEquals(1, code);
		assertEquals(0,  out.toLines().length);
		assertEquals(1,  err.toLines().length);
	}
	
	@Test
	public void testBadCommand2() {
		try {
			new ProcessExecutor().execute(new ProcessBuilder("catcatcat", "/nonesuch"));
			fail("exception expected");
		} catch (Exception e) {
			assertNotEquals(-1, e.getCause().getMessage().indexOf("Cannot run program"));
		}
	}
	
	@Test
	public void testVisitor() {
		ProcessExecutor exec = new ProcessExecutor();
		Inverter inv = new Inverter();
		exec.redirectOutput(inv);
		exec.execute(new ProcessBuilder("echo", "-n", "hello"));
		assertEquals("olleh", inv.get(null));
	}
	
	@Test
	public void testVisitorUnicode() {
		ProcessExecutor exec = new ProcessExecutor();
		Inverter inv = new Inverter();
		exec.redirectOutput(inv);
		exec.execute(new ProcessBuilder("echo", "-n", "\u21d2hello"));
		assertEquals("olleh\u21d2", inv.get(Charset.forName("UTF8")));
	}
	
	@Test
	public void testFeed() {
		ProcessOutput out = new ProcessOutput(100);
		ProcessExecutor exec = new ProcessExecutor();
		exec.redirectInput(new Echo("hello", null));
		exec.redirectOutput(out);
		exec.execute(new ProcessBuilder("cat"));
		assertEquals("hello", out.toString());
	}
	
	@Test
	public void testFeedUnicode() {
		String hello = "\u21d2 hello";
		ProcessOutput out = new ProcessOutput(100);
		ProcessExecutor exec = new ProcessExecutor();
		exec.redirectInput(new Echo(hello, null));
		exec.redirectOutput(out);
		exec.execute(new ProcessBuilder("cat"));
		assertEquals(hello, out.toString());
	}
	
	@Test
	public void testFeedUnicode2() {
		String hello = "\u21d2 hello";
		try {
			File f = File.createTempFile("foo", "bar");
			f.deleteOnExit();
			PrintStream ps = new PrintStream(f);
			ps.print(hello);
			ps.close();
		ProcessOutput out = new ProcessOutput(100);
		ProcessExecutor exec = new ProcessExecutor();
		exec.redirectOutput(out);
		exec.execute(new ProcessBuilder("cat", f.getAbsolutePath()));
		assertEquals(hello, out.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPipeline() {
		try {
			ProcessExecutor exec1 = new ProcessExecutor();
			ProcessExecutor exec2 = new ProcessExecutor();
			Process p1 = exec1.start(new ProcessBuilder("echo", "-n", "hello"));
			ProcessOutput out = new ProcessOutput(100);
			exec2.redirectInput(p1.getInputStream());
			exec2.redirectOutput(out);
			exec1.waitForResult(p1);
			exec2.execute(new ProcessBuilder("wc", "-c"));
			assertEquals("5\n", out.toString());
		} catch (Exception e) {
			fail("exception not expected");
		}
	}
	
}
