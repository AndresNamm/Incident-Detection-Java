package bgt.IncidentDetection.Models;

/**
 * Created by admin on 2016/12/2.
 */
public class SegDistribution {
    public static int NUM_MIXTURE;  // number of traffic states
    public static double[] lambdas; // parameter of each state distribution
    public static double[][] deltas; // the pair-wise KL distance between states

    public String seg_id;   // segment id
    public double[] pis; // weights for each state distribution
    public double[] sigmas; // prepared for weighted KL divergence

    public SegDistribution(String seg_id, double[] pis, double[] sigmas){
        this.seg_id = seg_id;
        this.pis = pis;
        this.sigmas = sigmas;
    }
}
