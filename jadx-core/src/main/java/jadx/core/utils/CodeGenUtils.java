package jadx.core.utils;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.CodePosition;
import jadx.api.ICodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ICodeNode;

public class CodeGenUtils {

	public static void addComments(ICodeWriter code, IAttributeNode node) {
		List<String> comments = node.getAll(AType.COMMENTS);
		if (!comments.isEmpty()) {
			comments.stream().distinct()
					.forEach(comment -> code.startLine("/* ").addMultiLine(comment).add(" */"));
		}
		addCodeComments(code, node);
	}

	public static void addCodeComments(ICodeWriter code, @Nullable IAttributeNode node) {
		if (node == null) {
			return;
		}
		List<String> comments = node.getAll(AType.CODE_COMMENTS);
		if (comments.isEmpty()) {
			return;
		}
		if (node instanceof ICodeNode) {
			// for classes, fields and methods add on line before node declaration
			code.startLine();
		} else {
			code.add(' ');
		}
		if (comments.size() == 1) {
			String comment = comments.get(0);
			if (!comment.contains("\n")) {
				code.add("// ").add(comment);
				return;
			}
		}
		addMultiLineComment(code, comments);
	}

	private static void addMultiLineComment(ICodeWriter code, List<String> comments) {
		boolean first = true;
		String indent = "";
		Object lineAnn = null;
		for (String comment : comments) {
			for (String line : comment.split("\n")) {
				if (first) {
					first = false;
					StringBuilder buf = code.getRawBuf();
					int startLinePos = buf.lastIndexOf(ICodeWriter.NL) + 1;
					indent = Utils.strRepeat(" ", buf.length() - startLinePos);
					if (code.isMetadataSupported()) {
						lineAnn = code.getRawAnnotations().get(new CodePosition(code.getLine()));
					}
				} else {
					code.newLine().add(indent);
					if (lineAnn != null) {
						code.attachLineAnnotation(lineAnn);
					}
				}
				code.add("// ").add(line);
			}
		}
	}

	public static void addRenamedComment(ICodeWriter code, AttrNode node, String origName) {
		code.startLine("/* renamed from: ").add(origName);
		RenameReasonAttr renameReasonAttr = node.get(AType.RENAME_REASON);
		if (renameReasonAttr != null) {
			code.add("  reason: ");
			code.add(renameReasonAttr.getDescription());
		}
		code.add(" */");
	}

	public static void addSourceFileInfo(ICodeWriter code, ClassNode node) {
		SourceFileAttr sourceFileAttr = node.get(AType.SOURCE_FILE);
		if (sourceFileAttr != null) {
			String fileName = sourceFileAttr.getFileName();
			String topClsName = node.getTopParentClass().getClassInfo().getShortName();
			if (topClsName.contains(fileName)) {
				// ignore similar name
				return;
			}
			code.startLine("/* compiled from: ").add(fileName).add(" */");
		}
	}

	public static CodeVar getCodeVar(RegisterArg arg) {
		SSAVar svar = arg.getSVar();
		if (svar != null) {
			return svar.getCodeVar();
		}
		return null;
	}

	private CodeGenUtils() {
	}
}
