package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.AnalysisFactory;
import org.apache.bcel.generic.MethodGen;

public class CreatorDataflowFactory extends AnalysisFactory<CreatorDataflow> {
    /**
     * Constructor.
     */
    public CreatorDataflowFactory() {
        super("creator dataflow", CreatorDataflow.class);
    }

    @Override
    public CreatorDataflow analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        MethodGen methodGen = getMethodGen(analysisCache, descriptor);
        if (methodGen == null) {
            throw new MethodUnprofitableException(descriptor);
        }
        CFG cfg = getCFG(analysisCache, descriptor);
        DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);
        CreatorDataAnalysis creatorDataAnalysis = new CreatorDataAnalysis(dfs, methodGen);
        CreatorDataflow creatorDataflow = new CreatorDataflow(cfg, creatorDataAnalysis);
        creatorDataflow.execute();
        return creatorDataflow;
    }
}
