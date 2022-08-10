package jadx.core.dex.visitors.rename;

import java.util.List;
import java.util.stream.Collectors;

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
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class UserRenames {
	private static final Logger LOG = LoggerFactory.getLogger(UserRenames.class);

	public static void apply(RootNode root) {
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
				cls.rename(rename.getNewName());
				break;

			case FIELD:
				FieldNode fieldNode = cls.searchFieldByShortId(nodeRef.getShortId());
				if (fieldNode == null) {
					LOG.warn("Field reference not found: {}", nodeRef);
				} else {
					fieldNode.rename(rename.getNewName());
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

	private static void applyPkgRenames(RootNode root, List<ICodeRename> renames) {
		renames.stream()
				.filter(r -> r.getNodeRef().getType() == IJavaNodeRef.RefType.PKG)
				.forEach(pkgRename -> {
					String pkgFullName = pkgRename.getNodeRef().getDeclaringClass();
					PackageNode pkgNode = root.resolvePackage(pkgFullName);
					if (pkgNode == null) {
						LOG.warn("Package for rename not found: {}", pkgFullName);
					} else {
						pkgNode.rename(pkgRename.getNewName());
					}
				});
	}
}
