package jadx.core.dex.attributes.nodes;

import java.util.BitSet;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class SkipMethodArgsAttr extends PinnedAttribute {

	public static void skipArg(MethodNode mth, RegisterArg arg) {
		int argNum = Utils.indexInListByRef(mth.getArgRegs(), arg);
		if (argNum == -1) {
			throw new JadxRuntimeException("Arg not found: " + arg);
		}
		skipArg(mth, argNum);
	}

	public static void skipArg(MethodNode mth, int argNum) {
		SkipMethodArgsAttr attr = mth.get(AType.SKIP_MTH_ARGS);
		if (attr == null) {
			attr = new SkipMethodArgsAttr(mth);
			mth.addAttr(attr);
		}
		attr.skip(argNum);
	}

	public static boolean isSkip(@Nullable MethodNode mth, int argNum) {
		if (mth == null) {
			return false;
		}
		SkipMethodArgsAttr attr = mth.get(AType.SKIP_MTH_ARGS);
		if (attr == null) {
			return false;
		}
		return attr.isSkip(argNum);
	}

	private final BitSet skipArgs;

	private SkipMethodArgsAttr(MethodNode mth) {
		this.skipArgs = new BitSet(mth.getMethodInfo().getArgsCount());
	}

	public void skip(int argNum) {
		skipArgs.set(argNum);
	}

	public boolean isSkip(int argNum) {
		return skipArgs.get(argNum);
	}

	public int getSkipCount() {
		return skipArgs.cardinality();
	}

	@Override
	public AType<SkipMethodArgsAttr> getAttrType() {
		return AType.SKIP_MTH_ARGS;
	}

	@Override
	public String toString() {
		return "SKIP_MTH_ARGS: " + skipArgs;
	}
}
