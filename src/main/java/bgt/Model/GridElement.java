package bgt.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by UDU on 7/29/2017.
 */
public class GridElement {

    final double LON_MIN;
    final double LON_MAX;
    final double LAT_MIN;
    final double LAT_MAX;
    private List<Segment> segmentList=new ArrayList<Segment>();
    public GridElement(double lon_min, double lon_max, double lat_min, double lat_max) {
        LON_MIN = lon_min;
        LON_MAX = lon_max;
        LAT_MIN = lat_min;
        LAT_MAX = lat_max;
    }

    public List<Segment> getSegmentList() {
        return segmentList;
    }

    public void setSegmentList(List<Segment> segmentList) {
        this.segmentList = segmentList;
    }

    public void addSegSegList(Segment segment){
        this.segmentList.add(segment);
    }




}
