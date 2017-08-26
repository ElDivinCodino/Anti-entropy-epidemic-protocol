package AEP.nodeUtilities;

import java.io.Serializable;

/**
 * Created by Francesco on 24/08/17.
 */
public class Couple implements Cloneable, Serializable {
    private String value;
    private long version;

    /**
     * @param value the value of the key
     * @param version last version of the key
     */
    public Couple(String value, long version) {
        this.value = value;
        this.version = version;
    }

    /**
     *
     * @param value the new value that has to be assigned to the key
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *
     * @param version the new version that has to be assigned to the key
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     *
     * @return the value of the key currently stored
     */
    public String getValue() {
        return value;
    }

    /**
     *
     * @return the version of the key currently stored
     */
    public long getVersion() {
        return version;
    }

    /**
     * @return a copy of the Couple object
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {
        Couple clone = new Couple (getValue(), getVersion());
        return clone;
    }

    /**
     *
     * @return a fancy way to represent the couple
     */
    public String toString() {
        return "(" + getValue() + " , " + getVersion() + ")";
    }

    /**
     *
     * @param couple the new Couple for the proper key
     */
    public void updateCouple(Couple couple) {
        this.value = couple.getValue();
        this.version = couple.getVersion();
    }
}
