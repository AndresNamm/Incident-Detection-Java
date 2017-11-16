package bgt.MapMatching;

import bgt.Model.Accident;
import bgt.Model.Record;
import bgt.Model.*;
import bgt.parsing.OSMparser;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MapMatcher {

    public class SegmentStructures {

        public final List<Segment> segList;
        public final HashMap<String, Segment> segMap;

        SegmentStructures(List<Segment> segList, HashMap<String, Segment> segMap) {
            this.segList = segList;
            this.segMap = segMap;
        }

    }
    public final double minAcceptableDist = 100.0;
    public final double minAcceptableDistAccidents = 25.0;
    public final double minAcceptableDeg = 45.0;
    public double MIN_LAT;
    public double MIN_LON;
    public double MAX_LAT;
    public double MAX_LON;
    public double D_LAT = 56021.5; // in meters
    public double D_LON = 73162.1; // in meters
    public final double GRID_SIZE = 100;
    public int MAX_LAT_INDEX;
    public int MAX_LON_INDEX;
    public List<Segment> segList;
    public HashMap<String, Segment> segMap;
    public List<String>[][] segIDGrid;

    //Andres added elements
    public final Routes routes;
    public Grid grid;

    public MapMatcher() throws FileNotFoundException, DocumentException {
        this("data/route_segmentation/Hong_Kong-result.osm");
    }

    public MapMatcher(String fileName) throws DocumentException, FileNotFoundException {
        // Initiate SEGMENTS - THESE ARE JUST SUBINSTANCES OF WAYS
        this.routes = OSMparser.parseNodesAndWays(fileName);
        //Iterator<Node> nodesIterator = routes.getNodeList().iterator();
        SegmentStructures segmentStructures = this.parseSegments(routes);
        this.segList = segmentStructures.segList;
        this.segMap = segmentStructures.segMap;
        //INITIATE GRID VARIABLES
        this.MIN_LAT = routes.getBoundaries().getMinlat();
        this.MAX_LAT = routes.getBoundaries().getMaxlat();
        this.MIN_LON = routes.getBoundaries().getMinlon();
        this.MAX_LON = routes.getBoundaries().getMaxlon();
        this.D_LAT = Node.calcDist(this.MIN_LAT, this.MIN_LON, this.MAX_LAT, this.MIN_LON);
        this.D_LON = Node.calcDist(this.MIN_LAT, this.MIN_LON, this.MIN_LAT, this.MAX_LON);
        System.out.println(this.toString());
    }

    public SegmentStructures parseSegments(Routes routes) {
        List<Segment> segList = new ArrayList<>();
        HashMap<String, Segment> segMap = new HashMap<>();
        Iterator<Way> wayItr = routes.getWayList().iterator();
        while (wayItr.hasNext()) {
            Way way = wayItr.next();
            List<Node> nodeList = way.getNodeList();
            for (int i = 0; i + 1 < nodeList.size(); i++) {
                Node startNode = nodeList.get(i);
                Node endNode = nodeList.get(i + 1);
                Segment seg = new Segment(way.id, i, startNode, endNode);
                segList.add(seg);
                segMap.put(seg.getSeg_id(), seg);
            }
        }
        SegmentStructures segmentStructures = new SegmentStructures(segList, segMap);
        return segmentStructures;
    }
    // PLACES ALL SEGMENTS TO GRID.
    public void indexSegs() {
        // index the segments into grids
        MAX_LAT_INDEX = (int) (D_LAT / GRID_SIZE);// CREATES #GRIDSIZE ELEMENTS FROM NORTH TO SOUTH
        int numOfLatGrids = MAX_LAT_INDEX + 1;
        MAX_LON_INDEX = (int) (D_LON / GRID_SIZE);
        int numOfLonGrids = MAX_LON_INDEX + 1;
        List<String>[][] segIDGrid;
        segIDGrid = new List[numOfLatGrids][numOfLonGrids];
        grid = new Grid(numOfLatGrids, numOfLonGrids, this.routes.getBoundaries());
        for (int i = 0; i < numOfLatGrids; i++) {
            for (int j = 0; j < numOfLonGrids; j++) {
                segIDGrid[i][j] = new ArrayList<>();
                double lon_min = this.MIN_LON + (Double.valueOf(j) / Double.valueOf(numOfLonGrids)) * (this.MAX_LON - this.MIN_LON);
                double lon_max = this.MIN_LON + ((Double.valueOf(j) + 1.0) / Double.valueOf(numOfLonGrids)) * (this.MAX_LON - this.MIN_LON);
                double lat_min = this.MIN_LAT + (Double.valueOf(i) / Double.valueOf(numOfLatGrids)) * (this.MAX_LAT - this.MIN_LAT);
                double lat_max = this.MIN_LAT + ((Double.valueOf(i) + 1.0) / Double.valueOf(numOfLatGrids)) * (this.MAX_LAT - this.MIN_LAT);
                GridElement gridElement = new GridElement(lon_min, lon_max, lat_min, lat_max);
                grid.setListGridElement(i, j, gridElement);
            }
        }

        Iterator<Segment> segItr = segList.iterator();
        while (segItr.hasNext()) {
            Segment seg = segItr.next();
            Node sNode = seg.getStartNode();
            int sLatIndex = (int) (Node.calcDist(sNode.getLat(), MIN_LON, MIN_LAT, MIN_LON) / GRID_SIZE);
            int sLonIndex = (int) (Node.calcDist(MIN_LAT, sNode.getLon(), MIN_LAT, MIN_LON) / GRID_SIZE);
            Node eNode = seg.getEndNode();
            int eLatIndex = (int) (Node.calcDist(eNode.getLat(), MIN_LON, MIN_LAT, MIN_LON) / GRID_SIZE);
            int eLonIndex = (int) (Node.calcDist(MIN_LAT, eNode.getLon(), MIN_LAT, MIN_LON) / GRID_SIZE);

            int minLatIndex = sLatIndex < eLatIndex ? sLatIndex : eLatIndex;
            int maxLatIndex = sLatIndex > eLatIndex ? sLatIndex : eLatIndex;

            int minLonIndex = sLonIndex < eLonIndex ? sLonIndex : eLonIndex;
            int maxLonIndex = sLatIndex > eLatIndex ? sLonIndex : eLonIndex;
            for (int i = minLatIndex; i <= maxLatIndex; i++) {
                for (int j = minLonIndex; j <= maxLonIndex; j++) {
                    segIDGrid[i][j].add(seg.getSeg_id());
                    grid.getListGrid()[i][j].addSegSegList(seg);
                }
            }
        }
        this.segIDGrid = segIDGrid;
    }

    public String mapRecord(Record record) {
        Node recNode = record.getLocation();
        // THIS PART MAPS THE THING INTO THE RIGHT PLACE IN GRID - MAPS IT INTO 3 PLACES IN THE GRID  -- TOTALING 300 * 300 MS
        int latIndex = (int) (Node.calcDist(recNode.getLat(), MIN_LON, MIN_LAT, MIN_LON) / GRID_SIZE);
        int lonIndex = (int) (Node.calcDist(MIN_LAT, recNode.getLon(), MIN_LAT, MIN_LON) / GRID_SIZE);

        int minLatIndex = latIndex - 1 >= 0 ? latIndex - 1 : 0;
        int maxLatIndex = latIndex + 1 <= MAX_LAT_INDEX ? latIndex + 1 : MAX_LAT_INDEX;

        int minLonIndex = lonIndex - 1 >= 0 ? lonIndex - 1 : 0;
        int maxLonIndex = lonIndex + 1 <= MAX_LON_INDEX ? lonIndex + 1 : MAX_LON_INDEX;
        // THIS PART MAPS THE THING INTO THE RIGHT PLACE IN GRID

        List<String> checkedSegIDList = new ArrayList<>();
        double minDist = minAcceptableDist+1;
        String minSegID = null;
        for (int i = minLatIndex; i <= maxLatIndex; i++) {
            for (int j = minLonIndex; j <= maxLonIndex; j++) {// ASSIGNS TO CLOSEST SEGMENT IN GRID
                // map the segments in each grid
                List<String> segIDList = this.segIDGrid[i][j];// here are the segments that belong to the specific gridelement . these segments are mapped in indexSegs()
                for (String segID : segIDList) {
                    if (checkedSegIDList.indexOf(segID) == -1) {// OII
                        // this segment has not been checked
                        Segment seg = this.segMap.get(segID);
                        if (seg.calcRecordDirectDist(record) > minAcceptableDeg) {
                            continue;
                        } else {
                            double dist = seg.calcNodeDist(recNode);
                            if (dist < minDist) {
                                minDist = dist;
                                minSegID = segID;
                            }
                        }
                    }
                }
            }
        }

        if (minDist < minAcceptableDist) {
            // there exists a segment that has a distance less than 100 from the record,
            // and their direction difference is less than 45 degree.
            this.segMap.get(minSegID).addRecord(record); // adds TRAFFIC RECORD TO CLOSEST SEGMENT
            return minSegID;
        }
        return "";
    }

    public void parseRecords(String flag, int start_fileNum, int end_fileNum) throws IOException {
        parseRecords(flag, start_fileNum, end_fileNum, 1,false);
    }

    public long parseRecords(String flag, int start_fileNum, int end_fileNum, int foldNr,boolean justTest) throws IOException {
        // map the record into the segments
        long start = System.currentTimeMillis();
        createEmptySegmentFiles(foldNr);
        int lastFileNr = justTest ? 2 : 52;
        for (int i = 0; i <= lastFileNr; i++) {
            if (flag.equals("train") && (i >= start_fileNum) && (i <= end_fileNum)) {
                continue;
            }
            if (flag.equals("test") && ((i < start_fileNum) || (i > end_fileNum))) {
                continue;
            }
            // NAMING PART
            String records_filename = "";
            if (i < 10) {
                records_filename = "data/GPS_data/TaxiData20100" + i + ".mdb";
            }else {
                records_filename = "data/GPS_data/TaxiData2010" + i + ".mdb";
            }

            parseRecordsOfFile(records_filename);// transfers db row into record and adds this record to some segment recordlist.
            appendIntermediateResultsFlush(foldNr);
            printElapsedTime(start);
        }
        System.out.println("Fold " + foldNr + " took:");
        printElapsedTime(start);
        return printElapsedTime(start);

    }

    public static long printElapsedTime(long start){
        long elapsedTimeMillis = System.currentTimeMillis()-start;
        float elapsedTimeSec = elapsedTimeMillis/1000F;
        float elapsedTimeMin = elapsedTimeMillis/(60*1000F);
        System.out.println(elapsedTimeMillis + " ms");
        System.out.println(elapsedTimeSec+ "sec");
        System.out.println(elapsedTimeMin+ "min");
        return elapsedTimeMillis;
    }

    public void appendIntermediateResultsFlush(int foldNr) throws IOException {
        for(Segment seg : this.segMap.values()){
            if(seg.recordList.size()>0){
                FileWriter fw = new FileWriter("data/map_matching/fold_"+foldNr+"/temp/"+seg.seg_id, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw);
                for(Record rec : seg.recordList){
                    out.println(recToOutput(rec));
                }
                out.close();
                seg.recordList.clear();
            }
        }
    }

    public void createEmptySegmentFiles(int foldNr) throws IOException {
        FileUtils.cleanDirectory(new File("data/map_matching/fold_"+foldNr+"/temp/"));
        for(Segment seg : this.segMap.values()){
            File f = new File("data/map_matching/fold_"+foldNr+"/temp/"+seg.seg_id);
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
    };

    public void parseRecordsOfFile(String records_filename) throws IOException {
        List<Integer> dev_list = new ArrayList<>();
        System.out.println("Now processing: " + records_filename);
        Database db = DatabaseBuilder.open(new File(records_filename));
        Table table = db.getTable("TAXIDATA");
        int counter = 0;
        int matchCounter=0;
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

            if(!mapRecord(record).equals("")){
                matchCounter++;
            };
            counter++;

        }
        System.out.println("Device number: " + dev_list.size());
        printOutMathcingPercentages(matchCounter,counter);
        db.close();
    }

    // MAPRECORD, PARSERECORD - MAPS EACHT TRAFFIC OBSERVATION INTO 1 SEGMENT -- IS KINDA SLOW. COULD USE MAP IN SPECIFIC PLASE
    public void mapAccident(Accident acc) {
        // map one accident onto a road segment
        Node accNode = acc.getAcc_location();
        int latIndex = (int) (Node.calcDist(accNode.getLat(), MIN_LON, MIN_LAT, MIN_LON) / GRID_SIZE);
        int lonIndex = (int) (Node.calcDist(MIN_LAT, accNode.getLon(), MIN_LAT, MIN_LON) / GRID_SIZE);

        int minLatIndex = latIndex - 1 >= 0 ? latIndex - 1 : 0;
        int maxLatIndex = latIndex + 1 <= MAX_LAT_INDEX ? latIndex + 1 : MAX_LAT_INDEX;  // 3 grid

        int minLonIndex = lonIndex - 1 >= 0 ? lonIndex - 1 : 0;
        int maxLonIndex = lonIndex + 1 <= MAX_LON_INDEX ? lonIndex + 1 : MAX_LON_INDEX;  // 3 grid

        List<String> checkedSegIDList = new ArrayList<>();
        double minDist = this.minAcceptableDistAccidents+1;
        String minSegID = null;

        for (int i = minLatIndex; i <= maxLatIndex; i++) {
            for (int j = minLonIndex; j <= maxLonIndex; j++) {
                // map the segments in each grid
                List<String> segIDList = this.segIDGrid[i][j];
                for (String segID : segIDList) {
                    if (checkedSegIDList.indexOf(segID) == -1) {
                        // this segment has not been checked
                        Segment seg = this.segMap.get(segID);
                        double dist = seg.calcNodeDist(accNode);
                        if (dist < minDist) {
                            minDist = dist;
                            minSegID = segID;
                        }
                    }
                }
            }
        }

        if (minDist < this.minAcceptableDistAccidents) {
            // there exists a segment that has a distance less than 100 from the record,
            // and their direction difference is less than 45 degree.
            segMap.get(minSegID).addAcc(acc);  //
        }
    }

    public void parseAccidents() throws IOException, BiffException {
        // read accident data, parse them and map them onto road segments
        Workbook book = Workbook.getWorkbook(new File("data/map_matching/acc_info.xls"));
        Sheet sheet = book.getSheet(0);
        int numOfAcc = sheet.getRows() - 2;
        System.out.println(numOfAcc);
        for (int i = 1; i < numOfAcc + 1; i++) {
            String acc_no_str = sheet.getCell(0, i).getContents();
            int acc_no = Integer.valueOf(acc_no_str);

            String severity_str = sheet.getCell(1, i).getContents();
            int severity = Integer.valueOf(severity_str);

            String date_str = sheet.getCell(2, i).getContents();
            String[] date_split = date_str.split("/");
            int month = Integer.valueOf(date_split[0]);
            System.out.println("Month: " + month);
            int day = Integer.valueOf(date_split[1]);

            String acc_time_str = sheet.getCell(3, i).getContents();
            double acc_time = Double.valueOf(acc_time_str.substring(0, 2)) + Double.valueOf(acc_time_str.substring(2, 4)) / 60.0;

            String lon_str = sheet.getCell(4, i).getContents();
            if (lon_str.equals("---")) continue;
            String lat_str = sheet.getCell(5, i).getContents();
            Node acc_loc = new Node(Double.valueOf(lat_str), Double.valueOf(lon_str));

            Accident acc = new Accident(acc_no, severity, month, day, acc_time, acc_loc);

            mapAccident(acc);
            System.out.println(i);
        }
    }

    /// parseAccidents and mapAccident maps the accident to closest segment .,.

    public void generateFullOutput(int foldNr, String record_match_filename ) throws IOException {
        try{
            Files.delete(new File(record_match_filename).toPath());
        }catch (NoSuchFileException e){
            System.out.println("No such file exists");
        }
        FileWriter fw = new FileWriter(record_match_filename);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);
        for(Segment seg : this.segList){
            String fileN = "data/map_matching/fold_"+foldNr+"/temp/"+seg.seg_id;


            BufferedReader br = new BufferedReader(new FileReader(fileN));
            out.println("S"+"\t"+ seg.seg_id);
            String seg_id = seg.seg_id;
            String line = "";
            while ((line = br.readLine()) != null){
                StringTokenizer st = new StringTokenizer(line);
                st.nextToken();
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
                //out.println(line);
                seg.addRecord(rec);
            }
            long sortStart = System.currentTimeMillis();
            Collections.sort(seg.recordList);
            long elapsedTimeMillis = System.currentTimeMillis()-sortStart;
//            System.out.println("Sort took: ");
//            printElapsedTime(sortStart);
            int goodCount, failCount;
            goodCount=failCount=0;
            if(seg.recordList.size()>0){
                out.println(recToOutput(seg.recordList.get(0)));
            }
            for(int i = 1; i<seg.recordList.size();i++){
                Record currentRecord = seg.recordList.get(i);
                Record previousRecord = seg.recordList.get(i-1);
                int compare = currentRecord.compareTo(previousRecord);
                if(compare== 1 || compare==0){
                    goodCount++;
                }else {
                    failCount++;
                }
                out.println(recToOutput(currentRecord));
            }
            seg.recordList.clear();
            System.out.println("goodCount"+goodCount);
            System.out.println("failCount"+failCount);

        }
        out.close();
    }

    public static String recToOutput(Record rec){
        String line =
                "R" + "\t" +
                        rec.rec_id + "\t" +
                        rec.dev_id + "\t" +
                        rec.month + "\t" +
                        rec.day + "\t" +
                        rec.time + "\t" +
                        rec.getLocation().getLat() + "," + rec.getLocation().getLon() + "\t" +
                        rec.speed + "\t" +
                        rec.getDirection();
        return line;
    }

    public void printOutMathcingPercentages(int matchCounter, int allCounter){
        System.out.println("All: "+allCounter+" Matched: "+ matchCounter +" Percentage: "+ Double.valueOf(matchCounter)/Double.valueOf(allCounter)*100);
    }

    public void outputAccResults(String acc_match_filename) throws IOException {
        FileWriter fw_acc_result = new FileWriter(acc_match_filename);
        Iterator<Segment> segItr = segList.iterator();
        while (segItr.hasNext()) {
            Segment seg = segItr.next();
            if (seg.accNum() > 0) {
                Iterator<Accident> accItr = seg.accList.iterator();
                while (accItr.hasNext()) {
                    Accident acc = accItr.next();
                    fw_acc_result.write(
                            seg.seg_id + "\t" + acc.severity + "\t" + acc.month + "\t" +
                                    acc.day + "\t" + acc.acc_time + "\t" +
                                    acc.getAcc_location().getLat() + "," + acc.getAcc_location().getLon() + "\n"
                    );
                }
            }
        }
        fw_acc_result.close();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("MIN_LAT " + String.valueOf(this.MIN_LAT));
        str.append("this.MAX_LAT " + String.valueOf(this.MAX_LAT));
        str.append("this.MIN_LON " + String.valueOf(this.MIN_LON));
        str.append("this.MAX_LON " + String.valueOf(this.MAX_LON));
        str.append("this.D_LAT " + String.valueOf(this.D_LAT));
        str.append("this.D_LON " + String.valueOf(this.D_LON));
        return str.toString();
    }

    public static void main(String[] args) throws DocumentException, IOException, BiffException {
        // fold 1: 0 12
        // fold 2: 13 25
        // fold 3: 26 38
        // fold 4: 39 52

        int[] fold_start = {0, 13, 26, 39};
        int[] fold_end = {12, 25, 38, 52};


        long elapsedMILLIS = 0;
        // FOR TRAINING
        for (int i = 1; i <= 0; i++) {
            System.out.println("Now fold: "+i);
            MapMatcher mm = new MapMatcher("data/route_segmentation/Hong_Kong-result.osm");
            //mm.parseSegs();
            mm.indexSegs();
            //Train
//            elapsedMILLIS+=mm.parseRecords("train", fold_start[i - 1], fold_end[i - 1],i, false);
//            mm.generateFullOutput(i, "data/map_matching/fold_" + i + "/train_match_result.txt");
////            // Test
            elapsedMILLIS+=mm.parseRecords("test", fold_start[i-1], fold_end[i-1],i, false); // I SHOULD DO THIS PART WITH test
            mm.generateFullOutput(i, "data/map_matching/fold_" + i + "/test_match_result.txt");
//////
        }
        System.out.println("TOTAL TIME " + elapsedMILLIS/(60*1000F));
        // Accident
        MapMatcher mm = new MapMatcher();
        mm.indexSegs();
        mm.parseAccidents();
        mm.outputAccResults("data/map_matching/acc_match_result.txt");
    }
}
