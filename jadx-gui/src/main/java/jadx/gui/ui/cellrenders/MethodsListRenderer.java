package jadx.gui.ui.cellrenders;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import jadx.api.JavaMethod;
import jadx.gui.utils.UiUtils;

public class MethodsListRenderer extends JPanel implements ListCellRenderer<JavaMethod> {
	private final JCheckBox checkBox;
	private final JLabel label;

	public MethodsListRenderer() {
		setLayout(new BorderLayout(5, 0));
		checkBox = new JCheckBox();
		label = new JLabel();

		setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));

		add(checkBox, BorderLayout.WEST);
		add(label, BorderLayout.CENTER);

		setOpaque(true);

		checkBox.setOpaque(false);
		label.setOpaque(false);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends JavaMethod> list,
			JavaMethod value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {
		label.setText(UiUtils.typeFormatHtml(MethodRenderHelper.makeBaseString(value), value.getReturnType()));
		label.setIcon(MethodRenderHelper.getIcon(value));

		checkBox.setSelected(isSelected);

		setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
		setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
		label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

		return this;
	}
}
