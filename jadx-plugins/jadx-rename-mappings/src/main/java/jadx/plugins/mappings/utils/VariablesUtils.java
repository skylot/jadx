package jadx.plugins.mappings.utils;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.utils.CodeUtils;
import jadx.core.dex.nodes.MethodNode;

public class VariablesUtils {
	private static final Logger LOG = LoggerFactory.getLogger(VariablesUtils.class);

	public static class VarInfo {
		private final VarNode var;
		private final int startOpIdx;
		private int endOpIdx;

		public VarInfo(VarNode var, int startOpIdx) {
			this.var = var;
			this.startOpIdx = startOpIdx;
			this.endOpIdx = startOpIdx;
		}

		public VarNode getVar() {
			return var;
		}

		public int getStartOpIdx() {
			return startOpIdx;
		}

		public int getEndOpIdx() {
			return endOpIdx;
		}

		public void setEndOpIdx(int endOpIdx) {
			this.endOpIdx = endOpIdx;
		}
	}

	public static List<VarInfo> collect(MethodNode mth) {
		ICodeInfo codeInfo = mth.getTopParentClass().getCode();
		int mthDefPos = mth.getDefPosition();
		int mthLineEndPos = CodeUtils.getLineEndForPos(codeInfo.getCodeStr(), mthDefPos);
		CodeVisitor codeVisitor = new CodeVisitor(mth);
		codeInfo.getCodeMetadata().searchDown(mthLineEndPos, codeVisitor::process);
		return codeVisitor.getVars();
	}

	private static class CodeVisitor {
		private final MethodNode mth;
		private final List<VarInfo> vars = new ArrayList<>();
		private int lastOffset = -1;

		public CodeVisitor(MethodNode mth) {
			this.mth = mth;
		}

		public @Nullable Boolean process(Integer pos, ICodeAnnotation ann) {
			if (ann instanceof InsnCodeOffset) {
				lastOffset = ((InsnCodeOffset) ann).getOffset();
			}
			if (ann instanceof NodeDeclareRef) {
				ICodeNodeRef declRef = ((NodeDeclareRef) ann).getNode();
				if (declRef instanceof VarNode) {
					VarNode varNode = (VarNode) declRef;
					if (!varNode.getMth().equals(mth)) { // Stop if we've gone too far and have entered a different method
						if (!vars.isEmpty()) {
							vars.get(vars.size() - 1).setEndOpIdx(declRef.getDefPosition() - 1);
						}
						return Boolean.TRUE;
					}
					if (lastOffset != -1) {
						if (!vars.isEmpty()) {
							vars.get(vars.size() - 1).setEndOpIdx(lastOffset - 1);
						}
						vars.add(new VarInfo(varNode, lastOffset));
					} else {
						LOG.warn("Local variable not present in bytecode, skipping: {}#{}",
								mth.getMethodInfo().getRawFullId(), varNode.getName());
					}
					lastOffset = -1;
				}
			}
			return null;
		}

		public List<VarInfo> getVars() {
			return vars;
		}
	}
}
