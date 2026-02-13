package jadx.gui.ui.dialog;

import java.io.File;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import jadx.api.JavaMethod;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.DotGraphUtils;
import jadx.gui.treemodel.JMethod;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class ControlFlowGraphDialog extends GraphDialog {

	private static final long serialVersionUID = -68749445239697710L;

	public ControlFlowGraphDialog(MainWindow mainWindow, String method) {
		super(mainWindow, String.format("%s: %s", NLS.str("graph_viewer.cfg.title"), method));
	}

	public static void open(MainWindow window, JMethod method, boolean useRegions, boolean rawInsn) {

		JavaMethod javaMethod = method.getJavaMethod();

		GraphDialog graphDialog = new ControlFlowGraphDialog(window, DotGraphUtils.methodFormatName(javaMethod, false));
		graphDialog.addMenuBar();
		graphDialog.setVisible(true);

		SwingUtilities.invokeLater(() -> {
			String graph = generateGraph(javaMethod, useRegions, rawInsn);
			if (graph != null) {
				graphDialog.getPanel().setGraph(graph);
			} else {
				graphDialog.getPanel().invalidateImage(graphError(NLS.str("graph_viewer.file_not_found_error")));
			}
		});
	}

	private static String generateGraph(JavaMethod javaMethod, boolean useRegions, boolean rawInsn) {
		MethodNode mth = javaMethod.getMethodNode();
		File file = new DotGraphUtils(useRegions, rawInsn).getFullFile(mth);

		try (Scanner reader = new Scanner(file)) {
			String contents = reader.useDelimiter("\\Z").next();
			return contents;
		} catch (Exception e) {
			return null;
		}
	}
}
