package com.gw.service;

import com.gw.util.Result;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportService {

    private static final Log log = LogFactory.getLog(ImportService.class);

    public Result ImportData(File excel){
        Result r = new Result();
        try {
            //String encoding = "GBK";
//            File excel = new File(selectedFile.getParent());
            if (excel.isFile() && excel.exists()) {   //判断文件是否存在

                String[] split = excel.getName().split("\\.");  //.是特殊字符，需要转义！！！！！
                Workbook wb;
                //根据文件后缀（xls/xlsx）进行判断
                if ( "xls".equals(split[1])){
                    FileInputStream fis = new FileInputStream(excel);   //文件流对象
                    wb = new HSSFWorkbook(fis);
                }else if ("xlsx".equals(split[1])){
                    wb = new XSSFWorkbook(excel);
                }else {
                    log.info("文件类型错误!");
                    r.setStatus(0);
                    r.setMsg("文件类型错误!");
                    return r;
                }

                //开始解析
                Sheet sheet = wb.getSheetAt(0);     //读取sheet 0

                int firstRowIndex = sheet.getFirstRowNum();   //开始行
                int lastRowIndex = sheet.getLastRowNum();
                log.info("开始行: "+firstRowIndex);
                log.info("结束行: "+lastRowIndex);
                List<String> title = new ArrayList();
                List<List<String>> datalist =  new ArrayList();
                for(int rIndex = firstRowIndex; rIndex <= lastRowIndex; rIndex++) {   //遍历行
                    List data =  new ArrayList();
                    Row row = sheet.getRow(rIndex);
                    if (row != null) {       //行数据不能为空、
                        int firstCellIndex = row.getFirstCellNum();
                        int lastCellIndex = row.getLastCellNum();
                        for (int cIndex = firstCellIndex; cIndex < lastCellIndex; cIndex++) {   //遍历列
                            Cell cell = row.getCell(cIndex);
                            if (cell != null) {
                                if (rIndex < 2) {   //标题
                                    if(!cell.toString().equals("1-Cycle Test") && !cell.toString().trim().equals("")){
                                        title.add(cell.toString());
                                    }
                                } else {
                                    data.add(cell.toString());
                                }
                            }
                        }
                    }
                    datalist.add(data);
                }
                //合并excel数据 lxg
                List<Map<String,String>> listMap = new ArrayList<Map<String,String>>();
                for (List<String> l : datalist) {
                    if(l.size()>0){
                        int i = 0;
                        Map m = new HashMap();
                        for (String str : l) {
                            m.put(title.get(i),str);
                            i++;
                        }
                        listMap.add(m);
                    }
                }
                r.setStatus(200);
                r.setMsg("导入成功！");
                r.setData(listMap);
            } else {
                r.setStatus(404);
                r.setMsg("找不到指定的文件！");
                log.info("找不到指定的文件");
            }
        } catch (Exception e) {
            r.setStatus(404);
            r.setMsg(e.getMessage());
            e.printStackTrace();
        }
        return r;
    }
}
