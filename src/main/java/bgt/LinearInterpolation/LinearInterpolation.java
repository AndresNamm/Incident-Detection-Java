package bgt.LinearInterpolation;

import bgt.MapMatching.MapMatcher;
import bgt.Model.Record;
import bgt.Model.Segment;
import bgt.Model.Node;
import bgt.parsing.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.DocumentException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.*;

/**
 * Created by admin on 2016/11/29.
 */
public class LinearInterpolation {

    public enum GenerationType{
        train, test
    }

    public List<Segment> segList;
    public HashMap<String, Segment> segsMap; //  store (seg_id, seg) pairs
    public HashMap<Integer, List<List<Record>>> trajsMap; // store (dev_id, trajectbories)
    MapMatcher mm;

    public LinearInterpolation() throws IOException, DocumentException {
        this.mm = new MapMatcher();
        //mm.parseSegs();
        mm.indexSegs();
        //mm.MapMatcher();
        this.segsMap = mm.segMap;
        this.segList = mm.segList;
    }

    public static int getNrLines(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        int lines = 0;
        while (reader.readLine() != null) lines++;
        reader.close();
        return  lines;
    }

    public static Record generateTestRec(String line){
        String[] split = line.split(",");
        String segId = split[0];
        Double time = Double.valueOf(split[1]);
        Double speed = Double.valueOf(split[2]);
        int month,day;
        day = Integer.valueOf(split[3]);
        month = Integer.valueOf(split[4]);
        Record rec = new Record(0,month,day,time,speed,segId);
        return  rec;
    }

    public void printToHistogramLongThan2(HashMap<Integer, List<List<Record>>> devTrajs, String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        for(Integer key : devTrajs.keySet()){
            List<List<Record>> trajs = devTrajs.get(key);
            for(List<Record> traj : trajs){
                int cnt = Analysis.getNrOfSegs(traj);
                if(cnt!=1){
                    lines.add(String.valueOf(cnt));
                }
            }
        }
        Parser.printArrayListLines(lines, fileName,true);
    }


    public void filterShorterTrajsThan(int boundary, HashMap<Integer, List<List<Record>>> devTrajs){
        for(Integer key : devTrajs.keySet()){
            List<List<Record>> trajs = devTrajs.get(key);
            Iterator<List<Record>>it= trajs.iterator();

            while(it.hasNext()){
                List<Record> traj = it.next();
                //int cnt = Analysis.getNrOfSegs(traj);
                Record lastRec = traj.get(traj.size()-1);
                Record firstRec = traj.get(0);
                int distance = Integer.valueOf(lastRec.seg_id.split("_")[1])- Integer.valueOf(firstRec.seg_id.split("_")[1]);
                if (distance * 75 < boundary){
                    it.remove();
                }
            }
        }
    }


    public void parseAndInterpolatePieceWise(String filename, int fold, GenerationType type, Boolean distance) throws IOException, InterruptedException, DocumentException {
        File file = FileUtils.getFile(filename);
        try{
            Files.delete(new File("data/linear_interpolation/fold_"+fold+"/"+type+"_interpolation_result.csv").toPath());// DELETE OLD OUTUT FILE
        }catch (NoSuchFileException e){
            System.out.println("NO such file");
        }
        LineIterator it = FileUtils.lineIterator(file);
        HashMap<Integer, Integer > devMap = new HashMap<>();
        int lines = 0;
        while(it.hasNext()){
            lines++;
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")){  // this line corresponds to a segment
                String seg_id = st.nextToken();
            }else if (flag.equals("R")){ // this line belongs to a record
                long rec_id = Long.valueOf(st.nextToken());
                int dev_id = Integer.valueOf(st.nextToken());
                devMap.put(dev_id,dev_id);
            }
        }

        System.out.println("We have " +lines+ " lines" );
        //Checking LineNR

        int bucketSize = 50;
        int previous = 0;

        // Converting DevIDs hashmap into multiple submaps to iterate over
        ArrayList<HashMap<Integer,Integer>> buckets = new ArrayList<>();
        int nrOfBuckets = (int) Math.floor(devMap.size() / bucketSize) + 1;
        for(int j =0;j<nrOfBuckets;j++){
            buckets.add(new HashMap<Integer,Integer>());
        }

        List<Integer> devIDs = new ArrayList(devMap.values());
        for(int i = 0;i <devIDs.size();i++){
            buckets.get(i/bucketSize).put(devIDs.get(i),devIDs.get(i));
        }

        TrajectoryGenerator tG = new TrajectoryGenerator(this.mm);
        for(HashMap<Integer, Integer> fracDevID :  buckets){ // GENERATES TRAJECTORY MAPS FOR ONLY 100 DEVICES IN EACH ITERATION
            HashMap<Integer, List<List<Record>>> partialTrajsMap = new HashMap<>();
            System.out.println(fracDevID);
            System.out.println(fracDevID.size());
            //parseRecords(filename, fracDevID, partialTrajsMap, lines,true); // IMPORTANT PART 1
            HashMap<Integer,List<Record>> devRecMap = tG.parseRecords(filename, fracDevID,  lines,true); // IMPORTANT PART 1
            partialTrajsMap = tG.genTrajForDevices(devRecMap);
            this.printToHistogramLongThan2(partialTrajsMap, "plots/difHist.txt");
            if(distance){
                this.filterShorterTrajsThan(500, partialTrajsMap);
            }
            System.out.println("Start Interpolation");
            interpolate(partialTrajsMap);// IMPORTANT PART 2
            outputResult("data/linear_interpolation/fold_"+fold+"/"+type+"_interpolation_result.csv", partialTrajsMap,  type);
            partialTrajsMap=null;
            System.gc();
        }

    }

    public void parseRecords(String filename, HashMap<Integer,Integer> deviceNrs, HashMap<Integer, List<List<Record>>> partialTrajsMap, int lines, boolean filter ) throws IOException, InterruptedException{
        //cur_trajsMap = new HashMap<>();
        //Checking lineNR;
        //InputStreamReader isr = new InputStreamReader(new FileInputStream(filename));
        //BufferedReader br = new BufferedReader(isr);
        String seg_id = null;
        int cnt = 0;
        int c = 1;
        String line = null;
        File file = FileUtils.getFile(filename);
        LineIterator it = FileUtils.lineIterator(file);
        while (it.hasNext()){
            line=it.nextLine();
            cnt++;
            if(cnt>c*(lines/4)){
                System.out.println(c +"/4 th  of file parsed");
                c++;
            }
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")){  // this line corresponds to a segment
                seg_id = st.nextToken();
            }else if (flag.equals("R")){ // this line belongs to a record
                long rec_id = Long.valueOf(st.nextToken());
                int dev_id = Integer.valueOf(st.nextToken());
                if(!deviceNrs.containsKey(dev_id) && filter){continue;}
                int month = Integer.valueOf(st.nextToken());
                int day = Integer.valueOf(st.nextToken());
                double time = Double.valueOf(st.nextToken());

                String[] loc_coords = st.nextToken().split(",");
                double loc_lat = Double.valueOf(loc_coords[0]);
                double loc_lon = Double.valueOf(loc_coords[1]);

                double speed = Double.valueOf(st.nextToken());
                double direction = Double.valueOf(st.nextToken());

                Record rec = new Record(rec_id, dev_id, month, day,
                        time, loc_lat, loc_lon, speed, direction, seg_id);

                // Store this rec in cur_trajsMap
                if (!partialTrajsMap.containsKey(dev_id)){ // the first record of this device
                    List<Record> traj = new ArrayList<>();
                    traj.add(rec);
                    List<List<Record>> trajs = new ArrayList<>();
                    trajs.add(traj);
                    partialTrajsMap.put(dev_id, trajs);
                }else{ // not the first record
                    List<List<Record>> trajs = partialTrajsMap.get(dev_id);
                    List<Record> lastTraj = trajs.get(trajs.size()-1);
                    Record lastRec = lastTraj.get(lastTraj.size()-1);
                    Segment lastSeg = segsMap.get(lastRec.seg_id);
                    Segment thisSeg = segsMap.get(rec.seg_id);
                    if (thisSeg.way_id == lastSeg.way_id &&
                            thisSeg.inner_id >= lastSeg.inner_id &&
                            rec.month == lastRec.month &&
                            rec.day == lastRec.day)
                    {
                        if (rec.time > lastRec.time && rec.time - lastRec.time <= (1.0/60.0)){
                            lastTraj.add(rec);//
                        }else if(rec.time == lastRec.time){ // GPS device is broken in this case
                            continue;
                        }else{
                            List<Record> newTraj = new ArrayList<>();
                            newTraj.add(rec);
                            trajs.add(newTraj);
                        }
                    }else{
                        List<Record> newTraj = new ArrayList<>();
                        newTraj.add(rec);
                        trajs.add(newTraj);
                    }
                }
            }

        }
    }

    public void parseRecords(String filename) throws IOException, InterruptedException{
        this.trajsMap = new HashMap<>();
        int lines = getNrLines(filename);
        parseRecords(filename,new HashMap<Integer,Integer>(),trajsMap,lines,false );
    }

    public void interpolate(){
        interpolate(this.trajsMap);
    }

    public void interpolate(HashMap<Integer, List<List<Record>>> trajsMap){
        Iterator trajsItr = trajsMap.entrySet().iterator();
        int fullSize = trajsMap.size();
        int count= 0;
        int c = 1;
        while (trajsItr.hasNext()){
            Map.Entry dev_trajs_pair = (Map.Entry)trajsItr.next();
            int dev_id = (int)dev_trajs_pair.getKey();
            List<List<Record>> trajs = (List<List<Record>>) dev_trajs_pair.getValue();
            List<List<Record>> newTrajs = new ArrayList<>();
            Iterator<List<Record>> trajItr = trajs.iterator();
            count++;
            if(count > c*(fullSize/4)){
                System.out.println(c + "/4 is done ");
                c++;
            }

            while (trajItr.hasNext()){
                List<Record> traj = trajItr.next();
                if (traj.size() == 1){  // if this trajectory contains only one record
                    newTrajs.add(traj);
                }else{  // the trajectory contains two or more records
                    List<Record> newTraj = new ArrayList<>();
                    Record firstRec = traj.get(0);
                    int firstInner_id = segsMap.get(firstRec.seg_id).inner_id;

                    Record lastRec = traj.get(traj.size()-1);
                    int lastInner_id = segsMap.get(traj.get(traj.size()-1).seg_id).inner_id;
                    if (lastInner_id == firstInner_id){
                        double distance = Node.calcDist(firstRec.getLocation(), lastRec.getLocation());
                        double speed = distance/1000.0/(lastRec.time-firstRec.time);
                        double time = (firstRec.time + lastRec.time) / 2;

                        Record newRec = new Record(dev_id, firstRec.month, firstRec.day, time, speed, firstRec.seg_id);
                        newTraj.add(newRec);
                    }else{  // at least cross over 2 segments
                        int inner_id_diff = lastInner_id-firstInner_id;
                        double[] times = new double[inner_id_diff+2];
                        times[0] = firstRec.time; times[inner_id_diff+1] = lastRec.time;
                        int i = 0, j = 1, index = 1;
                        while (j < traj.size()){
                            Record sRec = traj.get(i);
                            Record eRec = traj.get(j);
                            int sInner_id = segsMap.get(sRec.seg_id).inner_id;
                            int eInner_id = segsMap.get(eRec.seg_id).inner_id;
                            if (eInner_id - sInner_id >= 1){// THIS MEANS THAT IN TRAJECTORY SOME SEGMENTS HAVE BEEND PASSED
                                double dist_s = Node.calcDist(sRec.getLocation(), segsMap.get(sRec.seg_id).endNode);
                                double dist_e = Node.calcDist(segsMap.get(eRec.seg_id).startNode, eRec.getLocation());
                                double dist = dist_s + dist_e;
                                String[] seg_id_frags = sRec.seg_id.split("_");
                                double[] inter_dists = new double[eInner_id-sInner_id-1];
                                for (int k=sInner_id+1; k<eInner_id; k++){
                                    String seg_id = seg_id_frags[0]+"_"+k+"_"+seg_id_frags[2];
                                    Segment inter_seg = segsMap.get(seg_id);// GET THE MIDDLE SEGMENT
                                    double inter_dist = Node.calcDist(inter_seg.getStartNode(), inter_seg.getEndNode());//segmen length
                                    inter_dists[k-sInner_id-1] = inter_dist;
                                    dist += inter_dist;
                                }
                                double timeDiff = eRec.time - sRec.time;
                                times[index] = sRec.time + dist_s/dist*timeDiff;
                                index++;
                                for (int k=sInner_id+1; k<eInner_id; k++){
                                    double time_add = inter_dists[k-sInner_id-1]/dist*timeDiff;
                                    times[index] = times[index-1] + time_add;
                                    index++;
                                }
                            }
                            i++; j++;
                        }
                        String firstSeg_id = firstRec.seg_id;
                        double firstSpeed = Node.calcDist(firstRec.getLocation(), segsMap.get(firstSeg_id).getEndNode())
                                / 1000.0 / (times[1] - times[0]);
                        double firstTime = (times[0] + times[1]) / 2.0;
                        Record newFirstRec = new Record(dev_id, firstRec.month, firstRec.day, firstTime, firstSpeed, firstSeg_id);
                        newTraj.add(newFirstRec);
                        String[] first_seg_id_frags = firstSeg_id.split("_");
                        for (i = firstInner_id+1; i<lastInner_id; i++){
                            int pure_index = i - firstInner_id+1;
                            String innerSeg_id = first_seg_id_frags[0] + "_" +i + "_" + first_seg_id_frags[2];
                            double innerSpeed = 0.0;
                            innerSpeed = Node.calcDist(segsMap.get(innerSeg_id).startNode, segsMap.get(innerSeg_id).endNode)
                                    / 1000.0 / (times[pure_index] - times[pure_index - 1]);
                            double innerTime = (times[pure_index-1] + times[pure_index]) / 2.0;
                            Record newInnerRec = new Record(dev_id, firstRec.month, firstRec.day, innerTime, innerSpeed, innerSeg_id);
                            newTraj.add(newInnerRec);
                        }
                        String lastSeg_id = lastRec.seg_id;
                        double lastSpeed = Node.calcDist(segsMap.get(lastSeg_id).getStartNode(), lastRec.getLocation())
                                / 1000.0 / (times[inner_id_diff+1] - times[inner_id_diff]);
                        double lastTime = (times[inner_id_diff+1] + times[inner_id_diff]) / 2.0;
                        Record newLastRec = new Record(dev_id, lastRec.month, lastRec.day, lastTime, lastSpeed, lastSeg_id);
                        newTraj.add(newLastRec);
                    }
                    newTrajs.add(newTraj);
                }
                trajItr.remove();
            }
            trajsMap.put(dev_id, newTrajs);
        }
    }

    public void outputResult(String result_filename, HashMap<Integer, List<List<Record>>> trajsMap, GenerationType type ) throws IOException{
        FileWriter fw_result = new FileWriter(result_filename,true);
        Iterator trajsItr = trajsMap.entrySet().iterator();
        while (trajsItr.hasNext()){
            Map.Entry dev_trajs_pair = (Map.Entry)trajsItr.next();
            List<List<Record>> trajs = (List<List<Record>>) dev_trajs_pair.getValue();
            Iterator<List<Record>> trajItr = trajs.iterator();
            while (trajItr.hasNext()){
                List<Record> traj = trajItr.next();
                Iterator<Record> recItr = traj.iterator();
                while (recItr.hasNext()){
                    Record rec = recItr.next();
                    String seg_id = rec.seg_id;
                    String[] seg_id_split = seg_id.split("_");
                    if(type==GenerationType.train){
                        fw_result.write(seg_id_split[0]+"_"+seg_id_split[1]+"_"+(int)rec.time + "," + (int)Math.round(rec.speed) + "\n");
                    }else if(type==GenerationType.test){
                        int month = rec.month;
                        int day = rec.day;

                        fw_result.write(seg_id_split[0]+"_"+seg_id_split[1]+"_0,"+rec.time + "," + (int)Math.round(rec.speed) + "," + rec.day +","+ rec.month+ "\n" );
                    }
                }
            }
        }
        fw_result.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException, DocumentException{
        // train
//        for (int i=1; i<=4; i++){
//            LinearInterpolation li = new LinearInterpolation();
//            li.parseAndInterpolatePieceWise("data/map_matching/fold_"+i+"/train_match_result.txt",i);
//        }
        // test OUTPUT , DIRECTED FOR LABELING
        for (int i=1; i<=4; i++){
            LinearInterpolation li = new LinearInterpolation();
            GenerationType type = GenerationType.test;
            li.parseAndInterpolatePieceWise("data/map_matching/fold_"+i+"/"+type+"_match_result.txt",i,type,true);
        }
    }

}
