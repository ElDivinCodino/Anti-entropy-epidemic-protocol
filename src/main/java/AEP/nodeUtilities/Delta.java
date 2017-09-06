package AEP.nodeUtilities;

import java.io.Serializable;

/**
 * Created by StefanoFiora on 28/08/2017.
 */
public class Delta implements Serializable{

    private int p;
    private int k;
    private String v;
    private long n;

    /**
     * @param value the v of the key
     * @param version last n of the key
     */
    public Delta(int process, int key, String value, long version) {
        this.v = value;
        this.n = version;
        this.p = process;
        this.k = key;
    }

    /**
     *
     * @param v the new v that has to be assigned to the key
     */
    public void setV(String v) {
        this.v = v;
    }

    /**
     *
     * @param n the new n that has to be assigned to the key
     */
    public void setN(long n) {
        this.n = n;
    }

    /**
     *
     * @return the v of the key currently stored
     */
    public String getV() {
        return v;
    }

    /**
     *
     * @return the n of the key currently stored
     */
    public long getN() {
        return n;
    }

    public int getP() {
        return p;
    }

    public int getK() {
        return k;
    }

    @Override
    public String toString() {
        return "Delta{" +
                "p=" + p +
                ", k=" + k +
                ", v='" + v + '\'' +
                ", n=" + n +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Delta delta = (Delta) o;

        if (p != delta.p) return false;
        if (k != delta.k) return false;
        if (n != delta.n) return false;
        return v.equals(delta.v);
    }

    @Override
    public int hashCode() {
        int result = p;
        result = 31 * result + k;
        result = 31 * result + v.hashCode();
        result = 31 * result + (int) (n ^ (n >>> 32));
        return result;
    }
}
