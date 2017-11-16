package bgt.parsing;

import bgt.Model.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
// This Class focuses itself On Elements which could be visualized in OSM format. Even if the elements
// themselves are not exactly based in OSM but have some Geographical coordinate info in WGS84 system.
// I should actually generify the NODES WITH COORDINATES AND

/**
 * Created by UDU on 7/28/2017.
 */
public class OSMparser extends Parser {
    public static String PREFIX = ""; //OUTPUT INPUT DIRECTORY

    public static void routesToOSM(Routes routes, String name) throws IOException {
        Document doc = DocumentHelper.createDocument();
        Element rootElem = doc.addElement("osm").addAttribute("version", "0.6");
        rootElem = addBoundsToRoot(rootElem, routes.getBoundaries());

        // This part ensures that no Node is doubled and nodes are in order
        HashMap<Long, Node> nodes = new HashMap<>();

        for (Node node : routes.getNodeList()) {
            nodes.put(node.id, node);
        }

        ArrayList<Node> nodeList = new ArrayList<>(nodes.values());
        Collections.sort(nodeList, Node.getComparator());
        // This part ensures that no Node is doubled and nodes are in order
        for (Node node : nodeList) {
            addNodeElementToXmlElement(rootElem, node);
        }

        Iterator<Way> wayItr = routes.getWayList().iterator();

        while (wayItr.hasNext()) {
            Way way = wayItr.next();
            addWayElementToXmlElement(rootElem, way);
        }


        FileWriter fileWriter = new FileWriter(PREFIX + name);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(fileWriter, format);
        writer.write(doc);
        fileWriter.close();
    }

    //This method is for older dependencies
    public static void waysToOsm(List<Way> waysForPrint, Routes routes, String fileName) throws IOException {

        Document doc = DocumentHelper.createDocument();
        Element rootElem = doc.addElement("osm").addAttribute("version", "0.6");
        Element boundsElem = addBoundsToRoot(rootElem, routes.getBoundaries());
        List<Node> nodeList = new ArrayList<>();
        for (Way way : waysForPrint) {
            List<Node> tempNodeList = way.getNodeList();
            nodeList.addAll(new ArrayList<>(tempNodeList));
        }

        HashMap<Long, Node> nodeHashMap = new HashMap<>();
        for (Node node : nodeList) {
            nodeHashMap.put(node.id, node);
        }
        nodeList = new ArrayList<>();
        for (Node node : nodeHashMap.values()) {
            nodeList.add(node);
        }
        Collections.sort(nodeList, Node.getComparator());


        Iterator<Node> itr = nodeList.iterator();

        while (itr.hasNext()) {
            addNodeElementToXmlElement(rootElem, itr.next());
        }
        for (Way way : waysForPrint) {
            addWayElementToXmlElement(rootElem, way);
        }

        FileWriter fileWriter = new FileWriter(PREFIX + fileName);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(fileWriter, format);
        writer.write(doc);
        fileWriter.close();

    }

    public static void waysToOsm(List<Way> waysForPrint, Boundaries boundaries, String fileName) throws IOException {

        Document doc = DocumentHelper.createDocument();
        Element rootElem = doc.addElement("osm").addAttribute("version", "0.6");
        Element boundsElem = addBoundsToRoot(rootElem, boundaries);
        List<Node> nodeList = new ArrayList<>();
        for (Way way : waysForPrint) {
            List<Node> tempNodeList = way.getNodeList();
            nodeList.addAll(new ArrayList<>(tempNodeList));
        }

        HashMap<Long, Node> nodeHashMap = new HashMap<>();
        for (Node node : nodeList) {
            nodeHashMap.put(node.id, node);
        }
        nodeList = new ArrayList<>();
        for (Node node : nodeHashMap.values()) {
            nodeList.add(node);
        }
        Collections.sort(nodeList, Node.getComparator());


        Iterator<Node> itr = nodeList.iterator();

        while (itr.hasNext()) {
            addNodeElementToXmlElement(rootElem, itr.next());
        }
        for (Way way : waysForPrint) {
            addWayElementToXmlElement(rootElem, way);
        }

        FileWriter fileWriter = new FileWriter(PREFIX + fileName);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(fileWriter, format);
        writer.write(doc);
        fileWriter.close();

    }

    public static void wayToOSM(Way wayForPrint, Routes routes) throws IOException {
        System.out.println(wayForPrint.getId() + " id");
        //segmentor.wayList.get;
        List<Node> nodeList = wayForPrint.getNodeList();
        Collections.sort(nodeList, Node.getComparator());
        Iterator<Node> itr = nodeList.iterator();
        Document doc = DocumentHelper.createDocument();
        Element rootElem = doc.addElement("osm").addAttribute("version", "0.6");
        Element boundsElem = addBoundsToRoot(rootElem, routes.getBoundaries());
        while (itr.hasNext()) {
            addNodeElementToXmlElement(rootElem, itr.next());
        }
        addWayElementToXmlElement(rootElem, wayForPrint);
        FileWriter fileWriter = new FileWriter(PREFIX + String.valueOf(wayForPrint.getId()) + ".osm");
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(fileWriter, format);
        writer.write(doc);
        fileWriter.close();
    }

    public static void addNodeElementToXmlElement(Element rootElem, Node node) {
        Element nodeElementrootElem = rootElem.addElement("node");
        nodeElementrootElem
                .addAttribute("id", String.valueOf(node.id))
                .addAttribute("version", "10")
                .addAttribute("lat", String.valueOf(node.getLat()))
                .addAttribute("lon", String.valueOf(node.getLon()))
                .addAttribute("visible", String.valueOf(node.visible));
        if (node.type == Node.NodeType.RECORD) {
            nodeElementrootElem.addAttribute("seg_id", String.valueOf(node.seg_id));
        }

        nodeElementrootElem.addElement("tag").addAttribute("k", "highway")
                .addAttribute("v", "motorway_junction");
        nodeElementrootElem.addElement("tag").addAttribute("k", "ref")
                .addAttribute("v", node.ref);

    }

    public static void addWayElementToXmlElement(Element rootElem, Way way) {
        Element wayElem = rootElem.addElement("way")
                .addAttribute("id", String.valueOf(way.id))
                .addAttribute("version", "20");
        for (Node node : way.getNodeList()) {
            wayElem.addElement("nd")
                    .addAttribute("ref", String.valueOf(node.id));
        }
        // add tags to way here
        if (way.isBridge) {
            wayElem.addElement("tag")
                    .addAttribute("k", "bridge")
                    .addAttribute("v", "yes");
        }

        wayElem.addElement("tag")
                .addAttribute("k", "name")
                .addAttribute("v", way.getName() == null ? "name" : way.getName());
        if (way.getName_en() != null) {
            wayElem.addElement("tag")
                    .addAttribute("k", "name:en")
                    .addAttribute("v", way.getName_en());
        }
//        if (way.getName_zh() != null) {
//            wayElem.addElement("tag")
//                    .addAttribute("k", "name:zh")
//                    .addAttribute("v", way.getName_zh());
//        }
        if (way.isOneWay) {
            wayElem.addElement("tag")
                    .addAttribute("k", "oneway")
                    .addAttribute("v", "yes");
        }
        if (way.highway != null) {
            wayElem.addElement("tag")
                    .addAttribute("k", "highway")
                    .addAttribute("v", way.highway);
        }

        if(way.group!= null){
            wayElem.addElement("tag")
                    .addAttribute("k", "group")
                    .addAttribute("v", way.group);
        }
        if(way.VALUE != null){
            wayElem.addElement("tag")
                    .addAttribute("k", "barrier")
                    .addAttribute("v", String.valueOf(way.VALUE));
        }

//        if (way.speedPanelId != null) {
//            wayElem.addElement("tag")
//                    .addAttribute("k", "speedPanelId")
//                    .addAttribute("v", way.speedPanelId);
//        }
    }

    public static Element addBoundsToRoot(Element rootElement, Boundaries boundaries) {
        Element boundsElement = rootElement.addElement("bounds");
        boundsElement.
                addAttribute("origin", String.valueOf((boundaries.getOrigin()))).
                addAttribute("maxlat", String.valueOf(boundaries.getMaxlat())).
                addAttribute("minlat", String.valueOf(boundaries.getMinlat())).
                addAttribute("maxlon", String.valueOf(boundaries.getMaxlon())).
                addAttribute("minlon", String.valueOf(boundaries.getMinlon()));

        return rootElement;
    }

    public static Routes parseNodesAndWays(String fileName) throws FileNotFoundException, DocumentException {
        SAXReader sr = new SAXReader();
        //Document doc = sr.read(new File("data/route_segmentation/Hong_Kong_Highways-Merged-Remove_Deleted.osm"));
        if (Objects.equals(fileName, "")) {
            fileName = "Hong_Kong_Highways-Merged-Remove_Deleted.osm";
        }
        Document doc = sr.read(new File(PREFIX + fileName));
        Element rootElem = doc.getRootElement();

        Routes routes = new Routes(buildBoundaries(rootElem.element("bounds")));
        Iterator<Element> nodesIterator = rootElem.elements("node").iterator();

        Map<Long, Node> nodeMap = new HashMap<>();
        while (nodesIterator.hasNext()){
            Element nodeElem = nodesIterator.next();
            long node_id = Long.valueOf(nodeElem.attributeValue("id")); // get node id
            double node_lat = Double.valueOf(nodeElem.attributeValue("lat")); // get node latitude
            double node_lon = Double.valueOf(nodeElem.attributeValue("lon")); // get node longitude
            Node node = new Node(node_id, node_lat, node_lon);
            nodeMap.put(node.id, node);
        }

        routes.setNodeList(new ArrayList<Node>(nodeMap.values()));
        for (Element wayElem : rootElem.elements("way")) {
            int way_id = Integer.valueOf(wayElem.attributeValue("id")); // get way id
            Way way = new Way(way_id);
            List<Node> nodesOfWayList = new ArrayList<>();  // to store all nodes of the way
            for (Element nodeOfWayElem : wayElem.elements("nd")) {
                long node_refID = Long.valueOf(nodeOfWayElem.attributeValue("ref"));
                nodesOfWayList.add(nodeMap.get(node_refID));
            }
            way.setNodeList(nodesOfWayList); // set the node list of the way
            parseWayTags(way, wayElem, true);
            routes.addWay(way);
        }
        return routes;
    }

    public static Routes parseFilterNodesAndWays(String fileName) throws FileNotFoundException, DocumentException {
        SAXReader sr = new SAXReader();
        //Document doc = sr.read(new File("data/route_segmentation/Hong_Kong_Highways-Merged-Remove_Deleted.osm"));
        Document doc = sr.read(new File(PREFIX + fileName));
        Element rootElem = doc.getRootElement();

        Routes routes = new Routes(buildBoundaries(rootElem.element("bounds")));
        Iterator<Element> nodesIterator = rootElem.elements("node").iterator();

        Map<Long, Node> nodeMap = new HashMap<>();
        while (nodesIterator.hasNext()) {
            Element nodeElem = nodesIterator.next();
            long node_id = Long.valueOf(nodeElem.attributeValue("id")); // get node id
            double node_lat = Double.valueOf(nodeElem.attributeValue("lat")); // get node latitude
            double node_lon = Double.valueOf(nodeElem.attributeValue("lon")); // get node longitude
            Node node = new Node(node_id, node_lat, node_lon);
            nodeMap.put(node.id, node);
        }

        //routes.setNodeList(new ArrayList<Node>(nodeMap.values()));
        Map<Long, Node> filteredNodeMap = new HashMap<>();
        for (Element wayElem : rootElem.elements("way")) {
            int way_id = Integer.valueOf(wayElem.attributeValue("id")); // get way id
            Way way = new Way(way_id);
            List<Node> nodesOfWayList = new ArrayList<>();  // to store all nodes of the way
            Map<Long, Node> subFilteredNodeMap = new HashMap<>();
            for (Element nodeOfWayElem : wayElem.elements("nd")) {
                long node_refID = Long.valueOf(nodeOfWayElem.attributeValue("ref"));
                nodesOfWayList.add(nodeMap.get(node_refID));
                //filteredNodeMap.putnodeMap.get(node_refID));
                //routes.addNode(nodeMap.get(node_refID));
                subFilteredNodeMap.put(node_refID, nodeMap.get(node_refID));
            }

            way.setNodeList(nodesOfWayList); // set the node list of the way
            Boolean add = parseWayTags(way, wayElem, false);


            if (add) {
                routes.addWay(way);
                filteredNodeMap.putAll(subFilteredNodeMap);
            }
        }

        routes.setNodeList(new ArrayList<>(filteredNodeMap.values()));
        return routes;
    }

    public static Routes parseFilterNodesAndWaysLong(String fileName, double boundary) throws FileNotFoundException, DocumentException {
        SAXReader sr = new SAXReader();
        //Document doc = sr.read(new File("data/route_segmentation/Hong_Kong_Highways-Merged-Remove_Deleted.osm"));
        Document doc = sr.read(new File(PREFIX + fileName));
        Element rootElem = doc.getRootElement();

        Routes routes = new Routes(buildBoundaries(rootElem.element("bounds")));
        Iterator<Element> nodesIterator = rootElem.elements("node").iterator();

        Map<Long, Node> nodeMap = new HashMap<>();
        while (nodesIterator.hasNext()) {
            Element nodeElem = nodesIterator.next();
            long node_id = Long.valueOf(nodeElem.attributeValue("id")); // get node id
            double node_lat = Double.valueOf(nodeElem.attributeValue("lat")); // get node latitude
            double node_lon = Double.valueOf(nodeElem.attributeValue("lon")); // get node longitude
            Node node = new Node(node_id, node_lat, node_lon);
            nodeMap.put(node.id, node);
        }

        //routes.setNodeList(new ArrayList<Node>(nodeMap.values()));
        Map<Long, Node> filteredNodeMap = new HashMap<>();
        for (Element wayElem : rootElem.elements("way")) {
            int way_id = Integer.valueOf(wayElem.attributeValue("id")); // get way id
            Way way = new Way(way_id);
            List<Node> nodesOfWayList = new ArrayList<>();  // to store all nodes of the way
            Map<Long, Node> subFilteredNodeMap = new HashMap<>();
            for (Element nodeOfWayElem : wayElem.elements("nd")) {
                long node_refID = Long.valueOf(nodeOfWayElem.attributeValue("ref"));
                nodesOfWayList.add(nodeMap.get(node_refID));
                //filteredNodeMap.putnodeMap.get(node_refID));
                //routes.addNode(nodeMap.get(node_refID));
                subFilteredNodeMap.put(node_refID, nodeMap.get(node_refID));
            }

            way.setNodeList(nodesOfWayList); // set the node list of the way
            Boolean add = parseWayTags(way, wayElem, true);
            add =  way.getWayLength()>boundary;
            if (add) {
                routes.addWay(way);
                filteredNodeMap.putAll(subFilteredNodeMap);
            }
        }

        routes.setNodeList(new ArrayList<>(filteredNodeMap.values()));
        return routes;
    }

    public static Boolean parseWayTags(Way way, Element wayElem, Boolean noFilter) {
        Boolean add = noFilter;
        for (Element tag : wayElem.elements("tag")) {
            switch (tag.attributeValue("k")) {
                case "bridge":
                    if (tag.attributeValue("v").equals("yes")) {
                        way.setBridgeFlag(true);
                    }
                    break;
                case "name":
                    way.setName(tag.attributeValue("v"));
                    break;
                case "name:en":
                    way.setName_en(tag.attributeValue("v"));
                    break;
                case "name:zh":
                    way.setName_zh(tag.attributeValue("v"));
                    break;
                case "oneway":
                    if (tag.attributeValue("v").equals("yes")) {
                        way.setOneWayTag(true);
                    }
                    break;
                case "tunnel":
                    if (tag.attributeValue("v").equals("yes")) {
                        way.setTunnelFlag(true);
                    }
                    break;
                case "highway":
                    switch (tag.attributeValue("v")) {
                        //"highway"="trunk" OR "highway"="motorway_link" OR "highway"="motorway"
                        case "trunk":
                            add = true;
                            way.highway = tag.attributeValue("v");
                            break;
                        case "motorway_link":
                            add = true;
                            way.highway = tag.attributeValue("v");
                            break;
                        case "motorway":
                            add = true;
                            way.highway = tag.attributeValue("v");
                            break;
                    }
                case "group":
                    way.group = tag.attributeValue("v");
//                case "speedPanelId":
//                    way.speedPanelId = tag.attributeValue("v");
                default:
                    continue;
            }
        }

        return add;

    }

    public static Boundaries buildBoundaries(Element boundsElement) {
        String origin = boundsElement.attributeValue("origin");
        Double maxlat = (Double.valueOf(boundsElement.attributeValue("maxlat")));
        Double minlat = (Double.valueOf(boundsElement.attributeValue("minlat")));
        Double maxlon = (Double.valueOf(boundsElement.attributeValue("maxlon")));
        Double minlon = (Double.valueOf(boundsElement.attributeValue("minlon")));
        return new Boundaries(minlat, maxlat, maxlon, minlon, origin);
    }

    public static void  filterRoutesByTag() throws IOException, DocumentException {
        OSMparser.PREFIX = "data/route_segmentation/";
        String file = "Hong_Kong_Highways-Merged-Remove_Deleted.osm";
        Routes filtered = OSMparser.parseFilterNodesAndWays(file);
        OSMparser.routesToOSM(filtered, "result_filtered-" + file);
    }

    public static void  filterRoutesByDistance() throws IOException, DocumentException {
        String pref = "data/route_segmentation/";
        String file = "merged-filtered.osm";
        Routes filtered = OSMparser.parseFilterNodesAndWaysLong(pref+file,1500);
        OSMparser.routesToOSM(filtered, "data/route_segmentation/long_result_filtered-" + file);
    }

    public static void main(String args[]) throws IOException, DocumentException {
        OSMparser.filterRoutesByDistance();
    }

}
