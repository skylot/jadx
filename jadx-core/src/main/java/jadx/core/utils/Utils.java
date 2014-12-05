package jadx.core.utils;

import jadx.cli.JadxCLIArgs;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.AbstractRegion;
import jadx.core.dex.visitors.regions.AbstractRegionVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Utils {

private Utils() {
}

public static String cleanObjectName(String obj) {
	int last = obj.length() - 1;
	if (obj.charAt(0) == 'L' && obj.charAt(last) == ';') {
		return obj.substring(1, last).replace('/', '.');
	}
	return obj;
}

public static String makeQualifiedObjectName(String obj) {
	return 'L' + obj.replace('.', '/') + ';';
}

public static String escape(String str) {
	int len = str.length();
	StringBuilder sb = new StringBuilder(len);
	for (int i = 0; i < len; i++) {
		char c = str.charAt(i);
		switch (c) {
			case '.':
			case '/':
			case ';':
			case '$':
			case ' ':
			case ',':
			case '<':
				sb.append('_');
				break;

			case '[':
				sb.append('A');
				break;

			case ']':
			case '>':
			case '?':
			case '*':
				break;

			default:
				sb.append(c);
				break;
		}
	}
	return sb.toString();
}

public static String listToString(Iterable<?> list) {
	if (list == null) {
		return "";
	}
	StringBuilder str = new StringBuilder();
	for (Iterator<?> it = list.iterator(); it.hasNext(); ) {
		Object o = it.next();
		str.append(o);
		if (it.hasNext()) {
			str.append(", ");
		}
	}
	return str.toString();
}

public static String arrayToString(Object[] array) {
	if (array == null) {
		return "";
	}
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < array.length; i++) {
		if (i != 0) {
			sb.append(", ");
		}
		sb.append(array[i]);
	}
	return sb.toString();
}

public static String getStackTrace(Throwable throwable) {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw, true);
	throwable.printStackTrace(pw);
	return sw.getBuffer().toString();
}

public static int compare(int x, int y) {
	return (x < y) ? -1 : ((x == y) ? 0 : 1);
}

public static String numbToLetters(int nn) {  //RAF Used to generate ids for Regions for debug
	// Convert a number to one or more letters (ie modulo 26)
	if (nn < 26) return "ABCDEFGHIJKLMNOPQRSTUVWYXZ".substring(nn, nn+1);
	return numbToLetters(nn/26-1)+numbToLetters(nn%26);
}

//RAF Various debug functions
public static void prt(String str) {  //RAF
	System.out.println(str);
}
public static void prt(int indent, String str) {  //RAF
	System.out.println(indent(indent, str));
}

public static JadxCLIArgs jadxCLIArgs;  // RAF 
public static boolean quiet = true;  // Controls verbosity of some stuff, temporarily

public static boolean debugIt(String mthName) {
	// Decide whether more debugging info/operations should be done for this method
	return jadxCLIArgs==null  // When decompiling a .class file, args may be null?
			;//|| jadxCLIArgs.singleMethod!=null && mthName.indexOf(jadxCLIArgs.singleMethod)!=-1;
	//return mthName.charAt(0)=='m' || mthName.charAt(0)=='<';
}

public static final String spaces = "                                                                               ";
public static String indent(int depth, String str) {
	StringBuffer sb = new StringBuffer();
	int ii, jj=0;
	while (true) {
		if ((ii = str.indexOf('\n', jj)) < 0) ii = str.length(); 
		try {
			sb.append(spaces.substring(0, depth));
		} catch (Exception ex) {
			sb.append(spaces.substring(0, depth%spaces.length()));
		}
		sb.append(str.substring(jj, ii));
		sb.append('\n');
		if ((jj = ii+1) > str.length()) break;
	}
	if ((ii = sb.length())>0 && sb.charAt(ii-1)=='\n') sb.setLength(ii-1);
	return sb.toString();
} // end of indent

public static void showBlocks(MethodNode mth, String title) {  //RAF
	prt("\n"+mth.getName()+"-"+title+":");
	for (BlockNode blk : mth.getBasicBlocks()) {  //RAF
		String pre = "", suc = "";
		for (BlockNode b2 : blk.getPredecessors()) {
			pre += "b:"+b2.getId()+" ";
		}

		for (BlockNode b2 : blk.getSuccessors()) {
			suc += "b:"+b2.getId()+" ";
		}

		AbstractRegion pReg = null;  //tmp blk.getParentRegion();
		prt("  "+pad(pre, 12)+ "> b:"+blk.getId()+" >   "+pad(suc,12)
				+(pReg==null ? "" : "  pReg R"+pReg.letterId));
	}
} // end of showBlocks

public static int showTreeMode;
public static  void showTree(MethodNode mth, String title, int mode) {
	prt(genTree(mth, title, mode));
}

public static String genTree(MethodNode mth, String title, int mode) {
	StringBuffer sb = new StringBuffer();
	sb.append("\n--"+mth.getName()+"-"+title+":\n");


	class TreeARV extends AbstractRegionVisitor {
		ArrayList<IRegion> enclosingRegs = new ArrayList<IRegion>();
		StringBuffer sb;
		int depth = 0;
		int showTreeMode;
		public TreeARV(StringBuffer sb, int mode) {
			this.sb = sb;  this.showTreeMode = mode;
		}

		@Override
		public void enterRegion(MethodNode mth, IRegion reg) {
			String attrs = reg.getAttributesString();
			AbstractRegion pReg = null; //tmp ((AttrNode)reg).getParentRegion();
			sb.append(Utils.indent(depth*2, "{ reg"+((AbstractRegion)reg).letterId+" "+reg.baseString())+" "
					+reg.getClass().getSimpleName()+",  "+reg.getSubBlocks().size()+" blocks"
					+(attrs!=null&&attrs.length()>0 ? ", attrs: "+attrs : "")
					+" "+reg.getAttributesString()
					+(pReg==null ? "" : ", pReg R"+pReg.letterId)+"\n");

			enclosingRegs.add(reg);  // Remember nesting of regions
			if (depth > 0) {
				if (pReg != enclosingRegs.get(depth-1)) {
	//tmp				sb.append("     ^^^ region's parentRegion is not right\n");
				}
			}

			depth++; 
		}
		@Override
		public void processBlock(MethodNode mth, IBlock cont) {
			StringBuffer sb = new StringBuffer();
			if (cont instanceof BlockNode) {
				BlockNode blk = (BlockNode)cont;
				sb.append(" p:");
				for (BlockNode bn : blk.getPredecessors()) {
					sb.append(bn.baseString());  sb.append(",");
				}
				sb.append(" s"+blk.getCleanSuccessors().size()+"/"+blk.getSuccessors().size()+":");
				for (BlockNode bn : blk.getSuccessors()) {
					sb.append(bn.baseString());  sb.append(",");
				}

				String attrs = blk.getAttributesString();
				if (attrs!=null && attrs.length()>0) {
					sb.append(", attrs: ");  sb.append(attrs);
				}
			}

			AbstractRegion pReg = null; //tmp ((AttrNode)cont).getParentRegion();
			sb.append(Utils.indent(depth*2, "BLK ")+cont+sb+(pReg==null ? "" : ", pReg R"+pReg.letterId)+"\n");

			if (pReg != enclosingRegs.get(depth-1)) {
	//tmp			sb.append("     ^^^ block's parentRegion is not right, is the block duplicated?\n");
			}

			if (showTreeMode>=1) {
				prevBlock = (BlockNode)cont;
				showInsts(cont.getInstructions(), null, mth, depth*2+2);
				//List<InsnNode> ins = cont.getInstructions();
				//for(InsnNode inst : cont.getInstructions()) {
				//  etc = inst.getType().name().compareTo("IF")==0
				//    ? "then "+((IfNode)inst).getThenBlock()
				//      +" else "+((IfNode)inst).getElseBlock() : "";
				//  sb.append(Utils.indent(depth*2+2, "i"+Integer.toHexString(inst.getOffset())
				//    +", srcLine "+inst.getSourceLine()+", "+inst.getType()+" "+etc
				//    +"\n"+inst)+"\n");
				//}
			}
		}
		@Override
		public void leaveRegion(MethodNode mth, IRegion reg) {
			depth--;
			sb.append(Utils.indent(depth*2, "} reg"+((AbstractRegion)reg).letterId)+"\n");
			enclosingRegs.remove(depth);
		}
	} // end of TreeARV class
	
	TreeARV arv = new TreeARV(sb, mode);
	DepthRegionTraversal.traverseAll(mth, arv);
	
	return arv.sb.toString();
} // end of showTree



public static void showInsts(Object instsArg, String title, MethodNode mth) {
	showInsts(instsArg, title, mth, 0);
}
public static void showInsts(Object instsArg, String title, MethodNode mth,
		int indent) {
	InsnNode[] instructions;
	if (instsArg instanceof InsnNode[]) {
		instructions = (InsnNode[])instsArg;
	} else if (instsArg == null) {
		prt(indent, title+" --no instructions");
		return;
	} else {
		List<InsnNode> instsList = (List<InsnNode>)instsArg;
		instructions = new InsnNode[instsList.size()];
		for (int ii=0; ii<instructions.length; ii++) {
			instructions[ii] = instsList.get(ii);
		}
	}
	if (title != null) prt(indent, "\ninstructions, "+title+":");
	List<BlockNode> blocks = mth==null ? null : mth.getBasicBlocks();

	for (InsnNode inst : instructions) {
		showInst(inst, blocks, indent);
	} // end of for loop scanning all instructions
} // end of showInsts


public static BlockNode prevBlock = null;
public static boolean debugEachPass = false;  // Do debug stuff in ProcessClass for each pass
public static MethodNode dbgMth; 
public static void showInst(InsnNode inst, List<BlockNode> blocks, int indent) {
	// Return BlockNode of inst, so showInsts can separate blocks with blank lines
	if (inst == null) return;
	RegisterArg res = inst.getResult();
	StringBuffer sb = new StringBuffer();
	int ii=0;

	if (res != null) {
		decodeArg(res, sb, indent);
		//String resType = res.getType().toString();
		//if ((jj = resType.lastIndexOf('.')) > 0) resType = resType.substring(jj+1);
		//sb.append((res.getName()!=null ? res.getName() : "")
		//  +" "+res.getRegNum()+" "+resType);
	} else {
		//sb.append("(noRes)");
	}
	sb.append(" < ");
	for (InsnArg arg : inst.getArguments()) {
		sb.append("a"); sb.append(ii++);  sb.append(' ');
		decodeArg(arg, sb, indent);
		sb.append(", ");
	}
	if (sb.length()>0 && sb.charAt(sb.length()-2)==',') sb.setLength(sb.length()-2);

	String srcLine = pad(String.valueOf(inst.getSourceLine()), 4);
	BlockNode blk = blocks==null ? null : findBlockForOffset(blocks, inst.getOffset());
	String blkNo = blk==null ? "  " : pad(String.valueOf(blk.getId()), 2);
	InsnType type = inst.getType();
	sb.append(" ");

	decodeInst(inst, sb);

	// Separate blocks with a blank line
	if (blk != prevBlock) {
		if (prevBlock!=null && blk!=null) prt("");
		prevBlock = blk;
	}

	prt(indent, srcLine+" b"+blkNo+" "
			+pad(Integer.toHexString(inst.getOffset()).toUpperCase(), -4)
			+" "+type+" "+sb);  //RAF 
} // end of showInst


public static void decodeArg(InsnArg arg, StringBuffer sb, int indent) {
	int jj;
	if (arg instanceof RegisterArg) {
		String typeName = arg.getType().toString();
		if (typeName.charAt(0) == '?') typeName = "";
		if ((jj = typeName.lastIndexOf('.')) > 0) typeName = typeName.substring(jj+1);
		sb.append(typeName+" r"+((RegisterArg)arg).getRegNum());

	} else if (arg instanceof LiteralArg) {
		sb.append("lit "+((LiteralArg)arg).getLiteral());

	} else if (arg instanceof NamedArg) {
		sb.append("named "+((NamedArg)arg).getName());

	} else if (arg instanceof InsnWrapArg) {
		InsnWrapArg iwa = (InsnWrapArg)arg;
		showInst(iwa.getWrapInsn(), null, indent+2);
		sb.append("arg^");

		//} else if (arg instanceof Arg) {
		//  sb.append("arg??");

		//} else if (arg instanceof Arg) {
		//  sb.append("arg??");

	} else {
		sb.append("?arg?"+arg);
	}
} // end of decodeArg


public static void decodeInst(InsnNode inst, StringBuffer sb) {
	String typeName = inst.getType().name();

	if (typeName.compareTo("INVOKE") == 0) {
		String mthName = ((InvokeNode)inst).getCallMth().getName();
		sb.append(" call "+mthName);

	} else if (typeName.compareTo("GOTO") == 0) {
		GotoNode gn = (GotoNode)inst;
		sb.append("goto "+Integer.toHexString(gn.getTarget()).toUpperCase());               

	} else if (typeName.compareTo("IF") == 0) {
		IfNode ifInst = (IfNode)inst;
		sb.append(" "+ifInst.getOp()+" "
				+Integer.toHexString(ifInst.getTarget()).toUpperCase()
				+" then: "+ifInst.getThenBlock()
				+" else: "+ifInst.getElseBlock());

	} else if (typeName.compareTo("IGET") == 0) {
		IndexInsnNode iin = (IndexInsnNode)inst;
		sb.append("index "+((FieldInfo)iin.getIndex()).getName());

	} else if (typeName.compareTo("SGET") == 0) {
		IndexInsnNode iin = (IndexInsnNode)inst;
		sb.append("static "+((FieldInfo)iin.getIndex()).getName());

	} else if (typeName.compareTo("CONST_STR") == 0) {
		ConstStringNode csn = (ConstStringNode)inst;
		sb.append(" str: \""+csn.getString()+"\"");

	} else if (typeName.compareTo("") == 0) {
		prt("??");

	} else {
		//prt("??typeName "+typeName);
	}
} // end of decodeInst


public static BlockNode findBlockForOffset(List<BlockNode> blocks, int off) {
	if (blocks == null) return null;
	for (BlockNode blk : blocks) {
		for (InsnNode inst : blk.getInstructions()) {
			if (inst.getOffset() == off) return blk;
		}
	}
	return null;
} // end of findBlockForOffset


public static String pad(String str, int pad) {
	// Pad str on right to to be length of pad,  Or, if pad is negative, pad on left
	if (pad > 0) {
		return str.length()>=pad ? str : str+spaces.substring(0, pad-str.length());
	} else {
		return str.length()>=-pad ? str : spaces.substring(0, -pad-str.length())+str;
	}
} // end of pad

// Debug version of ArrayList class, allows hooking(breakpoint) of all 
// add and remove operations. (to find out who is adding or removing some 
// entry in the array
public static class RiboArrayList<T> extends ArrayList<T> {

// ADD methods:
@Override
public boolean add(T obj) {
	return super.add(obj);
}

@Override
public void add(int ind, T obj) {
	super.add(ind, obj);
}

@Override
public T set(int p0, T p1) {
	return null;
}

@Override
public boolean addAll(Collection<? extends T> coll) {
	return super.addAll(coll);
}

@Override
public boolean addAll(int ind, Collection<? extends T> coll) {
	return super.addAll(ind, coll);
}

// REMOVE methods
@Override
public boolean remove(Object obj) {
	String etc = obj instanceof BlockNode ? ((BlockNode)obj).getAttributesString()
			: "";
	prt("  REMOVE "+obj+" -- "+etc);
	return super.remove(obj);
}

@Override
public T remove(int ind) {
	return super.remove(ind);
}

@Override
public boolean removeAll(Collection<?> coll) {
	return removeAll(coll);
}

@Override
public boolean retainAll(Collection<?> coll) {
	return super.retainAll(coll);
}

@Override
public void clear() {
	super.clear();
}
} // end of RiboArrayList class

} // end of Utils class

