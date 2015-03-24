package jadx.core.dex.visitors;

import jadx.api.IJadxArgs;
import jadx.core.codegen.TypeGen;
import jadx.core.deobf.Deobfuscator;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class RenameVisitor extends AbstractVisitor {

	@NotNull
	private Deobfuscator deobfuscator;

	@Override
	public void init(RootNode root) {
		IJadxArgs args = root.getArgs();
		File deobfMapFile = new File(args.getOutDir(), "deobf_map.jobf");
		deobfuscator = new Deobfuscator(args, root.getDexNodes(), deobfMapFile);
		if (args.isDeobfuscationOn()) {
			// TODO: check classes for case sensitive names (issue #24)
			deobfuscator.execute();
		}
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

	private void checkFields(ClassNode cls) {
		Set<String> names = new HashSet<String>();
		for (FieldNode field : cls.getFields()) {
			FieldInfo fieldInfo = field.getFieldInfo();
			if (!names.add(fieldInfo.getAlias())) {
				fieldInfo.setAlias(deobfuscator.makeFieldAlias(field));
			}
		}
	}

	private void checkMethods(ClassNode cls) {
		Set<String> names = new HashSet<String>();
		for (MethodNode mth : cls.getMethods()) {
			if (mth.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			MethodInfo methodInfo = mth.getMethodInfo();
			String signature = makeMethodSignature(methodInfo);
			if (!names.add(signature)) {
				methodInfo.setAlias(deobfuscator.makeMethodAlias(mth));
			}
		}
	}

	private static String makeMethodSignature(MethodInfo methodInfo) {
		StringBuilder signature = new StringBuilder();
		signature.append(methodInfo.getAlias());
		signature.append('(');
		for (ArgType arg : methodInfo.getArgumentsTypes()) {
			signature.append(TypeGen.signature(arg));
		}
		signature.append(')');
		return signature.toString();
	}
}
