package jadx.core.dex.instructions;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.custom.IArrayPayload;
import jadx.api.plugins.input.insns.custom.ISwitchPayload;
import jadx.core.Consts;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;

public class InsnDecoder {
	private static final Logger LOG = LoggerFactory.getLogger(InsnDecoder.class);

	private final MethodNode method;
	private final RootNode root;

	public InsnDecoder(MethodNode mthNode) {
		this.method = mthNode;
		this.root = method.root();
	}

	public InsnNode[] process(ICodeReader codeReader) {
		InsnNode[] instructions = new InsnNode[codeReader.getInsnsCount()];
		codeReader.visitInstructions(rawInsn -> {
			int offset = rawInsn.getOffset();
			InsnNode insn;
			try {
				rawInsn.decode();
				insn = decode(rawInsn);
			} catch (Exception e) {
				method.addError("Failed to decode insn: " + rawInsn + ", method: " + method, e);
				insn = new InsnNode(InsnType.NOP, 0);
				insn.addAttr(AType.JADX_ERROR, new JadxError("decode failed: " + e.getMessage(), e));
			}
			insn.setOffset(offset);
			instructions[offset] = insn;
		});
		return instructions;
	}

	@NotNull
	protected InsnNode decode(InsnData insn) throws DecodeException {
		switch (insn.getOpcode()) {
			case NOP:
				return new InsnNode(InsnType.NOP, 0);

			// move-result will be process in invoke and filled-new-array instructions
			case MOVE_RESULT:
				return insn(InsnType.MOVE_RESULT, InsnArg.reg(insn, 0, ArgType.UNKNOWN));

			case CONST:
				LiteralArg narrowLitArg = InsnArg.lit(insn, ArgType.NARROW);
				return insn(InsnType.CONST, InsnArg.reg(insn, 0, narrowLitArg.getType()), narrowLitArg);

			case CONST_WIDE:
				LiteralArg wideLitArg = InsnArg.lit(insn, ArgType.WIDE);
				return insn(InsnType.CONST, InsnArg.reg(insn, 0, wideLitArg.getType()), wideLitArg);

			case CONST_STRING:
				InsnNode constStrInsn = new ConstStringNode(insn.getIndexAsString());
				constStrInsn.setResult(InsnArg.reg(insn, 0, ArgType.STRING));
				return constStrInsn;

			case CONST_CLASS: {
				ArgType clsType = ArgType.parse(insn.getIndexAsType());
				InsnNode constClsInsn = new ConstClassNode(clsType);
				constClsInsn.setResult(InsnArg.reg(insn, 0, ArgType.generic(Consts.CLASS_CLASS, clsType)));
				return constClsInsn;
			}

			case MOVE:
				return insn(InsnType.MOVE,
						InsnArg.reg(insn, 0, ArgType.NARROW),
						InsnArg.reg(insn, 1, ArgType.NARROW));

			case MOVE_WIDE:
				return insn(InsnType.MOVE,
						InsnArg.reg(insn, 0, ArgType.WIDE),
						InsnArg.reg(insn, 1, ArgType.WIDE));

			case MOVE_OBJECT:
				return insn(InsnType.MOVE,
						InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT),
						InsnArg.reg(insn, 1, ArgType.UNKNOWN_OBJECT));

			case ADD_INT:
				return arith(insn, ArithOp.ADD, ArgType.INT);

			case ADD_DOUBLE:
				return arith(insn, ArithOp.ADD, ArgType.DOUBLE);

			case ADD_FLOAT:
				return arith(insn, ArithOp.ADD, ArgType.FLOAT);

			case ADD_LONG:
				return arith(insn, ArithOp.ADD, ArgType.LONG);

			case ADD_INT_LIT:
				return arithLit(insn, ArithOp.ADD, ArgType.INT);

			case SUB_INT:
				return arith(insn, ArithOp.SUB, ArgType.INT);

			case RSUB_INT:
				return new ArithNode(ArithOp.SUB,
						InsnArg.reg(insn, 0, ArgType.INT),
						InsnArg.lit(insn, ArgType.INT),
						InsnArg.reg(insn, 1, ArgType.INT));

			case SUB_LONG:
				return arith(insn, ArithOp.SUB, ArgType.LONG);

			case SUB_FLOAT:
				return arith(insn, ArithOp.SUB, ArgType.FLOAT);

			case SUB_DOUBLE:
				return arith(insn, ArithOp.SUB, ArgType.DOUBLE);

			case MUL_INT:
				return arith(insn, ArithOp.MUL, ArgType.INT);

			case MUL_DOUBLE:
				return arith(insn, ArithOp.MUL, ArgType.DOUBLE);

			case MUL_FLOAT:
				return arith(insn, ArithOp.MUL, ArgType.FLOAT);

			case MUL_LONG:
				return arith(insn, ArithOp.MUL, ArgType.LONG);

			case MUL_INT_LIT:
				return arithLit(insn, ArithOp.MUL, ArgType.INT);

			case DIV_INT:
				return arith(insn, ArithOp.DIV, ArgType.INT);

			case REM_INT:
				return arith(insn, ArithOp.REM, ArgType.INT);

			case REM_LONG:
				return arith(insn, ArithOp.REM, ArgType.LONG);

			case REM_FLOAT:
				return arith(insn, ArithOp.REM, ArgType.FLOAT);

			case REM_DOUBLE:
				return arith(insn, ArithOp.REM, ArgType.DOUBLE);

			case DIV_DOUBLE:
				return arith(insn, ArithOp.DIV, ArgType.DOUBLE);

			case DIV_FLOAT:
				return arith(insn, ArithOp.DIV, ArgType.FLOAT);

			case DIV_LONG:
				return arith(insn, ArithOp.DIV, ArgType.LONG);

			case DIV_INT_LIT:
				return arithLit(insn, ArithOp.DIV, ArgType.INT);

			case REM_INT_LIT:
				return arithLit(insn, ArithOp.REM, ArgType.INT);

			case AND_INT:
				return arith(insn, ArithOp.AND, ArgType.INT);

			case AND_INT_LIT:
				return arithLit(insn, ArithOp.AND, ArgType.INT);

			case XOR_INT_LIT:
				return arithLit(insn, ArithOp.XOR, ArgType.INT);

			case AND_LONG:
				return arith(insn, ArithOp.AND, ArgType.LONG);

			case OR_INT:
				return arith(insn, ArithOp.OR, ArgType.INT);

			case OR_INT_LIT:
				return arithLit(insn, ArithOp.OR, ArgType.INT);

			case XOR_INT:
				return arith(insn, ArithOp.XOR, ArgType.INT);

			case OR_LONG:
				return arith(insn, ArithOp.OR, ArgType.LONG);

			case XOR_LONG:
				return arith(insn, ArithOp.XOR, ArgType.LONG);

			case USHR_INT:
				return arith(insn, ArithOp.USHR, ArgType.INT);

			case USHR_LONG:
				return arith(insn, ArithOp.USHR, ArgType.LONG);

			case SHL_INT:
				return arith(insn, ArithOp.SHL, ArgType.INT);

			case SHL_LONG:
				return arith(insn, ArithOp.SHL, ArgType.LONG);

			case SHR_INT:
				return arith(insn, ArithOp.SHR, ArgType.INT);

			case SHR_LONG:
				return arith(insn, ArithOp.SHR, ArgType.LONG);

			case SHL_INT_LIT:
				return arithLit(insn, ArithOp.SHL, ArgType.INT);
			case SHR_INT_LIT:
				return arithLit(insn, ArithOp.SHR, ArgType.INT);
			case USHR_INT_LIT:
				return arithLit(insn, ArithOp.USHR, ArgType.INT);

			case NEG_INT:
				return neg(insn, ArgType.INT);
			case NEG_LONG:
				return neg(insn, ArgType.LONG);
			case NEG_FLOAT:
				return neg(insn, ArgType.FLOAT);
			case NEG_DOUBLE:
				return neg(insn, ArgType.DOUBLE);

			case NOT_INT:
				return not(insn, ArgType.INT);
			case NOT_LONG:
				return not(insn, ArgType.LONG);

			case INT_TO_BYTE:
				return cast(insn, ArgType.INT, ArgType.BYTE);
			case INT_TO_CHAR:
				return cast(insn, ArgType.INT, ArgType.CHAR);
			case INT_TO_SHORT:
				return cast(insn, ArgType.INT, ArgType.SHORT);
			case INT_TO_FLOAT:
				return cast(insn, ArgType.INT, ArgType.FLOAT);
			case INT_TO_DOUBLE:
				return cast(insn, ArgType.INT, ArgType.DOUBLE);
			case INT_TO_LONG:
				return cast(insn, ArgType.INT, ArgType.LONG);

			case FLOAT_TO_INT:
				return cast(insn, ArgType.FLOAT, ArgType.INT);
			case FLOAT_TO_DOUBLE:
				return cast(insn, ArgType.FLOAT, ArgType.DOUBLE);
			case FLOAT_TO_LONG:
				return cast(insn, ArgType.FLOAT, ArgType.LONG);

			case DOUBLE_TO_INT:
				return cast(insn, ArgType.DOUBLE, ArgType.INT);
			case DOUBLE_TO_FLOAT:
				return cast(insn, ArgType.DOUBLE, ArgType.FLOAT);
			case DOUBLE_TO_LONG:
				return cast(insn, ArgType.DOUBLE, ArgType.LONG);

			case LONG_TO_INT:
				return cast(insn, ArgType.LONG, ArgType.INT);
			case LONG_TO_FLOAT:
				return cast(insn, ArgType.LONG, ArgType.FLOAT);
			case LONG_TO_DOUBLE:
				return cast(insn, ArgType.LONG, ArgType.DOUBLE);

			case IF_EQ:
			case IF_EQZ:
				return new IfNode(insn, IfOp.EQ);

			case IF_NE:
			case IF_NEZ:
				return new IfNode(insn, IfOp.NE);

			case IF_GT:
			case IF_GTZ:
				return new IfNode(insn, IfOp.GT);

			case IF_GE:
			case IF_GEZ:
				return new IfNode(insn, IfOp.GE);

			case IF_LT:
			case IF_LTZ:
				return new IfNode(insn, IfOp.LT);

			case IF_LE:
			case IF_LEZ:
				return new IfNode(insn, IfOp.LE);

			case CMP_LONG:
				return cmp(insn, InsnType.CMP_L, ArgType.LONG);
			case CMPL_FLOAT:
				return cmp(insn, InsnType.CMP_L, ArgType.FLOAT);
			case CMPL_DOUBLE:
				return cmp(insn, InsnType.CMP_L, ArgType.DOUBLE);

			case CMPG_FLOAT:
				return cmp(insn, InsnType.CMP_G, ArgType.FLOAT);
			case CMPG_DOUBLE:
				return cmp(insn, InsnType.CMP_G, ArgType.DOUBLE);

			case GOTO:
				return new GotoNode(insn.getTarget());

			case THROW:
				return insn(InsnType.THROW, null, InsnArg.reg(insn, 0, ArgType.THROWABLE));

			case MOVE_EXCEPTION:
				return insn(InsnType.MOVE_EXCEPTION, InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT_NO_ARRAY));

			case RETURN_VOID:
				return new InsnNode(InsnType.RETURN, 0);

			case RETURN:
				return insn(InsnType.RETURN,
						null,
						InsnArg.reg(insn, 0, method.getReturnType()));

			case INSTANCE_OF:
				InsnNode instInsn = new IndexInsnNode(InsnType.INSTANCE_OF, ArgType.parse(insn.getIndexAsType()), 1);
				instInsn.setResult(InsnArg.reg(insn, 0, ArgType.BOOLEAN));
				instInsn.addArg(InsnArg.reg(insn, 1, ArgType.UNKNOWN_OBJECT));
				return instInsn;

			case CHECK_CAST:
				ArgType castType = ArgType.parse(insn.getIndexAsType());
				InsnNode checkCastInsn = new IndexInsnNode(InsnType.CHECK_CAST, castType, 1);
				checkCastInsn.setResult(InsnArg.reg(insn, 0, castType));
				checkCastInsn.addArg(InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT));
				return checkCastInsn;

			case IGET:
				FieldInfo igetFld = FieldInfo.fromData(root, insn.getIndexAsField());
				InsnNode igetInsn = new IndexInsnNode(InsnType.IGET, igetFld, 1);
				igetInsn.setResult(InsnArg.reg(insn, 0, tryResolveFieldType(igetFld)));
				igetInsn.addArg(InsnArg.reg(insn, 1, igetFld.getDeclClass().getType()));
				return igetInsn;

			case IPUT:
				FieldInfo iputFld = FieldInfo.fromData(root, insn.getIndexAsField());
				InsnNode iputInsn = new IndexInsnNode(InsnType.IPUT, iputFld, 2);
				iputInsn.addArg(InsnArg.reg(insn, 0, tryResolveFieldType(iputFld)));
				iputInsn.addArg(InsnArg.reg(insn, 1, iputFld.getDeclClass().getType()));
				return iputInsn;

			case SGET:
				FieldInfo sgetFld = FieldInfo.fromData(root, insn.getIndexAsField());
				InsnNode sgetInsn = new IndexInsnNode(InsnType.SGET, sgetFld, 0);
				sgetInsn.setResult(InsnArg.reg(insn, 0, tryResolveFieldType(sgetFld)));
				return sgetInsn;

			case SPUT:
				FieldInfo sputFld = FieldInfo.fromData(root, insn.getIndexAsField());
				InsnNode sputInsn = new IndexInsnNode(InsnType.SPUT, sputFld, 1);
				sputInsn.addArg(InsnArg.reg(insn, 0, tryResolveFieldType(sputFld)));
				return sputInsn;

			case ARRAY_LENGTH:
				InsnNode arrLenInsn = new InsnNode(InsnType.ARRAY_LENGTH, 1);
				arrLenInsn.setResult(InsnArg.reg(insn, 0, ArgType.INT));
				arrLenInsn.addArg(InsnArg.reg(insn, 1, ArgType.array(ArgType.UNKNOWN)));
				return arrLenInsn;

			case AGET:
				return arrayGet(insn, ArgType.INT_FLOAT);
			case AGET_BOOLEAN:
				return arrayGet(insn, ArgType.BOOLEAN);
			case AGET_BYTE:
				return arrayGet(insn, ArgType.BYTE);
			case AGET_CHAR:
				return arrayGet(insn, ArgType.CHAR);
			case AGET_SHORT:
				return arrayGet(insn, ArgType.SHORT);
			case AGET_WIDE:
				return arrayGet(insn, ArgType.WIDE);
			case AGET_OBJECT:
				return arrayGet(insn, ArgType.UNKNOWN_OBJECT);

			case APUT:
				return arrayPut(insn, ArgType.INT_FLOAT);
			case APUT_BOOLEAN:
				return arrayPut(insn, ArgType.BOOLEAN);
			case APUT_BYTE:
				return arrayPut(insn, ArgType.BYTE);
			case APUT_CHAR:
				return arrayPut(insn, ArgType.CHAR);
			case APUT_SHORT:
				return arrayPut(insn, ArgType.SHORT);
			case APUT_WIDE:
				return arrayPut(insn, ArgType.WIDE);
			case APUT_OBJECT:
				return arrayPut(insn, ArgType.UNKNOWN_OBJECT);

			case INVOKE_STATIC:
				return invoke(insn, InvokeType.STATIC, false);

			case INVOKE_STATIC_RANGE:
				return invoke(insn, InvokeType.STATIC, true);

			case INVOKE_DIRECT:
				return invoke(insn, InvokeType.DIRECT, false);
			case INVOKE_INTERFACE:
				return invoke(insn, InvokeType.INTERFACE, false);
			case INVOKE_SUPER:
				return invoke(insn, InvokeType.SUPER, false);
			case INVOKE_VIRTUAL:
				return invoke(insn, InvokeType.VIRTUAL, false);
			case INVOKE_CUSTOM:
				return invoke(insn, InvokeType.CUSTOM, false);

			case INVOKE_DIRECT_RANGE:
				return invoke(insn, InvokeType.DIRECT, true);
			case INVOKE_INTERFACE_RANGE:
				return invoke(insn, InvokeType.INTERFACE, true);
			case INVOKE_SUPER_RANGE:
				return invoke(insn, InvokeType.SUPER, true);
			case INVOKE_VIRTUAL_RANGE:
				return invoke(insn, InvokeType.VIRTUAL, true);
			case INVOKE_CUSTOM_RANGE:
				return invoke(insn, InvokeType.CUSTOM, true);

			case NEW_INSTANCE:
				ArgType clsType = ArgType.parse(insn.getIndexAsType());
				IndexInsnNode newInstInsn = new IndexInsnNode(InsnType.NEW_INSTANCE, clsType, 0);
				newInstInsn.setResult(InsnArg.reg(insn, 0, clsType));
				return newInstInsn;

			case NEW_ARRAY:
				ArgType arrType = ArgType.parse(insn.getIndexAsType());
				return new NewArrayNode(arrType,
						InsnArg.reg(insn, 0, arrType),
						InsnArg.typeImmutableReg(insn, 1, ArgType.INT));

			case FILL_ARRAY_DATA:
				return new FillArrayInsn(InsnArg.reg(insn, 0, ArgType.UNKNOWN_ARRAY), insn.getTarget());
			case FILL_ARRAY_DATA_PAYLOAD:
				return new FillArrayData(((IArrayPayload) insn.getPayload()));

			case FILLED_NEW_ARRAY:
				return filledNewArray(insn, false);
			case FILLED_NEW_ARRAY_RANGE:
				return filledNewArray(insn, true);

			case PACKED_SWITCH:
				return new SwitchInsn(InsnArg.reg(insn, 0, ArgType.UNKNOWN), insn.getTarget(), true);
			case SPARSE_SWITCH:
				return new SwitchInsn(InsnArg.reg(insn, 0, ArgType.UNKNOWN), insn.getTarget(), false);

			case PACKED_SWITCH_PAYLOAD:
			case SPARSE_SWITCH_PAYLOAD:
				return new SwitchData(((ISwitchPayload) insn.getPayload()));

			case MONITOR_ENTER:
				return insn(InsnType.MONITOR_ENTER,
						null,
						InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT));

			case MONITOR_EXIT:
				return insn(InsnType.MONITOR_EXIT,
						null,
						InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT));

			default:
				throw new DecodeException("Unknown instruction: '" + insn + '\'');
		}
	}

	private ArgType tryResolveFieldType(FieldInfo igetFld) {
		FieldNode fieldNode = root.resolveField(igetFld);
		if (fieldNode != null) {
			return fieldNode.getType();
		}
		return igetFld.getType();
	}

	private InsnNode filledNewArray(InsnData insn, boolean isRange) {
		ArgType arrType = ArgType.parse(insn.getIndexAsType());
		ArgType elType = arrType.getArrayElement();
		boolean typeImmutable = elType.isPrimitive();
		int regsCount = insn.getRegsCount();
		InsnArg[] regs = new InsnArg[regsCount];
		if (isRange) {
			int r = insn.getReg(0);
			for (int i = 0; i < regsCount; i++) {
				regs[i] = InsnArg.reg(r, elType, typeImmutable);
				r++;
			}
		} else {
			for (int i = 0; i < regsCount; i++) {
				int regNum = insn.getReg(i);
				regs[i] = InsnArg.reg(regNum, elType, typeImmutable);
			}
		}
		InsnNode node = new FilledNewArrayNode(elType, regs.length);
		// node.setResult(resReg == -1 ? null : InsnArg.reg(resReg, arrType));
		for (InsnArg arg : regs) {
			node.addArg(arg);
		}
		return node;
	}

	private InsnNode cmp(InsnData insn, InsnType itype, ArgType argType) {
		InsnNode inode = new InsnNode(itype, 2);
		inode.setResult(InsnArg.reg(insn, 0, ArgType.INT));
		inode.addArg(InsnArg.reg(insn, 1, argType));
		inode.addArg(InsnArg.reg(insn, 2, argType));
		return inode;
	}

	private InsnNode cast(InsnData insn, ArgType from, ArgType to) {
		InsnNode inode = new IndexInsnNode(InsnType.CAST, to, 1);
		inode.setResult(InsnArg.reg(insn, 0, to));
		inode.addArg(InsnArg.reg(insn, 1, from));
		return inode;
	}

	private InsnNode invoke(InsnData insn, InvokeType type, boolean isRange) {
		if (type == InvokeType.CUSTOM) {
			return InvokeCustomBuilder.build(method, insn, isRange);
		}
		MethodInfo mthInfo = MethodInfo.fromRef(root, insn.getIndexAsMethod());
		return new InvokeNode(mthInfo, insn, type, isRange);
	}

	private InsnNode arrayGet(InsnData insn, ArgType argType) {
		InsnNode inode = new InsnNode(InsnType.AGET, 2);
		inode.setResult(InsnArg.typeImmutableIfKnownReg(insn, 0, argType));
		inode.addArg(InsnArg.typeImmutableIfKnownReg(insn, 1, ArgType.array(argType)));
		inode.addArg(InsnArg.reg(insn, 2, ArgType.NARROW_INTEGRAL));
		return inode;
	}

	private InsnNode arrayPut(InsnData insn, ArgType argType) {
		InsnNode inode = new InsnNode(InsnType.APUT, 3);
		inode.addArg(InsnArg.typeImmutableIfKnownReg(insn, 1, ArgType.array(argType)));
		inode.addArg(InsnArg.reg(insn, 2, ArgType.NARROW_INTEGRAL));
		inode.addArg(InsnArg.typeImmutableIfKnownReg(insn, 0, argType));
		return inode;
	}

	private InsnNode arith(InsnData insn, ArithOp op, ArgType type) {
		return ArithNode.build(insn, op, type);
	}

	private InsnNode arithLit(InsnData insn, ArithOp op, ArgType type) {
		return ArithNode.buildLit(insn, op, type);
	}

	private InsnNode neg(InsnData insn, ArgType type) {
		InsnNode inode = new InsnNode(InsnType.NEG, 1);
		inode.setResult(InsnArg.reg(insn, 0, type));
		inode.addArg(InsnArg.reg(insn, 1, type));
		return inode;
	}

	private InsnNode not(InsnData insn, ArgType type) {
		InsnNode inode = new InsnNode(InsnType.NOT, 1);
		inode.setResult(InsnArg.reg(insn, 0, type));
		inode.addArg(InsnArg.reg(insn, 1, type));
		return inode;
	}

	private InsnNode insn(InsnType type, RegisterArg res) {
		InsnNode node = new InsnNode(type, 0);
		node.setResult(res);
		return node;
	}

	private InsnNode insn(InsnType type, RegisterArg res, InsnArg arg) {
		InsnNode node = new InsnNode(type, 1);
		node.setResult(res);
		node.addArg(arg);
		return node;
	}
}
