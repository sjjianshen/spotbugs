package edu.umd.cs.findbugs.detect;

public enum CreatorDataValue {
    NOT_JSON,
    JSON,
    JSON_OBJECT,
    JSON_OBJECT_RAW,
    JSON_OBJECT_ARRAY,

    JSON_EXP,
    JSON_OBJECT_EXP;
    
    public boolean isJson() {
        return this == CreatorDataValue.JSON || this == CreatorDataValue.JSON_EXP;
    }

    public boolean isJsonExp() {
        return this == CreatorDataValue.JSON_EXP;
    }

    public boolean isJsonSource() {
        return this == CreatorDataValue.JSON_OBJECT || this == CreatorDataValue.JSON || isJsonSourceExp();
    }

    public boolean isJsonSourceExp() {
        return this == CreatorDataValue.JSON_OBJECT_EXP  ||
                this == CreatorDataValue.JSON_OBJECT_RAW ||
                this == CreatorDataValue.JSON_OBJECT_ARRAY ||
                isJsonExp();
    }
}
