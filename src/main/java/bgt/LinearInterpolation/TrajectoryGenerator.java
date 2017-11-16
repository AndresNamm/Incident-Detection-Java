package bgt.LinearInterpolation;

import bgt.MapMatching.MapMatcher;
import bgt.Model.Record;
import bgt.Model.Segment;
import bgt.Utility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.DocumentException;

import java.io.*;
import java.util.*;

public class TrajectoryGenerator {

    public MapMatcher mm;

    public TrajectoryGenerator(MapMatcher mm) throws FileNotFoundException, DocumentException {
        this.mm = mm;
    }

    public HashMap<Integer, List<Record>> parseRecords(String filename, HashMap<Integer, Integer> deviceNrs, int lines, boolean filter) throws IOException {
        HashMap<Integer, List<Record>> devRecMap = new HashMap<>();
        String seg_id = null;
        int cnt = 0;
        int c = 1;
        String line = null;
        File file = FileUtils.getFile(filename);
        LineIterator it = FileUtils.lineIterator(file);
        while (it.hasNext()) {
            line = it.nextLine();
            cnt++;
            if (cnt > c * (lines / 4)) {
                System.out.println(c + "/4 th  of file parsed");
                c++;
            }
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")) {  // this line corresponds to a segment
                seg_id = st.nextToken();
            } else if (flag.equals("R")) { // this line belongs to a record
                long rec_id = Long.valueOf(st.nextToken());
                int dev_id = Integer.valueOf(st.nextToken());
                if (!deviceNrs.containsKey(dev_id) && filter) {
                    continue;
                }
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
                if (!devRecMap.containsKey(dev_id)) {
                    devRecMap.put(dev_id, new ArrayList<>());
                }
                devRecMap.get(dev_id).add(rec);
            }
        }
        return devRecMap;
    }

    public HashMap<Integer, List<List<Record>>> genTrajForDevices(HashMap<Integer, List<Record>> devRecMap) throws IOException {
        ArrayList<Integer > trajLenghts = new ArrayList<>();
        HashMap<Integer, List<List<Record>>> partjalTrajsMap = new HashMap<>();
        for (Map.Entry<Integer, List<Record>> entry : devRecMap.entrySet()) {
            Integer devId = entry.getKey();
            List<Record> devRecs = entry.getValue();
            System.out.println(devRecs.size());
            long start = System.currentTimeMillis();
            Collections.sort(devRecs);
            System.out.println(devId);
            MapMatcher.printElapsedTime(start);
            int trajLength = 1;
            ArrayList<Record> trajectory= new ArrayList<>();
            trajectory.add(devRecs.get(0));
            for (int i = 1; i < devRecs.size(); i++) {
                Record lastRec = trajectory.get(trajectory.size() - 1);
                Record rec = devRecs.get(i);
                Segment lastSeg = this.mm.segMap.get(lastRec.seg_id);
                Segment thisSeg = this.mm.segMap.get(rec.seg_id);
                if (thisSeg.way_id == lastSeg.way_id &&
                        thisSeg.inner_id >= lastSeg.inner_id &&
                        rec.month == lastRec.month &&
                        rec.day == lastRec.day) {
                    if (rec.time > lastRec.time && rec.time - lastRec.time <= (1.0/60.0)){
                        //lastTraj.add(rec);//
                        trajLength++;
                        trajectory.add(rec);
                    }else if(rec.time == lastRec.time){ // GPS device is broken in this case
                        continue;// just eliminates failed records.
                    }else{
                        addTrajForDevice(partjalTrajsMap,devId,trajectory);
                        trajLenghts.add(trajLength);
                        trajLength=1;
                        trajectory= new ArrayList<>();
                        trajectory.add(rec); // new trajectory is started
                    }
                }else{
                    addTrajForDevice(partjalTrajsMap,devId,trajectory);
                    trajLenghts.add(trajLength);
                    trajLength=1;
                    trajectory = new ArrayList<>();
                    trajectory.add(rec);
                    int k = 1;
                }

            }
            addTrajForDevice(partjalTrajsMap,devId,trajectory);// add final trajectory
        }

        System.out.println("mean of traj lengths"+ Utility.mean(trajLenghts));
        System.out.println("std "+ Utility.sd(trajLenghts));


//
//        String fileName = "plots/trajLens.txt";
//        FileWriter fw = new FileWriter(fileName,true);
//        BufferedWriter bw = new BufferedWriter(fw);
//        PrintWriter out = new PrintWriter(bw);
//        for(Integer i : trajLenghts){
//            out.println(i);
//        }
//        out.close();

        return partjalTrajsMap;


    }

    public void addTrajForDevice(HashMap<Integer,List<List<Record>>> trajMap, Integer devId, List<Record> trajectory){
        if(!trajMap.containsKey(devId)){
            trajMap.put(devId, new ArrayList<>());
        }trajMap.get(devId).add(trajectory);
    }


    // created the main function here for testing
    public static void main(String args[]) throws IOException, DocumentException {
        String filename = "data/map_matching/fold_3/train_match_result.txt";
        File file = FileUtils.getFile(filename);
        //Files.delete(new File("data/linear_interpolation/fold_"+fold+"/"+type+"_interpolation_result.csv").toPath());// DELETE OLD OUTUT FILE
        LineIterator it = FileUtils.lineIterator(file);
        HashMap<Integer, Integer> devMap = new HashMap<>();
        int lines = 0;
        while (it.hasNext()) {
            lines++;
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")) {  // this line corresponds to a segment
                String seg_id = st.nextToken();
            } else if (flag.equals("R")) { // this line belongs to a record
                long rec_id = Long.valueOf(st.nextToken());
                int dev_id = Integer.valueOf(st.nextToken());
                devMap.put(dev_id, dev_id);
            }
        }


        int bucketSize = 100;
        int previous = 0;

        // Converting DevIDs hashmap into multiple submaps to iterate over
        ArrayList<HashMap<Integer, Integer>> buckets = new ArrayList<>();
        int nrOfBuckets = (int) Math.floor(devMap.size() / bucketSize) + 1;
        for (int j = 0; j < nrOfBuckets; j++) {
            buckets.add(new HashMap<Integer, Integer>());
        }

        List<Integer> devIDs = new ArrayList(devMap.values());
        for (int i = 0; i < devIDs.size(); i++) {
            buckets.get(i / bucketSize).put(devIDs.get(i), devIDs.get(i));
        }
        System.out.println("We have " + lines + " lines");
        //Checking LineNR
        TrajectoryGenerator tG = new TrajectoryGenerator(new MapMatcher());
        HashMap<Integer, List<Record>> k = tG.parseRecords(filename, buckets.get(0), lines, true);
        tG.genTrajForDevices(k);

    }

}
