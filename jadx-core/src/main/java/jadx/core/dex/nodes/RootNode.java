package jadx.core.dex.nodes;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResTableParser;
import jadx.core.xmlgen.ResourceStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootNode {
	private static final Logger LOG = LoggerFactory.getLogger(RootNode.class);

	private final Map<String, ClassNode> names = new HashMap<String, ClassNode>();
	private final ErrorsCounter errorsCounter = new ErrorsCounter();

	private List<DexNode> dexNodes;

	/**
	 * Resources *
	 */
	private Map<Integer, String> resourcesNames = new HashMap<Integer, String>();
	@Nullable
	private String appPackage;
	private ClassNode appResClass;

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

	public void loadResources(List<ResourceFile> resources) {
		ResourceFile arsc = null;
		for (ResourceFile rf : resources) {
			if (rf.getType() == ResourceType.ARSC) {
				arsc = rf;
				break;
			}
		}
		if (arsc == null) {
			LOG.debug("'.arsc' file not found");
			return;
		}
		final ResTableParser parser = new ResTableParser();
		try {
			ResourcesLoader.decodeStream(arsc, new ResourcesLoader.ResourceDecoder() {
				@Override
				public Object decode(long size, InputStream is) throws IOException {
					parser.decode(is);
					return null;
				}
			});
		} catch (JadxException e) {
			LOG.error("Failed to parse '.arsc' file", e);
			return;
		}

		ResourceStorage resStorage = parser.getResStorage();
		resourcesNames = resStorage.getResourcesNames();
	}

	public void initAppResClass() {
		ClassNode resCls = null;
		if (appPackage != null) {
			resCls = searchClassByName(appPackage + ".R");
		} else {
			for (ClassNode cls : names.values()) {
				if (cls.getShortName().equals("R")) {
					resCls = cls;
					break;
				}
			}
		}
		if (resCls != null) {
			appResClass = resCls;
			return;
		}
		appResClass = new ClassNode(dexNodes.get(0), ClassInfo.fromName("R"));
	}

	private static void initClassPath(List<ClassNode> classes) throws IOException, DecodeException {
		if (!ArgType.isClspSet()) {
			ClspGraph clsp = new ClspGraph();
			clsp.load();
			clsp.addApp(classes);

			ArgType.setClsp(clsp);
		}
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

	public List<DexNode> getDexNodes() {
		return dexNodes;
	}

	public ErrorsCounter getErrorsCounter() {
		return errorsCounter;
	}

	public Map<Integer, String> getResourcesNames() {
		return resourcesNames;
	}

	@Nullable
	public String getAppPackage() {
		return appPackage;
	}

	public ClassNode getAppResClass() {
		return appResClass;
	}
}
