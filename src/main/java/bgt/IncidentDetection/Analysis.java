package bgt.IncidentDetection;

import bgt.Model.Routes;
import bgt.Model.Way;
import bgt.parsing.OSMparser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Analysis {

    public HashMap<Long, Double> wayAUC ;
    public HashMap<Long, Double> readInWayAucPairs(String fileN) throws IOException {
        //DATASTRUCTURES
        HashMap<Long, Double> aucPairs = new HashMap<>();
        //String flag;
        //String speedPanelId = "";
        //FILE IO
        File file = FileUtils.getFile(fileN);
        LineIterator it = FileUtils.lineIterator(file);
        while (it.hasNext()) {
            String line = it.nextLine();
            try{
                StringTokenizer st = new StringTokenizer(line,",");

                String wayId = st.nextToken();

                String auc = st.nextToken();
                if(NumberUtils.isNumber(wayId)&&NumberUtils.isNumber(auc)){
                    aucPairs.put(Long.valueOf(wayId),Double.valueOf(auc));
                }else{
                    System.out.println("Not is number");
                }
            }catch (NoSuchElementException e){
                System.out.println(line);
            }
        }
        return aucPairs;
    }

    public void printAUCtoOSM(String fileName) throws IOException, DocumentException {
        String prefix = "data/incident_detection/";
        this.wayAUC =  readInWayAucPairs(prefix+fileName);
        Routes routes = OSMparser.parseNodesAndWays("data/route_segmentation/Hong_Kong-result.osm");
        Iterator<Way> it = routes.getWayList().iterator();
        while(it.hasNext()){
            Way way = it.next();
            if(this.wayAUC.containsKey(way.id)){
                way.VALUE = this.wayAUC.get(way.id);
            }else{
                it.remove();
            }
        }
        OSMparser.routesToOSM(routes,"data/route_segmentation/"+fileName+"-result.osm");
    }

    public static void main(String args[]) throws IOException, DocumentException {
        Analysis ana = new Analysis();
        String fileName = "old.csv";
        ana.printAUCtoOSM(fileName);
    }
}
