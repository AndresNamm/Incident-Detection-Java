    package bgt.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by admin on 2016/11/26.
 */
public class Segment {
    public String seg_id;
    public long way_id;
    public int inner_id;
    public Node startNode;
    public Node endNode;
    public double direction;
    public int startHour;
    public String speedPanelId;
    public List<Record> recordList;
    public List<Accident> accList;

    public Segment(long way_id, int inner_id, Node startNode, Node endNode){
        this.way_id = way_id;
        this.inner_id = inner_id;
        this.startHour = 0; // THis shit is alway 0
        calcSeg_id();
        this.startNode = startNode;
        this.endNode = endNode;
        calcDirection();
        this.recordList = new ArrayList<>();
        this.accList = new ArrayList<>();
    }

    public Segment(Segment seg, int startHour){
        this.way_id = seg.way_id;
        this.inner_id = seg.inner_id;
        this.startHour = startHour;
        calcSeg_id();
        this.startNode = seg.startNode;
        this.endNode = seg.endNode;
        this.direction = seg.direction;
        this.recordList = new ArrayList<>();
        this.accList = new ArrayList<>();
    }

    public void calcSeg_id() {
        this.seg_id = way_id +"_"+inner_id+"_"+startHour; // generate segment id
    }

    public String getSeg_id() {
        return seg_id;
    }

    public Node getStartNode() {
        return startNode;
    }

    public Node getEndNode() {
        return endNode;
    }

    public static double convertToRad(double degree){
        return degree/180.0*Math.PI;
    }

    public void calcDirection(){
        // calculate the azimuth of the segment
        double s_lat = convertToRad(startNode.getLat());
        double s_lon = convertToRad(startNode.getLon());
        double e_lat = convertToRad(endNode.getLat());
        double e_lon = convertToRad(endNode.getLon());

        double direction = Math.sin(s_lat)*Math.sin(e_lat) +
                Math.cos(s_lat)*Math.cos(e_lat)*Math.cos(e_lon-s_lon);
        direction = Math.sqrt(1-direction*direction);
        direction = Math.cos(e_lat)*Math.sin(e_lon-s_lon)/direction;
        direction = Math.asin(direction)*180/Math.PI;

        if (e_lon > s_lon && e_lat > s_lat){
            // in 1st quadrant
            this.direction = direction;
        }else if (e_lon < s_lon && e_lat > s_lat){
            // in 2nd quadrant
            this.direction = 360 + direction;
        }else if (e_lat < s_lat ){
            // in 3rd or 4th quadrant
            this.direction = 180 - direction;
        }else {
            this.direction = direction;
        }
        //System.out.println(this.direction);
        return;
    }

    public double calcRecordDirectDist(Record r){
        // calculate the direction difference between record and segment
        return Math.abs(this.direction - r.direction);
    }

    public double calcNodeDist(Node n){
        double a = Node.calcDist(startNode, endNode); // segment length
        double b = Node.calcDist(startNode, n);
        double c = Node.calcDist(endNode, n);

        if (a == b+c){
            // the record is located on the segment
            return 0;
        }

        if (c*c >= a*a + b*b){
            // right triangle or obtuse triangle, with c being longest edge
            return b;
        }

        if (b*b >= a*a + c*c){
            // right triangle or obtuse triangle, with b being longest edge
            return c;
        }

        // if not above, then we calculate the height of the acute triangle
        double s = (a+b+c) / 2; // half of perimeter
        double A = Math.sqrt(s*(s-a)*(s-b)*(s-c));
        double h = 2*A/a;
        return h;
    }

    public void addRecord(Record record) {
        this.recordList.add(record);
    }

    public void addAcc(Accident acc) {this.accList.add(acc);}

    public Segment[] splitSegByHour(){
        Segment[] segsByHour = new Segment[24];
        for (int i=0; i<24; i++){
            segsByHour[i] = new Segment(this, i);
        }
        Iterator<Record> recItr = this.recordList.iterator();
        while (recItr.hasNext()){
            Record rec = recItr.next();
            int rec_hour = (int)rec.time;
            segsByHour[rec_hour].addRecord(rec);
        }
        return segsByHour;
    }

    public int recordNum(){
        return this.recordList.size();
    }

    public int accNum() {return this.accList.size();}

    public Way toWay(String name,String otherName, Long id){
        List<Node> ndList = new ArrayList<>();
        ndList.add(startNode);
        ndList.add(endNode);
        Way newWay = new Way(id,ndList);
        newWay.name=name;
        newWay.group=otherName;
        return newWay;
    }

    public Way toWay(String name, Long id){
        List<Node> ndList = new ArrayList<>();
        ndList.add(startNode);
        ndList.add(endNode);
        Way newWay = new Way(id,ndList);
        newWay.name=name;
        return newWay;
    }
}
