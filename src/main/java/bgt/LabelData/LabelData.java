package bgt.LabelData;

import bgt.LinearInterpolation.LinearInterpolation;
import bgt.MapMatching.MapMatcher;
import bgt.Model.Accident;
import bgt.Model.Record;
import bgt.Model.Segment;
import bgt.SpeedPanel.SpeedPanelObsParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.DocumentException;

import java.io.*;
import java.util.*;

/**
 * Created by admin on 2016/12/1.
 */
public class LabelData {
    class AccidentCounts{
        public int accident_rec_number=0;
        public int accident_recorded_numer=0;

    }

    public static final int AFFECTED_SEG_NUM_SEVERITY_2 = 3;
    public static final int AFFECTED_SEG_NUM_SEVERITY_3 = 2;
    public static final double LAST_HOUR_SEVERITY_2 = 3;
    public static final double LAST_HOUR_SEVERITY_3 = 2;
    //public List<Segment> segList;
    public HashMap<String, Segment> segsMap; //  store (seg_id, seg) pairs
    public HashMap<Integer, List<List<Record>>> trajsMap;   // store (dev_id, trajectories)



    public static int dateToHour(int month, int day) {
        int sum = 0;
        switch (month) {
            case 1:
                sum = 0;
                break;
            case 2:
                sum = 31;
                break;
            case 3:
                sum = 59;
                break;
            case 4:
                sum = 90;
                break;
            case 5:
                sum = 120;
                break;
            case 6:
                sum = 151;
                break;
            case 7:
                sum = 181;
                break;
            case 8:
                sum = 212;
                break;
            case 9:
                sum = 243;
                break;
            case 10:
                sum = 273;
                break;
            case 11:
                sum = 304;
                break;
            case 12:
                sum = 335;
                break;
        }
        sum += (day - 1);
        return sum * 24;
    }

    public void LoadRecsIntoSegs(int foldNr) throws IOException {
        // Load data into segments
        Iterator trajsItr = trajsMap.entrySet().iterator();// INTERPOLATED TRAJS ITERATOR
        while (trajsItr.hasNext()) {
            Map.Entry dev_trajs_pair = (Map.Entry) trajsItr.next();
            List<List<Record>> trajs = (List<List<Record>>) dev_trajs_pair.getValue();
            Iterator<List<Record>> trajItr = trajs.iterator();
            while (trajItr.hasNext()) {
                List<Record> traj = trajItr.next();
                Iterator<Record> recItr = traj.iterator();
                while (recItr.hasNext()) {
                    Record rec = recItr.next(); // LOADS NEWLY GENERATED RECORDS INTO SEGMENTS - PERHAPS ITS BETTER TO GENERATE A NEW MAP_MATCHING FILE // OH LUCKILY WITH test
                    // DATASET I DONT HAVE THIS ISSUE I THINK , BECAUSE LABELING HAPPENS ONLY ON test DATASET RIGHT?
                    Segment seg = segsMap.get(rec.seg_id);
                    seg.addRecord(rec);
                }
            }
        }

        // Sort the records in each segment according to date and time

        String fileN = "data/speed_panels/labeling/observations" + foldNr + ".csv";
        File file = FileUtils.getFile(fileN);
        LineIterator it = FileUtils.lineIterator(file);
        String segId;
        String time;
        String speed;
        String date;
        //HashMap<String,Integer> goodRecords = new HashMap<>();
        //HashMap<String,Integer> badRecords = new HashMap<>();
        while (it.hasNext()) {
            String line = it.nextLine();
            StringTokenizer st = new StringTokenizer(line, ",");
            segId = st.nextToken();
            time = st.nextToken();
            speed = st.nextToken();
            date = st.nextToken();
            Record rec = SpeedPanelObsParser.generateRecord(segId, time, speed, date);
            try {
                segsMap.get(rec.seg_id).addRecord(rec);
            } catch (Exception e) {
                System.out.println(line);
            }
        }
        Iterator<Segment> segItr = segsMap.values().iterator();
        while (segItr.hasNext()) {
            Segment seg = segItr.next();
            Collections.sort(seg.recordList);
        }
    }

    public void parseAccidents(String filename) throws FileNotFoundException, IOException {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(filename));
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String seg_id = st.nextToken();
            String[] seg_id_split = seg_id.split("_");
            int inner_id = Integer.valueOf(seg_id_split[1]);
            int severity = Integer.valueOf(st.nextToken());
            int month = Integer.valueOf(st.nextToken());
            int day = Integer.valueOf(st.nextToken());
            double time = Double.valueOf(st.nextToken());
            int affected_seg_num = (severity == 2 ? AFFECTED_SEG_NUM_SEVERITY_2 : AFFECTED_SEG_NUM_SEVERITY_3);
            for (int i = 0; i <= affected_seg_num; i++) {
                if (i == 0) {
                    Segment seg = segsMap.get(seg_id);
                    labelAccOnSegment(seg, severity, month, day, time);
                } else {
                    // Label the accident on upstream segement
                    String upstream_seg_id = seg_id_split[0] + (inner_id - i) + seg_id_split[2];
                    if (segsMap.containsKey(upstream_seg_id)) {
                        Segment upstream_seg = segsMap.get(upstream_seg_id);
                        labelAccOnSegment(upstream_seg, severity, month, day, time);
                    }
                    // Label the accident on downstream segment
                    String downstream_seg_id = seg_id_split[0] + (inner_id + i) + seg_id_split[2];
                    if (segsMap.containsKey(downstream_seg_id)) {
                        Segment downstream_seg = segsMap.get(downstream_seg_id);
                        labelAccOnSegment(downstream_seg, severity, month, day, time);
                    }
                }
            }
        }
        br.close();
        isr.close();
    }

    public void outputLabelingResult(String label_filename_prefix) throws IOException {
        FileWriter fw;
        HashMap<Long, List<Segment>> routeMap = new HashMap<>();

        List<Long> accRouteList = new ArrayList<>(); // List of route numbers with at least one incident
        List<Integer> routeAccRecNumList = new ArrayList<>();


        // Go thorugh all segments . Based on the ways of segments add a new
        // Way to routemMap with ArrayList of segments.Fill every
        // Way_id in map with Segments belonging to id.
        Iterator<Segment> segItr = segsMap.values().iterator();
        while (segItr.hasNext()) {
            Segment seg = segItr.next();
            long route_id = seg.way_id;
            List<Segment> routeSegList;
            if (!routeMap.containsKey(route_id)) {
                routeSegList = new ArrayList<>();
            } else {
                routeSegList = routeMap.get(route_id);
            }
            routeSegList.add(seg);// CATEGORIZES SEGMENTS INTO WAYMAP
            routeMap.put(route_id, routeSegList);
        }


        boolean route_acc_flag; // whether there is an accident on the route
        for (Map.Entry<Long, List<Segment>> route_segList_pair : routeMap.entrySet()) {//GOES THROUGH ALL THE WAYS
            long route_id = route_segList_pair.getKey();
            int route_acc_rec_num = 0;
            route_acc_flag = false;
            fw = new FileWriter(label_filename_prefix + "_" + route_segList_pair.getKey() + ".txt");
            List<Segment> routeSegList = route_segList_pair.getValue();
            segItr = routeSegList.iterator();
            while (segItr.hasNext()) { // GOES THROUGH ALL SEGMENTS IN WAY
                Segment seg = segItr.next();
                Segment[] segsByHour = seg.splitSegByHour(); // ADDS TEMPORAL CONSTRAINT, DIVIDES LOCALIZED SEGMENTS INTO 24 HOURS
                for (int i = 0; i < 24; i++) {
                    Segment segByHour = segsByHour[i];
                    List<Record> recList = segByHour.recordList; // FOR EACH HOUR SEGMENT, GETS ITS RECORD LIST
                    int prev_month = 0, prev_day = 0;
                    Iterator<Record> recItr = recList.iterator();
                    while (recItr.hasNext()) { // FOR EACH RECORD  WRITES DOWN
                        Record rec = recItr.next();
                        if (rec.acc_flag == 1) {  // the first positive record
                            route_acc_rec_num++;
                            if (!route_acc_flag) {
                                route_acc_flag = true;
                                accRouteList.add(route_id);
                            }
                        }
                        if (prev_month != 0 && prev_day != 0 &&
                                (prev_month != rec.month || prev_day != rec.day)) {
                            fw.write("\n");
                        }
                        fw.write(segByHour.getSeg_id() + "\t" + rec.time + "\t" + Math.round(rec.speed) + "\t" + rec.acc_flag + "\n");
                        prev_month = rec.month;
                        prev_day = rec.day;
                    }
                }
            }
            if (route_acc_flag) routeAccRecNumList.add(route_acc_rec_num);
            fw.close();
        }


        fw = new FileWriter(label_filename_prefix + "_list.txt");
        for (int i = 0; i < accRouteList.size(); i++) {
            fw.write(accRouteList.get(i) + "\t" + routeAccRecNumList.get(i) + "\n");
        }
        fw.close();
    }

    public void labelAccOnSegment(Segment seg, int severity, int month, int day, double time) {
        // Label the accident on records in a segment
        double acc_last_time = (severity == 2 ? LAST_HOUR_SEVERITY_2 : LAST_HOUR_SEVERITY_3);
        double acc_time = (double) dateToHour(month, day) + time;

        List<Record> recList = seg.recordList;
        //System.out.println(seg.recordList);
        Iterator<Record> recItr = recList.iterator();
        while (recItr.hasNext()) {
            Record rec = recItr.next();
            double rec_time = (double) dateToHour(rec.month, rec.day) + rec.time;
            if (rec_time < acc_time) {    // severity 2: 20min; severity 3: 30min
                continue;
            } else if ((rec_time >= acc_time) && (rec_time <= (acc_time + acc_last_time))) {
                rec.acc_flag = 1;
                //System.out.println("Suxxess");
            } else if (rec_time > (acc_time + acc_last_time)) {
                break;
            }
        }
    }

    public void readInInterpolatedRecords(int foldNr, HashMap<String,String> control) throws IOException {
        String fName = "data/linear_interpolation/fold_" + foldNr + "/test_interpolation_result.csv";
        File file = FileUtils.getFile(fName);
        LineIterator it = FileUtils.lineIterator(file);
        String time;
        String speed;
        String date;
        String segId;
        while (it.hasNext()) {
            String line = it.nextLine();
            StringTokenizer st = new StringTokenizer(line, ",");
            segId = st.nextToken();
            if(!control.containsKey(segId)){continue;}
            Record rec = LinearInterpolation.generateTestRec(line);
            segsMap.get(segId).addRecord(rec);

        }
    }

    public void readInSpeedPanelRecords(String segId, int foldNr) throws IOException {
        String fName = "data/speed_panels/labeling/fold_" + foldNr + "/" + segId;
        try {
            File file = FileUtils.getFile(fName);
            // System.out.println("Read in observations for " + fName);
            LineIterator it = FileUtils.lineIterator(file);
            String time;
            String speed;
            String date;
            //HashMap<String,Integer> goodRecords = new HashMap<>();
            //HashMap<String,Integer> badRecords = new HashMap<>();
            while (it.hasNext()) {
                String line = it.nextLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                st.nextToken();
                time = st.nextToken();
                speed = st.nextToken();
                date = st.nextToken();
                Record rec = SpeedPanelObsParser.generateRecord(segId, time, speed, date);
                segsMap.get(segId).addRecord(rec);
            }
        } catch (IOException io) {

        }
    }

    class GenerateWaySegments {
        public HashMap<String, List<Segment>> accWaySegMap;  // WAY-id , Way segments pair for ways which have had accidents recorded on them.
        public HashMap<String, List<Accident>> segAcc; // Seg acc pairs
        public HashMap<String, String> wayMap; // Ways that have accidents

        GenerateWaySegments(String fileName) throws IOException {
            this.segAcc = segAccGenerator(fileName);
            this.wayMap = getAccWays(segAcc);
            this.accWaySegMap = getAccWaySegMap(wayMap);
        }

        public HashMap<String, List<Accident>> segAccGenerator(String fileName) throws IOException {
            HashMap<String, List<Accident>> segAcc = new HashMap<>();
            InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName));
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int cnt = 0;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                String seg_id = st.nextToken();
                String[] seg_id_split = seg_id.split("_");
                int inner_id = Integer.valueOf(seg_id_split[1]);
                int severity = Integer.valueOf(st.nextToken());
                int month = Integer.valueOf(st.nextToken());
                int day = Integer.valueOf(st.nextToken());
                double time = Double.valueOf(st.nextToken());

                //int affected_seg_num = (severity==2?AFFECTED_SEG_NUM_SEVERITY_2:AFFECTED_SEG_NUM_SEVERITY_3);
                Accident acc = new Accident(cnt++, severity, month, day, time, inner_id);
                if (!segAcc.containsKey(seg_id)) {
                    segAcc.put(seg_id, new ArrayList<Accident>());
                }
                segAcc.get(seg_id).add(acc);
            }
            return segAcc;

        }

        public HashMap<String, String> getAccWays(HashMap<String, List<Accident>> segAcc) {
            HashMap<String, String> wayMap = new HashMap<>();
            segAcc.forEach((segId, arr) -> wayMap.put(segId.split("_")[0], ""));
            return wayMap;
        }

        public HashMap<String, List<Segment>> getAccWaySegMap(HashMap<String, String> wayMap) {
            HashMap<String, List<Segment>> accWaySegMap = new HashMap<>();
            for (String wayId : wayMap.keySet()) {
                int cnt = 0;
                accWaySegMap.put(wayId, new ArrayList<>());
                while (LabelData.this.segsMap.containsKey(wayId + "_" + String.valueOf(cnt) + "_0")) {
                    accWaySegMap.get(wayId).add(LabelData.this.segsMap.get(wayId + "_" + String.valueOf(cnt) + "_0"));
                    cnt++;
                }
            }
            return accWaySegMap;
        }

    }
    /**
     * @param foldNr
     * @throws IOException THIS FUNCTION IS THE CENTRE OF LABELING. LABELING STORES EVERY RECORD INT segsMap and every datastructure that deals with segments
     *                     refers obtains the mutable segments from segsMap. This means when I delete elements from here, the elements will be deleted everywhere.
     *                     This allows me to go through the labelin way by way. Load in records to segments under this way and then delete all the segment records from
     *                     segsMap
     *                     1. IT READS IN ALL THE WAYS THAT HAVE ACCIDENTS RECORDED ON THEM
     *                     2. NOW IT
     */
    public void performLabeling(int foldNr, Boolean addSpeedPanels, Boolean addGps) throws IOException {
        FileUtils.cleanDirectory(new File("data/label_data/fold_" + foldNr + "/"));
        GenerateWaySegments waySegments = new GenerateWaySegments("data/map_matching/acc_match_result.txt");
        HashMap<String, List<Segment>> accWaySegs = waySegments.accWaySegMap;
        System.out.println(accWaySegs.size());
        int bucketSize = 100;
        List<HashMap<String,String>> wayBuckets = genBuckets(accWaySegs, bucketSize);
        for(HashMap<String,String> bucket: wayBuckets){
            HashMap<String,String> buckSegs = new HashMap<>();
            bucket.forEach((key,value)-> accWaySegs.get(key).forEach((seg)-> buckSegs.put(seg.seg_id,"") ));// biiutiful double loop
            if(addGps){
                readInInterpolatedRecords(foldNr,buckSegs);
            }
            for (String wayId : bucket.keySet()) {
                System.out.println(" Labeling way: " + wayId);
                if(addSpeedPanels){
                    for (Segment seg : accWaySegs.get(wayId)) {
                        readInSpeedPanelRecords(seg.seg_id, foldNr);
                    }
                }
                for (Segment seg : accWaySegs.get(wayId)) {

                    if (waySegments.segAcc.containsKey(seg.seg_id) && seg.recordList.size()>0) {
                        Collections.sort(seg.recordList);
                        //System.out.println(seg.seg_id);
                        List<Accident> accList = waySegments.segAcc.get(seg.seg_id);
                        for (Accident acc : accList) {
                            String segId = seg.seg_id;
                            String[] seg_id_split = segId.split("_");
                            int inner_id = acc.inner_id;
                            int severity = acc.severity;
                            int month = acc.month;
                            //System.out.println(month);
                            int day = acc.day;
                            double time = acc.acc_time;
                            //System.out.println(time);
                            int affected_seg_num = (severity == 2 ? AFFECTED_SEG_NUM_SEVERITY_2 : AFFECTED_SEG_NUM_SEVERITY_3);
                            for (int i = 0; i <= affected_seg_num; i++) {
                                if (i == 0) {

                                    labelAccOnSegment(seg, severity, month, day, time);
                                } else {
                                    // Label the accident on upstream segement
                                    String upstream_seg_id = wayId + (inner_id - i) + "_" + seg_id_split[2];
                                    if (segsMap.containsKey(upstream_seg_id)) {
                                        Segment upstream_seg = segsMap.get(upstream_seg_id);
                                        labelAccOnSegment(upstream_seg, severity, month, day, time);
                                    }
                                    // Label the accident on downstream segment
                                    String downstream_seg_id = wayId + (inner_id + i) + "_" + seg_id_split[2];
                                    if (segsMap.containsKey(downstream_seg_id)) {
                                        Segment downstream_seg = segsMap.get(downstream_seg_id);
                                        labelAccOnSegment(downstream_seg, severity, month, day, time);
                                    }
                                }
                            }
                        }
                    }
                }
                AccidentCounts accCount = printResult(wayId, foldNr, accWaySegs.get(wayId));
                if (accCount.accident_rec_number != 0) {
                    FileWriter fw = new FileWriter("data/label_data/fold_" + foldNr + "/label_result_list.txt", true);
                    fw.write(wayId + "\t" + accCount.accident_rec_number + "\t"+ accCount.accident_recorded_numer + "\n");
                    fw.close();
                }
                for(Segment seg : accWaySegs.get(wayId)){
                    seg.recordList.clear();
                }
            }
        }
    }

    private List<HashMap<String,String>> genBuckets(HashMap<String, List<Segment>> accWaySegs, int bucketSize) {
        int maxSize = accWaySegs.size();
        int nrOfBuckets = maxSize/ bucketSize;
        List<HashMap<String,String>> buckets  = new ArrayList<>();
        int count = 0;
        for(String key : accWaySegs.keySet()){
            if(count%bucketSize==0){
                buckets.add(new HashMap<>());
            }
            buckets.get(buckets.size()-1).put(key,key);
            count++;
        }
        return buckets;
    }

    public AccidentCounts printResult(String wayId, int foldNr, List<Segment> waySegList) throws IOException {
        FileWriter fw;
        String label_filename_prefix = "data/label_data/fold_" + foldNr + "/label_result";
        int route_acc_rec_num = 0;
        int route_acc_recorded_num = 0;
        boolean way_acc_flag = false;
        fw = new FileWriter(label_filename_prefix + "_" + wayId + ".txt");
        Iterator<Segment> segItr = waySegList.iterator();
        while (segItr.hasNext()) { // GOES THROUGH ALL SEGMENTS IN WAY
            Segment seg = segItr.next();
            Segment[] segsByHour = seg.splitSegByHour(); // ADDS TEMPORAL CONSTRAINT, DIVIDES LOCALIZED SEGMENTS INTO 24 HOURS
            for (int i = 0; i < 24; i++)
            {
                boolean accCounted = false;
                fw.write("\n");
                Segment segByHour = segsByHour[i];
                List<Record> recList = segByHour.recordList; // FOR EACH HOUR SEGMENT, GETS ITS RECORD LIST
                int prev_month = 0, prev_day = 0;
                Iterator<Record> recItr = recList.iterator();
                while (recItr.hasNext()) { // FOR EACH RECORD  WRITES DOWN
                    Record rec = recItr.next();
                    if (rec.acc_flag == 1) {  // the first positive record
                        route_acc_rec_num++;
                        if(!accCounted){
                            accCounted=true;
                            route_acc_recorded_num++;
                        }
                        if (!way_acc_flag) {
                            way_acc_flag = true;
                        }
                    }
                    if (prev_month != 0 && prev_day != 0 &&
                            (prev_month != rec.month || prev_day != rec.day)) {
                        accCounted=false;
                        fw.write("\n");
                    }
                    fw.write(segByHour.getSeg_id() + "\t" + rec.time + "\t" + Math.round(rec.speed) + "\t" + rec.acc_flag + "\n");
                    prev_month = rec.month;
                    prev_day = rec.day;
                }
            }
        }
        ;
        fw.close();
        AccidentCounts accCounts = new AccidentCounts();
        accCounts.accident_rec_number=route_acc_rec_num;
        accCounts.accident_recorded_numer= route_acc_recorded_num;
        return accCounts;
    }

    public static void main(String[] args) throws DocumentException, IOException, InterruptedException {
        // Parse segments for interpolation
        for (int i = 1; i <= 4; i++) {
            // Parse segments for interpolation
            System.out.println("Now fold: " + i);
            MapMatcher mm = new MapMatcher();
            mm.indexSegs();
            // LABEL DATA
            LabelData ld = new LabelData();
            ld.segsMap = mm.segMap;
            ld.performLabeling(i, true, false);
        }
    }
}
