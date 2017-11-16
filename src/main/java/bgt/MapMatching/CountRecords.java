package bgt.MapMatching;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;

import java.io.File;
import java.io.IOException;

/**
 * Created by admin on 2016/11/25.
 */
public class CountRecords {
    public static void main (String[] args) throws IOException{
        int sumNumOfRecords = 0;
        for (int i=0; i<=52; i++){
            String filename = "";
            if (i<10){
                filename = "data/GPS_data/TaxiData20100"+i+".mdb";
            }else{
                filename = "data/GPS_data/TaxiData2010"+i+".mdb";
            }
            Database db = DatabaseBuilder.open(new File(filename));
            Table table = db.getTable("TAXIDATA");
            int numOfRecords = table.getRowCount();
            sumNumOfRecords += numOfRecords;
            System.out.println(filename + ":\t" + numOfRecords);
            db.close();
        }
        System.out.println("Total number of records: " + sumNumOfRecords);
    }
}
