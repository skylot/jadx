package jadx.gui.ui.codearea;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Very simple token maker to use only one token per line without any parsing
 */
@SuppressWarnings("unused") // class registered by name in {@link AbstractCodeArea}
public class SimpleTokenMaker extends TokenMakerBase {
	private final TokenImpl token;

	public SimpleTokenMaker() {
		token = new TokenImpl();
		token.setType(TokenTypes.IDENTIFIER);
	}

	@Override
	public Token getTokenList(Segment segment, int initialTokenType, int startOffset) {
		token.text = segment.array;
		token.textOffset = startOffset;
		token.textCount = segment.count;
		token.setOffset(startOffset);
		return token;
	}
}
