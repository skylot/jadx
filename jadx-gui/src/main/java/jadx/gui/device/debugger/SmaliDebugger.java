package jadx.gui.device.debugger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.hqktech.JDWP;
import io.github.hqktech.JDWP.ArrayReference.Length.LengthReplyData;
import io.github.hqktech.JDWP.ByteBuffer;
import io.github.hqktech.JDWP.Event.Composite.*;
import io.github.hqktech.JDWP.EventRequest.Set.ClassMatchRequest;
import io.github.hqktech.JDWP.EventRequest.Set.CountRequest;
import io.github.hqktech.JDWP.EventRequest.Set.LocationOnlyRequest;
import io.github.hqktech.JDWP.EventRequest.Set.StepRequest;
import io.github.hqktech.JDWP.Method.VariableTableWithGeneric.VarTableWithGenericData;
import io.github.hqktech.JDWP.Method.VariableTableWithGeneric.VarWithGenericSlot;
import io.github.hqktech.JDWP.ObjectReference;
import io.github.hqktech.JDWP.ObjectReference.ReferenceType.ReferenceTypeReplyData;
import io.github.hqktech.JDWP.ObjectReference.SetValues.FieldValueSetter;
import io.github.hqktech.JDWP.Packet;
import io.github.hqktech.JDWP.ReferenceType.FieldsWithGeneric.FieldsWithGenericData;
import io.github.hqktech.JDWP.ReferenceType.FieldsWithGeneric.FieldsWithGenericReplyData;
import io.github.hqktech.JDWP.ReferenceType.MethodsWithGeneric.MethodsWithGenericData;
import io.github.hqktech.JDWP.ReferenceType.MethodsWithGeneric.MethodsWithGenericReplyData;
import io.github.hqktech.JDWP.ReferenceType.Signature.SignatureReplyData;
import io.github.hqktech.JDWP.StackFrame.GetValues.GetValuesReplyData;
import io.github.hqktech.JDWP.StackFrame.GetValues.GetValuesSlots;
import io.github.hqktech.JDWP.StackFrame.SetValues.SlotValueSetter;
import io.github.hqktech.JDWP.StackFrame.ThisObject.ThisObjectReplyData;
import io.github.hqktech.JDWP.StringReference.Value.ValueReplyData;
import io.github.hqktech.JDWP.ThreadReference.Frames.FramesReplyData;
import io.github.hqktech.JDWP.ThreadReference.Frames.FramesReplyDataFrames;
import io.github.hqktech.JDWP.ThreadReference.Name.NameReplyData;
import io.github.hqktech.JDWP.VirtualMachine.AllClassesWithGeneric.AllClassesWithGenericData;
import io.github.hqktech.JDWP.VirtualMachine.AllClassesWithGeneric.AllClassesWithGenericReplyData;
import io.github.hqktech.JDWP.VirtualMachine.AllThreads.AllThreadsReplyData;
import io.github.hqktech.JDWP.VirtualMachine.AllThreads.AllThreadsReplyDataThreads;
import io.github.hqktech.JDWP.VirtualMachine.CreateString.CreateStringReplyData;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.gui.device.debugger.smali.RegisterInfo;
import jadx.gui.utils.ObjectPool;

// TODO: Finish error notification, inner errors should be logged let user notice.

public class SmaliDebugger {

	private final JDWP jdwp;
	private int localTcpPort;
	private InputStream inputStream;
	private OutputStream outputStream;

	// All event callbacks will be called in this queue, e.g. class prepare/unload
	private static final Executor EVENT_LISTENER_QUEUE = Executors.newSingleThreadExecutor();

	// Handle callbacks of single step, breakpoint and watchpoint
	private static final Executor SUSPEND_LISTENER_QUEUE = Executors.newSingleThreadExecutor();

	private final Map<Integer, ICommandResult> callbackMap = new ConcurrentHashMap<>();
	private final Map<Integer, EventListenerAdapter> eventListenerMap = new ConcurrentHashMap<>();

	private final Map<String, AllClassesWithGenericData> classMap = new ConcurrentHashMap<>();
	private final Map<Long, AllClassesWithGenericData> classIDMap = new ConcurrentHashMap<>();
	private final Map<Long, List<MethodsWithGenericData>> clsMethodMap = new ConcurrentHashMap<>();
	private final Map<Long, List<FieldsWithGenericData>> clsFieldMap = new ConcurrentHashMap<>();
	private Map<Long, Map<Long, RuntimeDebugInfo>> varMap = Collections.emptyMap(); // cls id: <mth id: var table>

	private final CountRequest oneOffEventReq;
	private final AtomicInteger idGenerator = new AtomicInteger(1);

	private final SuspendInfo suspendInfo = new SuspendInfo();
	private final SuspendListener suspendListener;

	private ObjectPool<List<GetValuesSlots>> slotsPool;
	private ObjectPool<List<JDWP.EventRequestEncoder>> stepReqPool;
	private ObjectPool<SynchronousQueue<Packet>> syncQueuePool;
	private ObjectPool<List<Long>> fieldIdPool;
	private final Map<Integer, Thread> syncQueueMap = new ConcurrentHashMap<>();
	private final AtomicInteger syncQueueID = new AtomicInteger(0);

	private static final ICommandResult SKIP_RESULT = res -> {
	};

	private SmaliDebugger(SuspendListener suspendListener, int localTcpPort, JDWP jdwp) {
		this.jdwp = jdwp;
		this.localTcpPort = localTcpPort;
		this.suspendListener = suspendListener;

		oneOffEventReq = jdwp.eventRequest().cmdSet().newCountRequest();
		oneOffEventReq.count = 1;
	}

	/**
	 * After a successful attach the remote app will be suspended, so we have times to
	 * set breakpoints or do any other things, after that call resume() to activate the app.
	 */
	public static SmaliDebugger attach(String host, int port, SuspendListener suspendListener) throws SmaliDebuggerException {
		try {
			byte[] bytes = JDWP.IDSizes.encode().getBytes();
			JDWP.setPacketID(bytes, 1);
			Socket socket = new Socket(host, port);
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();

			socket.setSoTimeout(5000);
			JDWP jdwp = initJDWP(outputStream, inputStream);
			socket.setSoTimeout(0); // set back to 0 so the decodingLoop won't break for timeout.

			SmaliDebugger debugger = new SmaliDebugger(suspendListener, port, jdwp);
			debugger.inputStream = inputStream;
			debugger.outputStream = outputStream;

			debugger.decodingLoop();
			debugger.listenClassUnloadEvent();
			debugger.initPools();
			return debugger;
		} catch (IOException e) {
			throw new SmaliDebuggerException(e);
		}
	}

	private void onSuspended(long thread, long clazz, long mth, long offset) {
		suspendInfo.update()
				.updateThread(thread)
				.updateClass(clazz)
				.updateMethod(mth)
				.updateOffset(offset);
		if (suspendInfo.isAnythingChanged()) {
			SUSPEND_LISTENER_QUEUE.execute(() -> suspendListener.onSuspendEvent(suspendInfo));
		}
	}

	public void stepInto() throws SmaliDebuggerException {
		sendStepRequest(suspendInfo.getThreadID(), JDWP.StepDepth.INTO);
	}

	public void stepOver() throws SmaliDebuggerException {
		sendStepRequest(suspendInfo.getThreadID(), JDWP.StepDepth.OVER);
	}

	public void stepOut() throws SmaliDebuggerException {
		sendStepRequest(suspendInfo.getThreadID(), JDWP.StepDepth.OUT);
	}

	public void exit() throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.virtualMachine().cmdExit().encode(-1));
		tryThrowError(res);
	}

	public void detach() throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.virtualMachine().cmdDispose().encode());
		tryThrowError(res);
	}

	private void initPools() {
		slotsPool = new ObjectPool<>(() -> {
			List<GetValuesSlots> slots = new ArrayList<>(1);
			GetValuesSlots slot = jdwp.stackFrame().cmdGetValues().newValuesSlots();
			slot.slot = 0;
			slot.sigbyte = JDWP.Tag.OBJECT;
			slots.add(slot);
			return slots;
		});
		stepReqPool = new ObjectPool<>(() -> {
			List<JDWP.EventRequestEncoder> eventEncoders = new ArrayList<>(2);
			eventEncoders.add(jdwp.eventRequest().cmdSet().newStepRequest());
			eventEncoders.add(oneOffEventReq);
			return eventEncoders;
		});
		syncQueuePool = new ObjectPool<>(SynchronousQueue::new);
		fieldIdPool = new ObjectPool<>(() -> {
			List<Long> ids = new ArrayList<>(1);
			ids.add((long) -1);
			return ids;
		});
	}

	/**
	 * @param regNum If it's an argument, just pass its index, non-static method should be index + 1.
	 *               e.g. void a(int b, int c), you want the value of b, then should pass 1 (0 + 1),
	 *               this is a virtual method, so 0 is for the this object and 1 is the real index of b.
	 *               <p>
	 *               If it's a variable then should be the reg number + number of arguments and + 1
	 *               if it's in a non-static method.
	 *               e.g. to get the value of v3 in a virtual method with 2 arguments, should pass
	 *               6 (3 + 2 + 1 = 6).
	 */
	public RuntimeRegister getRegisterSync(long threadID, long frameID, int regNum, RuntimeType type) throws SmaliDebuggerException {
		List<GetValuesSlots> slots = slotsPool.get();
		GetValuesSlots slot = slots.get(0);
		slot.slot = regNum;
		slot.sigbyte = (byte) type.getTag();
		Packet res = sendCommandSync(jdwp.stackFrame().cmdGetValues().encode(threadID, frameID, slots));
		tryThrowError(res);
		slotsPool.put(slots);
		GetValuesReplyData val = jdwp.stackFrame().cmdGetValues().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return buildRegister(regNum, val.values.get(0).slotValue.tag, val.values.get(0).slotValue.idOrValue);
	}

	public long getThisID(long threadID, long frameID) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.stackFrame().cmdThisObject().encode(threadID, frameID));
		tryThrowError(res);
		ThisObjectReplyData data = jdwp.stackFrame().cmdThisObject().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return data.objectThis.objectID;
	}

	public List<RuntimeField> getAllFieldsSync(long clsID) throws SmaliDebuggerException {
		return getAllFields(clsID);
	}

	public void getFieldValueSync(long clsID, RuntimeField fld) throws SmaliDebuggerException {
		List<RuntimeField> list = new ArrayList<>(1);
		list.add(fld);
		getAllFieldValuesSync(clsID, list);
	}

	public void getAllFieldValuesSync(long thisID, List<RuntimeField> flds) throws SmaliDebuggerException {
		List<Long> ids = new ArrayList<>(flds.size());
		flds.forEach(f -> ids.add(f.getFieldID()));
		Packet res = sendCommandSync(jdwp.objectReference().cmdGetValues().encode(thisID, ids));
		tryThrowError(res);
		ObjectReference.GetValues.GetValuesReplyData data =
				jdwp.objectReference().cmdGetValues().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		List<ObjectReference.GetValues.GetValuesReplyDataValues> values = data.values;
		for (int i = 0; i < values.size(); i++) {
			ObjectReference.GetValues.GetValuesReplyDataValues value = values.get(i);
			flds.get(i).setValue(value.value.idOrValue)
					.setType(getType(value.value.tag));
		}
	}

	public Frame getCurrentFrame(long threadID) throws SmaliDebuggerException {
		return getCurrentFrameInternal(threadID);
	}

	public List<Frame> getFramesSync(long threadID) throws SmaliDebuggerException {
		return getAllFrames(threadID);
	}

	public List<Long> getAllThreadsSync() throws SmaliDebuggerException {
		return getAllThreads();
	}

	@Nullable
	public String getThreadNameSync(long threadID) throws SmaliDebuggerException {
		return sendThreadNameReq(threadID);
	}

	@Nullable
	public String getClassSignatureSync(long classID) throws SmaliDebuggerException {
		return getClassSignatureInternal(classID);
	}

	@Nullable
	public String getMethodSignatureSync(long classID, long methodID) throws SmaliDebuggerException {
		return getMethodSignatureInternal(classID, methodID);
	}

	public boolean errIsTypeMismatched(int errCode) {
		return errCode == JDWP.Error.TYPE_MISMATCH;
	}

	public boolean errIsInvalidSlot(int errCode) {
		return errCode == JDWP.Error.INVALID_SLOT;
	}

	public boolean errIsInvalidObject(int errCode) {
		return errCode == JDWP.Error.INVALID_OBJECT;
	}

	private static class ClassListenerInfo {
		int prepareReqID;
		int unloadReqID;
		ClassListener listener;

		void reset(ClassListener l) {
			this.listener = l;
			this.prepareReqID = -1;
			this.unloadReqID = -1;
		}
	}

	private ClassListenerInfo clsListener;

	/**
	 * Listens for class preparation and unload events.
	 */
	public void setClassListener(ClassListener listener) throws SmaliDebuggerException {
		if (clsListener != null) {
			if (listener != clsListener.listener) {
				unregisterEventSync(JDWP.EventKind.CLASS_PREPARE, clsListener.prepareReqID);
				unregisterEventSync(JDWP.EventKind.CLASS_UNLOAD, clsListener.unloadReqID);
			}
		} else {
			clsListener = new ClassListenerInfo();
		}
		clsListener.reset(listener);
		regClassPrepareEvent(clsListener);
		regClassUnloadEvent(clsListener);
	}

	private void regClassUnloadEvent(ClassListenerInfo info) throws SmaliDebuggerException {
		Packet res = sendCommandSync(
				jdwp.eventRequest().cmdSet().newClassExcludeRequest((byte) JDWP.EventKind.CLASS_UNLOAD,
						(byte) JDWP.SuspendPolicy.NONE, "java.*"));
		tryThrowError(res);
		info.unloadReqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		eventListenerMap.put(info.unloadReqID, new EventListenerAdapter() {
			@Override
			void onClassUnload(ClassUnloadEvent event) {
				info.listener.onUnloaded(DbgUtils.classSigToRawFullName(event.signature));
			}
		});
	}

	private void regClassPrepareEvent(ClassListenerInfo info) throws SmaliDebuggerException {
		Packet res = sendCommandSync(
				jdwp.eventRequest().cmdSet().newClassExcludeRequest((byte) JDWP.EventKind.CLASS_PREPARE,
						(byte) JDWP.SuspendPolicy.NONE, "java.*"));
		tryThrowError(res);
		info.prepareReqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		eventListenerMap.put(info.prepareReqID, new EventListenerAdapter() {
			@Override
			void onClassPrepare(ClassPrepareEvent event) {
				info.listener.onPrepared(DbgUtils.classSigToRawFullName(event.signature), event.typeID);
			}
		});
	}

	public void regClassPrepareEventForBreakpoint(String clsSig, ClassPrepareListener l) throws SmaliDebuggerException {
		Packet res = sendCommandSync(buildClassMatchReqForBreakpoint(clsSig, JDWP.EventKind.CLASS_PREPARE));
		tryThrowError(res);
		int reqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		eventListenerMap.put(reqID, new EventListenerAdapter() {
			@Override
			void onClassPrepare(ClassPrepareEvent event) {
				EVENT_LISTENER_QUEUE.execute(() -> {
					try {
						l.onPrepared(event.typeID);
					} finally {
						eventListenerMap.remove(reqID);
						try {
							resume();
						} catch (SmaliDebuggerException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
	}

	public interface MethodEntryListener {
		/**
		 * return ture to remove
		 */
		boolean entry(String mthSig);
	}

	public void regMethodEntryEventSync(String clsSig, MethodEntryListener l) throws SmaliDebuggerException {
		Packet res = sendCommandSync(
				jdwp.eventRequest().cmdSet().newClassMatchRequest((byte) JDWP.EventKind.METHOD_ENTRY,
						(byte) JDWP.SuspendPolicy.ALL, clsSig));
		tryThrowError(res);
		int reqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		eventListenerMap.put(reqID, new EventListenerAdapter() {
			@Override
			void onMethodEntry(MethodEntryEvent event) {
				EVENT_LISTENER_QUEUE.execute(() -> {
					boolean removeListener = false;
					try {
						String sig = getMethodSignatureInternal(event.location.classID, event.location.methodID);
						removeListener = l.entry(sig);
						if (removeListener) {
							sendCommand(jdwp.eventRequest().cmdClear().encode((byte) JDWP.EventKind.METHOD_ENTRY, reqID), SKIP_RESULT);
							onSuspended(event.thread, event.location.classID, event.location.methodID, -1);
							eventListenerMap.remove(reqID);
						}
					} catch (SmaliDebuggerException e) {
						e.printStackTrace();
					} finally {
						if (!removeListener) {
							try {
								resume();
							} catch (SmaliDebuggerException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		});
	}

	private void unregisterEventSync(int eventKind, int reqID) throws SmaliDebuggerException {
		eventListenerMap.remove(reqID);
		Packet rst = sendCommandSync(jdwp.eventRequest().cmdClear().encode((byte) eventKind, reqID));
		tryThrowError(rst);
	}

	public String readObjectSignatureSync(RuntimeValue val) throws SmaliDebuggerException {
		long objID = readID(val);
		// get type reference by object id.
		Packet res = sendCommandSync(jdwp.objectReference().cmdReferenceType().encode(objID));
		tryThrowError(res);
		ReferenceTypeReplyData data = jdwp.objectReference().cmdReferenceType().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);

		// get signature by type reference id.
		res = sendCommandSync(jdwp.referenceType().cmdSignature().encode(data.typeID));
		tryThrowError(res);
		SignatureReplyData sigData = jdwp.referenceType().cmdSignature().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return sigData.signature;
	}

	public String readStringSync(RuntimeValue val) throws SmaliDebuggerException {
		return readStringSync(readID(val));
	}

	public String readStringSync(long id) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.stringReference().cmdValue().encode(id));
		tryThrowError(res);
		ValueReplyData strData = jdwp.stringReference().cmdValue().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return strData.stringValue;
	}

	public boolean setValueSync(int runtimeRegNum, RuntimeType type, Object val, long threadID, long frameID)
			throws SmaliDebuggerException {
		if (type == RuntimeType.STRING) {
			long newID = createString((String) val);
			if (newID == -1) {
				return false;
			}
			val = newID;
			type = RuntimeType.OBJECT;
		}
		List<SlotValueSetter> setters = buildRegValueSetter(type.getTag(), runtimeRegNum);
		JDWP.encodeAny(setters.get(0).slotValue.idOrValue, val);
		Packet res = sendCommandSync(jdwp.stackFrame().cmdSetValues().encode(threadID, frameID, setters));
		tryThrowError(res);
		return jdwp.stackFrame().cmdSetValues().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
	}

	public boolean setValueSync(long objID, long fldID, RuntimeType type, Object val) throws SmaliDebuggerException {
		if (type == RuntimeType.STRING) {
			long newID = createString((String) val);
			if (newID == -1) {
				return false;
			}
			val = newID;
		}
		List<FieldValueSetter> setters = buildFieldValueSetter();
		FieldValueSetter setter = setters.get(0);
		setter.fieldID = fldID;
		JDWP.encodeAny(setter.value.idOrValue, val);
		Packet res = sendCommandSync(jdwp.objectReference().cmdSetValues().encode(objID, setters));
		tryThrowError(res);
		return jdwp.objectReference().cmdSetValues().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
	}

	public void getValueSync(long objID, RuntimeField fld) throws SmaliDebuggerException {
		List<Long> ids = fieldIdPool.get();
		ids.set(0, fld.getFieldID());
		Packet res = sendCommandSync(jdwp.objectReference().cmdGetValues().encode(objID, ids));
		tryThrowError(res);
		ObjectReference.GetValues.GetValuesReplyData data =
				jdwp.objectReference().cmdGetValues().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		fld.setValue(data.values.get(0).value.idOrValue)
				.setType(getType(data.values.get(0).value.tag));
	}

	private long createString(String localStr) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.virtualMachine().cmdCreateString().encode(localStr));
		tryThrowError(res);
		CreateStringReplyData id = jdwp.virtualMachine().cmdCreateString().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return id.stringObject;
	}

	public long readID(RuntimeValue val) {
		return JDWP.decodeBySize(val.getRawVal().getBytes(), 0, val.getRawVal().size());
	}

	public String readArraySignature(RuntimeValue val) throws SmaliDebuggerException {
		return readObjectSignatureSync(val);
	}

	public int readArrayLength(RuntimeValue val) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.arrayReference().cmdLength().encode(readID(val)));
		tryThrowError(res);
		LengthReplyData data = jdwp.arrayReference().cmdLength().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return data.arrayLength;
	}

	/**
	 * @param startIndex less than 0 means 0
	 * @param len        less than or equals 0 means the maximum value 99 or the rest of the elements.
	 * @return An entry, The key is the total length of this array when len is <= 0, otherwise 0,
	 *         the value, if this array is an object array then it's object ids.
	 */
	public Entry<Integer, List<Long>> readArray(RuntimeValue reg, int startIndex, int len) throws SmaliDebuggerException {
		long id = readID(reg);
		Entry<Integer, List<Long>> ret;
		if (len <= 0) {
			Packet res = sendCommandSync(jdwp.arrayReference().cmdLength().encode(id));
			tryThrowError(res);
			LengthReplyData data = jdwp.arrayReference().cmdLength().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
			len = Math.min(99, data.arrayLength);
			ret = new SimpleEntry<>(data.arrayLength, null);
		} else {
			ret = new SimpleEntry<>(0, null);
		}
		startIndex = Math.max(0, startIndex);
		Packet res = sendCommandSync(jdwp.arrayReference().cmdGetValues().encode(id, startIndex, len));
		tryThrowError(res);
		JDWP.ArrayReference.GetValues.GetValuesReplyData valData =
				jdwp.arrayReference().cmdGetValues().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		ret.setValue(valData.values.idOrValues);
		return ret;
	}

	public byte readByte(RuntimeValue val) {
		return JDWP.decodeByte(val.getRawVal().getBytes(), 0);
	}

	public char readChar(RuntimeValue val) {
		return JDWP.decodeChar(val.getRawVal().getBytes(), 0);
	}

	public short readShort(RuntimeValue val) {
		return JDWP.decodeShort(val.getRawVal().getBytes(), 0);
	}

	public int readInt(RuntimeValue val) {
		return JDWP.decodeInt(val.getRawVal().getBytes(), 0);
	}

	public float readFloat(RuntimeValue val) {
		return JDWP.decodeFloat(val.getRawVal().getBytes(), 0);
	}

	/**
	 * @param val has 8 bytes mostly
	 */
	public long readAll(RuntimeValue val) {
		return JDWP.decodeBySize(val.getRawVal().getBytes(), 0, Math.min(val.getRawVal().size(), 8));
	}

	public double readDouble(RuntimeValue val) {
		return JDWP.decodeDouble(val.getRawVal().getBytes(), 0);
	}

	@Nullable
	public RuntimeDebugInfo getRuntimeDebugInfo(long clsID, long mthID) throws SmaliDebuggerException {
		Map<Long, RuntimeDebugInfo> secMap = varMap.get(clsID);
		RuntimeDebugInfo info = null;
		if (secMap != null) {
			info = secMap.get(mthID);
		}
		if (info == null) {
			info = initDebugInfo(clsID, mthID);
		}
		return info;
	}

	private RuntimeDebugInfo initDebugInfo(long clsID, long mthID) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.method().cmdVariableTableWithGeneric.encode(clsID, mthID));
		tryThrowError(res);
		VarTableWithGenericData data = jdwp.method().cmdVariableTableWithGeneric.decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		if (varMap == Collections.EMPTY_MAP) {
			varMap = new ConcurrentHashMap<>();
		}
		RuntimeDebugInfo info = new RuntimeDebugInfo(data);
		varMap.computeIfAbsent(clsID, k -> new HashMap<>()).put(mthID, info);
		return info;
	}

	private static JDWP initJDWP(OutputStream outputStream, InputStream inputStream) throws SmaliDebuggerException {
		try {
			handShake(outputStream, inputStream);
			outputStream.write(JDWP.Suspend.encode().setPacketID(1).getBytes()); // suspend all threads
			Packet res = readPacket(inputStream);
			tryThrowError(res);
			if (res.isReplyPacket() && res.getID() == 1) {
				outputStream.write(JDWP.IDSizes.encode().setPacketID(1).getBytes()); // get id sizes for decoding & encoding of jdwp
				// packets.
				res = readPacket(inputStream);
				tryThrowError(res);
				if (res.isReplyPacket() && res.getID() == 1) {
					JDWP.IDSizes.IDSizesReplyData sizes = JDWP.IDSizes.decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
					return new JDWP(sizes);
				}
			}
		} catch (IOException e) {
			throw new SmaliDebuggerException(e);
		}
		throw new SmaliDebuggerException("Failed to init JDWP.");
	}

	private static void handShake(OutputStream outputStream, InputStream inputStream) throws SmaliDebuggerException {
		byte[] buf = new byte[14];
		try {
			outputStream.write(JDWP.encodeHandShakePacket());
			inputStream.read(buf, 0, 14);
		} catch (Exception e) {
			throw new SmaliDebuggerException("jdwp handshake failed, " + e.getMessage());
		}
		if (!JDWP.decodeHandShakePacket(buf)) {
			throw new SmaliDebuggerException("jdwp handshake failed.");
		}
	}

	private MethodsWithGenericData getMethodBySig(long classID, String sig) {
		List<MethodsWithGenericData> methods = clsMethodMap.get(classID);
		if (methods != null) {
			for (MethodsWithGenericData method : methods) {
				if (sig.startsWith(method.name + "(") && sig.endsWith(method.signature)) {
					return method;
				}
			}
		}
		return null;
	}

	private int genID() {
		return idGenerator.getAndAdd(1);
	}

	private static byte[] appendBytes(byte[] buf1, byte[] buf2) {
		byte[] tempBuf = new byte[buf1.length + buf2.length];
		System.arraycopy(buf1, 0, tempBuf, 0, buf1.length);
		System.arraycopy(buf2, 0, tempBuf, buf1.length, buf2.length);
		return tempBuf;
	}

	/**
	 * Read & decode packets from Socket connection
	 */
	private void decodingLoop() {
		Executors.newSingleThreadExecutor().execute(() -> {
			boolean errFromCallback;
			for (;;) {
				errFromCallback = false;
				try {
					Packet res = readPacket(inputStream);
					suspendInfo.nextRound();
					ICommandResult callback = callbackMap.remove(res.getID());
					if (callback != null) {
						if (callback != SKIP_RESULT) {
							errFromCallback = true;
							callback.onCommandReply(res);
						}
						continue;
					}
					if (res.getCommandSetID() == 64 && res.getCommandID() == 100) { // command from JVM
						errFromCallback = true;
						decodeCompositeEvents(res);
					} else {
						printUnexpectedID(res.getID());
					}
				} catch (SmaliDebuggerException e) {
					e.printStackTrace();
					if (!errFromCallback) { // fatal error
						break;
					}
				}
			}
			suspendInfo.setTerminated();
			clearWaitingSyncQueue();
			suspendListener.onSuspendEvent(suspendInfo);
		});
	}

	private void sendCommand(ByteBuffer buf, ICommandResult callback) throws SmaliDebuggerException {
		int id = genID();
		callbackMap.put(id, callback);
		try {
			outputStream.write(buf.setPacketID(id).getBytes());
		} catch (IOException e) {
			throw new SmaliDebuggerException(e);
		}
	}

	/**
	 * Do not use this method inside a ICommandResult callback, it will cause deadlock.
	 * It should be used in a thread.
	 */
	private Packet sendCommandSync(ByteBuffer buf) throws SmaliDebuggerException {
		SynchronousQueue<Packet> store = syncQueuePool.get();
		sendCommand(buf, res -> {
			try {
				store.put(res);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		Integer id = syncQueueID.getAndAdd(1);
		try {
			syncQueueMap.put(id, Thread.currentThread());
			return store.take();
		} catch (InterruptedException e) {
			throw new SmaliDebuggerException(e);
		} finally {
			syncQueueMap.remove(id);
			syncQueuePool.put(store);
		}
	}

	// called by decodingLoop() when fatal error occurred,
	// if don't do so the store.take() may block forever.
	private void clearWaitingSyncQueue() {
		syncQueueMap.keySet().forEach(k -> {
			Thread t = syncQueueMap.remove(k);
			if (t != null) {
				t.interrupt();
			}
		});
	}

	private void printUnexpectedID(int id) throws SmaliDebuggerException {
		throw new SmaliDebuggerException("Missing handler for this id: " + id);
	}

	private void decodeCompositeEvents(Packet res) throws SmaliDebuggerException {
		EventData data = jdwp.event().cmdComposite().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		for (JDWP.EventRequestDecoder event : data.events) {
			EventListenerAdapter listener = eventListenerMap.get(event.getRequestID());
			if (listener == null) {
				try {
					printUnexpectedID(event.getRequestID());
				} catch (Exception e) {
					e.printStackTrace();
				}
				continue;
			}
			if (event instanceof VMStartEvent) {
				listener.onVMStart((VMStartEvent) event);
				return;
			}
			if (event instanceof VMDeathEvent) {
				listener.onVMDeath((VMDeathEvent) event);
				return;
			}
			if (event instanceof SingleStepEvent) {
				listener.onSingleStep((SingleStepEvent) event);
				return;
			}
			if (event instanceof BreakpointEvent) {
				listener.onBreakpoint((BreakpointEvent) event);
				return;
			}
			if (event instanceof MethodEntryEvent) {
				listener.onMethodEntry((MethodEntryEvent) event);
				return;
			}
			if (event instanceof MethodExitEvent) {
				listener.onMethodExit((MethodExitEvent) event);
				return;
			}
			if (event instanceof MethodExitWithReturnValueEvent) {
				listener.onMethodExitWithReturnValue((MethodExitWithReturnValueEvent) event);
				return;
			}
			if (event instanceof MonitorContendedEnterEvent) {
				listener.onMonitorContendedEnter((MonitorContendedEnterEvent) event);
				return;
			}
			if (event instanceof MonitorContendedEnteredEvent) {
				listener.onMonitorContendedEntered((MonitorContendedEnteredEvent) event);
				return;
			}
			if (event instanceof MonitorWaitEvent) {
				listener.onMonitorWait((MonitorWaitEvent) event);
				return;
			}
			if (event instanceof MonitorWaitedEvent) {
				listener.onMonitorWaited((MonitorWaitedEvent) event);
				return;
			}
			if (event instanceof ExceptionEvent) {
				listener.onException((ExceptionEvent) event);
				return;
			}
			if (event instanceof ThreadStartEvent) {
				listener.onThreadStart((ThreadStartEvent) event);
				return;
			}
			if (event instanceof ThreadDeathEvent) {
				listener.onThreadDeath((ThreadDeathEvent) event);
				return;
			}
			if (event instanceof ClassPrepareEvent) {
				listener.onClassPrepare((ClassPrepareEvent) event);
				return;
			}
			if (event instanceof ClassUnloadEvent) {
				listener.onClassUnload((ClassUnloadEvent) event);
				return;
			}
			if (event instanceof FieldAccessEvent) {
				listener.onFieldAccess((FieldAccessEvent) event);
				return;
			}
			if (event instanceof FieldModificationEvent) {
				listener.onFieldModification((FieldModificationEvent) event);
				return;
			}
			throw new SmaliDebuggerException("Unexpected event: " + event);
		}
	}

	private final EventListenerAdapter stepListener = new EventListenerAdapter() {
		@Override
		void onSingleStep(SingleStepEvent event) {
			onSuspended(event.thread,
					event.location.classID,
					event.location.methodID,
					event.location.index);
		}
	};

	private void sendStepRequest(long threadID, int depth) throws SmaliDebuggerException {
		List<JDWP.EventRequestEncoder> stepReq = buildStepRequest(threadID, JDWP.StepSize.MIN, depth);
		ByteBuffer stepEncodedBuf = jdwp.eventRequest().cmdSet().encode(
				(byte) JDWP.EventKind.SINGLE_STEP,
				(byte) JDWP.SuspendPolicy.ALL,
				stepReq);
		stepReqPool.put(stepReq);
		sendCommand(stepEncodedBuf, res -> {
			tryThrowError(res);
			int reqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
			eventListenerMap.put(reqID, stepListener);
		});
		resume();
	}

	public void resume() throws SmaliDebuggerException {
		sendCommand(JDWP.Resume.encode(), SKIP_RESULT);
	}

	public void suspend() throws SmaliDebuggerException {
		sendCommand(JDWP.Suspend.encode(), SKIP_RESULT);
	}

	public void setBreakpoint(RuntimeBreakpoint bp) throws SmaliDebuggerException {
		sendCommand(buildBreakpointRequest(bp), res -> {
			tryThrowError(res);
			bp.reqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
			eventListenerMap.put(bp.reqID, new EventListenerAdapter() {
				@Override
				void onBreakpoint(BreakpointEvent event) {
					onSuspended(event.thread,
							event.location.classID,
							event.location.methodID,
							event.location.index);
				}
			});
		});
	}

	public long getClassID(String clsSig, boolean fetch) throws SmaliDebuggerException {
		do {
			AllClassesWithGenericData data = classMap.get(clsSig);
			if (data == null) {
				if (fetch) {
					getAllClasses();
					fetch = false;
					continue;
				}
				break;
			} else {
				return data.typeID;
			}
		} while (true);
		return -1;
	}

	public long getMethodID(long cid, String mthSig) throws SmaliDebuggerException {
		initClassCache(cid);
		MethodsWithGenericData data = getMethodBySig(cid, mthSig);
		if (data != null) {
			return data.methodID;
		}
		return -1;
	}

	public void initClassCache(long clsID) throws SmaliDebuggerException {
		initFields(clsID);
		initMethods(clsID);
	}

	public void removeBreakpoint(RuntimeBreakpoint bp) throws SmaliDebuggerException {
		sendCommand(jdwp.eventRequest().cmdClear().encode((byte) JDWP.EventKind.BREAKPOINT, bp.reqID), SKIP_RESULT);
	}

	private ByteBuffer buildBreakpointRequest(RuntimeBreakpoint bp) {
		LocationOnlyRequest req = jdwp.eventRequest().cmdSet().newLocationOnlyRequest();
		req.loc.classID = bp.clsID;
		req.loc.methodID = bp.mthID;
		req.loc.index = bp.offset;
		req.loc.tag = JDWP.TypeTag.CLASS;
		List<JDWP.EventRequestEncoder> list = new ArrayList<>(1);
		list.add(req);
		return jdwp.eventRequest().cmdSet().encode((byte) JDWP.EventKind.BREAKPOINT,
				(byte) JDWP.SuspendPolicy.ALL, list);
	}

	/**
	 * Builds a one-off class prepare event for setting up breakpoints.
	 */
	private ByteBuffer buildClassMatchReqForBreakpoint(String cls, int eventKind) {
		List<JDWP.EventRequestEncoder> encoders = new ArrayList<>(2);
		ClassMatchRequest match = jdwp.eventRequest().cmdSet().newClassMatchRequest();
		encoders.add(match);
		encoders.add(oneOffEventReq);
		match.classPattern = cls;
		return jdwp.eventRequest().cmdSet().encode((byte) eventKind,
				(byte) JDWP.SuspendPolicy.ALL, encoders);
	}

	private List<JDWP.EventRequestEncoder> buildStepRequest(long threadID, int stepSize, int stepDepth) {
		List<JDWP.EventRequestEncoder> eventEncoders = stepReqPool.get();
		StepRequest req = (StepRequest) eventEncoders.get(0);
		req.size = stepSize;
		req.depth = stepDepth;
		req.thread = threadID;
		return eventEncoders;
	}

	private List<FieldValueSetter> buildFieldValueSetter() {
		FieldValueSetter setter = jdwp.objectReference().cmdSetValues().new FieldValueSetter();
		setter.value = jdwp.new UntaggedValuePacket();
		setter.value.idOrValue = new ByteBuffer();
		List<FieldValueSetter> setters = new ArrayList<>(1);
		setters.add(setter);
		return setters;
	}

	private List<SlotValueSetter> buildRegValueSetter(int tag, int regNum) {
		List<SlotValueSetter> setters = new ArrayList<>(1);
		SlotValueSetter setter = jdwp.stackFrame().cmdSetValues().new SlotValueSetter();
		setters.add(setter);
		setter.slot = regNum;
		setter.slotValue = jdwp.new ValuePacket();
		setter.slotValue.tag = tag;
		setter.slotValue.idOrValue = new ByteBuffer();
		return setters;
	}

	private String getClassSignatureInternal(long id) throws SmaliDebuggerException {
		AllClassesWithGenericData data = classIDMap.get(id);
		if (data == null) {
			getAllClasses();
		}
		data = classIDMap.get(id);
		if (data != null) {
			return data.signature;
		}
		return null;
	}

	private String getMethodSignatureInternal(long clsID, long mthID) throws SmaliDebuggerException {
		List<MethodsWithGenericData> mthData = clsMethodMap.get(clsID);
		if (mthData == null) {
			Packet res = sendCommandSync(jdwp.referenceType().cmdMethodsWithGeneric().encode(clsID));
			tryThrowError(res);
			MethodsWithGenericReplyData data =
					jdwp.referenceType().cmdMethodsWithGeneric().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
			mthData = data.declared;
			clsMethodMap.put(clsID, mthData);
		}
		if (mthData != null) {
			for (MethodsWithGenericData data : mthData) {
				if (data.methodID == mthID) {
					return data.name + data.signature;
				}
			}
		}
		return null;
	}

	private String sendThreadNameReq(long id) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.threadReference().cmdName().encode(id));
		tryThrowError(res);
		NameReplyData nameData = jdwp.threadReference().cmdName().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		return nameData.threadName;
	}

	private List<RuntimeField> getAllFields(long clsID) throws SmaliDebuggerException {
		initFields(clsID);
		List<FieldsWithGenericData> flds = clsFieldMap.get(clsID);
		if (flds != null && flds.size() > 0) {
			List<RuntimeField> rfs = new ArrayList<>(flds.size());
			for (FieldsWithGenericData fld : flds) {
				String type = fld.signature;
				if (fld.genericSignature != null && !fld.genericSignature.trim().isEmpty()) {
					type += "<" + fld.genericSignature + ">";
				}
				rfs.add(new RuntimeField(fld.name, type, fld.fieldID, fld.modBits));
			}
			return rfs;
		}
		return Collections.emptyList();
	}

	public Frame getCurrentFrameInternal(long threadID) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.threadReference().cmdFrames().encode(threadID, 0, 1));
		tryThrowError(res);
		FramesReplyData frameData = jdwp.threadReference().cmdFrames().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		FramesReplyDataFrames frame = frameData.frames.get(0);
		return new Frame(frame.frameID, frame.location.classID, frame.location.methodID,
				frame.location.index);
	}

	private List<Frame> getAllFrames(long threadID) throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.threadReference().cmdFrames().encode(threadID, 0, -1));
		tryThrowError(res);
		FramesReplyData frameData = jdwp.threadReference().cmdFrames().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		List<Frame> frames = new ArrayList<>();
		for (FramesReplyDataFrames frame : frameData.frames) {
			frames.add(new Frame(frame.frameID, frame.location.classID,
					frame.location.methodID, frame.location.index));
		}
		return frames;
	}

	private List<Long> getAllThreads() throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.virtualMachine().cmdAllThreads().encode());
		tryThrowError(res);
		AllThreadsReplyData data;
		data = jdwp.virtualMachine().cmdAllThreads().decode(res.getBuf(),
				JDWP.PACKET_HEADER_SIZE);
		List<Long> threads = new ArrayList<>(data.threads.size());
		for (AllThreadsReplyDataThreads thread : data.threads) {
			threads.add(thread.thread);
		}
		return threads;
	}

	private void getAllClasses() throws SmaliDebuggerException {
		Packet res = sendCommandSync(jdwp.virtualMachine().cmdAllClassesWithGeneric().encode());
		tryThrowError(res);
		AllClassesWithGenericReplyData classData =
				jdwp.virtualMachine().cmdAllClassesWithGeneric().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
		for (AllClassesWithGenericData aClass : classData.classes) {
			classMap.put(DbgUtils.classSigToRawFullName(aClass.signature), aClass);
			classIDMap.put(aClass.typeID, aClass);
		}
	}

	private void initFields(long clsID) throws SmaliDebuggerException {
		if (clsFieldMap.get(clsID) == null) {
			Packet res = sendCommandSync(jdwp.referenceType().cmdFieldsWithGeneric().encode(clsID));
			tryThrowError(res);
			FieldsWithGenericReplyData data =
					jdwp.referenceType().cmdFieldsWithGeneric().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
			clsFieldMap.put(clsID, data.declared);
		}
	}

	private void initMethods(long clsID) throws SmaliDebuggerException {
		if (clsMethodMap.get(clsID) == null) {
			Packet res = sendCommandSync(jdwp.referenceType().cmdMethodsWithGeneric().encode(clsID));
			tryThrowError(res);
			MethodsWithGenericReplyData data =
					jdwp.referenceType().cmdMethodsWithGeneric().decode(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
			clsMethodMap.put(clsID, data.declared);
		}
	}

	/**
	 * Removes class cache when it's unloaded from JVM.
	 */
	private void listenClassUnloadEvent() throws SmaliDebuggerException {
		sendCommand(
				jdwp.eventRequest().cmdSet().encode((byte) JDWP.EventKind.CLASS_UNLOAD,
						(byte) JDWP.SuspendPolicy.NONE, Collections.emptyList()),
				res -> {
					int reqID = jdwp.eventRequest().cmdSet().decodeRequestID(res.getBuf(), JDWP.PACKET_HEADER_SIZE);
					eventListenerMap.put(reqID, new EventListenerAdapter() {
						@Override
						void onClassUnload(ClassUnloadEvent event) {
							EVENT_LISTENER_QUEUE.execute(() -> {
								System.out.printf("ClassUnloaded: %s%n", event.signature);
								AllClassesWithGenericData clsData = classMap.remove(event.signature);
								if (clsData != null) {
									classIDMap.remove(clsData.typeID);
									clsFieldMap.remove(clsData.typeID);
									clsMethodMap.remove(clsData.typeID);
									varMap.remove(clsData.typeID);
								}
							});
						}
					});
				});
	}

	/**
	 * Reads a JDWP packet.
	 */
	private static Packet readPacket(InputStream inputStream) throws SmaliDebuggerException {
		byte[] bytes = new byte[JDWP.PACKET_HEADER_SIZE];
		try {
			if (inputStream.read(bytes, 0, bytes.length) == bytes.length) {
				int len = JDWP.getPacketLength(bytes, 0) - JDWP.PACKET_HEADER_SIZE;
				if (len > 0) {
					byte[] payload = new byte[len];
					int readSize = 0;
					do {
						readSize += inputStream.read(payload, readSize, len - readSize);
						if (readSize == len) {
							bytes = appendBytes(bytes, payload);
							break;
						}
					} while (true);
				}
				return Packet.make(bytes);
			}
		} catch (IOException e) {
			throw new SmaliDebuggerException(e);
		}
		throw new SmaliDebuggerException("read packet failed.");
	}

	private static void tryThrowError(Packet res) throws SmaliDebuggerException {
		if (res.isError()) {
			throw new SmaliDebuggerException("(JDWP Error Code:" + res.getErrorCode() + ") "
					+ res.getErrorText(), res.getErrorCode());
		}
	}

	private interface ICommandResult {
		void onCommandReply(Packet res) throws SmaliDebuggerException;
	}

	private abstract class EventListenerAdapter {
		void onVMStart(VMStartEvent event) {
		}

		void onVMDeath(VMDeathEvent event) {
		}

		void onSingleStep(SingleStepEvent event) {
		}

		void onBreakpoint(BreakpointEvent event) {
		}

		void onMethodEntry(MethodEntryEvent event) {
		}

		void onMethodExit(MethodExitEvent event) {
		}

		void onMethodExitWithReturnValue(MethodExitWithReturnValueEvent event) {
		}

		void onMonitorContendedEnter(MonitorContendedEnterEvent event) {
		}

		void onMonitorContendedEntered(MonitorContendedEnteredEvent event) {
		}

		void onMonitorWait(MonitorWaitEvent event) {
		}

		void onMonitorWaited(MonitorWaitedEvent event) {
		}

		void onException(ExceptionEvent event) {
		}

		void onThreadStart(ThreadStartEvent event) {
		}

		void onThreadDeath(ThreadDeathEvent event) {
		}

		void onClassPrepare(ClassPrepareEvent event) {
		}

		void onClassUnload(ClassUnloadEvent event) {
		}

		void onFieldAccess(FieldAccessEvent event) {
		}

		void onFieldModification(FieldModificationEvent event) {
		}
	}

	public static class RuntimeField extends RuntimeValue {
		private final String name;
		private final String fldType;
		private final long fieldID;
		private final int modBits;

		private RuntimeField(String name, String type, long fieldID, int modBits) {
			super(null, null);
			this.name = name;
			this.fldType = type;
			this.fieldID = fieldID;
			this.modBits = modBits;
		}

		public String getFieldType() {
			return fldType;
		}

		public String getName() {
			return name;
		}

		public long getFieldID() {
			return fieldID;
		}

		private RuntimeField setValue(ByteBuffer rawVal) {
			super.rawVal = rawVal;
			return this;
		}

		public boolean isBelongToThis() {
			return !AccessFlags.hasFlag(modBits, AccessFlags.STATIC)
					&& !AccessFlags.hasFlag(modBits, AccessFlags.SYNTHETIC);
		}
	}

	public static class RuntimeBreakpoint {
		private long clsID;
		private long mthID;
		private long offset;
		private int reqID;

		public long getCodeOffset() {
			return offset;
		}
	}

	public RuntimeBreakpoint makeBreakpoint(long cid, long mid, long offset) {
		RuntimeBreakpoint bp = new RuntimeBreakpoint();
		bp.clsID = cid;
		bp.mthID = mid;
		bp.offset = offset;
		return bp;
	}

	private RuntimeRegister buildRegister(int num, int tag, ByteBuffer buf) throws SmaliDebuggerException {
		return new RuntimeRegister(num, getType(tag), buf);
	}

	private RuntimeType getType(int tag) throws SmaliDebuggerException {
		switch (tag) {
			case JDWP.Tag.ARRAY:
				return RuntimeType.ARRAY;
			case JDWP.Tag.BYTE:
				return RuntimeType.BYTE;
			case JDWP.Tag.CHAR:
				return RuntimeType.CHAR;
			case JDWP.Tag.OBJECT:
				return RuntimeType.OBJECT;
			case JDWP.Tag.FLOAT:
				return RuntimeType.FLOAT;
			case JDWP.Tag.DOUBLE:
				return RuntimeType.DOUBLE;
			case JDWP.Tag.INT:
				return RuntimeType.INT;
			case JDWP.Tag.LONG:
				return RuntimeType.LONG;
			case JDWP.Tag.SHORT:
				return RuntimeType.SHORT;
			case JDWP.Tag.VOID:
				return RuntimeType.VOID;
			case JDWP.Tag.BOOLEAN:
				return RuntimeType.BOOLEAN;
			case JDWP.Tag.STRING:
				return RuntimeType.STRING;
			case JDWP.Tag.THREAD:
				return RuntimeType.THREAD;
			case JDWP.Tag.THREAD_GROUP:
				return RuntimeType.THREAD_GROUP;
			case JDWP.Tag.CLASS_LOADER:
				return RuntimeType.CLASS_LOADER;
			case JDWP.Tag.CLASS_OBJECT:
				return RuntimeType.CLASS_OBJECT;
			default:
				throw new SmaliDebuggerException("Unexpected value: " + tag);
		}
	}

	public static class RuntimeValue {
		protected ByteBuffer rawVal;
		protected RuntimeType type;

		RuntimeValue(RuntimeType type, ByteBuffer rawVal) {
			this.rawVal = rawVal;
			this.type = type;
		}

		public RuntimeType getType() {
			return type;
		}

		public void setType(RuntimeType type) {
			this.type = type;
		}

		private ByteBuffer getRawVal() {
			return rawVal;
		}
	}

	public static class RuntimeRegister extends RuntimeValue {
		private final int num;

		private RuntimeRegister(int num, RuntimeType type, ByteBuffer rawVal) {
			super(type, rawVal);
			this.num = num;
		}

		public int getRegNum() {
			return num;
		}
	}

	public static class RuntimeVarInfo extends RegisterInfo {
		private final VarWithGenericSlot slot;

		private RuntimeVarInfo(VarWithGenericSlot slot) {
			this.slot = slot;
		}

		@Override
		public String getName() {
			return slot.name;
		}

		@Override
		public int getRegNum() {
			return slot.slot;
		}

		@Override
		public String getType() {
			String gen = getSignature();
			return gen.isEmpty() ? this.slot.signature : gen;
		}

		@NonNull
		@Override
		public String getSignature() {
			return this.slot.genericSignature.trim();
		}

		@Override
		public int getStartOffset() {
			return (int) slot.codeIndex;
		}

		@Override
		public int getEndOffset() {
			return (int) (slot.codeIndex + slot.length);
		}
	}

	public static class RuntimeDebugInfo {
		private final List<RuntimeVarInfo> infoList;

		private RuntimeDebugInfo(VarTableWithGenericData data) {
			infoList = new ArrayList<>(data.slots.size());
			for (VarWithGenericSlot slot : data.slots) {
				infoList.add(new RuntimeVarInfo(slot));
			}
		}

		public List<RuntimeVarInfo> getInfoList() {
			return infoList;
		}
	}

	public enum RuntimeType {
		ARRAY(91, "[]"),
		BYTE(66, "byte"),
		CHAR(67, "char"),
		OBJECT(76, "object"),
		FLOAT(70, "float"),
		DOUBLE(68, "double"),
		INT(73, "int"),
		LONG(74, "long"),
		SHORT(83, "short"),
		VOID(86, "void"),
		BOOLEAN(90, "boolean"),
		STRING(115, "string"),
		THREAD(116, "thread"),
		THREAD_GROUP(103, "thread_group"),
		CLASS_LOADER(108, "class_loader"),
		CLASS_OBJECT(99, "class_object");

		private final int jdwpTag;
		private final String desc;

		RuntimeType(int tag, String desc) {
			this.jdwpTag = tag;
			this.desc = desc;
		}

		private int getTag() {
			return jdwpTag;
		}

		public String getDesc() {
			return this.desc;
		}
	}

	public static class Frame {
		private final long id;
		private final long clsID;
		private final long mthID;
		private final long index;

		private Frame(long id, long clsID, long mthID, long index) {
			this.id = id;
			this.clsID = clsID;
			this.mthID = mthID;
			this.index = index;
		}

		public long getID() {
			return id;
		}

		public long getClassID() {
			return clsID;
		}

		public long getMethodID() {
			return mthID;
		}

		public long getCodeIndex() {
			return index;
		}
	}

	public interface ClassPrepareListener {
		void onPrepared(long id);
	}

	public interface ClassListener {
		void onPrepared(String cls, long id);

		void onUnloaded(String cls);
	}

	public static class SmaliDebuggerException extends Exception {
		private final int errCode;
		private static final long serialVersionUID = -1111111202102191403L;

		public SmaliDebuggerException(Exception e) {
			super(e);
			errCode = -1;
		}

		public SmaliDebuggerException(String msg) {
			super(msg);
			this.errCode = -1;
		}

		public SmaliDebuggerException(String msg, int errCode) {
			super(msg);
			this.errCode = errCode;
		}

		public int getErrCode() {
			return errCode;
		}
	}

	/**
	 * Listener for breakpoint, watch, step, etc.
	 */
	public interface SuspendListener {
		/**
		 * For step, breakpoint, watchpoint, and any other events that suspend the JVM.
		 * This method will be called in stateListenQueue.
		 */
		void onSuspendEvent(SuspendInfo current);
	}

	public static class SuspendInfo {
		private boolean terminated;
		private boolean newRound;
		private final InfoSetter updater = new InfoSetter();

		public long getThreadID() {
			return updater.thread;
		}

		public long getClassID() {
			return updater.clazz;
		}

		public long getMethodID() {
			return updater.method;
		}

		public long getOffset() {
			return updater.offset;
		}

		private InfoSetter update() {
			updater.changed = false;
			updater.nextRound(newRound);
			this.newRound = false;
			return updater;
		}

		// called by decodingLoop, to tell the updater even though the values are the same,
		// they are decoded from another packet, they should be treated as new.
		private void nextRound() {
			newRound = true;
		}

		// according to JDWP document it's legal to fire two or more events on a same location,
		// e.g. one for single step and the other for breakpoint, so when this happened we only
		// want one of them.
		private boolean isAnythingChanged() {
			return updater.changed;
		}

		public boolean isTerminated() {
			return terminated;
		}

		private void setTerminated() {
			terminated = true;
		}

		private static class InfoSetter {
			private long thread;
			private long clazz;
			private long method;
			private long offset; // code offset;
			private boolean changed;

			private void nextRound(boolean newRound) {
				if (!changed) {
					changed = newRound;
				}
			}

			private InfoSetter updateThread(long thread) {
				if (!changed) {
					changed = this.thread != thread;
				}
				this.thread = thread;
				return this;
			}

			private InfoSetter updateClass(long clazz) {
				if (!changed) {
					changed = this.clazz != clazz;
				}
				this.clazz = clazz;
				return this;
			}

			private InfoSetter updateMethod(long method) {
				if (!changed) {
					changed = this.method != method;
				}
				this.method = method;
				return this;
			}

			private InfoSetter updateOffset(long offset) {
				if (!changed) {
					changed = this.offset != offset;
				}
				this.offset = offset;
				return this;
			}
		}
	}
}
