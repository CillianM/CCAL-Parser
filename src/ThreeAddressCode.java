public class ThreeAddressCode extends Object {
    private String address1 = "";
    private String address2 = "";
    private String address3 = "";
    private String address4 = "";

    ThreeAddressCode() {}

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    public String toString() {
        return address1 + " " + address2 + " " + address3 + " " + address4;
    }
}
