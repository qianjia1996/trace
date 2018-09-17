/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trace.subtitle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import trace.utils.FileUtils;

/**
 *
 * @author M_T
 */
public class ParseSTR {
    //语料库 600000 + 拼写检查  = 128047 个单词
     private static final String COCA_STRING = "resource/word/en_US+frequency";
    //词库文件
    private static final String LELTS__STRING = "resource/word/雅思核心.txt";
    private static final String BASIC_STRING = "resource/word/基础2000.txt";
    private static final String TEM4__STRING = "resource/word/大学英语四级.txt";
    private static final String CET6__STRING = "resource/word/大学英语六级.txt";
    private static final String BEC_STRING = "resource/word/考研英语大纲词汇.txt";
    private static final String NMT_STRING = "resource/word/高考乱序.txt";
    
    public static void main(String[] args) throws IOException {

        ArrayList<String> list = new ArrayList<>();//统计总单词数
        HashMap<String,Integer> hashMap = new HashMap<>();//统计词频
        //视图
        
        final String regex = "[a-zA-Z]+-*[a-zA-Z]*-*[a-zA-Z]";
       //Loading the Tokenizer model
       InputStream inputStream = new FileInputStream("D:/OpenNLP_models/en-token.bin");
        final Pattern pattern = Pattern.compile(regex);
              Matcher matcher;
              
        // 读取词库文件，返回一个 List<String>
        List<String> basicList = FileUtils.read(BASIC_STRING);
        
        List<String> LELTSList = FileUtils.read(LELTS__STRING);
        List<String> TEM4List = FileUtils.read(TEM4__STRING);
        List<String> CET6List = FileUtils.read(CET6__STRING);
        List<String> BECList = FileUtils.read(BEC_STRING);
        List<String> NMTList = FileUtils.read(NMT_STRING);
        List<String> COCAList = FileUtils.read(COCA_STRING);
        try {
            FormatSRT fsrt = new FormatSRT();
            File fileDir = new File("C:\\Users\\M_T\\Documents\\NetBeansProjects\\trace\\src\\trace\\subtitles\\");
            String[] files = fileDir.list();
            
            
            for(String file : files){
                if(file.equalsIgnoreCase("utils")) continue;
                InputStream is = new FileInputStream(fileDir+"\\"+file);
                TimedTextObject tto = fsrt.parseFile(file, is);
                Collection<Caption> c = tto.captions.values();
                Iterator<Caption> itr = c.iterator();
                ArrayList<String> list1 = new ArrayList<>();
                HashMap<String, Integer> hashMap1 = new HashMap<>();

                while(itr.hasNext()){
                     Caption current = itr.next();
                     matcher = pattern.matcher(current.content);
                     while(matcher.find()){
                         String string = matcher.group().toLowerCase();
                         if(COCAList.contains(string)){
                           list1.add(string);
                           if(hashMap1.get(string) != null){
                               hashMap1.put(string, hashMap1.get(string)+1);
                           }else{
                              hashMap1.put(string, 1);
                           } 
                         }

                     }
                }
                list.addAll(list1);
                System.out.println(file+"总共 "+list1.size()+" 个单词。有 "+hashMap1.size()+" 个不重复的单词");
            }
            
           System.out.println("第一季总共有 "+list.size()+" 个单词");
           
           for(String str : list){
               
               if(COCAList.contains(str)){
                    if(hashMap.get(str) != null){
                        hashMap.put(str, hashMap.get(str)+1);
                    }else{
                        hashMap.put(str, 1);
                    }
               }

           }
           
           System.out.println("这一季总共有 "+hashMap.size()+" 个不重复的单词");
           System.out.println("显示这一季出现过单词的词频");
                SortMap(hashMap);
        } catch (IOException ex) {
            Logger.getLogger(ParseSTR.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Set set = hashMap.keySet();

    }
    
    
    
    /*
     * 词频统计排序，数量越多的越靠前。
    */
    public static void SortMap(Map<String,Integer> oldmap){
        ArrayList<Map.Entry<String,Integer>> list = new ArrayList<>(oldmap.entrySet());
        
        Collections.sort(list,new Comparator<Map.Entry<String,Integer>>(){    
 
            @Override
            public int compare(Map.Entry<String, Integer> t, Map.Entry<String, Integer> t1) {
               return t1.getValue() - t.getValue();
            }
        });
        
        for(int i = 0; i<list.size(); i++){
            System.out.println(list.get(i).getKey() + ":" + list.get(i).getValue());
        }
    }
    
    /*
     *统计字幕文件有多少词库中的单词 
     */
    public static Collection<String> countLexicon(String[] lexicon, Collection<String> subtitleWords) {
        
        ArrayList<String> list = new ArrayList<>();
        for (String str : lexicon) {
            if (subtitleWords.contains(str)) {
                if(list.contains(str) == false)
                    list.add(str);
            }
        }
        return list;
    }
   
}
