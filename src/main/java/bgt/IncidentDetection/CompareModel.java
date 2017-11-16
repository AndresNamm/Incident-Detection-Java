package bgt.IncidentDetection;

import bgt.MapMatching.MapMatcher;
import bgt.Model.Routes;
import bgt.parsing.OSMparser;
import org.dom4j.DocumentException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;

public class CompareModel {


    MapMatcher mm;
    public HashMap<String,Integer> segSpeeds = new HashMap<>();
    public CompareModel() throws FileNotFoundException, DocumentException {
        mm = new MapMatcher("data/route_segmentation/Hong_Kong-result.osm");
    }


    public static void main(String args[]){

    }
}
