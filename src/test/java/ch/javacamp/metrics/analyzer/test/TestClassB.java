package ch.javacamp.metrics.analyzer.test;

public class TestClassB {

    private int a,b,c,d,e;

    public void f1(){
        a = 1;
        b = 2;
    }

    public void f2(){
        c = 1;
        d = 2;
    }

    public void f3(){
        e = 13;
        a = 14;
    }

    public void f4(){
        f2();
    }

    @Override
    public String toString() {
        return super.toString();
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
