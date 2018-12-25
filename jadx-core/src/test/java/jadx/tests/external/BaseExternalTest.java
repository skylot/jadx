package jadx.tests.external;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JadxInternalAccess;
import jadx.api.JavaClass;
import jadx.core.codegen.CodeGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public abstract class BaseExternalTest extends IntegrationTest {
	private static final Logger LOG = LoggerFactory.getLogger(BaseExternalTest.class);

	protected abstract String getSamplesDir();

	protected JadxArgs prepare(String inputFile) {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(new File(getSamplesDir(), inputFile));
		args.setOutDir(new File("../jadx-external-tests-tmp"));
		return args;
	}

	protected void decompile(JadxArgs jadxArgs) {
		decompile(jadxArgs, null, null);
	}

	protected void decompile(JadxArgs jadxArgs, String clsPatternStr) {
		decompile(jadxArgs, clsPatternStr, null);
	}

	protected void decompile(JadxArgs jadxArgs, @Nullable String clsPatternStr, @Nullable String mthPatternStr) {
		JadxDecompiler jadx = new JadxDecompiler(jadxArgs);
		jadx.load();

		if (clsPatternStr == null) {
			processAll(jadx);
//			jadx.saveSources();
		} else {
			processByPatterns(jadx, clsPatternStr, mthPatternStr);
		}
		printErrorReport(jadx);
	}

	private void processAll(JadxDecompiler jadx) {
		for (JavaClass javaClass : jadx.getClasses()) {
			javaClass.decompile();
		}
	}

	private void processByPatterns(JadxDecompiler jadx, String clsPattern, @Nullable String mthPattern) {
		List<IDexTreeVisitor> passes = JadxInternalAccess.getPassList(jadx);
		RootNode root = JadxInternalAccess.getRoot(jadx);
		int processed = 0;
		for (ClassNode classNode : root.getClasses(true)) {
			String clsFullName = classNode.getClassInfo().getFullName();
			if (isMatch(clsFullName, clsPattern)) {
				if (processCls(mthPattern, passes, classNode)) {
					processed++;
				}
			}
		}
		assertThat("No classes processed", processed, greaterThan(0));
	}

	private boolean processCls(@Nullable String mthPattern, List<IDexTreeVisitor> passes, ClassNode classNode) {
		classNode.load();
		boolean decompile = false;
		if (mthPattern == null) {
			decompile = true;
		} else {
			for (MethodNode mth : classNode.getMethods()) {
				if (isMthMatch(mth, mthPattern)) {
					decompile = true;
					break;
				}
			}
		}
		if (!decompile) {
			return false;
		}
		for (IDexTreeVisitor visitor : passes) {
			DepthTraversal.visit(visitor, classNode);
		}
		try {
			new CodeGen().visit(classNode);
		} catch (Exception e) {
			throw new JadxRuntimeException("Codegen failed", e);
		}
		LOG.warn("\n Print class: {}, {}", classNode.getFullName(), classNode.dex());
		if (mthPattern != null) {
			printMethods(classNode, mthPattern);
		} else {
			LOG.info("Code: \n{}", classNode.getCode());
		}
		checkCode(classNode);
		return true;
	}

	private boolean isMthMatch(MethodNode mth, String mthPattern) {
		String shortId = mth.getMethodInfo().getShortId();
		return isMatch(shortId, mthPattern);
	}

	private boolean isMatch(String str, String pattern) {
		if (str.equals(pattern)) {
			return true;
		}
		return str.startsWith(pattern);
	}

	private void printMethods(ClassNode classNode, @NotNull String mthPattern) {
		String code = classNode.getCode().getCodeStr();
		if (code == null) {
			return;
		}
		String[] lines = code.split(CodeWriter.NL);
		for (MethodNode mth : classNode.getMethods()) {
			if (isMthMatch(mth, mthPattern)) {
				int decompiledLine = mth.getDecompiledLine() - 1;
				StringBuilder mthCode = new StringBuilder();
				int startLine = getCommentLinesCount(lines, decompiledLine);
				int brackets = 0;
				for (int i = startLine; i > 0 && i < lines.length; i++) {
					String line = lines[i];
					mthCode.append(line).append(CodeWriter.NL);
					if (i >= decompiledLine) {
						brackets += StringUtils.countMatches(line, '{');
						brackets -= StringUtils.countMatches(line, '}');
						if (brackets <= 0) {
							break;
						}
					}
				}
				LOG.info("Print method: {}\n{}", mth.getMethodInfo().getShortId(), mthCode);
			}
		}
	}

	protected int getCommentLinesCount(String[] lines, int line) {
		for (int i = line - 1; i > 0 && i < lines.length; i--) {
			String str = lines[i];
			if (str.isEmpty() || str.equals(CodeWriter.NL)) {
				return i + 1;
			}
		}
		return 0;
	}

	private void printErrorReport(JadxDecompiler jadx) {
		jadx.printErrorsReport();
		assertThat(jadx.getErrorsCount(), is(0));
	}
}
