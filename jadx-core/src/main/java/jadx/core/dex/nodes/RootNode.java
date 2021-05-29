package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.core.Jadx;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.InfoStorage;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.utils.MethodUtils;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.dex.visitors.typeinference.TypeUpdate;
import jadx.core.utils.CacheStorage;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.android.AndroidResourcesUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ResTableParser;
import jadx.core.xmlgen.ResourceStorage;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class RootNode {
	private static final Logger LOG = LoggerFactory.getLogger(RootNode.class);

	private final JadxArgs args;
	private final List<IDexTreeVisitor> preDecompilePasses;
	private final List<IDexTreeVisitor> passes;

	private final ErrorsCounter errorsCounter = new ErrorsCounter();
	private final StringUtils stringUtils;
	private final ConstStorage constValues;
	private final InfoStorage infoStorage = new InfoStorage();
	private final CacheStorage cacheStorage = new CacheStorage();
	private final TypeUpdate typeUpdate;
	private final MethodUtils methodUtils;
	private final TypeUtils typeUtils;

	private final Map<ClassInfo, ClassNode> clsMap = new HashMap<>();
	private List<ClassNode> classes = new ArrayList<>();

	private ClspGraph clsp;
	@Nullable
	private String appPackage;
	@Nullable
	private ClassNode appResClass;
	private boolean isProto;

	public RootNode(JadxArgs args) {
		this.args = args;
		this.preDecompilePasses = Jadx.getPreDecompilePassesList();
		this.passes = Jadx.getPassesList(args);
		this.stringUtils = new StringUtils(args);
		this.constValues = new ConstStorage(args);
		this.typeUpdate = new TypeUpdate(this);
		this.methodUtils = new MethodUtils(this);
		this.typeUtils = new TypeUtils(this);
		this.isProto = args.getInputFiles().size() > 0 && args.getInputFiles().get(0).getName().toLowerCase().endsWith(".aab");
	}

	public void loadClasses(List<ILoadResult> loadedInputs) {
		for (ILoadResult loadedInput : loadedInputs) {
			loadedInput.visitClasses(cls -> {
				try {
					addClassNode(new ClassNode(RootNode.this, cls));
				} catch (Exception e) {
					addDummyClass(cls, e);
				}
				Utils.checkThreadInterrupt();
			});
		}
		if (classes.size() != clsMap.size()) {
			// class name duplication detected
			classes.stream().collect(Collectors.groupingBy(ClassNode::getClassInfo))
					.entrySet().stream()
					.filter(entry -> entry.getValue().size() > 1)
					.forEach(entry -> {
						LOG.warn("Found duplicated class: {}, count: {}. Only one will be loaded!", entry.getKey(),
								entry.getValue().size());
						entry.getValue().forEach(cls -> cls.addAttr(AType.COMMENTS, "WARNING: Classes with same name are omitted"));
					});
		}
		classes = new ArrayList<>(clsMap.values());
		// sort classes by name, expect top classes before inner
		classes.sort(Comparator.comparing(ClassNode::getFullName));
		initInnerClasses();

		// print stats for loaded classes
		int mthCount = classes.stream().mapToInt(c -> c.getMethods().size()).sum();
		int insnsCount = classes.stream().flatMap(c -> c.getMethods().stream()).mapToInt(MethodNode::getInsnsCount).sum();
		LOG.info("Loaded classes: {}, methods: {}, instructions: {}", classes.size(), mthCount, insnsCount);
	}

	private void addDummyClass(IClassData classData, Exception exc) {
		String typeStr = classData.getType();
		String name = null;
		try {
			ClassInfo clsInfo = ClassInfo.fromName(this, typeStr);
			if (clsInfo != null) {
				name = clsInfo.getShortName();
			}
		} catch (Exception e) {
			LOG.error("Failed to get name for class with type {}", typeStr, e);
		}
		if (name == null || name.isEmpty()) {
			name = "CLASS_" + typeStr;
		}
		ClassNode clsNode = ClassNode.addSyntheticClass(this, name, classData.getAccessFlags());
		ErrorsCounter.error(clsNode, "Load error", exc);
	}

	public void addClassNode(ClassNode clsNode) {
		classes.add(clsNode);
		clsMap.put(clsNode.getClassInfo(), clsNode);
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
			ResTableParser parser = ResourcesLoader.decodeStream(arsc, (size, is) -> {
				ResTableParser tableParser = new ResTableParser(this);
				tableParser.decode(is);
				return tableParser;
			});
			if (parser != null) {
				processResources(parser.getResStorage());
				updateObfuscatedFiles(parser, resources);
			}
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
				newClsp.addApp(classes);
				this.clsp = newClsp;
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Error loading jadx class set", e);
		}
	}

	private void updateObfuscatedFiles(ResTableParser parser, List<ResourceFile> resources) {
		if (args.isSkipResources()) {
			return;
		}
		long start = System.currentTimeMillis();
		int renamedCount = 0;
		ResourceStorage resStorage = parser.getResStorage();
		ValuesParser valuesParser = new ValuesParser(parser.getStrings(), resStorage.getResourcesNames());
		Map<String, ResourceEntry> entryNames = new HashMap<>();
		for (ResourceEntry resEntry : resStorage.getResources()) {
			String val = valuesParser.getSimpleValueString(resEntry);
			if (val != null) {
				entryNames.put(val, resEntry);
			}
		}
		for (ResourceFile resource : resources) {
			ResourceEntry resEntry = entryNames.get(resource.getOriginalName());
			if (resEntry != null) {
				resource.setAlias(resEntry);
				renamedCount++;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Renamed obfuscated resources: {}, duration: {}ms", renamedCount, System.currentTimeMillis() - start);
		}
	}

	private void initInnerClasses() {
		// move inner classes
		List<ClassNode> inner = new ArrayList<>();
		for (ClassNode cls : classes) {
			if (cls.getClassInfo().isInner()) {
				inner.add(cls);
			}
		}
		List<ClassNode> updated = new ArrayList<>();
		for (ClassNode cls : inner) {
			ClassInfo clsInfo = cls.getClassInfo();
			ClassNode parent = resolveClass(clsInfo.getParentClass());
			if (parent == null) {
				clsMap.remove(clsInfo);
				clsInfo.notInner(this);
				clsMap.put(clsInfo, cls);
				updated.add(cls);
			} else {
				parent.addInnerClass(cls);
			}
		}
		// reload names for inner classes of updated parents
		for (ClassNode updCls : updated) {
			for (ClassNode innerCls : updCls.getInnerClasses()) {
				innerCls.getClassInfo().updateNames(this);
			}
		}
	}

	public void runPreDecompileStage() {
		for (IDexTreeVisitor pass : preDecompilePasses) {
			long start = System.currentTimeMillis();
			try {
				pass.init(this);
			} catch (Exception e) {
				LOG.error("Visitor init failed: {}", pass.getClass().getSimpleName(), e);
			}
			for (ClassNode cls : classes) {
				DepthTraversal.visit(pass, cls);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("{} time: {}ms", pass.getClass().getSimpleName(), System.currentTimeMillis() - start);
			}
		}
	}

	public void runPreDecompileStageForClass(ClassNode cls) {
		for (IDexTreeVisitor pass : preDecompilePasses) {
			DepthTraversal.visit(pass, cls);
		}
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	public List<ClassNode> getClassesWithoutInner() {
		return getClasses(false);
	}

	public List<ClassNode> getClasses(boolean includeInner) {
		if (includeInner) {
			return classes;
		}
		List<ClassNode> notInnerClasses = new ArrayList<>();
		for (ClassNode cls : classes) {
			if (!cls.getClassInfo().isInner()) {
				notInnerClasses.add(cls);
			}
		}
		return notInnerClasses;
	}

	@Nullable
	public ClassNode resolveClass(ClassInfo clsInfo) {
		return clsMap.get(clsInfo);
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
		return resolveClass(ClassInfo.fromType(this, clsType));
	}

	@Nullable
	public ClassNode resolveClass(String fullName) {
		ClassInfo clsInfo = ClassInfo.fromName(this, fullName);
		return resolveClass(clsInfo);
	}

	@Nullable
	public ClassNode searchClassByFullAlias(String fullName) {
		for (ClassNode cls : classes) {
			ClassInfo classInfo = cls.getClassInfo();
			if (classInfo.getFullName().equals(fullName)
					|| classInfo.getAliasFullName().equals(fullName)) {
				return cls;
			}
		}
		return null;
	}

	public List<ClassNode> searchClassByShortName(String shortName) {
		List<ClassNode> list = new ArrayList<>();
		for (ClassNode cls : classes) {
			if (cls.getClassInfo().getShortName().equals(shortName)) {
				list.add(cls);
			}
		}
		return list;
	}

	@Nullable
	public MethodNode resolveMethod(@NotNull MethodInfo mth) {
		ClassNode cls = resolveClass(mth.getDeclClass());
		if (cls != null) {
			return cls.searchMethod(mth);
		}
		return null;
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
		return deepResolveMethod(cls, mth.makeSignature(false));
	}

	@Nullable
	private MethodNode deepResolveMethod(@NotNull ClassNode cls, String signature) {
		for (MethodNode m : cls.getMethods()) {
			if (m.getMethodInfo().getShortId().startsWith(signature)) {
				return m;
			}
		}
		MethodNode found;
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			ClassNode superNode = resolveClass(superClass);
			if (superNode != null) {
				found = deepResolveMethod(superNode, signature);
				if (found != null) {
					return found;
				}
			}
		}
		for (ArgType iFaceType : cls.getInterfaces()) {
			ClassNode iFaceNode = resolveClass(iFaceType);
			if (iFaceNode != null) {
				found = deepResolveMethod(iFaceNode, signature);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	@Nullable
	public FieldNode resolveField(FieldInfo field) {
		ClassNode cls = resolveClass(field.getDeclClass());
		if (cls != null) {
			return cls.searchField(field);
		}
		return null;
	}

	@Nullable
	public FieldNode deepResolveField(@NotNull FieldInfo field) {
		ClassNode cls = resolveClass(field.getDeclClass());
		if (cls == null) {
			return null;
		}
		return deepResolveField(cls, field);
	}

	@Nullable
	private FieldNode deepResolveField(@NotNull ClassNode cls, FieldInfo fieldInfo) {
		FieldNode field = cls.searchFieldByNameAndType(fieldInfo);
		if (field != null) {
			return field;
		}
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			ClassNode superNode = resolveClass(superClass);
			if (superNode != null) {
				FieldNode found = deepResolveField(superNode, fieldInfo);
				if (found != null) {
					return found;
				}
			}
		}
		for (ArgType iFaceType : cls.getInterfaces()) {
			ClassNode iFaceNode = resolveClass(iFaceType);
			if (iFaceNode != null) {
				FieldNode found = deepResolveField(iFaceNode, fieldInfo);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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

	public ICodeWriter makeCodeWriter() {
		JadxArgs jadxArgs = this.args;
		return jadxArgs.getCodeWriterProvider().apply(jadxArgs);
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

	@Nullable
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

	public TypeCompare getTypeCompare() {
		return typeUpdate.getTypeCompare();
	}

	public ICodeCache getCodeCache() {
		return args.getCodeCache();
	}

	public MethodUtils getMethodUtils() {
		return methodUtils;
	}

	public TypeUtils getTypeUtils() {
		return typeUtils;
	}

	public boolean isProto() {
		return isProto;
	}
}
