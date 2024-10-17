package jadx.tests.external;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CommentsLevel;
import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JadxInternalAccess;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.tests.api.utils.TestUtils;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public abstract class BaseExternalTest extends TestUtils {
	private static final Logger LOG = LoggerFactory.getLogger(BaseExternalTest.class);

	protected JadxDecompiler decompiler;

	protected abstract String getSamplesDir();

	protected JadxArgs prepare(String inputFile) {
		return prepare(new File(getSamplesDir(), inputFile));
	}

	protected JadxArgs prepare(File input) {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(input);
		args.setOutDir(new File("../jadx-external-tests-tmp"));
		args.setSkipFilesSave(true);
		args.setSkipResources(true);
		args.setShowInconsistentCode(true);
		args.setCommentsLevel(CommentsLevel.DEBUG);
		return args;
	}

	protected JadxDecompiler decompile(JadxArgs jadxArgs) {
		return decompile(jadxArgs, null, null);
	}

	protected JadxDecompiler decompile(JadxArgs jadxArgs, String clsPatternStr) {
		return decompile(jadxArgs, clsPatternStr, null);
	}

	protected JadxDecompiler decompile(JadxArgs jadxArgs, @Nullable String clsPatternStr, @Nullable String mthPatternStr) {
		decompiler = new JadxDecompiler(jadxArgs);
		decompiler.load();

		if (clsPatternStr == null) {
			decompiler.save();
		} else {
			processByPatterns(decompiler, clsPatternStr, mthPatternStr);
		}
		printErrorReport(decompiler);
		return decompiler;
	}

	private void processByPatterns(JadxDecompiler jadx, String clsPattern, @Nullable String mthPattern) {
		RootNode root = JadxInternalAccess.getRoot(jadx);
		int processed = 0;
		for (ClassNode classNode : root.getClasses(true)) {
			String clsFullName = classNode.getClassInfo().getFullName();
			if (clsFullName.equals(clsPattern)) {
				if (processCls(mthPattern, classNode)) {
					processed++;
				}
			}
		}
		assertThat(processed).as("No classes processed").isGreaterThan(0);
	}

	private boolean processCls(@Nullable String mthPattern, ClassNode classNode) {
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
		try {
			classNode.decompile();
		} catch (Exception e) {
			throw new JadxRuntimeException("Class process failed", e);
		}
		LOG.info("----------------------------------------------------------------");
		LOG.info("Print class: {} from: {}", classNode.getFullName(), classNode.getInputFileName());
		if (mthPattern != null) {
			printMethods(classNode, mthPattern);
		} else {
			LOG.info("Code: \n{}", classNode.getCode());
		}
		checkCode(classNode, false);
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
		ICodeInfo codeInfo = classNode.getCode();
		String code = codeInfo.getCodeStr();
		if (code == null) {
			return;
		}
		String dashLine = "======================================================================================";
		for (MethodNode mth : classNode.getMethods()) {
			if (isMthMatch(mth, mthPattern)) {
				LOG.info("Print method: {}\n{}\n{}\n{}",
						mth.getMethodInfo().getShortId(),
						dashLine,
						mth.getCodeStr(),
						dashLine);
			}
		}
	}

	private void printErrorReport(JadxDecompiler jadx) {
		jadx.printErrorsReport();
		assertThat(jadx.getErrorsCount()).isEqualTo(0);
	}
}
