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
import java.util.Set;
import java.util.stream.Collectors;

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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.core.plugins.PluginContext;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.plugins.tools.JadxPluginsList;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;

class PluginSettingsGroup implements ISettingsGroup {
	private static final Logger LOG = LoggerFactory.getLogger(PluginSettingsGroup.class);

	private final PluginSettings pluginsSettings;
	private final MainWindow mainWindow;
	private final String title;
	private final List<ISettingsGroup> subGroups = new ArrayList<>();
	private final List<PluginContext> installedPlugins;

	private JPanel detailsPanel;

	public PluginSettingsGroup(PluginSettings pluginSettings, MainWindow mainWindow, List<PluginContext> installedPlugins) {
		this.pluginsSettings = pluginSettings;
		this.mainWindow = mainWindow;
		this.title = NLS.str("preferences.plugins");
		this.installedPlugins = installedPlugins;
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
		for (PluginContext plugin : installedPlugins) {
			nodes.add(new InstalledPluginNode(plugin, installedMap.get(plugin.getPluginId())));
		}
		nodes.sort(Comparator.comparing(BasePluginListNode::getTitle));

		DefaultListModel<BasePluginListNode> listModel = new DefaultListModel<>();
		listModel.addElement(new TitleNode("Installed"));
		nodes.stream().filter(n -> n.getVersion() != null).forEach(listModel::addElement);
		listModel.addElement(new TitleNode("Bundled"));
		nodes.stream().filter(n -> n.getVersion() == null).forEach(listModel::addElement);
		listModel.addElement(new TitleNode("Available"));

		JList<BasePluginListNode> pluginList = new JList<>(listModel);
		pluginList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pluginList.setCellRenderer(new PluginsListCellRenderer());
		pluginList.addListSelectionListener(ev -> onSelection(pluginList.getSelectedValue()));

		loadAvailablePlugins(listModel, installedPlugins);

		JScrollPane scrollPane = new JScrollPane(pluginList);
		scrollPane.setMinimumSize(new Dimension(80, 120));

		detailsPanel = new JPanel(new BorderLayout(5, 5));
		detailsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(NLS.str("preferences.plugins.details")),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.PAGE_AXIS));

		JSplitPane splitPanel = new JSplitPane();
		splitPanel.setBorder(BorderFactory.createEmptyBorder(10, 2, 2, 2));
		splitPanel.setLeftComponent(scrollPane);
		splitPanel.setRightComponent(detailsPanel);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(5, 5));
		mainPanel.setBorder(BorderFactory.createTitledBorder(title));
		mainPanel.add(actionsPanel, BorderLayout.PAGE_START);
		mainPanel.add(splitPanel, BorderLayout.CENTER);
		return mainPanel;
	}

	private void loadAvailablePlugins(DefaultListModel<BasePluginListNode> listModel, List<PluginContext> installedPlugins) {
		List<AvailablePluginNode> list = new ArrayList<>();
		mainWindow.getBackgroundExecutor().execute(
				NLS.str("preferences.plugins.task.downloading_list"),
				() -> {
					List<JadxPluginMetadata> availablePlugins;
					try {
						availablePlugins = JadxPluginsList.getInstance().fetch();
					} catch (Exception e) {
						LOG.warn("Failed to load available plugins list", e);
						return;
					}
					Set<String> installed = installedPlugins.stream().map(PluginContext::getPluginId).collect(Collectors.toSet());
					for (JadxPluginMetadata availablePlugin : availablePlugins) {
						if (!installed.contains(availablePlugin.getPluginId())) {
							list.add(new AvailablePluginNode(availablePlugin));
						}
					}
				},
				status -> listModel.addAll(list));
	}

	private void onSelection(BasePluginListNode node) {
		detailsPanel.removeAll();
		if (node.hasDetails()) {
			JLabel nameLbl = new JLabel(node.getTitle());
			Font baseFont = nameLbl.getFont();
			nameLbl.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 2));

			String desc;
			String homepage = node.getHomepage();
			if (StringUtils.notBlank(homepage)) {
				desc = node.getDescription() + "\n\nHomepage: " + homepage;
			} else {
				desc = node.getDescription();
			}

			JTextPane descArea = new JTextPane();
			descArea.setText(desc);
			descArea.setFont(baseFont.deriveFont(baseFont.getSize2D() + 1));
			descArea.setEditable(false);
			descArea.setBorder(BorderFactory.createEmptyBorder());
			descArea.setOpaque(true);

			JPanel top = new JPanel();
			top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
			top.setBorder(BorderFactory.createEmptyBorder(10, 2, 10, 2));
			top.add(nameLbl);
			top.add(Box.createHorizontalGlue());
			JButton actionBtn = makeActionButton(node);
			if (actionBtn != null) {
				top.add(actionBtn);
			}
			detailsPanel.add(top, BorderLayout.PAGE_START);
			detailsPanel.add(descArea, BorderLayout.CENTER);
		}
		detailsPanel.updateUI();
	}

	private @Nullable JButton makeActionButton(BasePluginListNode node) {
		switch (node.getAction()) {
			case NONE:
				return null;
			case INSTALL: {
				JButton installBtn = new JButton(NLS.str("preferences.plugins.install_btn"));
				installBtn.addActionListener(ev -> pluginsSettings.install(node.getLocationId()));
				return installBtn;
			}
			case UNINSTALL: {
				JButton uninstallBtn = new JButton(NLS.str("preferences.plugins.uninstall_btn"));
				uninstallBtn.addActionListener(ev -> pluginsSettings.uninstall(node.getPluginId()));
				return uninstallBtn;
			}
		}
		return null;
	}

	private static class PluginsListCellRenderer implements ListCellRenderer<BasePluginListNode> {
		private final JPanel panel;
		private final JLabel nameLbl;
		private final JLabel versionLbl;
		private final JLabel titleLbl;

		public PluginsListCellRenderer() {
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
			panel.add(Box.createHorizontalStrut(20));
			panel.add(Box.createHorizontalGlue());
			panel.add(versionLbl);

			titleLbl = new JLabel();
			titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
			titleLbl.setEnabled(false);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends BasePluginListNode> list,
				BasePluginListNode value, int index, boolean isSelected, boolean cellHasFocus) {
			if (!value.hasDetails()) {
				titleLbl.setText(value.getTitle());
				return titleLbl;
			}
			nameLbl.setText(value.getTitle());
			nameLbl.setToolTipText(value.getLocationId());
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
