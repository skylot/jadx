package jadx.core.utils.kotlin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.Utils;

// TODO: parse data from d1 (protobuf encoded) to get original method names and other useful info
public class KotlinMetadataUtils {
	private static final Logger LOG = LoggerFactory.getLogger(KotlinMetadataUtils.class);

	private static final String KOTLIN_METADATA_ANNOTATION = "Lkotlin/Metadata;";
	private static final String KOTLIN_METADATA_D2_PARAMETER = "d2";

	/**
	 * Try to get class info from Kotlin Metadata annotation
	 */
	@Nullable
	public static ClsAliasPair getClassAlias(ClassNode cls) {
		IAnnotation metadataAnnotation = cls.getAnnotation(KOTLIN_METADATA_ANNOTATION);
		List<EncodedValue> d2Param = getParamAsList(metadataAnnotation, KOTLIN_METADATA_D2_PARAMETER);
		if (d2Param == null || d2Param.isEmpty()) {
			return null;
		}
		EncodedValue firstValue = d2Param.get(0);
		if (firstValue == null || firstValue.getType() != EncodedType.ENCODED_STRING) {
			return null;
		}
		try {
			String rawClassName = ((String) firstValue.getValue()).trim();
			if (rawClassName.isEmpty()) {
				return null;
			}
			String clsName = Utils.cleanObjectName(rawClassName);
			ClsAliasPair alias = splitAndCheckClsName(cls, clsName);
			if (alias != null) {
				RenameReasonAttr.forNode(cls).append("from Kotlin metadata");
				return alias;
			}
		} catch (Exception e) {
			LOG.error("Failed to parse kotlin metadata", e);
		}
		return null;
	}

	// Don't use ClassInfo facility to not pollute class into cache
	private static ClsAliasPair splitAndCheckClsName(ClassNode originCls, String fullClsName) {
		if (!NameMapper.isValidFullIdentifier(fullClsName)) {
			return null;
		}
		String pkg;
		String name;
		int dot = fullClsName.lastIndexOf('.');
		if (dot == -1) {
			pkg = "";
			name = fullClsName;
		} else {
			pkg = fullClsName.substring(0, dot);
			name = fullClsName.substring(dot + 1);
		}
		ClassInfo originClsInfo = originCls.getClassInfo();
		String originName = originClsInfo.getShortName();
		if (originName.equals(name)
				|| name.contains("$")
				|| !NameMapper.isValidIdentifier(name)
				|| countPkgParts(originClsInfo.getPackage()) != countPkgParts(pkg)
				|| pkg.startsWith("java.")) {
			return null;
		}
		ClassNode newClsNode = originCls.root().resolveClass(fullClsName);
		if (newClsNode != null) {
			// class with alias name already exist
			return null;
		}
		return new ClsAliasPair(pkg, name);
	}

	private static int countPkgParts(String pkg) {
		if (pkg.isEmpty()) {
			return 0;
		}
		int count = 1;
		int pos = 0;
		while (true) {
			pos = pkg.indexOf('.', pos);
			if (pos == -1) {
				return count;
			}
			pos++;
			count++;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<EncodedValue> getParamAsList(IAnnotation annotation, String paramName) {
		if (annotation == null) {
			return null;
		}
		EncodedValue encodedValue = annotation.getValues().get(paramName);
		if (encodedValue == null || encodedValue.getType() != EncodedType.ENCODED_ARRAY) {
			return null;
		}
		return (List<EncodedValue>) encodedValue.getValue();
	}
}
