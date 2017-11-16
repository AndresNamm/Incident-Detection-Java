package bgt.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by UDU on 7/29/2017.
 */
public class Grid {

    private Boundaries boundaries;

    private GridElement[][] listGrid;
    //new GridElement[numOfLatGrids][numOfLonGrids];

    public Grid(int numOfLatGrids, int numOfLonGrids, Boundaries boundaries){
        setListGrid(new GridElement[numOfLatGrids][numOfLonGrids]);
        this.boundaries = boundaries;
    }
    public GridElement[][] getListGrid() {
        return listGrid;
    }
    public void setListGrid(GridElement[][] listGrid) {
        this.listGrid = listGrid;
    }
    public void setListGridElement(int latIndx, int lonIndx, GridElement gridElement){
        listGrid[latIndx][lonIndx]=gridElement;
    }

    public List<Node> getGridBoundaries(GridElement gridElement){
        ArrayList<Node> nodeList = new ArrayList<>();
        Node leftUpNode = new Node(gridElement.LAT_MAX,gridElement.LON_MIN,"boundary");
        Node rightUpNode = new Node(gridElement.LAT_MAX,gridElement.LON_MAX, "boundary");
        Node leftDownNode = new Node(gridElement.LAT_MIN,gridElement.LON_MIN, "boundary");
        Node rightDownNode = new Node(gridElement.LAT_MIN,gridElement.LON_MAX, "boundary");
        nodeList.add(leftUpNode);
        nodeList.add(leftDownNode);
        nodeList.add(rightDownNode);
        nodeList.add(rightUpNode);
        return nodeList;
    }

    public Routes convertToRoutes(int rowID, int colID){
        Routes routes = new Routes( this.boundaries);
        ArrayList<Node> nodeList = new ArrayList<>();
        if(rowID==-1){
            for(int i = 200; i<250; i++) {
                GridElement[] row = this.listGrid[i];
                for(int j=200; j<250;j++){
                    GridElement gridElement = row[j];
                    nodeList.addAll(this.getGridBoundaries(gridElement));
                }
            }
                } else{
            GridElement gridElement = this.getListGrid()[rowID][colID];
            nodeList.addAll(this.getGridBoundaries(gridElement));
            List<Segment> segmentList=  gridElement.getSegmentList();
        }
        routes.setNodeList(nodeList);
        return routes;
    }

}
