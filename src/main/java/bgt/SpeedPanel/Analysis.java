package bgt.SpeedPanel;

import bgt.parsing.Parser;

import java.io.IOException;

public class Analysis {


    public static Long countAvgSp() throws IOException {
        Long count = 0L;
        for(int foldNr =1; foldNr<=4;foldNr++){
           String fileName = "data/speed_panels/append_train/train_interpolation_result" + foldNr + ".csv";
            count+=Parser.countLinesFile(fileName);
        }

        return count/4;
    }
    public static void main(String args[]) throws IOException {
        System.out.println(Analysis.countAvgSp());
    }
}
