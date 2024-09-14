package jadx.plugins.input.java.data.attributes.stack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;
import jadx.plugins.input.java.data.attributes.types.StackMapTableAttr;
import jadx.plugins.input.java.utils.JavaClassParseException;

public class StackMapTableReader implements IJavaAttributeReader {

	@Override
	public IJavaAttribute read(JavaClassData clsData, DataReader reader) {
		int count = reader.readU2();
		Map<Integer, StackFrame> map = new HashMap<>(count, 1);
		StackFrame prevFrame = null;
		for (int i = 0; i < count; i++) {
			StackFrame frame = readFrame(reader, prevFrame);
			map.put(frame.getOffset(), frame);
			prevFrame = frame;
		}
		return new StackMapTableAttr(map);
	}

	private static final Map<StackFrameType, Consumer<FrameContext>> FRAME_READERS = registerReaders();

	private static Map<StackFrameType, Consumer<FrameContext>> registerReaders() {
		EnumMap<StackFrameType, Consumer<FrameContext>> map = new EnumMap<>(StackFrameType.class);
		map.put(StackFrameType.SAME_FRAME, context -> readSame(context, false));
		map.put(StackFrameType.SAME_FRAME_EXTENDED, context -> readSame(context, true));
		map.put(StackFrameType.SAME_LOCALS_1_STACK, context -> readSL1S(context, false));
		map.put(StackFrameType.SAME_LOCALS_1_STACK_EXTENDED, context -> readSL1S(context, true));
		map.put(StackFrameType.CHOP, StackMapTableReader::readChop);
		map.put(StackFrameType.APPEND, StackMapTableReader::readAppend);
		map.put(StackFrameType.FULL, StackMapTableReader::readFull);
		return map;
	}

	private StackFrame readFrame(DataReader reader, StackFrame prevFrame) {
		int typeData = reader.readU1();
		StackFrameType frameType = StackFrameType.getType(typeData);
		Consumer<FrameContext> frameReader = FRAME_READERS.get(frameType);
		if (frameReader == null) {
			throw new JavaClassParseException("Found unsupported stack frame type: " + frameType);
		}
		FrameContext frameContext = new FrameContext(reader, typeData, prevFrame);
		frameReader.accept(frameContext);
		return Objects.requireNonNull(frameContext.getFrame());
	}

	private static void readSame(FrameContext context, boolean extended) {
		int offsetDelta;
		StackFrameType type;
		if (extended) {
			type = StackFrameType.SAME_FRAME_EXTENDED;
			offsetDelta = context.getDataReader().readU2();
		} else {
			type = StackFrameType.SAME_FRAME;
			offsetDelta = context.getTypeData();
		}
		StackFrame frame = new StackFrame(calcOffset(context, offsetDelta), type);
		frame.setStackSize(0);
		frame.setLocalsCount(getPrevLocalsCount(context));
		context.setFrame(frame);
	}

	private static void readSL1S(FrameContext context, boolean extended) {
		DataReader reader = context.getDataReader();
		int offsetDelta;
		StackFrameType type;
		if (extended) {
			type = StackFrameType.SAME_LOCALS_1_STACK_EXTENDED;
			offsetDelta = reader.readU2();
		} else {
			type = StackFrameType.SAME_LOCALS_1_STACK;
			offsetDelta = context.getTypeData() - 64;
		}
		StackValueType[] stackTypes = TypeInfoReader.readTypeInfoList(reader, 1);
		StackFrame frame = new StackFrame(calcOffset(context, offsetDelta), type);
		frame.setStackSize(1);
		frame.setStackValueTypes(stackTypes);
		frame.setLocalsCount(getPrevLocalsCount(context));
		context.setFrame(frame);
	}

	private static void readChop(FrameContext context) {
		int k = 251 - context.getTypeData();
		int offsetDelta = context.getDataReader().readU2();
		StackFrame frame = new StackFrame(calcOffset(context, offsetDelta), StackFrameType.CHOP);
		frame.setStackSize(0);
		frame.setLocalsCount(getPrevLocalsCount(context) - k);
		context.setFrame(frame);
	}

	private static void readAppend(FrameContext context) {
		DataReader reader = context.getDataReader();
		int k = context.getTypeData() - 251;
		int offsetDelta = reader.readU2();
		TypeInfoReader.skipTypeInfoList(reader, k);
		StackFrame frame = new StackFrame(calcOffset(context, offsetDelta), StackFrameType.APPEND);
		frame.setStackSize(0);
		frame.setLocalsCount(getPrevLocalsCount(context) - k);
		context.setFrame(frame);
	}

	private static void readFull(FrameContext context) {
		DataReader reader = context.getDataReader();
		int offsetDelta = reader.readU2();
		int localsCount = reader.readU2();
		TypeInfoReader.skipTypeInfoList(reader, localsCount);
		int stackSize = reader.readU2();
		StackValueType[] stackTypes = TypeInfoReader.readTypeInfoList(reader, stackSize);

		StackFrame frame = new StackFrame(calcOffset(context, offsetDelta), StackFrameType.FULL);
		frame.setLocalsCount(localsCount);
		frame.setStackSize(stackSize);
		frame.setStackValueTypes(stackTypes);
		context.setFrame(frame);
	}

	private static int calcOffset(FrameContext context, int offsetDelta) {
		StackFrame prevFrame = context.getPrevFrame();
		if (prevFrame == null) {
			return offsetDelta;
		}
		return prevFrame.getOffset() + offsetDelta + 1;
	}

	private static int getPrevLocalsCount(FrameContext context) {
		StackFrame prevFrame = context.getPrevFrame();
		if (prevFrame == null) {
			return 0;
		}
		return prevFrame.getLocalsCount();
	}

	private static final class FrameContext {
		private final DataReader dataReader;
		private final int typeData;
		private final StackFrame prevFrame;

		private StackFrame frame;

		private FrameContext(DataReader dataReader, int typeData, StackFrame prevFrame) {
			this.dataReader = dataReader;
			this.typeData = typeData;
			this.prevFrame = prevFrame;
		}

		public DataReader getDataReader() {
			return dataReader;
		}

		public int getTypeData() {
			return typeData;
		}

		public StackFrame getPrevFrame() {
			return prevFrame;
		}

		public StackFrame getFrame() {
			return frame;
		}

		public void setFrame(StackFrame frame) {
			this.frame = frame;
		}
	}
}
