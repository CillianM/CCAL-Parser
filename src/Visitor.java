import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Visitor implements CCALParserVisitor {

    private HashMap<String, HashMap<String, Symbol>> symbolTable = new HashMap<>();
    //Keep track of the scope we're in along with what the above scope was --> for nesting
    private static final String PROGRAMME = "Programme";
    private static final String MAIN = "Main";
    private String currentScope = PROGRAMME;
    private String previousScope;
    //Is every variable both written to and read from?
    //Is every function called?
    private int functionsNotCalled = 0;
    //Easy way to keep track of all error messages to be printed out at the end
    private List<ErrorMessage> errorList = new ArrayList<>();
    private List<String> variablesNotRead = new ArrayList<>();
    private List<String> variablesNotWritten = new ArrayList<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        symbolTable.put(currentScope, new HashMap<>());
        node.childrenAccept(this, data);

        //Print out the symbol table and perform the semantic checks once the programme is evaluated
        System.out.println("-----Start Symbol Table-----");
        Set scopes = symbolTable.keySet();
        for (Object scope : scopes) { //Loop through each scope of the programme
            String scopeName = (String) scope;
            System.out.println("-----Start " + scopeName + " Scope-----");
            Set symbols = symbolTable.get(scopeName).keySet();
            if (symbols.size() == 0) {
                System.out.println(" Nothing declared");
            }
            for (Object symbol : symbols) {
                String symbolName = (String) symbol;
                Symbol currentSymbol = symbolTable.get(scopeName).get(symbolName);
                System.out.println(symbolName);
                System.out.println(" DataType: " + currentSymbol.getDataType());
                System.out.println(" Type: " + currentSymbol.getType());
                if (currentSymbol.getDataType() == DataType.Function) { //so we know what variables of the symbol to access
                    System.out.println(" Parameters: " + currentSymbol.printValues());
                    System.out.println(" Is called?: " + currentSymbol.getIsCalled());
                    if (!currentSymbol.getIsCalled()) {
                        functionsNotCalled++; //update counter if it has not been called
                    }
                } else {
                    if (currentSymbol.getValues().size() > 0) {
                        System.out.println(" Values: " + currentSymbol.printValues()); //print out all assignments of this variable
                    } else {
                        System.out.println(" Values: No assignments made"); //Variable has not been written to, update counter
                        variablesNotWritten.add(currentSymbol.getName().image);
                    }
                    System.out.println(" Is written to?: " + (currentSymbol.getValues().size() > 0));
                    System.out.println(" Is read from?: " + currentSymbol.getIsRead());
                    if (!currentSymbol.getIsRead()) {
                        variablesNotRead.add(currentSymbol.getName().image);
                    }
                }
            }
            System.out.println("-----End " + scopeName + " Scope-----"); //Show end of scope
        }
        System.out.println("-----End Symbol Table-----");

        if (errorList.size() == 0) { //make sure there are no errors
            isSematicError();
            System.out.println("No errors found");
            Representer representer = new Representer();
            node.jjtAccept(representer, symbolTable);
        } else { //print out errors from the object list
            System.out.println(errorList.size() + " error(s).");
            printErrorList();
        }

        return null;
    }

    @Override
    public Object visit(ASTMain node, Object data) {
        // update to currentScope of function
        previousScope = currentScope;
        currentScope = MAIN;
        symbolTable.put(currentScope, new HashMap<>()); //create the hashmap for the main scope
        node.childrenAccept(this, data);

        // update currentScope to previous currentScope
        currentScope = previousScope;
        previousScope = null;
        return null;
    }

    @Override
    public Object visit(ASTFunctionList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTFunction node, Object data) {
        HashMap<String, Symbol> currentHashScopeMap = symbolTable.get(currentScope);
        if (currentHashScopeMap == null) {
            currentHashScopeMap = new HashMap<>();
        }

        Token functionType = (Token) node.jjtGetChild(0).jjtAccept(this, null); //get function type (eg. integer, boolean)
        Token functionName = (Token) node.jjtGetChild(1).jjtAccept(this, null); //get function name
        Symbol symbol = new Symbol(); //create symbol for this function
        symbol.setName(functionName);
        symbol.setType(functionType);
        symbol.setScope(currentScope);
        symbol.setDataType(DataType.Function);
        symbol.setNumArgs(node.jjtGetChild(2).jjtGetNumChildren()); //get the number of arguments through it's children

        //Check if the function already exists
        if (currentHashScopeMap.containsKey(functionName.image)) {
            Symbol originalFunc = currentHashScopeMap.get(functionName.image);
            //Even if it does exist does it have the same amount of parameters?
            if (originalFunc.getNumArgs() == symbol.getNumArgs()) {
                //Throw an error if it does exist already with the same amount of parameters
                errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" already declared with " + symbol.getNumArgs() + " parameters"));
            }
        } else {
            //otherwise update the symbol table with this new function
            currentHashScopeMap.put(functionName.image, symbol);
            symbolTable.put(currentScope, currentHashScopeMap);
        }

        //Update current scope to function so we can evaluate it's children
        previousScope = currentScope;
        currentScope = functionName.image;

        //Evaluate to functions params and body
        node.jjtGetChild(2).jjtAccept(this, null);
        node.jjtGetChild(3).jjtAccept(this, null);

        //Revert back to current scope scope
        currentScope = previousScope;
        previousScope = null;

        return null;
    }

    @Override
    public Object visit(ASTFunctionBody node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTReturnStatement node, Object data) {
        if (node.jjtGetNumChildren() > 0) {
            Token returnedToken = (Token) node.jjtGetChild(0).jjtAccept(this, null);
            Node returnedNode = node.jjtGetChild(0);
            if (returnedNode instanceof ASTVariable) {
                HashMap<String, Symbol> currentSymbolTable = symbolTable.get(currentScope);
                Symbol returnedSymbol = currentSymbolTable.get(returnedToken.image);
                if (returnedSymbol == null) {
                    currentSymbolTable = symbolTable.get(PROGRAMME);
                    returnedSymbol = currentSymbolTable.get(returnedToken.image);
                }
                if (returnedSymbol.getValues().size() == 0) { //check if there are any values assigned to value
                    if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) { //If we're in anything but the function (as the value in the function came from a param)
                        errorList.add(new ErrorMessage(returnedToken.beginLine, returnedToken.beginColumn, "Variable \"" + returnedToken.image + "\" has no value in currentScope \"" + currentScope + "\""));
                    }
                }
                returnedSymbol.setIsRead(true);
                updateSymbol(returnedToken.image, returnedSymbol);
            }
        }
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTParamList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTParam node, Object data) {
        HashMap<String, Symbol> currentSymbolTable = symbolTable.get(currentScope);
        if (currentSymbolTable == null) { //If this is the first param then the scope for the function won't be set yet
            currentSymbolTable = new HashMap<>();
        }

        Token paramName = (Token) node.jjtGetChild(0).jjtAccept(this, null); //name of the parameter
        Token paramType = (Token) node.jjtGetChild(1).jjtAccept(this, null); //type of parameter (eg. integer)
        Symbol symbol = new Symbol(); //create symbol for the param
        symbol.setName(paramName);
        symbol.setType(paramType);
        symbol.setScope(currentScope);
        symbol.setDataType(DataType.ParamVariable);
        currentSymbolTable.put(paramName.image, symbol);
        symbolTable.put(currentScope, currentSymbolTable);

        Token functionName = (Token) node.jjtGetParent().jjtGetParent().jjtGetChild(1).jjtAccept(this, null);
        currentSymbolTable = symbolTable.get(currentScope);
        Symbol functionSymbol = currentSymbolTable.get(functionName.image);
        if (functionSymbol == null) {
            currentSymbolTable = symbolTable.get(PROGRAMME);
            functionSymbol = currentSymbolTable.get(functionName.image);
        }
        if (functionSymbol.getValues().containsKey(paramName.image)) { //check if the parameter is already in use for the function
            errorList.add(new ErrorMessage(paramName.beginLine, paramName.beginColumn, "Duplicate parameter names, \"" + paramName.image + "\" for function \"" + functionName.image + "\""));
        } else {
            Variable variable = new Variable(functionSymbol.getType().toString(), paramName.image);
            functionSymbol.addValue(paramName.image, variable);
            updateSymbol(functionName.image, functionSymbol);
        }
        return null;
    }

    @Override
    public Object visit(ASTStatementBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIf node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTWhile node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCondition node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTSkip node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTNot node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTDeclarationList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTConstDeclaration node, Object data) {
        HashMap<String, Symbol> currentScopeSymbolTable = symbolTable.get(currentScope);
        if (currentScopeSymbolTable == null) {
            currentScopeSymbolTable = new HashMap<>();
        }
        Token contantName = (Token) node.jjtGetChild(0).jjtAccept(this, null); //get the constant name
        if (currentScopeSymbolTable.get(contantName.image) == null) { //Check that the constant hasn't been defined in the current scope
            Token constantType = (Token) node.jjtGetChild(1).jjtAccept(this, null); //get the constant type (eg. integer)
            Token contantValue = (Token) node.jjtGetChild(2).jjtAccept(this, null); //get the constant value
            Symbol symbol = new Symbol(); //create a symbol for the constant
            symbol.setName(contantName);
            symbol.setType(constantType);
            symbol.setScope(currentScope);
            symbol.setDataType(DataType.Constant);
            symbolTable.put(currentScope, currentScopeSymbolTable);
            currentScopeSymbolTable.put(contantName.image, symbol);

            if ((constantType.image.equals("integer") && !isInt(contantValue.image)) || (constantType.image.equals("boolean") && !isBoolean(contantValue.image))) {
                errorList.add(new ErrorMessage(contantName.beginLine, contantName.beginColumn, "Invalid type assigned to constant \"" + contantName.image + "\""));
            } else {
                Variable variable = new Variable(symbol.getType().toString(), contantValue.image.toLowerCase());
                symbol.addValue(contantName.image, variable); //create new constant value
            }
            currentScopeSymbolTable.put(contantName.image, symbol);
        } else {
            errorList.add(new ErrorMessage(contantName.beginLine, contantName.beginColumn, "Constant \"" + contantName.image + "\" already declared in currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, currentScopeSymbolTable); //update the symbol table with the updates made to the current scope
        return null;
    }

    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        Token varName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        //get the current scope map and check for existence of the var
        HashMap<String, Symbol> currentScopeSymbolTable = symbolTable.get(currentScope);
        if (currentScopeSymbolTable == null) {
            currentScopeSymbolTable = new HashMap<>();
        }
        Symbol varSymbol = currentScopeSymbolTable.get(varName.image);
        if (varSymbol == null) {
            Token varType = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbol symbol = new Symbol();
            symbol.setName(varName);
            symbol.setType(varType);
            symbol.setScope(currentScope);
            symbol.setDataType(DataType.Variable);
            currentScopeSymbolTable.put(varName.image, symbol);
        } else {
            errorList.add(new ErrorMessage(varName.beginLine, varName.beginColumn, "Variable \"" + varName.image + "\" already declared in currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, currentScopeSymbolTable);

        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        Token functionName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        // check if function has been declared
        HashMap<String, Symbol> functionSymbolTable = symbolTable.get(currentScope);
        if (functionSymbolTable == null) {
            functionSymbolTable = symbolTable.get(PROGRAMME);
        }
        Symbol functionSymbol = functionSymbolTable.get(functionName.image);
        if (functionSymbol == null) {
            functionSymbolTable = symbolTable.get(PROGRAMME);
            functionSymbol = functionSymbolTable.get(functionName.image);
        }
        if (functionSymbol != null) {
            // go to ArgList
            if (node.jjtGetNumChildren() > 1) {
                node.jjtGetChild(1).jjtAccept(this, null);
            }
            functionSymbol.setIsCalled(true);
            updateSymbol(functionName.image, functionSymbol);
        } else {
            errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Programme\""));
        }
        return null;
    }

    @Override
    public Object visit(ASTMinus node, Object data) {
        Token variableName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbol> currentSymbolTable = symbolTable.get(currentScope);
        Symbol variableSymbol = currentSymbolTable.get(variableName.image);
        if (variableSymbol == null) {
            currentSymbolTable = symbolTable.get(PROGRAMME);
            variableSymbol = currentSymbolTable.get(variableName.image);
        }
        if (variableSymbol == null) {
            // If we're still not finding it then there is a problem
            errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Variable \"" + variableName.image + "\" not declared in any scope"));
        } else if (variableSymbol.getValues().size() == 0) {
            if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) {
                // error if var has no value
                errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Variable \"" + variableName.image + "\" has no value in currentScope \"" + currentScope + "\""));
            }
        } else if (!variableSymbol.getType().image.equals("integer")) {
            errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot create negative value from " + variableSymbol.getDataType() + " \"" + variableName.image + "\". Not of type integer"));
        } else {
            Variable variable = new Variable(variableSymbol.getType().toString(), "-" + variableName); //Assign the minus sign the the value here
            variableSymbol.addValue(variableName.image, variable);
            // set isRead value
            variableSymbol.setIsRead(true);
            variableSymbol.setIsCalled(true);
            updateSymbol(variableName.image, variableSymbol);
        }
        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        Token functionName = (Token) node.jjtGetParent().jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbol> mapTemp = symbolTable.get(PROGRAMME);
        if (mapTemp != null) {
            Symbol functionSymbol = mapTemp.get(functionName.image);
            if (functionSymbol == null) {
                // error, no such function
                errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "No such function called \"" + functionName.image + "\""));
            } else {
                // check for correct number of Args
                int numArgsDeclared = functionSymbol.getNumArgs();
                int numArgsPassed = node.jjtGetNumChildren();
                if (numArgsDeclared != numArgsPassed) {
                    errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" has invalid number of arguments. called with " + numArgsPassed + " argument(s), should have " + numArgsDeclared));
                } else if (numArgsDeclared > 0) {
                    int i = 0;
                    //Loop through the functions arguments and evaluate them
                    for (String key : functionSymbol.getValues().keySet()) {
                        String type = functionSymbol.getValues().get(key).get(0).getType();
                        Token argumentName = (Token) node.jjtGetChild(i).jjtGetChild(0).jjtAccept(this, null);
                        Symbol argumentSymbol = mapTemp.get(argumentName.image);
                        if (argumentSymbol == null) {
                            mapTemp = symbolTable.get(currentScope);
                            argumentSymbol = mapTemp.get(argumentName.image);
                        }
                        if (!argumentSymbol.getType().image.equals(type)) {
                            errorList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "\"" + argumentName.image + "\" in function call for \"" + functionName.image + "\" is the wrong type"));
                        }
                        //if argument is variable then it is accessed
                        if (argumentSymbol.getDataType() != DataType.Unknown) {
                            argumentSymbol.setIsRead(true);
                            updateSymbol(argumentName.image, argumentSymbol);
                        }
                        i++;
                    }
                }
            }
        }
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTArg node, Object data) {
        Token argumentName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbol> currentSymbolTable = symbolTable.get(currentScope);
        Symbol argumentSymbol = currentSymbolTable.get(argumentName.image);
        if (argumentSymbol == null) {
            currentSymbolTable = symbolTable.get(PROGRAMME);
            argumentSymbol = currentSymbolTable.get(argumentName.image);
        }
        if (argumentSymbol == null) {
            errorList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "Variable or Const \"" + argumentName.image + "\" has not been declared in currentScope \"" + currentScope + "\""));
        } else if (argumentSymbol.getValues().size() == 0) {
            if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) {
                errorList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "Variable \"" + argumentName.image + "\" has been declared in currentScope \"" + currentScope + "\", but has no value"));
            }
        } else {
            argumentSymbol.setIsRead(true);
            updateSymbol(argumentName.image, argumentSymbol);
        }
        return null;
    }

    @Override
    public Object visit(ASTVariable node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTDigit node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTTypeValue node, Object data) {
        return node.jjtGetValue();
    }


    @Override
    public Object visit(ASTEqual node, Object data) {
        isValidBooleanOperation(node, data);
        return null;
    }

    @Override
    public Object visit(ASTNotEqual node, Object data) {
        isValidBooleanOperation(node, data);
        return null;
    }

    @Override
    public Object visit(ASTLessThan node, Object data) {
        isValidBooleanOperation(node, data);
        return null;
    }

    @Override
    public Object visit(ASTLessThanEqual node, Object data) {
        isValidBooleanOperation(node, data);
        return null;
    }

    @Override
    public Object visit(ASTGreaterThan node, Object data) {
        isValidBooleanOperation(node, data);
        return null;
    }

    @Override
    public Object visit(ASTGreaterThanEqual node, Object data) {
        isValidBooleanOperation(node, data);
        return null;
    }

    @Override
    public Object visit(ASTOr node, Object data) {
        isValidBooleanComparison(node, data);
        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        isValidBooleanComparison(node, data);
        return null;
    }

    @Override
    public Object visit(ASTBoolean node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTAdd node, Object data) {
        performArithmatic(node, data);
        return null;
    }

    @Override
    public Object visit(ASTSubtract node, Object data) {
        performArithmatic(node, data);
        return null;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        HashMap<String, Symbol> currentScopeSymbolTable = symbolTable.get(currentScope);
        if (currentScopeSymbolTable == null) {
            currentScopeSymbolTable = new HashMap<>();
        }
        Token variableName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Symbol variableSymbol = currentScopeSymbolTable.get(variableName.image);
        if (variableSymbol == null) { //if it's not in the current scope check the programmes scope
            variableSymbol = symbolTable.get(PROGRAMME).get(variableName.image);
        }
        if (variableSymbol != null) { //If it's in the main programmes scope after the last check we're ok
            Token assignedValue = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Node assignedNode = node.jjtGetChild(1);
            if (assignedNode instanceof ASTVariable) {
                Symbol assignedSymbol = symbolTable.get(currentScope).get(assignedValue.image);
                if (assignedSymbol == null) {
                    assignedSymbol = symbolTable.get(PROGRAMME).get(assignedValue.image);
                }
                if (assignedSymbol == null) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, variableSymbol.getDataType() + " \"" + assignedValue.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Programme\""));
                } else if (!variableSymbol.getType().image.equals(assignedSymbol.getType().image)) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "\"" + variableName.image + "\" and \"" + assignedValue.image + "\" are not of same type"));
                } else if (variableSymbol.getValues().size() == 0) {
                    variableSymbol.setIsRead(true);
                    Variable variable = new Variable(variableSymbol.getType().toString(), assignedValue.toString());
                    variableSymbol.addValue(variableName.image, variable);
                    currentScopeSymbolTable.put(variableName.image, variableSymbol);
                    symbolTable.put(currentScope, currentScopeSymbolTable);
                    assignedSymbol.setIsRead(true);
                    updateSymbol(assignedValue.image, assignedSymbol);
                } else {
                    assignedSymbol.setIsRead(true);
                    assignedSymbol.setIsCalled(true);
                    updateSymbol(assignedValue.image, assignedSymbol);
                }
            } else if (assignedNode instanceof ASTDigit) {
                if (!variableSymbol.getType().image.equals("integer")) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot assign type number to \"" + variableName.image + "\""));
                } else if (variableSymbol.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    Variable variable = new Variable(variableSymbol.getType().toString(), assignedValue.toString());
                    variableSymbol.addValue(variableName.image, variable);
                    variableSymbol.setIsCalled(true);
                    updateSymbol(variableName.image, variableSymbol);
                }
            } else if (assignedNode instanceof ASTBoolean) {
                if (!variableSymbol.getType().image.equals("boolean")) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot assign type boolean to \"" + variableName.image + "\""));
                }
                // if its initial value has already been stored, leave it
                Variable variable = new Variable(variableSymbol.getType().toString(), assignedValue.toString());
                variableSymbol.addValue(variableName.image, variable);
                currentScopeSymbolTable.put(variableName.image, variableSymbol);
                symbolTable.put(currentScope, currentScopeSymbolTable);
            } else if (assignedNode instanceof ASTFunctionCall ||
                    assignedNode instanceof ASTMinus) {
                Token functionToken = (Token) assignedNode.jjtGetChild(0).jjtAccept(this, null);
                // if its initial value has already been stored, leave it
                Variable variable = new Variable(variableSymbol.getType().toString(), functionToken.toString());
                variableSymbol.addValue(functionToken.image, variable);
                currentScopeSymbolTable.put(variableName.image, variableSymbol);
                symbolTable.put(currentScope, currentScopeSymbolTable);
            }  else {
                node.childrenAccept(this, data);
            }
        } else {
            errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Variable \"" + variableName.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Programme\""));
        }
        return null;
    }

    private void setAsReadAndCalled(Token token) {
        Symbol rightSymbol = symbolTable.get(currentScope).get(token.image);
        if (rightSymbol == null) {
            rightSymbol = symbolTable.get(PROGRAMME).get(token.image);
        }
        rightSymbol.setIsRead(true);
        rightSymbol.setIsCalled(true);
        updateSymbol(token.image, rightSymbol);
    }

    //Place the symbol into the correct scope
    private void updateSymbol(String symbolName, Symbol symbol) {
        HashMap<String, Symbol> tempMap;
        if (symbol.getScope().equals(currentScope)) {
            tempMap = symbolTable.get(currentScope);
            tempMap.put(symbolName, symbol);
            symbolTable.put(currentScope, tempMap);
        } else {
            tempMap = symbolTable.get(symbol.getScope());
            tempMap.put(symbolName, symbol);
            symbolTable.put(symbol.getScope(), tempMap);
        }
    }

    private void isSematicError() {
        System.out.println("Semantic Check Results");
        if (functionsNotCalled > 0) {
            System.out.println(functionsNotCalled + " function(s) are declared but not used.");
        }
        if (variablesNotWritten.size() > 0) {
            StringBuilder notWrittenVariables = new StringBuilder();
            System.out.println(variablesNotWritten.size() + " variable(s) have not been initialised:");
            for(String variable: variablesNotWritten){
                notWrittenVariables.append(variable).append(",");
            }
            notWrittenVariables.deleteCharAt(notWrittenVariables.length()-1);
            System.out.println(notWrittenVariables.toString());
        }
        if (variablesNotRead.size() > 0) {
            StringBuilder notReadVariables = new StringBuilder();
            System.out.println(variablesNotRead.size() + " variable(s) have not been accessed:");
            for(String variable: variablesNotRead){
                notReadVariables.append(variable).append(",");
            }
            notReadVariables.deleteCharAt(notReadVariables.length()-1);
            System.out.println(notReadVariables.toString());
        }
    }

    private void printErrorList() {
        for (ErrorMessage errorMessage : errorList) {
            System.out.println(errorMessage + "\n");
        }
    }

    private static boolean isInt(String s) {
        try {
            // is integer
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBoolean(String s) {
        return Boolean.parseBoolean(s);
    }

    //Evaluate boolean comparison (AND & OR) node and return if their are children nodes to go down
    private void isValidBooleanComparison(Node parentNode, Object data) {
        Node leftNode = parentNode.jjtGetChild(0);
        Token leftToken = (Token) parentNode.jjtGetChild(0).jjtAccept(this, null);
        if (checkBooleanComparison(leftNode, leftToken)) {
            evaluateChildCondition(parentNode, data);
        }

        Node rightNode = parentNode.jjtGetChild(1);
        Token rightToken = (Token) parentNode.jjtGetChild(1).jjtAccept(this, null);
        if (checkBooleanComparison(rightNode, rightToken)) {
            evaluateChildCondition(parentNode, data);
        }
    }

    //Evaluates boolean condition and returns if there is a child node
    private boolean checkBooleanComparison(Node node, Token token) {
        if (node instanceof ASTVariable) {
            Symbol symbol = symbolTable.get(currentScope).get(token.image);
            if (symbol == null) {
                symbol = symbolTable.get(PROGRAMME).get(token.image);
            }
            if (symbol == null) {
                errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable or Const \"" + token.image + "\" not declared in  any scope \""));
                return false;
            } else if (symbol.getValues().size() == 0) {
                if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) {
                    // error if var has no value
                    errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable \"" + token.image + "\" has no value in scope \"" + currentScope + "\""));
                    return false;
                }
            } else if (!symbol.getType().image.equals("boolean")) {
                errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, symbol.getDataType() + " \"" + token.image + "\" is not of type boolean"));
                return false;
            }
        } else if (node instanceof ASTDigit) {
            errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Invalid type for comparison"));
            return false;
        } else if (node instanceof ASTBoolean) {
            return false;
        }
        return true;
    }

    //Evaluate two sides of boolean operation eg. greater than, equals
    private void isValidBooleanOperation(Node parentNode, Object data) {
        String leftSymbolType = "";
        Node leftNode = parentNode.jjtGetChild(0);
        Token leftToken = (Token) parentNode.jjtGetChild(0).jjtAccept(this, null);
        if (leftNode instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol leftSymbol = symbolTable.get(currentScope).get(leftToken.image);
            if (leftSymbol == null) {
                leftSymbol = symbolTable.get(PROGRAMME).get(leftToken.image);
            }
            if (leftSymbol == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable or Const \"" + leftToken.image + "\" not declared in any scope \""));
            } else {
                leftSymbolType = leftSymbol.getType().image;
                leftSymbol.setIsRead(true);
                updateSymbol(leftToken.image, leftSymbol);

                if (leftSymbol.getValues().size() == 0) {
                    if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) {
                        // error if var has no value
                        errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable \"" + leftToken.image + "\" has no value in scope \"" + currentScope + "\""));
                    }
                }
            }
        } else if (leftNode instanceof ASTDigit) {
            leftSymbolType = "integer";
        } else if (leftNode instanceof ASTBoolean) {
            leftSymbolType = "boolean";
        }

        Node rightNode = parentNode.jjtGetChild(1);
        Token rightToken = (Token) parentNode.jjtGetChild(1).jjtAccept(this, null);
        if (rightNode instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol rightSymbol = symbolTable.get(currentScope).get(rightToken.image);
            if (rightSymbol == null) {
                rightSymbol = symbolTable.get(PROGRAMME).get(rightToken.image);
            }
            if (rightSymbol == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable or Constant \"" + rightToken.image + "\" not declared in any scope \""));
            } else if (rightSymbol.getValues().size() == 0) {
                if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) {
                    // error if var has no value
                    errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable \"" + rightToken.image + "\" has no value in current scope \"" + currentScope + "\""));
                }
                rightSymbol.setIsRead(true);
                updateSymbol(leftToken.image, rightSymbol);
            } else if (!rightSymbol.getType().image.equals(leftSymbolType)) {
                // error if is not a boolean
                errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, rightSymbol.getDataType() + " \"" + rightToken.image + "\" is not of type " + leftSymbolType));
            }

        } else if ((rightNode instanceof ASTDigit && !leftSymbolType.equals("integer")) || (rightNode instanceof ASTBoolean && !leftSymbolType.equals("boolean"))) {
            // error if number
            errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Invalid boolean using \"" + rightToken.image));
        }
        evaluateChildCondition(parentNode, data);
    }

    private void evaluateChildCondition(Node parentNode, Object data) {
        //Allow for inner evaluation of child comparisons
        if (parentNode instanceof ASTAnd) {
            ASTAnd node = (ASTAnd) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTOr) {
            ASTOr node = (ASTOr) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTGreaterThan) {
            ASTGreaterThan node = (ASTGreaterThan) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTGreaterThanEqual) {
            ASTGreaterThanEqual node = (ASTGreaterThanEqual) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTLessThan) {
            ASTLessThan node = (ASTLessThan) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTLessThanEqual) {
            ASTLessThanEqual node = (ASTLessThanEqual) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTEqual) {
            ASTEqual node = (ASTEqual) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTNotEqual) {
            ASTNotEqual node = (ASTNotEqual) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTNot) {
            ASTNot node = (ASTNot) parentNode;
            node.childrenAccept(this, data);
        }
    }

    private void performArithmatic(Node parentNode, Object data) {
        Token leftToken = (Token) parentNode.jjtGetChild(0).jjtAccept(this, null);
        Node leftNode = parentNode.jjtGetChild(0);
        if (isValidArithmetic(leftNode, leftToken)) {
            evaluateArithmaticChildren(parentNode, data);
        }

        Token rightToken = (Token) parentNode.jjtGetChild(1).jjtAccept(this, null);
        Node rightNode = parentNode.jjtGetChild(1);
        if (isValidArithmetic(rightNode, rightToken)) {
            evaluateArithmaticChildren(parentNode, data);
        }
    }

    private void evaluateArithmaticChildren(Node parentNode, Object data) {
        if (parentNode instanceof ASTAdd) {
            ASTAdd node = (ASTAdd) parentNode;
            node.childrenAccept(this, data);
        } else if (parentNode instanceof ASTSubtract) {
            ASTSubtract node = (ASTSubtract) parentNode;
            node.childrenAccept(this, data);
        }
    }

    //Will throw appropriate errors and return back if it's something to recurse into
    private boolean isValidArithmetic(Node node, Token token) {
        if (node instanceof ASTVariable) {
            String foundIn1 = currentScope;

            HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
            Symbol child1Stc = mapTemp.get(token.image);
            if (child1Stc == null) {
                mapTemp = symbolTable.get(PROGRAMME);
                foundIn1 = PROGRAMME;
                child1Stc = mapTemp.get(token.image);
            }
            if (child1Stc == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable or Const \"" + token.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Programme\""));
                return false;
            } else if (child1Stc.getValues().size() == 0) {
                if (currentScope.equals(PROGRAMME) || currentScope.equals(MAIN)) {
                    // error if var has no value
                    errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable \"" + token.image + "\" has no value in currentScope \"" + currentScope + "\""));
                    return false;
                }
            } else if (!child1Stc.getType().image.equals("integer")) {
                // var / const has been declared and has value
                // error if var is of type boolean
                errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Cannot add variable \"" + token.image + "\". Not of type Number"));
                return false;
            } else {
                // set isRead value
                child1Stc.setIsRead(true);
                mapTemp.put(token.image, child1Stc);
                symbolTable.put(foundIn1, mapTemp);
            }
        } else if (node instanceof ASTDigit) {
            return false;
        } else if (node instanceof ASTBoolean) {
            // error if first child node is a boolean
            errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Cannot add value \"" + token.image + "\" (type boolean)"));
            return false;
        }
        return true;
    }

    private class ErrorMessage {
        int lineNumber;
        int columnNumber;
        String errorMessage;

        ErrorMessage(int lineNumber, int columnNumber, String errorMessage) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "Error at line " + lineNumber + ", column " + columnNumber + ":\n" + errorMessage;
        }
    }
}