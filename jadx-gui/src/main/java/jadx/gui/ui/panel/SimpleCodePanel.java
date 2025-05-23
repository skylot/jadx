package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.utils.NLS;

// The code panel class is used to display the code of the selected node.
public class SimpleCodePanel extends JPanel {
	private static final long serialVersionUID = -4073178549744330905L;
	private static final Logger LOG = LoggerFactory.getLogger(SimpleCodePanel.class);

	private final RSyntaxTextArea codeArea;
	private final RTextScrollPane codeScrollPane;
	private final JLabel titleLabel;

	public SimpleCodePanel(MainWindow mainWindow) {
		JadxSettings settings = mainWindow.getSettings();

		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Set the minimum size to ensure the panel is not completely minimized
		setMinimumSize(new Dimension(300, 400));
		setPreferredSize(new Dimension(800, 600));

		// The title label
		titleLabel = new JLabel(NLS.str("usage_dialog_plus.code_view"));
		titleLabel.setFont(settings.getFont());
		titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

		// The code area
		codeArea = AbstractCodeArea.getDefaultArea(mainWindow);
		codeArea.setText("// " + NLS.str("usage_dialog_plus.select_node"));

		codeScrollPane = new RTextScrollPane(codeArea);
		codeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		codeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		add(titleLabel, BorderLayout.NORTH);
		add(codeScrollPane, BorderLayout.CENTER);

		applySettings(settings);
	}

	private void applySettings(JadxSettings settings) {
		codeScrollPane.setLineNumbersEnabled(settings.getLineNumbersMode() != LineNumbersMode.DISABLE);
		codeScrollPane.getGutter().setLineNumberFont(settings.getFont());
		codeArea.setFont(settings.getFont());
	}

	public void showCode(JNode node, String codeLine) {
		if (node != null) {
			titleLabel.setText(NLS.str("usage_dialog_plus.code_for", node.makeLongString()));
			codeArea.setSyntaxEditingStyle(node.getSyntaxName());

			// Get the complete code
			String contextCode = getContextCode(node, codeLine);
			codeArea.setText(contextCode);

			// Highlight the key line and scroll to that position
			scrollToCodeLine(codeArea, codeLine);

			// If it is a CodeNode, we can get a more precise position
			if (node instanceof CodeNode) {
				CodeNode codeNode = (CodeNode) node;
				int pos = codeNode.getPos();
				if (pos > 0) {
					// Try to use the position information to more accurately locate
					try {
						String text = codeArea.getText();
						int lineNum = 0;
						int curPos = 0;
						// Calculate the line number corresponding to the position
						for (int i = 0; i < text.length() && curPos <= pos; i++) {
							if (text.charAt(i) == '\n') {
								lineNum++;
							}
							curPos++;
						}

						if (lineNum > 0) {
							// Scroll to the calculated line number
							int finalLineNum = lineNum;
							SwingUtilities.invokeLater(() -> {
								try {
									Rectangle2D lineRect = codeArea
											.modelToView2D(codeArea.getLineStartOffset(finalLineNum));
									if (lineRect != null) {
										JScrollPane scrollPane = (JScrollPane) codeArea.getParent().getParent();
										Rectangle viewRect = scrollPane.getViewport().getViewRect();
										int y = (int) (lineRect.getY() - (viewRect.height - lineRect.getHeight()) / 2);
										if (y < 0) {
											y = 0;
										}
										scrollPane.getViewport().setViewPosition(new Point(0, y));
									}
								} catch (Exception e) {
									// Fall back to using string matching
									scrollToCodeLine(codeArea, codeLine);
								}
							});
						}
					} catch (Exception e) {
						// Fall back to using string matching
						scrollToCodeLine(codeArea, codeLine);
					}
				} else {
					// If there is no position information, use string matching
					scrollToCodeLine(codeArea, codeLine);
				}
			} else {
				// Not a CodeNode, use string matching
				scrollToCodeLine(codeArea, codeLine);
			}
		} else {
			titleLabel.setText(NLS.str("usage_dialog_plus.code_view"));
			codeArea.setText("// " + NLS.str("usage_dialog_plus.select_node"));
		}
	}

	private String getContextCode(JNode node, String codeLine) {
		// Always try to get the complete code
		if (node instanceof CodeNode) {
			CodeNode codeNode = (CodeNode) node;
			JNode usageJNode = codeNode.getJParent();
			if (usageJNode != null) {
				// Try to get the complete code of the method or class
				String fullCode = getFullNodeCode(usageJNode);
				if (fullCode != null && !fullCode.isEmpty()) {
					return fullCode;
				}
			}
		}

		// If you cannot get more context, at least add some empty lines and comments
		return "// Unable to get complete context, only display related lines\n\n" + codeLine;
	}

	private String getFullNodeCode(JNode node) {
		if (node != null) {
			// Get the code information of the node
			ICodeInfo codeInfo = node.getCodeInfo();
			if (codeInfo != null && !codeInfo.equals(ICodeInfo.EMPTY)) {
				return codeInfo.getCodeStr();
			}

			// If it is a class node, try to get the class code
			if (node instanceof JClass) {
				JClass jClass = (JClass) node;
				return jClass.getCodeInfo().getCodeStr();
			}
		}
		return null;
	}

	private void scrollToCodeLine(RSyntaxTextArea textArea, String lineToHighlight) {
		// Try to find and highlight a specific line in the code and scroll to that position
		try {
			String fullText = textArea.getText();
			int lineIndex = fullText.indexOf(lineToHighlight);
			if (lineIndex >= 0) {
				// Ensure the text area has updated the layout
				textArea.revalidate();

				// Highlight the code line
				textArea.setCaretPosition(lineIndex);
				int endIndex = lineIndex + lineToHighlight.length();
				textArea.select(lineIndex, endIndex);
				textArea.getCaret().setSelectionVisible(true);

				// Use SwingUtilities.invokeLater to ensure the scroll is executed after the UI is updated
				SwingUtilities.invokeLater(() -> {
					try {
						// Get the line number
						int lineNum = textArea.getLineOfOffset(lineIndex);
						// Ensure the line is centered in the view
						Rectangle2D lineRect = textArea.modelToView2D(textArea.getLineStartOffset(lineNum));
						if (lineRect != null) {
							// Calculate the center point of the view
							JScrollPane scrollPane = (JScrollPane) textArea.getParent().getParent();
							Rectangle viewRect = scrollPane.getViewport().getViewRect();
							int y = (int) (lineRect.getY() - (viewRect.height - lineRect.getHeight()) / 2);
							if (y < 0) {
								y = 0;
							}
							// Scroll to the calculated position
							scrollPane.getViewport().setViewPosition(new Point(0, y));
						}
					} catch (Exception e) {
						LOG.debug("Error scrolling to line: {}", e.getMessage());
					}
				});
			} else {
				LOG.debug("Could not find line to highlight: {}", lineToHighlight);
			}
		} catch (Exception e) {
			LOG.debug("Error highlighting line: {}", e.getMessage());
		}
	}
}
