package bgt.SpeedPanel;

import jxl.read.biff.BiffException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppendSpeedPanel implements SpeedPanelBase {


    public final static String PREFIX = "data/speed_panels/";
    public HashMap<String, List<String>> speedPanelSegs;
    //public HashMap<String, List<String>> speedPanelObsr;

    public enum GenerationType {
        TRAIN, TEST
    }

    AppendSpeedPanel(String fileN) throws IOException {
        this.speedPanelSegs = SpeedPanelObsParser.readInSpSegs(fileN);
    }

    public HashMap<String, List<String>> generateObsForLabel(String fileN) throws IOException, BiffException {
        HashMap<String, List<String>> observations = SpeedPanelObsParser.readInSpObservations(fileN);
        HashMap<String, List<String>> segLabelObs = new HashMap<>();
        int cnt = 0;
        for (Map.Entry<String, List<String>> entry : observations.entrySet()) {// For every speedpanel
            String speedPanelId = entry.getKey();
            List<String> spObs = entry.getValue();
            //System.out.print(observationes.size()+" ");
            if (this.speedPanelSegs.containsKey(speedPanelId)){

                for (String segId : this.speedPanelSegs.get(speedPanelId)) {// For every segment for chosen speedpanel
                    segLabelObs.put(segId, new ArrayList<>());
                    for (String obs : spObs) { // for every observation in this this

                        String[] speedTime = obs.split(typeSep);
                        try {
                            cnt++;
                            int hour = Integer.valueOf(speedTime[1].split(timeSep)[0]);
                            int speed = (int) Math.floor(Double.valueOf(speedTime[0]));
                            segLabelObs.get(segId).add(SpeedPanelObsParser.generateLabelingObservation(speedTime[1], segId, speed, speedTime[2]));
                        } catch (NumberFormatException e) {
                            System.out.print(speedTime[0] + " ");
                            System.out.println(speedTime[1]);
                        }
                    }
                }
            }
        }
        return segLabelObs;
    }


    public ArrayList<String> appendSpObservations(String fileN) throws IOException, BiffException {
        HashMap<String, List<String>> observations = SpeedPanelObsParser.readInSpObservations(fileN);// SPEED_PANEL ID
        ArrayList<String> train_output = new ArrayList<>();
        int cnt = 0;
        for (Map.Entry<String, List<String>> entry : observations.entrySet()) {// For every speedpanel
            String speedPanelId = entry.getKey();
            List<String> spObs = entry.getValue();
            //System.out.print(observationes.size()+" ");
            if (this.speedPanelSegs.containsKey(speedPanelId)) {
                for (String segId : this.speedPanelSegs.get(speedPanelId)) {// For every segment for chosen speedpanel
                    for (String obs : spObs) { // for every observation in this this
                        String[] speedTime = obs.split(typeSep);
                        try {
                            cnt++;
                            int hour = Integer.valueOf(speedTime[1].split(timeSep)[0]);
                            int speed = (int) Math.floor(Double.valueOf(speedTime[0]));
                            train_output.add(SpeedPanelObsParser.generateModelTrainObs(hour, segId, speed));
                        } catch (NumberFormatException e) {
                            System.out.print(speedTime[0] + " ");
                            System.out.println(speedTime[1]);
                        }
                    }
                }
            }
        }
        return train_output;
    }

    public boolean copyInputFiles() {
        for (int i = 1; i <= 4; i++) {
            try {
                SpeedPanelObsParser.copytInterPolatResult(i);
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    public boolean createInputFiles() {
        for (int i = 1; i <= 4; i++) {
            try {
                SpeedPanelObsParser.createBaseInterPolatResultFiles(i);
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }


    public void appendAllObservations(GenerationType type, Boolean append) throws IOException, BiffException {
        int cnt = 0;
        int[] foldStart = {1, 4, 7, 10};
        int[] foldEnd = {3, 6, 9, 12};


        boolean startCondition = false;
        if (type == GenerationType.TRAIN) {
            FileUtils.cleanDirectory(new File(PREFIX + "append_train/"));
            if(append){
                startCondition = copyInputFiles();
            }
            else{
                startCondition = createInputFiles();

            }
        } else if (type == GenerationType.TEST) {
            startCondition = true;
        }
        if (startCondition) {
            for (int foldNr = 1; foldNr <= 4; foldNr++) {
                boolean dirPrepared = false;
                int startFileNr = foldStart[foldNr - 1];
                int endFileNr = foldEnd[foldNr - 1];
                for (int i = 1; i <= 12; i++) {
                    if (type == GenerationType.TRAIN && (i >= startFileNr) && (i <= endFileNr)) {
                        continue;
                    }
                    if (type == GenerationType.TEST && ((i < startFileNr) || (i > endFileNr))) {
                        continue;
                    }
                    String folderN = (i > 9 ? "2010" : "20100") + String.valueOf(i);
                    File folderF = new File(PREFIX + folderN);
                    File[] listOfFiles = folderF.listFiles();
                    for (File f : listOfFiles) {
                        if (f.isFile() && !f.getName().startsWith(".")) {
                            System.out.println(f.getPath());

                            if (type == GenerationType.TRAIN) {
                                ArrayList<String> train_data = appendSpObservations(f.getPath());
                                SpeedPanelObsParser.appendResult(PREFIX + "append_train/train_interpolation_result" + foldNr + ".csv", foldNr,train_data, true);
                            } else if (type == GenerationType.TEST) {
                                HashMap<String, List<String>> labelData = generateObsForLabel(f.getPath());
                                if(!dirPrepared){
                                    SpeedPanelObsParser.createEmptySegmentFiles(foldNr,labelData.keySet());
                                    dirPrepared=true;
                                }
                                SpeedPanelObsParser.appendIntermediateResults(foldNr,labelData);
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("Error");
        }
    }

    public static void main(String args[]) throws IOException, BiffException {
        AppendSpeedPanel speedPanelParser = new AppendSpeedPanel(AppendSpeedPanel.PREFIX + "filtered.csv");
        //speedPanelParser.appendSpObservations(AppendSpeedPanel.PREFIX+"201001/20100101.csv");
        //speedPanelParser.appendAllObservations(GenerationType.TRAIN);
        speedPanelParser.appendAllObservations(GenerationType.TRAIN, false);
    }
}
