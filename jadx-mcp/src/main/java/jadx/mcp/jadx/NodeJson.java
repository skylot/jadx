package jadx.mcp.jadx;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.mcp.tools.Schema;

/**
 * JSON views of jadx nodes and matching output schemas
 */
public final class NodeJson {

	private NodeJson() {
	}

	public static JsonObject classSummary(JavaClass cls) {
		ClassNode clsNode = cls.getClassNode();
		ClassInfo info = clsNode.getClassInfo();
		JsonObject obj = new JsonObject();
		obj.addProperty("id", info.getRawName());
		obj.addProperty("name", info.getAliasFullName());
		obj.addProperty("original_name", info.getFullName());
		obj.addProperty("package", info.getAliasPkg());
		obj.addProperty("is_inner", info.isInner());
		obj.addProperty("renamed", info.hasAlias());
		obj.addProperty("access", access(clsNode.getAccessFlags()));
		return obj;
	}

	public static JsonObject classSummarySchema() {
		return Schema.object().open()
				.required("id", Schema.string("Stable class id (original raw name, '$' for inner classes)"))
				.required("name", Schema.string("Current full name (with renames and deobfuscation)"))
				.required("original_name", Schema.string("Original full name"))
				.required("package", Schema.string("Current package"))
				.required("is_inner", Schema.bool("Inner class"))
				.required("renamed", Schema.bool("Class has alias (renamed by user or deobfuscation)"))
				.required("access", Schema.string("Access modifiers"))
				.build();
	}

	public static JsonObject methodSummary(JavaMethod mth) {
		MethodNode mthNode = mth.getMethodNode();
		MethodInfo info = mthNode.getMethodInfo();
		JsonObject obj = new JsonObject();
		obj.addProperty("id", NodeIds.methodId(mth));
		obj.addProperty("class_id", NodeIds.classId(mth.getDeclaringClass()));
		obj.addProperty("name", info.getAlias());
		obj.addProperty("original_name", info.getName());
		obj.addProperty("descriptor", info.getShortId());
		obj.addProperty("signature", javaSignature(mth));
		obj.addProperty("return_type", typeStr(mth.getReturnType()));
		JsonArray params = new JsonArray();
		for (ArgType arg : mth.getArguments()) {
			params.add(typeStr(arg));
		}
		obj.add("parameter_types", params);
		obj.addProperty("access", access(mth.getAccessFlags()));
		obj.addProperty("is_constructor", mth.isConstructor());
		obj.addProperty("renamed", info.hasAlias());
		return obj;
	}

	public static JsonObject methodSummarySchema() {
		return Schema.object().open()
				.required("id", Schema.string("Stable method id 'classId->name(descriptor)'"))
				.required("class_id", Schema.string("Declaring class id"))
				.required("name", Schema.string("Current name"))
				.required("original_name", Schema.string("Original name"))
				.required("descriptor", Schema.string("Name with JVM descriptor"))
				.required("signature", Schema.string("Java style declaration"))
				.required("return_type", Schema.string("Return type"))
				.required("parameter_types", Schema.stringArray("Parameter types"))
				.required("access", Schema.string("Access modifiers"))
				.required("is_constructor", Schema.bool("Constructor"))
				.required("renamed", Schema.bool("Has alias"))
				.build();
	}

	public static JsonObject fieldSummary(JavaField fld) {
		FieldInfo info = fld.getFieldNode().getFieldInfo();
		JsonObject obj = new JsonObject();
		obj.addProperty("id", NodeIds.fieldId(fld));
		obj.addProperty("class_id", NodeIds.classId(fld.getDeclaringClass()));
		obj.addProperty("name", info.getAlias());
		obj.addProperty("original_name", info.getName());
		obj.addProperty("type", typeStr(fld.getType()));
		obj.addProperty("access", access(fld.getAccessFlags()));
		obj.addProperty("renamed", info.hasAlias());
		return obj;
	}

	public static JsonObject fieldSummarySchema() {
		return Schema.object().open()
				.required("id", Schema.string("Stable field id 'classId->name:type'"))
				.required("class_id", Schema.string("Declaring class id"))
				.required("name", Schema.string("Current name"))
				.required("original_name", Schema.string("Original name"))
				.required("type", Schema.string("Field type"))
				.required("access", Schema.string("Access modifiers"))
				.required("renamed", Schema.bool("Has alias"))
				.build();
	}

	public static JsonObject packageSummary(JavaPackage pkg) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", pkg.getRawFullName());
		obj.addProperty("name", pkg.getFullName());
		obj.addProperty("class_count", pkg.getClassesNoDup().size());
		obj.addProperty("renamed", !pkg.getRawFullName().equals(pkg.getFullName()));
		return obj;
	}

	public static JsonObject packageSummarySchema() {
		return Schema.object().open()
				.required("id", Schema.string("Stable package id (original name)"))
				.required("name", Schema.string("Current package name"))
				.required("class_count", Schema.integer("Top-level classes in package"))
				.required("renamed", Schema.bool("Package renamed"))
				.build();
	}

	public static JsonObject variableSummary(JavaVariable var) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", NodeIds.variableId(var));
		obj.addProperty("short_id", NodeIds.varShortId(var.getReg(), var.getSsa()));
		obj.addProperty("name", var.getName());
		obj.addProperty("type", typeStr(var.getType()));
		return obj;
	}

	public static JsonObject variableSummarySchema() {
		return Schema.object().open()
				.required("id", Schema.string("Stable variable id 'methodId#rNvM'"))
				.required("short_id", Schema.string("Register and SSA version 'rNvM'"))
				.required("name", Schema.nullableString("Current variable name"))
				.required("type", Schema.string("Variable type"))
				.build();
	}

	/**
	 * Short reference to any node
	 */
	public static JsonObject nodeRef(JavaNode node) {
		JsonObject obj = new JsonObject();
		obj.addProperty("kind", NodeIds.kind(node));
		obj.addProperty("id", NodeIds.id(node));
		obj.addProperty("name", displayName(node));
		JavaClass declCls;
		if (node instanceof JavaClass) {
			declCls = (JavaClass) node;
		} else if (node instanceof JavaPackage) {
			declCls = null;
		} else {
			declCls = node.getDeclaringClass();
		}
		obj.addProperty("class_id", declCls == null ? null : NodeIds.classId(declCls));
		return obj;
	}

	public static JsonObject nodeRefSchema() {
		return Schema.object().open()
				.required("kind", Schema.enumOf("Node kind", null, "class", "method", "field", "package", "variable", "unknown"))
				.required("id", Schema.string("Stable node id"))
				.required("name", Schema.nullableString("Current display name"))
				.required("class_id", Schema.nullableString("Declaring (or own) class id"))
				.build();
	}

	public static JsonObject nullableRefSchema(String description) {
		JsonObject schema = nodeRefSchema();
		JsonArray types = new JsonArray();
		types.add("object");
		types.add("null");
		schema.add("type", types);
		schema.addProperty("description", description);
		return schema;
	}

	public static @Nullable String displayName(JavaNode node) {
		if (node instanceof JavaClass) {
			return ((JavaClass) node).getClassNode().getClassInfo().getAliasFullName();
		}
		if (node instanceof JavaMethod) {
			JavaMethod mth = (JavaMethod) node;
			return mth.getDeclaringClass().getClassNode().getClassInfo().getAliasFullName() + '.' + javaSignature(mth);
		}
		if (node instanceof JavaField) {
			JavaField fld = (JavaField) node;
			return fld.getDeclaringClass().getClassNode().getClassInfo().getAliasFullName() + '.' + fld.getName();
		}
		if (node instanceof JavaVariable) {
			return ((JavaVariable) node).getName();
		}
		return node.getFullName();
	}

	/**
	 * Java style method declaration without modifiers: 'void run(int, String)'
	 */
	public static String javaSignature(JavaMethod mth) {
		StringBuilder sb = new StringBuilder();
		if (!mth.isConstructor() && !mth.isClassInit()) {
			sb.append(typeStr(mth.getReturnType())).append(' ');
		}
		sb.append(mth.getName()).append('(');
		boolean first = true;
		for (ArgType arg : mth.getArguments()) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(typeStr(arg));
		}
		sb.append(')');
		return sb.toString();
	}

	public static String typeStr(@Nullable ArgType type) {
		if (type == null) {
			return "?";
		}
		return type.toString();
	}

	public static String access(AccessInfo accessInfo) {
		return accessInfo.makeString(false).trim();
	}

	/**
	 * Paging result fields
	 */
	public static void addPaging(JsonObject obj, int total, int offset, int count) {
		obj.addProperty("total", total);
		obj.addProperty("offset", offset);
		obj.addProperty("count", count);
		boolean hasMore = offset + count < total;
		obj.addProperty("has_more", hasMore);
		obj.addProperty("next_offset", hasMore ? offset + count : null);
	}

	public static Schema.ObjectBuilder pagingSchema(Schema.ObjectBuilder builder) {
		return builder
				.required("total", Schema.integer("Total items matching the query"))
				.required("offset", Schema.integer("Offset of first returned item"))
				.required("count", Schema.integer("Items returned"))
				.required("has_more", Schema.bool("More items available"))
				.required("next_offset", Schema.nullableInteger("Offset for next page, null if no more items"));
	}

	public static JsonObject pageSchema(String description) {
		return Schema.object().open()
				.description(description)
				.required("text", Schema.string("Text of returned lines"))
				.required("start_line", Schema.integer("First returned line (1-based)"))
				.required("end_line", Schema.integer("Last returned line"))
				.required("total_lines", Schema.integer("Total lines in text"))
				.required("truncated", Schema.bool("Not all requested lines returned (limit reached)"))
				.required("next_start_line", Schema.nullableInteger("Pass as 'start_line' to read next part"))
				.build();
	}
}
