package pt.estga.processing.models;

/** Mutable per-key state for compensated summation (Neumaier variant). */
public final class KahanState {
    private double sum;
    private double comp;

    public KahanState() {}

    /** Add a value into this state using Neumaier's compensated summation. */
    public void add(double value) {
        double t = sum + value;
        if (Math.abs(sum) >= Math.abs(value)) {
            comp += (sum - t) + value;
        } else {
            comp += (value - t) + sum;
        }
        sum = t;
    }

    /** Returns the corrected total (sum + compensation). */
    public double value() {
        return sum + comp;
    }

    /** Expose raw sum (for diagnostics). */
    public double rawSum() { return sum; }

    /** Expose compensation (for diagnostics). */
    public double compensation() { return comp; }
}