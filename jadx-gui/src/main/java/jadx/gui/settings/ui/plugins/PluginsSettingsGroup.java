package jadx.gui.settings.ui.plugins;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.core.plugins.PluginContext;
import jadx.core.utils.Utils;
import jadx.gui.utils.NLS;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;

class PluginsSettingsGroup implements ISettingsGroup {
	private final PluginsSettings pluginsSettings;
	private final String title;
	private final List<ISettingsGroup> subGroups = new ArrayList<>();
	private final List<PluginContext> pluginsList;

	private PluginListNode selectedPlugin;
	private JPanel detailsPanel;

	public PluginsSettingsGroup(PluginsSettings pluginsSettings, List<PluginContext> pluginsList) {
		this.pluginsSettings = pluginsSettings;
		this.title = NLS.str("preferences.plugins");
		this.pluginsList = pluginsList;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public List<ISettingsGroup> getSubGroups() {
		return subGroups;
	}

	@Override
	public JComponent buildComponent() {
		// lazy load main page
		return buildMainSettingsPage();
	}

	private JPanel buildMainSettingsPage() {
		JButton installPluginBtn = new JButton(NLS.str("preferences.plugins.install"));
		installPluginBtn.addActionListener(ev -> pluginsSettings.addPlugin());

		JButton updateAllBtn = new JButton(NLS.str("preferences.plugins.update_all"));
		updateAllBtn.addActionListener(ev -> pluginsSettings.updateAll());

		JPanel actionsPanel = new JPanel();
		actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.LINE_AXIS));
		actionsPanel.add(installPluginBtn);
		actionsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		actionsPanel.add(updateAllBtn);

		List<JadxPluginMetadata> installed = JadxPluginsTools.getInstance().getInstalled();
		Map<String, JadxPluginMetadata> installedMap = new HashMap<>(installed.size());
		installed.forEach(p -> installedMap.put(p.getPluginId(), p));

		List<BasePluginListNode> nodes = new ArrayList<>(installed.size() + 3);
		for (PluginContext plugin : pluginsList) {
			nodes.add(new PluginListNode(plugin, installedMap.get(plugin.getPluginId())));
		}
		nodes.sort(Comparator.comparing(n -> n.getPluginInfo().getName()));

		DefaultListModel<BasePluginListNode> listModel = new DefaultListModel<>();
		listModel.addElement(new TitleNode("Installed"));
		nodes.stream().filter(n -> n.getVersion() != null).forEach(listModel::addElement);
		listModel.addElement(new TitleNode("Bundled"));
		nodes.stream().filter(n -> n.getVersion() == null).forEach(listModel::addElement);
		// TODO: load external plugins list
		// listModel.addElement(new TitleNode("Available"));

		JList<BasePluginListNode> pluginsList = new JList<>(listModel);
		pluginsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pluginsList.setCellRenderer(new PluginsListCellRenderer(pluginsList));
		pluginsList.addListSelectionListener(ev -> onSelection(pluginsList.getSelectedValue()));

		JScrollPane scrollPane = new JScrollPane(pluginsList);

		detailsPanel = new JPanel(new BorderLayout(5, 5));
		detailsPanel.setBorder(BorderFactory.createTitledBorder("Plugin details"));
		detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.PAGE_AXIS));
		detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JSplitPane splitPanel = new JSplitPane();
		splitPanel.setBorder(BorderFactory.createEmptyBorder(10, 2, 2, 2));
		splitPanel.setLeftComponent(scrollPane);
		splitPanel.setRightComponent(detailsPanel);
		splitPanel.setDividerLocation(0.4);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(5, 5));
		mainPanel.setBorder(BorderFactory.createTitledBorder(title));
		mainPanel.add(actionsPanel, BorderLayout.PAGE_START);
		mainPanel.add(splitPanel, BorderLayout.CENTER);
		return mainPanel;
	}

	private void onSelection(BasePluginListNode node) {
		detailsPanel.removeAll();
		JadxPluginInfo pluginInfo = node.getPluginInfo();
		if (pluginInfo != null) {
			JButton uninstallBtn = new JButton("Uninstall");
			if (node.getVersion() != null) {
				uninstallBtn.addActionListener(ev -> pluginsSettings.uninstall(pluginInfo.getPluginId()));
			} else {
				uninstallBtn.setEnabled(false);
			}
			JLabel nameLbl = new JLabel(pluginInfo.getName());
			Font baseFont = nameLbl.getFont();
			nameLbl.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 2));

			JTextPane descArea = new JTextPane();
			descArea.setText(pluginInfo.getDescription());
			descArea.setFont(baseFont.deriveFont(baseFont.getSize2D() + 1));
			descArea.setEditable(false);
			descArea.setBorder(BorderFactory.createEmptyBorder());
			descArea.setOpaque(true);

			JPanel top = new JPanel();
			top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
			top.setBorder(BorderFactory.createEmptyBorder(10, 2, 10, 2));
			top.add(nameLbl);
			top.add(Box.createHorizontalGlue());
			top.add(uninstallBtn);

			detailsPanel.add(top, BorderLayout.PAGE_START);
			detailsPanel.add(descArea, BorderLayout.CENTER);
		}
		detailsPanel.updateUI();
	}

	private static class PluginsListCellRenderer implements ListCellRenderer<BasePluginListNode> {
		private final JPanel panel;
		private final JLabel nameLbl;
		private final JLabel versionLbl;
		private final JLabel titleLbl;

		public PluginsListCellRenderer(JList<BasePluginListNode> pluginsList) {
			panel = new JPanel();
			panel.setOpaque(true);
			panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
			panel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

			nameLbl = new JLabel("");
			nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD));
			nameLbl.setOpaque(true);
			versionLbl = new JLabel("");
			versionLbl.setOpaque(true);

			panel.add(nameLbl);
			panel.add(Box.createHorizontalGlue());
			panel.add(versionLbl);

			titleLbl = new JLabel();
			titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
			titleLbl.setEnabled(false);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends BasePluginListNode> list,
				BasePluginListNode value, int index, boolean isSelected, boolean cellHasFocus) {
			String title = value.getTitle();
			if (title != null) {
				titleLbl.setText(title);
				return titleLbl;
			}
			nameLbl.setText(value.getPluginInfo().getName());
			nameLbl.setToolTipText(value.getPluginInfo().getDescription());
			versionLbl.setText(Utils.getOrElse(value.getVersion(), ""));
			if (isSelected) {
				panel.setBackground(list.getSelectionBackground());
				nameLbl.setBackground(list.getSelectionBackground());
				nameLbl.setForeground(list.getSelectionForeground());
				versionLbl.setBackground(list.getSelectionBackground());
			} else {
				panel.setBackground(list.getBackground());
				nameLbl.setBackground(list.getBackground());
				nameLbl.setForeground(list.getForeground());
				versionLbl.setBackground(list.getBackground());
			}
			return panel;
		}
	}
}
