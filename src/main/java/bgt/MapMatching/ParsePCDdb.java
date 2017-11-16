package bgt.MapMatching;

import bgt.Model.Record;
import bgt.Model.Node;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ParsePCDdb {

    // This code is based on MapMatcher.parseRecords function
    // 1. Get 5 first taxi device ID
    // 2. Read the records FROM database into HashMap (Device ID, List of Records) MAP


    //Other class 3. Print out Time variables and differences with previous observation for each Device.



    public  static HashMap<Integer, ArrayList<Record>>  GetDeviceIds(int nrDevices) throws IOException {

        String records_filename = "data/GPS_data/TaxiData201000.mdb";
        List<Integer> dev_list = new ArrayList<>();
        HashMap<Integer, ArrayList<Record>> recordCollection = new HashMap<>();
        Database db = DatabaseBuilder.open(new File(records_filename));
        Table table = db.getTable("TAXIDATA");
        for(Row row : table) {
            int dev_id = Integer.valueOf((String) row.get("DevID"));
            if (recordCollection.size() < nrDevices) {
                if (!recordCollection.containsKey(dev_id)) {
                    recordCollection.put(dev_id, new ArrayList<Record>());
                }
            }else{
                break;
            }
        }

        db.close();
        return recordCollection;
    }

    public static  HashMap<Integer, ArrayList<Record>>  ParseSamplePCDdb() throws IOException {
        String records_filename = "data/GPS_data/TaxiData201000.mdb";
        List<Integer> dev_list = new ArrayList<>();
        HashMap<Integer, ArrayList<Record>> recordCollection = GetDeviceIds(5);
        Database db = DatabaseBuilder.open(new File(records_filename));
        Table table = db.getTable("TAXIDATA");
        for(Row row : table){
            int dev_id = Integer.valueOf((String)row.get("DevID"));
            if(recordCollection.containsKey(dev_id)){
                long rec_id = new Long((int)row.get("PosID"));
                if (dev_list.indexOf(dev_id) == -1){
                    dev_list.add(dev_id);
                }
                DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
                String date = df.format((Date)row.get("HkDt"));
                String[] dataFrags = date.split("/");
                int month = Integer.valueOf(dataFrags[1]);
                int day = Integer.valueOf(dataFrags[2]);

                DateFormat tf = new SimpleDateFormat("HH:mm:ss");
                String time = tf.format((Date)row.get("HkTm"));
                String[] timeFrags = time.split(":");
                double hour = Double.valueOf(timeFrags[0]);
                double minute = Double.valueOf(timeFrags[1]);
                double second = Double.valueOf(timeFrags[2]);
                hour = hour + minute/60.0 + second/3600.0;

                double lat = new Double((float)row.get("Lat"));
                double lon = new Double((float)row.get("Lon"));
                Node location = new Node(lat, lon);
                double speed = new Double((float)row.get("SpeedKmHr"));

                double direction = new Double((float)row.get("Direction"));

                Record record = new Record(rec_id, dev_id, month, day, hour, location,
                        speed, direction);
                recordCollection.get(dev_id).add(record);
            }
        }
        db.close();
        return recordCollection;
    }

    public static boolean printSampleRecordMap(HashMap<Integer, ArrayList<Record>> recordCollection) throws IOException {
        String fileName = recordCollection.size() + "device_records.txt";
        FileWriter fw_result = new FileWriter(fileName);
        for(int dev_id : recordCollection.keySet()){
            fw_result.write(dev_id + "\n");
            for(Record rec: recordCollection.get(dev_id)){
                fw_result.write(rec + "\n");
            }
        }
        fw_result.close();
        return  true;
    }
}
