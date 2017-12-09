import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

class Symbol {
    private Token name;
    private Token type;
    private SymbolType symbolType;
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

    void setSymbolType(SymbolType d0) {
        symbolType = d0;
    }

    SymbolType getSymbolType() {
        return symbolType;
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

    String getSymbolString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Name: ").append(getName()).append("\n");
        stringBuilder.append("\t ").append("SymbolType: ").append(getSymbolType()).append("\n");
        if(getSymbolType().equals(SymbolType.FUNC)){
            stringBuilder.append("\t ").append("Parameters: ").append(printValues()).append("\n");
            stringBuilder.append("\t ").append("Is called?: ").append(getIsCalled()).append("\n");
        }
        else {
            if (getValues().size() > 0) {
                stringBuilder.append("\t ").append(" Values: ").append(printValues()).append("\n");
            } else {
                stringBuilder.append("\t ").append(" Values: No assignments made").append("\n");
            }
            stringBuilder.append("\t ").append("Is written to: ").append(getValues().size() > 0).append("\n");
            stringBuilder.append("\t ").append("Is read from: ").append(getIsRead()).append("\n");
        }

        return stringBuilder.toString();

    }
}
