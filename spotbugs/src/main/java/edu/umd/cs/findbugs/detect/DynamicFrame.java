package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

public class DynamicFrame {
    private Set<String> notNullExpressions = new HashSet<>();
    private boolean isNew;

    public DynamicFrame() {
        isNew = true;
    }

    public static DynamicFrame emptyFrame() {
        return new DynamicFrame();
    }

    public void putDynamic(String methodName) {
        notNullExpressions.add(methodName);
    }

    public void mergeFrom(DynamicFrame sourceFact) {
        if (isNew) {
            isNew = false;
            notNullExpressions = sourceFact.copyExpressions();
        } else {
            doMergeFrame(sourceFact);
        }
    }

    private void doMergeFrame(DynamicFrame sourceFact) {
        Set<String> newExpressions = new HashSet<>();
//        Set<String> newExpressions = sourceFact.copyExpressions();
        for (String exp : notNullExpressions) {
            if (sourceFact.contains(exp)) {
                newExpressions.add(exp);
            }
        }
        this.notNullExpressions = newExpressions;
    }

    public boolean contains(String exp) {
        return notNullExpressions.contains(exp);
    }

    private Set<String> copyExpressions() {
        Set<String> newExpressions = new HashSet<>();
        for (String exp : notNullExpressions) {
            newExpressions.add(exp);
        }
        return newExpressions;
    }

    @Override
    public String toString() {
        return "DynamicFrame{" +
                "notNullExpressions=" + notNullExpressions +
                ", isNew=" + isNew +
                '}';
    }
}
