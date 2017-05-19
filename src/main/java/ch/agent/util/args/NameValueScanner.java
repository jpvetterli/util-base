package ch.agent.util.args;

import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.List;

import ch.agent.util.STRINGS.U;

/**
 * The name-value scanner supports splitting a string into a list of String
 * arrays of length 2 for name-value pairs and length 1 for isolated values.
 * <p>
 * The scanner understands four special characters which can be configured using
 * the constructor. When using the no-args constructor provides defaults. These
 * characters are (defaults in parentheses):
 * <ul>
 * <li>the name-value separator (=),
 * <li>the opening quote ([),
 * <li>the closing quote (]), and
 * <li>the escape (\).
 * </ul>
 * For brevity the documentation assumes defaults are used.
 * <p>
 * All four characters lose their special nature when preceded by \. White space
 * characters can also be escaped. Name and values are separated by =. If they
 * contain white space they must be written between [ and ]. Everything in
 * brackets is taken verbatim, and it is only necessary to escape unbalanced
 * closing quotes.
 * <p>
 * A few examples:
 * 
 * <pre>
 * <code>
 * "a b c" is scanned as three values "a", "b" and "c",
 * "a\ b\ c" as one value "a b c",
 * "a = [b c]" as name "a" and value "b c",
 * "a \= [b c]" as three values "a", "=" and "b c",
 * "[a b \c]" as value "a b \c",
 * "[a [b] c]" as value "a [b] c",
 * "[a b\] c]" as value "a b] c",
 * "[a \[b c]" as value "a [b c".
 * </code>
 * </pre>
 * 
 * 
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
public class NameValueScanner {

	private enum Token {
		END_OF_INPUT, EQUAL_TOKEN, STRING_TOKEN
	}
	
	/**
	 * The tokenizer takes care of extracting tokens from the input.
	 * <p>
	 * Specification:
	 * <ul>
	 * <li>There are 3 meta characters (meta): [ ] =.
	 * <li>Whitespace characters (ws) such that
	 * {@link Character#isWhitespace} returns true.
	 * <li>There is an escape character (esc): \
	 * <li>\ turns meta, whitespace and escape characters into normal characters
	 * anywhere in the input. In such cases, the escape itself is omitted from
	 * the output. In all other cases it is included in the output. 
	 * <li>\ at the end of the input produces an IllegalArgumentException
	 * (unless escaped).
	 * <li>[ starts a bracket and must be balanced with ] which terminates the
	 * bracket.
	 * <li>A bracket can contain a nested bracket.
	 * <li>An unbalanced ] produces an IllegalArgumentException (unless
	 * escaped).
	 * <li>The token corresponding to a terminated bracket is the string between
	 * [ and ], which are removed. Whitespace and = have no effect inside a
	 * bracket.
	 * <li>A simple string is a sequence of characters starting with no ws and
	 * no metas (unless escaped).
	 * <li>=is returned as a simple string containing only =.
	 * </ul>
	 */
	private static class Tokenizer {

		private enum State {
			BRACKET, END, EQUAL, ESCAPE, INIT, STRING 
		}
		
		private final char opening;
		private final char closing;
		private final char equals;
		private final char esc;

		private String input;
		private int position; // first is 0
		private String tokenString;
		private StringBuffer buffer;
		private State escapedState;
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
			this.opening = open;
			this.closing = close;
			this.equals = equals;
			this.esc = esc;
		}
		
		/**
		 * Return next token or null when there is no more input. Tokens are
		 * strings with surrounding white-space and enclosing brackets removed.
		 * Escapes are also removed, but only inside brackets and when followed
		 * by a closing bracket or another escape.
		 * 
		 * @return the next token or null
		 */
		public Token token() {
			state = State.INIT;
			while(true) {
				switch (process()) {
				case 0:
					break;
				case 1:
					return Token.STRING_TOKEN;
				case 2:
					return Token.EQUAL_TOKEN;
				case -1:
					return Token.END_OF_INPUT;
				default:
					throw new RuntimeException("bug");
				}
			}
		}
		
		public String getTokenString() {
			return tokenString;
		}
		
		private void setTokenString(boolean emptyOk) {
			if (depth != 0)
				throw new IllegalArgumentException(msg(U.U00155, closing, getPosition(), input));
			if (emptyOk || buffer.length() > 0) {
				tokenString = buffer.toString();
				buffer.setLength(0);
			}
		}
		
		/**
		 * Get the current scanner position. First position is 1.
		 * 
		 * @return a positive number
		 */
		public int getPosition() {
			return position + 1;
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
			tokenString = null;
			char ch = advance();
			switch (state) {
			case END:
				break;
			case ESCAPE:
				if (ch == 0) {
					throw new IllegalArgumentException(msg(U.U00153, esc, getPosition(), input));
				} else {
					if (!isMeta(ch) && ch != esc && !Character.isWhitespace(ch))
						buffer.append(esc);
					buffer.append(ch);
					state = escapedState == State.INIT ? State.STRING : escapedState;
				}
				break;
			case INIT:
				if (ch == 0) {
					setTokenString(false);
					state = State.END;
				} else if (ch == esc) {
					escapedState = state;
					state = State.ESCAPE;
				} else if (ch == equals) {
					state = State.EQUAL;
					buffer.append(equals);
					setTokenString(false);
				} else if (ch == opening) {
					state = State.BRACKET;
					depth = 1;
				} else if (ch == closing) {
					throw new IllegalArgumentException(msg(U.U00154, closing, getPosition(), input));
				} else if (Character.isWhitespace(ch)) {
				} else {
					buffer.append(ch);
					state = State.STRING;
				}
				break;
			case STRING:
				if (ch == 0) {
					setTokenString(false);
					state = State.END;
				} else if (ch == esc) {
					escapedState = state;
					state = State.ESCAPE;
				} else if (isMeta(ch)) {
					if (ch == closing) {
						// must do this test here to support ArgsScanner#immediate
						throw new IllegalArgumentException(msg(U.U00154, closing, getPosition(), input));
					}
					setTokenString(false);
					backtrack();
				} else if (Character.isWhitespace(ch))
					setTokenString(false);
				else
					buffer.append(ch);
				break;
			case BRACKET:
				if (ch == 0) {
					setTokenString(false);
					state = State.END;
				} else if (ch == esc) {
					escapedState = state;
					state = State.ESCAPE;
				} else if (ch == opening) {
					depth++;
					buffer.append(ch);
				} else if (ch == closing) {
					if (depth == 0)
						throw new IllegalArgumentException(msg(U.U00154, closing, input, getPosition()));
					--depth;
					if (depth == 0)
						setTokenString(true);
					else
						buffer.append(ch);
				} else {
					if (ch == esc)
						state = State.ESCAPE;
					buffer.append(ch);
				}
				break;
			default:
				throw new RuntimeException("bug: " + state.name());
			}
			return tokenString != null ? (state == State.EQUAL ? 2 : 1) : (ch == 0 ? -1 : 0);
		}

		private boolean isMeta(char ch) {
			return ch == equals || ch == opening || ch == closing;
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

	private enum NameValueState {
		END, INIT, NAME, VALUE
	}

	private Tokenizer tokenizer;
	private String eq;

	/**
	 * Constructor for a scanner with custom meta characters.
	 * 
	 * @param lq left quote
	 * @param rq right quote
	 * @param nvs name-value separator
	 * @param esc escape
	 */
	public NameValueScanner(char lq, char rq, char nvs, char esc) {
		Args.validateMetaCharacters(lq, rq, nvs, esc);
		tokenizer = new Tokenizer(lq, rq, nvs, esc);
		eq = String.valueOf(nvs);
	}
	
	/**
	 * Constructor for a scanner using default meta characters. The defaults are
	 * [, ], = and \.
	 */
	public NameValueScanner() {
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
	public List<String> asValues(String string) {
		List<String[]> values = asValuesAndPairs(string, true);
		List<String> result = new ArrayList<String>(values.size());
		for (String [] v : values) {
			result.add(v[0]);
		}
		return result;
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
	 * <p>
	 * In <code>valuesOnly</code> mode, the name-value separator does not play
	 * any special role and all elements of the result list will be an array of
	 * length 1.
	 * <p>
	 * It is illegal to invoke the method with <code>pairsOnly</code> and
	 * <code>valuesOnly</code> both true.
	 * 
	 * @param string
	 *            a string interpreted as a sequence of isolated values and and
	 *            name-value pairs, with name and value separated with the name-
	 *            value separator
	 * @param valuesOnly
	 *            if true, isolated values are forbidden
	 * @return a list of 1-element arrays representing isolated values and
	 *         2-elements arrays representing where name-value pairs
	 * @throws IllegalArgumentException
	 */
	private List<String[]> asValuesAndPairs(String string, boolean valuesOnly) {
		
		List<String[]> results = new ArrayList<String[]>();
		tokenizer.reset(string);
		NameValueState state = NameValueState.INIT;
		Token token1 = null;
		String token1String = null;
		Token token2 = null;
		while (state != NameValueState.END) {
			switch (state) {
			case INIT:
				token1 = tokenizer.token();
				token1String = tokenizer.getTokenString();
				switch(token1) {
				case END_OF_INPUT:
					state = NameValueState.END;
					break;
				case EQUAL_TOKEN:
					if (!valuesOnly)
						throw new IllegalArgumentException(msg(U.U00156, eq, tokenizer.getPosition(), string));
					else
						state = NameValueState.NAME;
					break;
				case STRING_TOKEN:
					state = NameValueState.NAME;
					break;
				default:
					throw new RuntimeException("bug " + state.name());
				}
				break;
			case NAME:
				token2 = tokenizer.token();
				String token2String = tokenizer.getTokenString();
				switch(token2) {
				case END_OF_INPUT:
					results.add(new String[]{token1String});
					state = NameValueState.END;
					break;
				case EQUAL_TOKEN:
					if (!valuesOnly)
						state = NameValueState.VALUE;
					else {
						results.add(new String[]{token1String});
						token1String = token2String; // token2String is "="
						state = NameValueState.NAME; // (no state transition)
					}
					break;
				case STRING_TOKEN:
					results.add(new String[]{token1String});
					token1String = token2String;
					state = NameValueState.NAME; // (no state transition)
					break;
				default:
					throw new RuntimeException("bug " + state.name());
				}
				break;
			case VALUE:
				token2 = tokenizer.token();
				token2String = tokenizer.getTokenString();
				switch(token2) {
				case END_OF_INPUT:
					throw new IllegalArgumentException(msg(U.U00159, eq, token1String, string));
				case EQUAL_TOKEN:
					throw new IllegalArgumentException(msg(U.U00156, eq, tokenizer.getPosition(), string));
				case STRING_TOKEN:
					results.add(new String[]{token1String, token2String});
					state = NameValueState.INIT;
					break;
				default:
					throw new RuntimeException("bug " + state.name());
				}
				break;
			default:
				throw new RuntimeException("bug: " + state.name());
			}
		}
		return results;
	}

}
