package jadx.core.dex.nodes;

import jadx.api.IJadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.info.ClassInfo;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResContainer;
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

	private final ErrorsCounter errorsCounter = new ErrorsCounter();
	private final IJadxArgs args;

	private List<DexNode> dexNodes;
	private Map<Integer, String> resourcesNames = new HashMap<Integer, String>();
	@Nullable
	private String appPackage;
	private ClassNode appResClass;
	private ClspGraph clsp;

	public RootNode(IJadxArgs args) {
		this.args = args;
	}

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
		initInnerClasses();
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
				public ResContainer decode(long size, InputStream is) throws IOException {
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
		appPackage = resStorage.getAppPackage();
	}

	public void initAppResClass() {
		ClassNode resCls;
		if (appPackage == null) {
			appResClass = makeClass("R");
			return;
		}
		String fullName = appPackage + ".R";
		resCls = searchClassByName(fullName);
		if (resCls != null) {
			appResClass = resCls;
		} else {
			appResClass = makeClass(fullName);
		}
	}

	private ClassNode makeClass(String clsName) {
		DexNode firstDex = dexNodes.get(0);
		ClassInfo r = ClassInfo.fromName(firstDex, clsName);
		return new ClassNode(firstDex, r);
	}

	public void initClassPath() throws DecodeException {
		try {
			if (this.clsp == null) {
				ClspGraph clsp = new ClspGraph();
				clsp.load();

				List<ClassNode> classes = new ArrayList<ClassNode>();
				for (DexNode dexNode : dexNodes) {
					classes.addAll(dexNode.getClasses());
				}
				clsp.addApp(classes);

				this.clsp = clsp;
			}
		} catch (IOException e) {
			throw new DecodeException("Error loading classpath", e);
		}
	}

	private void initInnerClasses() {
		for (DexNode dexNode : dexNodes) {
			dexNode.initInnerClasses();
		}
	}

	public List<ClassNode> getClasses(boolean includeInner) {
		List<ClassNode> classes = new ArrayList<ClassNode>();
		for (DexNode dex : dexNodes) {
			if (includeInner) {
				classes.addAll(dex.getClasses());
			} else {
				for (ClassNode cls : dex.getClasses()) {
					if (!cls.getClassInfo().isInner()) {
						classes.add(cls);
					}
				}
			}
		}
		return classes;
	}

	public ClassNode searchClassByName(String fullName) {
		for (DexNode dexNode : dexNodes) {
			ClassInfo clsInfo = ClassInfo.fromName(dexNode, fullName);
			ClassNode cls = dexNode.resolveClass(clsInfo);
			if (cls != null) {
				return cls;
			}
		}
		return null;
	}

	public List<DexNode> getDexNodes() {
		return dexNodes;
	}

	public ClspGraph getClsp() {
		return clsp;
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

	public IJadxArgs getArgs() {
		return args;
	}
}
