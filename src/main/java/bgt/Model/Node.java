package bgt.Model;

        import java.util.Comparator;

/**
 * Created by admin on 2016/11/16.
 */



class NodeComparator implements Comparator<Node> {
    @Override
    public int compare(Node i1, Node i2) {
        return i1.id > i2.id ? 1 : (i1 != i2) ? -1 : 0;
    }
}


public class Node {

    public static int numOfGenNodes = 0;
    public long id;
    public double lat;
    public double lon;
    public String visible= "true";
    public String ref="untyped";
    public String seg_id="";
    public Integer speed=0;
    public NodeType type=NodeType.POS;
    public enum NodeType {
        RECORD,ACCIDENT,POS
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;
        return id == node.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    public  static final double EARTH_RADIUS = 6378137.0;   //in meters

    public Node(long id){
        this.id = id;
    }

    public Node(double lat, double lon){
        this.id = ++numOfGenNodes;
        this.lat = lat;
        this.lon = lon;
    }
    public Node(double lat, double lon,String ref){
        this.id = ++numOfGenNodes;
        this.lat = lat;
        this.lon = lon;
        this.ref = ref;
    }


    public static int getNumOfGenNodes() {
        return numOfGenNodes;
    }

    public static void setNumOfGenNodes(int numOfGenNodes) {
        Node.numOfGenNodes = numOfGenNodes;
    }

    public Node(long id, double lat, double lon){
        this.id = id;
        this.lat = lat;
        this.lon = lon;

    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", lat=" + lat +
                ", lon=" + lon +
                ", visible='" + visible + '\'' +
                ", ref='" + ref + '\'' +
                '}';
    }

    //Convert coordinate(Latitude/Longitude) into radian
    public static double toRad(double coord){
        return coord/180.0*Math.PI;
    }

    //Calculate the distance between two points
    public static double calcDist(double aLat, double aLon, double bLat, double bLon){
        double radALat = toRad(aLat);
        double radBLat = toRad(bLat);
        double a = radALat - radBLat;
        double b = toRad(aLon) - toRad(bLon);
        double s = 2*Math.asin(Math.sqrt(Math.pow(Math.sin(a/2), 2) +
                Math.cos(aLat)*Math.cos(bLat)*Math.pow(Math.sin(b/2), 2) ));
        s *= EARTH_RADIUS;
        return s;
    }

    //Calculate the distance between two points
    public static double calcDist(Node aNode, Node bNode){
        double aLat = aNode.getLat();
        double aLon = aNode.getLon();
        double bLat = bNode.getLat();
        double bLon = bNode.getLon();
        return calcDist(aLat, aLon, bLat, bLon);
    }



    public static NodeComparator getComparator(){
        return new NodeComparator();
    }


}
