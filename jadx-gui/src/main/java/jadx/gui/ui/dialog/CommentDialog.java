package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.CommentStyle;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.gui.settings.JadxProject;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;

public class CommentDialog extends CommonDialog {
	private static final long serialVersionUID = -1865682124935757528L;

	private static final Logger LOG = LoggerFactory.getLogger(CommentDialog.class);

	public static void show(CodeArea codeArea, ICodeComment comment, boolean updateComment) {
		CommentDialog dialog = new CommentDialog(codeArea, comment, updateComment);
		dialog.setVisible(true);
	}

	private static void updateCommentsData(CodeArea codeArea, Consumer<List<ICodeComment>> updater) {
		try {
			JadxProject project = codeArea.getProject();
			JadxCodeData codeData = project.getCodeData();
			if (codeData == null) {
				codeData = new JadxCodeData();
			}
			List<ICodeComment> list = new ArrayList<>(codeData.getComments());
			updater.accept(list);
			Collections.sort(list);
			codeData.setComments(list);
			project.setCodeData(codeData);
			codeArea.getMainWindow().getWrapper().reloadCodeData();
		} catch (Exception e) {
			LOG.error("Comment action failed", e);
		}
		try {
			// refresh code
			codeArea.refreshClass();
		} catch (Exception e) {
			LOG.error("Failed to reload code", e);
		}
	}

	private final transient CodeArea codeArea;
	private final transient ICodeComment comment;
	private final transient boolean updateComment;

	private transient JTextArea commentArea;
	private transient JComboBox<CommentStyle> styleCombo;

	public CommentDialog(CodeArea codeArea, ICodeComment comment, boolean updateComment) {
		super(codeArea.getMainWindow());
		this.codeArea = codeArea;
		this.comment = comment;
		this.updateComment = updateComment;
		initUI();
	}

	private void apply() {
		String newCommentStr = commentArea.getText().trim();
		if (newCommentStr.isEmpty()) {
			if (updateComment) {
				remove();
			} else {
				cancel();
			}
			return;
		}
		CommentStyle style = (CommentStyle) styleCombo.getSelectedItem();
		ICodeComment newComment = new JadxCodeComment(comment.getNodeRef(), comment.getCodeRef(), newCommentStr, style);
		if (updateComment) {
			updateCommentsData(codeArea, list -> {
				list.remove(comment);
				list.add(newComment);
			});
		} else {
			updateCommentsData(codeArea, list -> list.add(newComment));
		}
		dispose();
	}

	private void remove() {
		updateCommentsData(codeArea, list -> list.removeIf(c -> c == comment));
		dispose();
	}

	private void cancel() {
		dispose();
	}

	private void initUI() {
		commentArea = new JTextArea();
		TextStandardActions.attach(commentArea);
		commentArea.setEditable(true);
		commentArea.setFont(mainWindow.getSettings().getFont());
		commentArea.setAlignmentX(Component.LEFT_ALIGNMENT);

		commentArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						if (e.isShiftDown() || e.isControlDown()) {
							commentArea.append("\n");
						} else {
							apply();
						}
						break;

					case KeyEvent.VK_ESCAPE:
						cancel();
						break;
				}
			}
		});
		if (updateComment) {
			commentArea.setText(comment.getComment());
		}

		JScrollPane textAreaScrollPane = new JScrollPane(commentArea);
		textAreaScrollPane.setAlignmentX(LEFT_ALIGNMENT);

		styleCombo = new JComboBox<>(CommentStyle.values());
		styleCombo.setSelectedItem(comment.getStyle());

		JLabel commentLabel = new JLabel(NLS.str("comment_dialog.label"), SwingConstants.LEFT);
		JLabel styleLabel = new JLabel(NLS.str("comment_dialog.style"), SwingConstants.LEFT);
		JLabel usageLabel = new JLabel(NLS.str("comment_dialog.usage"), SwingConstants.LEFT);

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.PAGE_AXIS));
		inputPanel.add(commentLabel);
		inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		inputPanel.add(textAreaScrollPane);
		inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		inputPanel.add(usageLabel);

		JPanel stylePanel = new JPanel();
		stylePanel.setLayout(new BoxLayout(stylePanel, BoxLayout.LINE_AXIS));
		stylePanel.setAlignmentX(LEFT_ALIGNMENT);
		stylePanel.add(styleLabel);
		stylePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		stylePanel.add(styleCombo);

		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.add(inputPanel, BorderLayout.CENTER);
		mainPanel.add(stylePanel, BorderLayout.PAGE_END);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(mainPanel, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		if (updateComment) {
			setTitle(NLS.str("comment_dialog.title.update"));
		} else {
			setTitle(NLS.str("comment_dialog.title.add"));
		}
		commonWindowInit();
	}

	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
		cancelButton.addActionListener(event -> cancel());

		String applyStr = updateComment ? NLS.str("common_dialog.update") : NLS.str("common_dialog.add");
		JButton renameBtn = new JButton(applyStr);
		renameBtn.addActionListener(event -> apply());
		getRootPane().setDefaultButton(renameBtn);

		JButton removeBtn;
		if (updateComment) {
			removeBtn = new JButton(NLS.str("common_dialog.remove"));
			removeBtn.addActionListener(event -> remove());
		} else {
			removeBtn = null;
		}

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(renameBtn);
		if (removeBtn != null) {
			buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPane.add(removeBtn);
		}
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}
}
