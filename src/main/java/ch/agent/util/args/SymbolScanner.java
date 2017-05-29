package ch.agent.util.args;

import static ch.agent.util.STRINGS.msg;

import java.util.ArrayList;
import java.util.List;

import ch.agent.util.STRINGS.U;
import ch.agent.util.base.Misc;

/**
 * The symbol scanner splits a string into a list to help locate symbols of
 * substitution variables.
 * <p>
 * Symbols (or identifiers) are prefixed with characters reserved for that
 * purpose. The character is passed to the constructor. This documentation
 * assumes it is a $ sign.
 * <p>
 * When setting a variable, its symbol is prefixed with a single $, like
 * <code>$foo-bar</code>. When accessing a variable, its symbol is prefixed with
 * $$, like <code>$$foo-bar</code>; this is called a variable reference.
 * <p>
 * A valid identifier consists of one or more letters and digits (tested with
 * {@link Character#isLetterOrDigit Character#isLetterOrDigit(char)}), hyphens
 * (-) and underscores (_). The character used as prefix ($) is not allowed
 * inside the identifier.
 * <p>
 * A reference is found when $$ is directly followed by a valid identifier. When
 * a reference is found, the following elements are appended to the list:
 * <ul>
 * <li>the input since the previous reference,
 * <li>a null, signifying the string $$ in its role as symbol prefix, and
 * <li>the symbol.
 * </ul>
 * This continues until the end of the input is reached. The string remaining
 * after the last reference is appended to the list.
 * <p>
 * A null part is inserted instead of $$ because $$ can appear by itself as a
 * plain string in a few corner cases ($$ at the end of input, for example).
 * Using a null removes the ambiguity.
 * <p>
 * In some cases a reference is nested inside a string, and is directly followed
 * by a valid symbol character, which makes it impossible to find the end of the
 * symbol. To handle this, the symbol must be surrounded with two $, like
 * <code>$$$foo-bar$baz</code>.
 */
public class SymbolScanner {

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
	 * Verify the syntax of a symbol. The method throws an
	 * <code>IllegalArgumentException</code> when something is wrong.
	 * 
	 * @param name
	 *            a non-null name with a $ prefix
	 * @throws IllegalArgumentException
	 *             if verification fails
	 */
	public void verify(String name) {
		Misc.nullIllegal(name, "name null");
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

	private boolean isValid(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' && ch != dollar;
	}

	/**
	 * Split the input into a list of strings to make it easy to find symbol
	 * references.
	 * <p>
	 * As an example, the input <code>"abc$$xy1 def $$A5 $$[0] bx"</code> is
	 * split into the list
	 * <code>("abc", null, "xy1", " def ", null, "A5", " $$[0] bx")</code>. The
	 * last occurrence of $$ is not followed by a valid symbol, so it is
	 * ignored.
	 * <p>
	 * When a part is null it is always followed by a part containing a symbol.
	 * In some corner cases, a part can contain the string $$ without being a
	 * symbol prefix.
	 * 
	 * @param input
	 *            a string
	 * @return a list of strings
	 */
	public List<String> split(String input) {
		reset(input);
		List<String> output = new ArrayList<String>();
		boolean end = false;
		while (!end) {
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
	 * the end of the input. The method modifies 
	 * {@link #symbolPosition) and {@link #currentPosition).
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
