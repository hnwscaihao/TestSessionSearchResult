package com.gw.util;

import com.gw.service.ImportService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.*;

/*
*Dom4j解析xml
 */
public class AnalysisXML {

    private static final Log log = LogFactory.getLog(ImportService.class);

    public Result resultXml(String type){
       Result r  = new Result();
       String jdsx = type;
       //1.创建Reader对象
       SAXReader reader = new SAXReader();
       //2.加载xml
       Document document = null;
       List<Map<String,String>> m = new ArrayList<Map<String,String>>();
       Map<String,String> lsm1 = new HashMap<String,String>();
       Map<String,String> lsm2 = new HashMap<String,String>();
       try {
//           String pathxml = System.getProperty("user.home")+"\\InfoMapping.xml";
//           ClassLoader.getSystemResourceAsStream("InfoMapping.xml");
//           //获取文件输入流
//           ClassPathResource resource = new ClassPathResource("\\InfoMapping.xml");
           InputStream in = AnalysisXML.class.getClassLoader().getSystemResourceAsStream("InfoMapping.xml");
//           writeToLocal(pathxml,in);
           document = reader.read(in);
//           document = reader.read(new File(System.getProperty("user.dir")+"\\src\\main\\resources\\InfoMapping.xml"));
           //3.获取根节点
           Element rootElement = document.getRootElement();
           Iterator iterator = rootElement.elementIterator();
           while (iterator.hasNext()){
               Element stu = (Element) iterator.next();
               List<Attribute> attributes = stu.attributes();
//               System.out.println("======获取属性值======");
               String s = "";
               for (Attribute attribute : attributes) {
                   s = attribute.getValue();
//                   System.out.println(attribute.getValue());
               }
//               System.out.println("======遍历子节点======");
               if(s.equals(jdsx)){
                   Iterator iterator1 = stu.elementIterator();
                   while (iterator1.hasNext()){
                       Element stuChild = (Element) iterator1.next();
                       Map<String,String> lsm = new HashMap<String,String>();
                       //记录名字
                       lsm.put(stuChild.attribute("name").getValue(),stuChild.attribute("reality").getValue());
                       m.add(lsm);
                       //记录类型
                       lsm1.put(stuChild.attribute("name").getValue(),stuChild.attribute("type").getValue());

                       //记录类型
                       lsm2.put(stuChild.attribute("name").getValue(),stuChild.attribute("fieldType").getValue());
//                       System.out.println("节点名："+stuChild.getName()+"---节点值："+stuChild.getStringValue());
                   }
               }
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       r.setData(m);
       r.setMap(lsm1);
       r.setMap1(lsm2);
       return r;
   }

    /**
     * 将InputStream写入本地文件
     * @param destination 写入本地目录
     * @param input 输入流
     * @throws IOException IOException
     */
    public  void writeToLocal(String destination, InputStream input)
            throws IOException {
        File file = new File(destination);
        if (!file.exists()) {
            file.createNewFile();
        }
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream downloadFile = new FileOutputStream(destination);
        while ((index = input.read(bytes)) != -1) {
            downloadFile.write(bytes, 0, index);
            downloadFile.flush();
        }
        input.close();
        downloadFile.close();

    }

   public static void main(String[] args){
       AnalysisXML r = new AnalysisXML();
//       r.resultXml();
   }
}
