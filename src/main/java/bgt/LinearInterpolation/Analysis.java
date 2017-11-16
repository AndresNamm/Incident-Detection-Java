package bgt.LinearInterpolation;

import bgt.MapMatching.MapMatcher;
import bgt.MapMatching.ParseMapMatchResults;
import bgt.Model.Record;
import bgt.Model.Node;
import bgt.Model.Routes;
import bgt.Model.Way;
import bgt.parsing.OSMparser;
import bgt.parsing.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;


class Nqueue {


    public PriorityQueue<List<Record>> getPriority() {
        return new PriorityQueue<>(priority); // UNDEFENSIVE COPY
    }

    private PriorityQueue<List<Record>> priority;
    private final int maxSize;

    public Nqueue(int maxSize, PriorityQueue<List<Record>> nExtremes) {
        this.maxSize = maxSize;
        this.priority = nExtremes;
    }

    public void tryPlaceToQueue(List<Record> value, BiPredicate<List<Record>, List<Record>> p) {
        if (priority.size() >= this.maxSize) { // DONT ALLOW THE QUEUE TO GROW TOO LARGE
            List<Record> extremum = priority.peek();
            if (p.test(value, extremum)) {
                priority.poll();
                priority.offer(value);
            }
        } else {
            priority.offer(value);
        }
    }
}


public class Analysis {
    MapMatcher mm;
    public enum GenerationType {
        SPEEDPANELSGPS, GPS
    }

    Analysis() throws FileNotFoundException, DocumentException {
        mm = new MapMatcher();
    }

    class ComparePoint implements Comparator<List<Record>> {  // THIS COMPARATOR HAS SMALLES IN BEGINNING
        @Override
        public int compare(List<Record> o1, List<Record> o2) {
            if (getNrOfSegs(o1) > getNrOfSegs(o2)) {
                return 1;
            } else if (getNrOfSegs(o1) < getNrOfSegs(o2)) {
                return -1;
            }
            return 0;
        }
    }

    public static int getNrOfSegs(List<Record> recList) {
        int segCnt = 1;
        for (int j = 1; j < recList.size(); j++) {
            Record sRec = recList.get(j - 1);
            Record eRec = recList.get(j);
            if (!sRec.seg_id.equals(eRec.seg_id)) {
                segCnt++;
            }
        }
        return segCnt;
    }

    public List<List<Record>> getXlongestTrajs(HashMap<Integer, List<List<Record>>> trajsMap, int nrOfLargest) {
        List<Record> maxTraj = new ArrayList<>();
        Nqueue nLargest = new Nqueue(nrOfLargest, new PriorityQueue<>(nrOfLargest, new ComparePoint()));
        for (Map.Entry<Integer, List<List<Record>>> ee : trajsMap.entrySet()) {
            Integer key = ee.getKey();
            List<List<Record>> trajList = ee.getValue();
            for (List<Record> recList : trajList) {
                nLargest.tryPlaceToQueue(recList, (rL1, rL2) -> this.getNrOfSegs(rL1) > this.getNrOfSegs(rL2));
            }
        }
        ArrayList<List<Record>> lTrajs = new ArrayList<>();
        PriorityQueue<List<Record>> xLargest = nLargest.getPriority();
        while (!xLargest.isEmpty()) {
            lTrajs.add(xLargest.poll());
        }
        return lTrajs;
    }

    public void getHistorgramOfTrajectoriesAndLongest(int sampleSize, Boolean interpolation, int nrTrajs) throws IOException, InterruptedException, DocumentException {
        //file handling
        int foldNr = 1;
        String filename = "data/map_matching/fold_" + foldNr + "/train_match_result.txt";
        File file = FileUtils.getFile(filename);
        LineIterator it = FileUtils.lineIterator(file);
        //file handling


        // GET HASHMAP OF DEVICE IDS
        HashMap<Integer, Integer> devMap = new HashMap<>();
        int count = 0;
        while (it.hasNext()) {
            count++;
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

        System.out.println("This many lines in  " + filename + " # " + count);
        int offset = 100;
        int[] boundaries = {offset, offset + sampleSize};
        List<Integer> devIDS = new ArrayList<>();
        for (Integer key : devMap.values()) {
            devIDS.add(key);
        }
        devIDS = devIDS.subList(boundaries[0], boundaries[1]);
        HashMap<Integer, Integer> deviceMap = new HashMap<>();

        LinearInterpolation li = new LinearInterpolation();
        for (Integer devID : devIDS) {
            deviceMap.put(devID, devID);
        }
        // GET HASHMAP OF DEVICE IDS

        // GENERATE A HASHMAP OF TRAJECTORIES WITH INTERPOLATION OBJECT

        HashMap<Integer, List<List<Record>>> trajsMap ;
        //li.parseRecords(filename, deviceMap, trajsMap, count,true); // IMPORTANT PART 1
        TrajectoryGenerator tG = new TrajectoryGenerator(this.mm);
        HashMap<Integer, List<Record>> devRecMap = tG.parseRecords(filename, deviceMap, count, true); // IMPORTANT PART 1
        trajsMap = tG.genTrajForDevices(devRecMap);//GENERATE A HASHMAP OF TRAJECTORIES


        //PRINT OUT HISTOGRAM OF TRAJECTORY LENGHTS
        String fileName = "trajHistogramNew.txt";
        FileWriter fw_result = new FileWriter(fileName);
        for (Map.Entry<Integer, List<List<Record>>> ee : trajsMap.entrySet()) {
            Integer key = ee.getKey();
            List<List<Record>> trajList = ee.getValue();
            for (List<Record> recList : trajList) {
                fw_result.write(recList.size() + "\n");
            }
        }

        fw_result.close();
        Node location;
        int eqCount = 0;
        int goodCount = 0;
        int maxSegCnt = Integer.MIN_VALUE;
        String idMaxSegCnt = "";
        //END PRINT OUT HISTOGRAM

        //PRINT OUT TRAJECTORY SEGMENT COUNT HISTORGAM
        fileName = "size2biggerTrajSegmentHistogram.txt";
        fw_result = new FileWriter(fileName);
        // GET THE AMOUNT OF SIZE>2 TRAJECTORIES WITH END AND  BEGIN RECORD THE SAME
        HashMap<String, Integer> trajectorySegCount = new HashMap<>();
        for (Map.Entry<Integer, List<List<Record>>> ee : trajsMap.entrySet()) {
            Integer key = ee.getKey();
            List<List<Record>> trajList = ee.getValue();
            for (int i = 0; i < trajList.size(); i++) {
                List<Record> recList = trajList.get(i);
                String id = String.valueOf(key) + "_" + String.valueOf(i);
                if (recList.size() != 1) {
                    Record firstRec = recList.get(0);
                    Record lastRec = recList.get(recList.size() - 1);
                    if (firstRec.location.lat == lastRec.location.lat &&
                            firstRec.location.lon == lastRec.location.lon) {
                        eqCount++;
                        fw_result.write(1 + "\n");
                    } else {
                        int segCnt = 0;
                        for (int j = 1; j < recList.size(); j++) {
                            Record sRec = recList.get(j - 1);
                            Record eRec = recList.get(j);
                            if (!sRec.seg_id.equals(eRec.seg_id)) {
                                segCnt++;
                            }
                        }
                        if (segCnt > maxSegCnt) {
                            idMaxSegCnt = id;
                            maxSegCnt = segCnt;
                        }
                        trajectorySegCount.put(id, segCnt);
                        fw_result.write(segCnt + "\n");
                        goodCount++;
                    }
                } else {
                    //fw_result.write(1 + "\n");// Write down length 1 trajectorys to histogram as well.
                }

            }
        }

        fw_result.close();
        System.out.println("EQUAL START END NODE COUNT " + eqCount);
        System.out.println("DIFFERENT START END NODE COUNT " + goodCount);
        System.out.println("MAX SEGMENT COUNT " + maxSegCnt);
        //END GET THE AMOUNT OF SIZE>2 TRAJECTORIES WITH END AND  BEGIN RECORD THE SAME
        //END PRINT OUT TRAJECTORY SEGMENT COUNT HISTORGAM
        //CHOOSE OUT  LARGEST LONGEST TRAJECTORY
        //Get the maximum trajectory
        Integer maxDeviceId = Integer.valueOf(idMaxSegCnt.split("_")[0]);
        Integer trajInx = Integer.valueOf(idMaxSegCnt.split("_")[1]);
        List<Record> maxTraj = trajsMap.get(maxDeviceId).get(trajInx);
        //End Get the maximum trajectory

        // Get 20 largest trajs



        List<List<Record>> nLargestTrajs = getXlongestTrajs(trajsMap, nrTrajs);
        System.out.println("Printing out " + nLargestTrajs.size() +" larges trajectories.");

        HashMap<Integer, List<List<Record>>> longTrajs = new HashMap<>();
        for (List<Record> traj : nLargestTrajs) {
            // check that all records share the same device id
            Integer devId = traj.get(0).dev_id;
            for (Record rec : traj){
                assert (devId.equals(rec.dev_id));
            }
            if (!longTrajs.containsKey(devId)) {
                longTrajs.put(devId,new ArrayList<>());
            }longTrajs.get(devId).add(traj);
        }

        if(interpolation){
            li.interpolate(longTrajs);
        }
        List<List<Record>> largeTrajs = new ArrayList<>();
        for(List<List<Record>> tList : longTrajs.values()){
            for(List<Record> traj: tList){
                largeTrajs.add(traj);
            }
        }

        // End Get 20 largest trajs
        //END CHOOSE OUT LONGEST TRAJECTORY
        // PRINT OUT LONGEST TRAJECTORY
        fileName = "longesTrajectory.txt";
        fw_result = new FileWriter(fileName);
        fw_result.write(maxTraj.size());
        for (Record rec : maxTraj) {
            fw_result.write(rec + "\n");
        }
        fw_result.close();
        //END PRINT OUT LONGEST TRAJECTORY
        // GENERATE OSM BASED ON 5 LONGEST TRAJS
        //List<List<Record>> largeTrajs=  this.getXlongestTrajs(trajsMap,2);
        System.out.println("SIZES");
        System.out.println(largeTrajs.get(largeTrajs.size() - 1).size());
        System.out.println(largeTrajs.get(0).size());
        this.generateOSMforSegmentAndTraj(largeTrajs, interpolation);
        // GENERATE OSM BASED ON 5 LONGEST TRAJS
    }

    public void generateOSMforSegmentAndTraj(List<List<Record>> trajList, Boolean interpolation) throws IOException, DocumentException {
        int cnt = 0;
        List<List<Node>> listOfTrajs = new ArrayList<>();
        //PRINT TRAJECTORIES
        for (List<Record> traj : trajList) {
            List<Node> nodeList = new ArrayList<>();
            for (Record rec : traj) {
                Node newNode = rec.toNode(rec.seg_id, String.valueOf(rec.speed));
                if(newNode.lat==0 & newNode.lon==0){
                    newNode.lat = mm.segMap.get(newNode.seg_id).startNode.lat;
                    newNode.lon = mm.segMap.get(newNode.seg_id).startNode.lon;
                }
                nodeList.add(newNode);
                cnt++;
            }
            listOfTrajs.add(nodeList);
        }

        List<Way> wayList = new ArrayList<>();
        for (List<Node> nodeList : listOfTrajs) {
            Way temp = new Way(cnt, nodeList);
            wayList.add(temp);
            cnt++;
        }

        String trajFname = interpolation ?"interpTrajectories.osm": "Trajectories.osm" ;
        OSMparser.waysToOsm(wayList, this.mm.routes, trajFname);
        //END PRINT TRAJECTORIES

        //PRINT SEGMENTS ASSIGNED TO THE RECORDS IN TRAJECTORIES

        // 1. get info about all the ways in the input map file
        Routes routes = OSMparser.parseNodesAndWays("data/route_segmentation/Hong_Kong-result.osm");
        List<Way> allWayList = routes.getWayList();
        HashMap<Long, Way> wayMap = new HashMap<>();
        for (Way way : allWayList) {
            wayMap.put(way.id, way);
        }

        // 2. get info about all the ways that are in segments
        HashMap<Long, Way> waysForRecords = new HashMap<>();
        for (List<Record> traj : trajList) {
            List<Node> nodeList = new ArrayList<>();
            // get way based on 1 record in trajectory because all should be same in 1 trajcetory
            String oldWayId = traj.get(0).seg_id.split("_")[0];
            String oldInnerId = traj.get(0).seg_id.split("_")[0];
            waysForRecords.put(Long.valueOf(oldWayId), wayMap.get(Long.valueOf(oldWayId)));
            // just testing that way ids are the same for all records in trajectory
            for (Record rec : traj) {
                String wayId = rec.seg_id.split("_")[0];
                String innerId = rec.seg_id.split("_")[1];
                if (!oldWayId.equals(wayId)) {
                    System.out.println("We have a fail with trajectory");
                    System.out.println(wayId);
                    System.out.println(oldWayId);
                }
                oldWayId = wayId;
            }
            listOfTrajs.add(nodeList);
        }


        //  3. now Print out all way
        List<Way> waylistRec = new ArrayList<>();
        for (Way way : waysForRecords.values()) {
            waylistRec.add(way);

        }

        OSMparser.waysToOsm(waylistRec, routes, "Ways.osm");

        //END PRINT SEGMENTS ASSIGNED TO THE RECORDS IN TRAJECTORIES

    }

    public void getWayRecordCounts( GenerationType type) throws IOException {
        HashMap<Long,Long> wayCount = new HashMap<>();
        long totalRows = 0l;//totalRows + Parser.countLinesFile(fileName);
        long totalRecords = 0l;
        for(int foldNr = 1;foldNr<=4; foldNr++){
            String fileName = "";
            if(type==GenerationType.GPS){
               fileName= "data/linear_interpolation/fold_" + foldNr + "/train_interpolation_result.csv";
            }else if(type == GenerationType.SPEEDPANELSGPS){
               fileName = "data/speed_panels/append_train/train_interpolation_result"+foldNr+".csv";
            }
            File f = FileUtils.getFile(fileName);
            totalRows = totalRows + Parser.countLinesFile(fileName);
            HashMap<String, Long> aRcounts = InpolatParser.getSegmentsArCounts(fileName);

            for (String key: aRcounts.keySet()){
                Long wayId = Long.valueOf(key.split("_")[0]);
                totalRecords+=aRcounts.get(key);
                if(!wayCount.containsKey(wayId)){
                    wayCount.put(wayId,0l);
                }wayCount.put(wayId,wayCount.get(wayId)+aRcounts.get(key));
            }
        }

        System.out.println("totalRows "+(totalRows));
        System.out.println("totalRecs" +(totalRecords) );
        String dm = Parser.defaultDelimiter;
        ArrayList<String> lineList = new ArrayList<>();
        for(Long key : wayCount.keySet()){
            String line = String.valueOf(key) + dm + String.valueOf(wayCount.get(key)/4.0);
            lineList.add(line);
        }
        Parser.printArrayListLines(lineList, "plots/i_way_counts"+type+".txt", false);

    }

    public static void main(String[] args) throws IOException, InterruptedException, DocumentException {
        Analysis liPolManTest = new Analysis();
        //liPolManTest.getHistorgramOfTrajectoriesAndLongest(20, false, 1);
        //liPolManTest.getHistorgramOfTrajectoriesAndLongest(20, true, 1);
        //int foldNr = 2;
        //liPolManTest.getWayRecordCounts("data/linear_interpolation/fold_" + foldNr + "/train_interpolation_result.csv");
        //liPolManTest.getWayRecordCounts(GenerationType.SPEEDPANELSGPS);
        //System.out.println(Parser.countLinesFile("data/speed_panels/append_train/train_interpolation_result"+foldNr+".csv"));
        //liPolManTest.getWayRecordCounts(GenerationType.GPS);
        Long n = Parser.countLinesFile("/home/andres/Documents/Thesis/Incident Detection Package/Source Code/Incident Detection (Java)/data/linear_interpolation/fold_1/train_interpolation_result.csv");
        System.out.println(n);
    }


}
