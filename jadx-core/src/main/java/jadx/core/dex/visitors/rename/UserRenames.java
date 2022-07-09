package jadx.core.dex.visitors.rename;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.ICodeData;
import jadx.api.data.ICodeRename;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.InfoStorage;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class UserRenames {
	private static final Logger LOG = LoggerFactory.getLogger(UserRenames.class);

	public static void applyForNodes(RootNode root) {
		ICodeData codeData = root.getArgs().getCodeData();
		if (codeData == null || codeData.getRenames().isEmpty()) {
			return;
		}
		InfoStorage infoStorage = root.getInfoStorage();
		codeData.getRenames().stream()
				.filter(r -> r.getCodeRef() == null && r.getNodeRef().getType() != IJavaNodeRef.RefType.PKG)
				.collect(Collectors.groupingBy(r -> r.getNodeRef().getDeclaringClass()))
				.forEach((clsRawName, renames) -> {
					ClassInfo clsInfo = infoStorage.getCls(ArgType.object(clsRawName));
					if (clsInfo != null) {
						ClassNode cls = root.resolveClass(clsInfo);
						if (cls != null) {
							for (ICodeRename rename : renames) {
								applyRename(cls, rename);
							}
							return;
						}
					}
					LOG.warn("Class info with reference '{}' not found", clsRawName);
				});
		applyPkgRenames(root, codeData.getRenames());
	}

	private static void applyRename(ClassNode cls, ICodeRename rename) {
		IJavaNodeRef nodeRef = rename.getNodeRef();
		switch (nodeRef.getType()) {
			case CLASS:
				cls.getClassInfo().changeShortName(rename.getNewName());
				break;

			case FIELD:
				FieldNode fieldNode = cls.searchFieldByShortId(nodeRef.getShortId());
				if (fieldNode == null) {
					LOG.warn("Field reference not found: {}", nodeRef);
				} else {
					fieldNode.getFieldInfo().setAlias(rename.getNewName());
				}
				break;

			case METHOD:
				MethodNode mth = cls.searchMethodByShortId(nodeRef.getShortId());
				if (mth == null) {
					LOG.warn("Method reference not found: {}", nodeRef);
				} else {
					IJavaCodeRef codeRef = rename.getCodeRef();
					if (codeRef == null) {
						mth.rename(rename.getNewName());
					}
				}
				break;
		}
	}

	// TODO: Very inefficient!!! Add PackageInfo class to build package hierarchy
	private static void applyPkgRenames(RootNode root, List<ICodeRename> renames) {
		List<ClassNode> classes = root.getClasses(false);
		renames.stream()
				.filter(r -> r.getNodeRef().getType() == IJavaNodeRef.RefType.PKG)
				.forEach(pkgRename -> {
					String pkgFullName = pkgRename.getNodeRef().getDeclaringClass();
					String pkgFullNameDot = pkgFullName + ".";
					for (ClassNode cls : classes) {
						ClassInfo clsInfo = cls.getClassInfo();
						String pkg = clsInfo.getPackage();
						if (pkg.equals(pkgFullName)) {
							clsInfo.changePkg(cutLastPkgPart(clsInfo.getAliasPkg()) + '.' + pkgRename.getNewName());
						} else if (pkg.startsWith(pkgFullNameDot)) {
							clsInfo.changePkg(rebuildPkgMiddle(clsInfo.getAliasPkg(), pkgFullName, pkgRename.getNewName()));
						}
					}
				});
	}

	@NotNull
	private static String cutLastPkgPart(String pkgFullName) {
		int lastDotIndex = pkgFullName.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return pkgFullName;
		}
		return pkgFullName.substring(0, lastDotIndex);
	}

	private static String rebuildPkgMiddle(String aliasPkg, String renameOriginPkg, String newName) {
		String[] aliasParts = aliasPkg.split("\\.");
		String[] renameParts = renameOriginPkg.split("\\.");
		aliasParts[renameParts.length - 1] = newName;
		return String.join(".", aliasParts);
	}
}
