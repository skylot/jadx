package jadx.gui.settings.ui.cache;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.events.types.ReloadProject;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.gui.cache.manager.CacheManager;
import jadx.gui.settings.JadxProject;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.MousePressedHandler;

public class CachesTable extends JTable {
	private static final long serialVersionUID = 5984107298264276049L;

	private static final Logger LOG = LoggerFactory.getLogger(CachesTable.class);

	private final MainWindow mainWindow;
	private final CachesTableModel dataModel;

	public CachesTable(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.dataModel = new CachesTableModel();
		setModel(dataModel);
		setDefaultRenderer(Object.class, new CachesTableRenderer());

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setShowHorizontalLines(true);
		setDragEnabled(false);
		setColumnSelectionAllowed(false);
		setAutoscrolls(true);
		setFocusable(false);

		addMouseListener(new MousePressedHandler(ev -> {
			int row = rowAtPoint(ev.getPoint());
			if (row != -1) {
				dataModel.changeSelection(row);
				UiUtils.uiRun(this::updateUI);
			}
		}));
	}

	public void updateData() {
		List<TableRow> rows = mainWindow.getCacheManager().getCachesList().stream()
				.map(TableRow::new)
				.collect(Collectors.toList());
		updateRows(rows);
	}

	public void reloadData() {
		Map<String, String> prevUsageMap = dataModel.getRows().stream()
				.collect(Collectors.toMap(TableRow::getProject, TableRow::getUsage));

		List<TableRow> rows = mainWindow.getCacheManager().getCachesList().stream()
				.map(TableRow::new)
				.peek(r -> r.setUsage(Utils.getOrElse(prevUsageMap.get(r.getProject()), "-")))
				.collect(Collectors.toList());
		updateRows(rows);
	}

	private void updateRows(List<TableRow> rows) {
		dataModel.setRows(rows);

		// fix allocated space for default 20 rows
		int width = getPreferredSize().width;
		int height = rows.size() * getRowHeight();
		setPreferredScrollableViewportSize(new Dimension(width, height));

		UiUtils.uiRun(this::updateUI);
	}

	public void updateSizes() {
		List<Runnable> list = dataModel.getRows().stream()
				.map(row -> (Runnable) () -> calcSize(row))
				.collect(Collectors.toList());
		mainWindow.getBackgroundExecutor().execute(
				NLS.str("preferences.cache.task.usage"),
				list,
				status -> updateUI());
	}

	private void calcSize(TableRow row) {
		String cacheDir = row.getCacheEntry().getCache();
		try {
			Path dir = Paths.get(cacheDir);
			if (Files.isDirectory(dir)) {
				long size = calcSizeOfDirectory(dir);
				row.setUsage(FileUtils.byteCountToDisplaySize(size));
			} else {
				row.setUsage("not found");
			}
		} catch (Exception e) {
			LOG.warn("Failed to calculate size of directory: {}", cacheDir, e);
			row.setUsage("error");
		}
	}

	private static long calcSizeOfDirectory(Path dir) {
		try (Stream<Path> stream = Files.walk(dir)) {
			long blockSize = Files.getFileStore(dir).getBlockSize();
			return stream.mapToLong(p -> {
				if (Files.isRegularFile(p)) {
					try {
						long fileSize = Files.size(p);
						// ceil round to blockSize
						return (fileSize / blockSize + 1L) * blockSize;
					} catch (Exception e) {
						LOG.error("Failed to get file size: {}", p, e);
					}
				}
				return 0;
			}).sum();
		} catch (Exception e) {
			LOG.error("Failed to calculate directory size: {}", dir, e);
			return 0;
		}
	}

	public void deleteSelected() {
		delete(ListUtils.filter(dataModel.getRows(), TableRow::isSelected));
	}

	public void deleteAll() {
		delete(dataModel.getRows());
	}

	private void delete(List<TableRow> rows) {
		// force reload if cache for current project is deleted
		boolean reload = searchCurrentProject(rows);

		List<Runnable> list = rows.stream()
				.map(TableRow::getCacheEntry)
				.map(entry -> (Runnable) () -> mainWindow.getCacheManager().removeCacheEntry(entry))
				.collect(Collectors.toList());
		mainWindow.getBackgroundExecutor().execute(
				NLS.str("preferences.cache.task.delete"),
				list,
				status -> {
					reloadData();
					if (reload) {
						mainWindow.events().send(ReloadProject.EVENT);
					}
				});
	}

	private boolean searchCurrentProject(List<TableRow> rows) {
		JadxProject project = mainWindow.getProject();
		if (!project.getFilePaths().isEmpty()) {
			String cacheStr = CacheManager.pathToString(project.getCacheDir());
			for (TableRow row : rows) {
				if (row.getCacheEntry().getCache().equals(cacheStr)) {
					project.resetCacheDir();
					LOG.debug("Found current project in cache delete list -> request full reload");
					return true;
				}
			}
		}
		return false;
	}
}
