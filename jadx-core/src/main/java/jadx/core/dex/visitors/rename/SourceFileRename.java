package jadx.core.dex.visitors.rename;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.BetterName;
import jadx.core.utils.StringUtils;

public class SourceFileRename {

	public static void process(RootNode root) {
		if (root.getArgs().isUseSourceNameAsClassAlias()) {
			for (ClassNode cls : root.getClasses()) {
				if (cls.contains(AFlag.DONT_RENAME)) {
					continue;
				}
				String alias = getAliasFromSourceFile(cls);
				if (alias != null) {
					cls.rename(alias);
				}
			}
		}
	}

	@Nullable
	private static String getAliasFromSourceFile(ClassNode cls) {
		SourceFileAttr sourceFileAttr = cls.get(JadxAttrType.SOURCE_FILE);
		if (sourceFileAttr == null) {
			return null;
		}
		if (cls.getClassInfo().isInner()) {
			return null;
		}
		String name = sourceFileAttr.getFileName();
		name = StringUtils.removeSuffix(name, ".java");
		name = StringUtils.removeSuffix(name, ".kt");
		if (!NameMapper.isValidAndPrintable(name)) {
			return null;
		}
		ClassNode otherCls = cls.root().resolveClass(cls.getPackage() + '.' + name);
		if (otherCls != null) {
			return null;
		}

		if (cls.getClassInfo().hasAlias()) {
			// ignore source name if current alias is "better"
			String currentAlias = cls.getAlias();
			String betterName = BetterName.compareAndGet(name, currentAlias);
			if (betterName.equals(currentAlias)) {
				return null;
			}
		}
		cls.remove(JadxAttrType.SOURCE_FILE);
		return name;
	}
}
