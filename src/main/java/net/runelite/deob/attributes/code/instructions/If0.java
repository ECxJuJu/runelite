package net.runelite.deob.attributes.code.instructions;

import net.runelite.deob.attributes.code.Instruction;
import net.runelite.deob.attributes.code.InstructionType;
import net.runelite.deob.attributes.code.Instructions;
import net.runelite.deob.attributes.code.instruction.types.ComparisonInstruction;
import net.runelite.deob.attributes.code.instruction.types.JumpingInstruction;
import net.runelite.deob.execution.Frame;
import net.runelite.deob.execution.InstructionContext;
import net.runelite.deob.execution.Stack;
import net.runelite.deob.execution.StackContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.runelite.deob.attributes.code.instruction.types.MappableInstruction;
import net.runelite.deob.deobfuscators.rename.ParallelExecutorMapping;

public abstract class If0 extends Instruction implements JumpingInstruction, ComparisonInstruction, MappableInstruction
{
	private Instruction to;
	private short offset;

	public If0(Instructions instructions, InstructionType type, int pc)
	{
		super(instructions, type, pc);
	}
	
	public If0(Instructions instructions, Instruction to)
	{
		super(instructions, InstructionType.IFEQ, -1);
		
		assert this != to;
		assert to.getInstructions() == this.getInstructions();
		
		this.to = to;
	}
	
	@Override
	public void load(DataInputStream is) throws IOException
	{
		offset = is.readShort();
		length += 2;
	}
	
	@Override
	public void resolve()
	{
		to = this.getInstructions().findInstruction(this.getPc() + offset);
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException
	{
		super.write(out);
		assert to.getInstructions() == this.getInstructions();
		assert to.getInstructions().getInstructions().contains(to);
		out.writeShort(to.getPc() - this.getPc());
	}

	@Override
	public void buildJumpGraph()
	{
		this.addJump(to);
	}

	@Override
	public void execute(Frame frame)
	{
		InstructionContext ins = new InstructionContext(this, frame);
		Stack stack = frame.getStack();
		
		StackContext one = stack.pop();
		
		ins.pop(one);
		
		Frame other = frame.dup();
		other.jump(ins, to);
		
		ins.branch(other);
		
		frame.addInstructionContext(ins);
	}
	
	@Override
	public void replace(Instruction oldi, Instruction newi)
	{
		if (to == oldi)
			to = newi;
	}
	
	@Override
	public List<Instruction> getJumps()
	{
		return Arrays.asList(to);
	}
	
	@Override
	public final/*XXX tmp*/ void map(ParallelExecutorMapping mapping, InstructionContext ctx, InstructionContext other)
	{
		InstructionContext one = ctx.getPops().get(0).getPushed().resolve(ctx.getPops().get(0)),
			two = other.getPops().get(0).getPushed().resolve(other.getPops().get(0));
		
		// we can map these if they are getfield instructions?
		
		assert ctx.getInstruction().getClass().equals(other.getInstruction().getClass());
		
		Frame branch1 = ctx.getBranches().get(0),
			branch2 = other.getBranches().get(0);
		
		assert branch1.other == null;
		assert branch2.other == null;
		
		branch1.other = branch2;
		branch2.other = branch1;
	}
	
	@Override
	public boolean isSame(InstructionContext thisIc, InstructionContext otherIc)
	{
		return thisIc.getInstruction().getClass() == otherIc.getInstruction().getClass();
	}
}
