import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Visitor implements CCALParserVisitor {

    private String currentScope = "Program"; // Initial currentScope, will always start with program
    private String previousScope;
    private HashMap<String, HashMap<String, Symbol>> symbolTable = new HashMap<>();
    private int variablesNotWritten = 0;
    private int variablesNotRead = 0;
    private int functionsNotCalled = 0;
    private List<ErrorMessage> errorList = new ArrayList<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        symbolTable.put(currentScope, new HashMap<>());
        node.childrenAccept(this, data);

        // Perform semantic checks over symbol table once programme has been read
        System.out.println("-----Start Symbol Table-----");
        Set scopes = symbolTable.keySet();
        for (Object scope : scopes) {
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
                if (currentSymbol.getDataType() == DataType.Function) {
                    System.out.println(" Parameters: " + currentSymbol.printValues());
                    System.out.println(" Is called?: " + currentSymbol.getIsCalled());
                    if (!currentSymbol.getIsCalled()) {
                        functionsNotCalled++;
                    }
                } else {
                    if (currentSymbol.getValues().size() > 0) {
                        System.out.println(" Values: " + currentSymbol.printValues());
                    }
                    else {
                        System.out.println(" Values: {}");
                        variablesNotWritten++;
                    }

                    System.out.println(" Is written to?: " + (currentSymbol.getValues().size() > 0));
                    System.out.println(" Is read from?: " + currentSymbol.getIsRead());
                    if (!currentSymbol.getIsRead()) {
                        variablesNotRead++;
                    }
                }
            }
            System.out.println("-----End " + scopeName + " Scope-----");
        }
        System.out.println("-----End Symbol Table-----");

        if (errorList.size() == 0) {
            isSematicError();
            System.out.println("No errors found");
        } else {
            System.out.println(errorList.size() + " error(s).");
            printErrorList();
        }

        return null;
    }

    @Override
    public Object visit(ASTMain node, Object data) {
        // update to currentScope of function
        previousScope = currentScope;
        currentScope = "Main";
        symbolTable.put(currentScope, new HashMap<>());
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

        Token type = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Token name = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        Symbol symbol = new Symbol();
        symbol.setName(name);
        symbol.setType(type);
        symbol.setScope(currentScope);
        symbol.setDataType(DataType.Function);
        symbol.setNumArgs(node.jjtGetChild(2).jjtGetNumChildren());

        //Check if we already have the function
        if (currentHashScopeMap.containsKey(name.image)) {
            Symbol originalFunc = currentHashScopeMap.get(name.image);
            //Check if it contains the same number of arguments
            if (originalFunc.getNumArgs() == symbol.getNumArgs()) {
                errorList.add(new ErrorMessage(name.beginLine, name.beginColumn, "Function \"" + name.image + "\" already declared with " + symbol.getNumArgs() + " parameters"));
            }
        } else {
            currentHashScopeMap.put(name.image, symbol);
            symbolTable.put(currentScope, currentHashScopeMap);
        }


        //Update current scope to function
        previousScope = currentScope;
        currentScope = name.image;

        //Evaluate to functions params and body
        node.jjtGetChild(2).jjtAccept(this, null);
        node.jjtGetChild(3).jjtAccept(this, null);

        //Revert back scope
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
            Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
            Node node1 = node.jjtGetChild(0);
            if (node1 instanceof ASTVariable) {
                String foundIn = currentScope;

                HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
                Symbol idStc = mapTemp.get(id.image);
                if (idStc == null) {
                    mapTemp = symbolTable.get("Program");
                    foundIn = "Program";
                    idStc = mapTemp.get(id.image);
                }
                if (idStc.getValues().size() == 0) {
                    if (currentScope.equals("Program") || currentScope.equals("Main")) {
                        // error if var has no value
                        errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Variable \"" + id.image + "\" has no value in currentScope \"" + currentScope + "\""));
                    }
                }
                idStc.setIsRead(true);
                mapTemp.put(id.image, idStc);
                symbolTable.put(foundIn, mapTemp);
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
        HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
        if (mapTemp == null) {
            mapTemp = new HashMap<>();
        }

        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Token type = (Token) node.jjtGetChild(1).jjtAccept(this, null);

        Symbol symbol = new Symbol();
        symbol.setName(id);
        symbol.setType(type);
        symbol.setScope(currentScope);
        symbol.setDataType(DataType.ParamVariable);
        mapTemp.put(id.image, symbol);
        symbolTable.put(currentScope, mapTemp);

        Token funcId = (Token) node.jjtGetParent().jjtGetParent().jjtGetChild(1).jjtAccept(this, null);
        String foundIn = currentScope;
        mapTemp = symbolTable.get(currentScope);
        Symbol funcStc = mapTemp.get(funcId.image);
        if (funcStc == null) {
            mapTemp = symbolTable.get("Program");
            funcStc = mapTemp.get(funcId.image);
            foundIn = "Program";
        }
        if (funcStc.getValues().containsKey(id.image)) {
            // error
            // same param id already in this function
            errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Duplicate parameter names, \"" + id.image + "\" for function \"" + funcId.image + "\""));
        } else {
            Variable variable = new Variable(funcStc.getType().toString(),id.image);
            funcStc.addValue(id.image, variable);
            mapTemp.put(funcId.image, funcStc);
            symbolTable.put(foundIn, mapTemp);
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
        Node node1 = node.jjtGetChild(0);
        Token child = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol childStc = symbolTable.get(currentScope).get(child.image);
            if(childStc == null) {
                childStc = symbolTable.get("Program").get(child.image);
            }
            if(childStc == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(child.beginLine,child.beginColumn,"Variable or Const \"" + child.image + "\" not declared in scope \"" + currentScope + "\" or \"Program\""));
            } else if(childStc.getValues().size() == 0) {
                if(currentScope.equals("Program") || currentScope.equals("Main")) {
                    // error if var has no value
                    errorList.add(new ErrorMessage(child.beginLine,child.beginColumn,"Variable \"" + child.image + "\" has no value in scope \"" + currentScope + "\""));
                }
            } else if(!childStc.getType().image.equals("boolean")) {
                // error if is not a boolean
                errorList.add(new ErrorMessage(child.beginLine,child.beginColumn,childStc.getDataType() + " \"" + child.image + "\" is not of type boolean"));
            }
        } else if(node1 instanceof ASTDigit) {
            // error if number
            errorList.add(new ErrorMessage(child.beginLine,child.beginColumn,"Invalid value for boolean comparison"));

        } else if(!(node1 instanceof ASTBoolean)) {
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTDeclarationList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTConstDeclaration node, Object data) {
        HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
        if (mapTemp == null) {
            mapTemp = new HashMap<>();
        }

        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if (mapTemp.get(id.image) == null) {
            Token type = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbol symbol = new Symbol();
            symbol.setName(id);
            symbol.setType(type);
            symbol.setScope(currentScope);
            symbol.setDataType(DataType.Constant);
            symbolTable.put(currentScope, mapTemp);
            mapTemp.put(id.image, symbol);

            Token value = (Token) node.jjtGetChild(2).jjtAccept(this, null);
            if (type.image.equals("integer") && !isInt(value.image)) {
                errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Cannot assign boolean \"" + value.image + "\" to constant \"" + id.image + "\""));
            } else if (type.image.equals("boolean") && !isBoolean(value.image)) {
                errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Cannot assign number \"" + value.image + "\" to constant \"" + id.image + "\""));
            } else {
                Variable variable = new Variable(symbol.getType().toString(),value.image.toLowerCase());
                symbol.addValue(id.image, variable);
            }

            mapTemp.put(id.image, symbol);
        } else {
            errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Constant \"" + id.image + "\" already declared in currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, mapTemp);

        return null;
    }

    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);

        HashMap<String, Symbol> mapTemp1 = symbolTable.get("Program");
        if (mapTemp1 != null && mapTemp1.size() != 0) {
            if (mapTemp1.get(id.image) != null && currentScope.equals("Main")) {
                // error
                // id already declared in currentScope Program, if currentScope is Main
                System.out.println("Variable \"" + id.image + "\" already declared in currentScope \"" + currentScope + "\"");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
            }
        }


        HashMap<String, Symbol> mapTemp2 = symbolTable.get(currentScope);
        if (mapTemp2 == null) {
            mapTemp2 = new HashMap<>();
        }

        // if the var has not been declared already, add it to the symbolTable
        // if the var has been declared already, then we have an error
        Symbol idStc = mapTemp2.get(id.image);
        if (idStc == null) {
            Token type = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbol symbol = new Symbol();
            symbol.setName(id);
            symbol.setType(type);
            symbol.setScope(currentScope);
            symbol.setDataType(DataType.Variable);
            mapTemp2.put(id.image, symbol);
        } else {
            System.out.println("Variable \"" + id.image + "\" already declared in currentScope \"" + currentScope + "\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
        }
        symbolTable.put(currentScope, mapTemp2);

        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        String foundIn = currentScope;

        // check if function(id) has been declared
        HashMap<String, Symbol> scopeST = symbolTable.get(currentScope);
        if (scopeST == null) {
            scopeST = symbolTable.get("Program");
            foundIn = "Program";
        }
        Symbol idStc = scopeST.get(id.image);
        if (idStc == null) {
            foundIn = "Program";
            scopeST = symbolTable.get("Program");
            idStc = scopeST.get(id.image);
        }

        if (idStc != null) {
            // go to ArgList
            if (node.jjtGetNumChildren() > 1) {
                node.jjtGetChild(1).jjtAccept(this, null);
            }
            idStc.setIsCalled(true);
            scopeST.put(id.image, idStc);
            symbolTable.put(foundIn, scopeST);
        } else {
            errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Variable \"" + id.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Program\""));

        }

        return null;
    }

    @Override
    public Object visit(ASTMinus node, Object data) {
        // check if id has been declared
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);

        HashMap<String, Symbol> currentSymbolTable = symbolTable.get(currentScope);
        Symbol symbol = currentSymbolTable.get(id.image);
        if (symbol == null) {
            currentSymbolTable = symbolTable.get("Program");
            symbol = currentSymbolTable.get(id.image);
        }

        if (symbol == null) {
            // error if still not declared
            errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Variable \"" + id.image + "\" not declared in any scope"));
        } else if (symbol.getValues().size() == 0) {
            if (currentScope.equals("Program") || currentScope.equals("Main")) {
                // error if var has no value
                errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Variable \"" + id.image + "\" has no value in currentScope \"" + currentScope + "\""));
            }
        } else if (!symbol.getType().image.equals("integer")) {
            errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Cannot create negative value from " + symbol.getDataType() + " \"" + id.image + "\". Not of type integer"));
        } else {
            Variable variable = new Variable(symbol.getType().toString(),"-" +id);
            symbol.addValue(id.image, variable);
            // set isRead value
            symbol.setIsRead(true);
            symbol.setIsCalled(true);
            updateScope(id.image,symbol);
        }

        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        Token funcId = (Token) node.jjtGetParent().jjtGetChild(0).jjtAccept(this, null);

        HashMap<String, Symbol> mapTemp = symbolTable.get("Program");
        if (mapTemp != null) {
            Symbol funcStc = mapTemp.get(funcId.image);
            if (funcStc == null) {
                // error, no such function
                errorList.add(new ErrorMessage(funcId.beginLine, funcId.beginColumn, "No such function called \"" + funcId.image + "\""));
            } else {
                // check for correct number of Args
                int numArgsDeclared = funcStc.getNumArgs();
                int numArgsPassed = node.jjtGetNumChildren();
                if (numArgsDeclared != numArgsPassed) {
                    // error, incorrect num of args

                    errorList.add(new ErrorMessage(funcId.beginLine, funcId.beginColumn, "Function \"" + funcId.image + "\" has invalid number of arguments. called with " + numArgsPassed + " argument(s), should have " + numArgsDeclared));

                } else if (numArgsDeclared > 0) {
                    int i = 0;
                    for (String key : funcStc.getValues().keySet()) {
                        String type = funcStc.getValues().get(key).get(0).getType();
                        Token argi = (Token) node.jjtGetChild(i).jjtGetChild(0).jjtAccept(this, null);
                        Symbol argiStc = mapTemp.get(argi.image);
                        if (argiStc == null) {
                            mapTemp = symbolTable.get(currentScope);
                            argiStc = mapTemp.get(argi.image);
                        }
                        if (!argiStc.getType().image.equals(type)) {
                            // error
                            // argument in function call is incorrect type
                            errorList.add(new ErrorMessage(argi.beginLine, argi.beginColumn, "\"" + argi.image + "\" in function call for \"" + funcId.image + "\" is of incorrect type"));
                        }
                        //if argument is variable then it is accessed
                        if (argiStc.getDataType() != DataType.Unknown) {
                            argiStc.setIsRead(true);
                            updateScope(argi.image, argiStc);
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
        // Check if Arg has been declared
        Token arg = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        String foundIn = currentScope;

        HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
        Symbol argStc = mapTemp.get(arg.image);
        if (argStc == null) {
            mapTemp = symbolTable.get("Program");
            foundIn = "Program";
            argStc = mapTemp.get(arg.image);
        }

        if (argStc == null) {
            // error
            // no such var or const in this currentScope with that name
            System.out.println("Variable or Const \"" + arg.image + "\" has not been declared in currentScope \"" + currentScope + "\"");
            System.out.println("Error at line " + arg.beginLine + ", column " + arg.beginColumn);
            System.out.println();
            //numberOfErrors++;
        } else if (argStc.getValues().size() == 0) {
            if (currentScope.equals("Program") || currentScope.equals("Main")) {
                // error
                // there is a var in this currentScope with that name, but it has no value
                System.out.println("Variable \"" + arg.image + "\" has been declared in currentScope \"" + currentScope + "\", but has no value");
                System.out.println("Error at line " + arg.beginLine + ", column " + arg.beginColumn);
                System.out.println();
                //numberOfErrors++;
            }
        } else {
            argStc.setIsRead(true);
            mapTemp.put(arg.image, argStc);
            symbolTable.put(foundIn, mapTemp);
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
        isValidBooleanOperation(node,data);
        return null;
    }

    @Override
    public Object visit(ASTNotEqual node, Object data) {
        isValidBooleanOperation(node,data);
        return null;
    }

    @Override
    public Object visit(ASTLessThan node, Object data) {
        isValidBooleanOperation(node,data);
        return null;
    }

    @Override
    public Object visit(ASTLessThanEqual node, Object data) {
        isValidBooleanOperation(node,data);
        return null;
    }

    @Override
    public Object visit(ASTGreaterThan node, Object data) {
        isValidBooleanOperation(node,data);
        return null;
    }

    @Override
    public Object visit(ASTGreaterThanEqual node, Object data) {
        isValidBooleanOperation(node,data);
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

    //Will throw appropriate errors and return back if it's something to recurse into
    boolean isValidArithmetic(Node node, Token token) {
        if (node instanceof ASTVariable) {
            String foundIn1 = currentScope;

            HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
            Symbol child1Stc = mapTemp.get(token.image);
            if (child1Stc == null) {
                mapTemp = symbolTable.get("Program");
                foundIn1 = "Program";
                child1Stc = mapTemp.get(token.image);
            }
            if (child1Stc == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable or Const \"" + token.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Program\""));
                return false;
            } else if (child1Stc.getValues().size() == 0) {
                if (currentScope.equals("Program") || currentScope.equals("Main")) {
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

    @Override
    public Object visit(ASTAdd node, Object data) {
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Node node1 = node.jjtGetChild(0);
        if (isValidArithmetic(node1, child1)) {
            node.childrenAccept(this, data);
        }

        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        Node node2 = node.jjtGetChild(1);
        if (isValidArithmetic(node2, child2)) {
            node.childrenAccept(this, data);
        }
        return null;
    }

    @Override
    public Object visit(ASTSubtract node, Object data) {
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Node node1 = node.jjtGetChild(0);
        if (isValidArithmetic(node1, child1)) {
            node.childrenAccept(this, data);
        }

        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        Node node2 = node.jjtGetChild(1);
        if (isValidArithmetic(node2, child2)) {
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        HashMap<String, Symbol> mapTemp = symbolTable.get(currentScope);
        if (mapTemp == null) {
            mapTemp = new HashMap<String, Symbol>();
        }

        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);

        // get stc from being declared in this currentScope
        Symbol idStc = mapTemp.get(id.image);
        // if not declared here, check if delcared in Program currentScope (Global variable)
        if (idStc == null) {
            idStc = symbolTable.get("Program").get(id.image);
        }

        if (idStc != null) {
            Token value = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Node valueNode = node.jjtGetChild(1);

            if (valueNode instanceof ASTVariable) {
                Symbol valueStc = symbolTable.get(currentScope).get(value.image);
                if (valueStc == null) {
                    valueStc = symbolTable.get("Program").get(value.image);
                }
                if (valueStc == null) {
                    // error
                    // valueStc is not declared
                    errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, idStc.getDataType() + " \"" + value.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Program\""));
                } else if (!idStc.getType().image.equals(valueStc.getType().image)) {
                    // error
                    // id and value not of same type
                    errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "\"" + id.image + "\" and \"" + value.image + "\" are not of same type"));
                } else if (idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    idStc.setIsRead(true);
                    Variable variable = new Variable(idStc.getType().toString(),value.toString());
                    idStc.addValue(id.image, variable);
                    mapTemp.put(id.image, idStc);
                    symbolTable.put(currentScope, mapTemp);

                    valueStc.setIsRead(true);
                    updateScope(value.image, valueStc);
                } else {
                    valueStc.setIsRead(true);
                    valueStc.setIsCalled(true);
                    if (valueStc.getScope().equals(currentScope)) {
                        mapTemp.put(value.image, valueStc);
                        symbolTable.put(currentScope, mapTemp);
                    } else {
                        HashMap<String, Symbol> outerScope = symbolTable.get(valueStc.getScope());
                        outerScope.put(value.image, valueStc);
                        symbolTable.put(valueStc.getScope(), outerScope);
                    }
                }
            } else if (valueNode instanceof ASTDigit) {
                if (!idStc.getType().image.equals("integer")) {
                    // error
                    // if attempting to assign number to id not of type number
                    errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Cannot assign type number to \"" + id.image + "\""));
                } else if (idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    Variable variable = new Variable(idStc.getType().toString(),value.toString());
                    idStc.addValue(id.image, variable);
                    idStc.setIsCalled(true);
                    updateScope(id.image, idStc);
                }
            } else if (valueNode instanceof ASTBoolean) {
                if (!idStc.getType().image.equals("boolean")) {
                    // error
                    // if attempting to assign boolean to id not of type boolean
                    errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Cannot assign type boolean to \"" + id.image + "\""));
                }
                // if its initial value has already been stored, leave it
                Variable variable = new Variable(idStc.getType().toString(),value.toString());
                idStc.addValue(id.image, variable);
                mapTemp.put(id.image, idStc);
                symbolTable.put(currentScope, mapTemp);
            } else if (valueNode instanceof ASTFunctionCall ||
                    valueNode instanceof ASTPos ||
                    valueNode instanceof ASTMinus) {
                Token subChild = (Token) valueNode.jjtGetChild(0).jjtAccept(this, null);
                // if its initial value has already been stored, leave it
                Variable variable = new Variable(idStc.getType().toString(),subChild.toString());
                idStc.addValue(subChild.image, variable);
                mapTemp.put(id.image, idStc);
                symbolTable.put(currentScope, mapTemp);
            } else if (valueNode instanceof ASTAdd ||
                    valueNode instanceof ASTSubtract) {
                Token subChild1 = (Token) valueNode.jjtGetChild(0).jjtAccept(this, null);
                Token subChild2 = (Token) valueNode.jjtGetChild(1).jjtAccept(this, null);
                // if its initial value has already been stored, leave it
                String operator = "-";
                if(valueNode instanceof ASTAdd)
                    operator = "+";
                Variable variable1 = new Variable(idStc.getType().toString(),subChild1.toString() + operator + subChild2.toString());
                idStc.addValue(subChild1.image, variable1);
                mapTemp.put(id.image, idStc);
                symbolTable.put(currentScope, mapTemp);
                
                //update tokens if variables
                if(valueNode.jjtGetChild(0) instanceof  ASTVariable){
                    setAsReadAndCalled(subChild1);
                }
                if(valueNode.jjtGetChild(1) instanceof  ASTVariable){
                    setAsReadAndCalled(subChild2);
                }
            } else {
                node.childrenAccept(this, data);
            }
        } else {
            // error
            // still not declared
            errorList.add(new ErrorMessage(id.beginLine, id.beginColumn, "Variable \"" + id.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Program\""));
        }


        return null;
    }

    private void setAsReadAndCalled(Token token){
        Symbol rightSymbol =  symbolTable.get(currentScope).get(token.image);
        if(rightSymbol == null){
            rightSymbol =  symbolTable.get("Program").get(token.image);
        }
        rightSymbol.setIsRead(true);
        rightSymbol.setIsCalled(true);
        updateScope(token.image,rightSymbol);
    }

    private void updateScope(String symbolName, Symbol symbol) {
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
        if (variablesNotWritten > 0) {
            System.out.println(variablesNotWritten + " variable(s) have not been initialised.");
        }
        if (variablesNotRead > 0) {
            System.out.println(variablesNotRead + " variable(s) have not been accessed.");
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
            evaluateChildCondition(parentNode,data);
        }

        Node rightNode = parentNode.jjtGetChild(1);
        Token rightToken = (Token) parentNode.jjtGetChild(1).jjtAccept(this, null);
        if (checkBooleanComparison(rightNode, rightToken)) {
            evaluateChildCondition(parentNode,data);
        }
    }

    //Evaluates boolean condition and returns if there is a child node
    private boolean checkBooleanComparison(Node node, Token token) {
        if (node instanceof ASTVariable) {
            Symbol symbol = symbolTable.get(currentScope).get(token.image);
            if (symbol == null) {
                symbol = symbolTable.get("Program").get(token.image);
            }
            if (symbol == null) {
                errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable or Const \"" + token.image + "\" not declared in  any scope \""));
                return false;
            } else if (symbol.getValues().size() == 0) {
                if (currentScope.equals("Program") || currentScope.equals("Main")) {
                    // error if var has no value
                    errorList.add(new ErrorMessage(token.beginLine, token.beginColumn, "Variable \"" + token.image + "\" has no value in scope \"" + currentScope + "\""));
                    return false;
                }
            } else if (!symbol.getType().image.equals("boolean")) {
                // error if is not a boolean
                System.out.println();
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
                leftSymbol = symbolTable.get("Program").get(leftToken.image);
            }
            if (leftSymbol == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable or Const \"" + leftToken.image + "\" not declared in any scope \""));
            } else {
                leftSymbolType = leftSymbol.getType().image;
                leftSymbol.setIsRead(true);
                updateScope(leftToken.image,leftSymbol);

                if (leftSymbol.getValues().size() == 0) {
                    if (currentScope.equals("Program") || currentScope.equals("Main")) {
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
                rightSymbol = symbolTable.get("Program").get(rightToken.image);
            }
            if (rightSymbol == null) {
                // error if var or const has not been declared before
                errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable or Constant \"" + rightToken.image + "\" not declared in any scope \""));
            } else if (rightSymbol.getValues().size() == 0) {
                if (currentScope.equals("Program") || currentScope.equals("Main")) {
                    // error if var has no value
                    errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Variable \"" + rightToken.image + "\" has no value in current scope \"" + currentScope + "\""));
                }
                rightSymbol.setIsRead(true);
                updateScope(leftToken.image,rightSymbol);
            } else if (!rightSymbol.getType().image.equals(leftSymbolType)) {
                // error if is not a boolean
                errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, rightSymbol.getDataType() + " \"" + rightToken.image + "\" is not of type " + leftSymbolType));
            }

        } else if ((rightNode instanceof ASTDigit && !leftSymbolType.equals("integer")) || (rightNode instanceof ASTBoolean && !leftSymbolType.equals("boolean"))) {
            // error if number
            errorList.add(new ErrorMessage(leftToken.beginLine, leftToken.beginColumn, "Invalid boolean using \"" + rightToken.image));
        }
        evaluateChildCondition(parentNode,data);
    }

    private void evaluateChildCondition(Node parentNode, Object data){
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