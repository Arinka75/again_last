package ru.brk.demo1;

public class Load {
    int point;
    double power;

    public Load(int point, double power) {
        this.point = point;
        this.power = power;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }
}