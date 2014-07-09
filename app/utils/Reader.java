package utils;

import scala.Int;
import scala.Tuple2;

import java.io.*;

/**
 * Created by jinwei on 8/7/14.
 */
public class Reader {
    public Tuple2<String, Int> reader(File file, Long from, Long offset){
        byte buffer[]=new byte[Integer.parseInt(offset.toString())];
        int len = 0;
        try {
            FileInputStream fi=new FileInputStream(file);
            fi.skip(from);
            InputStream bi = new BufferedInputStream(fi);
            len = bi.read(buffer);
            bi.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new  Tuple2(new String(buffer),len);
    }
}
