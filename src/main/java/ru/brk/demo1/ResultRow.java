package ru.brk.demo1;

public class ResultRow {
    private int rodId;
    private double displacementStart, displacementEnd, forceStart, forceEnd;
    private double stressStart, stressEnd, allowableStress;
    private boolean checkStart, checkEnd;

    public ResultRow(int rodId, double displacementStart, double displacementEnd,
                     double forceStart, double forceEnd, double stressStart, double stressEnd,
                     double allowableStress, boolean checkStart, boolean checkEnd) {
        this.rodId = rodId;
        this.displacementStart = displacementStart;
        this.displacementEnd = displacementEnd;
        this.forceStart = forceStart;
        this.forceEnd = forceEnd;
        this.stressStart = stressStart;
        this.stressEnd = stressEnd;
        this.allowableStress = allowableStress;
        this.checkStart = checkStart;
        this.checkEnd = checkEnd;
    }

    @Override
    public String toString() {
        return "ResultRow{" +
                "displacementStart=" + displacementStart +
                ", displacementEnd=" + displacementEnd +
                ", forceStart=" + forceStart +
                ", forceEnd=" + forceEnd +
                ", stressStart=" + stressStart +
                ", stressEnd=" + stressEnd +
                ", allowableStress=" + allowableStress +
                ", checkStart=" + checkStart +
                ", checkEnd=" + checkEnd +
                '}';
    }

    // Геттеры для отображения в TableView
    public int getRodId() { return rodId; }
    public double getDisplacementStart() { return displacementStart; }
    public double getDisplacementEnd() { return displacementEnd; }
    public double getForceStart() { return forceStart; }
    public double getForceEnd() { return forceEnd; }
    public double getStressStart() { return stressStart; }
    public double getStressEnd() { return stressEnd; }
    public double getAllowableStress() { return allowableStress; }
    public boolean isCheckStart() { return checkStart; }
    public boolean isCheckEnd() { return checkEnd; }
}