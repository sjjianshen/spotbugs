package edu.umd.cs.findbugs.detect;

public enum CreatorDataValue {
    NEW,
    UK,
    NOT_JSON,
    JSON_SOURCE,
    JSON;

    public boolean isJson() {
        return this == CreatorDataValue.JSON;
    }
}
