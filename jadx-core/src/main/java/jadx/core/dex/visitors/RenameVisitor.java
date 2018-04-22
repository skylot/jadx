package jadx.core.dex.visitors;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import jadx.api.JadxArgs;
import jadx.core.Consts;
import jadx.core.deobf.Deobfuscator;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.FileUtils;
import jadx.core.utils.files.InputFile;

public class RenameVisitor extends AbstractVisitor {

	private Deobfuscator deobfuscator;

	@Override
	public void init(RootNode root) {
		List<DexNode> dexNodes = root.getDexNodes();
		if (dexNodes.isEmpty()) {
			return;
		}
		InputFile firstInputFile = dexNodes.get(0).getDexFile().getInputFile();
		String firstInputFileName = firstInputFile.getFile().getAbsolutePath();
		String inputPath = FilenameUtils.getFullPathNoEndSeparator(firstInputFileName);
		String inputName = FilenameUtils.getBaseName(firstInputFileName);

		File deobfMapFile = new File(inputPath, inputName + ".jobf");
		JadxArgs args = root.getArgs();
		deobfuscator = new Deobfuscator(args, dexNodes, deobfMapFile);
		boolean deobfuscationOn = args.isDeobfuscationOn();
		if (deobfuscationOn) {
			deobfuscator.execute();
		}
		boolean isCaseSensitive = FileUtils.isCaseSensitiveFS(new File(inputPath)); // args.getOutDir() - not set in gui
		checkClasses(root, isCaseSensitive);
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		checkFields(cls);
		checkMethods(cls);
		for (ClassNode inner : cls.getInnerClasses()) {
			visit(inner);
		}
		return false;
	}

	private void checkClasses(RootNode root, boolean caseSensitive) {
		Set<String> clsNames = new HashSet<>();
		for (ClassNode cls : root.getClasses(true)) {
			checkClassName(cls);
			if (!caseSensitive) {
				ClassInfo classInfo = cls.getClassInfo();
				String clsFileName = classInfo.getAlias().getFullPath();
				if (!clsNames.add(clsFileName.toLowerCase())) {
					String newShortName = deobfuscator.getClsAlias(cls);
					String newFullName = classInfo.makeFullClsName(newShortName, true);
					classInfo.rename(cls.root(), newFullName);
					clsNames.add(classInfo.getAlias().getFullPath().toLowerCase());
				}
			}
		}
	}

	private void checkClassName(ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		String clsName = classInfo.getAlias().getShortName();
		String newShortName = null;
		char firstChar = clsName.charAt(0);
		if (Character.isDigit(firstChar)) {
			newShortName = Consts.ANONYMOUS_CLASS_PREFIX + clsName;
		} else if (firstChar == '$') {
			newShortName = "C" + clsName;
		}
		if (newShortName != null) {
			classInfo.rename(cls.root(), classInfo.makeFullClsName(newShortName, true));
		}
		if (classInfo.getAlias().getPackage().isEmpty()) {
			String fullName = classInfo.makeFullClsName(classInfo.getAlias().getShortName(), true);
			String newFullName = Consts.DEFAULT_PACKAGE_NAME + "." + fullName;
			classInfo.rename(cls.root(), newFullName);
		}
	}

	private void checkFields(ClassNode cls) {
		Set<String> names = new HashSet<>();
		for (FieldNode field : cls.getFields()) {
			FieldInfo fieldInfo = field.getFieldInfo();
			if (!names.add(fieldInfo.getAlias())) {
				deobfuscator.renameField(field);
			}
		}
	}

	private void checkMethods(ClassNode cls) {
		Set<String> names = new HashSet<>();
		for (MethodNode mth : cls.getMethods()) {
			if (mth.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			String signature = mth.getMethodInfo().makeSignature(false);
			if (!names.add(signature)) {
				deobfuscator.renameMethod(mth);
			}
		}
	}
}
