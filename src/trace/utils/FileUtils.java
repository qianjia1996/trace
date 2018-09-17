/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trace.utils;

import com.google.gson.Gson;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import trace.subtitle.Caption;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileUtils {
    public static void main(String[] args) {

    }
    /*
     * 读取的文件是一行一个单词，返回 ArrayList<String>
    */
    public static ArrayList<String>  read(String filePathString) {


        ArrayList<String> list = new ArrayList<>();

        BufferedReader in = null;
        try {

            // 创建一个使用默认大小输入缓冲区的缓冲字符输入流
            in = new BufferedReader(new InputStreamReader(new FileInputStream(filePathString), "UTF-8"));
            String line = null;
            while (null != (line = in.readLine())) {
                list.add(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return list;

    }

    /**
     * 读取文件，返回 String
     **/
    public static String readJSON(String filePathString) {
        StringBuilder builder = new StringBuilder();

        BufferedReader in = null;
        try {
            // 创建一个使用默认大小输入缓冲区的缓冲字符输入流
            in = new BufferedReader(new InputStreamReader(new FileInputStream(filePathString), "UTF-8"));
            String line = null;
            while (null != (line = in.readLine())) {
                builder.append(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return builder.toString();
    }

    public static String readJSON(File file) {
        StringBuilder builder = new StringBuilder();

        BufferedReader in = null;
        try {
            // 创建一个使用默认大小输入缓冲区的缓冲字符输入流
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line = null;
            while (null != (line = in.readLine())) {
                builder.append(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return builder.toString();
    }


    public static String charsetDetector(String subtitlePath) {
        String charset = null;

        try (InputStream input = new FileInputStream(subtitlePath)) {
            BufferedInputStream bis = new BufferedInputStream(input);
            CharsetDetector cd = new CharsetDetector();
            cd.setText(bis);
            CharsetMatch cm = cd.detect();
            if (cm != null) {
                charset = cm.getName();
            } else {
                throw new UnsupportedCharsetException("无法识别文件编码");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return charset;
    }
    /**
     * 读取文件然后分词，返回 HashSet<String>
     **/
    public static TreeSet<String> participle(String filePathString){
            //分词 正则表达式
            final String regex = "[a-zA-Z]+-*[a-zA-Z]*-*[a-zA-Z]";
            final Pattern pattern = Pattern.compile(regex);
            Matcher matcher;
            TreeSet<String> set = new TreeSet<>();
        FileReader fr = null;
        try {
            // 在给定从中读取数据的文件名的情况下创建一个新 FileReader
            fr = new FileReader(filePathString);
            // 创建一个使用默认大小输入缓冲区的缓冲字符输入流
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null) {
                matcher = pattern.matcher(line);
                while (matcher.find()) {
                    set.add(matcher.group().toLowerCase());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return set;
    }


    /**
     * @param outFilePath
     * @param hashSet
     */
    public static void write(String outFilePath ,TreeSet<String> hashSet){
        PrintWriter out = null;
        try {
            out = new PrintWriter(outFilePath, "UTF-8");
            for (String str : hashSet) {
                out.println(str);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }


    /**
     * @param outFilePath
     * @param words
     */
    public static void write(String outFilePath, HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>> words) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(outFilePath, "UTF-8");
            Gson gson = new Gson();
            String json = gson.toJson(words);
            out.print(json);
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }


    /**
     * @param outFilePath 文件地址,输出 JSON 格式
     * @param words       ArrayList
     */
    public static void write(String outFilePath, List words) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(outFilePath, "UTF-8");
            Gson gson = new Gson();
            String json = gson.toJson(words);
            out.print(json);
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }

    }

    public static void write(String outFilePath, Map map) {
        try (PrintWriter out = new PrintWriter(outFilePath, "UTF-8")) {
            Gson gson = new Gson();
            String json = gson.toJson(map);
            out.print(json);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
