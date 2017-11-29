public class Symbol extends Object{
    String type;
    String value;


    public Symbol(String type, String value){
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString(){
        return "{Type: " + type + " Value: " + value + "}";
    }


}
