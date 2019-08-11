package jadx.gui.ui.codearea;

import java.util.Objects;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.utils.JumpPosition;

public class CodeLinkGenerator implements LinkGenerator, HyperlinkListener {
	private static final Logger LOG = LoggerFactory.getLogger(CodeLinkGenerator.class);

	private final ContentPanel contentPanel;
	private final CodeArea codeArea;
	private final JNode jNode;

	public CodeLinkGenerator(CodeArea codeArea) {
		this.contentPanel = codeArea.getContentPanel();
		this.codeArea = codeArea;
		this.jNode = codeArea.getNode();
	}

	@Override
	public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, int offset) {
		try {
			if (jNode.getCodeInfo() == null) {
				return null;
			}
			Token token = textArea.modelToToken(offset);
			int sourceOffset = codeArea.adjustOffsetForToken(token);
			if (sourceOffset == -1) {
				return null;
			}
			final JumpPosition defPos = codeArea.getDefPosForNodeAtOffset(sourceOffset);
			if (defPos == null) {
				return null;
			}
			if (Objects.equals(defPos.getNode().getRootClass(), jNode)
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
