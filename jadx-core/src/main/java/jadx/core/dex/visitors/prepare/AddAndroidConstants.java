package jadx.core.dex.visitors.prepare;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.utils.android.AndroidResourcesMap;
import jadx.core.utils.exceptions.JadxException;

// TODO: move this pass to separate "Android plugin"
@JadxVisitor(
		name = "AddAndroidConstants",
		desc = "Insert Android constants from resource mapping file",
		runBefore = {
				CollectConstValues.class
		}
)
public class AddAndroidConstants extends AbstractVisitor {

	private static final String R_CLS = "android.R";
	private static final String R_INNER_CLS = R_CLS + '$';

	@Override
	public void init(RootNode root) throws JadxException {
		if (!root.getArgs().isReplaceConsts()) {
			return;
		}
		if (root.resolveClass(R_CLS) != null) {
			// Android R class already loaded
			return;
		}
		ConstStorage constStorage = root.getConstValues();
		AndroidResourcesMap.getMap().forEach((resId, path) -> {
			int sep = path.indexOf('/');
			String clsName = R_INNER_CLS + path.substring(0, sep);
			String resName = path.substring(sep + 1);
			ClassInfo cls = ClassInfo.fromName(root, clsName);
			FieldInfo field = FieldInfo.from(root, cls, resName, ArgType.INT);
			constStorage.addGlobalConstField(field, resId);
		});
	}
}
