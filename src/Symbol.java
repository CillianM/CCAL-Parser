import java.util.LinkedHashMap;

public class Symbol extends Object{
    private Token name;
    private Token type;
    private DataType dataType;
    private String scope;
    private LinkedHashMap<String, Object> values;
    private int numArgs = -1; // used when dataType is DataType.Function
    private boolean isRead = false; // should always be false when initialised
    private boolean isCalled = false; // should always be false when initialised

    public Symbol(Token n0, Token t0, String s0, DataType d0) {
        name = n0;
        type = t0;
        scope = s0;
        dataType = d0;
        values = new LinkedHashMap<String, Object>();
        // numArgs will be left as -1
    }

    public Symbol(Token n0, Token t0, String s0, DataType d0, int na0) {
        name = n0;
        type = t0;
        scope = s0;
        dataType = d0;
        values = new LinkedHashMap<String, Object>();
        numArgs = na0;
    }

    public Symbol(Token n0, Token t0, DataType d0) {
        name = n0;
        type = t0;
        // scope will be left undefined
        dataType = d0;
        values = new LinkedHashMap<String, Object>();
        // numArgs will be left as -1
    }

    public Symbol(Token n0, Token t0, DataType d0, int na0) {
        name = n0;
        type = t0;
        // scope will be left undefined
        dataType = d0;
        values = new LinkedHashMap<String, Object>();
        numArgs = na0;
    }

    // Getter and Setters
    public void setName(Token n0) {
        name = n0;
    }

    public Token getName() {
        return name;
    }

    public void setType(Token t0) {
        type = t0;
    }

    public Token getType() {
        return type;
    }

    public void setDataType(DataType d0) {
        dataType = d0;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setScope(String s0) {
        scope = s0;
    }

    public String getScope() {
        return scope;
    }

    public void setValues(LinkedHashMap<String, Object> v0) {
        values = v0;
    }

    public LinkedHashMap<String, Object> getValues() {
        return values;
    }

    public void setNumArgs(int na0) {
        numArgs = na0;
    }

    public int getNumArgs() {
        return numArgs;
    }

    public void setIsRead(boolean i0) {
        isRead = i0;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setIsCalled(boolean i0) {
        isCalled = i0;
    }

    public boolean getIsCalled() {
        return isCalled;
    }

    // get the value of the token by its name
    public Object getValue(String name) {
        return values.get(name);
    }

    // add a value of a token by its name
    public void addValue(String name, Object value) {
        values.put(name, value);
    }

    public String toString() {
        return "[" + name + ", " + type + ", " + dataType + ", " + scope
                + ", " + values + ", " + numArgs + ", " + isRead + "]";
    }


}
