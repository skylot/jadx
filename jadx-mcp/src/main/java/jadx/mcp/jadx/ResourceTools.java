package jadx.mcp.jadx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.android.AndroidManifestParser;
import jadx.core.xmlgen.ResContainer;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolArgs;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;
import jadx.mcp.tools.ToolRegistry;

import static jadx.mcp.jadx.ToolSupport.MAX_LIMIT;
import static jadx.mcp.jadx.ToolSupport.limitParam;
import static jadx.mcp.jadx.ToolSupport.offsetParam;
import static jadx.mcp.jadx.ToolSupport.page;
import static jadx.mcp.jadx.ToolSupport.pageList;
import static jadx.mcp.jadx.ToolSupport.textPageParams;
import static jadx.mcp.jadx.ToolSupport.withProject;

/**
 * AndroidManifest and resources tools. Content decoded by jadx resources loader.
 */
final class ResourceTools {
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	private static final int MAX_BASE64_BYTES = 1024 * 1024;
	private static final String[] COMPONENT_TAGS = { "activity", "activity-alias", "service", "receiver", "provider" };

	/**
	 * Decoded resources table (values xml files) cache, one per project generation
	 */
	private static final Map<RootNode, SoftReference<ResContainer>> ARSC_CACHE = new WeakHashMap<>();

	private ResourceTools() {
	}

	static void register(ToolRegistry registry, McpBackend backend) {
		registry.register(McpTool.builder("get_android_manifest")
				.title("AndroidManifest.xml")
				.description("Get decoded AndroidManifest.xml text (paged by lines)")
				.input(textPageParams(Schema.object()).build())
				.output(Schema.object().open()
						.required("file", Schema.string("Resource file name"))
						.required("content", NodeJson.pageSchema("Manifest text page"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					ResourceFile manifest = getManifest(lease.getDecompiler());
					CodeText text = new CodeText(loadText(manifest));
					JsonObject result = new JsonObject();
					result.addProperty("file", manifest.getDeobfName());
					result.add("content", page(text, args));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_manifest_info")
				.title("Manifest summary")
				.description("Parse AndroidManifest.xml: package, versions, SDK levels, permissions, application class, "
						+ "main activity and components (activities, services, receivers, providers) with exported flags "
						+ "and intent filters. Component class ids included when class exists in project.")
				.input(Schema.object()
						.optional("component_type", Schema.enumOf("Return only components of this type", "all",
								"all", "activity", "service", "receiver", "provider"))
						.build())
				.output(Schema.object().open()
						.required("package", Schema.nullableString("Application package"))
						.required("version_code", Schema.nullableString("Version code"))
						.required("version_name", Schema.nullableString("Version name"))
						.required("min_sdk", Schema.nullableString("minSdkVersion"))
						.required("target_sdk", Schema.nullableString("targetSdkVersion"))
						.required("application", Schema.anyObject("Application element info"))
						.required("main_activity", Schema.nullableString("Launcher activity class name"))
						.required("main_activity_class_id", Schema.nullableString("Class id of launcher activity"))
						.required("permissions", Schema.stringArray("Requested permissions"))
						.required("components", Schema.array(Schema.anyObject("Component"), "Components"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx,
						(lease, resolver) -> manifestInfo(lease, args.getString("component_type"))))
				.build());

		registry.register(McpTool.builder("get_main_activity_class")
				.title("Main activity")
				.description("Find launcher activity from AndroidManifest.xml and return its class with decompiled source (paged)")
				.input(textPageParams(Schema.object()).build())
				.output(Schema.object().open()
						.required("main_activity", Schema.string("Activity name from manifest"))
						.required("class", NodeJson.classSummarySchema())
						.required("code_class_id", Schema.string("Top-level class whose code contains the source"))
						.required("range_start_line", Schema.integer("First line of class in code"))
						.required("range_end_line", Schema.integer("Last line of class in code"))
						.required("source", NodeJson.pageSchema("Source page"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JsonObject info = manifestInfo(lease, "activity");
					if (info.get("main_activity").isJsonNull()) {
						throw ToolException.notFound("Launcher activity not found in AndroidManifest.xml");
					}
					String mainActivity = info.get("main_activity").getAsString();
					if (info.get("main_activity_class_id").isJsonNull()) {
						throw ToolException.notFound("Main activity class not found in project: " + mainActivity);
					}
					JavaClass cls = resolver.resolveClass(info.get("main_activity_class_id").getAsString());
					JsonObject result = ClassTools.classSource(lease, cls, args);
					result.addProperty("main_activity", mainActivity);
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_all_resource_file_names")
				.title("List resources")
				.description("List resource files in the project (paged). Values files from resources.arsc "
						+ "(e.g. 'res/values/strings.xml') are listed when 'include_arsc_values' is true.")
				.input(Schema.object()
						.optional("name_filter", Schema.string("Case-insensitive substring of file name"))
						.optional("include_arsc_values", Schema.bool("Decode resources.arsc and list generated values files", false))
						.optional("offset", offsetParam())
						.optional("limit", limitParam(200, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("files", Schema.array(Schema.object().open()
								.required("name", Schema.string("File name to use in get_resource_file"))
								.required("original_name", Schema.nullableString("Original name if renamed"))
								.required("type", Schema.string("Resource type"))
								.build(), "Files"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					String filter = args.getOptString("name_filter");
					String filterLower = filter == null ? null : filter.toLowerCase(Locale.ROOT);
					List<JsonObject> files = new ArrayList<>();
					JadxDecompiler decompiler = lease.getDecompiler();
					for (ResourceFile rf : decompiler.getResources()) {
						String name = rf.getDeobfName();
						if (filterLower == null || name.toLowerCase(Locale.ROOT).contains(filterLower)) {
							JsonObject obj = new JsonObject();
							obj.addProperty("name", name);
							obj.addProperty("original_name", name.equals(rf.getOriginalName()) ? null : rf.getOriginalName());
							obj.addProperty("type", rf.getType().name());
							files.add(obj);
						}
					}
					if (args.getBool("include_arsc_values", false)) {
						ResContainer arsc = getArscContent(lease);
						if (arsc != null) {
							for (ResContainer sub : arsc.getSubFiles()) {
								if (filterLower == null || sub.getName().toLowerCase(Locale.ROOT).contains(filterLower)) {
									JsonObject obj = new JsonObject();
									obj.addProperty("name", sub.getName());
									obj.add("original_name", null);
									obj.addProperty("type", "ARSC_VALUES");
									files.add(obj);
								}
							}
						}
					}
					files.sort(Comparator.comparing(o -> o.get("name").getAsString()));
					JsonObject result = new JsonObject();
					result.add("files", pageList(result, files, args, 200, o -> o));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_resource_file")
				.title("Resource content")
				.description("Get resource file content. Binary XML and resources.arsc are decoded to text (paged by lines). "
						+ "Values files generated from resources.arsc (e.g. 'res/values/strings.xml') are supported. "
						+ "For binary files returns size and SHA-256, optionally base64 (up to 1 MiB).")
				.input(textPageParams(Schema.object()
						.required("file_name", Schema.nonEmptyString("Resource name from get_all_resource_file_names"))
						.optional("include_base64", Schema.bool("Return base64 content for binary files", false)))
								.build())
				.output(Schema.object().open()
						.required("file", Schema.string("Resource name"))
						.required("type", Schema.string("Resource type"))
						.required("kind", Schema.enumOf("Content kind", null, "text", "binary"))
						.optional("content", NodeJson.pageSchema("Text page"))
						.optional("size", Schema.integer("Binary size in bytes"))
						.optional("sha256", Schema.string("SHA-256 of binary content"))
						.optional("base64", Schema.string("Base64 binary content"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> getResourceFile(lease, args)))
				.build());

		registry.register(McpTool.builder("get_strings")
				.title("String resources")
				.description("Get string resources (name and value) from decoded resources.arsc, paged. "
						+ "Use 'qualifier' for localized values, e.g. 'zh-rCN' reads res/values-zh-rCN/strings.xml.")
				.input(Schema.object()
						.optional("filter", Schema.string("Case-insensitive substring of string name or value"))
						.optional("qualifier", Schema.string("Resource qualifier (empty for default values)", ""))
						.optional("offset", offsetParam())
						.optional("limit", limitParam(200, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("file", Schema.string("Values file name"))
						.required("strings", Schema.array(Schema.object().open()
								.required("name", Schema.string("Resource name"))
								.required("value", Schema.string("String value"))
								.build(), "Strings"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> getStrings(lease, args)))
				.build());
	}

	static ResourceFile getManifest(JadxDecompiler decompiler) throws ToolException {
		ResourceFile manifest = AndroidManifestParser.getAndroidManifest(decompiler.getResources());
		if (manifest == null) {
			throw ToolException.notFound("AndroidManifest.xml not found (input is not an APK/AAB?)");
		}
		return manifest;
	}

	static String loadText(ResourceFile rf) throws ToolException {
		ResContainer content = rf.loadContent();
		if (content.getDataType() == ResContainer.DataType.TEXT || content.getDataType() == ResContainer.DataType.RES_TABLE) {
			return content.getText().getCodeStr();
		}
		throw new ToolException(ToolErrorCode.UNSUPPORTED, "Resource is not a text file: " + rf.getDeobfName());
	}

	private static @Nullable ResContainer getArscContent(ProjectLease lease) {
		JadxDecompiler decompiler = lease.getDecompiler();
		RootNode root = decompiler.getRoot();
		synchronized (ARSC_CACHE) {
			SoftReference<ResContainer> ref = ARSC_CACHE.get(root);
			ResContainer cached = ref == null ? null : ref.get();
			if (cached != null) {
				return cached;
			}
		}
		for (ResourceFile rf : decompiler.getResources()) {
			if (rf.getType() == ResourceType.ARSC) {
				ResContainer content = rf.loadContent();
				lease.check();
				if (content.getDataType() == ResContainer.DataType.RES_TABLE) {
					synchronized (ARSC_CACHE) {
						ARSC_CACHE.put(root, new SoftReference<>(content));
					}
					return content;
				}
			}
		}
		return null;
	}

	static @Nullable String findArscValuesText(ProjectLease lease, String name) {
		ResContainer arsc = getArscContent(lease);
		if (arsc == null) {
			return null;
		}
		for (ResContainer sub : arsc.getSubFiles()) {
			if (sub.getName().equals(name) && sub.getDataType() == ResContainer.DataType.TEXT) {
				return sub.getText().getCodeStr();
			}
		}
		return null;
	}

	/**
	 * Text resources for search: decoded XML files and values from resources.arsc
	 */
	static List<String> listTextResourceNames(ProjectLease lease) {
		List<String> names = new ArrayList<>();
		for (ResourceFile rf : lease.getDecompiler().getResources()) {
			switch (rf.getType()) {
				case MANIFEST:
				case XML:
				case JSON:
				case TEXT:
				case HTML:
					names.add(rf.getDeobfName());
					break;
				default:
					break;
			}
		}
		ResContainer arsc = getArscContent(lease);
		if (arsc != null) {
			for (ResContainer sub : arsc.getSubFiles()) {
				names.add(sub.getName());
			}
		}
		return names;
	}

	static @Nullable String loadTextByName(ProjectLease lease, String name) {
		for (ResourceFile rf : lease.getDecompiler().getResources()) {
			if (rf.getDeobfName().equals(name)) {
				ResContainer content = rf.loadContent();
				if (content.getDataType() == ResContainer.DataType.TEXT) {
					return content.getText().getCodeStr();
				}
				return null;
			}
		}
		return findArscValuesText(lease, name);
	}

	private static JsonObject getResourceFile(ProjectLease lease, ToolArgs args) throws ToolException {
		String name = args.getRequiredString("file_name").replace('\\', '/');
		ResourceFile found = null;
		for (ResourceFile rf : lease.getDecompiler().getResources()) {
			if (rf.getDeobfName().equals(name) || rf.getOriginalName().equals(name)) {
				found = rf;
				break;
			}
		}
		JsonObject result = new JsonObject();
		if (found == null) {
			String valuesText = findArscValuesText(lease, name);
			if (valuesText == null) {
				throw ToolException.notFound("Resource not found: " + name + ". Use get_all_resource_file_names.");
			}
			result.addProperty("file", name);
			result.addProperty("type", "ARSC_VALUES");
			result.addProperty("kind", "text");
			result.add("content", page(new CodeText(valuesText), args));
			return result;
		}
		result.addProperty("file", found.getDeobfName());
		result.addProperty("type", found.getType().name());
		ResContainer content = found.loadContent();
		lease.check();
		switch (content.getDataType()) {
			case TEXT:
			case RES_TABLE:
				result.addProperty("kind", "text");
				result.add("content", page(new CodeText(content.getText().getCodeStr()), args));
				return result;

			case DECODED_DATA:
				addBinary(result, content.getDecodedData(), args);
				return result;

			case RES_LINK:
			default:
				byte[] data = readRaw(found);
				if (isTextType(found.getType()) && isProbablyText(data)) {
					result.addProperty("kind", "text");
					result.add("content", page(new CodeText(new String(data, StandardCharsets.UTF_8)), args));
					return result;
				}
				addBinary(result, data, args);
				return result;
		}
	}

	private static boolean isTextType(ResourceType type) {
		switch (type) {
			case XML:
			case JSON:
			case TEXT:
			case HTML:
			case UNKNOWN:
				return true;
			default:
				return false;
		}
	}

	private static boolean isProbablyText(byte[] data) {
		int len = Math.min(data.length, 8192);
		for (int i = 0; i < len; i++) {
			if (data[i] == 0) {
				return false;
			}
		}
		return true;
	}

	private static byte[] readRaw(ResourceFile rf) throws ToolException {
		try {
			return ResourcesLoader.decodeStream(rf, (size, is) -> {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				is.transferTo(out);
				return out.toByteArray();
			});
		} catch (Exception e) {
			throw new ToolException(ToolErrorCode.OPERATION_FAILED, "Failed to read resource: " + rf.getDeobfName(), e);
		}
	}

	private static void addBinary(JsonObject result, byte[] data, ToolArgs args) {
		result.addProperty("kind", "binary");
		result.addProperty("size", data.length);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			StringBuilder sb = new StringBuilder();
			for (byte b : md.digest(data)) {
				sb.append(String.format("%02x", b));
			}
			result.addProperty("sha256", sb.toString());
		} catch (Exception e) {
			// ignore
		}
		if (args.getBool("include_base64", false)) {
			if (data.length <= MAX_BASE64_BYTES) {
				result.addProperty("base64", Base64.getEncoder().encodeToString(data));
			} else {
				result.addProperty("message", "File too big for base64 output (limit " + MAX_BASE64_BYTES + " bytes)");
			}
		}
	}

	private static JsonObject getStrings(ProjectLease lease, ToolArgs args) throws ToolException {
		String qualifier = args.getOptString("qualifier");
		String fileName = qualifier == null ? "res/values/strings.xml" : "res/values-" + qualifier + "/strings.xml";
		String text = findArscValuesText(lease, fileName);
		if (text == null) {
			throw ToolException.notFound("String resources not found: " + fileName);
		}
		Document doc = lease.getDecompiler().getArgs().getSecurity()
				.parseXml(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
		String filter = args.getOptString("filter");
		String filterLower = filter == null ? null : filter.toLowerCase(Locale.ROOT);
		List<JsonObject> list = new ArrayList<>();
		NodeList nodes = doc.getElementsByTagName("string");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element el = (Element) nodes.item(i);
			String name = el.getAttribute("name");
			String value = el.getTextContent();
			if (filterLower != null
					&& !name.toLowerCase(Locale.ROOT).contains(filterLower)
					&& !value.toLowerCase(Locale.ROOT).contains(filterLower)) {
				continue;
			}
			JsonObject obj = new JsonObject();
			obj.addProperty("name", name);
			obj.addProperty("value", value);
			list.add(obj);
		}
		JsonObject result = new JsonObject();
		result.addProperty("file", fileName);
		result.add("strings", pageList(result, list, args, 200, o -> o));
		return result;
	}

	static JsonObject manifestInfo(ProjectLease lease, @Nullable String componentType) throws ToolException {
		JadxDecompiler decompiler = lease.getDecompiler();
		String xml = loadText(getManifest(decompiler));
		Document doc = decompiler.getArgs().getSecurity().parseXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element manifest = doc.getDocumentElement();
		String pkg = attr(manifest, "package");
		JsonObject result = new JsonObject();
		result.addProperty("package", pkg);
		result.addProperty("version_code", androidAttr(manifest, "versionCode"));
		result.addProperty("version_name", androidAttr(manifest, "versionName"));
		Element usesSdk = firstChild(manifest, "uses-sdk");
		result.addProperty("min_sdk", usesSdk == null ? null : androidAttr(usesSdk, "minSdkVersion"));
		result.addProperty("target_sdk", usesSdk == null ? null : androidAttr(usesSdk, "targetSdkVersion"));

		JsonArray permissions = new JsonArray();
		for (Element perm : children(manifest, "uses-permission")) {
			String name = androidAttr(perm, "name");
			if (name != null) {
				permissions.add(name);
			}
		}
		result.add("permissions", permissions);

		Element app = firstChild(manifest, "application");
		JsonObject appInfo = new JsonObject();
		String mainActivity = null;
		JsonArray components = new JsonArray();
		if (app != null) {
			String appClass = fullClassName(pkg, androidAttr(app, "name"));
			appInfo.addProperty("class_name", appClass);
			appInfo.addProperty("class_id", findClassId(decompiler, appClass));
			appInfo.addProperty("label", androidAttr(app, "label"));
			appInfo.addProperty("debuggable", androidAttr(app, "debuggable"));
			appInfo.addProperty("allow_backup", androidAttr(app, "allowBackup"));
			appInfo.addProperty("network_security_config", androidAttr(app, "networkSecurityConfig"));
			for (String tag : COMPONENT_TAGS) {
				String type = tag.equals("activity-alias") ? "activity" : tag;
				for (Element comp : children(app, tag)) {
					boolean launcher = isLauncher(comp);
					String clsName = fullClassName(pkg, androidAttr(comp, tag.equals("activity-alias") ? "targetActivity" : "name"));
					if (launcher && mainActivity == null && type.equals("activity")) {
						mainActivity = clsName;
					}
					if (componentType != null && !componentType.equals("all") && !componentType.equals(type)) {
						continue;
					}
					components.add(componentJson(decompiler, pkg, tag, type, comp, clsName, launcher));
				}
			}
		}
		result.add("application", appInfo);
		result.addProperty("main_activity", mainActivity);
		result.addProperty("main_activity_class_id", findClassId(decompiler, mainActivity));
		result.add("components", components);
		return result;
	}

	private static JsonObject componentJson(JadxDecompiler decompiler, @Nullable String pkg, String tag, String type,
			Element comp, @Nullable String clsName, boolean launcher) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type);
		obj.addProperty("tag", tag);
		obj.addProperty("name", fullClassName(pkg, androidAttr(comp, "name")));
		obj.addProperty("class_id", findClassId(decompiler, clsName));
		String exported = androidAttr(comp, "exported");
		JsonArray filters = new JsonArray();
		for (Element filter : children(comp, "intent-filter")) {
			JsonObject f = new JsonObject();
			f.add("actions", namesOf(filter, "action"));
			f.add("categories", namesOf(filter, "category"));
			JsonArray data = new JsonArray();
			for (Element d : children(filter, "data")) {
				JsonObject dataObj = new JsonObject();
				for (String attrName : new String[] { "scheme", "host", "port", "path", "pathPrefix", "pathPattern", "mimeType" }) {
					String value = androidAttr(d, attrName);
					if (value != null) {
						dataObj.addProperty(attrName, value);
					}
				}
				data.add(dataObj);
			}
			f.add("data", data);
			filters.add(f);
		}
		obj.addProperty("exported", exported);
		// before Android 12 components with intent filters were exported by default
		obj.addProperty("effectively_exported", exported != null ? Boolean.parseBoolean(exported) : !filters.isEmpty());
		obj.addProperty("permission", androidAttr(comp, "permission"));
		obj.addProperty("enabled", androidAttr(comp, "enabled"));
		if (tag.equals("provider")) {
			obj.addProperty("authorities", androidAttr(comp, "authorities"));
			obj.addProperty("grant_uri_permissions", androidAttr(comp, "grantUriPermissions"));
		}
		obj.addProperty("launcher", launcher);
		obj.add("intent_filters", filters);
		return obj;
	}

	private static boolean isLauncher(Element comp) {
		for (Element filter : children(comp, "intent-filter")) {
			boolean main = false;
			boolean launcher = false;
			for (Element action : children(filter, "action")) {
				if ("android.intent.action.MAIN".equals(androidAttr(action, "name"))) {
					main = true;
				}
			}
			for (Element category : children(filter, "category")) {
				String name = androidAttr(category, "name");
				if ("android.intent.category.LAUNCHER".equals(name) || "android.intent.category.LEANBACK_LAUNCHER".equals(name)) {
					launcher = true;
				}
			}
			if (main && launcher) {
				return true;
			}
		}
		return false;
	}

	private static JsonArray namesOf(Element parent, String tag) {
		JsonArray arr = new JsonArray();
		for (Element el : children(parent, tag)) {
			String name = androidAttr(el, "name");
			if (name != null) {
				arr.add(name);
			}
		}
		return arr;
	}

	private static @Nullable String findClassId(JadxDecompiler decompiler, @Nullable String clsName) {
		if (clsName == null) {
			return null;
		}
		JavaClass cls = decompiler.searchJavaClassByOrigFullName(clsName);
		if (cls == null) {
			cls = decompiler.searchJavaClassByAliasFullName(clsName);
		}
		return cls == null ? null : NodeIds.classId(cls);
	}

	private static @Nullable String fullClassName(@Nullable String pkg, @Nullable String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		if (name.startsWith(".") && pkg != null) {
			return pkg + name;
		}
		if (!name.contains(".") && pkg != null) {
			return pkg + '.' + name;
		}
		return name;
	}

	private static @Nullable String attr(Element el, String name) {
		return el.hasAttribute(name) ? el.getAttribute(name) : null;
	}

	private static @Nullable String androidAttr(Element el, String name) {
		if (el.hasAttributeNS(ANDROID_NS, name)) {
			return el.getAttributeNS(ANDROID_NS, name);
		}
		String prefixed = "android:" + name;
		if (el.hasAttribute(prefixed)) {
			return el.getAttribute(prefixed);
		}
		return null;
	}

	private static @Nullable Element firstChild(Element parent, String tag) {
		List<Element> list = children(parent, tag);
		return list.isEmpty() ? null : list.get(0);
	}

	private static List<Element> children(Element parent, String tag) {
		List<Element> list = new ArrayList<>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && tag.equals(node.getNodeName())) {
				list.add((Element) node);
			}
		}
		return list;
	}
}
