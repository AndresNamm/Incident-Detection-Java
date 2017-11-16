package bgt.Model;

import java.util.*;

public class Routes {
    public Routes(Boundaries boundaries){
        this.setBoundaries(boundaries);
    }
    private Boundaries boundaries;
    private List<Way> wayList= new ArrayList<>();
    private List<Node> nodeList=new ArrayList<>();
    public List<Way> getWayList() {
        return wayList;
    }

    public void setWayList(List<Way> wayList) {
        this.wayList = wayList;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public void addNode(Node node){
        this.nodeList.add(node);
    }

    public void addWay(Way way){
        this.wayList.add(way);
    }

    public Boundaries getBoundaries() {
        return boundaries;
    }

    public void setBoundaries(Boundaries boundaries) {
        this.boundaries = boundaries;
    }

    public void resegmentWays(double LOW_THRES, double HIGH_THRES){
        Map<Long,Node> updated = new HashMap<>();
        Iterator<Way> it = this.getWayList().iterator();
        while (it.hasNext()){
            Way way = it.next();
            List<Node> nodesList = way.resegmentWay(50.0, 100.0); // Call the method of the way to resegment itself
            for(Node node : nodesList){
                updated.put(node.id,node);
            }
        }
        this.setNodeList(new ArrayList<>(updated.values()));
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("NrOfNodes " + this.nodeList.size()+ System.lineSeparator());
        sb.append("NrOfWays " + this.wayList.size() + System.lineSeparator());
        return sb.toString();
    }






}
