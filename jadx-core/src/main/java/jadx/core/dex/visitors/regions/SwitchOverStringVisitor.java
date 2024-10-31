package jadx.core.dex.visitors.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.CodeFeaturesAttr;
import jadx.core.dex.attributes.nodes.CodeFeaturesAttr.CodeFeature;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "SwitchOverStringVisitor",
		desc = "Restore switch over string",
		runAfter = IfRegionVisitor.class,
		runBefore = ReturnVisitor.class
)
public class SwitchOverStringVisitor extends AbstractVisitor implements IRegionIterativeVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (!CodeFeaturesAttr.contains(mth, CodeFeature.SWITCH)) {
			return;
		}
		DepthRegionTraversal.traverseIterative(mth, this);
	}

	@Override
	public boolean visitRegion(MethodNode mth, IRegion region) {
		if (region instanceof SwitchRegion) {
			return restoreSwitchOverString(mth, (SwitchRegion) region);
		}
		return false;
	}

	private boolean restoreSwitchOverString(MethodNode mth, SwitchRegion switchRegion) {
		try {
			InsnNode swInsn = BlockUtils.getLastInsnWithType(switchRegion.getHeader(), InsnType.SWITCH);
			if (swInsn == null) {
				return false;
			}
			RegisterArg strArg = getStrHashCodeArg(swInsn.getArg(0));
			if (strArg == null) {
				return false;
			}
			int casesCount = switchRegion.getCases().size();
			SSAVar strVar = strArg.getSVar();
			if (strVar.getUseCount() - 1 < casesCount) {
				// one 'hashCode' invoke and at least one 'equals' per case
				return false;
			}
			// quick checks done, start collecting data to create a new switch region
			Map<InsnNode, String> strEqInsns = collectEqualsInsns(mth, strVar);
			if (strEqInsns.size() < casesCount) {
				return false;
			}
			SwitchData switchData = new SwitchData(mth, switchRegion);
			switchData.setStrEqInsns(strEqInsns);
			switchData.setCases(new ArrayList<>(strEqInsns.size()));
			for (SwitchRegion.CaseInfo swCaseInfo : switchRegion.getCases()) {
				if (!processCase(switchData, swCaseInfo)) {
					mth.addWarnComment("Failed to restore switch over string. Please report as a decompilation issue");
					return false;
				}
			}
			// match remapping var to collect code from second switch
			if (!mergeWithCode(switchData)) {
				mth.addWarnComment("Failed to restore switch over string. Please report as a decompilation issue");
				return false;
			}
			// all checks passed, replace with new switch
			IRegion parentRegion = switchRegion.getParent();
			SwitchRegion replaceRegion = new SwitchRegion(parentRegion, switchRegion.getHeader());
			for (CaseData caseData : switchData.getCases()) {
				replaceRegion.addCase(Collections.unmodifiableList(caseData.getStrValues()), caseData.getCode());
			}
			replaceRegion.addDefaultCase(switchData.getDefaultCode());
			if (!parentRegion.replaceSubBlock(switchRegion, replaceRegion)) {
				mth.addWarnComment("Failed to restore switch over string. Please report as a decompilation issue");
				return false;
			}
			// replace confirmed, remove original code
			markCodeForRemoval(switchData);
			// use string arg directly in switch
			swInsn.replaceArg(swInsn.getArg(0), strArg.duplicate());
			return true;
		} catch (Throwable e) {
			mth.addWarnComment("Failed to restore switch over string. Please report as a decompilation issue", e);
			return false;
		}
	}

	private static void markCodeForRemoval(SwitchData switchData) {
		MethodNode mth = switchData.getMth();
		try {
			switchData.getToRemove().forEach(i -> i.add(AFlag.REMOVE));
			SwitchRegion codeSwitch = switchData.getCodeSwitch();
			if (codeSwitch != null) {
				IRegion parentRegion = switchData.getSwitchRegion().getParent();
				parentRegion.getSubBlocks().remove(codeSwitch);
				codeSwitch.getHeader().add(AFlag.REMOVE);
			}
			RegisterArg numArg = switchData.getNumArg();
			if (numArg != null) {
				for (SSAVar ssaVar : numArg.getSVar().getCodeVar().getSsaVars()) {
					InsnNode assignInsn = ssaVar.getAssignInsn();
					if (assignInsn != null) {
						assignInsn.add(AFlag.REMOVE);
					}
					for (RegisterArg useArg : ssaVar.getUseList()) {
						InsnNode parentInsn = useArg.getParentInsn();
						if (parentInsn != null) {
							parentInsn.add(AFlag.REMOVE);
						}
					}
					mth.removeSVar(ssaVar);
				}
			}
			InsnRemover.removeAllMarked(mth);
		} catch (Throwable e) {
			mth.addWarnComment("Failed to clean up code after switch over string restore", e);
		}
	}

	private boolean mergeWithCode(SwitchData switchData) {
		List<CaseData> cases = switchData.getCases();
		// search index assign in cases code
		RegisterArg numArg = null;
		int extracted = 0;
		for (CaseData caseData : cases) {
			IContainer container = caseData.getCode();
			List<InsnNode> insns = RegionUtils.collectInsns(switchData.getMth(), container);
			insns.removeIf(i -> i.getType() == InsnType.BREAK);
			if (insns.size() != 1) {
				continue;
			}
			InsnNode numInsn = insns.get(0);
			if (numInsn.getArgsCount() == 1) {
				Object constVal = InsnUtils.getConstValueByArg(switchData.getMth().root(), numInsn.getArg(0));
				if (constVal instanceof LiteralArg) {
					if (numArg == null) {
						numArg = numInsn.getResult();
					} else {
						if (!numArg.sameCodeVar(numInsn.getResult())) {
							return false;
						}
					}
					int num = (int) ((LiteralArg) constVal).getLiteral();
					caseData.setCodeNum(num);
					extracted++;
				}
			}
		}
		if (extracted == 0) {
			// nothing to merge, code already inside first switch cases
			return true;
		}
		if (extracted != cases.size()) {
			return false;
		}
		// TODO: additional checks for found index numbers
		cases.sort(Comparator.comparingInt(CaseData::getCodeNum));

		// extract complete, second switch on 'numArg' should be the next region
		IContainer nextContainer = RegionUtils.getNextContainer(switchData.getMth(), switchData.getSwitchRegion());
		if (!(nextContainer instanceof SwitchRegion)) {
			return false;
		}
		SwitchRegion codeSwitch = (SwitchRegion) nextContainer;
		InsnNode swInsn = BlockUtils.getLastInsnWithType(codeSwitch.getHeader(), InsnType.SWITCH);
		if (swInsn == null || !swInsn.getArg(0).isSameCodeVar(numArg)) {
			return false;
		}
		Map<Integer, CaseData> casesMap = new HashMap<>(cases.size());
		for (CaseData caseData : cases) {
			CaseData prev = casesMap.put(caseData.getCodeNum(), caseData);
			if (prev != null) {
				return false;
			}
			RegionUtils.visitBlocks(switchData.getMth(), caseData.getCode(),
					block -> switchData.getToRemove().add(block));
		}

		IContainer defaultContainer = null;
		for (SwitchRegion.CaseInfo caseInfo : codeSwitch.getCases()) {
			CaseData prevCase = null;
			for (Object key : caseInfo.getKeys()) {
				if (key instanceof Integer) {
					Integer intKey = (Integer) key;
					CaseData caseData = casesMap.get(intKey);
					if (caseData == null) {
						return false;
					}
					if (prevCase == null) {
						caseData.setCode(caseInfo.getContainer());
						prevCase = caseData;
					} else {
						// merge cases
						prevCase.getStrValues().addAll(caseData.getStrValues());
						caseData.setCodeNum(-1);
					}
				} else if (key == SwitchRegion.DEFAULT_CASE_KEY) {
					defaultContainer = caseInfo.getContainer();
				} else {
					return false;
				}
			}
		}
		cases.removeIf(c -> c.getCodeNum() == -1);

		switchData.setDefaultCode(defaultContainer);
		switchData.setCodeSwitch(codeSwitch);
		switchData.setNumArg(numArg);
		return true;
	}

	private static Map<InsnNode, String> collectEqualsInsns(MethodNode mth, SSAVar strVar) {
		Map<InsnNode, String> map = new IdentityHashMap<>(strVar.getUseCount() - 1);
		for (RegisterArg useReg : strVar.getUseList()) {
			InsnNode parentInsn = useReg.getParentInsn();
			if (parentInsn != null && parentInsn.getType() == InsnType.INVOKE) {
				InvokeNode inv = (InvokeNode) parentInsn;
				if (inv.getCallMth().getRawFullId().equals("java.lang.String.equals(Ljava/lang/Object;)Z")) {
					InsnArg strArg = inv.getArg(1);
					Object strValue = InsnUtils.getConstValueByArg(mth.root(), strArg);
					if (strValue instanceof String) {
						map.put(parentInsn, (String) strValue);
					}
				}
			}
		}
		return map;
	}

	private boolean processCase(SwitchData switchData, SwitchRegion.CaseInfo caseInfo) {
		AtomicBoolean fail = new AtomicBoolean(false);
		RegionUtils.visitRegions(switchData.getMth(), caseInfo.getContainer(), region -> {
			if (fail.get()) {
				return false;
			}
			if (region instanceof IfRegion) {
				CaseData caseData = fillCaseData((IfRegion) region, switchData);
				if (caseData == null) {
					fail.set(true);
					return false;
				}
				switchData.getCases().add(caseData);
			}
			return true;
		});
		return !fail.get();
	}

	private @Nullable CaseData fillCaseData(IfRegion ifRegion, SwitchData switchData) {
		IfCondition condition = Objects.requireNonNull(ifRegion.getCondition());
		boolean neg = false;
		if (condition.getMode() == IfCondition.Mode.NOT) {
			condition = condition.getArgs().get(0);
			neg = true;
		}
		String str = null;
		if (condition.isCompare()) {
			IfNode ifInsn = condition.getCompare().getInsn();
			InsnArg firstArg = ifInsn.getArg(0);
			if (firstArg.isInsnWrap()) {
				str = switchData.getStrEqInsns().get(((InsnWrapArg) firstArg).getWrapInsn());
			}
			if (ifInsn.getOp() == IfOp.NE && ifInsn.getArg(1).isTrue()) {
				neg = true;
			}
			if (str != null) {
				switchData.getToRemove().add(ifInsn);
				switchData.getToRemove().addAll(ifRegion.getConditionBlocks());
			}
		}
		if (str == null) {
			return null;
		}
		CaseData caseData = new CaseData();
		caseData.getStrValues().add(str);
		caseData.setCode(neg ? ifRegion.getElseRegion() : ifRegion.getThenRegion());
		return caseData;
	}

	private @Nullable RegisterArg getStrHashCodeArg(InsnArg arg) {
		if (arg.isRegister()) {
			return getStrFromInsn(((RegisterArg) arg).getAssignInsn());
		}
		if (arg.isInsnWrap()) {
			return getStrFromInsn(((InsnWrapArg) arg).getWrapInsn());
		}
		return null;
	}

	private @Nullable RegisterArg getStrFromInsn(@Nullable InsnNode insn) {
		if (insn == null || insn.getType() != InsnType.INVOKE) {
			return null;
		}
		InvokeNode invInsn = (InvokeNode) insn;
		MethodInfo callMth = invInsn.getCallMth();
		if (!callMth.getRawFullId().equals("java.lang.String.hashCode()I")) {
			return null;
		}
		InsnArg arg = invInsn.getInstanceArg();
		if (arg == null || !arg.isRegister()) {
			return null;
		}
		return (RegisterArg) arg;
	}

	private static final class SwitchData {
		private final MethodNode mth;
		private final SwitchRegion switchRegion;
		private final List<IAttributeNode> toRemove = new ArrayList<>();
		private Map<InsnNode, String> strEqInsns;
		private List<CaseData> cases;
		private IContainer defaultCode;
		private SwitchRegion codeSwitch;
		private RegisterArg numArg;

		private SwitchData(MethodNode mth, SwitchRegion switchRegion) {
			this.mth = mth;
			this.switchRegion = switchRegion;
		}

		public List<CaseData> getCases() {
			return cases;
		}

		public void setCases(List<CaseData> cases) {
			this.cases = cases;
		}

		public IContainer getDefaultCode() {
			return defaultCode;
		}

		public void setDefaultCode(IContainer defaultCode) {
			this.defaultCode = defaultCode;
		}

		public MethodNode getMth() {
			return mth;
		}

		public Map<InsnNode, String> getStrEqInsns() {
			return strEqInsns;
		}

		public void setStrEqInsns(Map<InsnNode, String> strEqInsns) {
			this.strEqInsns = strEqInsns;
		}

		public SwitchRegion getSwitchRegion() {
			return switchRegion;
		}

		public List<IAttributeNode> getToRemove() {
			return toRemove;
		}

		public SwitchRegion getCodeSwitch() {
			return codeSwitch;
		}

		public void setCodeSwitch(SwitchRegion codeSwitch) {
			this.codeSwitch = codeSwitch;
		}

		public RegisterArg getNumArg() {
			return numArg;
		}

		public void setNumArg(RegisterArg numArg) {
			this.numArg = numArg;
		}
	}

	private static final class CaseData {
		private final List<String> strValues = new ArrayList<>();
		private IContainer code = null;
		private int codeNum = -1;

		public List<String> getStrValues() {
			return strValues;
		}

		public IContainer getCode() {
			return code;
		}

		public void setCode(IContainer code) {
			this.code = code;
		}

		public int getCodeNum() {
			return codeNum;
		}

		public void setCodeNum(int codeNum) {
			this.codeNum = codeNum;
		}

		@Override
		public String toString() {
			return "CaseData{" + strValues + '}';
		}
	}
}
