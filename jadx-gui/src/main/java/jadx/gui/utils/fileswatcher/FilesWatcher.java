package jadx.gui.utils.fileswatcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.Utils;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class FilesWatcher {
	private static final Logger LOG = LoggerFactory.getLogger(FilesWatcher.class);

	private final WatchService watcher = FileSystems.getDefault().newWatchService();
	private final Map<WatchKey, Path> keys = new HashMap<>();
	private final Map<Path, Set<Path>> files = new HashMap<>();
	private final AtomicBoolean cancel = new AtomicBoolean(false);
	private final BiConsumer<Path, WatchEvent.Kind<Path>> listener;

	public FilesWatcher(List<Path> paths, BiConsumer<Path, WatchEvent.Kind<Path>> listener) throws IOException {
		this.listener = listener;
		for (Path path : paths) {
			if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
				registerDirs(path);
			} else {
				Path parentDir = path.toAbsolutePath().getParent();
				register(parentDir);
				files.merge(parentDir, Collections.singleton(path), Utils::mergeSets);
			}
		}
	}

	public void cancel() {
		cancel.set(true);
	}

	@SuppressWarnings("unchecked")
	public void watch() {
		cancel.set(false);
		LOG.debug("File watcher started for {} dirs", keys.size());
		while (!cancel.get()) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				LOG.debug("File watcher interrupted");
				return;
			}
			Path dir = keys.get(key);
			if (dir == null) {
				LOG.warn("Unknown directory key: {}", key);
				continue;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				if (cancel.get() || Thread.interrupted()) {
					return;
				}
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == OVERFLOW) {
					continue;
				}
				Path fileName = ((WatchEvent<Path>) event).context();
				Path path = dir.resolve(fileName);

				Set<Path> files = this.files.get(dir);
				if (files == null || files.contains(path)) {
					listener.accept(path, (WatchEvent.Kind<Path>) kind);
				}
				if (kind == ENTRY_CREATE) {
					try {
						if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
							registerDirs(path);
						}
					} catch (Exception e) {
						LOG.warn("Failed to update directory watch: {}", path, e);
					}
				}
			}
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);
				if (keys.isEmpty()) {
					LOG.debug("File watcher stopped: all watch keys removed");
					return;
				}
			}
		}
	}

	private void registerDirs(Path start) throws IOException {
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		keys.put(key, dir);
	}
}
