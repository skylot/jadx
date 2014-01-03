package jadx.core.dex.nodes;

import jadx.core.clsp.ClspGraph;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.files.InputFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RootNode {
	private final Map<String, ClassNode> names = new HashMap<String, ClassNode>();
	private List<DexNode> dexNodes;

	public void load(List<InputFile> dexFiles) throws DecodeException {
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
		for (DexNode dexNode : dexNodes) {
			dexNode.loadClasses();
		}

		List<ClassNode> classes = new ArrayList<ClassNode>();
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				names.put(cls.getFullName(), cls);
			}
			classes.addAll(dexNode.getClasses());
		}

		try {
			initClassPath(classes);
		} catch (IOException e) {
			throw new DecodeException("Error loading classpath", e);
		}
		initInnerClasses(classes);
	}

	private void initClassPath(List<ClassNode> classes) throws IOException, DecodeException {
		ClspGraph clsp = new ClspGraph();
		clsp.load();
		clsp.addApp(classes);

		ArgType.setClsp(clsp);
	}

	private void initInnerClasses(List<ClassNode> classes) {
		// move inner classes
		List<ClassNode> inner = new ArrayList<ClassNode>();
		for (ClassNode cls : classes) {
			if (cls.getClassInfo().isInner()) {
				inner.add(cls);
			}
		}
		for (ClassNode cls : inner) {
			ClassNode parent = resolveClass(cls.getClassInfo().getParentClass());
			if (parent == null) {
				names.remove(cls.getFullName());
				cls.getClassInfo().notInner();
				names.put(cls.getFullName(), cls);
			} else {
				parent.addInnerClass(cls);
			}
		}
	}

	public List<ClassNode> getClasses(boolean includeInner) {
		List<ClassNode> classes = new ArrayList<ClassNode>();
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				if (includeInner) {
					classes.add(cls);
				} else {
					if (!cls.getClassInfo().isInner()) {
						classes.add(cls);
					}
				}
			}
		}
		return classes;
	}

	public ClassNode searchClassByName(String fullName) {
		return names.get(fullName);
	}

	public ClassNode resolveClass(ClassInfo cls) {
		String fullName = cls.getFullName();
		return searchClassByName(fullName);
	}
}
