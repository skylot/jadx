package jadx.core.utils.kotlin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;

// TODO: parse data from d1 (protobuf encoded) to get original method names and other useful info
public class KotlinMetadataUtils {
	private static final Logger LOG = LoggerFactory.getLogger(KotlinMetadataUtils.class);

	private static final String KOTLIN_METADATA_ANNOTATION = "Lkotlin/Metadata;";
	private static final String KOTLIN_METADATA_D2_PARAMETER = "d2";
	private static final String KOTLIN_METADATA_CLASSNAME_REGEX = "(L.*;)";

	/**
	 * Try to get class info from Kotlin Metadata annotation
	 */
	@Nullable
	public static ClassInfo getClassName(ClassNode cls) {
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
			if (rawClassName.matches(KOTLIN_METADATA_CLASSNAME_REGEX)) {
				return ClassInfo.fromName(cls.root(), rawClassName);
			}
		} catch (Exception e) {
			LOG.error("Failed to parse kotlin metadata", e);
		}
		return null;
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
