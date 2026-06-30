package jadx.core.codegen.utils;

import java.util.List;
import java.util.function.BiConsumer;
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
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.utils.Utils;

public class CodeGenUtils {

	public static void addErrorsAndComments(ICodeWriter code, NotificationAttrNode node) {
		addComments(code, node);
		addErrors(code, node);
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
		addCommentWithStyle(code, comment.getStyle(), comment.getComment());
	}

	public static void addJadxNodeComment(ICodeWriter code, NotificationAttrNode node,
			CommentsLevel level, BiConsumer<ICodeWriter, String> commentFunc) {
		if (node.checkCommentsLevel(level)) {
			code.startLine();
			addCommentWithStyle(code, CommentStyle.BLOCK_CONDENSED, (commentCode, newLinePrefix) -> {
				commentCode.add("JADX ").add(level.name()).add(": ");
				commentFunc.accept(commentCode, newLinePrefix);
			});
		}
	}

	public static void addJadxComment(ICodeWriter code, CommentsLevel level, String commentStr) {
		code.startLine();
		addCommentWithStyle(code, CommentStyle.BLOCK_CONDENSED, "JADX " + level.name() + ": " + commentStr);
	}

	private static void addCommentWithStyle(ICodeWriter code, CommentStyle style, String commentStr) {
		appendMultiLineString(code, "", style.getStart());
		appendMultiLineString(code, style.getOnNewLine(), commentStr);
		appendMultiLineString(code, "", style.getEnd());
	}

	/**
	 * Insert comment with function, use second arg as new line prefix
	 */
	private static void addCommentWithStyle(ICodeWriter code, CommentStyle style, BiConsumer<ICodeWriter, String> commentFunc) {
		appendMultiLineString(code, "", style.getStart());
		commentFunc.accept(code, style.getOnNewLine());
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

	public static void addClassRenamedComment(ICodeWriter code, ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		if (classInfo.hasAlias()) {
			addRenamedComment(code, cls, classInfo.getType().getObject());
		}
	}

	public static void addRenamedComment(ICodeWriter code, NotificationAttrNode node, String origName) {
		addJadxNodeComment(code, node, CommentsLevel.INFO, (commentCode, newLinePrefix) -> {
			commentCode.add("renamed from: ").add(origName);
			RenameReasonAttr renameReasonAttr = node.get(AType.RENAME_REASON);
			if (renameReasonAttr != null) {
				commentCode.add(", reason: ").add(renameReasonAttr.getDescription());
			}
		});
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
			addJadxComment(code, CommentsLevel.INFO, "compiled from: " + fileName);
		}
	}

	public static void addInputFileInfo(ICodeWriter code, ClassNode cls) {
		if (cls.checkCommentsLevel(CommentsLevel.INFO) && cls.getClsData() != null) {
			String inputFileName = cls.getClsData().getInputFileName();
			if (inputFileName != null) {
				ClassNode declCls = cls.getDeclaringClass();
				if (declCls != null
						&& declCls.getClsData() != null
						&& inputFileName.equals(declCls.getClsData().getInputFileName())) {
					// don't add same comment for inner classes
					return;
				}
				addJadxComment(code, CommentsLevel.INFO, "loaded from: " + inputFileName);
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
