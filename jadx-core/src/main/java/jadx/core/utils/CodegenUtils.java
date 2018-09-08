package jadx.core.utils;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;

public class CodegenUtils {

	public static void addComments(CodeWriter code, AttrNode node) {
		for (String comment : node.getAll(AType.COMMENTS)) {
			code.startLine("/* ").add(comment).add(" */");
		}
	}
}
