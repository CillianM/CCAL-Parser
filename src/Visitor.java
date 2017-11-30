import java.util.HashMap;
import java.util.Set;

public class Visitor implements CCALParserVisitor {

    String scope = "Program"; // Initial scope, will always start with program
    String prevScope;
    HashMap<String, HashMap<String, Symbol>> SymbolTable = new HashMap<>();
    int numErrors = 0;
    int varsNotWritten = 0;
    int varsNotRead = 0;
    int funcsNotCalled = 0;

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        // Add a new node to symbol table
        SymbolTable.put(scope, new HashMap<String, Symbol>());

        // Visit all child nodes (i.e. whole program)
        node.childrenAccept(this, data);

        // This will be printed out when the visitor is finished
        System.out.println("**** Symbol Table ****");
        Set scopes = SymbolTable.keySet();
        for (Object scope1 : scopes) {
            String temp = (String) scope1;
            System.out.println("Scope: " + temp);

            Set symbols = SymbolTable.get(temp).keySet();
            if (symbols.size() == 0) {
                System.out.println(" Nothing declared");
            }
            for (Object symbol : symbols) {
                String temp2 = (String) symbol;
                Symbol temp3 = SymbolTable.get(temp).get(temp2);
                System.out.println(temp2);
                System.out.println(" DataType: " + temp3.getDataType());
                System.out.println(" Type: " + temp3.getType());
                if (temp3.getDataType() == DataType.Function) {
                    System.out.println(" Parameters: " + temp3.getValues());
                    System.out.println(" Is called?: " + temp3.getIsCalled());
                    if (!temp3.getIsCalled()) {
                        funcsNotCalled++;
                    }
                } else {
                    System.out.println(" Initial Value: " + temp3.getValues());
                    System.out.println(" Is written to?: " + (temp3.getValues().size() > 0));
                    System.out.println(" Is read from?: " + temp3.getIsRead());
                    if (temp3.getValues().size() == 0) {
                        varsNotWritten++;
                    }
                    if (!temp3.getIsRead()) {
                        varsNotRead++;
                    }
                }
            }
            System.out.println();
        }
        System.out.println("**********************");

        System.out.println();

        System.out.println("**** Semantic Analysis ****");
        if(funcsNotCalled == 1) {
            System.out.println("WARNING: " + funcsNotCalled + " function is declared but not used.");
        } else if(funcsNotCalled > 1) {
            System.out.println("WARNING: " + funcsNotCalled + " functions are declared but not used.");
        }
        if(varsNotWritten == 1) {
            System.out.println("WARNING: " + varsNotWritten + " variable has not been initialised.");
        } else if(varsNotWritten > 1) {
            System.out.println("WARNING: " + varsNotWritten + " variables have not been initialised.");
        }
        if(varsNotRead == 1) {
            System.out.println("WARNING: " + varsNotRead + " variable has not been accessed.");
        } else if(varsNotRead > 1) {
            System.out.println("WARNING: " + varsNotRead + " variables have not been accessed.");
        }
        if(numErrors == 0) {
            System.out.println("No errors.");
//            ThreeAddressVisitor tav = new ThreeAddressVisitor();
//            node.jjtAccept(tav, SymbolTable);
        } else {
            System.out.println(numErrors + " error(s).");
        }

        return null;
    }

    @Override
    public Object visit(ASTMain node, Object data) {
        // update to scope of function
        prevScope = scope;
        scope = "Main";

        node.childrenAccept(this, data);

        // update scope to previous scope
        scope = prevScope;
        prevScope = null;
        return null;
    }

    @Override
    public Object visit(ASTFunctionList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTFunction node, Object data) {
        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        if(mapTemp == null) {
            mapTemp = new HashMap<String, Symbol>();
        }

        Token type = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Token id = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        Symbol stc = new Symbol(id, type, scope, DataType.Function, node.jjtGetChild(2).jjtGetNumChildren());

        if(mapTemp.containsKey(id.image)) {
            Symbol originalFunc = mapTemp.get(id.image);
            if(originalFunc.getNumArgs() == stc.getNumArgs()) {
                // error
                // function already declared with same number of params
                System.out.println("Function \"" + id.image + "\" already declared with " + stc.getNumArgs() + " parameters");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else {
            mapTemp.put(id.image, stc);
            SymbolTable.put(scope, mapTemp);
        }


        // update to scope of function
        prevScope = scope;
        scope = id.image;

        // go to functions parameters
        node.jjtGetChild(2).jjtAccept(this, null);

        // go to function body
        node.jjtGetChild(3).jjtAccept(this, null);

        // update scope to previous scope
        scope = prevScope;
        prevScope = null;

        return null;
    }

    @Override
    public Object visit(ASTFunctionBody node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTReturnStatement node, Object data) {
        if(node.jjtGetNumChildren() > 0) {
            Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
            Node node1 = node.jjtGetChild(0);
            if(node1 instanceof ASTVariable) {
                String foundIn = scope;

                HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
                Symbol idStc = mapTemp.get(id.image);
                if(idStc == null) {
                    mapTemp = SymbolTable.get("Program");
                    foundIn = "Program";
                    idStc = mapTemp.get(id.image);
                }
                if(idStc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + id.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
                idStc.setIsRead(true);
                mapTemp.put(id.image, idStc);
                SymbolTable.put(foundIn, mapTemp);
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
        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        if(mapTemp == null) {
            mapTemp = new HashMap<String, Symbol>();
        }

        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Token type = (Token) node.jjtGetChild(1).jjtAccept(this, null);

        Symbol stc = new Symbol(id, type, scope, DataType.ParamVariable);
        mapTemp.put(id.image, stc);
        SymbolTable.put(scope, mapTemp);

        Token funcId = (Token) node.jjtGetParent().jjtGetParent().jjtGetChild(1).jjtAccept(this, null);
        String foundIn = scope;
        mapTemp = SymbolTable.get(scope);
        Symbol funcStc = mapTemp.get(funcId.image);
        if(funcStc == null) {
            mapTemp = SymbolTable.get("Program");
            funcStc = mapTemp.get(funcId.image);
            foundIn = "Program";
        }
        if(funcStc.getValues().containsKey(id.image)) {
            // error
            // same param id already in this function
            System.out.println("Duplicate parameter names, \"" + id.image + "\" for function \"" + funcId.image + "\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            funcStc.addValue(id.image, type.image);
            mapTemp.put(funcId.image, funcStc);
            SymbolTable.put(foundIn, mapTemp);
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
        Node node1 = (Node) node.jjtGetChild(0);
        Token child = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol childStc = SymbolTable.get(scope).get(child.image);
            if(childStc == null) {
                childStc = SymbolTable.get("Program").get(child.image);
            }
            if(childStc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child.beginLine + ", column " + child.beginColumn);
                System.out.println();
                numErrors++;
            } else if(childStc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child.beginLine + ", column " + child.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!childStc.getType().image.equals("boolean")) {
                // error if is not a boolean
                System.out.println(childStc.getDataType() + " \"" + child.image + "\" is not of type boolean");
                System.out.println("Error at line " + child.beginLine + ", column " + child.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node1 instanceof ASTDigit) {
            // error if number
            System.out.println("Cannot create not value of type number");
            System.out.println("Error at line " + child.beginLine + ", column " + child.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node1 instanceof ASTBoolean) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTOr node, Object data) {
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child1Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child1Stc.getType().image.equals("boolean")) {
                // error if is not a boolean
                System.out.println(child1Stc.getDataType() + " \"" + child1.image + "\" is not of type boolean");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node1 instanceof ASTDigit) {
            // error if number
            System.out.println("Cannot create OR value using \"" + child1.image + "\" (Type number)");
            System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node1 instanceof ASTBoolean) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals("boolean")) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type boolean");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit) {
            // error if number
            System.out.println("Cannot create OR value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child1Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child1Stc.getType().image.equals("boolean")) {
                // error if is not a boolean
                System.out.println(child1Stc.getDataType() + " \"" + child1.image + "\" is not of type boolean");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node1 instanceof ASTDigit) {
            // error if number
            System.out.println("Cannot create AND value using \"" + child1.image + "\" (Type number)");
            System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node1 instanceof ASTBoolean) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals("boolean")) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type boolean");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit) {
            // error if number
            System.out.println("Cannot create AND value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
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
        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        if(mapTemp == null) {
            mapTemp = new HashMap<String, Symbol>();
        }

        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        // if the const has not been declared already, add it to the SymbolTable
        // if the const has been declared already, then we have an error
        if(mapTemp.get(id.image) == null) {
            Token type = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbol stc = new Symbol(id, type, scope, DataType.Constant);
            mapTemp.put(id.image, stc);

            // add the const value to the map of values
            // since its a const this value should not ever change
            Token value = (Token) node.jjtGetChild(2).jjtGetChild(0).jjtAccept(this, null);
            if(type.image.equals("integer") && !isInt(value.image)) {
                System.out.println("Cannot assign boolean \"" + value.image + "\" to constant \"" + id.image + "\"");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
                numErrors++;
            } else if(type.image.equals("boolean") && !isBoolean(value.image)) {
                System.out.println("Cannot assign number \"" + value.image + "\" to constant \"" + id.image + "\"");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                stc.addValue(id.image, value);
            }

            mapTemp.put(id.image, stc);
        } else {
            System.out.println("Constant \"" + id.image + "\" already declared in scope \"" + scope + "\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        }
        SymbolTable.put(scope, mapTemp);

        return null;
    }

    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);

        HashMap<String, Symbol> mapTemp1 = SymbolTable.get("Program");
        if(mapTemp1 != null && mapTemp1.size() != 0) {
            if(mapTemp1.get(id.image) != null && scope.equals("Main")) {
                // error
                // id already declared in scope Program, if scope is Main
                System.out.println("Variable \"" + id.image + "\" already declared in scope \"" + scope + "\"");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
                numErrors++;
            }
        }


        HashMap<String, Symbol> mapTemp2 = SymbolTable.get(scope);
        if(mapTemp2 == null) {
            mapTemp2 = new HashMap<String, Symbol>();
        }

        // if the var has not been declared already, add it to the SymbolTable
        // if the var has been declared already, then we have an error
        Symbol idStc = mapTemp2.get(id.image);
        if(idStc == null) {
            Token type = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbol stc = new Symbol(id, type, scope, DataType.Variable);
            mapTemp2.put(id.image, stc);
        } else {
            System.out.println("Variable \"" + id.image + "\" already declared in scope \"" + scope + "\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        }
        SymbolTable.put(scope, mapTemp2);

        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        String foundIn = scope;

        // check if function(id) has been declared
        HashMap <String, Symbol> scopeST = SymbolTable.get(scope);
        if(scopeST == null) {
            scopeST = SymbolTable.get("Program");
            foundIn = "Program";
        }
        Symbol idStc = scopeST.get(id.image);
        if(idStc == null) {
            foundIn = "Program";
            scopeST = SymbolTable.get("Program");
            idStc = scopeST.get(id.image);
        }

        if(idStc != null) {
            // go to ArgList
            if(node.jjtGetNumChildren() > 1) {
                node.jjtGetChild(1).jjtAccept(this, null);
            }
            idStc.setIsCalled(true);
            scopeST.put(id.image, idStc);
            SymbolTable.put(foundIn, scopeST);
        } else {
            System.out.println("Function \"" + id.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        }

        return null;
    }


    @Override
    public Object visit(ASTVariableValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTMinus node, Object data) {
        // check if id has been declared
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        String foundIn = scope;
        String foundIn2 = scope;

        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        Symbol idStc = mapTemp.get(id.image);
        if(idStc == null) {
            mapTemp = SymbolTable.get("Program");
            foundIn = "Program";
            idStc = mapTemp.get(id.image);
        }

        if(idStc == null) {
            // error if still not declared
            System.out.println("Variable \"" + id.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        } else if(idStc.getValues().size() == 0) {
            if(scope.equals("Program") || scope.equals("Main")) {
                // error if var has no value
                System.out.println("Variable \"" + id.image + "\" has no value in scope \"" + scope + "\"");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(!idStc.getType().image.equals("integer")) {
            // var / const has been declared and has value
            // error if var is of type boolean
            System.out.println("Cannot create negative value from " + idStc.getDataType().toString().toLowerCase() + " \"" + id.image + "\". Not of type integer");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            // set value for assignment
            Token parentId = (Token) node.jjtGetParent().jjtGetChild(0).jjtAccept(this, null);
            HashMap<String, Symbol> mapTemp2 = SymbolTable.get(scope);
            Symbol parentStc = mapTemp2.get(id.image);
            if(parentStc == null) {
                mapTemp2 = SymbolTable.get("Program");
                foundIn2 = "Program";
                parentStc = mapTemp2.get(id.image);
            }
            parentStc.addValue(id.image, id);
            SymbolTable.put(foundIn2, mapTemp);

            // set isRead value
            idStc.setIsRead(true);
            mapTemp.put(id.image, idStc);
            SymbolTable.put(foundIn, mapTemp);
        }

        return null;
    }

    @Override
    public Object visit(ASTPos node, Object data) {
        // check if id has been declared
        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);

        String foundIn = scope;

        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        Symbol idStc = mapTemp.get(id.image);
        if(idStc == null) {
            mapTemp = SymbolTable.get("Program");
            foundIn = "Program";
            idStc = mapTemp.get(id.image);
        }

        if(idStc == null) {
            // error if still not declared
            System.out.println("Variable \"" + id.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        } else if(idStc.getValues().size() == 0) {
            if(scope.equals("Program") || scope.equals("Main")) {
                // error if var has no value
                System.out.println("Variable \"" + id.image + "\" has no value in scope \"" + scope + "\"");
                System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(!idStc.getType().image.equals("integer")) {
            // var / const has been declared and has value
            // error if var is of type boolean
            System.out.println("Cannot create positive value from " + idStc.getDataType().toString().toLowerCase() + " \"" + id.image + "\". Not of type integer");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            // set isRead value
            idStc.setIsRead(true);
            mapTemp.put(id.image, idStc);
            SymbolTable.put(foundIn, mapTemp);
        }

        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        Token funcId = (Token) node.jjtGetParent().jjtGetChild(0).jjtAccept(this, null);

        HashMap<String, Symbol> mapTemp = SymbolTable.get("Program");
        if(mapTemp != null) {
            Symbol funcStc = mapTemp.get(funcId.image);
            if(funcStc == null) {
                // error, no such function
                System.out.println("No such function called \"" + funcId.image + "\"");
                System.out.println("Error at line " + funcId.beginLine + ", column " + funcId.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                // check for correct number of Args
                int numArgsDeclared = funcStc.getNumArgs();
                int numArgsPassed = node.jjtGetNumChildren();
                if(numArgsDeclared != numArgsPassed) {
                    // error, incorrect num of args
                    System.out.println("Function \"" + funcId.image + "\" has invalid number of arguments");
                    if(numArgsPassed == 1) {
                        System.out.println("Function called with " + numArgsPassed + " argument, should have " + numArgsDeclared);
                    } else {
                        System.out.println("Function called with " + numArgsPassed + " arguments, should have " + numArgsDeclared);
                    }
                    System.out.println("Error at line " + funcId.beginLine + ", column " + funcId.beginColumn);
                    System.out.println();
                    numErrors++;
                } else if(numArgsDeclared > 0) {
                    int i = 0;
                    for(String key : funcStc.getValues().keySet()) {
                        String type = (String) funcStc.getValues().get(key);
                        Token argi = (Token) node.jjtGetChild(i).jjtGetChild(0).jjtAccept(this, null);
                        Symbol argiStc = mapTemp.get(argi.image);
                        if(argiStc == null) {
                            mapTemp = SymbolTable.get(scope);
                            argiStc = mapTemp.get(argi.image);
                        }
                        if(!argiStc.getType().image.equals(type)) {
                            // error
                            // argument in function call is incorrect type
                            System.out.println("\"" + argi.image + "\" in function call for \"" + funcId.image + "\" is of incorrect type");
                            System.out.println("Error at line " + argi.beginLine + ", column " + argi.beginColumn);
                            System.out.println();
                            numErrors++;
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
        String foundIn = scope;

        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        Symbol argStc = mapTemp.get(arg.image);
        if(argStc == null) {
            mapTemp = SymbolTable.get("Program");
            foundIn = "Program";
            argStc = mapTemp.get(arg.image);
        }

        if(argStc == null) {
            // error
            // no such var or const in this scope with that name
            System.out.println("Variable or Const \"" + arg.image + "\" has not been declared in scope \"" + scope + "\"");
            System.out.println("Error at line " + arg.beginLine + ", column " + arg.beginColumn);
            System.out.println();
            numErrors++;
        } else if(argStc.getValues().size() == 0) {
            if(scope.equals("Program") || scope.equals("Main")) {
                // error
                // there is a var in this scope with that name, but it has no value
                System.out.println("Variable \"" + arg.image + "\" has been declared in scope \"" + scope + "\", but has no value");
                System.out.println("Error at line " + arg.beginLine + ", column " + arg.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else {
            argStc.setIsRead(true);
            mapTemp.put(arg.image, argStc);
            SymbolTable.put(foundIn, mapTemp);
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
        String child1type = "";
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                child1type = child1Stc.getType().image;

                if(child1Stc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
            }
        } else if(node1 instanceof ASTDigit) {
            child1type = "integer";
        } else if(node1 instanceof ASTBoolean) {
            child1type = "boolean";
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals(child1type)) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type " + child1type);
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit && !child1type.equals("integer")) {
            // error if number
            System.out.println("Cannot create equals value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean && !child1type.equals("boolean")) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTNotEqual node, Object data) {
        String child1type = "";
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                child1type = child1Stc.getType().image;

                if(child1Stc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
            }
        } else if(node1 instanceof ASTDigit) {
            child1type = "integer";
        } else if(node1 instanceof ASTBoolean) {
            child1type = "boolean";
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals(child1type)) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type " + child1type);
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit && !child1type.equals("integer")) {
            // error if number
            System.out.println("Cannot create not equals value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean && !child1type.equals("boolean")) {
            // no error
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTLessThan node, Object data) {
        String child1type = "";
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                child1type = child1Stc.getType().image;

                if(child1Stc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
            }
        } else if(node1 instanceof ASTDigit) {
            child1type = "integer";
        } else if(node1 instanceof ASTBoolean) {
            child1type = "boolean";
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals(child1type)) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type " + child1type);
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit && !child1type.equals("integer")) {
            // error if number
            System.out.println("Cannot create less than value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean && !child1type.equals("boolean")) {
            // error if boolean
            System.out.println("Cannot create less than value using \"" + child2.image + "\" (Type boolean)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        }

        // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
        //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
        node.childrenAccept(this, data);

        return null;
    }

    @Override
    public Object visit(ASTLessThanEqual node, Object data) {
        String child1type = "";
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                child1type = child1Stc.getType().image;

                if(child1Stc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
            }
        } else if(node1 instanceof ASTDigit) {
            child1type = "integer";
        } else if(node1 instanceof ASTBoolean) {
            child1type = "boolean";
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals(child1type)) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type " + child1type);
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit && !child1type.equals("integer")) {
            // error if number
            System.out.println("Cannot create less than or equal value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean && !child1type.equals("boolean")) {
            // error if boolean
            System.out.println("Cannot create less than or equal value using \"" + child2.image + "\" (Type boolean)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTGreaterThan node, Object data) {
        String child1type = "";
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                child1type = child1Stc.getType().image;

                if(child1Stc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
            }
        } else if(node1 instanceof ASTDigit) {
            child1type = "integer";
        } else if(node1 instanceof ASTBoolean) {
            child1type = "boolean";
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals(child1type)) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type " + child1type);
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit && !child1type.equals("integer")) {
            // error if number
            System.out.println("Cannot create greater than value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean && !child1type.equals("boolean")) {
            // error if boolean
            System.out.println("Cannot create greater than value using \"" + child2.image + "\" (Type boolean)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTGreaterThanEqual node, Object data) {
        String child1type = "";
        Node node1 = (Node) node.jjtGetChild(0);
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        if(node1 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child1Stc = SymbolTable.get(scope).get(child1.image);
            if(child1Stc == null) {
                child1Stc = SymbolTable.get("Program").get(child1.image);
            }
            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                child1type = child1Stc.getType().image;

                if(child1Stc.getValues().size() == 0) {
                    if(scope.equals("Program") || scope.equals("Main")) {
                        // error if var has no value
                        System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                        System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                        System.out.println();
                        numErrors++;
                    }
                }
            }
        } else if(node1 instanceof ASTDigit) {
            child1type = "integer";
        } else if(node1 instanceof ASTBoolean) {
            child1type = "boolean";
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        Node node2 = (Node) node.jjtGetChild(1);
        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        if(node2 instanceof ASTVariable) {
            // check if var or const has been declared before
            Symbol child2Stc = SymbolTable.get(scope).get(child2.image);
            if(child2Stc == null) {
                child2Stc = SymbolTable.get("Program").get(child2.image);
            }
            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals(child1type)) {
                // error if is not a boolean
                System.out.println(child2Stc.getDataType() + " \"" + child2.image + "\" is not of type " + child1type);
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            }
        } else if(node2 instanceof ASTDigit && !child1type.equals("integer")) {
            // error if number
            System.out.println("Cannot create greater than or equal to value using \"" + child2.image + "\" (Type number)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else if(node2 instanceof ASTBoolean && !child1type.equals("boolean")) {
            // error if boolean
            System.out.println("Cannot create greater than or equal to value using \"" + child2.image + "\" (Type boolean)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            // if child is of type; Equals, NotEquals, LessThan, LessThanOrEqualTo, GreaterThan,
            //    GreaterThanOrEqualTo, OR, AND, Not, Positive or Negative. Then, accept children
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTBoolean node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTAdd node, Object data) {
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Node node1 = node.jjtGetChild(0);

        if(node1 instanceof ASTVariable) {
            String foundIn1 = scope;

            HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
            Symbol child1Stc = mapTemp.get(child1.image);
            if(child1Stc == null) {
                mapTemp = SymbolTable.get("Program");
                foundIn1 = "Program";
                child1Stc = mapTemp.get(child1.image);
            }

            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child1Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child1Stc.getType().image.equals("integer")) {
                // var / const has been declared and has value
                // error if var is of type boolean
                System.out.println("Cannot add variable \"" + child1.image + "\". Not of type Number");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                // set isRead value
                child1Stc.setIsRead(true);
                mapTemp.put(child1.image, child1Stc);
                SymbolTable.put(foundIn1, mapTemp);
            }
        } else if(node1 instanceof ASTDigit) {
            // no error
        } else if(node1 instanceof ASTBoolean) {
            // error if first child node is a boolean
            System.out.println("Cannot add value \"" + child1.image + "\" (type boolean)");
            System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            node.childrenAccept(this, data);
        }

        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        Node node2 = node.jjtGetChild(1);

        if(node2 instanceof ASTVariable) {
            String foundIn2 = scope;

            HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
            Symbol child2Stc = mapTemp.get(child1.image);
            if(child2Stc == null) {
                mapTemp = SymbolTable.get("Program");
                foundIn2 = "Program";
                child2Stc = mapTemp.get(child1.image);
            }

            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals("integer")) {
                // var / const has been declared and has value
                // error if var is of type boolean
                System.out.println("Cannot add variable \"" + child2.image + "\". Not of type Number");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                // set isRead value
                child2Stc.setIsRead(true);
                mapTemp.put(child2.image, child2Stc);
                SymbolTable.put(foundIn2, mapTemp);
            }
        } else if(node2 instanceof ASTDigit) {
            // no error
        } else if(node2 instanceof ASTBoolean) {
            // error if second child node is a boolean
            System.out.println("Cannot add value \"" + child2.image + "\" (type boolean)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        }

        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTSubtract node, Object data) {
        Token child1 = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Node node1 = node.jjtGetChild(0);

        if(node1 instanceof ASTVariable) {
            String foundIn1 = scope;

            HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
            Symbol child1Stc = mapTemp.get(child1.image);
            if(child1Stc == null) {
                mapTemp = SymbolTable.get("Program");
                foundIn1 = "Program";
                child1Stc = mapTemp.get(child1.image);
            }

            if(child1Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child1.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child1Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child1.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child1Stc.getType().image.equals("integer")) {
                // var / const has been declared and has value
                // error if var is of type boolean
                System.out.println("Cannot subtract variable \"" + child1.image + "\". Not of type Number");
                System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                // set isRead value
                child1Stc.setIsRead(true);
                mapTemp.put(child1.image, child1Stc);
                SymbolTable.put(foundIn1, mapTemp);
            }
        } else if(node1 instanceof ASTVariable) {
            // no error
        } else if(node1 instanceof ASTBoolean) {
            // error if first child node is a boolean
            System.out.println("Cannot subtract value \"" + child1.image + "\" (type boolean)");
            System.out.println("Error at line " + child1.beginLine + ", column " + child1.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            node.childrenAccept(this, data);
        }

        Token child2 = (Token) node.jjtGetChild(1).jjtAccept(this, null);
        Node node2 = node.jjtGetChild(1);

        if(node2 instanceof ASTVariable) {
            String foundIn2 = scope;

            HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
            Symbol child2Stc = mapTemp.get(child1.image);
            if(child2Stc == null) {
                mapTemp = SymbolTable.get("Program");
                foundIn2 = "Program";
                child2Stc = mapTemp.get(child1.image);
            }

            if(child2Stc == null) {
                // error if var or const has not been declared before
                System.out.println("Variable or Const \"" + child2.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else if(child2Stc.getValues().size() == 0) {
                if(scope.equals("Program") || scope.equals("Main")) {
                    // error if var has no value
                    System.out.println("Variable \"" + child2.image + "\" has no value in scope \"" + scope + "\"");
                    System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                    System.out.println();
                    numErrors++;
                }
            } else if(!child2Stc.getType().image.equals("integer")) {
                // var / const has been declared and has value
                // error if var is of type boolean
                System.out.println("Cannot subtract variable \"" + child2.image + "\". Not of type Number");
                System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
                System.out.println();
                numErrors++;
            } else {
                // set isRead value
                child2Stc.setIsRead(true);
                mapTemp.put(child2.image, child2Stc);
                SymbolTable.put(foundIn2, mapTemp);
            }
        } else if(node2 instanceof ASTVariable) {
            // no error
        } else if(node2 instanceof ASTBoolean) {
            // error if second child node is a boolean
            System.out.println("Cannot subtract value \"" + child2.image + "\" (type boolean)");
            System.out.println("Error at line " + child2.beginLine + ", column " + child2.beginColumn);
            System.out.println();
            numErrors++;
        } else {
            node.childrenAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        HashMap<String, Symbol> mapTemp = SymbolTable.get(scope);
        if(mapTemp == null) {
            mapTemp = new HashMap<String, Symbol>();
        }

        Token id = (Token) node.jjtGetChild(0).jjtAccept(this, null);

        // get stc from being declared in this scope
        Symbol idStc = mapTemp.get(id.image);
        // if not declared here, check if delcared in Program scope (Global variable)
        if(idStc == null) {
            idStc = SymbolTable.get("Program").get(id.image);
        }

        if(idStc != null) {
            Token value = (Token) node.jjtGetChild(1).jjtGetChild(0).jjtAccept(this, null);
            Node valueNode = node.jjtGetChild(1).jjtGetChild(0);

            if(valueNode instanceof ASTVariable) {
                Symbol valueStc = SymbolTable.get(scope).get(value.image);
                if(valueStc == null) {
                    valueStc = SymbolTable.get("Program").get(value.image);
                }
                if(valueStc == null) {
                    // error
                    // valueStc is not declared
                    System.out.println(idStc.getDataType() + " \"" + value.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
                    System.out.println("Error at line " + value.beginLine + ", column " + value.beginColumn);
                    System.out.println();
                    numErrors++;
                } else if(!idStc.getType().image.equals(valueStc.getType().image)) {
                    // error
                    // id and value not of same type
                    System.out.println("\"" + id.image + "\" and \"" + value.image + "\" are not of same type");
                    System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                    System.out.println();
                    numErrors++;
                } else if(idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    idStc.addValue(id.image, value);
                    mapTemp.put(id.image, idStc);
                    SymbolTable.put(scope, mapTemp);
                } else {
                    valueStc.setIsRead(true);
                    mapTemp.put(value.image, valueStc);
                    SymbolTable.put(scope, mapTemp);
                }
            } else if(valueNode instanceof ASTDigit) {
                if(!idStc.getType().image.equals("integer")) {
                    // error
                    // if attempting to assign number to id not of type number
                    System.out.println("Cannot assign type number to \"" + id.image + "\"");
                    System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                    System.out.println();
                    numErrors++;
                } else if(idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    idStc.addValue(id.image, value);
                    mapTemp.put(id.image, idStc);
                    SymbolTable.put(scope, mapTemp);
                }
            } else if(valueNode instanceof ASTBoolean) {
                if(!idStc.getType().image.equals("boolean")) {
                    // error
                    // if attempting to assign boolean to id not of type boolean
                    System.out.println("Cannot assign type boolean to \"" + id.image + "\"");
                    System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
                    System.out.println();
                    numErrors++;
                } else if(idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    idStc.addValue(id.image, value);
                    mapTemp.put(id.image, idStc);
                    SymbolTable.put(scope, mapTemp);
                }
            } else if(valueNode instanceof ASTFunctionCall ||
                    valueNode instanceof ASTPositive ||
                    valueNode instanceof ASTNegative) {
                Token subChild = (Token) valueNode.jjtGetChild(0).jjtAccept(this, null);
                if(idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    idStc.addValue(subChild.image, subChild);
                    mapTemp.put(id.image, idStc);
                    SymbolTable.put(scope, mapTemp);
                }
            } else if(valueNode instanceof ASTAdd ||
                    valueNode instanceof ASTSubtract) {
                Token subChild1 = (Token) valueNode.jjtGetChild(0).jjtAccept(this, null);
                Token subChild2 = (Token) valueNode.jjtGetChild(1).jjtAccept(this, null);
                if(idStc.getValues().size() == 0) {
                    // if its initial value has already been stored, leave it
                    idStc.addValue(subChild1.image, subChild1);
                    idStc.addValue(subChild2.image, subChild2);
                    mapTemp.put(id.image, idStc);
                    SymbolTable.put(scope, mapTemp);
                }
            } else {
                node.childrenAccept(this, data);
            }
        } else {
            // error
            // still not declared
            System.out.println("Variable \"" + id.image + "\" not declared in scope \"" + scope + "\" or \"Program\"");
            System.out.println("Error at line " + id.beginLine + ", column " + id.beginColumn);
            System.out.println();
            numErrors++;
        }




        return null;
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

}