package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.gui.settings.JadxProject;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;

public class CommentDialog extends JDialog {
	private static final Logger LOG = LoggerFactory.getLogger(CommentDialog.class);

	public static void show(CodeArea codeArea, ICodeComment blankComment) {
		ICodeComment existComment = searchForExistComment(codeArea, blankComment);
		Dialog dialog;
		if (existComment != null) {
			dialog = new CommentDialog(codeArea, existComment, true);
		} else {
			dialog = new CommentDialog(codeArea, blankComment, false);
		}
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

	private static ICodeComment searchForExistComment(CodeArea codeArea, ICodeComment blankComment) {
		try {
			JadxProject project = codeArea.getProject();
			JadxCodeData codeData = project.getCodeData();
			if (codeData == null || codeData.getComments().isEmpty()) {
				return null;
			}
			for (ICodeComment comment : codeData.getComments()) {
				if (Objects.equals(comment.getNodeRef(), blankComment.getNodeRef())
						&& comment.getOffset() == blankComment.getOffset()
						&& comment.getAttachType() == blankComment.getAttachType()) {
					return comment;
				}
			}
		} catch (Exception e) {
			LOG.error("Error searching for exists comment", e);
		}
		return null;
	}

	private final transient CodeArea codeArea;
	private final transient ICodeComment comment;
	private final transient boolean updateComment;

	private transient JTextField commentField;

	public CommentDialog(CodeArea codeArea, ICodeComment comment, boolean updateComment) {
		super(codeArea.getMainWindow());
		this.codeArea = codeArea;
		this.comment = comment;
		this.updateComment = updateComment;
		initUI();
	}

	private void apply() {
		String newCommentStr = commentField.getText();
		if (newCommentStr == null || newCommentStr.trim().isEmpty()) {
			dispose();
			return;
		}
		ICodeComment newComment = new JadxCodeComment(comment.getNodeRef(),
				newCommentStr, comment.getOffset(), comment.getAttachType());
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

	private void initUI() {
		commentField = new JTextField(40);
		commentField.addActionListener(e -> apply());
		if (updateComment) {
			commentField.setText(comment.getComment());
			commentField.selectAll();
		}
		new TextStandardActions(commentField);

		JLabel lbl = new JLabel(NLS.str("comment_dialog.label"), SwingConstants.LEFT);

		JPanel commentPanel = new JPanel();
		commentPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		commentPanel.add(lbl);
		commentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

		JPanel textPane = new JPanel();
		textPane.setLayout(new BoxLayout(textPane, BoxLayout.PAGE_AXIS));
		textPane.add(commentField);
		textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(commentPanel, BorderLayout.PAGE_START);
		contentPane.add(textPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		if (updateComment) {
			setTitle(NLS.str("comment_dialog.title.update"));
		} else {
			setTitle(NLS.str("comment_dialog.title.add"));
		}
		if (!codeArea.getMainWindow().getSettings().loadWindowPos(this)) {
			setSize(800, 80);
		}
		// always pack (ignore saved windows sizes)
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		UiUtils.addEscapeShortCutToDispose(this);
	}

	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());

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

	@Override
	public void dispose() {
		codeArea.getMainWindow().getSettings().saveWindowPos(this);
		super.dispose();
	}
}
