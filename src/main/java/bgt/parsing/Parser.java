package bgt.parsing;

import jxl.read.biff.BiffException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.DocumentException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;

/**
 * Created by UDU on 8/5/2017.
 */
public class Parser {

    public static String defaultDelimiter = "\t";

    public static void printArrayListLines(List<String> lines, String fileName, boolean append) throws IOException {
        if (!append) {
            try {
                Files.delete(new File(fileName).toPath());
            } catch (NoSuchFileException e) {
                System.out.println("No such file exists");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileWriter fw = new FileWriter(fileName,append);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);
        for (String line : lines) {
            out.println(line);
        }
        out.close();
    }

    public static long countLinesFile(String fileName) throws IOException {
        File f = new File(fileName);
        LineIterator it = FileUtils.lineIterator(f);
        long cnt = 0;
        while (it.hasNext()) {
            it.nextLine();
            cnt++;
        }
        return cnt;

    }

    public static void printLinesFile(File file, int boundary) throws IOException {
        LineIterator it = FileUtils.lineIterator(file);
        int cnt = 0;
        FileWriter fw_result = new FileWriter(file.getPath()+"Sample.txt");
        while (it.hasNext() && cnt < boundary) {
            String line = it.next();
            String newLine = modBeforePrint(line);
            fw_result.write(line + "\n");
            cnt++;
        }
        System.out.println(cnt);
        fw_result.close();
    }

    public static String modBeforePrint(String line) {
        return line;
    }

    public static void copyFileUsingApacheCommonsIO(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }

}
