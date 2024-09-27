package jadx.core.dex.attributes.nodes;

import java.util.EnumSet;
import java.util.Set;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.MethodNode;

public class CodeFeaturesAttr implements IJadxAttribute {

	public enum CodeFeature {
		/**
		 * Code contains switch instruction
		 */
		SWITCH,
	}

	public static boolean contains(MethodNode mth, CodeFeature feature) {
		CodeFeaturesAttr codeFeaturesAttr = mth.get(AType.METHOD_CODE_FEATURES);
		if (codeFeaturesAttr == null) {
			return false;
		}
		return codeFeaturesAttr.getCodeFeatures().contains(feature);
	}

	public static void add(MethodNode mth, CodeFeature feature) {
		CodeFeaturesAttr codeFeaturesAttr = mth.get(AType.METHOD_CODE_FEATURES);
		if (codeFeaturesAttr == null) {
			codeFeaturesAttr = new CodeFeaturesAttr();
			mth.addAttr(codeFeaturesAttr);
		}
		codeFeaturesAttr.getCodeFeatures().add(feature);
	}

	private final Set<CodeFeature> codeFeatures = EnumSet.noneOf(CodeFeature.class);

	public Set<CodeFeature> getCodeFeatures() {
		return codeFeatures;
	}

	@Override
	public AType<CodeFeaturesAttr> getAttrType() {
		return AType.METHOD_CODE_FEATURES;
	}

	@Override
	public String toAttrString() {
		return "CodeFeatures{" + codeFeatures + '}';
	}
}
