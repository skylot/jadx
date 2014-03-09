package jadx.gui;

import jadx.api.CodePosition;
import jadx.gui.treemodel.JClass;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import java.awt.Color;

import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxTextArea extends RSyntaxTextArea {
	private static final Logger LOG = LoggerFactory.getLogger(JadxTextArea.class);

	private static final Color BACKGROUND = new Color(0xf7f7f7);
	private static final Color JUMP_FOREGROUND = new Color(0x785523);
	private static final Color JUMP_BACKGROUND = new Color(0xE6E6FF);

	private final JClass cls;
	private final MainWindow rootWindow;


	public JadxTextArea(MainWindow mainWindow, JClass cls) {
		this.rootWindow = mainWindow;
		this.cls = cls;

		setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		SyntaxScheme scheme = getSyntaxScheme();
		scheme.getStyle(Token.FUNCTION).foreground = Color.BLACK;

		setMarkOccurrences(true);
		setBackground(BACKGROUND);
		setAntiAliasingEnabled(true);
		setEditable(false);
		getCaret().setVisible(true);

		setHyperlinksEnabled(true);
		CodeLinkGenerator codeLinkProcessor = new CodeLinkGenerator(cls);
		setLinkGenerator(codeLinkProcessor);
		addHyperlinkListener(codeLinkProcessor);

		setText(cls.getCode());
	}

	private boolean isJumpToken(Token token) {
		if (token.getType() == TokenTypes.IDENTIFIER) {
			CodePosition pos = getCodePosition(cls, this, token.getOffset());
			if (pos != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean getUnderlineForToken(Token t) {
		if (isJumpToken(t)) {
			return true;
		}
		return super.getUnderlineForToken(t);
	}

	static CodePosition getCodePosition(JClass jCls, RSyntaxTextArea textArea, int offset) {
		try {
			int line = textArea.getLineOfOffset(offset);
			int lineOffset = offset - textArea.getLineStartOffset(line);
			return jCls.getCls().getDefinitionPosition(line + 1, lineOffset + 1);
		} catch (BadLocationException e) {
			LOG.error("Can't get line by offset", e);
			return null;
		}
	}

	private class CodeLinkGenerator implements LinkGenerator, HyperlinkListener {
		private final JClass jCls;

		public CodeLinkGenerator(JClass cls) {
			this.jCls = cls;
		}

		@Override
		public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, int offset) {
			try {
				Token token = textArea.modelToToken(offset);
				if (token != null) {
					offset = token.getOffset();
				}
				final CodePosition defPos = getCodePosition(jCls, textArea, offset);
				if (defPos != null) {
					final int sourceOffset = offset;
					return new LinkGeneratorResult() {
						@Override
						public HyperlinkEvent execute() {
							return new HyperlinkEvent(defPos, HyperlinkEvent.EventType.ACTIVATED, null,
									defPos.getJavaClass().getFullName());
						}

						@Override
						public int getSourceOffset() {
							return sourceOffset;
						}
					};
				}
			} catch (Exception e) {
				LOG.error("isLinkAtOffset error", e);
			}
			return null;
		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			Object obj = e.getSource();
			if (obj instanceof CodePosition) {
				CodePosition pos = (CodePosition) obj;
				JClass cls = new JClass(pos.getJavaClass());
				rootWindow.showCode(cls, pos.getLine());
				LOG.debug("Code jump to: {}", pos);
			}
		}
	}
}
