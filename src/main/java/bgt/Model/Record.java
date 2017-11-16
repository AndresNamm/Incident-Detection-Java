package bgt.Model;

import bgt.LabelData.LabelData;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.Random;

/**
 * Created by admin on 2016/11/26.
 */
public class Record implements Comparable{
    public static int toNodeCnt =0;
    public long rec_id;
    public int dev_id;
    public int month;
    public int day;
    public double time;
    public Node location;
    public double speed; // in km/h
    public double direction;
    public String seg_id;
    public int acc_flag; // whether encounter an accident or not

    public Record(long rec_id, int dev_id, int month, int day, double time,
                  Node location, double speed, double direction){
        this.rec_id = rec_id;
        this.dev_id = dev_id;
        this.month = month;
        this.day = day;
        this.time = time;
        this.location = location;
        this.speed = speed;
        this.direction = direction;
    }

    public Record(long rec_id, int dev_id, int month, int day, double time,
                  Node location, double speed, double direction, String seg_id){

        this(rec_id,dev_id,month,day,time,location,speed,direction);
        this.seg_id=seg_id;
    }

    public Record(long rec_id, int dev_id, int month, int day, double time,
                  double loc_lat, double loc_lon, double speed, double direction, String seg_id){
        this(rec_id, dev_id, month, day, time, null, speed, direction);
        this.location = new Node(loc_lat, loc_lon);
        this.seg_id = seg_id;
        this.acc_flag = 0;
    }

    public Record(int dev_id, int month, int day, double time,  double speed, String seg_id){
        this(0, dev_id, month, day, time, null, speed, 0);
        this.seg_id = seg_id;
    }

    public Node getLocation() {
        return location;
    }

    public double getDirection() {
        return direction;
    }

    @Override
    public int compareTo(Object o) {    // Compare according to date and time
        Record rec = (Record)o;
//        if (this.month > rec.month){
//            return 1;
//        }else if (this.month < rec.month){
//            return -1;
//        }else if (this.month == rec.month){
//            if (this.day > rec.day){
//                return 1;
//            }else if (this.day < rec.day){
//                return -1;
//            }else if (this.day == rec.day){
//                if (this.time > rec.time){
//                    return 1;
//                }else if (this.time < rec.time){
//                    return -1;
//                }else if (this.time == rec.time){
//                    return 0;
//                }
//            }
//        }
        double this_time = (double) LabelData.dateToHour(this.month, this.day) + this.time;
        double rec_time = (double) LabelData.dateToHour(rec.month, rec.day) + rec.time;
        if (this_time > rec_time){
            return 1;
        }else if (this_time < rec_time){
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public Node toNode(String seg_id,String device_id ){
        if(this.location!=null){
            Node node = new Node(toNodeCnt , this.location.lat,this.location.lon);
            node.seg_id=this.seg_id;
            node.type= Node.NodeType.RECORD;
            toNodeCnt++;
            return node;
        }else if(this.seg_id!=null) {
            Node node = new Node(toNodeCnt,0,0);
            node.seg_id=this.seg_id;
            node.type= Node.NodeType.RECORD;
            toNodeCnt++;
            return node;
        }
        System.out.println("Something is fishy");
        return null;


    }
    public Node toNode(){
        Long k = System.currentTimeMillis();
        Random randomGenerator = new Random(k*10L);
        Long maximum = 10000L;
        Long minimum = -10000L;
        Long randomInt = randomGenerator.nextLong() % (maximum - minimum) + maximum;
        Node node = new Node(toNodeCnt , this.location.lat,this.location.lon);
        toNodeCnt++;
        return node;
    }


}
