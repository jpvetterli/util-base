package ch.agent.util.args;

import java.util.ArrayList;
import java.util.List;

import ch.agent.util.UtilMsg;
import ch.agent.util.UtilMsg.U;

/**
 * Scanning support for {@link Args}. It is used to parse lists of name-value
 * pairs or isolated values. It supports embedded white space. To include white
 * space in a string, the string must be put inside brackets. Everything inside
 * brackets is taken verbatim. Empty brackets are used to specify an empty
 * string. Brackets can be nested: to terminate a string with two or more open
 * brackets two or more closing brackets are needed. This nesting effect can be
 * disabled by prefixing embedded brackets with an escape character. The escape
 * character has only an effect inside brackets and only before embedded
 * brackets or another escape: a double escape is replaced with a single escape.
 * This allows to write a string like "{}=!" as "{{!}=!!}" (where {} open and
 * close the bracket and ! escapes). The opening and closing bracket characters
 * can be included inside a normal string, and the name-value separator can be
 * included inside a string in brackets. An isolated name-value separator in
 * brackets is still a name-value separator.
 * <p>
 * The meta characters and their default values are:
 * <ul>
 * <li>opening bracket [
 * <li>closing bracket ]
 * <li>name-value separator =
 * <li>escape character \
 * </ul>
 * 
 * It is possible to redefine meta characters on the fly in the input using the
 * reserved name Tokenizer.MetaCharacters. The value must have length 4 and is
 * interpreted as the sequence opening bracket, closing bracket, name-value
 * separator, and escape character. These characters must be distinct.
 * 
 * @author Jean-Paul Vetterli
 * 
 */
public class ArgsScanner {

	/**
	 * The tokenizer takes care of extracting tokens from the input.
	 */
	private static class Tokenizer {

		private enum State {
			INIT, STRING, BRACKET, ESCAPE, END
		}

		private char opening;
		private char closing;
		private char equals;
		private char esc;

		private String input;
		private int position;
		private String token;
		private StringBuffer buffer;
		private State state;
		private int depth; // nested BRACKET

		/**
		 * Constructor taking default meta characters.
		 * 
		 * @param open
		 *            the opening bracket
		 * @param close
		 *            the closing bracket
		 * @param equals
		 *            the name-value separator
		 * @param esc
		 *            the escape character
		 */
		public Tokenizer(char open, char close, char equals, char esc) {
			setMetaCharacters(open, close, equals, esc);
		}
	
		/**
		 * Set meta characters. Characters must be distinct except open and
		 * close.
		 * 
		 * @param ocee
		 *            a string of length 4 with the open, close, equals and
		 *            escape meta characters
		 */
		public void setMetaCharacters(char open, char close, char equals, char esc) {
			checkMetaCharacters(open, close, equals, esc);
			this.opening = open;
			this.closing = close;
			this.equals = equals;
			this.esc = esc;
		}
		
		private void checkMetaCharacters(char open, char close, char equals, char esc) {
			if (open == equals || open == esc || open == close || close == equals || close == esc || equals == esc)
				throw new IllegalArgumentException(UtilMsg.msg(U.U00163, open, close, equals, esc));
		}
		
		/**
		 * Return next token or null when there is no more input. Tokens are
		 * strings with surrounding white-space and enclosing brackets removed.
		 * Escapes are also removed, but only inside brackets and when followed
		 * by a closing bracket or another escape.
		 * 
		 * @return the next token or null
		 */
		public String token() {
			while(true) {
				switch (process()) {
				case 0:
					break;
				case 1:
					return token;
				case -1:
					return null;
				default:
					throw new RuntimeException("bug");
				}
			}
		}

		/**
		 * Reset the input.
		 * 
		 * @param input
		 *            a non-null string
		 */
		public void reset(String input) {
			if (input == null)
				throw new IllegalArgumentException("input null");
			this.input = input;
			position = -1;
			buffer = new StringBuffer();
			state = State.INIT;
		}

		/**
		 * Process the next char. Return 0 to indicate to continue,
		 * 1 to indicate that a token is available, and -1 to indicate
		 * the end of the input.
		 * 
		 * @return true to continue or false to stop
		 */
		private int process() {
			token = null;
			char ch = advance();
			switch (state) {
			case END:
				break;
			case INIT:
				if (ch == 0) {
					addToken(false);
					state = State.END;
				} else if (ch == equals) {
					buffer.append(equals);
					addToken(false);
				} else if (ch == opening) {
					state = State.BRACKET;
					depth = 1;
				} else if (Character.isWhitespace(ch)) {
				} else {
					buffer.append(ch);
					state = State.STRING;
				}
				break;
			case STRING:
				if (ch == 0) {
					addToken(false);
					state = State.END;
				} else if (ch == equals) {
					addToken(false);
					backtrack();
				} else if (Character.isWhitespace(ch)) {
					addToken(false);
				} else {
					// embedded brackets play no special role here
					buffer.append(ch);
				}
				break;
			case ESCAPE:
				if (ch == 0) {
					addToken(false);
					state = State.END;
				} else if (ch == closing || ch == opening) {
					// replace the escape with the bracket
					buffer.setCharAt(buffer.length() - 1, ch);
					state = State.BRACKET;
				} else {
					// replace two escapes with a single one
					if (ch != esc)
						buffer.append(ch);
					state = State.BRACKET;
				}
				break;
			case BRACKET:
				if (ch == 0) {
					addToken(false);
					state = State.END;
				} else if (ch == opening) {
					depth++;
					buffer.append(ch);
				} else if (ch == closing) {
					--depth;
					if (depth == 0)
						addToken(true);
					else
						buffer.append(ch);
				} else {
					if (ch == esc)
						state = State.ESCAPE;
					buffer.append(ch);
				}
				break;
			default:
				throw new RuntimeException("bug");
			}
			return token != null ? 1 : (ch == 0 ? -1 : 0);
		}

		private void addToken(boolean emptyOk) {
			if (emptyOk || buffer.length() > 0) {
				token = buffer.toString();
				buffer.setLength(0);
			}
			state = State.INIT;
		}

		/**
		 * Return the next char from the input or 0 if there is no more input.
		 * 
		 * @return the next char or 0
		 */
		private char advance() {
			try {
				return input.charAt(++position);
			} catch (IndexOutOfBoundsException e) {
				return 0;
			}
		}
		
		/**
		 * Change position so that {@link #advance()} returns the current char
		 * again.
		 */
		private void backtrack() {
			position--;
		}
	}
	
	/**
	 * Name of the special parameter used to modify meta characters. Takes a value
	 * of length four with the open, close, equals, and escape meta characters.
	 */
	public static final String METACHAR = "Tokenizer.MetaCharacters";
	
	private enum NameValueState {
		INIT, END, NAME, VALUE
	}

	private Tokenizer tokenizer;
	private String eq;

	/**
	 * Constructor for a scanner with custom meta characters.
	 * 
	 * @param opening the opening bracket
	 * @param closing the closing bracket
	 * @param equals the name-value separator
	 * @param esc the escape character
	 */
	public ArgsScanner(char opening, char closing, char equals, char esc) {
		tokenizer = new Tokenizer(opening, closing, equals, esc);
		eq = String.valueOf(equals);
	}

	/**
	 * Constructor for a scanner using default meta characters.
	 */
	public ArgsScanner() {
		this('[', ']', '=', '\\');
	}

	
	/**
	 * Turn a string into a list of name-value pairs. An
	 * <code>IllegalArgumentException</code> is thrown when parsing becomes
	 * impossible because of a badly formed input.
	 * 
	 * @param string
	 *            a string interpreted as a sequence of names, equals, and
	 *            values
	 * @return a list of 2-elements array representing name-value pairs
	 * @throws IllegalArgumentException
	 */
	public List<String[]> asPairs(String string) {
		return asValuesAndPairs(string, true);
	}
	
	/**
	 * Turn a string into a list of isolated values and name-value pairs. An
	 * <code>IllegalArgumentException</code> is thrown when parsing becomes
	 * impossible because of a badly formed input.
	 * 
	 * @param string
	 *            a string interpreted as a sequence of isolated values and and
	 *            name-value pairs, with name and value separated with an equal
	 *            sign
	 * @return a list of 1-element arrays representing isolated values and
	 *         2-elements arrays representing where name-value pairs
	 * @throws IllegalArgumentException
	 */
	public List<String[]> asValuesAndPairs(String string) {
		return asValuesAndPairs(string, false);
	}
	
	/**
	 * Turn a string into a list of isolated values and name-value pairs.
	 * <code>IllegalArgumentException</code> is thrown when parsing becomes
	 * impossible because of a badly formed input.
	 * 
	 * @param string
	 *            a string interpreted as a sequence of isolated values and and
	 *            name-value pairs, with name and value separated with an equal
	 *            sign
	 * @param strict if true, isolated values are forbidden
	 * @return a list of 1-element arrays representing isolated values and
	 *         2-elements arrays representing where name-value pairs
	 * @throws IllegalArgumentException
	 */
	public List<String[]> asValuesAndPairs(String string, boolean strict) {
		List<String[]> results = new ArrayList<String[]>();
		tokenizer.reset(string);
		NameValueState state = NameValueState.INIT;
		String token1 = null;
		String token2 = null;
		while (state != NameValueState.END) {
			switch (state) {
			case INIT:
				token1 = tokenizer.token();
				if (token1 == null) {
					state = NameValueState.END;
				} else {
					if (token1.equals(eq)) {
						if (results.size() == 0)
							throw new IllegalArgumentException(UtilMsg.msg(U.U00158, eq, string));
						else
							throw new IllegalArgumentException(UtilMsg.msg(U.U00156, eq, lastToken(results), string));
					} else
						state = NameValueState.NAME;
				}
				break;
			case NAME:
				token2 = tokenizer.token();
				if (token2 == null) {
					if (strict)
						throw new IllegalArgumentException(UtilMsg.msg(U.U00157, eq, token1, string));
					results.add(new String[]{token1});
					state = NameValueState.END;
				} else {
					if (token2.equals(eq))
						state = NameValueState.VALUE;
					else {
						if (strict)
							throw new IllegalArgumentException(UtilMsg.msg(U.U00157, eq, token1, string));
						results.add(new String[]{token1});
						token1 = token2;
						state = NameValueState.NAME; // (no state transition)
					}
				}
				break;
			case VALUE:
				token2 = tokenizer.token();
				if (token2 == null) {
					throw new IllegalArgumentException(UtilMsg.msg(U.U00159, eq, token1, string));
				} else {
					if (token1.equals(METACHAR))
						setMetaCharacters(token2);
					else 
						results.add(new String[]{token1, token2});
					state = NameValueState.INIT;
				}
				break;
			default:
				throw new RuntimeException("bug: " + state.name());
			}
		}
		return results;
	}

	private void setMetaCharacters(String spec) {
		if (spec.length() != 4)
			throw new IllegalArgumentException(UtilMsg.msg(U.U00164, spec));
		tokenizer.setMetaCharacters(spec.charAt(0), spec.charAt(1), 
				spec.charAt(2), spec.charAt(3));
		eq = String.valueOf(spec.charAt(2));
	}
	
	private String lastToken(List<String[]> pairs) {
		String lastToken = null;
		if (pairs.size() > 0) {
			String[] pair = pairs.get(pairs.size() - 1);
			switch (pair.length) {
			case 1:
				lastToken = pair[0];
				break;
			case 2:
				lastToken = pair[1];
				break;
			default:
				throw new RuntimeException("bug: " + pair.length);
			}
		}
		return lastToken;
	}	
}
