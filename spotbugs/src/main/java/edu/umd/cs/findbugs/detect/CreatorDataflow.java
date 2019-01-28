package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.AbstractDataflow;
import edu.umd.cs.findbugs.ba.CFG;

public class CreatorDataflow extends AbstractDataflow<CreatorDataFrame, CreatorDataAnalysis> {

    public CreatorDataflow(CFG cfg, CreatorDataAnalysis creatorDataAnalysis) {
        super(cfg, creatorDataAnalysis);
    }
}
