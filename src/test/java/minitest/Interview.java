package minitest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Interview {

    static  List<Output> allData = new ArrayList<Output>();

    private class Output{
        private String name;
        private String county;
        private String gender;
        private Date birthday;
        private String color;

        public Output(String name, String county, String gender, String birthday, String color) throws ParseException {
            this.name = name;
            this.county = county;
            this.gender = gender;
            this.birthday = DateUtils.parseDate(birthday,new String[]{"MM/dd/yyyy","MM-dd-yyyy"});
            this.color = color;
        }

        @Override
        public String toString() {
            return this.name + " "+ county + " "+ gender + " "+ DateFormatUtils.format(birthday,"MM/dd/yyyy") + " "+ color;
        }
    }

    @Before
    public void init() throws IOException, ParseException {
        final Map<String,String> dataMirror = new HashMap<String, String>();
        dataMirror.put("F","Female");
        dataMirror.put("M","Male");

        File baseDir = new File("C:\\users\\9420\\Downloads\\data collector\\input_files");
        List<String> common = FileUtils.readLines(new File(baseDir, "comma.txt"));
        List<String> pipe = FileUtils.readLines(new File(baseDir, "pipe.txt"));
        List<String> space = FileUtils.readLines(new File(baseDir, "space.txt"));

        // parse common
        for (int i = 0; i < common.size(); i++) {
            String line = common.get(i);
            String[] split = line.split(",");
            allData.add(new Output(split[0].trim(),split[1].trim(),split[2].trim(),split[4].trim(),split[3].trim()));
        }

        //parse pipe
        for (int i = 0; i < pipe.size(); i++) {
            String line = pipe.get(i);
            String[] split = line.split("\\|");
            allData.add(new Output(split[0].trim(),split[1].trim(),dataMirror.get(split[3].trim()),split[5].trim(),split[4].trim()));
        }

        //parse space
        for (int i = 0; i < space.size(); i++) {
            String line = space.get(i);
            String[] split = line.split(" ");
            allData.add(new Output(split[0].trim(),split[1].trim(),dataMirror.get(split[3].trim()),split[4].trim(),split[5].trim()));
        }


    }
    @Test
    public void testReadOutput1() throws IOException {
        Collections.sort(allData, new Comparator<Output>() {
            @Override
            public int compare(Output o1, Output o2) {
                int result = o1.gender.compareTo(o2.gender);
                if(result == 0){
                    return o1.name.compareTo(o2.name);
                }
                return result;
            }
        });
        for (Output allDatum : allData) {
            System.out.println(allDatum);
        }
    }

    @Test
    public void testOutput2(){
        Collections.sort(allData, new Comparator<Output>() {
            @Override
            public int compare(Output o1, Output o2) {
               return o1.birthday.compareTo(o2.birthday);
            }
        });
        for (Output allDatum : allData) {
            System.out.println(allDatum);
        }
    }

    @Test
    public void testOutput3(){ // 不清楚以什么排序
//        Collections.sort(allData, new Comparator<Output>() {
//            @Override
//            public int compare(Output o1, Output o2) {
//                return o1.gender.compareTo(o2.gender);
//            }
//        });
        for (Output allDatum : allData) {
            System.out.println(allDatum);
        }
    }
}
