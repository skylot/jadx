package jadx.gui.device.debugger;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import io.reactivex.annotations.Nullable;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.device.debugger.BreakpointManager.FileBreakpoint;
import jadx.gui.device.debugger.JDebuggerPanel.*;
import jadx.gui.device.debugger.SmaliDebugger.*;
import jadx.gui.device.debugger.smali.Smali;
import jadx.gui.treemodel.JClass;

import static jadx.gui.device.debugger.SmaliDebugger.Type.*;

public final class DebugController implements SmaliDebugger.SuspendListener {
	private static final String ONCREATE_SIGNATURE = "onCreate(Landroid/os/Bundle;)V";

	private JDebuggerPanel debuggerPanel;
	private SmaliDebugger debugger;
	private long curThreadID;
	private long curClsID;
	private long curMthID;
	private final AtomicLong curCodeOffset = new AtomicLong(-1);
	private JClass curClsNode;
	private String curMthFullID;
	private Smali curSmali;

	private BreakpointStore bpStore;
	private boolean updateAllFldAndReg = false; // update all fields and registers
	private ValueTreeNode toBeUpdatedTreeNode; // a field or register number.
	private volatile boolean isSuspended = true;
	private ResumeCmd run;
	private ResumeCmd stepOver;
	private ResumeCmd stepInto;
	private ResumeCmd stepOut;
	private StateListener stateListener;

	/**
	 * fetches ids from remote, and show them in UI.
	 * First updateQueue got ids and then lazyQueue get names by ids.
	 */
	private final ExecutorService updateQueue = Executors.newSingleThreadExecutor();
	/**
	 * fetches info & updates UI according to the ids that updateQueue fetched,
	 * like the values of registers, thread names, or class/method names of stack frame.
	 */
	private final ExecutorService lazyQueue = Executors.newSingleThreadExecutor();

	protected boolean setDebugger(JDebuggerPanel debuggerPanel, String host, int port) {
		this.debuggerPanel = debuggerPanel;
		debuggerPanel.resetUI();
		try {
			debugger = SmaliDebugger.attach(host, port, this);
		} catch (SmaliDebuggerException e) {
			logErr(e);
			return false;
		}
		curThreadID = -1;
		curClsID = -1;
		curMthID = -1;
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

	public boolean isSuspended() {
		return isSuspended;
	}

	public boolean isDebugging() {
		return debugger != null;
	}

	public boolean run() {
		return execResumeCmd(run);
	}

	public boolean stepInto() {
		return execResumeCmd(stepInto);
	}

	public boolean stepOver() {
		return execResumeCmd(stepOver);
	}

	public boolean stepOut() {
		return execResumeCmd(stepOut);
	}

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

	/**
	 * Kills app
	 */
	public void stop() {
		if (isDebugging()) {
			try {
				debugger.exit();
			} catch (SmaliDebuggerException e) {
				logErr(e);
			}
		}
	}

	/**
	 * it not only kills app but also destroy JDebuggerPanel,
	 * exit completely.
	 */
	public void exit() {
		if (isDebugging()) {
			setDebuggerState(true, true);
			stop();
			debugger = null;
		}
		BreakpointManager.setDebugController(null);
		debuggerPanel.getMainWindow().destroyDebuggerPanel();
		debuggerPanel = null;
	}

	/**
	 * @param type must be one of int, long, float, double, string or object.
	 */
	public boolean setValue(ValueTreeNode valNode, Type type, Object value) {
		checkType(type, value);
		if (isDebugging() && isSuspended()) {
			return setValueInternal(valNode, type, value);
		}
		return false;
	}

	private void checkType(Type type, Object value) {
		if (!(type == INT && value instanceof Integer)
				&& !(type == STRING && value instanceof String)
				&& !(type == LONG && value instanceof Long)
				&& !(type == FLOAT && value instanceof Float)
				&& !(type == DOUBLE && value instanceof Double)
				&& !(type == OBJECT && value instanceof Long)) {
			throw new JadxRuntimeException("Type must be one of int, long, float, double, String or Object.");
		}
	}

	private boolean setValueInternal(ValueTreeNode valNode, Type type, Object value) {
		if (valNode instanceof RegTreeNode) {
			try {
				RegTreeNode regNode = (RegTreeNode) valNode;
				debugger.setValueSync(
						regNode.getRuntimeReg(),
						type,
						value,
						curThreadID,
						regNode.getFrameID());
				lazyQueue.execute(() -> {
					setRegsNotUpdated();
					updateRegister((RegTreeNode) valNode);
				});
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return false;
			}
		} else if (valNode instanceof FieldTreeNode) {
			// TODO: support field
			return false;
		}
		return true;
	}

	private interface ResumeCmd {
		void exec() throws SmaliDebuggerException;
	}

	private boolean execResumeCmd(ResumeCmd cmd) {
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
		if (stateListener != null) {
			stateListener.onStateChanged(suspended, stopped);
		}
	}

	protected void setDebuggerStateListener(StateListener listener) {
		stateListener = listener;
	}

	public long getCodeOffset() {
		return curCodeOffset.get();
	}

	public void updateRegister(RegTreeNode regNode) {
		try {
			RuntimeRegister register = debugger.getRegisterSync(
					regNode.getThreadID(),
					regNode.getFrameID(),
					regNode.getRuntimeRegNum());
			regNode.updateReg(register);
			decodeRuntimeValue(regNode);
			debuggerPanel.updateRegTree(regNode);
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	public void updateField(FieldNode fld) {

	}

	@Override
	public void onSuspendEvent(SmaliDebugger.SuspendInfo info) {
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
		int refreshLevel = 2; // update all threads, stack frames, registers and fields.
		if (info.getThreadID() != curThreadID) {
			curThreadID = info.getThreadID();
		}
		if (info.getClassID() != curClsID) {
			curClsID = info.getClassID();
		}
		if (info.getMethodID() != curMthID) {
			curMthID = info.getMethodID();
		} else {
			refreshLevel = 1; // relevant registers or fields.
		}
		if (refreshLevel == 2) {
			updateAllInfo(curThreadID, info.getOffset());

		} else {
			StackFrameElement frameEle = getCurrentFrame();
			if (curSmali != null) {
				if (updateAllFldAndReg) {
					if (frameEle != null) {
						debuggerPanel.resetAllRegAndFieldNodes();
						updateAllRegisters(curThreadID, frameEle, curSmali.getRegCount(curMthFullID));
					}
				} else if (toBeUpdatedTreeNode != null) {
					lazyQueue.execute(() -> updateRegOrField(toBeUpdatedTreeNode));
				}
				setRegsNotUpdated();
				markCodeOffset(info.getOffset());
			} else {
				debuggerPanel.resetAllRegNodes();
			}
			if (frameEle != null) {
				// update current code offset in stack frame.
				frameEle.updateCodeOffset(info.getOffset());
				debuggerPanel.refreshStackFrameList(Collections.emptyList());
			}
		}
	}

	private void updateRegOrField(ValueTreeNode valTreeNode) {
		if (valTreeNode instanceof RegTreeNode) {
			updateRegister((RegTreeNode) valTreeNode);
			return;
		}
		if (valTreeNode instanceof FieldTreeNode) {
			// TODO: support field
		}
	}

	// when single stepping we can detect which reg need to be updated.
	private void markNextToBeUpdated(long codeOffset) {
		if (codeOffset != -1) {
			Object rst = curSmali.getResultRegOrField(curMthFullID, codeOffset);
			StackFrameElement frameEle = getCurrentFrame();
			toBeUpdatedTreeNode = null;
			if (frameEle != null) {
				if (rst instanceof Integer) {
					int regNum = (int) rst;
					if (frameEle.getRegNodes().size() > regNum) {
						toBeUpdatedTreeNode = frameEle.getRegNodes().get(regNum);
					}
					return;
				}
				if (rst instanceof FieldInfo) {

				}
			}
		}
	}

	@Nullable
	private StackFrameElement getCurrentFrame() {
		DefaultListModel<StackFrameElement> model = debuggerPanel.getStackFrameModel();
		if (model.size() > 0) {
			return model.get(0);
		}
		return null;
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

	private StackFrameElement updateAllStackFrames(long threadID) {
		List<SmaliDebugger.Frame> frames;
		try {
			frames = debugger.getFramesSync(threadID);
		} catch (SmaliDebuggerException e) {
			logErr(e);
			return null;
		}
		if (frames.size() == 0) {
			return null;
		}
		List<StackFrameElement> frameEleList = new ArrayList<>(frames.size());
		for (SmaliDebugger.Frame frame : frames) {
			StackFrameElement ele = new StackFrameElement(frame);
			frameEleList.add(ele);
		}
		StackFrameElement curEle = frameEleList.get(0);
		fetchStackFrameNames(curEle);

		debuggerPanel.refreshStackFrameList(frameEleList);
		lazyQueue.execute(() -> { // get class & method names for frames
			for (int i = 1; i < frameEleList.size(); i++) {
				fetchStackFrameNames(frameEleList.get(i));
			}
			debuggerPanel.refreshStackFrameList(Collections.emptyList());
		});
		return curEle;
	}

	private void fetchStackFrameNames(StackFrameElement ele) {
		try {
			long clsID = ele.getFrame().getClassID();
			String clsSig = debugger.getClassSignatureSync(clsID);
			String mthSig = debugger.getMethodSignatureSync(clsID, ele.getFrame().getMethodID());
			ele.setSignatures(clsSig, mthSig);
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	private Smali decodeSmali(StackFrameElement frame) {
		JClass jClass = DbgUtils.getTopClassBySig(frame.getClsSig(), debuggerPanel.getMainWindow());
		if (jClass != null) {
			ClassNode cNode = jClass.getCls().getClassNode();
			curClsNode = jClass;
			curMthFullID = DbgUtils.classSigToRawFullName(frame.getClsSig()) + "." + frame.getMthSig();
			return DbgUtils.getSmali(cNode);
		}
		return null;
	}

	private void updateAllFields(StackFrameElement frameEle) {
		List<FieldNode> fldNodes = Collections.emptyList();
		String clsSig = frameEle.getClsSig();
		if (clsSig != null) {
			ClassNode clsNode = DbgUtils.getClassNodeBySig(clsSig, debuggerPanel.getMainWindow());
			if (clsNode != null) {
				fldNodes = clsNode.getFields();
			}
		}
		try {
			List<RuntimeField> flds = debugger.getAllFieldsSync(frameEle.getFrame().getClassID());
			List<FieldTreeNode> nodes = new ArrayList<>(flds.size());
			for (RuntimeField fld : flds) {
				FieldTreeNode fldNode = debuggerPanel.buildFieldNode(fld);
				fldNodes.stream()
						.filter(f -> f.getName().equals(fldNode.getName()))
						.findFirst()
						.ifPresent(smaliFld -> fldNode.setAlias(smaliFld.getAlias()));
				nodes.add(fldNode);
			}
			debuggerPanel.setThisFieldNodes(nodes);
			// if (nodes.size() > 0) {
			// lazyQueue.execute(() -> updateAllFieldValues(frameEle));
			// }
		} catch (SmaliDebuggerException e) {
			logErr(e);
		}
	}

	// TODO: has bug
	private void updateAllFieldValues(StackFrameElement frameEle) {
		List<FieldTreeNode> nodes = debuggerPanel.getThisFieldNodes();
		if (nodes.size() > 0) {
			List<RuntimeField> flds = new ArrayList<>(nodes.size());
			nodes.forEach(n -> flds.add(n.getRuntimeField()));
			try {
				debugger.getAllFieldValuesSync(frameEle.getFrame().getClassID(), flds);
				nodes.forEach(this::decodeRuntimeValue);
				debuggerPanel.refreshThisFieldTree();
			} catch (SmaliDebuggerException e) {
				logErr(e);
			}
		}
	}

	private void updateAllRegisters(long threadID, StackFrameElement frameEle, int regCount) {
		if (regCount > 0) {
			List<RuntimeRegister> regs;
			try {
				regs = debugger.getAllRegistersSync(threadID, frameEle.getFrame().getID(), regCount);
			} catch (SmaliDebuggerException e) {
				logErr(e);
				return;
			}
			List<RegTreeNode> regNodes = buildRegTreeNodes(regs, threadID, frameEle);
			debuggerPanel.refreshRegisterTree();
			lazyQueue.execute(() -> {
				boolean updated = false;
				for (RegTreeNode regNode : regNodes) {
					boolean ok = decodeRuntimeValue(regNode);
					if (!updated) {
						updated = ok;
					}
				}
				if (updated) {
					debuggerPanel.refreshRegisterTree();
				}
			});
		}
	}

	private void setRegsNotUpdated() {
		StackFrameElement frame = getCurrentFrame();
		if (frame != null) {
			for (RegTreeNode regNode : frame.getRegNodes()) {
				regNode.setUpdated(false);
			}
		}
	}

	private List<RegTreeNode> buildRegTreeNodes(List<RuntimeRegister> regs, long threadID, StackFrameElement frameEle) {
		List<RegTreeNode> regNodes = new ArrayList<>(regs.size());
		for (RuntimeRegister reg : regs) {
			int smaliRegNum = curSmali.getSmaliRegNum(curMthFullID, reg.getRegNum());
			regNodes.add(debuggerPanel.buildRegNode(reg,
					curSmali.getSmaliReg(curMthFullID, smaliRegNum),
					threadID,
					frameEle.getFrame().getID()));
		}
		regNodes.sort(Comparator.comparingInt(RegTreeNode::getRegNum));
		debuggerPanel.updateRegTreeNodes(regNodes);
		frameEle.setRegNodes(regNodes);
		return regNodes;
	}

	private boolean decodeRuntimeValue(ValueTreeNode valNode) {
		valNode.setUpdated(false);
		RuntimeValue rValue = valNode.getRuntimeValue();
		if (rValue.getType() == OBJECT) {
			return decodeObject(valNode);
		}
		Type type = rValue.getType();
		try {
			switch (type) {
				case STRING:
					String str = "\"" + debugger.readStringSync(rValue) + "\"";
					valNode.updateType("java.lang.String@" + debugger.readID(rValue));
					valNode.updateValue(str);
					break;
				case BOOLEAN: {
					int b = debugger.readByte(rValue);
					valNode.updateValue(b == 1 ? "true" : "false");
					break;
				}
				case INT:
				case LONG:
				case SHORT:
					long l = debugger.readAll(rValue);
					valNode.updateValue(Long.toString(l));
					break;
				case ARRAY:
					decodeArrayVal(valNode);
					break;
				case CHAR:
				case BYTE: {
					int b = (int) debugger.readAll(rValue);
					if (DbgUtils.isPrintableChar(b)) {
						valNode.updateValue(type == CHAR ? String.valueOf((char) b) : String.valueOf((byte) b));
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
					valNode.updateType(String.format("thread@%x", debugger.readID(rValue)));
					break;
				case THREAD_GROUP:
					valNode.updateType(String.format("thread_group@%x", debugger.readID(rValue)));
					break;
				case CLASS_LOADER:
					valNode.updateType(String.format("class_loader@%x", debugger.readID(rValue)));
					break;
				case CLASS_OBJECT:
					valNode.updateType(String.format("class_object@%x", debugger.readID(rValue)));
					break;
			}

		} catch (SmaliDebuggerException e) {
			logErr(e);
			return false;
		}
		return true;
	}

	private boolean decodeObject(ValueTreeNode valNode) {
		String sig;
		RuntimeValue rValue = valNode.getRuntimeValue();
		boolean ok = true;
		try {
			// TODO: fetch fields of object.
			sig = debugger.readObjectSignatureSync(rValue);
			valNode.updateType(String.format("%s@%x", DbgUtils.classSigToRawFullName(sig),
					debugger.readID(rValue)));
		} catch (SmaliDebuggerException e) {
			ok = e.getErrCode() == 20 && valNode instanceof RegTreeNode;
			if (ok) {
				// it's not an object id or it's unloaded, gc collected, mostly the former one, so treat it as int
				// TODO: update smali parser to distinguish double, float and int.
				try {
					RegTreeNode reg = (RegTreeNode) valNode;
					RuntimeRegister rr = debugger.getRegisterSync(
							curThreadID,
							getCurrentFrame().getFrame().getID(),
							reg.getRuntimeRegNum(), SmaliDebugger.Type.INT);
					reg.updateReg(rr);
					rValue = rr;
					valNode.updateType("int");
					valNode.updateValue(Long.toString((int) debugger.readAll(rValue)));
				} catch (SmaliDebuggerException except) {
					logErr(except, String.format("Update %s failed, %s", valNode.getName(), except.getMessage()));
					valNode.updateValue(except.getMessage());
				}
			} else {
				logErr(e);
			}
		}
		return ok;
	}

	private void decodeArrayVal(ValueTreeNode valNode) throws SmaliDebuggerException {
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
			// fetch all the Strings
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
			StackFrameElement curFrame = updateAllStackFrames(threadID);
			if (curFrame != null) {
				lazyQueue.execute(() -> updateAllFields(curFrame));
				if (curFrame.getClsSig() == null || curFrame.getMthSig() == null) {
					fetchStackFrameNames(curFrame);
				}
				if (curFrame.getClsSig() != null) {
					curSmali = decodeSmali(curFrame);
					if (curSmali != null) {
						int regCount = curSmali.getRegCount(curMthFullID);
						if (regCount > 0) {
							updateAllRegisters(threadID, curFrame, regCount);
						}
						markCodeOffset(codeOffset);
					}
				}
			}
			updateAllThreads();
		});
	}

	private void resetAllInfo() {
		toBeUpdatedTreeNode = null;
		curCodeOffset.set(-1);
		debuggerPanel.resetAllDebuggingInfo();
	}

	private void markCodeOffset(long codeOffset) {
		scrollToPos(codeOffset);
		markNextToBeUpdated(codeOffset);
	}

	private void logErr(Exception e, String extra) {
		debuggerPanel.log(e.getMessage());
		debuggerPanel.log(extra);
		e.printStackTrace();
	}

	private void logErr(Exception e) {
		debuggerPanel.log(e.getMessage());
		e.printStackTrace();
	}

	private void scrollToPos(long codeOffset) {
		int pos = -1;
		if (codeOffset > -1) {
			pos = curSmali.getInsnPosByCodeOffset(curMthFullID, codeOffset);
		}
		if (pos == -1) {
			pos = curSmali.getMethodDefPos(curMthFullID);
			if (pos == -1) {
				debuggerPanel.log("Can't scroll to " + curMthFullID);
				return;
			}
		}
		debuggerPanel.scrollToSmaliLine(curClsNode, pos, true);
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
		failBreakpoint(fbp, "Failed get method for breakpoint, " + fbp.mth + ":" + fbp.codeOffset);
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

	protected interface StateListener {
		void onStateChanged(boolean suspended, boolean stopped);
	}
}
