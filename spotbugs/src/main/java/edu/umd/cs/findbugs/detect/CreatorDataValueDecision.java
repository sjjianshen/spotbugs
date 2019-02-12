package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

import javax.annotation.CheckForNull;

public class CreatorDataValueDecision {
    private final @CheckForNull
    ValueNumber value;

    private final @CheckForNull
    CreatorDataValue ifcmpDecision;

    private final @CheckForNull
    CreatorDataValue fallThroughDecision;

    public CreatorDataValueDecision(ValueNumber value, CreatorDataValue ifcmpDecision, CreatorDataValue fallThroughDecision) {
        this.value = value;

        // At least one of the edges must be feasible
        assert !(ifcmpDecision == null && fallThroughDecision == null);
        this.ifcmpDecision = ifcmpDecision;
        this.fallThroughDecision = fallThroughDecision;

    }

    public ValueNumber getValue() {
        return value;
    }

    public CreatorDataValue getDecision(int edgeType) {
        switch (edgeType) {
            case EdgeTypes.IFCMP_EDGE:
                return ifcmpDecision;
            case EdgeTypes.FALL_THROUGH_EDGE:
                return fallThroughDecision;
            default:
                throw new IllegalArgumentException("Bad edge type: " + edgeType);
        }
    }
}
