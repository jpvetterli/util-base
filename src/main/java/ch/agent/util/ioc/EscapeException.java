package ch.agent.util.ioc;

/**
 * Unchecked exception thrown by a module or a command to request no-fuss system
 * termination. It is expected to be caught by a top-level system component and
 * interpreted like a request to terminate execution. It should not trigger a
 * stack trace as it constitutes unusual but normal behavior. The exception
 * message should be the name of the module or the (qualified) name of the
 * command, to be logged.
 * 
 */
@SuppressWarnings("serial")
public class EscapeException extends RuntimeException {

	public EscapeException(String moduleOrCommandName) {
		super(moduleOrCommandName);
	}

}
