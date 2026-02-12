package jadx.gui.utils.plugins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.plugins.context.ITreeInputCategory;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

public class TreeInputsHelper {
	private static final Logger LOG = LoggerFactory.getLogger(TreeInputsHelper.class);

	private final List<CategoryData> categoryData;
	private List<Path> simpleFiles;

	public TreeInputsHelper(MainWindow mainWindow) {
		categoryData = mainWindow.getWrapper().getGuiPluginsContext()
				.getTreeInputCategories()
				.stream()
				.map(CategoryData::new)
				.collect(Collectors.toList());
	}

	public void processInputs(List<Path> files) {
		simpleFiles = new ArrayList<>(files.size());
		for (Path file : files) {
			boolean added = false;
			for (CategoryData data : categoryData) {
				if (data.filesFilter(file)) {
					added = true;
					break;
				}
			}
			if (!added) {
				simpleFiles.add(file);
			}
		}
	}

	public List<JNode> getCustomNodes() {
		return categoryData.stream()
				.filter(CategoryData::notEmpty)
				.map(CategoryData::buildInputNode)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public List<Path> getSimpleFiles() {
		return simpleFiles;
	}

	private static final class CategoryData {
		private final ITreeInputCategory provider;
		private final List<Path> collectedFiles = new ArrayList<>();

		private CategoryData(ITreeInputCategory provider) {
			this.provider = provider;
		}

		public boolean filesFilter(Path file) {
			try {
				if (provider.filesFilter(file)) {
					collectedFiles.add(file);
					return true;
				}
			} catch (Exception e) {
				LOG.error("Failed to filter input files", e);
			}
			return false;
		}

		public @Nullable JNode buildInputNode() {
			try {
				return provider.buildInputNode(collectedFiles);
			} catch (Exception e) {
				LOG.error("Failed to build custom input node", e);
				return null;
			}
		}

		public boolean notEmpty() {
			return !collectedFiles.isEmpty();
		}
	}
}
