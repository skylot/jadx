package jadx.core.dex.visitors;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.api.JadxArgs;
import jadx.core.Consts;
import jadx.core.deobf.Deobfuscator;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
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
		Path inputFilePath = firstInputFile.getFile().toPath();

		String inputName = inputFilePath.getFileName().toString();
		inputName = inputName.substring(0, inputName.lastIndexOf('.'));

		Path deobfMapPath = inputFilePath.getParent().resolve(inputName + ".jobf");
		JadxArgs args = root.getArgs();
		deobfuscator = new Deobfuscator(args, dexNodes, deobfMapPath);
		boolean deobfuscationOn = args.isDeobfuscationOn();
		if (deobfuscationOn) {
			deobfuscator.execute();
		}
		checkClasses(root, args);
	}

	private void checkClasses(RootNode root, JadxArgs args) {
		List<ClassNode> classes = root.getClasses(true);
		for (ClassNode cls : classes) {
			checkClassName(cls, args);
			checkFields(cls, args);
			checkMethods(cls, args);
		}
		if (!args.isFsCaseSensitive() && args.isRenameCaseSensitive()) {
			Set<String> clsFullPaths = new HashSet<>(classes.size());
			for (ClassNode cls : classes) {
				ClassInfo clsInfo = cls.getClassInfo();
				ClassInfo aliasClsInfo = clsInfo.getAlias();
				if (!clsFullPaths.add(aliasClsInfo.getFullPath().toLowerCase())) {
					String newShortName = deobfuscator.getClsAlias(cls);
					clsInfo.renameShortName(newShortName);
					clsFullPaths.add(clsInfo.getAlias().getFullPath().toLowerCase());
				}
			}
		}
	}

	private void checkClassName(ClassNode cls, JadxArgs args) {
		ClassInfo classInfo = cls.getClassInfo();
		String clsName = classInfo.getAlias().getShortName();

		String newShortName = fixClsShortName(args, clsName);
		if (!newShortName.equals(clsName)) {
			classInfo.renameShortName(newShortName);
		}
		if (classInfo.getAlias().getPackage().isEmpty()) {
			classInfo.renamePkg(Consts.DEFAULT_PACKAGE_NAME);
		}
	}

	private String fixClsShortName(JadxArgs args, String clsName) {
		char firstChar = clsName.charAt(0);
		boolean renameValid = args.isRenameValid();
		if (Character.isDigit(firstChar) && renameValid) {
			return Consts.ANONYMOUS_CLASS_PREFIX + NameMapper.removeInvalidCharsMiddle(clsName);
		}
		if (firstChar == '$' && renameValid) {
			return 'C' + NameMapper.removeInvalidCharsMiddle(clsName);
		}
		String cleanClsName = args.isRenamePrintable()
				? NameMapper.removeInvalidChars(clsName, "C")
				: clsName;
		if (renameValid && !NameMapper.isValidIdentifier(cleanClsName)) {
			return 'C' + cleanClsName;
		}
		return cleanClsName;
	}

	private void checkFields(ClassNode cls, JadxArgs args) {
		Set<String> names = new HashSet<>();
		for (FieldNode field : cls.getFields()) {
			FieldInfo fieldInfo = field.getFieldInfo();
			String fieldName = fieldInfo.getAlias();
			if (!names.add(fieldName)
					|| (args.isRenameValid() && !NameMapper.isValidIdentifier(fieldName))
					|| (args.isRenamePrintable() && !NameMapper.isAllCharsPrintable(fieldName))) {
				deobfuscator.forceRenameField(field);
			}
		}
	}

	private void checkMethods(ClassNode cls, JadxArgs args) {
		for (MethodNode mth : cls.getMethods()) {
			String alias = mth.getAlias();
			if (args.isRenameValid() && !NameMapper.isValidIdentifier(alias)
					|| (args.isRenamePrintable() && !NameMapper.isAllCharsPrintable(alias))) {
				deobfuscator.forceRenameMethod(mth);
			}
		}
		Set<String> names = new HashSet<>();
		for (MethodNode mth : cls.getMethods()) {
			AccessInfo accessFlags = mth.getAccessFlags();
			if (accessFlags.isConstructor()
					|| accessFlags.isBridge()
					|| accessFlags.isSynthetic()
					|| mth.contains(AFlag.DONT_GENERATE) /* this flag not set yet */) {
				continue;
			}
			String signature = mth.getMethodInfo().makeSignature(true, false);
			if (!names.add(signature)) {
				deobfuscator.forceRenameMethod(mth);
			}
		}
	}
}
