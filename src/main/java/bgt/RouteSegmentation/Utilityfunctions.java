package bgt.RouteSegmentation;

import bgt.Model.Node;
import bgt.Model.Routes;
import bgt.Model.Way;
import bgt.parsing.OSMparser;
import bgt.parsing.Parser;
import org.dom4j.DocumentException;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

 class NodesWay {
    long wayId;
    long srcNodeId;
    long dstNodeId;
    double distance;

    public void changeInfo(long wayId, long srcNodeI, long dstNodeId, double distance) {
        this.wayId = wayId;
        this.srcNodeId = srcNodeI;
        this.dstNodeId = dstNodeId;
        this.distance = distance;
    }

    @Override
    public String toString() {
        DecimalFormat dfmt = new DecimalFormat(",000.0");
        return "wayId: " + String.valueOf(wayId) + " srcNodeId: " + String.valueOf(srcNodeId) + " dstNodeId:" + String.valueOf(dstNodeId) + " distance: " + dfmt.format(distance);
    }
}
public class Utilityfunctions {
    public static String findGlobalExtremum(Routes routes, BiPredicate<Double, Double> p, double extremum) {
        NodesWay result = new NodesWay();
        List<Way> ways = routes.getWayList();
        double globalExtremum = extremum;//
        for (Way way : ways) {
            NodesWay temp = findeExtremumInWay(way, p, extremum);
            if (p.test(temp.distance, globalExtremum)) {
                globalExtremum = temp.distance;
                result = temp;
            }
        }
        return result.toString();
    }


    public static NodesWay findeExtremumInWay(Way way, BiPredicate<Double, Double> p, double extremum) {
        NodesWay result = new NodesWay();
        List<Node> nodes = way.getNodeList();
        for (int i = 1; i < nodes.size(); i++) {
            Node srcNode = nodes.get(i - 1);
            Node dstNode = nodes.get(i);
            double distance = Node.calcDist(srcNode, dstNode);
            if (p.test(distance, extremum)) {
                extremum = distance;
                result.changeInfo(way.getId(), srcNode.id, dstNode.id, distance);
            }
        }
        return result;
    }

    public static void printWayLengths(Routes routes, String outFileName) throws IOException {
        List<String> lineList = new ArrayList<>();
        String dMiter = Parser.defaultDelimiter;
        int cnt = 0;
        lineList.add("way en name"+ dMiter + "# segs"+ dMiter+ "way id"+dMiter+ "length");
        for(Way way : routes.getWayList()){
            cnt++;
            lineList.add(way.getName_en() +dMiter+ way.nodeList.size()+dMiter+String.valueOf(way.id) +dMiter+String.valueOf(way.getWayLength()));
        }
        System.out.println(cnt);
        Parser.printArrayListLines(lineList, outFileName,false);
    }


    /**
     * Created by UDU on 7/14/2017.
     */
    public static void main(String args[]) throws IOException, DocumentException {
        String prefix = "data/route_segmentation/";
        String outPrefix = "plots/";
        String unsegmented = prefix+"merged-filtered.osm";
        String segmented = prefix+ "Hong_Kong-result.osm";
        Routes routes = OSMparser.parseNodesAndWays(unsegmented);
        Routes segRoutes = OSMparser.parseNodesAndWays(segmented);

        printWayLengths(routes,outPrefix+"unsegmented.txt");
        printWayLengths(segRoutes,outPrefix+"segmented.txt");
    }

    // this class is used to keep specific info about

}
