package AEP.nodeUtilities;

import java.io.Serializable;

/**
 * Created by Francesco on 24/08/17.
 */
public class Couple implements Cloneable, Serializable {
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

    public Object clone() throws CloneNotSupportedException {
        Couple clone = new Couple (getValue(), getVersion());
        return clone;
    }

    public String toString() {
        return "(" + getValue() + " , " + getVersion() + ")";
    }

    public void updateCouple(Couple couple) {
        this.value = couple.getValue();
        this.version = couple.getVersion();
    }
}
