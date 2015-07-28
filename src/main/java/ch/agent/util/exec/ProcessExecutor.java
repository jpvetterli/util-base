package ch.agent.util.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * A process executor takes care of the input and output of an operating system
 * process. It supports redirection of input, output and error streams.
 * Redirection can be done by passing streams or using a callback mechanism.
 */
public class ProcessExecutor {

	/**
	 * The visitor interface supports output redirection.
	 */
	public interface Visitor {
		
		/**
		 * Have a look at each byte in the process output.
		 * 
		 * @param b a byte
		 */
		void visit(byte b);
	}
	
	/**
	 * The feed interface supports input redirection.
	 */
	public interface Feed {
		/**
		 * Feed the next byte of input to the process.
		 * Throw {@link NoSuchElementException} when there is no more input.
		 * 
		 * @return a byte
		 * @throws NoSuchElementException
		 */
		byte get() throws NoSuchElementException;
	}
	
	private static class OutputReader extends Thread {
	    InputStream is;
	    OutputStream os;
	    Visitor visitor;

	    private OutputReader(InputStream is, OutputStream os) {
	        this.is = is;
	        this.os = os;
	    }
	    private OutputReader(InputStream is, Visitor visitor) {
	        this.is = is;
	        this.visitor = visitor;
	    }

	    @Override
		public void run() {
			try {
				byte[] bytes = new byte[1];
				if (os != null) {
					while (is.read(bytes) >= 0) {
						os.write(bytes[0]);
					}
				} else {
					while (is.read(bytes) >= 0) {
						visitor.visit(bytes[0]);
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	private static class IntpuWriter extends Thread {
		OutputStream os;
		InputStream is;
		Feed feed;

	    private IntpuWriter(OutputStream os, InputStream is) {
	        this.os = os;
	        this.is = is;
	    }
	    private IntpuWriter(OutputStream os, Feed feed) {
	        this.os = os;
	        this.feed = feed;
	    }
	    
	    @Override
	    public void run() {
	        try {
	        	byte[] bytes = new byte[1];
				if (is != null) {
					while (is.read(bytes) >= 0) {
						os.write(bytes[0]);
					}
				} else {
					try {
						while (true) {
							os.write(feed.get());
						}
					} catch (NoSuchElementException e) {}
				}
				os.close();
        } catch (IOException ioe) {
	            ioe.printStackTrace();
	        }
	    }
    
	}
	
	private InputStream input;
	private Feed inputFeed;
	private OutputStream output;
	private Visitor outputVisitor;
	private OutputStream errorOutput;
	private Visitor errorOutputVisitor;
	private OutputReader outr;
	private OutputReader errr;
	private IntpuWriter inw;
	private List<String> command;

	/**
	 * Constructor.
	 */
	public ProcessExecutor() {
		super();
	}
	
	/**
	 * Redirect the standard input. BRedirect from a non-null stream only if
	 * there is some actual input, else the process will seem to hang. Clear the
	 * redirection feed, if any.
	 * 
	 * @param in
	 *            an input stream or null for no redirection (the default)
	 */
	public void redirectInput(InputStream in) {
		this.input = in;
		this.inputFeed = null;
	}

	/**
	 * Redirect the standard input. Be sure to feed something, else the process
	 * will seem to hang. Clear the redirection input stream, if any.
	 * 
	 * @param f
	 *            a feed or null for no redirection
	 */
	public void redirectInput(Feed f) {
		this.input = null;
		this.inputFeed = f;
	}

	/**
	 * Redirect the standard output. Clear the redirection visitor, if any.
	 * 
	 * @param out
	 *            an output stream or null for no redirection (the default)
	 */
	public void redirectOutput(OutputStream out) {
		this.output = out;
		this.outputVisitor = null;
	}
	
	/**
	 * Redirect the standard output. Clear the redirection output stream, if
	 * any.
	 * 
	 * @param v
	 *            a visitor or null for no redirection (the default)
	 */
	public void redirectOutput(Visitor v) {
		this.output = null;
		this.outputVisitor = v;
	}

	/**
	 * Redirect the standard error. Clear the redirection visitor, if any.
	 * 
	 * @param out
	 *            an output stream or null for no redirection (the default)
	 */
	public void redirectError(OutputStream out) {
		this.errorOutput = out;
		this.errorOutputVisitor = null;
	}
	
	/**
	 * Redirect the standard error. Clear the redirection stream, if any.
	 * 
	 * @param v
	 *            a visitor or null for no redirection (the default)
	 */
	public void redirectError(Visitor v) {
		this.errorOutput = null;
		this.errorOutputVisitor = v;
	}
	
	/**
	 * Execute a process specified with a {@link ProcessBuilder}. Threads are
	 * set up according to the redirection specifications. The method returns
	 * the exit status of the process. Refer to the documentation of
	 * {@link ProcessBuilder} for details on process attributes.
	 * <p>
	 * Execution is done in two phases:
	 * <ol>
	 * <li>star a process
	 * <li>wait for the result
	 * </ol>
	 * The method combines the two phases and wraps any checked exception into
	 * a runtime exception. It is possible to execute the two phases directly
	 * with the methods {@link #start(ProcessBuilder)} and
	 * {@link #waitForResult(Process)}. This is useful when an application needs
	 * to access the underlying {@link Process}, as is the case when
	 * establishing a command pipeline.
	 * 
	 * @param builder
	 *            a process builder with all attributes of the process
	 * @return the exit status of the process
	 * @throws RuntimeException
	 *             wrapping an {@link IOException} from {@link #start} or an
	 *             {@link InterruptedException} from {@link #waitForResult}
	 */
	public int execute(ProcessBuilder builder) {
		try {
			return waitForResult(start(builder));
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute: " + builder.command().toString(), e);
		} 
	}
	
	/**
	 * Start a process. The method starts the process and creates and runs
	 * auxiliary threads for redirecting input and output.
	 * 
	 * @param builder
	 *            a process builder with all process attributes
	 * @return the started process
	 * @throws IOException
	 *             thrown by {@link ProcessBuilder#start()}
	 */
	public Process start(ProcessBuilder builder) throws IOException {
		command = builder.command();
		outr = null;
		errr = null;
		inw = null;
		Process p = builder.start();
		if (output != null) {
			outr = new OutputReader(p.getInputStream(), output);
			outr.start();
		}
		if (outputVisitor != null) {
			outr = new OutputReader(p.getInputStream(), outputVisitor);
			outr.start();
		}
		if (errorOutput != null) {
			errr = new OutputReader(p.getErrorStream(), errorOutput);
			errr.start();
		}
		if (errorOutputVisitor != null) {
			errr = new OutputReader(p.getErrorStream(), errorOutputVisitor);
			errr.start();
		}
		if (input != null) {
			inw = new IntpuWriter(p.getOutputStream(), input);
			inw.start();
		}
		if (inputFeed != null) {
			inw = new IntpuWriter(p.getOutputStream(), inputFeed);
			inw.start();
		}
		return p;
	}
	
	/**
	 * Wait for a started process to terminate and cleanup auxiliary threads.
	 * 
	 * @param p
	 *            a started process
	 * @return the exit code of the process
	 * @throws InterruptedException
	 *             thrown by {@link Process#waitFor()} or by
	 *             {@link Thread#join()}
	 */
	public int waitForResult(Process p) throws InterruptedException {
		if (command == null)
			throw new IllegalStateException("command null");
		int code = p.waitFor();
		if (inw != null)
			inw.join();
		if (outr != null)
			outr.join();
		if (errr != null)
			errr.join();
		return code;
	}
	
}
