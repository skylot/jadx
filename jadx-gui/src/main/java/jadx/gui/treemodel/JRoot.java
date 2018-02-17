package jadx.gui.treemodel;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import jadx.api.ResourceFile;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JResource.JResType;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;

public class JRoot extends JNode {
	private static final long serialVersionUID = 8888495789773527342L;

	private static final ImageIcon ROOT_ICON = Utils.openIcon("java_model_obj");

	private final transient JadxWrapper wrapper;

	private transient boolean flatPackages = false;

	public JRoot(JadxWrapper wrapper) {
		this.wrapper = wrapper;
		update();
	}

	public final void update() {
		removeAllChildren();
		add(new JSources(this, wrapper));

		List<JResource> resList = getHierarchyResources(wrapper.getResources());
		for (JResource jRes : resList) {
			jRes.update();
			add(jRes);
		}
	}

	private List<JResource> getHierarchyResources(List<ResourceFile> resources) {
		if (resources.isEmpty()) {
			return Collections.emptyList();
		}
		JResource root = new JResource(null, NLS.str("tree.resources_title"), JResType.ROOT);
		String splitPathStr = Pattern.quote(File.separator);
		for (ResourceFile rf : resources) {
			String rfName;
			if (rf.getZipRef() != null) {
				rfName = rf.getName();
			} else {
				rfName = new File(rf.getName()).getName();
			}
			String[] parts = new File(rfName).getPath().split(splitPathStr);
			JResource curRf = root;
			int count = parts.length;
			for (int i = 0; i < count; i++) {
				String name = parts[i];
				JResource subRF = getResourceByName(curRf, name);
				if (subRF == null) {
					if (i != count - 1) {
						subRF = new JResource(null, name, JResType.DIR);
					} else {
						subRF = new JResource(rf, name, JResType.FILE);
					}
					curRf.getFiles().add(subRF);
				}
				curRf = subRF;
			}
		}
		return Collections.singletonList(root);
	}

	private JResource getResourceByName(JResource rf, String name) {
		for (JResource sub : rf.getFiles()) {
			if (sub.getName().equals(name)) {
				return sub;
			}
		}
		return null;
	}

	public JNode searchClassInTree(JNode node) {
		Enumeration<?> en = this.breadthFirstEnumeration();
		while (en.hasMoreElements()) {
			Object obj = en.nextElement();
			if (node.equals(obj)) {
				return (JNode) obj;
			}
		}
		return null;
	}

	public boolean isFlatPackages() {
		return flatPackages;
	}

	public void setFlatPackages(boolean flatPackages) {
		if (this.flatPackages != flatPackages) {
			this.flatPackages = flatPackages;
			update();
		}
	}

	@Override
	public Icon getIcon() {
		return ROOT_ICON;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public String makeString() {
		File file = wrapper.getOpenFile();
		return file != null ? file.getName() : "File not open";
	}
}
