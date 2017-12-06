import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Representer implements CCALParserVisitor {

    private String currentLable = "L0";
    private String previousLable;
    private int labelCount = 0;
    private HashMap<String, ArrayList<ThreeAddressCode>> addrCode = new HashMap<>();
    private HashMap<String, String> jumpLables = new HashMap<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        System.out.println("---- 3-address code representation ----");
        node.childrenAccept(this, data);

        Set keys = addrCode.keySet();
        if(keys.size() > 0) {
            for (Object key : keys) {
                String s = (String) key;
                ArrayList<ThreeAddressCode> a = addrCode.get(s);
                System.out.println(s);
                for (ThreeAddressCode threeAddressCode : a) {
                    System.out.println(" " + threeAddressCode.toString());
                }
            }
        } else {
            System.out.println("Nothing declared");
        }
        System.out.println("---- End 3-address code representation ----");

        return null;
    }

    @Override
    public Object visit(ASTMain node, Object data) {
        previousLable = currentLable;
        currentLable = "L" + (labelCount + 1);

        node.childrenAccept(this, data);

        currentLable = previousLable;
        labelCount++;

        return null;
    }

    @Override
    public Object visit(ASTFunctionList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTFunction node, Object data) {
        previousLable = currentLable;
        currentLable = "L" + (labelCount + 1);

        jumpLables.put((String) node.jjtGetChild(1).jjtAccept(this, null), currentLable);

        node.childrenAccept(this, data);

        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode returnAddressCode = new ThreeAddressCode();
        returnAddressCode.setAddress1("return");

        currentAddressCodes.add(returnAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        currentLable = previousLable;
        labelCount++;

        return null;
    }

    @Override
    public Object visit(ASTFunctionBody node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTReturnStatement node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTParamList node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTParam node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTDeclarationList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTConstDeclaration node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if (currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.setAddress1("=");
        ac.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        ac.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());
        ac.setAddress4(node.jjtGetChild(2).jjtAccept(this, null).toString());
        currentAddressCodes.add(ac);
        addrCode.put(currentLable, currentAddressCodes);
        return null;
    }

    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTStatementBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCondition node, Object data) {
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
    public Object visit(ASTSkip node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode skipAddressCode = new ThreeAddressCode();
        skipAddressCode.setAddress1("Skip");

        currentAddressCodes.add(skipAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTNot node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode notAddressCode = new ThreeAddressCode();
        notAddressCode.setAddress1("~" + getBooleanOperator(node.jjtGetChild(0)));
        notAddressCode.setAddress2(node.jjtGetChild(0).jjtGetChild(0).jjtAccept(this, null).toString());
        notAddressCode.setAddress3(node.jjtGetChild(0).jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(notAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode functionCallAddressCode = new ThreeAddressCode();
        functionCallAddressCode.setAddress1("functionCall");
        functionCallAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        if(node.jjtGetNumChildren() > 1) {
            functionCallAddressCode.setAddress3(node.jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, null).toString());
        }
        currentAddressCodes.add(functionCallAddressCode);

        ThreeAddressCode gotoAddressCode = new ThreeAddressCode();
        gotoAddressCode.setAddress1("goto");
        gotoAddressCode.setAddress2(jumpLables.get(node.jjtGetChild(0).jjtAccept(this,null)));

        currentAddressCodes.add(gotoAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTMinus node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTArg node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTVariable node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTDigit node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTTypeValue node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTEqual node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode equalAddressCode = new ThreeAddressCode();
        equalAddressCode.setAddress1("==");
        equalAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        equalAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(equalAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTNotEqual node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode notEqualAddressCode = new ThreeAddressCode();
        notEqualAddressCode.setAddress1("!=");
        notEqualAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        notEqualAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(notEqualAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTLessThan node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode lessThanAddressCode = new ThreeAddressCode();
        lessThanAddressCode.setAddress1("<");
        lessThanAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        lessThanAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(lessThanAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTLessThanEqual node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode lessThanEqualAddressCode = new ThreeAddressCode();
        lessThanEqualAddressCode.setAddress1("<=");
        lessThanEqualAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        lessThanEqualAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(lessThanEqualAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTGreaterThan node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode greaterThanAddressCode = new ThreeAddressCode();
        greaterThanAddressCode.setAddress1(">");
        greaterThanAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        greaterThanAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(greaterThanAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTGreaterThanEqual node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode greaterThanEqualAddressCode = new ThreeAddressCode();
        greaterThanEqualAddressCode.setAddress1(">=");
        greaterThanEqualAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        greaterThanEqualAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(greaterThanEqualAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTOr node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode orAddressCode = new ThreeAddressCode();
        orAddressCode.setAddress1("||");
        orAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        orAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(orAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode andAddressCode = new ThreeAddressCode();
        andAddressCode.setAddress1("&&");
        andAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        andAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(andAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTBoolean node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTAdd node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode addAddressCode = new ThreeAddressCode();
        addAddressCode.setAddress1("+");
        addAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        addAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(addAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTSubtract node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode subtractAddressCode = new ThreeAddressCode();
        subtractAddressCode.setAddress1("-");
        subtractAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());
        subtractAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());

        currentAddressCodes.add(subtractAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        ArrayList<ThreeAddressCode> currentAddressCodes = addrCode.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        ThreeAddressCode asignAddressCode = new ThreeAddressCode();
        asignAddressCode.setAddress1("=");
        asignAddressCode.setAddress2(node.jjtGetChild(0).jjtAccept(this, null).toString());

        if(node.jjtGetChild(1) instanceof ASTFunctionCall){
            asignAddressCode.setAddress3(node.jjtGetChild(1).jjtGetChild(0).jjtAccept(this, null).toString());
        } else if(node.jjtGetChild(1) instanceof ASTSubtract){ //we need a quadruple to represent this operation and what it stores into
            asignAddressCode.setAddress4(node.jjtGetChild(0).jjtAccept(this, null).toString());
            asignAddressCode.setAddress1("-");
            asignAddressCode.setAddress2(node.jjtGetChild(1).jjtGetChild(0).jjtAccept(this, null).toString());
            asignAddressCode.setAddress3(node.jjtGetChild(1).jjtGetChild(1).jjtAccept(this, null).toString());
        } else if(node.jjtGetChild(1) instanceof ASTAdd ){
            asignAddressCode.setAddress4(node.jjtGetChild(0).jjtAccept(this, null).toString());
            asignAddressCode.setAddress1("+");
            asignAddressCode.setAddress2(node.jjtGetChild(1).jjtGetChild(0).jjtAccept(this, null).toString());
            asignAddressCode.setAddress3(node.jjtGetChild(1).jjtGetChild(1).jjtAccept(this, null).toString());
        }else{
            asignAddressCode.setAddress3(node.jjtGetChild(1).jjtAccept(this, null).toString());
        }

        currentAddressCodes.add(asignAddressCode);
        addrCode.put(currentLable, currentAddressCodes);

        return null;
    }

    //Get boolean operators for the notAddressCode
    private String getBooleanOperator(Node node){
        if (node instanceof ASTAnd) {
            return "&&";
        } else if (node instanceof ASTOr) {
            return "||";
        } else if (node instanceof ASTGreaterThan) {
            return ">";
        } else if (node instanceof ASTGreaterThanEqual) {
            return ">=";
        } else if (node instanceof ASTLessThan) {
            return "<";
        } else if (node instanceof ASTLessThanEqual) {
            return "<=";
        } else if (node instanceof ASTEqual) {
            return "==";
        } else if (node instanceof ASTNotEqual) {
            return "!=";
        } else if (node instanceof ASTNot) {
            return "~";
        }
        return "";
    }
}
