package ru.brk.demo1;

public class Rod {
    double L, A, E, stress;

    public Rod(double length, double area, double elasticity, double allowableStress) {
        this.L = length;
        this.A = area;
        this.E = elasticity;
        this.stress = allowableStress;
    }

    public double getL() {
        return L;
    }

    public void setL(double l) {
        L = l;
    }

    public double getA() {
        return A;
    }

    public void setA(double a) {
        A = a;
    }

    public double getE() {
        return E;
    }

    public void setE(double e) {
        E = e;
    }

    public double getStress() {
        return stress;
    }

    public void setStress(double stress) {
        this.stress = stress;
    }
}