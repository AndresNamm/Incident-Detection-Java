package bgt.IncidentDetection;

import bgt.IncidentDetection.Models.EvaluateResult;
import bgt.IncidentDetection.Models.LabeledRecord;
import bgt.IncidentDetection.Models.SegDistribution;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by admin on 2016/12/2.
 */
public class IncidentDetection {
    public List<SegDistribution> segDistributionList;
    public HashMap<String, SegDistribution> segDistributionMap;
    public static final int TIME_WINDOW = 300; //can be 60, 300, 600, 1800, 3600


    public void parseDistribution(String distribution_filename) throws FileNotFoundException, IOException{
        segDistributionList = new ArrayList<>();
        segDistributionMap = new HashMap<>();
        InputStreamReader is = new InputStreamReader(new FileInputStream(distribution_filename));
        BufferedReader br = new BufferedReader(is);
        String line = null;
        int line_count = 1;
        while ((line = br.readLine()) != null){
            if (line_count == 1){   // info about # of mixtures
                String[] line_split = line.split(" ");
                SegDistribution.NUM_MIXTURE = Integer.valueOf(line_split[line_split.length-1]);
                line_count++;
            }else if (line_count == 3){
                SegDistribution.lambdas = new double[SegDistribution.NUM_MIXTURE];
                SegDistribution.deltas = new double[SegDistribution.NUM_MIXTURE][SegDistribution.NUM_MIXTURE];
                for (int i=0; i<SegDistribution.NUM_MIXTURE; i++){
                    line = br.readLine();
                    SegDistribution.lambdas[i] = Double.valueOf(line);
                    line_count++;
                }
                for (int i=0; i<SegDistribution.NUM_MIXTURE; i++){
                    for (int j=0; j<SegDistribution.NUM_MIXTURE; j++){
                        SegDistribution.deltas[i][j] = KL_Distance(SegDistribution.lambdas[i], SegDistribution.lambdas[j]);
                    }
                }// DELTAS ON SIIS MINGI KL_DISTANCE BETWEEN THE K DISTRIBUTION.
            }else if(line_count >= 10){
                String[] line_split = line.split(",");
                String seg_id = line_split[0];
                double[] pis = new double[SegDistribution.NUM_MIXTURE];
                for (int i=2; i<2+SegDistribution.NUM_MIXTURE; i++){
                    pis[i-2] = Double.valueOf(line_split[i]);
                }
                double[] sigmas = new double[SegDistribution.NUM_MIXTURE];
                for (int i=0; i< SegDistribution.NUM_MIXTURE; i++){
                    double sigma = 0.0;
                    for (int j=0; j < SegDistribution.NUM_MIXTURE; j++){
                        sigma += pis[j]*SegDistribution.deltas[j][i];
                    }
                    sigmas[i] = sigma;
                }
                SegDistribution segDistribution = new SegDistribution(seg_id, pis, sigmas);
                segDistributionList.add(segDistribution);
                segDistributionMap.put(seg_id, segDistribution);
                line_count++;
            }else{
                line_count++;
            }
        }
        br.close();
        is.close();
    }

    public double factorial(int x) {
        double fact = 1;
        for (int i = 2; i <= x; i++) {
            fact *= i;
        }
        return fact;
    }

    public double poissonDistribution(double lambda, int x){
        return (Math.pow(lambda, x)*Math.exp(-lambda))/factorial(x);
    }

    public double KL_Distance(double lambda_k, double lambda_l){
        return lambda_l - lambda_k + lambda_k*Math.log(lambda_k/lambda_l);
    }

    public double evalDivergence(String seg_id, int speed){
        if (segDistributionMap.containsKey(seg_id)){
            SegDistribution segDistribution = segDistributionMap.get(seg_id);
            // look for the Usual Traffic State
            int delta_s = 0;
            double max_pi_s = segDistribution.pis[0];
            for (int i=1; i<SegDistribution.NUM_MIXTURE; i++){
                if (segDistribution.pis[i] > max_pi_s){
                    delta_s = i;
                    max_pi_s = segDistribution.pis[i];
                }
            }

            // look for the Current Traffic State
            int delta_sx = 0;
            double max_pi_sx = segDistribution.pis[0] * poissonDistribution(SegDistribution.lambdas[0], speed);
            for (int i=1; i<SegDistribution.NUM_MIXTURE; i++){
                double this_pi_sx = segDistribution.pis[i]* poissonDistribution(SegDistribution.lambdas[i], speed);
                if (this_pi_sx > max_pi_sx){
                    delta_sx = i;
                    max_pi_sx = this_pi_sx;
                }
            }
            double lambda_s = SegDistribution.lambdas[delta_s];
            double lambda_sx = SegDistribution.lambdas[delta_sx];
            return KL_Distance(lambda_s, lambda_sx);
        }
        return Double.MAX_VALUE;
    }

    public double evalDivergence_weighted(String seg_id, int speed){
        if (segDistributionMap.containsKey(seg_id)){
            SegDistribution segDistribution = segDistributionMap.get(seg_id);
            double[] sigmas = segDistribution.sigmas;
            double[] pis = new double[SegDistribution.NUM_MIXTURE];
            double pi_sum = 0.0;
            for (int i=0; i<SegDistribution.NUM_MIXTURE; i++){
                pis[i] = segDistribution.pis[i]*poissonDistribution(SegDistribution.lambdas[i], speed);
                pi_sum += pis[i];
            }
            double result = 0.0;
            for (int i=0; i<SegDistribution.NUM_MIXTURE; i++){
                result += sigmas[i]*(pis[i]/pi_sum);
            }
            return result;
        }
        return Double.MAX_VALUE;
    }

    public List<EvaluateResult> detectOnStream(String seg_id, List<LabeledRecord> recList){
        // return list of anomaly values
        List<EvaluateResult> streamResult = new ArrayList<>();
        int totalRecNum = recList.size();
        if (totalRecNum == 1){
            double divergence = evalDivergence(seg_id, recList.get(0).speed);
            double divergence_weighted = evalDivergence_weighted(seg_id, recList.get(0).speed);
            EvaluateResult evalRes = new EvaluateResult(divergence, divergence_weighted, recList.get(0).acc_flag);
            streamResult.add(evalRes);
        }else{
            int windowStart = 0;
            while (windowStart < recList.size()){   // until start from the last record
                int windowEnd = windowStart;
                while ( windowEnd+1 < recList.size() &&
                        ((int)(recList.get(windowEnd+1).time - recList.get(windowStart).time)*3600) < TIME_WINDOW){
                    windowEnd++;
                }

                double divSum = 0;
                double divSum_weighted = 0;
                int acc_flag = 0;
                for (int i=windowStart; i<=windowEnd; i++){
                    divSum += evalDivergence(seg_id, recList.get(i).speed);
                    divSum_weighted += evalDivergence_weighted(seg_id, recList.get(i).speed);
                    if (recList.get(i).acc_flag == 1) acc_flag = 1;
                }
                int windowLen = windowEnd-windowStart+1;
                EvaluateResult evalRes = new EvaluateResult(divSum/windowLen, divSum_weighted/windowLen, acc_flag);
                streamResult.add(evalRes);

                windowStart++;  // next window
            }
        }
        return streamResult;
    }

    public void parseLabeledRecords(String label_filename_prefix, int fold) throws FileNotFoundException, IOException{
        List<Long> acc_route_list = new ArrayList<>();   // List of route numbers that has at least one positive record
        FileUtils.cleanDirectory(new File("data/incident_detection/TW_"+TIME_WINDOW+"/fold_"+fold+"/"));

        InputStreamReader is = new InputStreamReader(new FileInputStream(label_filename_prefix+"_list.txt"));
        BufferedReader br = new BufferedReader(is);
        FileWriter fw;
        String line = null;
        while ((line = br.readLine()) != null){
            String[] line_split = line.split("\t");
            acc_route_list.add(Long.valueOf(line_split[0])); //
        }
        br.close();
        is.close();
        for (long route_num : acc_route_list){// Reads in way files
            is = new InputStreamReader(new FileInputStream(label_filename_prefix+"_"+route_num+".txt"));
            br = new BufferedReader(is);
            List<EvaluateResult> routeResult = new ArrayList<>();
            String seg_id = null;
            List<LabeledRecord> recList = new ArrayList<>();   // one day's record stream for one segment
            while ((line = br.readLine()) != null){
                if (!line.equals("")){  // not empty line
                    String[] line_split = line.split("\t");
                    if (seg_id == null) seg_id = line_split[0];
                    LabeledRecord lRec = new LabeledRecord(Double.valueOf(line_split[1]), Integer.valueOf(line_split[2]), Integer.valueOf(line_split[3]));
                    recList.add(lRec);
                }else{
                    routeResult.addAll(detectOnStream(seg_id, recList));
                    seg_id = null;
                    recList.clear();
                }
            }
            routeResult.addAll(detectOnStream(seg_id, recList));    // last stream
            br.close();
            is.close();

            fw = new FileWriter("data/incident_detection/TW_"+TIME_WINDOW+"/fold_"+fold+"/detect_result_"+route_num+".txt",true);
            double min_anomaly_value = Double.MAX_VALUE, min_anomaly_value_weighted = Double.MAX_VALUE;
            double max_anomaly_value = Double.MIN_VALUE, max_anomaly_value_weighted = Double.MIN_VALUE;
            for (EvaluateResult evalRes : routeResult){
                min_anomaly_value = evalRes.anomaly_val < min_anomaly_value ? evalRes.anomaly_val : min_anomaly_value;
                min_anomaly_value_weighted = evalRes.anomaly_val_weighted < min_anomaly_value_weighted ?
                        evalRes.anomaly_val_weighted : min_anomaly_value_weighted;
                max_anomaly_value = (evalRes.anomaly_val > max_anomaly_value && evalRes.anomaly_val!=Double.MAX_VALUE && Double.isFinite(evalRes.anomaly_val))? evalRes.anomaly_val : max_anomaly_value;
                max_anomaly_value_weighted = (evalRes.anomaly_val_weighted > max_anomaly_value_weighted && evalRes.anomaly_val_weighted!=Double.MAX_VALUE && Double.isFinite(evalRes.anomaly_val_weighted))?
                        evalRes.anomaly_val_weighted : max_anomaly_value_weighted;
            }
            int acc_num = 0,total_num = 0;
            for (EvaluateResult evalRes : routeResult){
                if (evalRes.acc_label == 1){
                    acc_num++;
                }
                total_num++;
            }
            fw.write("DR\tFAR\n");
            fw.write("KL Result\n");
            double threshold = min_anomaly_value;
            double step = (max_anomaly_value - min_anomaly_value) / 1000.0;
            int detected_num, false_alarm_num;
            while (threshold <= max_anomaly_value){
                detected_num = 0; false_alarm_num = 0;
                for (EvaluateResult evalRes : routeResult){
                    if (evalRes.acc_label == 1 && evalRes.anomaly_val >= threshold){
                        detected_num++;
                    }
                    if (evalRes.acc_label == 0 && evalRes.anomaly_val >= threshold){
                        false_alarm_num++;
                    }
                }
                double DR = (double) detected_num / (double) acc_num;
                double FAR = (double) false_alarm_num / (double) total_num;
                fw.write(DR+"\t"+FAR+"\n");
                threshold += step;
            }
            fw.write("Weighted KL Result\n");

            threshold = min_anomaly_value_weighted;
            step = (max_anomaly_value_weighted - min_anomaly_value_weighted) / 1000.0;
            while (threshold <= max_anomaly_value_weighted){
                detected_num = 0; false_alarm_num = 0;
                for (EvaluateResult evalRes : routeResult){
                    if (evalRes.acc_label == 1 && evalRes.anomaly_val_weighted >= threshold){
                            detected_num++;
                    }
                    if (evalRes.acc_label == 0 && evalRes.anomaly_val_weighted >= threshold){
                        false_alarm_num++;
                    }
                }
                double DR = (double) detected_num / (double) acc_num;
                double FAR = (double) false_alarm_num / (double) total_num;
                fw.write(DR+"\t"+FAR+"\n");
                threshold += step;
            }
            fw.close();
            br.close();
            is.close();
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException{
        for (int i=1; i<=4; i++){
            IncidentDetection id = new IncidentDetection();
            id.parseDistribution("data/estimate_result/fold_"+i+"/estimate.out");
            id.parseLabeledRecords("data/label_data/fold_"+i+"/label_result", i);
        }

    }
}
