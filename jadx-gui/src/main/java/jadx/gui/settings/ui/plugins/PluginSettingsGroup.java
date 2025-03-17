package jadx.gui.settings.ui.plugins;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
import jadx.gui.utils.Link;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.plugins.tools.JadxPluginsList;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;

class PluginSettingsGroup implements ISettingsGroup {
	private static final Logger LOG = LoggerFactory.getLogger(PluginSettingsGroup.class);

	private final PluginSettings pluginsSettings;
	private final MainWindow mainWindow;
	private final String title;
	private final List<ISettingsGroup> subGroups = new ArrayList<>();
	private final List<PluginContext> collectedPlugins;

	private JPanel detailsPanel;

	public PluginSettingsGroup(PluginSettings pluginSettings, MainWindow mainWindow, List<PluginContext> collectedPlugins) {
		this.pluginsSettings = pluginSettings;
		this.mainWindow = mainWindow;
		this.title = NLS.str("preferences.plugins");
		this.collectedPlugins = collectedPlugins;
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

		DefaultListModel<BasePluginListNode> listModel = new DefaultListModel<>();
		JList<BasePluginListNode> pluginList = new JList<>(listModel);
		pluginList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pluginList.setCellRenderer(new PluginsListCellRenderer());
		pluginList.addListSelectionListener(ev -> onSelection(pluginList.getSelectedValue()));
		pluginList.setFocusable(true);

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

		applyData(listModel);
		return mainPanel;
	}

	private void applyData(DefaultListModel<BasePluginListNode> listModel) {
		List<JadxPluginMetadata> installed = JadxPluginsTools.getInstance().getInstalled();
		List<BasePluginListNode> nodes = new ArrayList<>(installed.size() + collectedPlugins.size());
		Set<String> installedSet = new HashSet<>(installed.size());
		for (JadxPluginMetadata pluginMetadata : installed) {
			installedSet.add(pluginMetadata.getPluginId());
			nodes.add(new InstalledPluginNode(pluginMetadata));
		}
		for (PluginContext plugin : collectedPlugins) {
			if (!installedSet.contains(plugin.getPluginId())) {
				nodes.add(new LoadedPluginNode(plugin));
			}
		}
		nodes.sort(Comparator.comparing(BasePluginListNode::getTitle));

		fillListModel(listModel, nodes, Collections.emptyList());
		loadAvailablePlugins(listModel, nodes, installedSet);
	}

	private static void fillListModel(DefaultListModel<BasePluginListNode> listModel,
			List<BasePluginListNode> nodes, List<AvailablePluginNode> available) {
		listModel.clear();
		listModel.addElement(new TitleNode("Installed"));
		nodes.stream().filter(n -> n.getAction() == PluginAction.UNINSTALL).forEach(listModel::addElement);
		listModel.addElement(new TitleNode("Available"));
		listModel.addAll(available);
		listModel.addElement(new TitleNode("Bundled"));
		nodes.stream().filter(n -> n.getAction() == PluginAction.NONE).forEach(listModel::addElement);
	}

	private void loadAvailablePlugins(DefaultListModel<BasePluginListNode> listModel,
			List<BasePluginListNode> nodes, Set<String> installedSet) {
		mainWindow.getBackgroundExecutor().execute(
				NLS.str("preferences.plugins.task.downloading_list"),
				() -> {
					try {
						JadxPluginsList.getInstance().get(availablePlugins -> {
							List<AvailablePluginNode> availableNodes = availablePlugins.stream()
									.filter(availablePlugin -> !installedSet.contains(availablePlugin.getPluginId()))
									.map(AvailablePluginNode::new)
									.collect(Collectors.toList());
							UiUtils.uiRunAndWait(() -> fillListModel(listModel, nodes, availableNodes));
						});
					} catch (Exception e) {
						LOG.warn("Failed to load available plugins list", e);
					}
				});
	}

	private void onSelection(BasePluginListNode node) {
		detailsPanel.removeAll();
		if (node.hasDetails()) {
			JLabel nameLbl = new JLabel(node.getTitle());
			Font baseFont = nameLbl.getFont();
			nameLbl.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 2));

			JLabel homeLink = null;
			String homepage = node.getHomepage();
			if (StringUtils.notBlank(homepage)) {
				homeLink = new Link("Homepage: " + homepage, homepage);
				homeLink.setHorizontalAlignment(SwingConstants.LEFT);
			}

			JTextPane descArea = new JTextPane();
			descArea.setText(node.getDescription());
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
			if (node.getAction() == PluginAction.UNINSTALL) {
				// TODO: allow disable bundled plugins
				boolean disabled = node.isDisabled();
				String statusChangeLabel = disabled
						? NLS.str("preferences.plugins.enable_btn")
						: NLS.str("preferences.plugins.disable_btn");
				JButton statusBtn = new JButton(statusChangeLabel);
				statusBtn.addActionListener(ev -> pluginsSettings.changeDisableStatus(node.getPluginId(), !disabled));
				top.add(Box.createHorizontalStrut(10));
				top.add(statusBtn);
			}

			JPanel center = new JPanel();
			center.setLayout(new BoxLayout(center, BoxLayout.PAGE_AXIS));
			center.setBorder(BorderFactory.createEmptyBorder(10, 2, 10, 2));
			center.add(descArea);
			if (homeLink != null) {
				JPanel link = new JPanel();
				link.setLayout(new BoxLayout(link, BoxLayout.LINE_AXIS));
				link.add(homeLink);
				link.add(Box.createHorizontalGlue());
				center.add(link);
			}
			center.add(Box.createVerticalGlue());

			detailsPanel.add(top, BorderLayout.PAGE_START);
			detailsPanel.add(center, BorderLayout.CENTER);
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
			versionLbl.setPreferredSize(new Dimension(40, 10));

			panel.add(nameLbl);
			panel.add(Box.createHorizontalStrut(20));
			panel.add(Box.createHorizontalGlue());
			panel.add(versionLbl);
			panel.add(Box.createHorizontalStrut(10));

			titleLbl = new JLabel();
			titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
			titleLbl.setPreferredSize(new Dimension(40, 10));
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends BasePluginListNode> list,
				BasePluginListNode plugin, int index, boolean isSelected, boolean cellHasFocus) {
			if (!plugin.hasDetails()) {
				titleLbl.setText(plugin.getTitle());
				return titleLbl;
			}
			nameLbl.setText(plugin.getTitle());
			nameLbl.setToolTipText(plugin.getLocationId());
			versionLbl.setText(Utils.getOrElse(plugin.getVersion(), ""));
			panel.getAccessibleContext().setAccessibleName(plugin.getTitle());

			boolean enabled = !plugin.isDisabled();
			nameLbl.setEnabled(enabled);
			versionLbl.setEnabled(enabled);

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
