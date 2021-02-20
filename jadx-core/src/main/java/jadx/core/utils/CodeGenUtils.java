package jadx.core.utils;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;

public class CodeGenUtils {

	public static void addComments(ICodeWriter code, AttrNode node) {
		List<String> comments = node.getAll(AType.COMMENTS);
		if (!comments.isEmpty()) {
			comments.stream().distinct()
					.forEach(comment -> code.startLine("/* ").addMultiLine(comment).add(" */"));
		}
		addCodeComments(code, node);
	}

	public static void addCodeComments(ICodeWriter code, @Nullable AttrNode node) {
		if (node == null) {
			return;
		}
		List<String> comments = node.getAll(AType.CODE_COMMENTS);
		if (!comments.isEmpty()) {
			if (node instanceof InsnNode) {
				code.add(' ');
			} else {
				code.startLine();
			}
			code.add("// ").add(Utils.listToString(comments, " "));
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
			code.startLine("/* compiled from: ").add(sourceFileAttr.getFileName()).add(" */");
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
