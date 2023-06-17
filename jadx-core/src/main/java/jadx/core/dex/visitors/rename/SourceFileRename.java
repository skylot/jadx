package jadx.core.dex.visitors.rename;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BetterName;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxException;

public class SourceFileRename extends AbstractVisitor {

	@Override
	public String getName() {
		return "SourceFileRename";
	}

	@Override
	public void init(RootNode root) throws JadxException {
		if (!root.getArgs().isUseSourceNameAsClassAlias()) {
			return;
		}
		List<ClassNode> classes = root.getClasses();
		Map<String, Boolean> canUseAlias = new HashMap<>();
		for (ClassNode cls : classes) {
			canUseAlias.put(cls.getClassInfo().getShortName(), Boolean.FALSE);
		}
		List<ClsRename> renames = new ArrayList<>();
		for (ClassNode cls : classes) {
			if (cls.contains(AFlag.DONT_RENAME)) {
				continue;
			}
			String alias = getAliasFromSourceFile(cls);
			if (alias != null) {
				Boolean prev = canUseAlias.get(alias);
				if (prev == null) {
					canUseAlias.put(alias, Boolean.TRUE);
					renames.add(new ClsRename(cls, alias));
				} else if (prev == Boolean.TRUE) {
					canUseAlias.put(alias, Boolean.FALSE);
				}
			}
		}
		for (ClsRename clsRename : renames) {
			String alias = clsRename.getAlias();
			if (canUseAlias.get(alias) == Boolean.TRUE) {
				applyRename(clsRename.getCls(), alias);
			}
		}
	}

	private static void applyRename(ClassNode cls, String alias) {
		if (cls.getClassInfo().hasAlias()) {
			// ignore source name if current alias is "better"
			String currentAlias = cls.getAlias();
			String betterName = BetterName.compareAndGet(alias, currentAlias);
			if (betterName.equals(currentAlias)) {
				return;
			}
		}
		cls.getClassInfo().changeShortName(alias);
		cls.addAttr(new RenameReasonAttr(cls).append("use source file name"));
	}

	private static @Nullable String getAliasFromSourceFile(ClassNode cls) {
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
		if (name.equals(cls.getName())) {
			return null;
		}
		return name;
	}

	private static final class ClsRename {
		private final ClassNode cls;
		private final String alias;

		private ClsRename(ClassNode cls, String alias) {
			this.cls = cls;
			this.alias = alias;
		}

		public ClassNode getCls() {
			return cls;
		}

		public String getAlias() {
			return alias;
		}

		@Override
		public String toString() {
			return "ClsRename{" + cls + " -> '" + alias + "'}";
		}
	}
}
