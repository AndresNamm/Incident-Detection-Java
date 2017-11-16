package bgt.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 2016/11/16.
 */
public class Way {
    public long id;
    public List<Node> nodeList;
    public boolean isBridge;
    public boolean isTunnel;
    public String name;
    public Double VALUE;
    public String name_en;
    public String name_zh;
    public String highway;
    public String group;
    public boolean isOneWay;
    public static final double LOW_THRES = 50.0;
    public static final double HIGH_THRES = 100.0;
    public Way(long id){
        this.id = id;
    }

    public Way(long id, List<Node> nodeList){
        this.id = id;
        this.nodeList = nodeList;
    }

    public double getWayLength(){
        double dist = 0;
        for(int i = 1; i<nodeList.size();i++){
            dist+=Node.calcDist(nodeList.get(i-1),nodeList.get(i));
        }
        return dist;
    }
    public long getId() { return id; }

    public void setBridgeFlag(boolean bridge) {
        isBridge = bridge;
    }

    public void setTunnelFlag(boolean tunnel) {
        isTunnel = tunnel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName_en(String name_en) {
        this.name_en = name_en;
    }

    public String getName_en() {
        return name_en;
    }

    public void setName_zh(String name_zh) {
        this.name_zh = name_zh;
    }

    public String getName_zh() {
        return name_zh;
    }

    public void setOneWayTag(boolean oneWay) {
        isOneWay = oneWay;
    }

    // Set the nodes of the way
    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public List<Node> resegmentWay(double LOW_THRES,double HIGH_THRES){
        List<Node> oldNodeList = this.nodeList;
        List<Node> newNodeList = new ArrayList<Node>();
        newNodeList.add(oldNodeList.get(0));    //Add first node into new list
        //Initialization

        int remStartIndex = 0;  // Used to remember the start index
        int startIndex = 0;
        int endIndex = 1;
        int oldListSize = oldNodeList.size();

        Node srcNode = null;
        Node dstNode = null;

        while (endIndex < oldListSize){
            startIndex = remStartIndex;
            endIndex = startIndex+1;
            double sumDist = 0.0;
            while (endIndex < oldListSize && sumDist < LOW_THRES){
                // If less than low threshold, connect one after another until larger than low threshold
                srcNode = oldNodeList.get(startIndex++);
                dstNode = oldNodeList.get(endIndex++);  // endIndex is the index of the last Node with distance calculated +1
                sumDist += Node.calcDist(srcNode, dstNode);
            }
            if (sumDist >= LOW_THRES && sumDist <= HIGH_THRES){
                // If bigger than low threshold, and less than high threshold, keep it untouched

                newNodeList.add(dstNode);
                remStartIndex = startIndex;
            }else if (sumDist > HIGH_THRES){
                // If bigger than high threshold, then divide it equally
                int portion = 2;    // this requires that high_thres is larger than low_thres*2
                while (sumDist/portion > HIGH_THRES) portion++;
                double portDist = sumDist/portion; // The distance of each portion

                // Inner loop initialization
                int innerSrcIndex = remStartIndex;
                int innerDstIndex = innerSrcIndex+1;

                Node innerSrcNode = null;
                Node innerDstNode = null;
                double innerDist = 0.0;

                while (innerDstIndex < endIndex){
                    double innerSumDst = 0.0;
                    while (innerDstIndex < endIndex && innerSumDst < portDist){
                        innerSrcNode = oldNodeList.get(innerSrcIndex++);
                        innerDstNode = oldNodeList.get(innerDstIndex++);
                        innerDist = Node.calcDist(innerSrcNode, innerDstNode);
                        innerSumDst += innerDist;
                    }
                    if (innerSumDst >= portDist){
                        double rmvDist = innerSumDst - portDist;
                        if (rmvDist <= 2.0 && innerDstIndex == endIndex){ // if the distance need to subtract from the last segment is very small
                            newNodeList.add(innerDstNode); // then just do not subtract
                        }
                        else{
                            double newNodeLat = innerDstNode.getLat() - (rmvDist/innerDist)*(innerDstNode.getLat() - innerSrcNode.getLat());
                            double newNodeLon = innerDstNode.getLon() - (rmvDist/innerDist)*(innerDstNode.getLon() - innerSrcNode.getLon());
                            Node newNode = new Node(newNodeLat, newNodeLon);
                            newNodeList.add(newNode);
                            oldNodeList.add(innerSrcIndex, newNode);
                            endIndex ++;
                            oldListSize ++;
                        }
                    }else{  // the last portion is not long enough, but should be close
                        newNodeList.add(innerDstNode);
                    }
                }
                remStartIndex = endIndex-1;
            }else{
                // The last portion of the route is less than threshold
                newNodeList.add(dstNode);
            }
        }

        this.setNodeList(newNodeList);
        return newNodeList;
    }
}
