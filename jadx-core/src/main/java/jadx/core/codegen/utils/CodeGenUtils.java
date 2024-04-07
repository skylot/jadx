package jadx.core.codegen.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.api.CommentsLevel;
import jadx.api.ICodeWriter;
import jadx.api.data.CommentStyle;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxCommentsAttr;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.utils.Utils;

public class CodeGenUtils {

	public static void addErrorsAndComments(ICodeWriter code, NotificationAttrNode node) {
		addErrors(code, node);
		addComments(code, node);
	}

	public static void addErrors(ICodeWriter code, NotificationAttrNode node) {
		if (!node.checkCommentsLevel(CommentsLevel.ERROR)) {
			return;
		}
		List<JadxError> errors = node.getAll(AType.JADX_ERROR);
		if (!errors.isEmpty()) {
			errors.stream().distinct().sorted().forEach(err -> {
				addError(code, err.getError(), err.getCause());
			});
		}
	}

	public static void addError(ICodeWriter code, String errMsg, Throwable cause) {
		code.startLine("/*  JADX ERROR: ").add(errMsg);
		if (cause != null) {
			code.incIndent();
			Utils.appendStackTrace(code, cause);
			code.decIndent();
		}
		code.add("*/");
	}

	public static void addComments(ICodeWriter code, NotificationAttrNode node) {
		JadxCommentsAttr commentsAttr = node.get(AType.JADX_COMMENTS);
		if (commentsAttr != null) {
			commentsAttr.formatAndFilter(node.getCommentsLevel())
					.forEach(comment -> code.startLine("/* ").addMultiLine(comment).add(" */"));
		}
		addCodeComments(code, node, node);
	}

	public static void addCodeComments(ICodeWriter code, NotificationAttrNode parent, @Nullable IAttributeNode node) {
		if (node == null) {
			return;
		}
		if (parent.checkCommentsLevel(CommentsLevel.USER_ONLY)) {
			addCodeComments(code, node);
		}
	}

	private static void addCodeComments(ICodeWriter code, @Nullable IAttributeNode node) {
		if (node == null) {
			return;
		}
		boolean startNewLine = node instanceof ICodeNode; // add on same line for instructions
		for (CodeComment comment : node.getAll(AType.CODE_COMMENTS)) {
			addCodeComment(code, comment, startNewLine);
		}
	}

	private static void addCodeComment(ICodeWriter code, CodeComment comment, boolean startNewLine) {
		if (startNewLine) {
			code.startLine();
		} else {
			code.add(' ');
		}
		CommentStyle style = comment.getStyle();
		appendMultiLineString(code, "", style.getStart());
		appendMultiLineString(code, style.getOnNewLine(), comment.getComment());
		appendMultiLineString(code, "", style.getEnd());
	}

	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\\R");

	private static void appendMultiLineString(ICodeWriter code, String onNewLine, String str) {
		String[] lines = NEW_LINE_PATTERN.split(str);
		int linesCount = lines.length;
		if (linesCount == 0) {
			return;
		}
		code.add(lines[0]);
		for (int i = 1; i < linesCount; i++) {
			code.startLine(onNewLine);
			code.add(lines[i]);
		}
	}

	public static void addRenamedComment(ICodeWriter code, NotificationAttrNode node, String origName) {
		if (!node.checkCommentsLevel(CommentsLevel.INFO)) {
			return;
		}
		code.startLine("/* renamed from: ").add(origName);
		RenameReasonAttr renameReasonAttr = node.get(AType.RENAME_REASON);
		if (renameReasonAttr != null) {
			code.add(", reason: ").add(renameReasonAttr.getDescription());
		}
		code.add(" */");
	}

	public static void addSourceFileInfo(ICodeWriter code, ClassNode node) {
		if (!node.checkCommentsLevel(CommentsLevel.INFO)) {
			return;
		}
		SourceFileAttr sourceFileAttr = node.get(JadxAttrType.SOURCE_FILE);
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

	public static void addInputFileInfo(ICodeWriter code, ClassNode node) {
		if (node.getClsData() != null && node.checkCommentsLevel(CommentsLevel.INFO)) {
			String inputFileName = node.getClsData().getInputFileName();
			if (inputFileName != null) {
				code.startLine("/* loaded from: ").add(inputFileName).add(" */");
			}
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
