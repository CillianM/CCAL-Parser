import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Representer implements CCALParserVisitor {

    String label = "L0";
    String prevLabel;
    int curTempCount = 0;
    int paramCount = 0;
    int labelCount = 0;
    HashMap<String, ArrayList<ThreeAddressCode>> addrCode = new HashMap<>();
    HashMap<String, String> jumpLabelMap = new HashMap<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        System.out.println("***************************");

        System.out.println();

        System.out.println("**** IR using 3-address code ****");
        node.childrenAccept(this, data);

        Set keys = addrCode.keySet();
        if(keys.size() > 0) {
            Iterator iter = keys.iterator();
            while(iter.hasNext()) {
                String s = (String) iter.next();
                ArrayList<ThreeAddressCode> a = addrCode.get(s);
                System.out.println(s);
                for(int i=0; i<a.size(); i++) {
                    System.out.println(" " + a.get(i).toString());
                }
            }
        } else {
            System.out.println("Nothing declared");
        }
        System.out.println("*********************************");

        return null;
    }

    @Override
    public Object visit(ASTMain node, Object data) {
        prevLabel = label;
        label = "L" + (labelCount + 1);

        node.childrenAccept(this, data);

        label = prevLabel;
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
        prevLabel = label;
        label = "L" + (labelCount + 1);

        jumpLabelMap.put((String) node.jjtGetChild(1).jjtAccept(this, null), label);

        node.childrenAccept(this, data);

        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ret = new ThreeAddressCode();
        ret.addr1 = "return";

        allAc.add(ret);
        addrCode.put(label, allAc);

        label = prevLabel;
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
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if (allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "=";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);
        ac.addr4 = (String) node.jjtGetChild(2).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

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
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "Skip";

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTNot node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "~" + getBooleanOperator(node.jjtGetChild(0));
        ac.addr2 = (String) node.jjtGetChild(0).jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(0).jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac1 = new ThreeAddressCode();
        ac1.addr1 = "funcCall";
        ac1.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        if(node.jjtGetNumChildren() > 1) {
            ac1.addr3 = (String) node.jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, null);
        }
        allAc.add(ac1);

        ThreeAddressCode ac2 = new ThreeAddressCode();
        ac2.addr1 = "goto";
        ac2.addr2 = jumpLabelMap.get((String) node.jjtGetChild(0).jjtAccept(this,null));

        allAc.add(ac2);
        addrCode.put(label, allAc);

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
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "==";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTNotEqual node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "!=";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTLessThan node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "<";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTLessThanEqual node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "<=";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTGreaterThan node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = ">";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTGreaterThanEqual node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = ">=";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTOr node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "||";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "&&";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTBoolean node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTAdd node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "+";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTSubtract node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "-";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        ArrayList<ThreeAddressCode> allAc = addrCode.get(label);
        if(allAc == null) {
            allAc = new ArrayList<>();
        }

        ThreeAddressCode ac = new ThreeAddressCode();
        ac.addr1 = "=";
        ac.addr2 = (String) node.jjtGetChild(0).jjtAccept(this, null);
        ac.addr3 = (String) node.jjtGetChild(1).jjtAccept(this, null);
        if(node.jjtGetChild(1) instanceof ASTFunctionCall){
            ac.addr3 = (String) node.jjtGetChild(1).jjtGetChild(0).jjtAccept(this, null);
        }

        allAc.add(ac);
        addrCode.put(label, allAc);

        return null;
    }

    public String getBooleanOperator(Node node){
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
