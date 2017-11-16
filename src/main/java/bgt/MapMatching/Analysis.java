package bgt.MapMatching;

import bgt.Model.Record;
import bgt.Model.*;
import bgt.parsing.OSMparser;
import bgt.parsing.Parser;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by UDU on 7/31/2017.
 */


class ComparePoint implements Comparator<String[]> {
    @Override
    public int compare(String[] o1, String[] o2) {
        if (Integer.valueOf(o1[1]) > Integer.valueOf(o2[1])) {
            return -1;

        } else if (Integer.valueOf(o1[1]) < Integer.valueOf(o2[1])) {
            return 1;
        }
        return 0;
    }
}

public class Analysis {

    private MapMatcher mapMatcher;
    Analysis() throws IOException, DocumentException {
        this.mapMatcher = new MapMatcher();
        this.mapMatcher.indexSegs();
    }


    public void printGridElement(Routes forPrint, int rowId, int colId, String name) throws IOException {
        //Routes forPrint = buildGridElementRoots(rowId,colId);
        OSMparser.routesToOSM(forPrint, name + "-" + rowId + "-" + colId + ".osm");

    }

    public Routes buildGridElementRoots(int rowId, int colId) throws IOException {

        List<Way> baseRoutesWayList = this.mapMatcher.routes.getWayList();
        HashMap<Long, Way> wayHashMap = new HashMap<>();

        for (Way way : baseRoutesWayList) {
            wayHashMap.put(way.id, way);
        }
        // GET WAYS FOR SPECIFIC NODES
        GridElement gridElement = this.mapMatcher.grid.getListGrid()[rowId][colId];
        List<Segment> segmentList = gridElement.getSegmentList();
        //IMPORTANT
        List<Way> segmentWayList = new ArrayList<>();
        HashMap<Long, Node> segInclusive = new HashMap<>();
        HashMap<Long, Node> recordInclusive = new HashMap<>();
        HashMap<Long, Node> segmentNodeMap = new HashMap<>();

        //Add nodes from RecordList
        for (Segment seg : segmentList) {
            segmentWayList.add(wayHashMap.get(seg.way_id));
            segInclusive.put(seg.startNode.id, seg.startNode);
            segInclusive.put(seg.endNode.id, seg.endNode);
            for (Record rec : seg.recordList) {
                Node node = rec.toNode();
                segmentNodeMap.put(node.id, node);
                recordInclusive.put(node.id, node);
            }
        }

        //Add nodes from Ways
        for (Way way : segmentWayList) {
            for (Node node : way.getNodeList()) {
                segmentNodeMap.put(node.id, node);
            }
        }

        //IMPORTANT
        ArrayList<Node> segmentWaysNodeList = new ArrayList<>(segmentNodeMap.values());

        // Double Check that nodes have right categories. That hashing has not overwritten them.
        for (Node node : segmentWaysNodeList) {
            if (recordInclusive.containsKey(node.id)) {
                node.ref = "record";
            } else if (segInclusive.containsKey(node.id)) {
                node.ref = "segment";
            }
        }

        Routes gridElementRoutes = mapMatcher.grid.convertToRoutes(rowId, colId);
        List<Node> gridNodes = gridElementRoutes.getNodeList();
        gridNodes.addAll(segmentWaysNodeList);
        List<Way> gridWays = gridElementRoutes.getWayList();
        gridWays.addAll(segmentWayList);
        return gridElementRoutes;

    }

    public void printGridWithSegments() throws IOException {
        int[] id = this.findSegmentRichGE();
        Routes routes = buildGridElementRoots(id[0], id[1]);
        this.printGridElement(routes, id[0], id[1], "GE-SEGMENTS-WAYS");
    }

    // PRINT MOST POPULAR SEGMENT WITH WAYS AND RECORDS
    public void buildSegmentHistogram() throws IOException {
        int foldNR = 1;
        ArrayList<Integer> segmentCounts = new ArrayList<>();
        for(int foldNr=1;foldNR<=4;foldNR++){
            File f = FileUtils.getFile("data/map_matching/fold_" + foldNR + "/train_match_result.txt");
            HashMap<String, Long> aRcounts = ParseMapMatchResults.getSegmentsArCounts(f);
            //TreeMap<Long,String> aRcountsTM = new TreeMap<>(Collections.reverseOrder());

//            for (String id : aRcounts.keySet()) {
//                aRcountsList.add(new String[]{id, String.valueOf(aRcounts.get(id))});
//            }
//
//
//            // SORT SEGMENTS ACCORDING TO THEIR RECORDCOUNTS
//            //Collections.sort(aRcountsList, new ComparePoint());
//            //System.out.println("Most popular segment " + aRcountsList.get(0)[0] + " with this many records  " + aRcountsList.get(0)[1]);
//
//            // PRINT SEGMENT  RECORD COUNTS TO FILE
//            ArrayList<Integer> segmentCounts = new ArrayList<>();
//            for (String[] k : aRcountsList) {
//                segmentCounts.add(Integer.valueOf(k[1]));
//            }
            for(Long count : aRcounts.values()){
                segmentCounts.add(Math.toIntExact(count));
            }

        }
        ParseMapMatchResults.printSegmentCountsToFile(segmentCounts);
    }

    public void segRecDensityOSM() throws IOException {
        int foldNR = 1;
        HashMap<String,Integer> segmentCounts = new HashMap<>();
        for(int foldNr=1;foldNR<=4;foldNR++){
            File f = FileUtils.getFile("data/map_matching/fold_" + foldNR + "/train_match_result.txt");
            HashMap<String, Long> aRcounts = ParseMapMatchResults.getSegmentsArCounts(f);
            for(String key : aRcounts.keySet()){
                segmentCounts.put(key,segmentCounts.containsKey(key)? segmentCounts.get(key)+ Math.toIntExact(aRcounts.get(key)) : 0  );
            }
        }

        Long cnt = 0l;
        List<Way> toPrint = new ArrayList<>();
        for(String key : segmentCounts.keySet()){
            Integer count = segmentCounts.get(key);
            Segment seg = this.mapMatcher.segMap.get(key);
            Way way = seg.toWay("s+l",cnt++ );
            way.VALUE = Double.valueOf(count)/4; //count;
            toPrint.add(way);
        }

        OSMparser.waysToOsm(toPrint,mapMatcher.routes.getBoundaries(), "data/route_segmentation/routesegDensity.osm");
    }


    public void segAccDensityOSM() throws IOException {
        int foldNR = 1;

        HashMap<String, Long> segAccCount = ParseMapMatchResults.getSegmentAccCount("data/map_matching/acc_match_result.txt");
        Long cnt = 0l;
        List<Way> toPrint = new ArrayList<>();
        for(String key : segAccCount.keySet()){
            Long count = segAccCount.get(key);
            Segment seg = this.mapMatcher.segMap.get(key);
            Way way = seg.toWay("s+l",cnt++ );
            way.VALUE = Double.valueOf(count); //count;
            toPrint.add(way);
        }

        OSMparser.waysToOsm(toPrint,mapMatcher.routes.getBoundaries(), "data/route_segmentation/routesegDensity.osm");
    }


    public void getWayRecordCounts() throws IOException {

        HashMap<Long,Long> wayCount = new HashMap<>();
        long totalRows = 0l;
        long totalRecords = 0l;
        for(int foldNr = 1;foldNr<=4; foldNr++){
            String fileName= "data/map_matching/fold_" + foldNr + "/train_match_result.txt";
            File f = FileUtils.getFile(fileName);
            totalRows = Parser.countLinesFile(fileName);
            totalRecords = 0l;

            HashMap<String, Long> aRcounts = ParseMapMatchResults.getSegmentsArCounts(f);
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
        Parser.printArrayListLines(lineList, "plots/match_way_counts_fold.txt", false);

    }


    public void printAllsegments() throws IOException {
        int foldNR = 1;
        File f = FileUtils.getFile("data/map_matching/fold_" + foldNR + "/train_match_result.txt");
        ParseMapMatchResults.printAllsegments(f,1 );
    }

    private int[] findSegmentRichGE() {

        GridElement[][] gridLsit = this.mapMatcher.grid.getListGrid();
        int max = Integer.MIN_VALUE;
        int[] maxPos = {-1, -1};
        for (int i = 0; i < gridLsit.length; i++) {
            for (int j = 0; j < gridLsit[i].length; j++) {
                GridElement gridElement = gridLsit[i][j];
                max = max < gridElement.getSegmentList().size() ? gridElement.getSegmentList().size() : max;
                maxPos = gridElement.getSegmentList().size() == max ? new int[]{i, j} : maxPos;
            }
        }
        return maxPos;
    }

    public Long countRecords() throws IOException {
        Long count = 0l;
        Integer segCount = 0;

        for(int foldNr= 1; foldNr<=4;foldNr++){
            String fileName = "/home/andres/Documents/Thesis/RESULTS/mapmatching/100_with_long_ways/fold_"+foldNr+"/train_match_result.txt";
            count+=Parser.countLinesFile(fileName);
            count-=ParseMapMatchResults.getAllSegments(new File(fileName)).size();

        }
        return count/4;
    }


    public static void main(String args[]) throws IOException, DocumentException {
        Analysis analysis = new Analysis();

       //System.out.println(analysis.countRecords());
        analysis.segAccDensityOSM();
        //analysis.printGridWithSegments();
        //analysis.buildSegmentHistogram();
        //analysis.getWayRecordCounts();

    }
}
