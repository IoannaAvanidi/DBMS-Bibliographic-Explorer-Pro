package dbms.models;

public class ChartPoint {
    public Object label; // x-axis
    public Number value; // y-axis
    public Object secondaryValue; // for scatter plots (x-axis vs y-axis) or second metric
    public Object tertiaryValue;  // for third metric
    public Object quaternaryValue; // for fourth metric

    public ChartPoint(Object label, Number value) {
        this.label = label;
        this.value = value;
    }

    public ChartPoint(Object label, Number value, Object secondaryValue) {
        this.label = label;
        this.value = value;
        this.secondaryValue = secondaryValue;
    }

    public ChartPoint(Object label, Number value, Object secondaryValue, Object tertiaryValue) {
        this.label = label;
        this.value = value;
        this.secondaryValue = secondaryValue;
        this.tertiaryValue = tertiaryValue;
    }

    public ChartPoint() {}
}
