package ch.agent.util.args;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.agent.util.UtilMsg;
import ch.agent.util.UtilMsg.U;

/**
 * ArgsScanner provides scanning support to {@link Args}. The scanner turns a
 * string into a list of tokens, which are actually just strings. It is used to
 * parse lists of name-value pairs with support for embedded white space. To
 * include white space in a string, the string can be put in brackets.
 * Everything inside brackets is taken verbatim. Empty brackets are used to
 * specify an empty string.To include a closing bracket it must be preceded by
 * the escape character. Brackets can be included inside a normal string, and
 * the name-value separator can be included inside a string in brackets.
 * <p>
 * The special characters and their default values are:
 * <ul>
 * <li>opening bracket [
 * <li>closing bracket ]
 * <li>name-value separator =
 * <li>escape character \
 * </ul>
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class ArgsScanner {

	private enum State {
		INIT, STRING, BRACKET, ESCAPE
	}

	private String input;
	private int currentChar;

	private List<String> tokens;
	private StringBuffer buffer;
	private State state;
	private char opening;
	private char closing;
	private char equals;
	private char esc;

	/**
	 * Constructor for a custom scanner.
	 * 
	 * @param opening the opening bracket
	 * @param closing the closing bracket
	 * @param equals the name-value separator
	 * @param esc the escape character
	 */
	public ArgsScanner(char opening, char closing, char equals, char esc) {
		this.opening = opening;
		this.closing = closing;
		this.equals = equals;
		this.esc = esc;
	}

	/**
	 * Constructor for a scanner using defaults.
	 */
	public ArgsScanner() {
		this('[', ']', '=', '\\');
	}

	/**
	 * Extract list of tokens from a string. Tokens are string with all
	 * white-space, brackets, and escapes removed.
	 * 
	 * @param string
	 *            a string
	 * @return a list of strings
	 */
	public List<String> tokenize(String string) {
		if (string == null)
			throw new IllegalArgumentException("string null");
		input = string;
		currentChar = -1;
		tokens = new ArrayList<String>();
		buffer = new StringBuffer();
		state = State.INIT;
		char ch = 0;
		do {
			ch = next();
		} while (process(ch));
		return tokens;
	}
	
	/**
	 * Turn a string into a list of name-value pairs.
	 * 
	 * @param string
	 *            a string interpreted as a sequence of names, equals, and
	 *            values
	 * @return a list of 2-elements array representing name-value pairs
	 */
	public List<String[]> asPairs(String string) {
		tokenize(string);
		List<String[]> pairs = new ArrayList<String[]>();
		String eq = String.valueOf(equals);
		Iterator<String> it = tokens.iterator();
		while(it.hasNext()) {
			String[] nv = new String[2];
			nv[0] = it.next();
			if (it.hasNext() && it.next().equals(eq)) {
				if (it.hasNext())
					nv[1] = it.next();
			}
			if (nv[1] == null) {
				if (pairs.size() > 0) {
					String[] pair = pairs.get(pairs.size() - 1);
					throw new UtilMsg(U.U00107, nv[0], pair[0], pair[1]).runtimeException();
				} else
					throw new UtilMsg(U.U00106, nv[0]).runtimeException();
			}
			pairs.add(nv);
		}
		return pairs;
	}

	/**
	 * Process the char and return true to indicate to continue or false to
	 * indicate to stop.
	 * 
	 * @param ch
	 *            a char
	 * @return true to continue or false to stop
	 */
	private boolean process(char ch) {
		if (ch == 0)
			addToken(false);
		else {
			switch (state) {
			case INIT:
				if (ch == equals) {
					buffer.append(ch);
					addToken(false);
				} else if (ch == opening) {
					state = State.BRACKET;
				} else if (Character.isWhitespace(ch)) {
				} else {
					buffer.append(ch);
					state = State.STRING;
				}
				break;
			case STRING:
				if (ch == equals) {
					addToken(false);
					buffer.append(ch);
					addToken(false);
				} else if (Character.isWhitespace(ch)) {
					addToken(false);
				} else {
					// embedded brackets play no special role here
					buffer.append(ch);
				}
				break;
			case ESCAPE:
				if (ch == closing) {
					// replace the escape with the closing bracket
					buffer.setCharAt(buffer.length() - 1, closing);
					state = State.BRACKET;
				} else {
					if (ch != esc)
						state = State.BRACKET;
					buffer.append(ch);
				}
				break;
			case BRACKET:
				if (ch == closing)
					addToken(true);
				else {
					if (ch == esc)
						state = State.ESCAPE;
					buffer.append(ch);
				}
				break;
			default:
				throw new RuntimeException("bug");
			}
		}
		return ch != 0;
	}

	private void addToken(boolean emptyOk) {
		if (emptyOk || buffer.length() > 0) {
			tokens.add(buffer.toString());
			buffer.setLength(0);
		}
		state = State.INIT;
	}

	/**
	 * Returns the next char from the input or 0 if there are no more chars.
	 * 
	 * @return the next char or 0
	 */
	private char next() {
		try {
			return input.charAt(++currentChar);
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}

}
