package bgt.SpeedPanel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class SpeedPanelToProjectFormaterTest {

    @Before
    public void setUp() throws Exception {


    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void addNodeId() throws Exception {
    }

    @Test
    public void addNodeId1() throws Exception {
    }

    @Test
    public void printWays() throws Exception {
    }

    @Test
    public void generateWaysFromNodeCombos() throws Exception {
    }

    @Test
    public void getCurrentSpeedPanels() throws Exception {

        SpeedPanelToProjectFormater newSol = new SpeedPanelToProjectFormater();
        newSol.getCurrentSpeedPanels();
        System.out.println(newSol.getPanelCombosStr());
        System.out.println(newSol.panelCombos.size());
        System.out.println(newSol.panelNodeMap);
        System.out.println(newSol.panelNodeMap.size());

        SpeedPanelToProjectFormater oldSol = new SpeedPanelToProjectFormater();
        oldSol.getCurrentSpeedPanelsOld().size();

        System.out.println(oldSol.getPanelCombosStr());
        System.out.println(oldSol.panelCombos.size());
        System.out.println(oldSol.panelNodeMap);
        System.out.println(oldSol.panelNodeMap.size());

        HashMap<String,String[]> checker = new HashMap<>();
        for(String[] k : newSol.panelCombos){
            checker.putIfAbsent(String.join("-",k), k);
        }

        for(String[] k : oldSol.panelCombos) {
            if (!checker.containsKey(String.join("-", k))) {
                System.out.println(String.join("-", k));
            }
        }
    }

    @Test
    public void getCurrentSpeedPanelsOld() throws Exception {


    }

    @Test
    public void getHeaderInfo() throws Exception {
    }

    @Test
    public void getPanelCombosStr() throws Exception {
    }

    @Test
    public void readSpeedPanelsToNodes() throws Exception {
    }

    @Test
    public void readInSpeedPanelsCSV() throws Exception {
    }

    @Test
    public void filterSpeedPanelNodesCSV() throws Exception {
    }

    @Test
    public void main() throws Exception {
    }

}