package bgt.MapMatching.Models;

import bgt.MapMatching.ParsePCDdb;
import bgt.Model.Node;
import bgt.Model.Record;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;

public class RecordTest {

    ArrayList<Record> recList;
    @Before
    public void setUp() throws Exception {
        recList=new ArrayList<>();
        HashMap<Integer, ArrayList<Record>> recMap = ParsePCDdb.ParseSamplePCDdb();
        for(ArrayList<Record> recL : recMap.values()){
            for(Record rec : recL){
                this.recList.add(rec);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getLocation() throws Exception {
    }

    @Test
    public void getDirection() throws Exception {
    }

    @Test
    public void toNode() throws Exception {
        HashMap<Long,Integer > idMap = new HashMap<>();
        boolean uniq = true;
        for(Record rec : this.recList){
            Node node = rec.toNode();
            Long id = node.id;
            idMap.putIfAbsent(id,0);
            idMap.put(id,idMap.get(id) +1 );
            System.out.println(id);
            System.out.println(node.lat);

            assertTrue(idMap.get(id)<2);

        }
    }
}