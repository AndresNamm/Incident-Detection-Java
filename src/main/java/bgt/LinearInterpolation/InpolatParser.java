package bgt.LinearInterpolation;

import bgt.Model.Record;
import bgt.parsing.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by UDU on 8/4/2017.
 */
public class InpolatParser extends Parser {
    public void outPutRowNr(File file) throws IOException {
        LineIterator it = FileUtils.lineIterator(file);
        int cnt = 0;
        FileWriter fw_result = new FileWriter("Sample.csv");
        while (it.hasNext() ){
            String line = it.next();
            String cols[]=line.split(",");
            String ids[] =cols[0].split("_");
            String s = ids[1];
            String d = cols[1];
            //fw_result.write(line+"\n");
            cnt++;
        }
        System.out.println(cnt);
        fw_result.close();
    }


    public static HashMap<String,Long> getSegmentsArCounts(String fileName) throws IOException {
        File file = new File(fileName);
        LineIterator it = FileUtils.lineIterator(file);
        HashMap<String,Long> segmentCounts = new HashMap<>();
        while(it.hasNext()){
            String line = it.next();
            StringTokenizer st = new StringTokenizer(line,",");
            String seg_id = st.nextToken();
            if(!segmentCounts.containsKey(seg_id)){
                segmentCounts.put(seg_id,0l);
            }segmentCounts.put(seg_id,segmentCounts.get(seg_id)+1);
        }
        return segmentCounts;

    }


    public static void main(String[] args) throws IOException {
        InpolatParser ipp = new InpolatParser();
        int foldNR = 1;
        File f =  FileUtils.getFile("data/linear_interpolation/fold_"+foldNR+"/train_interpolation_result.csv");
        ipp.outPutRowNr(f);
    }

}
