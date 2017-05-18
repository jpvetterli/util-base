package ch.agent.util.args;

import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.List;

import ch.agent.util.STRINGS.U;

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
			INIT, STRING, BRACKET, ESCAPE, END
		}
		
		private final char opening;
		private final char closing;
		private final char equals;
		private final char esc;

		private String input;
		private int position; // first is 0
		private String token;
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
			token = null;
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
					state = escapedState;
				}
				break;
			case INIT:
				if (ch == 0) {
					addToken(false);
					state = State.END;
				} else if (ch == esc) {
					escapedState = state;
					state = State.ESCAPE;
				} else if (ch == equals) {
					buffer.append(equals);
					addToken(false);
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
					addToken(false);
					state = State.END;
				} else if (ch == esc) {
					escapedState = state;
					state = State.ESCAPE;
				} else if (isMeta(ch)) {
					if (ch == closing) {
						// must do this test here to support ArgsScanner#immediate
						throw new IllegalArgumentException(msg(U.U00154, closing, getPosition(), input));
					}
					addToken(false);
					backtrack();
				} else if (Character.isWhitespace(ch))
					addToken(false);
				else
					buffer.append(ch);
				break;
			case BRACKET:
				if (ch == 0) {
					addToken(false);
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
				throw new RuntimeException("bug: " + state.name());
			}
			return token != null ? 1 : (ch == 0 ? -1 : 0);
		}

		private boolean isMeta(char ch) {
			return ch == equals || ch == opening || ch == closing;
		}
		
		private void addToken(boolean emptyOk) {
			if (depth != 0)
				throw new IllegalArgumentException(msg(U.U00155, closing, getPosition(), input));
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
	 * The symbol scanner supports splitting a string to help locate references.
	 * References look like $$foo, where foo is a variable. A reference is found
	 * when $$ is directly followed by a symbol made of one or more letters or
	 * digits (as determined by {@link Character#isLetterOrDigit}) except $.
	 * When a reference is found, the input since the previous reference, a null
	 * signifying the string $$ in its role as symbol prefix, and the symbol are
	 * appended to the list. This continues until the end of the input is
	 * reached, which is appended to the list.
	 * <p>
	 * A null part is inserted instead of $$ because $$ can appear by itself as
	 * a valid part in a few corner cases ($$ at the end of input or $$$$x for
	 * example). Using a null removes the ambiguity.
	 * <p>
	 * A valid identifier consists of one or more letters, digits, hyphens and
	 * underscore. The character used as prefix, usually $, is not allowed
	 * inside the identifier.
	 * 
	 * <p>
	 * The scanner has no notion of escape characters. The client can handle
	 * them; they are easy to find as they trail the part preceding any $$.
	 */
	public static class SymbolScanner {

		private enum State {
			INIT, DOLLAR1, DOLLAR2, DOLLAR3, SYMBOL, DOLLARSYMBOL, END
		}
		
		private final char dollar;

		private String input;
		private int prefixPosition; // 0-based
		private int symbolPosition; // 0-based
		private int currentPosition; // 0-based
		private State state;

		/**
		 * Constructor.
		 * 
		 * @param dollar
		 *            the character which stands doubled in front of symbols
		 */
		public SymbolScanner(char dollar) {
			this.dollar = dollar;
		}
		
		/**
		 * Verify the name syntax of a substitution variable.
		 * 
		 * @param name a name with the $ prefix not removed
		 */
		public void verify(String name) {
			if (name.length() == 0)
				throw new IllegalArgumentException(msg(U.U00126, dollar));
			if (name.charAt(0) != dollar)
				throw new IllegalArgumentException(msg(U.U00125, name, dollar));
			if (name.length() == 1)
				throw new IllegalArgumentException(msg(U.U00124, name));
			for (int i = 1; i < name.length(); i++) {
				char ch = name.charAt(i);
				if (!isValid(ch)) {
					throw new IllegalArgumentException(msg(U.U00125, name, dollar));
				}
			}
		}
		
		/**
		 * @param ch
		 * @return
		 */
		private boolean isValid(char ch) {
			return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' && ch != dollar;
		}
		
		/**
		 * Split the input into a list of strings to make it easy to find symbol
		 * references. A symbol has one or more letter or digits and usually
		 * stands directly after the sequence $$ (or any character passed to the
		 * constructor, doubled). In some cases the symbol is surrounded by two
		 * $ characters, to make it possible to put a symbol reference anywhere.
		 * In such cases, the symbol is preceded by three $ and followed by one.
		 * <p>
		 * As an example, the input "abc$$xy1 def $$A5 $$[0] bx" is split into
		 * the list ("abc", null, "xy1", " def ", null, "A5", " $$[0] bx"). The
		 * last occurrence of $$ is not followed by a valid symbol, so it is
		 * ignored.
		 * <p>
		 * When a part is null it is always followed by a part containing a
		 * symbol. In some corner cases, a part can contain the string $$
		 * without being a symbol prefix.
		 * 
		 * @param input
		 *            a string
		 * @return the string split into parts
		 */
		public List<String> split(String input) {
			reset(input);
			List<String> output = new ArrayList<String>();
			boolean end = false;
			while(!end) {
				switch (split()) {
				case 0:
					break;
				case 1:
					if (input.charAt(symbolPosition) == dollar) {
						if (symbolPosition - prefixPosition > 2)
							output.add(input.substring(prefixPosition, symbolPosition - 2));
						output.add(null);
						output.add(input.substring(symbolPosition + 1, currentPosition - 1));
						prefixPosition = currentPosition;
					} else {
						if (symbolPosition - prefixPosition > 2)
							output.add(input.substring(prefixPosition, symbolPosition - 2));
						output.add(null);
						output.add(input.substring(symbolPosition, currentPosition));
						prefixPosition = currentPosition;
					}
					symbolPosition = -1;
					break;
				case -1:
					if (prefixPosition < input.length())
						output.add(input.substring(prefixPosition));
					end = true;
					break;
				default:
					throw new RuntimeException("bug");
				}
			}
			return output;
		}
		
		private void reset(String input) {
			if (input == null)
				throw new IllegalArgumentException("input null");
			this.input = input;
			prefixPosition = 0; // = start of input
			symbolPosition = -1;
			currentPosition = -1;
			state = State.INIT;
		}

		/**
		 * Process the next char. Return 0 to indicate to continue,
		 * 1 to indicate that a part is available, and -1 to indicate
		 * the end of the input. The part available starts at {@link #lastPosition}
		 * and extends to {@link #position).
		 * 
		 * @return 0 (continue) or 1 (part found) or -1 (end)
		 */
		private int split() {
			boolean partFound = false;
			char ch = state == State.END ? 0 : advance();
			switch (state) {
			case END:
				break;
			case INIT:
				if (ch == dollar)
					state = State.DOLLAR1;
				else if (ch == 0)
					state = State.END;
				break;
			case DOLLAR1: 
				if (ch == dollar)
					state = State.DOLLAR2;
				else 
					state = ch == 0 ? State.END : State.INIT;
				break;
			case DOLLAR2: 
				if (isValid(ch)) {
					// symbol cannot be empty
					symbolPosition = currentPosition;
					state = State.SYMBOL;
				} else if (ch == dollar)
					state = State.DOLLAR3; 
				else
					state = ch == 0 ? State.END : State.INIT;
				break;
			case DOLLAR3: 
				if (isValid(ch)) {
					// symbol as above + between 2 dollars
					symbolPosition = currentPosition - 1;
					state = State.DOLLARSYMBOL;
				} else if (ch == dollar)
					; // same 
				else
					state = ch == 0 ? State.END : State.INIT;
				break;
			case SYMBOL:
				if (!isValid(ch)) {
					partFound = true;
					state = ch == 0 ? State.END : State.INIT;
				}
				break;
			case DOLLARSYMBOL:
				if (ch == dollar) {
					partFound = true;
					state = advance() == 0 ? State.END : State.INIT;
				}
				break;
			default:
				throw new RuntimeException("bug: " + state.name());
			}
			return partFound ? 1 : (ch == 0 ? -1 : 0);
		}
	
		/**
		 * Return the next char from the input or 0 if there is no more input.
		 * 
		 * @return the next char or 0
		 */
		private char advance() {
			if (state == State.END)
				throw new RuntimeException("bug");
			try {
				return input.charAt(++currentPosition);
			} catch (IndexOutOfBoundsException e) {
				return 0;
			}
		}
		
	}
	
	private enum NameValueState {
		INIT, END, NAME, VALUE
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
	public ArgsScanner(char lq, char rq, char nvs, char esc) {
		Args.validateMetaCharacters(lq, rq, nvs, esc);
		tokenizer = new Tokenizer(lq, rq, nvs, esc);
		eq = String.valueOf(nvs);
	}
	
	/**
	 * Constructor for a scanner using default meta characters. The defaults are
	 * [, ], = and \.
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
		String token1 = null;
		String token2 = null;
		while (state != NameValueState.END) {
			switch (state) {
			case INIT:
				token1 = tokenizer.token();
				if (token1 == null) {
					state = NameValueState.END;
				} else {
					if (token1.equals(eq) && !valuesOnly)
						throw new IllegalArgumentException(msg(U.U00156, eq, tokenizer.getPosition(), string));
					else
						state = NameValueState.NAME;
				}
				break;
			case NAME:
				token2 = tokenizer.token();
				if (token2 == null) {
					results.add(new String[]{token1});
					state = NameValueState.END;
				} else {
					if (token2.equals(eq) && !valuesOnly)
						state = NameValueState.VALUE;
					else {
						results.add(new String[]{token1});
						token1 = token2;
						state = NameValueState.NAME; // (no state transition)
					}
				}
				break;
			case VALUE:
				token2 = tokenizer.token();
				if (token2 == null) {
					throw new IllegalArgumentException(msg(U.U00159, eq, token1, string));
				} else {
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
	
}
