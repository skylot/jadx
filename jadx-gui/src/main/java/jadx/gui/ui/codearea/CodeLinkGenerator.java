package jadx.gui.ui.codearea;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.util.Objects;

import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JClass;
import jadx.gui.utils.JumpPosition;

public class CodeLinkGenerator implements LinkGenerator, HyperlinkListener {
	private static final Logger LOG = LoggerFactory.getLogger(CodeLinkGenerator.class);

	private final CodePanel contentPanel;
	private final CodeArea codeArea;
	private final JClass jCls;

	public CodeLinkGenerator(CodePanel contentPanel, CodeArea codeArea, JClass cls) {
		this.contentPanel = contentPanel;
		this.codeArea = codeArea;
		this.jCls = cls;
	}

	@Override
	public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, int offset) {
		try {
			Token token = textArea.modelToToken(offset);
			if (token == null) {
				return null;
			}
			int type = token.getType();
			final int sourceOffset;
			if (type == TokenTypes.IDENTIFIER) {
				sourceOffset = token.getOffset();
			} else if (type == TokenTypes.ANNOTATION && token.length() > 1) {
				sourceOffset = token.getOffset() + 1;
			} else {
				return null;
			}
			// fast skip
			if (token.length() == 1) {
				char ch = token.getTextArray()[token.getTextOffset()];
				if (ch == '.' || ch == ',' || ch == ';') {
					return null;
				}
			}
			final JumpPosition defPos = codeArea.getDefPosForNodeAtOffset(jCls, sourceOffset);
			if (defPos == null) {
				return null;
			}
			if (Objects.equals(defPos.getNode().getRootClass(), jCls)
					&& defPos.getLine() == textArea.getLineOfOffset(sourceOffset) + 1) {
				// ignore self jump
				return null;
			}
			return new LinkGeneratorResult() {
				@Override
				public HyperlinkEvent execute() {
					return new HyperlinkEvent(defPos, HyperlinkEvent.EventType.ACTIVATED, null,
							defPos.getNode().makeLongString());
				}

				@Override
				public int getSourceOffset() {
					return sourceOffset;
				}
			};
		} catch (Exception e) {
			LOG.error("isLinkAtOffset error", e);
			return null;
		}
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		Object obj = e.getSource();
		if (obj instanceof JumpPosition) {
			contentPanel.getTabbedPane().codeJump((JumpPosition) obj);
		}
	}
}
