package jadx.gui.utils.res;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFileContent;
import jadx.api.ResourceType;
import jadx.core.xmlgen.ResContainer;
import jadx.gui.treemodel.JResource;

public class ResTableHelper {

	/**
	 * Build UI tree for resource table container.
	 *
	 * @return root nodes
	 */
	public static List<JResource> buildTree(ResContainer resTable) {
		ResTableHelper resTableHelper = new ResTableHelper();
		resTableHelper.process(resTable);
		return resTableHelper.roots;
	}

	private final List<JResource> roots = new ArrayList<>();
	private final Map<String, JResource> dirs = new HashMap<>();

	private ResTableHelper() {
	}

	private void process(ResContainer resTable) {
		for (ResContainer subFile : resTable.getSubFiles()) {
			loadSubNodes(subFile);
		}
	}

	private void loadSubNodes(ResContainer rc) {
		String resName = rc.getName();
		int split = resName.lastIndexOf('/');
		String dir;
		String name;
		if (split == -1) {
			dir = null;
			name = resName;
		} else {
			dir = resName.substring(0, split);
			name = resName.substring(split + 1);
		}
		ICodeInfo code = rc.getText();
		ResourceFileContent fileContent = new ResourceFileContent(name, ResourceType.XML, code);
		JResource resFile = new JResource(fileContent, resName, name, JResource.JResType.FILE);
		addResFile(dir, resFile);

		for (ResContainer subFile : rc.getSubFiles()) {
			loadSubNodes(subFile);
		}
	}

	private void addResFile(@Nullable String dir, JResource resFile) {
		if (dir == null) {
			roots.add(resFile);
			return;
		}
		JResource dirRes = dirs.get(dir);
		if (dirRes != null) {
			dirRes.addSubNode(resFile);
			return;
		}
		JResource parentDir = null;
		int splitPos = -1;
		while (true) {
			int prevStart = splitPos + 1;
			splitPos = dir.indexOf('/', prevStart);
			boolean last = splitPos == -1;
			String path = last ? dir : dir.substring(0, splitPos);
			JResource curDir = dirs.get(path);
			if (curDir == null) {
				String dirName = last ? dir.substring(prevStart) : dir.substring(prevStart, splitPos);
				curDir = new JResource(null, dirName, JResource.JResType.DIR);
				dirs.put(path, curDir);
				if (parentDir == null) {
					roots.add(curDir);
				} else {
					parentDir.addSubNode(curDir);
				}
			}
			if (last) {
				curDir.addSubNode(resFile);
				return;
			}
			parentDir = curDir;
		}
	}
}
