package edu.umd.cs.findbugs.detect;

public enum CreatorDataValue {
    NOT_JSON,
    JSON_SOURCE,
    JSON;

    public boolean isJson() {
        return this == CreatorDataValue.JSON;
    }

    public boolean isJsonSource() {return this == CreatorDataValue.JSON_SOURCE || isJson();}
}
