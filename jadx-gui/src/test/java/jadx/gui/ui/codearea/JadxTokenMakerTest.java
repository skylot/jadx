package jadx.gui.ui.codearea;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JadxTokenMakerTest {

	@Test
	void fixContextualKeywordTokenTypes() {
		assertIdentifier("var", TokenTypes.DATA_TYPE);
		assertIdentifier("yield", TokenTypes.RESERVED_WORD_2);
		assertIdentifier("record", TokenTypes.RESERVED_WORD);
		assertIdentifier("sealed", TokenTypes.RESERVED_WORD);
		assertIdentifier("module", TokenTypes.RESERVED_WORD);
	}

	@Test
	void keepRealKeywordsAndTypes() {
		assertTypeUnchanged("int", TokenTypes.DATA_TYPE);
		assertTypeUnchanged("return", TokenTypes.RESERVED_WORD_2);
		assertTypeUnchanged("class", TokenTypes.RESERVED_WORD);
	}

	private static void assertIdentifier(String lexeme, int tokenType) {
		Token token = buildToken(lexeme, tokenType);
		JadxTokenMaker.fixContextualKeyword(token);
		assertThat(token.getType())
				.as("token type for '%s'", lexeme)
				.isEqualTo(TokenTypes.IDENTIFIER);
	}

	private static void assertTypeUnchanged(String lexeme, int tokenType) {
		Token token = buildToken(lexeme, tokenType);
		JadxTokenMaker.fixContextualKeyword(token);
		assertThat(token.getType())
				.as("token type for '%s'", lexeme)
				.isEqualTo(tokenType);
	}

	private static Token buildToken(String lexeme, int tokenType) {
		char[] chars = lexeme.toCharArray();
		return new TokenImpl(chars, 0, chars.length - 1, 0, tokenType, 0);
	}
}
