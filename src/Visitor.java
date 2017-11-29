import java.util.HashMap;
import java.util.LinkedList;

public class Visitor implements CCALParserVisitor {

    private LinkedList<String> stack = new LinkedList<>();
    private HashMap<String,Symbol> symbolTable = new HashMap<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTprogramme node, Object data) {
        node.childrenAccept(this, data);
        return symbolTable;
    }

    @Override
    public Object visit(ASTFunctionHeader node, Object data) {
        node.childrenAccept(this,data);
        Integer count= Integer.parseInt(node.data.get("paramCount"));
        System.out.println("Param Count: " + count);
        for(int i = 0; i < count; i++){
            String type=stack.removeFirst();
            String var=stack.removeFirst();
            symbolTable.put(var,new Symbol(type,"null"));
        }
        return null;
    }

    @Override
    public Object visit(ASTStatementAssignment node, Object data) {
        node.childrenAccept(this, data);
        String value=stack.removeFirst();
        String variable=stack.removeFirst();
        System.out.println("Statement Assignment: " + variable +" = "+ value);
        Symbol variableSymbol = (Symbol)symbolTable.get(variable);
        Symbol valueSymbol = (Symbol)symbolTable.get(value);
        if(variableSymbol == null){
            System.out.println("NO VARIABLE TO BE REASSIGNED");
            return null;
        }
        if(valueSymbol == null){
            System.out.println("ASSUMING CONSTANT");
            variableSymbol.value = value;
            symbolTable.put(variable,variableSymbol);
            return null;
        }
        if(variableSymbol.type.equals(valueSymbol.type)){
            variableSymbol.value = valueSymbol.value;
            symbolTable.put(variable,variableSymbol);
        }
        else{
            System.out.println("TYPE REASSIGNMENT ERROR");
        }
        return null;
    }

    @Override
    public Object visit(ASTconstdeclaration node, Object data) {
        node.childrenAccept(this,data);
        String value=stack.removeFirst();
        String type=stack.removeFirst();
        String var=stack.removeFirst();
        System.out.println("Const declaration: " + var + " : " + type +" = "+value);
        Symbol symbol = new Symbol(type,value);
        symbolTable.put(var,symbol);
        return null;
    }

    @Override
    public Object visit(ASTvardeclaration node, Object data) {
        node.childrenAccept(this, data);
        String type=stack.removeFirst();
        String var=stack.removeFirst();
        System.out.println("Var declaration: " + var +" : "+ type);
        Symbol symbol = new Symbol(type,"null");
        symbolTable.put(var,symbol);
        return null;
    }

    @Override
    public Object visit(ASTVariableValue node, Object data) {
        node.childrenAccept(this,data);
        String value=node.data.get("value");
        System.out.println("Variable Value: " + value);
        stack.addFirst(value);
        return null;
    }

    @Override
    public Object visit(ASTVariable node, Object data) {
        node.childrenAccept(this, data);
        String var=node.data.get("name");
        System.out.println("Variable: " + var);
        stack.addFirst(var);
        return null;
    }

    @Override
    public Object visit(ASTTypeValue node, Object data) {
        node.childrenAccept(this, data);
        String type=node.data.get("type");
        System.out.println("Type: " + type);
        stack.addFirst(type);
        return null;
    }

}