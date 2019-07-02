package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.npe.*;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import org.apache.bcel.Const;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import javax.annotation.CheckForNull;
import java.util.BitSet;

public class CreatorDataAnalysis extends FrameDataflowAnalysis<CreatorDataValue, CreatorDataFrame> implements EdgeTypes {
    private MethodGen methodGen;
    private CreatorDataFrame cachedEntryFact;
    private CreatorDataFrameModelingVistor visitor;
    private CreatorDataFrame instanceOfFrame;
    private CreatorDataFrame lastFrame;
    private ValueNumberDataflow vnaDataflow;
    private IsNullValueDataflow invDataflow;

    private static final BitSet nullComparisonInstructionSet = new BitSet();

    static {
        nullComparisonInstructionSet.set(Const.IFNULL);
        nullComparisonInstructionSet.set(Const.IFNONNULL);
        nullComparisonInstructionSet.set(Const.IF_ACMPEQ);
        nullComparisonInstructionSet.set(Const.IF_ACMPNE);
    }

    public CreatorDataAnalysis(DepthFirstSearch dfs, MethodGen methodGen, ValueNumberDataflow valueNumberDataflow, IsNullValueDataflow invDataflow) {
        super(dfs);
        this.methodGen = methodGen;
        this.vnaDataflow = valueNumberDataflow;
        this.invDataflow = invDataflow;
        this.visitor = new CreatorDataFrameModelingVistor(methodGen.getConstantPool());
    }

    @Override
    protected void mergeValues(CreatorDataFrame otherFrame, CreatorDataFrame resultFrame, int slot) throws DataflowAnalysisException {
        if (otherFrame.getValue(slot).isJson() || resultFrame.getValue(slot).isJson()) {
            if (otherFrame.getValue(slot).isJsonExp() && resultFrame.getValue(slot).isJsonExp()) {
                resultFrame.setValue(slot, CreatorDataValue.JSON_EXP);
            } else {
                resultFrame.setValue(slot, CreatorDataValue.JSON);
            }
        } else if (otherFrame.getValue(slot).isJsonSource() || resultFrame.getValue(slot).isJsonSource()) {
            if (otherFrame.getValue(slot) == CreatorDataValue.JSON_OBJECT_RAW ||
                    resultFrame.getValue(slot) == CreatorDataValue.JSON_OBJECT_RAW) {
                resultFrame.setValue(slot, CreatorDataValue.JSON_OBJECT_RAW);
            } else if (otherFrame.getValue(slot) == CreatorDataValue.JSON_OBJECT_ARRAY ||
                    resultFrame.getValue(slot) == CreatorDataValue.JSON_OBJECT_ARRAY) {
                resultFrame.setValue(slot, CreatorDataValue.JSON_OBJECT_RAW);
            } else if (otherFrame.getValue(slot) == CreatorDataValue.JSON_OBJECT_EXP &&
                    resultFrame.getValue(slot) == CreatorDataValue.JSON_OBJECT_EXP) {
                resultFrame.setValue(slot, CreatorDataValue.JSON_OBJECT_EXP);
            } else {
                resultFrame.setValue(slot, CreatorDataValue.JSON_OBJECT);
            }
        } else {
            resultFrame.setValue(slot, CreatorDataValue.NOT_JSON);
        }
    }

    @Override
    public void transfer(BasicBlock basicBlock, @CheckForNull InstructionHandle end, CreatorDataFrame start, CreatorDataFrame result) throws DataflowAnalysisException {
        instanceOfFrame = null;
        super.transfer(basicBlock, end, start, result);
        if (end == null) {
            if (result == null) {
                result.setDecision(null);
            } else {
                CreatorDataValueDecision decision = getDecision(basicBlock, lastFrame);
                result.setDecision(decision);
            }
        }
        instanceOfFrame = null;
    }

    private CreatorDataValueDecision getDecision(BasicBlock basicBlock, CreatorDataFrame lastFrame) throws DataflowAnalysisException {
        assert lastFrame != null;

        final InstructionHandle lastInSourceHandle = basicBlock.getLastInstruction();
        if (lastInSourceHandle == null) {
            return null; // doesn't end in null comparison
        }

        final short lastInSourceOpcode = lastInSourceHandle.getInstruction().getOpcode();
        if (lastInSourceOpcode == Const.IFEQ || lastInSourceOpcode == Const.IFNE) {
            // check for instanceof check
            InstructionHandle prev = lastInSourceHandle.getPrev();
            if (prev == null) {
                return null;
            }
            short secondToLastOpcode = prev.getInstruction().getOpcode();
            // System.out.println("Second last opcode: " +
            // Const.OPCODE_NAMES[secondToLastOpcode]);
            if (secondToLastOpcode != Const.INSTANCEOF) {
                return null;
            }
            if (instanceOfFrame == null) {
                return null;
            }
            CreatorDataValue tos = instanceOfFrame.getTopValue();
            Location atInstanceOf = new Location(prev, basicBlock);
            ValueNumberFrame instanceOfVnaFrame = vnaDataflow.getFactAtLocation(atInstanceOf);
            // Initially, assume neither branch is feasible.
            CreatorDataValue ifcmpDecision = null;
            CreatorDataValue fallThroughDecision = null;
            if (lastInSourceOpcode == Const.IFEQ) {
                fallThroughDecision = CreatorDataValue.NOT_JSON;
            } else {
                ifcmpDecision = CreatorDataValue.NOT_JSON;
            }

            return new CreatorDataValueDecision(instanceOfVnaFrame.getTopValue(), ifcmpDecision, fallThroughDecision);
        }

        if (!nullComparisonInstructionSet.get(lastInSourceOpcode)) {
            return null; // doesn't end in null comparison
        }

        Location atIf = new Location(lastInSourceHandle, basicBlock);
        ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);
        IsNullValueFrame previnvFrame = invDataflow.getFactAtLocation(atIf);

        switch (lastInSourceOpcode) {

            case Const.IFNULL:
            case Const.IFNONNULL:
                boolean ifnull = (lastInSourceOpcode == Const.IFNULL);
                ValueNumber prevTopValue = prevVnaFrame.getTopValue();
                return handleIfNull(prevTopValue, ifnull);
            case Const.IF_ACMPEQ:
                return null;
            case Const.IF_ACMPNE: {
                IsNullValue invTos = previnvFrame.getStackValue(0);
                IsNullValue invNextToTos = previnvFrame.getStackValue(1);
//                CreatorDataValue cdvTos = lastFrame.getStackValue(0);
//                CreatorDataValue cdvNextToTos = lastFrame.getStackValue(1);

                boolean tosNull = invTos.isDefinitelyNull();
                boolean nextToTosNull = invNextToTos.isDefinitelyNull();

                // Initially, assume neither branch is feasible.
                CreatorDataValue ifcmpDecision = null;
                CreatorDataValue fallThroughDecision = null;
                ValueNumber value;

                if (tosNull && ! nextToTosNull) {
                    return handleIfNull(prevVnaFrame.getStackValue(0), false);
                }
                if (! tosNull && nextToTosNull) {
                    return handleIfNull(prevVnaFrame.getStackValue(1), false);
                }

                if(invTos.isDefinitelyNotNull()) {
                    value = prevVnaFrame.getStackValue(1);
                    fallThroughDecision = CreatorDataValue.NOT_JSON;
                    return new CreatorDataValueDecision(value, ifcmpDecision, fallThroughDecision);
                }
                if (invNextToTos.isDefinitelyNotNull()) {
                    value = prevVnaFrame.getStackValue(0);
                    fallThroughDecision = CreatorDataValue.NOT_JSON;
                    return new CreatorDataValueDecision(value, ifcmpDecision, fallThroughDecision);
                }
                return null;
            }
            default:
                throw new IllegalStateException();
        }

    }

    private CreatorDataValueDecision handleIfNull(ValueNumber prevTopValue, boolean ifnull) {
        CreatorDataValue ifcmpDecision = null;
        CreatorDataValue fallThroughDecision = null;

        if (ifnull) {
            fallThroughDecision = CreatorDataValue.NOT_JSON;
        } else {
//            ifcmpDecision = CreatorDataValue.NOT_JSON; if this code is if null, it will be reported by isnullvalue analysis, so we omit it;
        }

        return new CreatorDataValueDecision(prevTopValue, ifcmpDecision, fallThroughDecision);

    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, CreatorDataFrame fact) throws DataflowAnalysisException {
        lastFrame = fact;
        if (handle.getInstruction().getOpcode() == Const.INSTANCEOF) {
            instanceOfFrame = createFact();
            instanceOfFrame.copyFrom(fact);
        }
        Location location = new Location(handle, basicBlock);
        visitor.setFrameAndLocation(fact, location);
        visitor.analyzeInstruction(handle.getInstruction());
    }

    @Override
    public CreatorDataFrame createFact() {
        return new CreatorDataFrame(methodGen.getMaxLocals());
    }

    @Override
    public void initEntryFact(CreatorDataFrame result) throws DataflowAnalysisException {
        if (cachedEntryFact == null) {
            cachedEntryFact = createFact();
            cachedEntryFact.setValid();
            int numLocals = methodGen.getMaxLocals();
            for (int i = 0; i < numLocals; ++i) {
                cachedEntryFact.setValue(i, CreatorDataValue.NOT_JSON);
            }
        }
        copy(cachedEntryFact, result);
    }

    @Override
    public void meetInto(CreatorDataFrame fact, Edge edge, CreatorDataFrame result) throws DataflowAnalysisException {
        final BasicBlock destBlock = edge.getTarget();
        CreatorDataFrame newfact = modifyFrame(fact, null);
        if (destBlock.isExceptionHandler()) {
            newfact.clearStack();
            newfact.pushValue(CreatorDataValue.NOT_JSON);
        } else {
            final int edgeType = edge.getType();
            final BasicBlock sourceBlock = edge.getSource();
            final ValueNumberFrame targetVnaFrame = vnaDataflow.getStartFact(destBlock);
            final ValueNumberFrame sourceVnaFrame = vnaDataflow.getResultFact(sourceBlock);

            assert targetVnaFrame != null;
            // Determine if the edge conveys any information about the
            // null/non-null status of operands in the incoming frame.

            if (edgeType == IFCMP_EDGE || edgeType == FALL_THROUGH_EDGE) {
                CreatorDataFrame resultFact = getResultFact(sourceBlock);
                CreatorDataValueDecision decision = resultFact.getDecision();
                if (decision != null) {
                    ValueNumber valueTested = decision.getValue();
                    if (valueTested != null) {
                        final Location atIf = new Location(sourceBlock.getLastInstruction(), sourceBlock);
                        final ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);

                        CreatorDataValue decisionValue = decision.getDecision(edgeType);
                        if (decisionValue != null) {
                            newfact = replaceValues(fact, valueTested, prevVnaFrame, targetVnaFrame,
                                    decisionValue);
                        }
                    }
                }
            }
        }

        super.mergeInto(newfact, result);
    }

    private CreatorDataFrame replaceValues(CreatorDataFrame origFrame, ValueNumber replaceMe,
                   ValueNumberFrame prevVnaFrame, ValueNumberFrame targetVnaFrame, CreatorDataValue replacementValue) {

        if (!targetVnaFrame.isValid()) {
            throw new IllegalArgumentException("Invalid frame in " + methodGen.getClassName() + "." + methodGen.getName() + " : "
                    + methodGen.getSignature());
        }
        // If required, make a copy of the frame
        CreatorDataFrame frame = modifyFrame(origFrame, null);

        if (frame.getNumSlots() != targetVnaFrame.getNumSlots()) {
            return frame;
        }

        final int targetNumSlots = targetVnaFrame.getNumSlots();
        final int prefixNumSlots = Math.min(frame.getNumSlots(), prevVnaFrame.getNumSlots());

        if (targetNumSlots < prefixNumSlots) {
            return frame;
        }

        if (!frame.isValid()) {
            return frame;
        }

        for (int i = 0; i < prefixNumSlots; ++i) {
            if (prevVnaFrame.getValue(i).equals(replaceMe)) {
                ValueNumber corresponding = targetVnaFrame.getValue(i);
                for (int j = 0; j < targetNumSlots; ++j) {
                    if (targetVnaFrame.getValue(j).equals(corresponding)) {
                        frame.setValue(j, replacementValue);
                    }
                }
            }
        }
        return frame;
    }
}
