package bgt.SpeedPanel;

import bgt.MapMatching.MapMatcher;
import bgt.Model.*;
import bgt.parsing.OSMparser;
import org.dom4j.DocumentException;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


// RATHER A TRANSFORMATION CLASS. 1 TIME USE IN THE PROJECT.
// BASED ON ROUTES GENERATED FROM HONG KONG OSM ROADS AND ROUTES GENERATED FROM SPEEDPANEL LOCATION(NODE) PAIRS
public class MatchSpeedPanelSegments extends MapMatcher {
    HashMap<String,List<String>> speedPanelSegs;
    HashMap<String,Segment> segmentsToMatch;
    HashMap<String,String> sPanelDictionary;
    //wrong
    //public double minAcceptableDist = 100.0;  // https://stackoverflow.com/questions/10722110/overriding-member-variables-in-java  reason why parent sees its own number which might be different
    //public  double minAcceptableDeg = 45.0;

    class SpDistanceComptor implements Comparator<SpDistance>{

        @Override
        public int compare(SpDistance spDistance, SpDistance t1) {
            if(spDistance.dist>t1.dist){return 1;}
            if(spDistance.dist<t1.dist){return -1;}
            return 0;
        }
    }

    class SpDistance{
        double dist;
        String segId;

    }

    public static String PREFIX ="data/speed_panels/intermediate_data/";

    public MatchSpeedPanelSegments(String fileName) throws DocumentException, FileNotFoundException {
        super(PREFIX + fileName);
        sPanelDictionary = new HashMap<>();
        for(Segment speedPanel : this.segMap.values()){
            String speedPanelId = String.valueOf(speedPanel.startNode.id)+"-"+String.valueOf(speedPanel.endNode.id);
            sPanelDictionary.put(speedPanelId,speedPanel.seg_id);
        }
    }

    public HashMap<String,List<Record>> segToRecs(List<Segment> segList){// Returns an HashMap with SegmentId as  key and list of
        // Records based on Segment start and endnode
        HashMap<String,List<Record>> segLrecMap = new HashMap<>();
        int cnt = 0;
        for(Segment seg : segList){
            Node sNode = seg.startNode;
            Node eNode = seg.endNode;
            Record sRecord = new Record(cnt,-1,0,0,0,sNode,0,seg.direction, seg.seg_id);
            Record eRecord = new Record(cnt+1,-1,0,0,0,eNode,0,seg.direction, seg.seg_id);
            cnt+=2;
            ArrayList<Record> pair = new ArrayList<>();
            pair.add(sRecord);
            pair.add(eRecord);
            segLrecMap.put(seg.seg_id, pair);
        }

        return segLrecMap;
    }

    public void putPanelSegMap(HashMap<String,List<String>> panelSegMap,String speedPanelId, String seg_ID){
        if(!panelSegMap.containsKey(speedPanelId)){
            panelSegMap.put(speedPanelId, new ArrayList<>());
        }
        panelSegMap.get(speedPanelId).add(seg_ID);
    }

    public HashMap<String,List<String>>  matchToSpeedPanels(HashMap<String,List<Record>> segRecMap){
        HashMap<String,List<String>> panelSegMap = new HashMap<>();
        for(Map.Entry<String,List<Record>> entry : segRecMap.entrySet()){
            List<Record> pair = entry.getValue();
            String sSegId = mapRecord(pair.get(0));
            String eSegId = mapRecord(pair.get(1));
            if(!(sSegId.equals("") && eSegId.equals(""))){
                double sDist;
                double eDist;
                if(sSegId.equals("") || eSegId.equals("")) {
                    sDist = sSegId.equals("") ? Double.MAX_VALUE : this.segMap.get(sSegId).calcRecordDirectDist(pair.get(0));
                    eDist = eSegId.equals("") ? Double.MAX_VALUE : this.segMap.get(eSegId).calcRecordDirectDist(pair.get(1));
                }else{// CALCULATE DOUBLE DISTANCE
                    sDist =  this.segMap.get(sSegId).calcRecordDirectDist(pair.get(0))+this.segMap.get(sSegId).calcRecordDirectDist(pair.get(1));
                    eDist =  this.segMap.get(eSegId).calcRecordDirectDist(pair.get(1))+this.segMap.get(eSegId).calcRecordDirectDist(pair.get(0));
                }

                if(sDist<eDist){
                    putPanelSegMap(panelSegMap, sSegId, entry.getKey());// sSegId=SpeedPanel which the resultSegment startNode was matched to, entry.getKet() - resultSegment which was matched.
                }else{
                    putPanelSegMap(panelSegMap, eSegId, entry.getKey());
                }
            }
        }
        return  panelSegMap;
    }

    public HashMap<String,List<String>>  findClosesToSpeedPanels(HashMap<String,List<Record>> segRecMap){
        HashMap<String,List<SpDistance>> panelSegMap = new HashMap<>();
        //HashMap<>
        for(Map.Entry<String,List<Record>> entry : segRecMap.entrySet()){
            List<Record> pair = entry.getValue();
            String sSpId = mapRecord(pair.get(0));
            String eSpId = mapRecord(pair.get(1));
            if(!(sSpId.equals("") && eSpId.equals(""))){
                SpDistance spDistance = new SpDistance();
                spDistance.segId=entry.getKey();
                double sDist;
                double eDist;
                if(sSpId.equals("") || eSpId.equals("")) {
                    sDist = sSpId.equals("") ? Double.MAX_VALUE : this.segMap.get(sSpId).calcRecordDirectDist(pair.get(0));
                    eDist = eSpId.equals("") ? Double.MAX_VALUE : this.segMap.get(eSpId).calcRecordDirectDist(pair.get(1));
                }else{// CALCULATE DOUBLE DISTANCE
                    sDist =  this.segMap.get(sSpId).calcRecordDirectDist(pair.get(0))+this.segMap.get(sSpId).calcRecordDirectDist(pair.get(1));
                    eDist =  this.segMap.get(eSpId).calcRecordDirectDist(pair.get(1))+this.segMap.get(eSpId).calcRecordDirectDist(pair.get(0));
                }

                if(sDist<eDist){
                //    putPanelSegMap(panelSegMap, sSpId, entry.getKey());// sSegId=SpeedPanel which the resultSegment startNode was matched to, entry.getKet() - resultSegment which was matched.
                    spDistance.dist=sDist;
                    if(!panelSegMap.containsKey(sSpId)){
                        panelSegMap.put(sSpId,new ArrayList<>());
                    }panelSegMap.get(sSpId).add(spDistance);
                }else{
                    spDistance.dist=sDist;
                    if(!panelSegMap.containsKey(eSpId)){
                        panelSegMap.put(eSpId,new ArrayList<>());
                    }panelSegMap.get(eSpId).add(spDistance);
                }
            }
        }

        HashMap<String, List<String>> ans = new HashMap<>();
        panelSegMap.forEach((spId, arr) -> Collections.sort(arr, new SpDistanceComptor()));
        panelSegMap.forEach((spId,arr) -> {
            ans.put(spId,new ArrayList<String>());
            ans.get(spId).add(arr.get(0).segId);
        });
        return ans;
    }

    public boolean doMatching() throws IOException, DocumentException {
        // THIS function will be run after the speedPanel file as the MapMAtching base ha beenr read in.
        Routes segmentResultRoutes = OSMparser.parseNodesAndWays("data/route_segmentation/Hong_Kong-result.osm");
        SegmentStructures ss = parseSegments(segmentResultRoutes);
        this.segmentsToMatch = ss.segMap;
        HashMap<String,List<Record>> segRecs = segToRecs(ss.segList);
        this.speedPanelSegs = matchToSpeedPanels(segRecs);
        //this.speedPanelSegs = findClosesToSpeedPanels(segRecs);
        System.out.println(this.speedPanelSegs);
        System.out.println(this.speedPanelSegs.size());
        printOutResult("data/speed_panels/spMatched.txt");
        printOutPanelsWSegments();
        return true;
    }


    public void printOutResult(String record_match_filename) throws IOException {
        FileWriter fw_record_result = new FileWriter( record_match_filename);
        for (Map.Entry<String,List<String>> entry :  this.speedPanelSegs.entrySet()){
            Segment speedPanel = this.segMap.get(entry.getKey());
            String speedPanelId = String.valueOf(speedPanel.startNode.id)+"-"+String.valueOf(speedPanel.endNode.id);
            fw_record_result.write(

                    "P" + "\t" + speedPanelId + "\n"
            );
            for(String segId : entry.getValue()){
                fw_record_result.write(

                        "S" + "\t" + segId + "\n"
                );
            }
        }
        fw_record_result.close();
    }

    public void printOutPanelsWSegments(HashMap<String,List<String>> speedPanelSegments) throws IOException {
        Long count = 0L;
        List<Way> speedPanelList = new ArrayList<>();
        List<Way> segList = new ArrayList<>();
        List<Way> wayList = new ArrayList<>();
        for (Map.Entry<String,List<String>> entry :  speedPanelSegments.entrySet()){

            Segment speedPanel = this.segMap.get(this.sPanelDictionary.get(entry.getKey()));
            String speedPanelId = entry.getKey();
            Way mainWay = speedPanel.toWay(speedPanelId,count);
            count++;
            speedPanelList.add(mainWay);
            wayList.add(mainWay);
            for(String segId : entry.getValue()){
                Segment seg = this.segmentsToMatch.get(segId);
                segList.add(seg.toWay(speedPanelId,seg.seg_id,count));
                wayList.add(seg.toWay(speedPanelId,seg.seg_id,count));
                count++;
            }

        }
        OSMparser.waysToOsm(wayList,this.routes.getBoundaries(), "data/speed_panels/SpeedPanelSegments-filtered.osm");
        //OSMparser.waysToOsm(segList,this.routes.getBoundaries(), "segmentSGrouped.osm");
        //OSMparser.waysToOsm(speedPanelList,this.routes.getBoundaries(), "speedpAGrouped.osm");
    }

    public void printOutPanelsWSegments() throws IOException {
        Long count = 0L;
        List<Way> speedPanelList = new ArrayList<>();
        List<Way> segList = new ArrayList<>();
        List<Way> wayList = new ArrayList<>();
        for (Map.Entry<String,List<String>> entry :  this.speedPanelSegs.entrySet()){
            Segment speedPanel = this.segMap.get(entry.getKey());
            String speedPanelId = String.valueOf(speedPanel.startNode.id)+"-"+String.valueOf(speedPanel.endNode.id);
            Way mainWay = speedPanel.toWay(speedPanelId,count);
            count++;
            speedPanelList.add(mainWay);
            wayList.add(mainWay);
            for(String segId : entry.getValue()){
                Segment seg = this.segmentsToMatch.get(segId);
                segList.add(seg.toWay(speedPanelId,seg.seg_id,count));
                wayList.add(seg.toWay(speedPanelId,seg.seg_id,count));
                count++;
            }

        }
        OSMparser.waysToOsm(wayList,this.routes.getBoundaries(), "data/speed_panels/SpeedPanelSegments"+this.minAcceptableDist+".osm");
        //OSMparser.waysToOsm(segList,this.routes.getBoundaries(), "segmentSGrouped.osm");
        //OSMparser.waysToOsm(speedPanelList,this.routes.getBoundaries(), "speedpAGrouped.osm");
    }

    public static void matchingSpeedPanelsToSegmentsInitiate() throws IOException, DocumentException {
        MatchSpeedPanelSegments matchSpeedPanelSegments = new MatchSpeedPanelSegments("speedPanelWays.osm");
        matchSpeedPanelSegments.indexSegs();
        matchSpeedPanelSegments.doMatching();
    }

    public static void printOutSpeedPanelsAndMatchedSegmentsBasedOnFile() throws IOException, DocumentException {
        MatchSpeedPanelSegments matchSpeedPanelSegments = new MatchSpeedPanelSegments("speedPanelWays.osm");
        Routes segmentResultRoutes = OSMparser.parseNodesAndWays("data/route_segmentation/Hong_Kong-result.osm");
        SegmentStructures ss = matchSpeedPanelSegments.parseSegments(segmentResultRoutes);
        matchSpeedPanelSegments.segmentsToMatch = ss.segMap;
        HashMap<String,List<String>> spSegs= SpeedPanelObsParser.readInSpSegs("data/speed_panels/filtered.csv");
        matchSpeedPanelSegments.printOutPanelsWSegments(spSegs);
    }

    public static void main(String args[]) throws IOException, DocumentException {
         MatchSpeedPanelSegments.matchingSpeedPanelsToSegmentsInitiate(); // uncomment this to perform matching
       //MatchSpeedPanelSegments.printOutSpeedPanelsAndMatchedSegmentsBasedOnFile();
    }

}
