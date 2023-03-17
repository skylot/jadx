package jadx.gui.device.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.device.debugger.BreakpointManager.FileBreakpoint;
import jadx.gui.device.debugger.SmaliDebugger.Frame;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeBreakpoint;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeDebugInfo;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeField;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeRegister;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeValue;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeVarInfo;
import jadx.gui.device.debugger.smali.Smali;
import jadx.gui.device.debugger.smali.SmaliRegister;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.panel.IDebugController;
import jadx.gui.ui.panel.JDebuggerPanel;
import jadx.gui.ui.panel.JDebuggerPanel.IListElement;
import jadx.gui.ui.panel.JDebuggerPanel.ValueTreeNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public final class DebugController implements SmaliDebugger.SuspendListener, IDebugController {

	private static final Logger LOG = LoggerFactory.getLogger(DebugController.class);
	private static final String ONCREATE_SIGNATURE = "onCreate(Landroid/os/Bundle;)V";
	private static final Map<String, RuntimeType> TYPE_MAP = new HashMap<>();
	private static final RuntimeType[] POSSIBLE_TYPES = { RuntimeType.OBJECT, RuntimeType.INT, RuntimeType.LONG };
	private static final int DEFAULT_CACHE_SIZE = 512;

	private JDebuggerPanel debuggerPanel;
	private SmaliDebugger debugger;
	private ArtAdapter.Debugger art;
	private final CurrentInfo cur = new CurrentInfo();

	private BreakpointStore bpStore;
	private boolean updateAllFldAndReg = false; // update all fields and registers
	private ValueTreeNode toBeUpdatedTreeNode; // a field or register number.
	private volatile boolean isSuspended = true;
	private boolean hasResumed;
	private ResumeCmd run;
	private ResumeCmd stepOver;
	private ResumeCmd stepInto;
	private ResumeCmd stepOut;
	private StateListener stateListener;

	private final Map<String, RegisterObserver> regAdaMap = new ConcurrentHashMap<>();

	private final ExecutorService updateQueue = Executors.newSingleThreadExecutor();
	private final ExecutorService lazyQueue = Executors.newSingleThreadExecutor();

	@Override
	public boolean startDebugger(JDebuggerPanel debuggerPanel, String adbHost, int adbPort, int androidVer) {
		if (TYPE_MAP.isEmpty()) {
			initTypeMap();
		}
		this.debuggerPanel = debuggerPanel;
		UiUtils.uiRunAndWait(debuggerPanel::resetUI);
		try {
			debugger = SmaliDebugger.attach(adbHost, adbPort, this);
		} catch (SmaliDebuggerException e) {
			JOptionPane.showMessageDialog(debuggerPanel.getMainWindow(), e.getMessage(),
					NLS.str("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
			logErr(e);
			return false;
		}
		art = ArtAdapter.getAdapter(androidVer);
		resetAllInfo();
		hasResumed = false;
		run = debugger::resume;
		stepOver = debugger::stepOver;
		stepInto = debugger::stepInto;
		stepOut = debugger::stepOut;
		stopAtOnCreate();
		if (bpStore == null) {
			bpStore = new BreakpointStore();
		} else {
			bpStore.reset();
		}
		BreakpointManager.setDebugController(this);
		initBreakpoints(BreakpointManager.getAllBreakpoints());
		return true;
	}

	private void openMainActivityTab(JClass mainActivity) {
		String fullID = DbgUtils.getRawFullName(mainActivity) + "." + ONCREATE_SIGNATURE;
		Smali smali = DbgUtils.getSmali(mainActivity.getCls().getClassNode());
		int pos = smali.getMethodDefPos(fullID);
		int finalPos = Math.max(1, pos);
		debuggerPanel.scrollToSmaliLine(mainActivity, finalPos, true);
	}

	private void stopAtOnCreate() {
		JClass mainActivity = DbgUtils.searchMainActivity(debuggerPanel.getMainWindow());
		if (mainActivity == null) {
			debuggerPanel.log("Failed to set breakpoint at onCreate, you have to do it yourself.");
			return;
		}
		lazyQueue.execute(() -> openMainActivityTab(mainActivity));
		String clsSig = DbgUtils.getRawFullName(mainActivity);
		try {
			long id = debugger.getClassID(clsSig, true);
			if (id != -1) {
				return; // this app is running, we can't stop at onCreate anymore.
			}
			debuggerPanel.log(String.format("Breakpoint will set at %s.%s", clsSig, ONCREATE_SIGNATURE));
			debugger.regMethodEntryEventSync(clsSig, ONCREATE_SIGNATURE::equals);
		} catch (SmaliDebuggerException e) {
			logErr(e, String.format("Failed set breakpoint at %s.%s", clsSig, ONCREATE_SIGNATURE));
		}
	}

	@Override
	public boolean isSuspended() {
		return isSuspended;
	}

	@Override
	public boolean isDebugging() {
		return debugger != null;
	}

	@Override
	public boolean run() {
		return execResumeCmd(run);
	}

	@Override
	public boolean stepInto() {
		return execResumeCmd(stepInto);
	}

	@Override
	public boolean stepOver() {
		return execResumeCmd(stepOver);
	}

	@Override
	public boolean stepOut() {
		return execResumeCmd(stepOut);
	}

	@Override
	public boolean pause() {
		if (isDebugging()) {
			try {
				debugger.suspend();
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return false;
			}
			setDebuggerState(true, false);
			resetAllInfo();
		}
		return true;
	}

	@Override
	public boolean stop() {
		if (isDebugging()) {
			try {
				debugger.exit();
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean exit() {
		if (isDebugging()) {
			setDebuggerState(true, true);
			stop();
			debugger = null;
		}
		BreakpointManager.setDebugController(null);
		debuggerPanel.getMainWindow().destroyDebuggerPanel();
		debuggerPanel = null;
		return true;
	}

	/**
	 * @param type must be one of int, long, float, double, string or object.
	 */
	@Override
	public boolean modifyRegValue(ValueTreeNode valNode, ArgType type, Object value) {
		checkType(type, value);
		if (isDebugging() && isSuspended()) {
			return modifyValueInternal(valNode, castType(type), value);
		}
		return false;
	}

	@Override
	public String getProcessName() {
		String pkg = DbgUtils.searchPackageName(debuggerPanel.getMainWindow());
		if (pkg.isEmpty()) {
			return "";
		}
		JClass cls = DbgUtils.searchMainActivity(debuggerPanel.getMainWindow());
		if (cls == null) {
			return "";
		}
		return pkg + "/" + cls.getCls().getClassNode().getClassInfo().getFullName();
	}

	private RuntimeType castType(ArgType type) {
		if (type == ArgType.INT) {
			return RuntimeType.INT;
		}
		if (type == ArgType.STRING) {
			return RuntimeType.STRING;
		}
		if (type == ArgType.LONG) {
			return RuntimeType.LONG;
		}
		if (type == ArgType.FLOAT) {
			return RuntimeType.FLOAT;
		}
		if (type == ArgType.DOUBLE) {
			return RuntimeType.DOUBLE;
		}
		if (type == ArgType.OBJECT) {
			return RuntimeType.OBJECT;
		}
		throw new JadxRuntimeException("Unexpected type: " + type);
	}

	protected static RuntimeType castType(String type) {
		RuntimeType rt = null;
		if (!StringUtils.isEmpty(type)) {
			rt = TYPE_MAP.get(type);
		}
		if (rt == null) {
			rt = POSSIBLE_TYPES[0];
		}
		return rt;
	}

	private void checkType(ArgType type, Object value) {
		if (!(type == ArgType.INT && value instanceof Integer)
				&& !(type == ArgType.STRING && value instanceof String)
				&& !(type == ArgType.LONG && value instanceof Long)
				&& !(type == ArgType.FLOAT && value instanceof Float)
				&& !(type == ArgType.DOUBLE && value instanceof Double)
				&& !(type == ArgType.OBJECT && value instanceof Long)) {
			throw new JadxRuntimeException("Type must be one of int, long, float, double, String or Object.");
		}
	}

	private boolean modifyValueInternal(ValueTreeNode valNode, RuntimeType type, Object value) {
		if (valNode instanceof RegTreeNode) {
			try {
				RegTreeNode regNode = (RegTreeNode) valNode;
				debugger.setValueSync(
						regNode.getRuntimeRegNum(),
						type,
						value,
						cur.frame.getThreadID(),
						cur.frame.getFrame().getID());
				lazyQueue.execute(() -> {
					setRegsNotUpdated();
					updateRegister((RegTreeNode) valNode, type, true);
				});
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return false;
			}
		} else if (valNode instanceof FieldTreeNode) {
			// TODO: check type.
			FieldTreeNode fldNode = (FieldTreeNode) valNode;
			try {
				debugger.setValueSync(
						fldNode.getObjectID(),
						((RuntimeField) fldNode.getRuntimeValue()).getFieldID(),
						fldNode.getRuntimeField().getType(),
						value);
				lazyQueue.execute(() -> {
					updateField((FieldTreeNode) valNode);
				});
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return false;
			}
		}
		return true;
	}

	private interface ResumeCmd {
		void exec() throws SmaliDebuggerException;
	}

	private boolean execResumeCmd(ResumeCmd cmd) {
		if (!hasResumed) {
			if (cmd != run) {
				return false;
			}
			hasResumed = true;
		}
		if (isDebugging() && isSuspended()) {
			updateAllFldAndReg = cmd == run;
			setDebuggerState(false, false);
			try {
				cmd.exec();
				return true;
			} catch (SmaliDebuggerException e) {
				logErr(e);
				setDebuggerState(true, false);
			}
		}
		return false;
	}

	/**
	 * @param suspended suspended by step, breakpoint, etc..
	 * @param stopped   remote app had been terminated, it's used to
	 *                  change icons only, to check if it's running use isDebugging() instead.
	 */
	private void setDebuggerState(boolean suspended, boolean stopped) {
		isSuspended = suspended;
		if (stopped) {
			hasResumed = false;
		}
		if (stateListener != null) {
			stateListener.onStateChanged(suspended, stopped);
		}
	}

	@Override
	public void setStateListener(StateListener listener) {
		stateListener = listener;
	}

	@Override
	public void onSuspendEvent(SuspendInfo info) {
		if (!isDebugging()) {
			return;
		}
		if (info.isTerminated()) {
			debuggerPanel.log("Debugger exited.");
			setDebuggerState(true, true);
			debugger = null;
			return;
		}
		setDebuggerState(true, false);
		long threadID = info.getThreadID();
		int refreshLevel = 2; // update all threads, stack frames, registers and fields.
		if (cur.frame != null) {
			if (threadID == cur.frame.getThreadID()
					&& info.getClassID() == cur.frame.getClsID()
					&& info.getMethodID() == cur.frame.getMthID()) {

				refreshLevel = 1; // relevant registers or fields.
			} else {
				cur.frame.getClsID();
			}
			setRegsNotUpdated();
		}
		if (refreshLevel == 2) {
			updateAllInfo(threadID, info.getOffset());
		} else {
			if (cur.smali != null && cur.frame != null) {
				refreshRegInfo(info.getOffset());
				refreshCurFrame(threadID, info.getOffset());
				if (updateAllFldAndReg) {
					debuggerPanel.resetRegTreeNodes();
					updateAllRegisters(cur.frame);
				} else if (toBeUpdatedTreeNode != null) {
					lazyQueue.execute(() -> updateRegOrField(toBeUpdatedTreeNode));
				}
				markCodeOffset(info.getOffset());
			} else {
				debuggerPanel.resetRegTreeNodes();
			}
			if (cur.frame != null) {
				// update current code offset in stack frame.
				cur.frame.updateCodeOffset(info.getOffset());
				debuggerPanel.refreshStackFrameList(Collections.emptyList());
			}
		}
	}

	private void refreshRegInfo(long codeOffset) {
		List<RegisterObserver.Info> list = cur.regAdapter.getInfoAt(codeOffset);
		for (RegisterObserver.Info info : list) {
			RegTreeNode reg = cur.frame.getRegNodes().get(info.getSmaliRegNum());
			if (info.isLoad()) {
				applyDbgInfo(reg, info.getInfo());
			} else {
				reg.setAlias("");
				reg.setAbsoluteType(false);
			}
		}
		if (list.size() > 0) {
			debuggerPanel.refreshRegisterTree();
		}
	}

	private void updateRegOrField(ValueTreeNode valTreeNode) {
		if (valTreeNode instanceof RegTreeNode) {
			updateRegister((RegTreeNode) valTreeNode, null, true);
			return;
		}
		if (valTreeNode instanceof FieldTreeNode) {
			updateField((FieldTreeNode) valTreeNode);
			return;
		}
	}

	public void updateField(FieldTreeNode node) {
		try {
			setFieldsNotUpdated();
			debugger.getValueSync(node.getObjectID(), node.getRuntimeField());
			decodeRuntimeValue(node);
			debuggerPanel.updateThisTree(node);
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	public boolean updateRegister(RegTreeNode regNode, RuntimeType type, boolean retry) {
		if (type == null) {
			if (regNode.isAbsoluteType()) {
				type = castType(regNode.getType());
			} else {
				type = POSSIBLE_TYPES[0];
			}
		}
		boolean ok = false;
		RuntimeRegister register = null;
		try {
			register = debugger.getRegisterSync(
					cur.frame.getThreadID(),
					cur.frame.getFrame().getID(),
					regNode.getRuntimeRegNum(),
					type);
		} catch (SmaliDebuggerException e) {
			if (retry) {
				if (debugger.errIsTypeMismatched(e.getErrCode())) {
					RuntimeType[] types = getPossibleTypes(type);
					for (RuntimeType nextType : types) {
						ok = updateRegister(regNode, nextType, false);
						if (ok) {
							regNode.updateType(nextType.getDesc());
							break;
						}
					}
				} else {
					logErr(e.getMessage() + " for " + regNode.getName());
					regNode.updateType(null);
					regNode.updateValue(null);
				}
			}
		}
		if (register != null) {
			regNode.updateReg(register);
			decodeRuntimeValue(regNode);
		}
		debuggerPanel.updateRegTree(regNode);
		return ok;
	}

	private RuntimeType[] getPossibleTypes(RuntimeType cur) {
		RuntimeType[] types = new RuntimeType[2];
		for (int i = 0, j = 0; i < POSSIBLE_TYPES.length; i++) {
			if (cur != POSSIBLE_TYPES[i]) {
				types[j++] = POSSIBLE_TYPES[i];
			}
		}
		return types;
	}

	// when single stepping we can detect which reg need to be updated.
	private void markNextToBeUpdated(long codeOffset) {
		if (codeOffset != -1) {
			Object rst = cur.smali.getResultRegOrField(cur.mthFullID, codeOffset);
			toBeUpdatedTreeNode = null;
			if (cur.frame != null) {
				if (rst instanceof Integer) {
					int regNum = (int) rst;
					if (cur.frame.getRegNodes().size() > regNum) {
						toBeUpdatedTreeNode = cur.frame.getRegNodes().get(regNum);
					}
					return;
				}
				if (rst instanceof FieldInfo) {
					FieldInfo info = (FieldInfo) rst;
					toBeUpdatedTreeNode = cur.frame.getFieldNodes()
							.stream()
							.filter(f -> f.getName().equals(info.getName()))
							.findFirst()
							.orElse(null);
				}
			}
		}
	}

	private void updateAllThreads() {
		List<Long> threads;
		try {
			threads = debugger.getAllThreadsSync();
		} catch (SmaliDebuggerException e) {
			logErr(e);
			return;
		}
		List<ThreadBoxElement> threadEleList = new ArrayList<>(threads.size());
		for (Long thread : threads) {
			ThreadBoxElement ele = new ThreadBoxElement(thread);
			threadEleList.add(ele);
		}
		debuggerPanel.refreshThreadBox(threadEleList);
		lazyQueue.execute(() -> {
			for (ThreadBoxElement ele : threadEleList) { // get thread names
				try {
					ele.setName(debugger.getThreadNameSync(ele.getThreadID()));
				} catch (SmaliDebuggerException e) {
					logErr(e);
				}
			}
			debuggerPanel.refreshThreadBox(Collections.emptyList());
		});
	}

	private FrameNode updateAllStackFrames(long threadID) {
		List<SmaliDebugger.Frame> frames = Collections.emptyList();
		try {
			frames = debugger.getFramesSync(threadID);
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
		if (frames.size() == 0) {
			return null;
		}
		List<FrameNode> frameEleList = new ArrayList<>(frames.size());
		for (SmaliDebugger.Frame frame : frames) {
			FrameNode ele = new FrameNode(threadID, frame);
			frameEleList.add(ele);
		}
		FrameNode curEle = frameEleList.get(0);
		fetchStackFrameNames(curEle);

		debuggerPanel.refreshStackFrameList(frameEleList);
		lazyQueue.execute(() -> { // get class & method names for frames
			for (int i = 1; i < frameEleList.size(); i++) {
				fetchStackFrameNames(frameEleList.get(i));
			}
			debuggerPanel.refreshStackFrameList(Collections.emptyList());
		});
		return frameEleList.get(0);
	}

	private void fetchStackFrameNames(FrameNode ele) {
		try {
			long clsID = ele.getFrame().getClassID();
			String clsSig = debugger.getClassSignatureSync(clsID);
			String mthSig = debugger.getMethodSignatureSync(clsID, ele.getFrame().getMethodID());
			ele.setSignatures(clsSig, mthSig);
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	private Smali decodeSmali(FrameNode frame) {
		if (cur.frame.getClsSig() != null) {
			JClass jClass = DbgUtils.getTopClassBySig(frame.getClsSig(), debuggerPanel.getMainWindow());
			if (jClass != null) {
				ClassNode cNode = jClass.getCls().getClassNode();
				cur.clsNode = jClass;
				cur.mthFullID = DbgUtils.classSigToRawFullName(frame.getClsSig()) + "." + frame.getMthSig();
				return DbgUtils.getSmali(cNode);
			}
		}
		return null;
	}

	private void refreshCurFrame(long threadID, long codeOffset) {
		try {
			Frame frame = debugger.getCurrentFrame(threadID);
			cur.frame.setFrame(frame);
			cur.frame.updateCodeOffset(codeOffset);
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	private void updateAllFields(FrameNode frame) {
		List<FieldNode> fldNodes = Collections.emptyList();
		String clsSig = frame.getClsSig();
		if (clsSig != null) {
			ClassNode clsNode = DbgUtils.getClassNodeBySig(clsSig, debuggerPanel.getMainWindow());
			if (clsNode != null) {
				fldNodes = clsNode.getFields();
			}
		}
		try {
			long thisID = debugger.getThisID(frame.getThreadID(), frame.getFrame().getID());
			List<RuntimeField> flds = debugger.getAllFieldsSync(frame.getClsID());
			List<FieldTreeNode> nodes = new ArrayList<>(flds.size());
			for (RuntimeField fld : flds) {
				FieldTreeNode fldNode = new FieldTreeNode(fld, thisID);
				fldNodes.stream()
						.filter(f -> f.getName().equals(fldNode.getName()))
						.findFirst()
						.ifPresent(smaliFld -> fldNode.setAlias(smaliFld.getAlias()));
				nodes.add(fldNode);
			}
			debuggerPanel.updateThisFieldNodes(nodes);
			frame.setFieldNodes(nodes);
			if (thisID > 0 && nodes.size() > 0) {
				lazyQueue.execute(() -> updateAllFieldValues(thisID, frame));
			}
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	private void updateAllFieldValues(long thisID, FrameNode frame) {
		List<FieldTreeNode> nodes = frame.getFieldNodes();
		if (nodes.size() > 0) {
			List<FieldTreeNode> flds = new ArrayList<>(nodes.size());
			List<RuntimeField> rts = new ArrayList<>(nodes.size());
			nodes.forEach(n -> {
				RuntimeField f = n.getRuntimeField();
				if (f.isBelongToThis()) {
					flds.add(n);
					rts.add(f);
				}
			});
			try {
				debugger.getAllFieldValuesSync(thisID, rts);
				flds.forEach(n -> decodeRuntimeValue(n));
				debuggerPanel.refreshThisFieldTree();
			} catch (SmaliDebuggerException e) {
				logErr(e);
			}
		}
	}

	private void updateAllRegisters(FrameNode frame) {
		UiUtils.uiRun(() -> {
			if (!buildRegTreeNodes(frame).isEmpty()) {
				fetchAllRegisters(frame);
			}
		});
	}

	private void fetchAllRegisters(FrameNode frame) {
		List<SmaliRegister> regs = cur.regAdapter.getInitializedList(frame.getCodeOffset());
		for (SmaliRegister reg : regs) {
			Entry<String, String> info = cur.regAdapter.getInfo(reg.getRuntimeRegNum(), frame.getCodeOffset());
			RegTreeNode regNode = frame.getRegNodes().get(reg.getRegNum());
			if (info != null) {
				applyDbgInfo(regNode, info);
			}
			updateRegister(regNode, null, true);
		}
	}

	private void applyDbgInfo(RegTreeNode rn, Entry<String, String> info) {
		rn.setAlias(info.getKey());
		rn.updateType(info.getValue());
		rn.setAbsoluteType(true);
	}

	private void setRegsNotUpdated() {
		if (cur.frame != null) {
			for (RegTreeNode regNode : cur.frame.getRegNodes()) {
				regNode.setUpdated(false);
			}
		}
	}

	private void setFieldsNotUpdated() {
		if (cur.frame != null) {
			for (FieldTreeNode node : cur.frame.getFieldNodes()) {
				node.setUpdated(false);
			}
		}
	}

	private List<RegTreeNode> buildRegTreeNodes(FrameNode frame) {
		List<SmaliRegister> regs = cur.smali.getRegisterList(cur.mthFullID);
		List<RegTreeNode> regNodes = new ArrayList<>(regs.size());
		List<RegTreeNode> inRtOrder = new ArrayList<>(regs.size());

		regs.forEach(r -> {
			RegTreeNode rn = new RegTreeNode(r);
			regNodes.add(rn);
			inRtOrder.add(rn);
		});
		inRtOrder.sort(Comparator.comparingInt(RegTreeNode::getRuntimeRegNum));
		frame.setRegNodes(regNodes);
		debuggerPanel.updateRegTreeNodes(inRtOrder);
		debuggerPanel.refreshRegisterTree();
		return regNodes;
	}

	private boolean decodeRuntimeValue(RuntimeValueTreeNode valNode) {
		RuntimeValue rValue = valNode.getRuntimeValue();
		RuntimeType type = rValue.getType();
		if (!valNode.isAbsoluteType()) {
			valNode.updateType(null);
		}
		try {
			switch (type) {
				case OBJECT:
					return decodeObject(valNode);
				case STRING:
					String str = "\"" + debugger.readStringSync(rValue) + "\"";
					valNode.updateType("java.lang.String")
							.updateTypeID(debugger.readID(rValue))
							.updateValue(str);
					break;
				case INT:
					valNode.updateValue(Integer.toString(debugger.readInt(rValue)));
					break;
				case LONG:
					valNode.updateValue(Long.toString(debugger.readAll(rValue)));
					break;
				case ARRAY:
					decodeArrayVal(valNode);
					break;
				case BOOLEAN: {
					int b = debugger.readByte(rValue);
					valNode.updateValue(b == 1 ? "true" : "false");
					break;
				}
				case SHORT:
					valNode.updateValue(Short.toString(debugger.readShort(rValue)));
					break;
				case CHAR:
				case BYTE: {
					int b = (int) debugger.readAll(rValue);
					if (DbgUtils.isPrintableChar(b)) {
						valNode.updateValue(type == RuntimeType.CHAR ? String.valueOf((char) b) : String.valueOf((byte) b));
					} else {
						valNode.updateValue(String.valueOf(b));
					}
					break;
				}
				case DOUBLE:
					double d = debugger.readDouble(rValue);
					valNode.updateValue(Double.toString(d));
					break;
				case FLOAT:
					float f = debugger.readFloat(rValue);
					valNode.updateValue(Float.toString(f));
					break;
				case VOID:
					valNode.updateType("void");
					break;
				case THREAD:
					valNode.updateType("thread").updateTypeID(debugger.readID(rValue));
					break;
				case THREAD_GROUP:
					valNode.updateType("thread_group").updateTypeID(debugger.readID(rValue));
					break;
				case CLASS_LOADER:
					valNode.updateType("class_loader").updateTypeID(debugger.readID(rValue));
					break;
				case CLASS_OBJECT:
					valNode.updateType("class_object").updateTypeID(debugger.readID(rValue));
					break;
			}
		} catch (SmaliDebuggerException e) {
			logErr(e);
			return false;
		}
		return true;
	}

	private boolean decodeObject(RuntimeValueTreeNode valNode) {
		RuntimeValue rValue = valNode.getRuntimeValue();
		boolean ok = true;
		if (debugger.readID(rValue) == 0) {
			if (valNode.isAbsoluteType()) {
				valNode.updateValue("null");
				return ok;
			} else if (!art.readNullObject()) {
				valNode.updateType(art.typeForNull());
				valNode.updateValue("0");
				return ok;
			}
		}
		String sig;
		try {
			sig = debugger.readObjectSignatureSync(rValue);
			valNode.updateType(String.format("%s@%d", DbgUtils.classSigToRawFullName(sig),
					debugger.readID(rValue)));
		} catch (SmaliDebuggerException e) {
			ok = debugger.errIsInvalidObject(e.getErrCode()) && valNode instanceof RegTreeNode;
			if (ok) {
				try {
					RegTreeNode reg = (RegTreeNode) valNode;
					RuntimeRegister rr = debugger.getRegisterSync(
							cur.frame.getThreadID(),
							cur.frame.getFrame().getID(),
							reg.getRuntimeRegNum(), RuntimeType.INT);
					reg.updateReg(rr);
					rValue = rr;
					valNode.updateType(RuntimeType.INT.getDesc());
					valNode.updateValue(Long.toString((int) debugger.readAll(rValue)));
				} catch (SmaliDebuggerException except) {
					logErr(except, String.format("Update %s failed, %s", valNode.getName(), except.getMessage()));
					valNode.updateValue(except.getMessage());
					ok = false;
				}
			} else {
				logErr(e);
			}
		}
		return ok;
	}

	private void decodeArrayVal(RuntimeValueTreeNode valNode) throws SmaliDebuggerException {
		String type = debugger.readObjectSignatureSync(valNode.getRuntimeValue());
		ArgType argType = ArgType.parse(type);
		String javaType = argType.toString();
		Entry<Integer, List<Long>> ret = debugger.readArray(valNode.getRuntimeValue(), 0, 0);
		javaType = javaType.substring(0, javaType.length() - 1) + ret.getKey() + "]";
		valNode.updateType(javaType + "@" + debugger.readID(valNode.getRuntimeValue()));

		if (argType.getArrayElement().isPrimitive()) {
			for (Long aLong : ret.getValue()) {
				valNode.add(new DefaultMutableTreeNode(Long.toString(aLong)));
			}
			return;
		}
		String typeSig = type.substring(1);
		if (DbgUtils.isStringObjectSig(typeSig)) {
			for (Long aLong : ret.getValue()) {
				valNode.add(new DefaultMutableTreeNode(debugger.readStringSync(aLong)));
			}
			return;
		}
		typeSig = DbgUtils.classSigToRawFullName(typeSig);
		for (Long aLong : ret.getValue()) {
			valNode.add(new DefaultMutableTreeNode(String.format("%s@%d", typeSig, aLong)));
		}
	}

	private void updateAllInfo(long threadID, long codeOffset) {
		updateQueue.execute(() -> {
			resetAllInfo();
			cur.frame = updateAllStackFrames(threadID);
			if (cur.frame != null) {
				lazyQueue.execute(() -> updateAllFields(cur.frame));
				if (cur.frame.getClsSig() == null || cur.frame.getMthSig() == null) {
					fetchStackFrameNames(cur.frame);
				}
				cur.smali = decodeSmali(cur.frame);
				if (cur.smali != null) {
					cur.regAdapter = regAdaMap.computeIfAbsent(cur.mthFullID,
							k -> RegisterObserver.merge(
									getRuntimeDebugInfo(cur.frame),
									getRegisterList()));

					if (cur.smali.getRegCount(cur.mthFullID) > 0) {
						updateAllRegisters(cur.frame);
					}
					markCodeOffset(codeOffset);
				}
			}
			updateAllThreads();
		});
	}

	private List<SmaliRegister> getRegisterList() {
		int regCount = cur.smali.getRegCount(cur.mthFullID);
		int paramStart = cur.smali.getParamRegStart(cur.mthFullID);
		List<SmaliRegister> srs = cur.smali.getRegisterList(cur.mthFullID);
		for (SmaliRegister sr : srs) {
			sr.setRuntimeRegNum(art.getRuntimeRegNum(sr.getRegNum(), regCount, paramStart));
		}
		return srs;
	}

	private void resetAllInfo() {
		isSuspended = true;
		toBeUpdatedTreeNode = null;
		debuggerPanel.resetAllDebuggingInfo();
		cur.reset();
	}

	private List<RuntimeVarInfo> getRuntimeDebugInfo(FrameNode frame) {
		try {
			RuntimeDebugInfo dbgInfo = debugger.getRuntimeDebugInfo(frame.getClsID(), frame.getMthID());
			if (dbgInfo != null) {
				return dbgInfo.getInfoList();
			}
		} catch (SmaliDebuggerException ignore) {
			// logErr(e);
		}
		return Collections.emptyList();
	}

	private void markCodeOffset(long codeOffset) {
		scrollToPos(codeOffset);
		markNextToBeUpdated(codeOffset);
	}

	private void logErr(Exception e, String extra) {
		debuggerPanel.log(e.getMessage());
		debuggerPanel.log(extra);
		LOG.error(extra, e);
	}

	private void logErr(Exception e) {
		debuggerPanel.log(e.getMessage());
		LOG.error("Debug error", e);
	}

	private void logErr(String e) {
		debuggerPanel.log(e);
		LOG.error("Debug error: {}", e);
	}

	private void scrollToPos(long codeOffset) {
		int pos = -1;
		if (codeOffset > -1) {
			pos = cur.smali.getInsnPosByCodeOffset(cur.mthFullID, codeOffset);
		}
		if (pos == -1) {
			pos = cur.smali.getMethodDefPos(cur.mthFullID);
			if (pos == -1) {
				debuggerPanel.log("Can't scroll to " + cur.mthFullID);
				return;
			}
		}
		debuggerPanel.scrollToSmaliLine(cur.clsNode, pos, true);
	}

	private void initBreakpoints(List<FileBreakpoint> fbps) {
		if (fbps.size() == 0) {
			return;
		}
		boolean fetch = true;
		for (FileBreakpoint fbp : fbps) {
			try {
				long id = debugger.getClassID(fbp.cls, fetch);
				// only fetch classes from JVM once,
				// if this time this class hasn't been loaded then it won't load next time, cuz JVM is freezed.
				fetch = false;
				if (id > -1) {
					setBreakpoint(id, fbp);
				} else {
					setDelayBreakpoint(fbp);
				}
			} catch (SmaliDebuggerException e) {
				logErr(e);
				failBreakpoint(fbp, e.getMessage());
			}
		}
	}

	protected boolean setBreakpoint(FileBreakpoint bp) {
		if (!isDebugging()) {
			return true;
		}
		try {
			long cid = debugger.getClassID(bp.cls, true);
			if (cid > -1) {
				setBreakpoint(cid, bp);
			} else {
				setDelayBreakpoint(bp);
			}
		} catch (SmaliDebuggerException e) {
			logErr(e);
			BreakpointManager.failBreakpoint(bp);
			return false;
		}
		return true;
	}

	private void setDelayBreakpoint(FileBreakpoint bp) {
		boolean hasSet = bpStore.hasSetDelaied(bp.cls);
		bpStore.add(bp, null);
		if (!hasSet) {
			updateQueue.execute(() -> {
				try {
					debugger.regClassPrepareEventForBreakpoint(bp.cls, id -> {
						List<FileBreakpoint> list = bpStore.get(bp.cls);
						for (FileBreakpoint fbp : list) {
							setBreakpoint(id, fbp);
						}
					});
				} catch (SmaliDebuggerException e) {
					logErr(e);
					failBreakpoint(bp, "");
				}
			});
		}
	}

	protected void setBreakpoint(long cid, FileBreakpoint fbp) {
		try {
			long mid = debugger.getMethodID(cid, fbp.mth);
			if (mid > -1) {
				RuntimeBreakpoint rbp = debugger.makeBreakpoint(cid, mid, fbp.codeOffset);
				debugger.setBreakpoint(rbp);
				bpStore.add(fbp, rbp);
				return;
			}
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
		failBreakpoint(fbp, "Failed to get method for breakpoint, " + fbp.mth + ":" + fbp.codeOffset);
	}

	private void failBreakpoint(FileBreakpoint fbp, String msg) {
		if (!msg.isEmpty()) {
			debuggerPanel.log(msg);
		}
		bpStore.removeBreakpoint(fbp);
		BreakpointManager.failBreakpoint(fbp);
	}

	protected boolean removeBreakpoint(FileBreakpoint fbp) {
		if (!isDebugging()) {
			return true;
		}
		RuntimeBreakpoint rbp = bpStore.removeBreakpoint(fbp);
		if (rbp != null) {
			try {
				debugger.removeBreakpoint(rbp);
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return false;
			}
		}
		return true;
	}

	private static RuntimeBreakpoint delayBP = null;

	private class BreakpointStore {
		Map<FileBreakpoint, RuntimeBreakpoint> bpm = Collections.emptyMap();

		BreakpointStore() {
			if (delayBP == null) {
				delayBP = debugger.makeBreakpoint(-1, -1, -1);
			}
		}

		void reset() {
			bpm.clear();
		}

		boolean hasSetDelaied(String cls) {
			for (Entry<FileBreakpoint, RuntimeBreakpoint> entry : bpm.entrySet()) {
				if (entry.getValue() == delayBP && entry.getKey().cls.equals(cls)) {
					return true;
				}
			}
			return false;
		}

		List<FileBreakpoint> get(String cls) {
			List<FileBreakpoint> fbps = new ArrayList<>();
			bpm.forEach((k, v) -> {
				if (v == delayBP && k.cls.equals(cls)) {
					fbps.add(k);
					bpm.remove(k);
				}
			});
			return fbps;
		}

		void add(FileBreakpoint fbp, RuntimeBreakpoint rbp) {
			if (bpm == Collections.EMPTY_MAP) {
				bpm = new ConcurrentHashMap<>();
			}
			bpm.put(fbp, rbp == null ? delayBP : rbp);
		}

		RuntimeBreakpoint removeBreakpoint(FileBreakpoint fbp) {
			return bpm.remove(fbp);
		}
	}

	public class FrameNode implements IListElement {
		private SmaliDebugger.Frame frame;
		private final long threadID;
		private String clsSig;
		private String mthSig;
		private StringBuilder cache;
		private long codeOffset = -1;
		private List<RegTreeNode> regNodes;
		private List<FieldTreeNode> thisNodes;
		private long thisID;

		public FrameNode(long threadID, SmaliDebugger.Frame frame) {
			cache = new StringBuilder(DEFAULT_CACHE_SIZE);
			this.frame = frame;
			this.threadID = threadID;
			regNodes = Collections.emptyList();
			thisNodes = Collections.emptyList();
		}

		public SmaliDebugger.Frame getFrame() {
			return frame;
		}

		public void setFrame(SmaliDebugger.Frame frame) {
			this.frame = frame;
		}

		public long getClsID() {
			return frame.getClassID();
		}

		public long getMthID() {
			return frame.getMethodID();
		}

		public long getThreadID() {
			return threadID;
		}

		public long getThisID() {
			return thisID;
		}

		public void setThisID(long thisID) {
			this.thisID = thisID;
		}

		public void setSignatures(String clsSig, String mthSig) {
			this.clsSig = clsSig;
			this.mthSig = mthSig;
			resetCache();
		}

		public String getClsSig() {
			return clsSig;
		}

		public String getMthSig() {
			return mthSig;
		}

		public void updateCodeOffset(long codeOffset) {
			this.codeOffset = codeOffset;
			if (this.codeOffset > -1) {
				resetCache();
			}
		}

		public long getCodeOffset() {
			return codeOffset == -1 ? frame.getCodeIndex() : codeOffset;
		}

		public void setRegNodes(List<RegTreeNode> regNodes) {
			this.regNodes = regNodes;
		}

		public List<RegTreeNode> getRegNodes() {
			return regNodes;
		}

		public List<FieldTreeNode> getFieldNodes() {
			return thisNodes;
		}

		public void setFieldNodes(List<FieldTreeNode> thisNodes) {
			this.thisNodes = thisNodes;
		}

		@Override
		public void onSelected() {
			if (clsSig != null) {
				JClass cls = DbgUtils.getTopClassBySig(clsSig, debuggerPanel.getMainWindow());
				if (cls != null) {
					Smali smali = DbgUtils.getSmali(cls.getCls().getClassNode());
					if (smali != null) {
						int pos = smali.getInsnPosByCodeOffset(
								DbgUtils.classSigToRawFullName(clsSig) + "." + mthSig,
								getCodeOffset());
						debuggerPanel.scrollToSmaliLine(cls, Math.max(0, pos), true);
						return;
					}
				}
				debuggerPanel.log("Can't open smali panel for " + clsSig + "->" + mthSig);
			}
		}

		private void resetCache() {
			// Do not reuse thee existing cache instance as this can result in
			// multi-threading access issues in case toString() method is active
			this.cache = new StringBuilder(DEFAULT_CACHE_SIZE);
		}

		@Override
		public String toString() {
			StringBuilder sbCache = cache;
			if (sbCache.length() == 0) {
				long off = getCodeOffset();
				if (off < 0) {
					sbCache.append(String.format("index: %-4d ", off));
				} else {
					sbCache.append(String.format("index: %04x ", off));
				}
				if (clsSig == null) {
					sbCache.append("clsID: ").append(frame.getClassID());
				} else {
					sbCache.append(clsSig).append("->");
				}
				if (mthSig == null) {
					sbCache.append(" mthID: ").append(frame.getMethodID());
				} else {
					sbCache.append(mthSig);
				}
			}
			return sbCache.toString();
		}
	}

	private static class ThreadBoxElement implements IListElement {
		private long threadID;
		private String name;

		public ThreadBoxElement(long threadID) {
			this.threadID = threadID;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getThreadID() {
			return threadID;
		}

		@Override
		public String toString() {
			if (name == null) {
				return "thread id: " + threadID;
			}
			return "thread id: " + threadID + " name:" + name;
		}

		@Override
		public void onSelected() {

		}
	}

	private static class RegTreeNode extends RuntimeValueTreeNode {
		private static final long serialVersionUID = -1111111202103122234L;

		private final SmaliRegister smaliReg;
		private RuntimeRegister runtimeReg;
		private String value;
		private String type;
		private String alias;
		private boolean absType;

		public RegTreeNode(SmaliRegister smaliReg) {
			this.smaliReg = smaliReg;
		}

		public void updateReg(RuntimeRegister reg) {
			runtimeReg = reg;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		@Override
		public RegTreeNode updateValue(String value) {
			setUpdated(true);
			this.value = value;
			removeAllChildren();
			return this;
		}

		@Override
		public RegTreeNode updateType(String type) {
			if (this.type == null || !this.type.equals(type)) {
				this.type = type;
				reset();
			}
			return this;
		}

		private void reset() {
			value = null;
			removeAllChildren();
			setUpdated(true);
			this.absType = false;
			updateTypeID(0);
		}

		@Override
		public String getName() {
			if (!StringUtils.isEmpty(alias)) {
				return String.format("%s (%s)", smaliReg.getName(), alias);
			}
			return String.format("%-3s", smaliReg.getName());
		}

		@Override
		@Nullable
		public String getValue() {
			return value;
		}

		public RuntimeRegister getRuntimeReg() {
			return runtimeReg;
		}

		public int getRuntimeRegNum() {
			return smaliReg.getRuntimeRegNum();
		}

		@Override
		public String getType() {
			if (type != null) {
				return type;
			}
			if (runtimeReg != null) {
				return runtimeReg.getType().getDesc();
			}
			return null;
		}

		@Override
		public RuntimeValue getRuntimeValue() {
			return getRuntimeReg();
		}

		@Override
		public boolean isAbsoluteType() {
			return absType;
		}

		public void setAbsoluteType(boolean abs) {
			absType = abs;
		}
	}

	private static class FieldTreeNode extends RuntimeValueTreeNode {
		private static final long serialVersionUID = -1111111202103122235L;

		private final RuntimeField field;
		private String value;
		private String alias;
		private long objectID;

		private FieldTreeNode(RuntimeField field, long id) {
			this.field = field;
			objectID = id;
		}

		public long getObjectID() {
			return objectID;
		}

		public void setObjectID(long id) {
			this.objectID = id;
		}

		public RuntimeField getRuntimeField() {
			return this.field;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		@Override
		public FieldTreeNode updateValue(String val) {
			setUpdated(true);
			value = val;
			removeAllChildren();
			return this;
		}

		@Override
		public FieldTreeNode updateType(String val) {
			return this;
		}

		@Override
		public String getName() {
			if (StringUtils.isEmpty(alias) || alias.equals(field.getName())) {
				return field.getName();
			}
			return field.getName() + " (" + alias + ")";
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String getType() {
			return ArgType.parse(field.getFieldType()).toString();
		}

		@Override
		public RuntimeValue getRuntimeValue() {
			return field;
		}

		@Override
		public boolean isAbsoluteType() {
			return true;
		}
	}

	private abstract static class RuntimeValueTreeNode extends ValueTreeNode {
		private static final long serialVersionUID = -1111111202103260222L;
		private long typeID;

		@Override
		public ValueTreeNode updateTypeID(long id) {
			this.typeID = id;
			return this;
		}

		@Override
		public long getTypeID() {
			return this.typeID;
		}

		public abstract RuntimeValue getRuntimeValue();

		public abstract boolean isAbsoluteType();
	}

	private class CurrentInfo {
		JClass clsNode;
		String mthFullID;
		Smali smali;
		FrameNode frame;
		RegisterObserver regAdapter;

		public void reset() {
			frame = null;
			smali = null;
			clsNode = null;
			regAdapter = null;
			mthFullID = "";
		}
	}

	private static void initTypeMap() {
		TYPE_MAP.put("I", RuntimeType.INT);
		TYPE_MAP.put("Z", RuntimeType.INT);
		TYPE_MAP.put("B", RuntimeType.INT);
		TYPE_MAP.put("C", RuntimeType.INT);
		TYPE_MAP.put("F", RuntimeType.INT);
		TYPE_MAP.put("S", RuntimeType.INT);
		TYPE_MAP.put("V", RuntimeType.INT);
		TYPE_MAP.put("int", RuntimeType.INT);
		TYPE_MAP.put("boolean", RuntimeType.INT);
		TYPE_MAP.put("byte", RuntimeType.INT);
		TYPE_MAP.put("short", RuntimeType.INT);
		TYPE_MAP.put("char", RuntimeType.INT);
		TYPE_MAP.put("float", RuntimeType.INT);
		TYPE_MAP.put("void", RuntimeType.INT);
		TYPE_MAP.put("L", RuntimeType.LONG);
		TYPE_MAP.put("D", RuntimeType.LONG);
		TYPE_MAP.put("long", RuntimeType.LONG);
		TYPE_MAP.put("double", RuntimeType.LONG);
		TYPE_MAP.put("java.lang.String", RuntimeType.STRING);
		TYPE_MAP.put("Ljava/lang/String;", RuntimeType.STRING);
	}
}
