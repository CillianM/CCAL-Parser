import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

class Symbol {
    private Token name;
    private Token type;
    private DataType dataType;
    private String scope;
    private LinkedHashMap<String, LinkedList<Variable>> values;
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

    void setValues(LinkedHashMap<String, LinkedList<Variable>> v0) {
        values = v0;
    }

    LinkedHashMap<String, LinkedList<Variable>> getValues() {
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
    LinkedList<Variable> getValue(String name) {
        return values.get(name);
    }

    // add a value of a token by its name
    void addValue(String name, Variable value) {
        LinkedList<Variable> variables =  values.get(name);
        if(variables == null){
            variables = new LinkedList<>();
        }
        variables.add(value);
        values.put(name, variables);
    }

    String getFirstValue(String name){
        return values.get(name).getFirst().getValue();
    }

    String printValues(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Map.Entry<String, LinkedList<Variable>> entry : values.entrySet()) {
            String key = entry.getKey();
            for (Variable variable : entry.getValue()) {
                stringBuilder.append(variable.getValue()).append(":").append(variable.getType()).append(",");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "[" + name + ", " + type + ", " + dataType + ", " + scope
                + ", " + values + ", " + numArgs + ", " + isRead + "]";
    }



}
