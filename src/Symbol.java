import java.util.LinkedHashMap;

class Symbol {
    private Token name;
    private Token type;
    private DataType dataType;
    private String scope;
    private LinkedHashMap<String, Object> values;
    private int numArgs = -1;
    private boolean isRead = false;
    private boolean isCalled = false;

    Symbol() {
        values = new LinkedHashMap<>();
    }

    // Getter and Setters
    void setName(Token n0) {
        name = n0;
    }

    Token getName() {
        return name;
    }

    void setType(Token t0) {
        type = t0;
    }

    Token getType() {
        return type;
    }

    void setDataType(DataType d0) {
        dataType = d0;
    }

    DataType getDataType() {
        return dataType;
    }

    void setScope(String s0) {
        scope = s0;
    }

    String getScope() {
        return scope;
    }

    void setValues(LinkedHashMap<String, Object> v0) {
        values = v0;
    }

    LinkedHashMap<String, Object> getValues() {
        return values;
    }

    void setNumArgs(int na0) {
        numArgs = na0;
    }

    int getNumArgs() {
        return numArgs;
    }

    void setIsRead(boolean i0) {
        isRead = i0;
    }

    boolean getIsRead() {
        return isRead;
    }

    void setIsCalled(boolean i0) {
        isCalled = i0;
    }

    boolean getIsCalled() {
        return isCalled;
    }

    // get the value of the token by its name
    Object getValue(String name) {
        return values.get(name);
    }

    // add a value of a token by its name
    void addValue(String name, Object value) {
        values.put(name, value);
    }

    @Override
    public String toString() {
        return "[" + name + ", " + type + ", " + dataType + ", " + scope
                + ", " + values + ", " + numArgs + ", " + isRead + "]";
    }



}
