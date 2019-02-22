package jadx.core.dex.visitors;

import com.android.dx.rop.code.AccessFlags;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

@JadxVisitor(
		name = "FixAccessModifiers",
		desc = "Change class and method access modifiers if needed",
		runAfter = ModVisitor.class
)
public class FixAccessModifiers extends AbstractVisitor {

	private boolean respectAccessModifiers;

	@Override
	public void init(RootNode root) {
		this.respectAccessModifiers = root.getArgs().isRespectBytecodeAccModifiers();
	}

	@Override
	public void visit(MethodNode mth) {
		if (respectAccessModifiers) {
			return;
		}
		int newVisFlag = fixVisibility(mth);
		if (newVisFlag != 0) {
			changeVisibility(mth, newVisFlag);
		}
	}

	public static void changeVisibility(ICodeNode node, int newVisFlag) {
		AccessInfo accessFlags = node.getAccessFlags();
		AccessInfo newAccFlags = accessFlags.changeVisibility(newVisFlag);
		if (newAccFlags != accessFlags) {
			node.setAccessFlags(newAccFlags);
			node.addAttr(AType.COMMENTS, "access modifiers changed from: " + accessFlags.rawString());
		}
	}

	private static int fixVisibility(MethodNode mth) {
		if (mth.isVirtual()) {
			// make virtual methods public
			return AccessFlags.ACC_PUBLIC;
		} else {
			AccessInfo accessFlags = mth.getAccessFlags();
			if (accessFlags.isAbstract()) {
				// make abstract methods public
				return AccessFlags.ACC_PUBLIC;
			}
			if (accessFlags.isConstructor() || accessFlags.isStatic()) {
				// TODO: make public if used outside
				return 0;
			}
			// make other direct methods private
			return AccessFlags.ACC_PRIVATE;
		}
	}
}
