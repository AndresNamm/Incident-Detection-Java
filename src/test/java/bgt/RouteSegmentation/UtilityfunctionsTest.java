package bgt.RouteSegmentation;

import org.junit.Before;

import java.util.Random;


/**
 * Created by UDU on 7/21/2017.
 */
public class UtilityfunctionsTest{

    RouteSegmentor roadMapParser;
    Random randomGenerator;
    @Before
    public void setUp() throws Exception {
        this.roadMapParser = new RouteSegmentor();
        //roadMapParser.parseNodesAndWays("");

    }

//    @Test
//    public void calculateDistanceInWay() throws Exception {
//
//        for(int i =0;i<1000;i++){
//
//            Way way = roadMapParser.wayList.get(randomGenerator.nextInt(roadMapParser.wayList.size()-1));
//            double result[]=Utilityfunctions.calculateDistanceInWay(way);
//            assertTrue("Distance cant be negative, I failed with way"+way.getId(),result[0]>0.0&&result[0]>0.0);
//
//        }
//
//
//    }



}