package jadx.api.gui;

import java.nio.file.Path;
import java.util.List;

/**
 * Main access point to all Jadx-GUI objects and services
 */
public interface IMainWindow {
	void open(List<Path> paths);
}
