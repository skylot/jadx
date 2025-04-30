package jadx.core.export.gen;

import jadx.core.export.OutDirs;

public interface IExportGradleGenerator {

	void init();

	OutDirs getOutDirs();

	void generateFiles();
}
