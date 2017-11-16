package bgt.RouteSegmentation;

import bgt.Model.Node;
import bgt.Model.Routes;
import bgt.Model.Way;
import bgt.parsing.OSMparser;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.*;

public class RouteSegmentor {
    public void resegmentWays(Routes routes){
        Map<Long,Node> updated = new HashMap<>();
        Iterator<Way> it = routes.getWayList().iterator();
        while (it.hasNext()){
            Way way = it.next();
            List<Node> nodesList = way.resegmentWay(50.0, 100.0); // Call the method of the way to resegment itself
            for(Node node : nodesList){
                updated.put(node.id,node);
            }
        }
        routes.setNodeList(new ArrayList<>(updated.values()));
    }

    public static void main(String[] args) throws IOException, DocumentException {
        //Routes routes = OSMparser.parseNodesAndWays("data/route_segmentation/long_result_filtered-merged-filtered.osm");
        Routes routes = OSMparser.parseNodesAndWays("data/route_segmentation/merged-filtered.osm");
        RouteSegmentor routeSegmentor = new RouteSegmentor();
        routes.resegmentWays(50.0,100.0);
        OSMparser.routesToOSM(routes, "data/route_segmentation/Hong_Kong-result.osm");
        //OSMparser.wayToOSM(routes.getWayList().get(0),routes);
        String min=Utilityfunctions.findGlobalExtremum(routes,(Double temp, Double extremum)-> temp<extremum ,Double.MAX_VALUE);
        String max=Utilityfunctions.findGlobalExtremum(routes,(Double temp, Double extremum)-> temp>extremum,Double.MIN_VALUE);
        System.out.println("Minimum " + min);
        System.out.println("Maximum " + max);
    }
}
