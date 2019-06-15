package jadx.core.codegen.json;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.api.JadxArgs;
import jadx.core.codegen.json.mapping.JsonClsMapping;
import jadx.core.codegen.json.mapping.JsonFieldMapping;
import jadx.core.codegen.json.mapping.JsonMapping;
import jadx.core.codegen.json.mapping.JsonMthMapping;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class JsonMappingGen {
	private static final Logger LOG = LoggerFactory.getLogger(JsonMappingGen.class);

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
			.disableHtmlEscaping()
			.create();

	public static void dump(RootNode root) {
		JsonMapping mapping = new JsonMapping();
		fillMapping(mapping, root);

		JadxArgs args = root.getArgs();
		File outDirSrc = args.getOutDirSrc().getAbsoluteFile();
		File mappingFile = new File(outDirSrc, "mapping.json");
		FileUtils.makeDirsForFile(mappingFile);
		try (Writer writer = new FileWriter(mappingFile)) {
			GSON.toJson(mapping, writer);
			LOG.info("Save mappings to {}", mappingFile.getAbsolutePath());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to save mapping json", e);
		}
	}

	private static void fillMapping(JsonMapping mapping, RootNode root) {
		List<ClassNode> classes = root.getClasses(true);
		mapping.setClasses(new ArrayList<>(classes.size()));
		for (ClassNode cls : classes) {
			ClassInfo classInfo = cls.getClassInfo();
			JsonClsMapping jsonCls = new JsonClsMapping();
			jsonCls.setName(classInfo.getRawName());
			jsonCls.setAlias(classInfo.getAliasFullName());
			jsonCls.setInner(classInfo.isInner());
			jsonCls.setJson(cls.getTopParentClass().getClassInfo().getAliasFullPath() + ".json");
			if (classInfo.isInner()) {
				jsonCls.setTopClass(cls.getTopParentClass().getClassInfo().getFullName());
			}
			addFields(cls, jsonCls);
			addMethods(cls, jsonCls);
			mapping.getClasses().add(jsonCls);
		}
	}

	private static void addMethods(ClassNode cls, JsonClsMapping jsonCls) {
		List<MethodNode> methods = cls.getMethods();
		if (methods.isEmpty()) {
			return;
		}
		jsonCls.setMethods(new ArrayList<>(methods.size()));
		for (MethodNode method : methods) {
			JsonMthMapping jsonMethod = new JsonMthMapping();
			MethodInfo methodInfo = method.getMethodInfo();
			jsonMethod.setSignature(methodInfo.getShortId());
			jsonMethod.setName(methodInfo.getName());
			jsonMethod.setAlias(methodInfo.getAlias());
			jsonMethod.setOffset("0x" + Long.toHexString(method.getMethodCodeOffset()));
			jsonCls.getMethods().add(jsonMethod);
		}
	}

	private static void addFields(ClassNode cls, JsonClsMapping jsonCls) {
		List<FieldNode> fields = cls.getFields();
		if (fields.isEmpty()) {
			return;
		}
		jsonCls.setFields(new ArrayList<>(fields.size()));
		for (FieldNode field : fields) {
			JsonFieldMapping jsonField = new JsonFieldMapping();
			jsonField.setName(field.getName());
			jsonField.setAlias(field.getAlias());
			jsonCls.getFields().add(jsonField);
		}
	}

	private JsonMappingGen() {
	}
}
