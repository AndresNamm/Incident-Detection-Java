package bgt.MapMatching;

import bgt.Model.Record;
import bgt.parsing.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by UDU on 8/5/2017.
 */
public class ParseMapMatchResults extends Parser {
    // GENERATES A ROUTES ELEMENT WITH
    // GRID ELEMENTS, AND SEGMENTS
    // RELIES ON

    public static  HashMap<String,Long> getSegmentsArCounts(File file) throws IOException {

        LineIterator it = FileUtils.lineIterator(file);
        String seg_id ="";
        HashMap<String,Long> segmentCounts = new HashMap<>();
        while(it.hasNext()){
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")){  // this line corresponds to a segment
                seg_id = st.nextToken();
                segmentCounts.put(seg_id, (long) 0);
            }else if (flag.equals("R")){ // this line belongs to a record
                segmentCounts.put(seg_id,segmentCounts.get(seg_id)+1);
            }
        }
        return segmentCounts;
    }

    public static HashMap<String,Long>  getSegmentAccCount(String fileName) throws IOException {
        File file = new File(fileName);
        LineIterator it = FileUtils.lineIterator(file);
        HashMap<String,Long> segmentCounts = new HashMap<>();
        while(it.hasNext()){
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line);
            String seg_id = st.nextToken();
            segmentCounts.put(seg_id, segmentCounts.containsKey(seg_id)?segmentCounts.get(seg_id)+1:1);
        }
        return segmentCounts;
    }
    public static void printSegmentCountsToFile(List<Integer> vals) throws IOException {
        FileWriter fw_result = new FileWriter("plots/SegmentRecordCounts.txt");
        for(Integer val : vals){
            fw_result.write(String.valueOf(val) + "\n");
        }
        fw_result.close();
    }

    public static List<String> getAllSegments(File file) throws IOException {
        LineIterator it = FileUtils.lineIterator(file);
        String seg_id ="";
        ArrayList<String> segIdList = new ArrayList<>();
        while(it.hasNext()){
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")){  // this line corresponds to a segment
                seg_id = st.nextToken();
                segIdList.add(seg_id);
                //segmentCounts.put(seg_id, (long) 0);
            }
        }
        return segIdList;
    }

    public static String printAllsegments(File file, int foldNr) throws IOException {
        List<String> segIdList = ParseMapMatchResults.getAllSegments(file);
        String fileName = "data/map_matching/fold_"+foldNr+"/SegmentID.txt";
        FileWriter fw_result = new FileWriter(fileName);
        for(String val : segIdList){
            fw_result.write(val + "\n");
        }
        fw_result.close();

        return fileName + "Printed out";
    }

    public static HashMap<String,HashMap<Integer,List<Record>>>  getSegmentsRecordsGroupedByDevices(List<String> segList, File file) throws IOException {
        HashMap<String,HashMap<Integer,List<Record>>> segDevs = new HashMap<>(); // Store all
        // SEGMENT - DEVICE ID LIST PAIRS , SO I CAN LATER ON GO THROUGH THE
        // RESULT FILES , FIND SEGMENTS -> THEN GENERATE A HASHMAP OF DEVICES AND GENERATE OBSERVATIONS
        // FROM EACH SEGMENT
        for(String segId : segList){
            segDevs.putIfAbsent(segId, new HashMap<Integer,List<Record>>());
        }

        String segId = null;
        String line = null;
        LineIterator it = FileUtils.lineIterator(file);
        boolean pass=true;
        while (it.hasNext()){
            line=it.nextLine();
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")) {  // this line corresponds to a segment
                segId = st.nextToken();
                if(segDevs.containsKey(segId)){
                    pass =false;
                }else{
                    pass=true;
                }
            }
            else if (flag.equals("R") && !pass){
                long rec_id = Long.valueOf(st.nextToken());
                int dev_id = Integer.valueOf(st.nextToken());
                int month = Integer.valueOf(st.nextToken());
                int day = Integer.valueOf(st.nextToken());
                double time = Double.valueOf(st.nextToken());
                String[] loc_coords = st.nextToken().split(",");
                double loc_lat = Double.valueOf(loc_coords[0]);
                double loc_lon = Double.valueOf(loc_coords[1]);
                double speed = Double.valueOf(st.nextToken());
                double direction = Double.valueOf(st.nextToken());
                Record rec = new Record(rec_id, dev_id, month, day,
                        time, loc_lat, loc_lon, speed, direction, segId);
                segDevs.get(segId).putIfAbsent(dev_id, new ArrayList<Record>());
                segDevs.get(segId).get(dev_id).add(rec);
            }
        }
        return segDevs;
    }

    public static ArrayList<Record>  getSegmentRecords(File file,String id) throws IOException {

        LineIterator it = FileUtils.lineIterator(file);
        String seg_id = "";
        ArrayList<Record> recordsList = new ArrayList<>();
        while (it.hasNext()) {
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")) {  // this line corresponds to a segment
                seg_id = st.nextToken();
                if (seg_id == id) {
                    while (it.hasNext()) {
                        line = it.next();
                        st = new StringTokenizer(line);
                        flag = st.nextToken();
                        if (flag.equals("R")) {
                            long rec_id = Long.valueOf(st.nextToken());
                            int dev_id = Integer.valueOf(st.nextToken());
                            //if(!deviceNrs.containsKey(dev_id)){continue;}
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

                            recordsList.add(rec);
                        } else {
                            return recordsList;
                        }
                    }
                }
            }   //segmentCounts.put(seg_id, (long) 0);
        }
        return recordsList;
    }

    public static void  main(String args[]) throws IOException {
        int foldNR=1;
        File f =  FileUtils.getFile("data/map_matching/fold_"+foldNR+"/train_match_result.txt");
        ParseMapMatchResults pMMR = new ParseMapMatchResults();
        //pMMR.printLinesFile(f,2000);
        pMMR.printAllsegments(f, foldNR);

    }
}
