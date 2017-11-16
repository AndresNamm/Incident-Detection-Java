package bgt.SpeedPanel;

import bgt.Model.Node;
import bgt.Model.Routes;
import bgt.Model.Way;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import bgt.parsing.OSMparser;

import javax.print.attribute.HashAttributeSet;
import java.io.*;
import java.util.*;


// The purpose of this class is to keep and transform the speedpanel data
//from regular speedpanel observation files and speedpanel dataspec to a full set of ways with assigned nodes.



public class SpeedPanelToProjectFormater {
    public static final String PREFIX = "data/speed_panels/intermediate_data/";
    HashMap<String, Integer> panelNodeMap = new HashMap<>();// Stores Ids from available speed panel nodes
    HashMap<Long, Node> nodeHashMap = new HashMap<>(); // Stores Nodes created from nodes based on panelNode map ids
    ArrayList<String[]> panelCombos = new ArrayList<>();
    ArrayList<Way> wayList = new ArrayList<>();

    public void addNodeId(String nodeId) {
        if (this.panelNodeMap.containsKey(nodeId)) {
            this.panelNodeMap.put(nodeId, this.panelNodeMap.get(nodeId) + 1);
        } else {
            this.panelNodeMap.put(nodeId, 1);
        }
    }

    public void addNodeId(HashMap<String, Integer> hMap, String nodeId) {
        if (hMap.containsKey(nodeId)) {
            hMap.put(nodeId, hMap.get(nodeId) + 1);
        } else {
            hMap.put(nodeId, 1);
        }
    }

    public void printWays(String oFileName) throws IOException, DocumentException {
        String fileName = "Hong_Kong-result.osm";
        //OSMparser.PREFIX = "";
        Routes routes = OSMparser.parseNodesAndWays(fileName);
        OSMparser.PREFIX = "";
        OSMparser.waysToOsm(this.wayList, routes.getBoundaries(), PREFIX +oFileName +"speedPanelWays.osm");
    }

    public void generateWaysFromNodeCombos() throws IOException, BiffException {
        int i = 1;
        for (String[] nodePair : this.panelCombos) {
            ArrayList<Node> nList = new ArrayList<>();
            Node startNode = this.nodeHashMap.get(Long.valueOf(nodePair[0]));
            Node endNode = this.nodeHashMap.get(Long.valueOf(nodePair[1]));
            nList.add(startNode);
            nList.add(endNode);
            Way way = new Way(i, nList);
            //way.speedPanelId=
            this.wayList.add(way);
            i++;
        }
        System.out.println(this.wayList);
    }

    public ArrayList<String[]> getCurrentSpeedPanelsOld() throws IOException, BiffException {
        Workbook book = Workbook.getWorkbook(new File(PREFIX+"panels.xls"));
        Sheet sheet = book.getSheet(0);
        int lastCol = sheet.getColumns()-1;
        //System.out.println(lastCol);
        String lastOne = sheet.getCell(lastCol,1).getContents();
        HashMap<String,String> hMap = new HashMap<>();
        int offSet=2;
        for(int i = 0; i<sheet.getColumns()-3;i++){
            if(i%3==0){
                String[] nodePair = sheet.getCell(offSet+i,1).getContents().split(" ")[1].split("-");
                addNodeId(this.panelNodeMap,nodePair[0]);
                addNodeId(this.panelNodeMap,nodePair[1]);
                this.panelCombos.add(nodePair);
            }
        }
        book.close();
        return panelCombos;
//        return this.panelNodeMap;
    }

    public ArrayList<String[]> getCurrentSpeedPanels() throws IOException, BiffException {

        int cnt = 0;
        HashMap<String, String> sPanelNodePair = new HashMap<>();
        HashMap<String, Integer> separateNodes = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            String path = String.join("/", Arrays.copyOfRange(PREFIX.split("/"), 0, 2)) + "/";
            String folderN = (i > 9 ? "2010" : "20100") + String.valueOf(i);
            File folderF = new File(path + folderN);
            File[] listOfFiles = folderF.listFiles();

            for (File f : listOfFiles) {
                if (f.isFile() && !f.getName().startsWith(".")) {
                    getHeaderInfo(sPanelNodePair, separateNodes, f.getPath());// Appends to these 2 datastucture
                }
            }
        }
        this.panelNodeMap = separateNodes;
        for (String pair : sPanelNodePair.keySet()) {
            this.panelCombos.add(pair.split("-"));
        }
        return this.panelCombos;
    }

    public void getHeaderInfo(HashMap<String, String> sPanelNodePair, HashMap<String, Integer> separateNodes, String fileN) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileN));
        String line = "";
        int offset = 2;
        int cnt = 0;
        //br.readLine();
        //br.readLine();

        line = br.readLine();
        String[] header = line.split(",");
        if (!header[0].equals("Date")) {
            System.out.println("Formating fault");
            return;

        }

        HashMap<Integer, String> colDict = new HashMap<>();
        //System.out.println(header.length);
        for (int i = 0; i < header.length - 2; i++) {
            if (i % 3 == 0) {
                String speedPanelId = header[i + offset].split(" ")[1];
                sPanelNodePair.put(speedPanelId, "");
                addNodeId(separateNodes, speedPanelId.split("-")[0]);
                addNodeId(separateNodes, speedPanelId.split("-")[1]);
                // if(!nodePair.containsKey())
                //System.out.println(speedPanelId);
                //panelObsrv.put(speedPanelId, new ArrayList<>());
                //colDict.put(i + offset, speedPanelId);
            }
        }


    }

    public String getPanelCombosStr(){
        StringBuilder end = new StringBuilder();

        for (String[] str: this.panelCombos){
            end.append(String.join("-",str) +"  ");
        }
        return end.toString();

    }

    public void readSpeedPanelsToNodes(HashMap<String, String[]> rowMap) {
        for (String key : rowMap.keySet()) {
            String[] row = rowMap.get(key);
            //System.out.println(row[0]);
            Node sp1 = new Node(Long.valueOf(row[0]), Double.valueOf(row[row.length - 2]), Double.valueOf(row[row.length - 1]));
            this.nodeHashMap.put(sp1.id, sp1);
        }
        System.out.println(nodeHashMap);
    }

    public HashMap<String, String[]> readInSpeedPanelsCSV(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(PREFIX + fileName));
        String line = "";
        HashMap<String, String[]> panelMap = new HashMap<>();
        while ((line = br.readLine()) != null) {
            String[] row = line.split(",");
            if ( NumberUtils.isNumber(row[0])){
                panelMap.put(row[0], row);
            }
        }
        return panelMap;
    }

    public void filterSpeedPanelNodesCSV(String fileName) throws IOException {
        HashMap<String, String[]> printOut = readInSpeedPanelsCSV(fileName);
        //REMOVE SPEEDPANEL NODES THAT DID NOT IN 2010 EXIS
        for (Iterator<Map.Entry<String, String[]>> it = printOut.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String[]> entry = it.next();
            if (!this.panelNodeMap.containsKey(entry.getKey())) {
                it.remove();
            }
        }
        // print out the filtered result
        FileWriter fw_result = new FileWriter(PREFIX + fileName + "-result.csv");
        fw_result.write("ID,Easting,Northing,Latitude(DDD.DDDDD),Longitude(DDD.DDDDD)\n");
        for (String key : printOut.keySet()) {
            String[] row = printOut.get(key);
            int rowSize = row.length;
            //System.out.println(row[rowSize-1]);
            fw_result.write(row[0] + "," + row[1] + "," + row[2] + "," + row[rowSize - 2] + "," + row[rowSize - 1] + "\n");
        }
        fw_result.close();
    }

    public static void main(String args[]) throws IOException, BiffException, DocumentException {
        SpeedPanelToProjectFormater k = new SpeedPanelToProjectFormater();
        k.getCurrentSpeedPanelsOld();
        //k.getCurrentSpeedPanels();
         //Read in the speed Panels available in the Current SpeedPanel data FILES
        k.filterSpeedPanelNodesCSV("20170925_163402_EndNode.csv"); // Filter out all the speedPanels => write the filtered file out with extension -result.csv
        k.filterSpeedPanelNodesCSV("20170925_163111_StartNode.csv"); //Filter out all the speedPanels => write the filtered file out with extension -result.csv

        // READ START NODES TO SPEEDPANELS
        HashMap<String, String[]> speedPanels = k.readInSpeedPanelsCSV("20170925_163402_EndNode.csv-result.csv"); // from filtered result read in the speedpanel node rows with location info and id
        k.readSpeedPanelsToNodes(speedPanels); // generate nodes based on this information.

        speedPanels = k.readInSpeedPanelsCSV("20170925_163111_StartNode.csv-result.csv"); // from filtered result read in the speedpanel node rows with location info and id
        k.readSpeedPanelsToNodes(speedPanels); // generate nodes based on this information.


        k.generateWaysFromNodeCombos();
        k.printWays("");
    }
}
