package bgt.SpeedPanel;

import bgt.MapMatching.MapMatcher;
import bgt.Model.Record;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.print.attribute.HashAttributeSet;
import java.io.File;
import java.util.HashMap;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

public class AppendSpeedPanelTest {


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void appendSpObservations() throws Exception {


    }

    @Test
    public void copyInputFiles() throws Exception {
    }

    @Test
    public void appendAllObservations() throws Exception {
        MapMatcher mm = new MapMatcher();
        String fileN = "data/speed_panels/labeling/observations"+1+".csv";
        File file = FileUtils.getFile(fileN);
        LineIterator it = FileUtils.lineIterator(file);
        String segId;
        String time;
        String speed;
        String date;
        int goodCount=0;
        int badCount=0;
        HashMap<String,String> goodOnes = new HashMap<>();
        HashMap<String,String> badOnes = new HashMap<>();
        while (it.hasNext()) {
            String line = it.nextLine();

            try{
                StringTokenizer st = new StringTokenizer(line,",");
                segId = st.nextToken();
                //assert(mm.segMap.containsKey(segId));
                if(mm.segMap.containsKey(segId)){
                    goodCount++;
                    goodOnes.putIfAbsent(segId,"");
                }else{
                    badCount++;
                    badOnes.putIfAbsent(segId,"");
                }
//                time = st.nextToken();
//                speed = st.nextToken();
//                date= st.nextToken();
//                Record rec = SpeedPanelObsParser.generateRecord(segId,time,speed,date);
            }catch(Exception e){
                System.out.println(line);
            }//segsMap.get(rec.seg_id).addRecord(rec);
        }
        System.out.println("goodCount: " +goodCount);
        System.out.println("badCount: " +badCount);
        System.out.println("badOnes" + badOnes);
        assert(badCount == 0);
    }

    @Test
    public void main() throws Exception {
    }

}