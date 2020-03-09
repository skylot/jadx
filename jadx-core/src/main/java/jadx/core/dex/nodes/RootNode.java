package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.JadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.core.Jadx;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.InfoStorage;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.utils.MethodUtils;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.typeinference.TypeUpdate;
import jadx.core.utils.CacheStorage;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.StringUtils;
import jadx.core.utils.android.AndroidResourcesUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.DexFile;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResTableParser;
import jadx.core.xmlgen.ResourceStorage;

public class RootNode {
	private static final Logger LOG = LoggerFactory.getLogger(RootNode.class);

	private final JadxArgs args;
	private final List<IDexTreeVisitor> passes;

	private final ErrorsCounter errorsCounter = new ErrorsCounter();
	private final StringUtils stringUtils;
	private final ConstStorage constValues;
	private final InfoStorage infoStorage = new InfoStorage();
	private final CacheStorage cacheStorage = new CacheStorage();
	private final TypeUpdate typeUpdate;
	private final MethodUtils methodUtils;
	private final TypeUtils typeUtils;

	private final ICodeCache codeCache;

	private ClspGraph clsp;
	private List<DexNode> dexNodes;
	@Nullable
	private String appPackage;
	@Nullable
	private ClassNode appResClass;

	public RootNode(JadxArgs args) {
		this.args = args;
		this.passes = Jadx.getPassesList(args);
		this.stringUtils = new StringUtils(args);
		this.constValues = new ConstStorage(args);
		this.typeUpdate = new TypeUpdate(this);
		this.codeCache = args.getCodeCache();
		this.methodUtils = new MethodUtils(this);
		this.typeUtils = new TypeUtils(this);

		this.dexNodes = Collections.emptyList();
	}

	public void load(List<InputFile> inputFiles) {
		dexNodes = new ArrayList<>();
		for (InputFile input : inputFiles) {
			for (DexFile dexFile : input.getDexFiles()) {
				try {
					LOG.debug("Load: {}", dexFile);
					DexNode dexNode = new DexNode(this, dexFile, dexNodes.size());
					dexNodes.add(dexNode);
				} catch (Exception e) {
					throw new JadxRuntimeException("Error decode file: " + dexFile, e);
				}
			}
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
		try {
			ResourceStorage resStorage = ResourcesLoader.decodeStream(arsc, (size, is) -> {
				ResTableParser parser = new ResTableParser(this);
				parser.decode(is);
				return parser.getResStorage();
			});
			processResources(resStorage);
		} catch (Exception e) {
			LOG.error("Failed to parse '.arsc' file", e);
		}
	}

	public void processResources(ResourceStorage resStorage) {
		constValues.setResourcesNames(resStorage.getResourcesNames());
		appPackage = resStorage.getAppPackage();
		appResClass = AndroidResourcesUtils.searchAppResClass(this, resStorage);
	}

	public void initClassPath() {
		try {
			if (this.clsp == null) {
				ClspGraph newClsp = new ClspGraph(this);
				newClsp.load();

				List<ClassNode> classes = new ArrayList<>();
				for (DexNode dexNode : dexNodes) {
					classes.addAll(dexNode.getClasses());
				}
				newClsp.addApp(classes);

				this.clsp = newClsp;
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Error loading classpath", e);
		}
	}

	private void initInnerClasses() {
		for (DexNode dexNode : dexNodes) {
			dexNode.initInnerClasses();
		}
	}

	public List<ClassNode> getClasses(boolean includeInner) {
		List<ClassNode> classes = new ArrayList<>();
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

	@Nullable
	public ClassNode resolveClass(ClassInfo clsInfo) {
		for (DexNode dexNode : dexNodes) {
			ClassNode cls = dexNode.resolveClassLocal(clsInfo);
			if (cls != null) {
				return cls;
			}
		}
		return null;
	}

	@Nullable
	public ClassNode resolveClass(ArgType clsType) {
		if (!clsType.isTypeKnown() || clsType.isGenericType()) {
			return null;
		}
		if (clsType.getWildcardBound() == ArgType.WildcardBound.UNBOUND) {
			return null;
		}
		if (clsType.isGeneric()) {
			clsType = ArgType.object(clsType.getObject());
		}
		for (DexNode dexNode : dexNodes) {
			ClassNode cls = dexNode.resolveClass(clsType);
			if (cls != null) {
				return cls;
			}
		}
		return null;
	}

	@Nullable
	public ClassNode searchClassByName(String fullName) {
		ClassInfo clsInfo = ClassInfo.fromName(this, fullName);
		return resolveClass(clsInfo);
	}

	@Nullable
	public ClassNode searchClassByFullAlias(String fullName) {
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				ClassInfo classInfo = cls.getClassInfo();
				if (classInfo.getFullName().equals(fullName)
						|| classInfo.getAliasFullName().equals(fullName)) {
					return cls;
				}
			}
		}
		return null;
	}

	public List<ClassNode> searchClassByShortName(String shortName) {
		List<ClassNode> list = new ArrayList<>();
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				if (cls.getClassInfo().getShortName().equals(shortName)) {
					list.add(cls);
				}
			}
		}
		return list;
	}

	@Nullable
	public MethodNode deepResolveMethod(@NotNull MethodInfo mth) {
		ClassNode cls = resolveClass(mth.getDeclClass());
		if (cls == null) {
			return null;
		}
		MethodNode methodNode = cls.searchMethod(mth);
		if (methodNode != null) {
			return methodNode;
		}
		return cls.dex().deepResolveMethod(cls, mth.makeSignature(false));
	}

	@Nullable
	public FieldNode deepResolveField(@NotNull FieldInfo field) {
		ClassNode cls = resolveClass(field.getDeclClass());
		if (cls == null) {
			return null;
		}
		return cls.dex().deepResolveField(cls, field);
	}

	public List<IDexTreeVisitor> getPasses() {
		return passes;
	}

	public void initPasses() {
		for (IDexTreeVisitor pass : passes) {
			try {
				pass.init(this);
			} catch (Exception e) {
				LOG.error("Visitor init failed: {}", pass.getClass().getSimpleName(), e);
			}
		}
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

	@Nullable
	public String getAppPackage() {
		return appPackage;
	}

	public ClassNode getAppResClass() {
		return appResClass;
	}

	public StringUtils getStringUtils() {
		return stringUtils;
	}

	public ConstStorage getConstValues() {
		return constValues;
	}

	public InfoStorage getInfoStorage() {
		return infoStorage;
	}

	public CacheStorage getCacheStorage() {
		return cacheStorage;
	}

	public JadxArgs getArgs() {
		return args;
	}

	public TypeUpdate getTypeUpdate() {
		return typeUpdate;
	}

	public ICodeCache getCodeCache() {
		return codeCache;
	}

	public MethodUtils getMethodUtils() {
		return methodUtils;
	}

	public TypeUtils getTypeUtils() {
		return typeUtils;
	}
}
