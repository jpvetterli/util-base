package ch.agent.util.ioc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class LogBuffer {
	
	private PrintStream stderr;
	private ByteArrayOutputStream capture = new ByteArrayOutputStream();
	
	public LogBuffer() {
		capture = new ByteArrayOutputStream();
	}
	
	public void capture() {
		if (stderr != null)
			throw new IllegalStateException("stderr != null");
		stderr = System.err;
		capture.reset();
		System.setErr(new PrintStream(capture));
	}
	
	public void reset() {
		if (stderr == null)
			throw new IllegalStateException("stderr == null");
		System.setErr(stderr);
		stderr = null;
	}
	
	public String toString() {
		return capture.toString();
	}

}
