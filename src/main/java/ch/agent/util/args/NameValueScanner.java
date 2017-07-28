package ch.agent.util.args;

import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.List;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;

/**
 * The name-value scanner splits a string into a list of arrays of length 2 for
 * name-value pairs and length 1 for isolated values.
 * <p>
 * The scanner understands four special characters which can be configured using
 * the constructor. A no-args constructor provides defaults. These
 * characters are (defaults in parentheses):
 * <ul>
 * <li>the name-value separator (=),
 * <li>the opening quote ([),
 * <li>the closing quote (]), and
 * <li>the escape (\).
 * </ul>
 * For brevity, the documentation assumes defaults are used. White space, as
 * defined by {@link Character#isWhitespace(char)}, is either significant or
 * ignored, depending on where it is used.
 * <p>
 * The four special characters and white space characters lose their special
 * nature when preceded by an escape. A name is separated from a value by the =
 * sign. White space can be freely used around the = sign. Name-value pairs and
 * isolated values are separated by at least one white space character. Names
 * and values which contain white space must be written between [ and ].
 * Everything in brackets is taken verbatim, and it is only necessary to escape
 * unbalanced brackets.
 * <p>
 * A few examples are more useful than many words:
 * 
 * <pre>
 * <code>
 * "a b c" is scanned as three values "a", "b" and "c",
 * "a\ b\ c"          as one value "a b c",
 * "a=b"              as name "a" and value "b",
 * " a = b "          as name "a" and value "b",
 * "a = [b c]"        as name "a" and value "b c",
 * "a \= [b c]"       as three values "a", "=" and "b c",
 * "[a b \c]"         as value "a b \c",
 * "[a [b] c]"        as value "a [b] c",
 * "[a b\] c]"        as value "a b] c",
 * "[a \[b c]"        as value "a [b c".
 * </code>
 * </pre>
 */
public class NameValueScanner {

	private enum Token {
		END_OF_INPUT, EQUAL_TOKEN, STRING_TOKEN
	}

	/**
	 * The tokenizer takes care of finding strings and equal signs in the input.
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
		private StringBuilder buffer;
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
		 * Return the next token. There are 3 possible tokens:
		 * <ul>
		 * <li>END_OF_INPUT
		 * <li>EQUAL_TOKEN
		 * <li>STRING_TOKEN
		 * </ul>
		 * 
		 * @return the next token
		 */
		public Token nextToken() {
			state = State.INIT;
			while (true) {
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

		/**
		 * Return the string associated to the token STRING_TOKEN.
		 * 
		 * @return a string
		 */
		public String getTokenString() {
			return tokenString;
		}

		private void setTokenString(boolean emptyOk) {
			if (depth != 0)
				throw new IllegalArgumentException(msg(U.U00155, closing, getPosition(), 
						Misc.mark(input, getPosition() - 1, 100)));
			if (emptyOk || buffer.length() > 0) {
				tokenString = buffer.toString();
				buffer.setLength(0);
			}
		}

		/**
		 * Get the current 1-based position of the tokenizer.
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
		private void reset(String input) {
			if (input == null)
				throw new IllegalArgumentException("input null");
			this.input = input;
			position = -1;
			buffer = new StringBuilder();
			state = State.INIT;
		}

		/**
		 * Process the next char. Return 0 to indicate to continue, 1 to
		 * indicate that a token is available, and -1 to indicate the end of the
		 * input.
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
					throw new IllegalArgumentException(msg(U.U00153, esc, getPosition(), Misc.mark(input, getPosition() - 1,  100)));
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
					throw new IllegalArgumentException(msg(U.U00154, closing, getPosition(), Misc.mark(input, getPosition() - 1, 100)));
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
						// must do this test here to support
						// ArgsScanner#immediate
						throw new IllegalArgumentException(msg(U.U00154, closing, getPosition(), Misc.mark(input, getPosition() - 1, 100)));
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
						throw new IllegalArgumentException(msg(U.U00154, closing, getPosition(), Misc.mark(input, getPosition() - 1, 100)));
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
	 * Constructor for a scanner with custom meta characters. An exception is thrown
	 * if any two meta characters are equal.
	 * 
	 * @param lq
	 *            left quote
	 * @param rq
	 *            right quote
	 * @param nvs
	 *            name-value separator
	 * @param esc
	 *            escape
	 * @throws IllegalArgumentException
	 *             as described in the comment
	 */
	public NameValueScanner(char lq, char rq, char nvs, char esc) {
		if (lq == rq || lq == nvs || lq == esc || rq == nvs || rq == esc || nvs == esc)
			throw new IllegalArgumentException(msg(U.U00163, lq, rq, nvs, esc, ""));
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
	 * Turn a string into a list of isolated values.
	 * 
	 * @param input
	 *            a string
	 * @return a list of strings
	 * @throws IllegalArgumentException
	 *             on invalid input
	 */
	public List<String> asValues(String input) {
		List<String[]> values = asValuesAndPairs(input, true);
		List<String> result = new ArrayList<String>(values.size());
		for (String[] v : values) {
			result.add(v[0]);
		}
		return result;
	}

	/**
	 * Turn a string into a list of name-value pairs and isolated values.
	 * 
	 * @param input
	 *            a string
	 * @return a list of 1-element arrays representing isolated values or
	 *         2-elements arrays representing where name-value pairs
	 * @throws IllegalArgumentException
	 *             on invalid input
	 */
	public List<String[]> asValuesAndPairs(String input) {
		return asValuesAndPairs(input, false);
	}

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
				token1 = tokenizer.nextToken();
				token1String = tokenizer.getTokenString();
				switch (token1) {
				case END_OF_INPUT:
					state = NameValueState.END;
					break;
				case EQUAL_TOKEN:
					if (!valuesOnly)
						throw new IllegalArgumentException(msg(U.U00156, eq, tokenizer.getPosition(), Misc.mark(string, tokenizer.getPosition() - 1, 100)));
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
				token2 = tokenizer.nextToken();
				String token2String = tokenizer.getTokenString();
				switch (token2) {
				case END_OF_INPUT:
					results.add(new String[] { token1String });
					state = NameValueState.END;
					break;
				case EQUAL_TOKEN:
					if (!valuesOnly)
						state = NameValueState.VALUE;
					else {
						results.add(new String[] { token1String });
						token1String = token2String; // token2String is "="
						state = NameValueState.NAME; // (no state transition)
					}
					break;
				case STRING_TOKEN:
					results.add(new String[] { token1String });
					token1String = token2String;
					state = NameValueState.NAME; // (no state transition)
					break;
				default:
					throw new RuntimeException("bug " + state.name());
				}
				break;
			case VALUE:
				token2 = tokenizer.nextToken();
				token2String = tokenizer.getTokenString();
				switch (token2) {
				case END_OF_INPUT:
					throw new IllegalArgumentException(msg(U.U00159, eq, token1String, string));
				case EQUAL_TOKEN:
					throw new IllegalArgumentException(msg(U.U00156, eq, tokenizer.getPosition(), string));
				case STRING_TOKEN:
					results.add(new String[] { token1String, token2String });
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
