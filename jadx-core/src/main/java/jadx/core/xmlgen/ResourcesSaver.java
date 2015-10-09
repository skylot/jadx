package jadx.core.xmlgen;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.codegen.CodeWriter;

import java.io.File;
import java.util.List;

public class ResourcesSaver implements Runnable {
	private final ResourceFile resourceFile;
	private File outDir;

	public ResourcesSaver(File outDir, ResourceFile resourceFile) {
		this.resourceFile = resourceFile;
		this.outDir = outDir;
	}

	@Override
	public void run() {
		if (!ResourceType.isSupportedForUnpack(resourceFile.getType())) {
			return;
		}
		ResContainer rc = resourceFile.getContent();
		if (rc != null) {
			saveResources(rc);
		}
	}

	private void saveResources(ResContainer rc) {
		if (rc == null) {
			return;
		}
		List<ResContainer> subFiles = rc.getSubFiles();
		if (subFiles.isEmpty()) {
			CodeWriter cw = rc.getContent();
			if (cw != null) {
				cw.save(new File(outDir, rc.getFileName()));
			}
		} else {
			for (ResContainer subFile : subFiles) {
				saveResources(subFile);
			}
		}
	}
}
