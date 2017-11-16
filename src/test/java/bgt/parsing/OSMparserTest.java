package bgt.parsing;

import bgt.Model.Routes;
import bgt.Model.Way;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class OSMparserTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void routesToOSM() throws Exception {
    }

    @Test
    public void parseNodesAndWays() throws Exception {
        Routes routes =OSMparser.parseNodesAndWays("Hong_Kong-result.osm");
        List<Way> wayList = routes.getWayList();
        OSMparser.waysToOsm(wayList,routes,"WayResult.osm");
    }


    @Test
    public void addNodeElementToXmlElement() throws Exception {
    }

    @Test
    public void addWayElementToXmlElement() throws Exception {
    }

    @Test
    public void buildBoundaries() throws Exception {
    }

    @Test
    public void addBoundsToRoot() throws Exception {
    }

    @Test
    public void waysToOsm() throws Exception {
    }

    @Test
    public void wayToOSM() throws Exception {
    }

}