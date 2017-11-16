package bgt.Model;

import bgt.Model.Node;

/**
 * Created by admin on 2016/12/1.
 */
public class Accident {
    public int acc_no;
    public int severity;
    public int month;
    public int day;
    public int inner_id;
    public double acc_time;
    public Node acc_location;



    public Accident(int acc_no, int severity, int month, int day,
                    double acc_time, int inner_id){
        this.acc_no = acc_no;
        this.severity = severity;
        this.month = month;
        this.day = day;
        this.acc_time = acc_time;
        this.inner_id = inner_id;
    }

    public Accident(int acc_no, int severity, int month, int day,
                    double acc_time, Node acc_location){
        this.acc_no = acc_no;
        this.severity = severity;
        this.month = month;
        this.day = day;
        this.acc_time = acc_time;
        this.acc_location = acc_location;
    }

    public Node getAcc_location() {
        return acc_location;
    }
}
