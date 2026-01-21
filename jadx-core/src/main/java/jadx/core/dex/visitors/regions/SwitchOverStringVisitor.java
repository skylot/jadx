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

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.CodeFeaturesAttr;
import jadx.core.dex.attributes.nodes.CodeFeaturesAttr.CodeFeature;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.conditions.Compare;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.Utils;
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
			boolean defaultCaseAdded = switchRegion.getCases().stream().anyMatch(SwitchRegion.CaseInfo::isDefaultCase);
			int casesWithString = defaultCaseAdded ? casesCount - 1 : casesCount;
			SSAVar strVar = strArg.getSVar();
			if (strVar.getUseCount() - 1 < casesWithString) {
				// one 'hashCode' invoke and at least one 'equals' per case
				return false;
			}
			// quick checks done, start collecting data to create a new switch region
			Map<InsnNode, String> strEqInsns = collectEqualsInsns(mth, strVar);
			if (strEqInsns.size() < casesWithString) {
				return false;
			}
			SwitchData switchData = new SwitchData(mth, switchRegion);
			switchData.setStrEqInsns(strEqInsns);
			switchData.setCases(new ArrayList<>(casesCount));
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
			for (SwitchRegion.CaseInfo caseInfo : switchData.getNewCases()) {
				replaceRegion.addCase(Collections.unmodifiableList(caseInfo.getKeys()), caseInfo.getContainer());
			}
			if (!parentRegion.replaceSubBlock(switchRegion, replaceRegion)) {
				mth.addWarnComment("Failed to restore switch over string. Please report as a decompilation issue");
				return false;
			}
			// replace confirmed, remove original code
			markCodeForRemoval(switchData);
			// use string arg directly in switch
			swInsn.replaceArg(swInsn.getArg(0), strArg.duplicate());
			return true;
		} catch (StackOverflowError | Exception e) {
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
		} catch (StackOverflowError | Exception e) {
			mth.addWarnComment("Failed to clean up code after switch over string restore", e);
		}
	}

	private boolean mergeWithCode(SwitchData switchData) {
		// check for second switch
		IContainer nextContainer = RegionUtils.getNextContainer(switchData.getMth(), switchData.getSwitchRegion());
		if (!(nextContainer instanceof SwitchRegion)) {
			return false;
		}
		SwitchRegion codeSwitch = (SwitchRegion) nextContainer;
		InsnNode swInsn = BlockUtils.getLastInsnWithType(codeSwitch.getHeader(), InsnType.SWITCH);
		if (swInsn == null || !swInsn.getArg(0).isRegister()) {
			return false;
		}
		RegisterArg numArg = (RegisterArg) swInsn.getArg(0);

		List<CaseData> cases = switchData.getCases();
		// search index assign in cases code
		int extracted = 0;
		for (CaseData caseData : cases) {
			InsnNode numInsn = searchConstInsn(switchData, caseData, swInsn);
			Integer num = extractConstNumber(switchData, numInsn, numArg);
			if (num != null) {
				caseData.setCodeNum(num);
				extracted++;
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

		// extract complete
		Map<Integer, CaseData> casesMap = new HashMap<>(cases.size());
		for (CaseData caseData : cases) {
			CaseData prev = casesMap.put(caseData.getCodeNum(), caseData);
			if (prev != null) {
				return false;
			}
			RegionUtils.visitBlocks(switchData.getMth(), caseData.getCode(),
					block -> switchData.getToRemove().add(block));
		}

		List<SwitchRegion.CaseInfo> newCases = new ArrayList<>();
		for (SwitchRegion.CaseInfo caseInfo : codeSwitch.getCases()) {
			SwitchRegion.CaseInfo newCase = null;
			for (Object key : caseInfo.getKeys()) {
				Integer intKey = unwrapIntKey(key);
				if (intKey != null) {
					CaseData caseData = casesMap.remove(intKey);
					if (caseData == null) {
						return false;
					}
					if (newCase == null) {
						List<Object> keys = new ArrayList<>(caseData.getStrValues());
						newCase = new SwitchRegion.CaseInfo(keys, caseInfo.getContainer());
					} else {
						// merge cases
						newCase.getKeys().addAll(caseData.getStrValues());
					}
				} else if (key == SwitchRegion.DEFAULT_CASE_KEY) {
					var iterator = casesMap.entrySet().iterator();
					while (iterator.hasNext()) {
						CaseData caseData = iterator.next().getValue();
						if (newCase == null) {
							List<Object> keys = new ArrayList<>(caseData.getStrValues());
							newCase = new SwitchRegion.CaseInfo(keys, caseInfo.getContainer());
						} else {
							// merge cases
							newCase.getKeys().addAll(caseData.getStrValues());
						}
						iterator.remove();
					}
					if (newCase == null) {
						newCase = new SwitchRegion.CaseInfo(new ArrayList<>(), caseInfo.getContainer());
					}
					newCase.getKeys().add(SwitchRegion.DEFAULT_CASE_KEY);
				} else {
					return false;
				}
			}
			newCases.add(newCase);
		}
		switchData.setCodeSwitch(codeSwitch);
		switchData.setNumArg(numArg);
		switchData.setNewCases(newCases);
		return true;
	}

	private @Nullable Integer extractConstNumber(SwitchData switchData, @Nullable InsnNode numInsn, RegisterArg numArg) {
		if (numInsn == null || numInsn.getArgsCount() != 1) {
			return null;
		}
		Object constVal = InsnUtils.getConstValueByArg(switchData.getMth().root(), numInsn.getArg(0));
		if (constVal instanceof LiteralArg) {
			if (numArg.sameCodeVar(numInsn.getResult())) {
				return (int) ((LiteralArg) constVal).getLiteral();
			}
		}
		return null;
	}

	private static @Nullable InsnNode searchConstInsn(SwitchData switchData, CaseData caseData, InsnNode swInsn) {
		IContainer container = caseData.getCode();
		if (container != null) {
			List<InsnNode> insns = RegionUtils.collectInsns(switchData.getMth(), container);
			insns.removeIf(i -> i.getType() == InsnType.BREAK);
			if (insns.size() == 1) {
				return insns.get(0);
			}
		} else if (caseData.getBlockRef() != null) {
			// variable used unchanged on path from block ref
			BlockNode blockRef = caseData.getBlockRef();
			InsnArg swArg = swInsn.getArg(0);
			if (swArg.isRegister()) {
				InsnNode assignInsn = ((RegisterArg) swArg).getSVar().getAssignInsn();
				if (assignInsn != null && assignInsn.getType() == InsnType.PHI) {
					RegisterArg arg = ((PhiInsn) assignInsn).getArgByBlock(blockRef);
					if (arg != null) {
						return arg.getAssignInsn();
					}
				}
			}
		}
		return null;
	}

	private Integer unwrapIntKey(Object key) {
		if (key instanceof Integer) {
			return (Integer) key;
		}
		if (key instanceof FieldNode) {
			EncodedValue encodedValue = ((FieldNode) key).get(JadxAttrType.CONSTANT_VALUE);
			if (encodedValue != null && encodedValue.getType() == EncodedType.ENCODED_INT) {
				return (Integer) encodedValue.getValue();
			}
			return null;
		}
		return null;
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
		if (caseInfo.isDefaultCase()) {
			CaseData caseData = new CaseData();
			caseData.setCode(caseInfo.getContainer());
			return true;
		}
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
		Compare compare = condition.getCompare();
		if (compare == null) {
			return null;
		}
		IfNode ifInsn = compare.getInsn();
		InsnArg firstArg = ifInsn.getArg(0);
		String str = null;
		if (firstArg.isInsnWrap()) {
			str = switchData.getStrEqInsns().get(((InsnWrapArg) firstArg).getWrapInsn());
		}
		if (str == null) {
			return null;
		}
		if (ifInsn.getOp() == IfOp.NE && ifInsn.getArg(1).isTrue()) {
			neg = true;
		}
		if (ifInsn.getOp() == IfOp.EQ && ifInsn.getArg(1).isFalse()) {
			neg = true;
		}
		switchData.getToRemove().add(ifInsn);
		switchData.getToRemove().addAll(ifRegion.getConditionBlocks());

		CaseData caseData = new CaseData();
		caseData.getStrValues().add(str);

		IContainer codeContainer = neg ? ifRegion.getElseRegion() : ifRegion.getThenRegion();
		if (codeContainer == null) {
			// no code
			// use last condition block for later data tracing
			caseData.setBlockRef(Utils.last(ifRegion.getConditionBlocks()));
		} else {
			caseData.setCode(codeContainer);
		}
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
		private List<SwitchRegion.CaseInfo> newCases;
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

		public List<SwitchRegion.CaseInfo> getNewCases() {
			return newCases;
		}

		public void setNewCases(List<SwitchRegion.CaseInfo> cases) {
			this.newCases = cases;
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
		private @Nullable IContainer code = null;
		private @Nullable BlockNode blockRef = null;
		private int codeNum = -1;

		public List<String> getStrValues() {
			return strValues;
		}

		public @Nullable IContainer getCode() {
			return code;
		}

		public void setCode(@Nullable IContainer code) {
			this.code = code;
		}

		public @Nullable BlockNode getBlockRef() {
			return blockRef;
		}

		public void setBlockRef(@Nullable BlockNode blockRef) {
			this.blockRef = blockRef;
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
