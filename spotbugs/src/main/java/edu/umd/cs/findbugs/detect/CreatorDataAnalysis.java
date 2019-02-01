package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

public class CreatorDataAnalysis extends FrameDataflowAnalysis<CreatorDataValue, CreatorDataFrame> {
    private MethodGen methodGen;
    private CreatorDataFrame cachedEntryFact;
    private CreatorDataFrameModelingVistor visitor;

    public CreatorDataAnalysis(DepthFirstSearch dfs, MethodGen methodGen) {
        super(dfs);
        this.methodGen = methodGen;
        this.visitor = new CreatorDataFrameModelingVistor(methodGen.getConstantPool());
    }

    @Override
    protected void mergeValues(CreatorDataFrame otherFrame, CreatorDataFrame resultFrame, int slot) throws DataflowAnalysisException {
        if (otherFrame.getValue(slot) == CreatorDataValue.JSON || resultFrame.getValue(slot) == CreatorDataValue.JSON) {
            resultFrame.setValue(slot, CreatorDataValue.JSON);
        } else {
            resultFrame.setValue(slot, CreatorDataValue.NOT_JSON);
        }
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, CreatorDataFrame fact) throws DataflowAnalysisException {
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
                cachedEntryFact.setValue(i, CreatorDataValue.UK);
            }
        }
        copy(cachedEntryFact, result);
    }

    @Override
    public void meetInto(CreatorDataFrame fact, Edge edge, CreatorDataFrame result) throws DataflowAnalysisException {
        super.mergeInto(fact, result);
    }
}
