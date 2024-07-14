package ch.javacamp.metrics.analyzer.test;

public class TestClassA {

    private String s;
    private int i;
    private Long l;

    public void setS() {
        this.s = "aS";
    }

    public void setI() {
        this.i = 15;
    }

    public void setL() {
        this.l = 16L;
    }

    public void foo() {
        this.l = 14L;
        this.i = 15;
    }

    public String getDelegatedS() {
        return getS();
    }

    public Object bar(String x, int i, int j, String s) {
        this.i = i;
        this.s = s;
        return null;
    }

    public String getS() {
        return s;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", getS(), i, l);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
