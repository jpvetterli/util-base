package ch.agent.util.ioc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class LogBuffer {
	
	public static LogBuffer startLogging() {
		LogBuffer log = new LogBuffer();
		log.capture();
		return log;
	}
	
	public static String stopLogging(LogBuffer log, boolean print) {
		log.reset();
		String logged = log.toString();
		if (print)
			System.err.println(logged);
		return logged;
	}
	
	private final static int OFFSET = 3;
	private PrintStream stderr;
	private ByteArrayOutputStream capture = new ByteArrayOutputStream();
	private String name;

	public LogBuffer() {
		capture = new ByteArrayOutputStream();
		name = extractMethodName(Thread.currentThread(), OFFSET);
	}
	
	private String extractMethodName(Thread thread, int offset) {
		StackTraceElement e = thread.getStackTrace()[offset];
		return e.getClassName() + "#" + e.getMethodName();
	}
	
	public void capture() {
		if (stderr != null)
			throw new IllegalStateException("stderr != null");
		stderr = System.err;
		capture.reset();
		System.setErr(new PrintStream(capture));
		System.err.println("======== " + name + " ========");
	}
	
	public void reset() {
		if (stderr == null)
			throw new IllegalStateException("stderr == null");
		System.setErr(stderr);
		stderr = null;
	}
	
	public void cleanup() {
		try {
			reset();
		} catch (Exception e) {
		}
	}
	
	public String toString() {
		return capture.toString();
	}

}
