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
	private PrintStream capturedErr;
	private PrintStream capturedOut;
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
		if (capturedErr != null || capturedOut != null)
			throw new IllegalStateException("err or out already captured");
		capturedErr = System.err;
		capturedOut = System.out;
		capture.reset();
		System.setErr(new PrintStream(capture));
		System.setOut(new PrintStream(capture));
		System.err.println("======== " + name + " ========");
	}
	
	public void reset() {
		if (capturedErr == null || capturedOut == null)
			throw new IllegalStateException("err or out not captured");
		System.setErr(capturedErr);
		System.setOut(capturedOut);
		capturedErr = null;
		capturedOut = null;
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
