package bgt.IncidentDetection.Models;

/**
 * Created by admin on 2016/12/2.
 */
public class EvaluateResult {
    public double anomaly_val;
    public double anomaly_val_weighted;
    public int acc_label;

    public EvaluateResult(double anomaly_val, double anomaly_val_weighted, int acc_label){
        this.anomaly_val = anomaly_val;
        this.anomaly_val_weighted = anomaly_val_weighted;
        this.acc_label = acc_label;
    }
}
