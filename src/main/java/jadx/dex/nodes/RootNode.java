package jadx.dex.nodes;

import jadx.JadxArgs;
import jadx.dex.info.ClassInfo;
import jadx.utils.exceptions.DecodeException;
import jadx.utils.files.InputFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootNode {
	private static final Logger LOG = LoggerFactory.getLogger(RootNode.class);

	private final JadxArgs jadxArgs;

	private List<DexNode> dexNodes;
	private final List<ClassNode> classes = new ArrayList<ClassNode>();

	private final Set<String> earlyClassList = new HashSet<String>();
	private final Map<String, ClassNode> names = new HashMap<String, ClassNode>();

	public RootNode(JadxArgs args) {
		this.jadxArgs = args;
	}

	public void load() throws DecodeException {
		List<InputFile> dexFiles = jadxArgs.getInput();
		dexNodes = new ArrayList<DexNode>(dexFiles.size());
		for (InputFile dex : dexFiles) {
			DexNode dexNode;
			try {
				dexNode = new DexNode(this, dex);
			} catch (Exception e) {
				throw new DecodeException("Error decode file: " + dex, e);
			}
			dexNodes.add(dexNode);
		}

		for (DexNode dexNode : dexNodes)
			earlyClassList.addAll(dexNode.getAllClassesNames());

		for (DexNode dexNode : dexNodes)
			dexNode.loadClasses(this);

		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses())
				names.put(cls.getFullName(), cls);
			classes.addAll(dexNode.getClasses());
		}
	}

	public void init() {
		// move inner classes
		List<ClassNode> inner = new ArrayList<ClassNode>();
		for (ClassNode cls : getClasses()) {
			if (cls.getClassInfo().isInner())
				inner.add(cls);
		}
		getClasses().removeAll(inner);

		for (ClassNode cls : inner) {
			ClassNode parent = resolveClass(cls.getClassInfo().getParentClass());
			if (parent == null)
				LOG.warn("Can't add inner class: {} to {}", cls, cls.getClassInfo().getParentClass());
			else
				parent.addInnerClass(cls);
		}
		inner.clear();
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	public ClassNode searchClassByName(String fullName) {
		return names.get(fullName);
	}

	public boolean isClassExists(String fullName) {
		return earlyClassList.contains(fullName);
	}

	public ClassNode resolveClass(ClassInfo cls) {
		String fullName = cls.getFullName();
		ClassNode rCls = searchClassByName(fullName);
		return rCls;
	}

	public List<DexNode> getDexNodes() {
		return dexNodes;
	}

	public JadxArgs getJadxArgs() {
		return jadxArgs;
	}
}
