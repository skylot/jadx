package jadx.gui.ui.codearea;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaNode;

public final class JadxTokenMaker extends JavaTokenMaker {
	private static final Logger LOG = LoggerFactory.getLogger(JadxTokenMaker.class);

	private final CodeArea codeArea;

	public JadxTokenMaker(CodeArea codeArea) {
		this.codeArea = codeArea;
	}

	@Override
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
		Token tokens = super.getTokenList(text, initialTokenType, startOffset);
		if (tokens.getType() != TokenTypes.NULL) {
			try {
				processTokens(tokens);
			} catch (Exception e) {
				LOG.error("Process tokens failed for text: {}", text, e);
			}
		}
		return tokens;
	}

	private void processTokens(Token tokens) {
		Token prev = null;
		Token current = tokens;
		while (current != null) {
			if (prev != null) {
				switch (current.getType()) {
					case TokenTypes.IDENTIFIER:
						current = mergeLongClassNames(prev, current, false);
						break;

					case TokenTypes.ANNOTATION:
						current = mergeLongClassNames(prev, current, true);
						break;
				}
			}
			prev = current;
			current = current.getNextToken();
		}
	}

	@NotNull
	private Token mergeLongClassNames(Token prev, Token current, boolean annotation) {
		int offset = current.getTextOffset();
		if (annotation) {
			offset++;
		}
		JavaNode javaNode = codeArea.getJavaNodeAtOffset(offset);
		if (javaNode instanceof JavaClass) {
			String name = javaNode.getName();
			String lexeme = current.getLexeme();
			if (annotation && lexeme.length() > 1) {
				lexeme = lexeme.substring(1);
			}
			if (!lexeme.equals(name) && isClassNameStart(javaNode, lexeme)) {
				// try to replace long class name with one token
				Token replace = concatTokensUntil(current, name);
				if (replace != null && prev instanceof TokenImpl) {
					TokenImpl impl = ((TokenImpl) prev);
					impl.setNextToken(replace);
					current = replace;
				}
			}
		}
		return current;
	}

	private boolean isClassNameStart(JavaNode javaNode, String lexeme) {
		if (javaNode.getFullName().startsWith(lexeme)) {
			// full class name
			return true;
		}
		if (javaNode.getTopParentClass().getName().startsWith(lexeme)) {
			// inner class references from parent class
			return true;
		}
		return false;
	}

	@Nullable
	private Token concatTokensUntil(Token start, String endText) {
		StringBuilder sb = new StringBuilder();
		Token current = start;
		while (current != null && current.getType() != TokenTypes.NULL) {
			String text = current.getLexeme();
			if (text != null) {
				sb.append(text);
				if (text.equals(endText)) {
					char[] line = sb.toString().toCharArray();
					TokenImpl token = new TokenImpl(line, 0, line.length - 1, start.getOffset(),
							start.getType(), start.getLanguageIndex());
					token.setNextToken(current.getNextToken());
					return token;
				}
			}
			current = current.getNextToken();
		}
		return null;
	}
}
