package edu.umd.cs.findbugs.detect;

public enum CreatorDataValue {
    NOT_JSON,
    JSON_OBJECT,
    JSON_OBJECT_RAW,
    JSON_OBJECT_ARRAY,
    JSON_CONDITION_OBJECT,
    JSON;

    public boolean isJson() {
        return this == CreatorDataValue.JSON;
    }

    public boolean isJsonSource() {
        return this == CreatorDataValue.JSON_OBJECT || this == CreatorDataValue.JSON;
    }
}
