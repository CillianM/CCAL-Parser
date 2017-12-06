public class ThreeAddressCode extends Object {
    public String addr1 = "";
    public String addr2 = "";
    public String addr3 = "";
    public String addr4 = "";

    public ThreeAddressCode() {}

    public ThreeAddressCode(String a0) {
        addr1 = a0;
    }

    public ThreeAddressCode(String a0, String b0) {
        addr1 = a0;
        addr2 = b0;
    }

    public ThreeAddressCode(String a0, String b0, String c0) {
        addr1 = a0;
        addr2 = b0;
        addr3 = c0;
    }

    public ThreeAddressCode(String a0, String b0, String c0, String d0) {
        addr1 = a0;
        addr2 = b0;
        addr3 = c0;
        addr4 = d0;
    }

    public String toString() {
        return addr1 + " " + addr2 + " " + addr3 + " " + addr4;
    }
}
