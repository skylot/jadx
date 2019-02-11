package jadx.core.dex.visitors;

import com.android.dx.rop.code.AccessFlags;

import jadx.core.dex.info.AccessInfo;
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
		AccessInfo accessFlags = mth.getAccessFlags();
		int newVisFlag = fixVisibility(mth, accessFlags);
		if (newVisFlag != 0) {
			AccessInfo newAccFlags = accessFlags.changeVisibility(newVisFlag);
			if (newAccFlags != accessFlags) {
				mth.setAccFlags(newAccFlags);
				mth.addComment("Access modifiers changed, original: " + accessFlags.rawString());
			}
		}
	}

	private int fixVisibility(MethodNode mth, AccessInfo accessFlags) {
		if (mth.isVirtual()) {
			// make virtual methods public
			return AccessFlags.ACC_PUBLIC;
		} else {
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
