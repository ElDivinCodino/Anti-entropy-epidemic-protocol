package AEP.nodeUtilities;

/**
 * Created by Francesco on 24/08/17.
 */
public class Couple {
    private String value;
    private long version;

    public Couple(String value, long version) {
        this.value = value;
        this.version = version;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getValue() {
        return value;
    }

    public long getVersion() {
        return version;
    }

    public String toString() {
        return "(" + getValue() + " , " + getVersion() + ")";
    }
}
