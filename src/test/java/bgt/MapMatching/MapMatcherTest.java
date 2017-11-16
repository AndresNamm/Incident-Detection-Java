package bgt.MapMatching;

import bgt.Model.Node;
import bgt.Model.Record;
import bgt.Model.Segment;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by UDU on 7/31/2017.
 */
public class MapMatcherTest {
    @Test
    public void parseSegments() throws Exception {
        MapMatcher.SegmentStructures k = testMatcher.parseSegments(testMatcher.routes);
        HashMap<String, Integer> segCounts = new HashMap<>();
        for (Segment seg : k.segList){
            if(!segCounts.containsKey(seg.seg_id)){
                segCounts.put(seg.seg_id,0);
            }
            segCounts.put(seg.seg_id,segCounts.get(seg.seg_id)+1);
            if(segCounts.get(seg.seg_id)>1){
                assert(false);
            }
        }
        System.out.println(k.segMap);
    }

    MapMatcher testMatcher;
    @Before
    public void setUp()  throws FileNotFoundException, DocumentException {
        testMatcher = new MapMatcher();

    }

    @After
    public void tearDown() throws Exception {
    }


    private void checkDbFileOrder(String records_filename) throws IOException {
        List<Integer> dev_list = new ArrayList<>();
        System.out.println("Now processing: " + records_filename);
        Database db = DatabaseBuilder.open(new File(records_filename));
        Table table = db.getTable("TAXIDATA");
        Record prevRec = null;
        int failCount, goodCount, devFailCount,devGoodCount;
        devFailCount=devGoodCount=failCount=goodCount=0;
        for (Row row : table) {  //every row is record

            long rec_id = new Long((int) row.get("PosID"));
            int dev_id = Integer.valueOf((String) row.get("DevID"));
            if (dev_list.indexOf(dev_id) == -1) {
                dev_list.add(dev_id);
            }
            DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
            String date = df.format((Date) row.get("HkDt"));
            String[] dataFrags = date.split("/");
            int month = Integer.valueOf(dataFrags[1]);
            int day = Integer.valueOf(dataFrags[2]);

            DateFormat tf = new SimpleDateFormat("HH:mm:ss");
            String time = tf.format((Date) row.get("HkTm"));
            String[] timeFrags = time.split(":");
            double hour = Double.valueOf(timeFrags[0]);
            double minute = Double.valueOf(timeFrags[1]);
            double second = Double.valueOf(timeFrags[2]);
            hour = hour + minute / 60.0 + second / 3600.0;
            double lat = new Double((float) row.get("Lat"));
            double lon = new Double((float) row.get("Lon"));
            Node location = new Node(lat, lon);
            double speed = new Double((float) row.get("SpeedKmHr"));
            double direction = new Double((float) row.get("Direction"));
            Record record = new Record(rec_id, dev_id, month, day, hour, location,
                    speed, direction);
            if(prevRec!=null && prevRec.dev_id == record.dev_id){
                int compare = record.compareTo(prevRec);
                if(compare==-1) {
                    devFailCount++;
                    if(devFailCount<3){
                        System.out.println("current rec: " + record );
                        System.out.println("previous rec: " + prevRec );

                    }
                }else if (compare==1 ||compare==0 ){
                    devGoodCount++;
                }
            }

            if(prevRec!=null ){
                int compare = record.compareTo(prevRec);
                if(compare==-1) {
                    failCount++;
                }else if (compare==1 ||compare==0 ){
                    goodCount++;
                }
            }
            prevRec=record;
        }

        System.out.println("goodCount "+goodCount);
        System.out.println("failCount "+failCount);
        System.out.println("devGoodCount "+devGoodCount);
        System.out.println("devFailCount "+devFailCount);
    }

    @Test
    public void checkDBorder() throws IOException {
        for (int i = 0; i <= 3; i++){
            String records_filename = "";
            if (i < 10) {
                records_filename = "data/GPS_data/TaxiData20100" + i + ".mdb";
            }else {
                records_filename = "data/GPS_data/TaxiData2010" + i + ".mdb";
            }
            checkDbFileOrder(records_filename);
        }
    }

    @Test
    public void indexSegs() throws Exception {

    }

    private void printOutRecs( Record lastRec , Record rec, String failPoint){
        System.out.println("Failed with "+ failPoint);
        System.out.println("Old record " + lastRec);
        System.out.println("New record " + rec );
    }

    @Test
    public void parseRecords() throws IOException, DocumentException {
//TODO: UNCOMMENT THESE VALUES
        int foldNr = 2;

        MapMatcher mm = new MapMatcher();
        //mm.indexSegs();
        // mm.parseRecords("test", 0,12,1,true);
        mm.generateFullOutput(foldNr,"data/map_matching/testparse.txt");

        HashMap<String,String> resultVals = new HashMap<>();

        // Test that final output line does not contain newline
        String finalFileN = "data/map_matching/testparse.txt";
        BufferedReader br = new BufferedReader(new FileReader(finalFileN));
        String line = "";
        int fullFcounter = 0;
        while ((line = br.readLine()) != null) {
            resultVals.put(line,"");
            if(line.equals("")){
                System.out.println("empty line in final file");
                System.out.println(fullFcounter);
            }
            fullFcounter++;
        }
        br.close();

        int segRowCounter = 0;
        // Test that segment lines dont contain newlines
        for(Segment seg : mm.segMap.values()){
            String fileN = "data/map_matching/fold_"+foldNr+"/temp/"+seg.seg_id;
            br = new BufferedReader(new FileReader(fileN));
            line = "";
            int counter = 0;
            while ((line = br.readLine()) != null){
                //System.out.println(line);
                if(line.equals("")){
                    System.out.println("empty line in segment file" + seg.getSeg_id());
                    System.out.println(counter);
                }
                assert(resultVals.containsKey(line));
                counter++;
            }
            segRowCounter+=counter;
        }


        System.out.println("fullFcounter "+(fullFcounter-mm.segMap.keySet().size()));
        System.out.println("segRowCounter "+segRowCounter);
    }

    @Test
    public void parseRecordsTrajSegOrder() throws IOException {
        // this code goes through the map matching result files. reads in records. and stores the last record for each device.
        // it then compares if the segments are in correct order to each other
        int i = 1;
        String filename = "data/map_matching/fold_"+i+"/test_match_result.txt";
        // REGULAR MAPMATCHER RESULT PARSE CODE
        String seg_id = null;
        int cnt = 0;
        int c = 1;
        String line = null;
        File file = FileUtils.getFile(filename);
        LineIterator it = FileUtils.lineIterator(file);

        HashMap<Integer,Record> devLastRec = new HashMap<>();

        int segInOrderCount= 0;
        int orderCount = 0;
        boolean timeInorderExistance=false;
        ArrayList<String> timeInorder = new ArrayList<>();
        ArrayList<String> timeOrder = new ArrayList<>();

        while (it.hasNext()){
            line=it.nextLine();
            cnt++;
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")){  // this line corresponds to a segment
                seg_id = st.nextToken();
            }else if (flag.equals("R")){ // this line belongs to a record
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
                        time, loc_lat, loc_lon, speed, direction, seg_id);

                // Store this rec in trajsMap
                if (!devLastRec.containsKey(dev_id)){ // the first record of this device
                    devLastRec.put(dev_id, rec);
                }else{ // not the first record for device.
                    Record lastRec = devLastRec.get(dev_id);
                    Segment lastSeg = this.testMatcher.segMap.get(lastRec.seg_id);
                    Segment thisSeg = this.testMatcher.segMap.get(rec.seg_id);
                    if(thisSeg.way_id == lastSeg.way_id)
                    {
                        if(thisSeg.inner_id<lastSeg.inner_id){

                            //System.out.println(rec.time - lastRec.time );
                            segInOrderCount++;
                            //timeInorder.add("new " + rec + "\nold "+  lastRec);
                            //timeInorderExistance=true;
                        }else {
                            orderCount++;
                            //timeOrder.add("new " + rec + "\nold "+  lastRec);
                        }
                    }
                    devLastRec.put(dev_id,rec);
                }
            }
        }
        //System.out.println(timeInorder.get(0));
        System.out.println("Time correct " + orderCount);
        System.out.println("Time fails " + segInOrderCount);

        System.out.println("CORRECT EXAMPLE");
        //System.out.println(timeOrder.get(0));


        assert(!timeInorderExistance);
    }

    @Test
    public void parseRecordsTimeSegOrder() throws Exception {
        int i = 1;
        String filename = "data/map_matching/testparse.txt";
        // REGULAR MAPMATCHER RESULT PARSE CODE
        String seg_id = null;
        int cnt = 0;
        int c = 1;
        String line = null;
        File file = FileUtils.getFile(filename);
        LineIterator it = FileUtils.lineIterator(file);
        HashMap<Integer,Record> devLastRec = new HashMap<>();
        int timeInOrderCount= 0;
        int orderCount = 0;
        boolean timeInorderExistance=false;
        ArrayList<String> timeInorder = new ArrayList<>();
        ArrayList<String> timeOrder = new ArrayList<>();

        while (it.hasNext()){
            line=it.nextLine();
            cnt++;
            StringTokenizer st = new StringTokenizer(line);
            String flag = st.nextToken();
            if (flag.equals("S")){  // this line corresponds to a segment
                seg_id = st.nextToken();
            }else if (flag.equals("R")){ // this line belongs to a record
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
                        time, loc_lat, loc_lon, speed, direction, seg_id);



                // Store this rec in trajsMap
                if (!devLastRec.containsKey(dev_id)){ // the first record of this device
                    devLastRec.put(dev_id, rec);
                }else{ // not the first record for device.
                    Record lastRec = devLastRec.get(dev_id);
                    Segment lastSeg = this.testMatcher.segMap.get(lastRec.seg_id);
                    Segment thisSeg = this.testMatcher.segMap.get(rec.seg_id);
                    if(thisSeg.way_id == lastSeg.way_id)
                    {
                        if (rec.month == lastRec.month && rec.day == lastRec.day && rec.time < lastRec.time && Math.abs(rec.time - lastRec.time) <= (1.0 / 60.0)) {
                            timeInOrderCount++;
                            timeInorderExistance = true;
                            //&& Math.abs(rec.time - lastRec.time) <= (1.0 / 60.0)
                        } else if (rec.month == lastRec.month && rec.day == lastRec.day && rec.time > lastRec.time && Math.abs(rec.time - lastRec.time) <= (1.0 / 60.0)) {
                            orderCount++;
                            //timeOrder.add("new " + rec + "\nold "+  lastRec);
                        }
                    }
                    devLastRec.put(dev_id,rec);
                }
            }
        }

        System.out.println("Time correct " + orderCount);
        System.out.println("Time fails " + timeInOrderCount);
        assert(!timeInorderExistance);
    }

}