package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import org.apache.commons.lang3.exception.ExceptionUtils;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.elf.ElfParser;
import jadx.gui.utils.elf.JElfParser;
import jadx.gui.utils.elf.Symbol;

/**
 * A ContentPanel extending BinaryContentPanel to show information regarding symbols in a loaded ELF
 * file.
 */
public class LibraryContentPanel extends BinaryContentPanel {

	private final transient JTextArea elfDetailsPane;
	private final transient JSplitPane splitPane;
	private final transient JScrollPane elfScrollPane;

	public LibraryContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);

		splitPane = new JSplitPane();

		elfDetailsPane = new JTextArea(NLS.str("elf_details.loading") + "...\t\t\t\t\t");
		elfDetailsPane.setEditable(false);
		elfDetailsPane.setFont(panel.getMainWindow().getSettings().getCodeFont());

		elfScrollPane = new JScrollPane(elfDetailsPane);

		splitPane.setLeftComponent(hexPreviewPanel);
		splitPane.setRightComponent(elfScrollPane);
		splitPane.setResizeWeight(1.0); // allocate as much space to the hexPreview as is possible

		add(splitPane, BorderLayout.CENTER);
	}

	@Override
	public void loadData() {
		super.loadData();

		// run potentially-lengthy elf parsing as a Background Task, avoiding blocking the UI thread (since
		// loadData explciitly runs on the UI thread).
		tabbedPane.getMainWindow().getBackgroundExecutor().execute(NLS.str("elf_details.loading"), () -> {
			String elfText = "";

			ElfParser parser = new JElfParser();

			try {
				parser.parse(getNodeData());

				elfText += NLS.str("elf_details.abi") + ": \t" + parser.getABI() + "\n";
				elfText += NLS.str("elf_details.type") + ": \t" + parser.getType() + "\n";
				elfText += NLS.str("elf_details.architecture") + ": \t" + parser.getArchitecture() + "\n\n";

				elfText += NLS.str("elf_details.symbols") + ": \n";
				List<Symbol> syms = parser.getSymbols();
				for (Symbol symbol : syms) {
					elfText += symbol.getTypeString() + "\t"
							+ symbol.getBindingString() + "\t"
							+ symbol.getName() + "\n";
				}

				// java requires variables used in lambdas to be final
				final String elfTextFinal = elfText;
				UiUtils.uiRunAndWait(() -> {
					elfDetailsPane.setText(elfTextFinal);
					elfDetailsPane.setCaretPosition(0); // ensure the view is scrolled to the top
				});
			} catch (Exception e) {
				UiUtils.uiRunAndWait(() -> {
					elfDetailsPane.setText(NLS.str("elf_details.errored") + "\n\n" + ExceptionUtils.getStackTrace(e));
					elfDetailsPane.setCaretPosition(0);
				});
			}
		});

	}

}
