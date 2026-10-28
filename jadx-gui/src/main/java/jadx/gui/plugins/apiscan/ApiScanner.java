package jadx.gui.plugins.apiscan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.Opcode;
import jadx.api.plugins.input.insns.custom.ICustomPayload;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.input.InsnDataUtils;

import static jadx.gui.plugins.apiscan.ApiCategory.CLASS_LOADING;
import static jadx.gui.plugins.apiscan.ApiCategory.EXIT;
import static jadx.gui.plugins.apiscan.ApiCategory.FILES;
import static jadx.gui.plugins.apiscan.ApiCategory.NATIVE_CODE;
import static jadx.gui.plugins.apiscan.ApiCategory.NETWORK;
import static jadx.gui.plugins.apiscan.ApiCategory.PROCESS;
import static jadx.gui.plugins.apiscan.ApiCategory.REFLECTION;
import static jadx.gui.plugins.apiscan.ApiCategory.UNSAFE;

// Collect calls to dangerous API using raw instructions (without decompilation)
public final class ApiScanner {
	private static final Logger LOG = LoggerFactory.getLogger(ApiScanner.class);

	private static final List<ApiRule> RULES = Arrays.asList(
			rule(PROCESS, "java.lang.Runtime", "exec"),
			rule(PROCESS, "java.lang.ProcessBuilder", "start"),
			rule(PROCESS, "java.awt.Desktop", "open", "edit", "print", "browse", "mail"),
			rule(FILES, "java.io.File", "delete", "deleteOnExit", "renameTo", "createNewFile", "createTempFile",
					"mkdir", "mkdirs", "setExecutable", "setReadable", "setWritable", "setReadOnly", "setLastModified"),
			rule(FILES, "java.io.FileInputStream", "<init>"),
			rule(FILES, "java.io.FileOutputStream", "<init>"),
			rule(FILES, "java.io.FileReader", "<init>"),
			rule(FILES, "java.io.FileWriter", "<init>"),
			rule(FILES, "java.io.RandomAccessFile", "<init>"),
			rule(FILES, "java.nio.file.Files"),
			rule(FILES, "java.nio.channels.FileChannel", "open"),
			rule(FILES, "kotlin.io.FilesKt"),
			rule(NETWORK, "java.net.URL", "openConnection", "openStream"),
			rule(NETWORK, "java.net.URLConnection", "connect", "getInputStream", "getOutputStream"),
			rule(NETWORK, "java.net.Socket", "<init>", "connect"),
			rule(NETWORK, "java.net.ServerSocket", "<init>", "bind", "accept"),
			rule(NETWORK, "java.net.DatagramSocket", "<init>", "connect", "send"),
			rule(NETWORK, "java.nio.channels.SocketChannel", "open", "connect"),
			rule(NETWORK, "java.net.http.HttpClient", "send", "sendAsync"),
			rule(REFLECTION, "java.lang.Class", "forName", "newInstance"),
			rule(REFLECTION, "java.lang.reflect.AccessibleObject", "setAccessible", "trySetAccessible"),
			rule(REFLECTION, "java.lang.reflect.Method", "invoke"),
			rule(REFLECTION, "java.lang.reflect.Constructor", "newInstance"),
			rule(REFLECTION, "java.lang.invoke.MethodHandles", "privateLookupIn"),
			rule(NATIVE_CODE, "java.lang.System", "load", "loadLibrary"),
			rule(NATIVE_CODE, "java.lang.Runtime", "load", "loadLibrary"),
			rule(CLASS_LOADING, "java.lang.ClassLoader", "defineClass"),
			rule(CLASS_LOADING, "java.net.URLClassLoader", "<init>", "newInstance"),
			rule(CLASS_LOADING, "java.lang.invoke.MethodHandles$Lookup", "defineClass", "defineHiddenClass"),
			rule(UNSAFE, "sun.misc.Unsafe"),
			rule(UNSAFE, "jdk.internal.misc.Unsafe"),
			rule(EXIT, "java.lang.System", "exit"),
			rule(EXIT, "java.lang.Runtime", "exit", "halt"));

	private static final Map<String, List<ApiRule>> RULES_MAP = RULES.stream().collect(Collectors.groupingBy(rule -> rule.cls));

	private final RootNode root;
	private final Map<String, List<ApiRule>> rulesCache = new HashMap<>();
	private final Set<ApiUsage> usages = new LinkedHashSet<>();

	public ApiScanner(RootNode root) {
		this.root = root;
	}

	public ApiScanResult scan() {
		for (ClassNode cls : root.getClasses()) {
			for (MethodNode mth : cls.getMethods()) {
				try {
					scanMethod(mth);
				} catch (Exception e) {
					LOG.warn("Dangerous API scan failed for method: {}", mth, e);
				}
			}
		}
		return new ApiScanResult(new ArrayList<>(usages));
	}

	private void scanMethod(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader == null) {
			return;
		}
		codeReader.visitInstructions(insn -> {
			IMethodRef mthRef = getMethodRef(insn);
			if (mthRef != null) {
				checkCall(mth, MethodInfo.fromRef(root, mthRef));
			}
		});
	}

	private static @Nullable IMethodRef getMethodRef(InsnData insn) {
		if (insn.getOpcode() == Opcode.UNKNOWN) {
			return null;
		}
		switch (insn.getIndexType()) {
			case METHOD_REF: {
				insn.decode();
				ICustomPayload payload = insn.getPayload();
				return payload != null ? (IMethodRef) payload : insn.getIndexAsMethod();
			}
			case CALL_SITE: {
				insn.decode();
				ICallSite callSite = InsnDataUtils.getCallSite(insn);
				if (callSite == null) {
					return null;
				}
				IMethodHandle methodHandle = InsnDataUtils.getMethodHandleAt(callSite, 4);
				if (methodHandle == null) {
					return null;
				}
				return methodHandle.getMethodRef();
			}
			default:
				return null;
		}
	}

	private void checkCall(MethodNode mth, MethodInfo calledMth) {
		ClassInfo declCls = calledMth.getDeclClass();
		for (ApiRule rule : getRules(declCls.getRawName())) {
			if (rule.matches(calledMth.getName())) {
				String api = calledMth.isConstructor()
						? "new " + declCls.getFullName()
						: declCls.getFullName() + '.' + calledMth.getName();
				usages.add(new ApiUsage(mth, rule.getCategory(), api));
			}
		}
	}

	/**
	 * Rule classes can extend each other (like {@code Method} and {@code AccessibleObject}),
	 * so collect rules from current class and all super classes.
	 */
	private List<ApiRule> getRules(String clsName) {
		return rulesCache.computeIfAbsent(clsName, name -> {
			List<ApiRule> list = new ArrayList<>(RULES_MAP.getOrDefault(name, Collections.emptyList()));
			for (String superType : root.getClsp().getSuperTypes(name)) {
				list.addAll(RULES_MAP.getOrDefault(superType, Collections.emptyList()));
			}
			return list;
		});
	}

	private static ApiRule rule(ApiCategory category, String cls, String... methods) {
		return new ApiRule(category, cls, new HashSet<>(Arrays.asList(methods)));
	}

	private static final class ApiRule {
		private final ApiCategory category;
		private final String cls;
		private final Set<String> methods;

		private ApiRule(ApiCategory category, String cls, Set<String> methods) {
			this.category = category;
			this.cls = cls;
			this.methods = methods;
		}

		public ApiCategory getCategory() {
			return category;
		}

		public String getCls() {
			return cls;
		}

		public boolean matches(String mthName) {
			return methods.isEmpty() || methods.contains(mthName);
		}
	}
}
