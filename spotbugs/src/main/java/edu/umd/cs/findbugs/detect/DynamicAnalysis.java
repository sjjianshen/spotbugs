package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.DepthFirstSearch;

public class DynamicAnalysis {
    private DepthFirstSearch dfs;

    public DynamicAnalysis(DepthFirstSearch dfs) {
        this.dfs = dfs;
    }
}
