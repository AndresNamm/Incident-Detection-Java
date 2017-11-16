package bgt.SpeedPanel;

import bgt.Model.Record;
import bgt.Model.Segment;
import bgt.parsing.Parser;
import jxl.read.biff.BiffException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.io.*;

import java.nio.file.NoSuchFileException;
import java.util.*;

public class SpeedPanelObsParser extends Parser implements SpeedPanelBase {
    public static HashMap<String, List<String>> readInSpSegs(String fileN) throws IOException {

        //DATASTRUCTURES
        HashMap<String, List<String>> speedPanelSegs = new HashMap<>();
        String flag;
        String speedPanelId = "";
        //FILE IO
        File file = FileUtils.getFile(fileN);
        LineIterator it = FileUtils.lineIterator(file);
        while (it.hasNext()) {
            String line = it.nextLine();
            try{
                StringTokenizer st = new StringTokenizer(line);

            flag = st.nextToken();
            if (flag.equals("P")) {
                speedPanelId = st.nextToken();
                speedPanelSegs.put(speedPanelId, new ArrayList<>());
            } else {
                String segmentId = st.nextToken();
                speedPanelSegs.get(speedPanelId).add(segmentId);
            }
            }catch (NoSuchElementException e){
                System.out.println(line);
            }
        }
        return speedPanelSegs;
    }

    public static HashMap<String, List<String>> readInSpObservations(String fileN) throws IOException, BiffException {
        //DATASTRUCTURE
        HashMap<String, List<String>> panelObsrv = new HashMap<>();
        //IO
        BufferedReader br = new BufferedReader(new FileReader(fileN));
        String line = "";

        int offset = 2;
        int cnt = 0;
        //br.readLine();
        //br.readLine();
        line = br.readLine();
        String[] header = line.split(",");

        if (!header[0].equals("Date")) {
            System.out.println("Formating fault");
            return panelObsrv;
        }
        HashMap<Integer, String> colDict = new HashMap<>();
        for (int i = 0; i < header.length - 2; i++) {
            if (i % 3 == 0) {
                String speedPanelId = header[i + offset].split(" ")[1];
                panelObsrv.put(speedPanelId, new ArrayList<>());
                colDict.put(i + offset, speedPanelId);
            }
        }

        while ((line = br.readLine()) != null) {
            String[] observations = line.split(",");
            String time = observations[1];
            String date = observations[0];
            for (int i = 0; i < observations.length; i++) {
                if (colDict.containsKey(i)){
                    String speedPanelId = colDict.get(i);
                    if ( NumberUtils.isNumber(observations[i]) && !observations[i].equals("")) {
                        panelObsrv.get(speedPanelId).add(observations[i] + typeSep + time + typeSep + date);
                    }
                }
            }

        }
        return panelObsrv;


    }

    public static String generateLabelingObservation(String time, String segmentId, Integer speed , String date){
        return segmentId +","+time+","+ String.valueOf(speed)+"," +date;

    }

    public static String generateModelTrainObs(int hour, String segId, int speed) {

        String[] segIdSplit = segId.split("_");
        return segIdSplit[0] + "_" + segIdSplit[1] + "_" + String.valueOf(hour) + "," + String.valueOf(speed);
    }

    public static Record generateRecord(String segId, String time, String speed, String date){
        int month, day;
        double dSpeed, hour ;
        String[] timeFrags = time.split(timeSep);
        hour = Double.valueOf(timeFrags[0]);
        double minute = Double.valueOf(timeFrags[1]);
        double second = Double.valueOf(timeFrags[2]);
        hour = hour + minute/60.0 + second/3600.0;
        String[] dataFrags = date.split("/");
        month = Integer.valueOf(dataFrags[1]);
        day = Integer.valueOf(dataFrags[0]);
        dSpeed = Double.valueOf(speed);
        Record rec = new Record(0,month,day,hour,dSpeed,segId);
        return rec;
    }

    public static void appendResult(String fileN, int foldNr, ArrayList<String> observations, boolean append) {
        try (FileWriter fw = new FileWriter(fileN, append);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            for (String observation : observations) {
                out.println(observation);
            }
            out.close();
            //more code
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            e.printStackTrace();
        }
    }

    public static void copytInterPolatResult(int foldNr) throws IOException {// RUN THIS ONLY IF YOU HAVE GENERATED
        //NEW INTREPOLATION RESULT
        File inputF = new File("data/linear_interpolation/fold_" + foldNr + "/train_interpolation_result.csv");
        File outPutF = new File("data/speed_panels/append_train/train_interpolation_result" + foldNr + ".csv");
        copyFileUsingApacheCommonsIO(inputF, outPutF);
    }


    public static void createBaseInterPolatResultFiles(int foldNr) throws IOException {// RUN THIS ONLY IF YOU HAVE GENERATED
        //NEW INTREPOLATION RESULT
        //File inputF = new File("data/linear_interpolation/fold_" + foldNr + "/train_interpolation_result.csv");
        File outPutF = new File("data/speed_panels/append_train/train_interpolation_result" + foldNr + ".csv");
        //copyFileUsingApacheCommonsIO(inputF, outPutF);
    }


    public static void createEmptySegmentFiles(int foldNr, Set<String> segIds) throws IOException {
        FileUtils.cleanDirectory(new File("data/speed_panels/labeling/fold_"+foldNr+"/"));
        for(String segId : segIds){
            File f = new File("data/speed_panels/labeling/fold_"+foldNr+"/"+segId);
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
    };

    public static void appendIntermediateResults(int foldNr,HashMap<String, List<String>> labelData) throws IOException {
        for(String segId : labelData.keySet()){
            if(labelData.get(segId).size()>0){
                FileWriter fw = new FileWriter("data/speed_panels/labeling/fold_"+foldNr+"/"+ segId, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw);
                for(String line : labelData.get(segId)){
                    out.println( line);
                }
                out.close();
            }
        }
    }

    public static void main(String args[]) throws IOException, BiffException {
        SpeedPanelObsParser.readInSpObservations("data/speed_panels/201001/20100101.csv");

//        for(int i = 1; i<5;i++){
//            copytInterPolatResult(i);
//        }
    }

}
