package jadx.gui.ui.startpage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;

import jadx.gui.utils.Icons;

public class RecentProjectListCellRenderer extends JPanel implements ListCellRenderer<RecentProjectItem> {
	private static final long serialVersionUID = 5550591869239586857L;

	private final JLabel fileNameLabel;
	private final JLabel pathLabel;
	private final JButton removeProjectBtn;

	private final Color defaultBackground;
	private final Color defaultForeground;

	private final Color selectedBackground;
	private final Color selectedForeground;

	private Rectangle removeIconBounds;

	public RecentProjectListCellRenderer(Font baseFont) {
		super(new BorderLayout(5, 0));
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

		this.fileNameLabel = new JLabel();
		fileNameLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize()));

		this.pathLabel = new JLabel();
		pathLabel.setFont(baseFont.deriveFont(baseFont.getSize() - 2f));
		pathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

		JPanel textPanel = new JPanel(new BorderLayout());
		textPanel.setOpaque(false);
		textPanel.add(fileNameLabel, BorderLayout.NORTH);
		textPanel.add(pathLabel, BorderLayout.SOUTH);

		removeProjectBtn = new JButton();
		removeProjectBtn.setIcon(Icons.CLOSE_INACTIVE);
		removeProjectBtn.setOpaque(false);
		removeProjectBtn.setUI(new BasicButtonUI());
		removeProjectBtn.setContentAreaFilled(false);
		removeProjectBtn.setFocusable(false);
		removeProjectBtn.setBorder(null);
		removeProjectBtn.setBorderPainted(false);

		add(textPanel, BorderLayout.CENTER);
		add(removeProjectBtn, BorderLayout.EAST);

		defaultBackground = UIManager.getColor("List.background");
		defaultForeground = UIManager.getColor("List.foreground");

		selectedBackground = UIManager.getColor("List.selectionBackground");
		selectedForeground = UIManager.getColor("List.selectionForeground");
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends RecentProjectItem> list,
			RecentProjectItem value, int index, boolean isSelected, boolean cellHasFocus) {

		fileNameLabel.setText(value.getProjectName());
		pathLabel.setText(value.getAbsolutePath());

		boolean isThisRemoveButtonHovered = (index == StartPagePanel.hoveredRemoveBtnIndex);
		removeProjectBtn.setIcon(isThisRemoveButtonHovered ? Icons.CLOSE : Icons.CLOSE_INACTIVE);
		removeProjectBtn.setRolloverEnabled(isThisRemoveButtonHovered);

		if (isSelected) {
			setBackground(selectedBackground);
			fileNameLabel.setForeground(selectedForeground);
			pathLabel.setForeground(selectedForeground.darker());
			removeProjectBtn.setForeground(selectedForeground);
		} else {
			setBackground(defaultBackground);
			fileNameLabel.setForeground(defaultForeground);
			pathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
			removeProjectBtn.setForeground(defaultForeground);
		}

		setToolTipText(value.getAbsolutePath());

		return this;
	}

	/**
	 * Overriding paint to calculate the bounds of the remove button.
	 * This is crucial for the MouseListener on the JList to determine if a click/hover was on the
	 * button.
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		// Ensure the button's layout is valid before getting bounds
		removeProjectBtn.doLayout();
		// Calculate bounds of the remove button relative to this renderer panel
		int x = getWidth() - removeProjectBtn.getWidth() - getBorder().getBorderInsets(this).right;
		int y = (getHeight() - removeProjectBtn.getHeight()) / 2;
		removeIconBounds = new Rectangle(x, y, removeProjectBtn.getWidth(), removeProjectBtn.getHeight());
	}

	/**
	 * Returns the bounds of the remove button within the renderer component's coordinate system.
	 * This is crucial for the MouseListener on the JList to determine if a click was on the icon.
	 *
	 * @return Rectangle representing the bounds of the remove icon.
	 */
	public Rectangle getRemoveIconBounds() {
		return removeIconBounds;
	}
}
