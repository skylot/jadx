package jadx.core.utils;

import java.util.List;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;

public class CodegenUtils {

	public static void addComments(CodeWriter code, AttrNode node) {
		List<String> comments = node.getAll(AType.COMMENTS);
		if (!comments.isEmpty()) {
			comments.stream().distinct()
					.forEach(comment -> code.startLine("/* ").addMultiLine(comment).add(" */"));
		}
	}
}
