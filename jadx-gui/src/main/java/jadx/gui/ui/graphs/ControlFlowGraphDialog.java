package jadx.gui.ui.graphs;

import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.DotGraphUtils;
import jadx.gui.treemodel.JMethod;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;

public class ControlFlowGraphDialog extends GraphDialog {
	private static final long serialVersionUID = -68749445239697710L;

	public static void open(MainWindow window, JMethod jMth) {
		ControlFlowGraphDialog graphDialog = new ControlFlowGraphDialog(window, jMth);
		graphDialog.addMenuBar();
		graphDialog.setVisible(true);
		graphDialog.usePreset(GraphPreset.NORMAL);
	}

	private static final String[] PRESETS_NSL = NLS.str("graph_viewer.cfg.preset_names").split("\\|");

	private enum GraphPreset {
		RAW(PRESETS_NSL[0], true, false, "SSATransform"),
		NORMAL(PRESETS_NSL[1], false, false, "RegionMakerVisitor"),
		REGION(PRESETS_NSL[2], false, true, "PrepareForCodeGen");

		final boolean useRawInsns;
		final boolean useRegions;
		final String beforePass;
		final String nlsStr;

		GraphPreset(String nlsStr, boolean useRawInsns, boolean useRegions, String beforePass) {
			this.nlsStr = nlsStr;
			this.useRawInsns = useRawInsns;
			this.useRegions = useRegions;
			this.beforePass = beforePass;
		}

		@Override
		public String toString() {
			return nlsStr;
		}
	}

	private final MethodNode mth;
	private @Nullable GraphPreset graphPreset;
	private JComboBox<GraphPreset> presetsCB;
	private JComboBox<String> passesCB;
	private String[] passNames;
	private int currentPassIdx;

	private ControlFlowGraphDialog(MainWindow mainWindow, JMethod jMth) {
		super(mainWindow);
		mth = jMth.getJavaMethod().getMethodNode();
		String mthName = DotGraphUtils.methodFormatName(jMth.getJavaMethod(), false);
		setTitle(String.format("%s: %s", NLS.str("graph_viewer.cfg.title"), mthName));
	}

	private void usePreset(@Nullable GraphPreset graphPreset) {
		if (graphPreset == null || this.graphPreset == graphPreset) {
			return;
		}
		this.graphPreset = graphPreset;
		this.currentPassIdx = selectPassBefore(graphPreset.beforePass);
		presetsCB.setSelectedItem(graphPreset);
		passesCB.setSelectedItem(passNames[currentPassIdx]);
		reloadGraph();
	}

	private int selectPassBefore(String beforePass) {
		List<IDexTreeVisitor> passes = getPassList();
		for (int i = 1, passesSize = passes.size(); i < passesSize; i++) {
			if (passes.get(i).getName().equals(beforePass)) {
				return i - 1;
			}
		}
		return passes.size() - 1;
	}

	@Override
	public JMenuBar addMenuBar() {
		JMenuBar menuBar = super.addMenuBar();
		presetsCB = new JComboBox<>(GraphPreset.values());
		presetsCB.addActionListener(e -> usePreset((GraphPreset) presetsCB.getSelectedItem()));

		List<IDexTreeVisitor> passList = getPassList();
		int size = passList.size();
		Map<String, Integer> passMap = new HashMap<>();
		passNames = new String[size];
		for (int i = 0; i < size; i++) {
			IDexTreeVisitor pass = passList.get(i);
			String name = i + ": " + pass.getName();
			passMap.put(name, i);
			passNames[i] = name;
		}
		passesCB = new JComboBox<>(passNames);
		passesCB.addActionListener(e -> {
			String newValue = (String) passesCB.getSelectedItem();
			if (newValue != null) {
				Integer newIdx = passMap.get(newValue);
				if (newIdx != null && newIdx != currentPassIdx) {
					currentPassIdx = newIdx;
					reloadGraph();
				}
			}
		});

		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		menuBarPanel.add(new JLabel(NLS.str("graph_viewer.cfg.preset_selector_label")));
		menuBarPanel.add(presetsCB);
		menuBarPanel.add(Box.createHorizontalBox());
		menuBarPanel.add(new JLabel(NLS.str("graph_viewer.cfg.pass_selector_label")));
		menuBarPanel.add(passesCB);
		menuBar.add(menuBarPanel);
		return menuBar;
	}

	@Override
	protected void disableMenu() {
		// don't disable menu if invalid combination of insn type and pass selected
	}

	private void reloadGraph() {
		UiUtils.uiRun(() -> {
			String graph = generateGraph();
			if (graph != null) {
				getPanel().setGraph(graph);
			} else {
				getPanel().invalidateImage(graphError(NLS.str("graph_viewer.default_error")));
			}
		});
	}

	private @Nullable String generateGraph() {
		GraphPreset preset = graphPreset;
		if (preset == null) {
			return null;
		}
		try {
			IDexTreeVisitor pass = getPassList().get(currentPassIdx);
			boolean success = mth.root().getProcessClasses().processMethodToVisitor(mth, pass);
			if (!success) {
				return null;
			}
			return new DotGraphUtils(preset.useRegions, preset.useRawInsns).dumpToString(mth);
		} finally {
			mth.unload();
		}
	}

	private List<IDexTreeVisitor> getPassList() {
		return mth.root().getProcessClasses().getPasses();
	}
}
