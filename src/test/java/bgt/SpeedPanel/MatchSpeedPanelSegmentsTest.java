package bgt.SpeedPanel;

import bgt.MapMatching.MapMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class MatchSpeedPanelSegmentsTest {
    MatchSpeedPanelSegments matchSpeedPanelSegments ;

    @Before
    public void setUp() throws Exception {
        MatchSpeedPanelSegments matchSpeedPanelSegments = new MatchSpeedPanelSegments("speedPanelWays.osm");
        matchSpeedPanelSegments.indexSegs();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void segToRecs() throws Exception {
    }

    @Test
    public void putPanelSegMap() throws Exception {
    }

    @Test
    public void matchToSpeedPanels() throws Exception {
    }

    @Test
    public void doMatching() throws Exception {
        MapMatcher mm = new MapMatcher();
        HashMap<String, List<String>> speedPanelSegs = SpeedPanelObsParser.readInSpSegs("data/speed_panels/spMatched.txt");
        for(String spId : speedPanelSegs.keySet()){
            for( String segId : speedPanelSegs.get(spId)){
                System.out.println(segId);
                assert(mm.segMap.containsKey(segId));
            }
        }
    }

    @Test
    public void printOutResult() throws Exception {
    }

    @Test
    public void printOutPanelsWSegments() throws Exception {
    }

    @Test
    public void main() throws Exception {
    }

}