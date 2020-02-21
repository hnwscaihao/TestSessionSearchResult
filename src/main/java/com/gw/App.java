package com.gw;

import com.gw.service.ImportService;
import com.gw.ui.ImportGUI;
import com.gw.util.MKSCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gw.util.MKSCommand.getSelectedIdList;
import static com.gw.util.MKSCommand.initMksCommand;

public class App {

    private static final Log log = LogFactory.getLog(ImportService.class);

    public static void main(String[] args){
        try {
            ImportGUI imp = new ImportGUI();
            imp.ImportGUI();
            imp.glasspane.start();//开始动画加载效果
            MKSCommand m = new MKSCommand();
            initMksCommand();//初始化MKSCommand中的参数，并获得连接
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());//设置与本机适配的swing样式
            Map<String, List<String>> l = getSelectedIdList();//获取到当前选中的id添加进集合Ids集合
            if(l.get("tsIds")!=null){
                log.info("连接成功！");
                if(l.get("tsIds").size()>1){
                    JOptionPane.showMessageDialog(null, "暂不支持多选！","错误",0);
                    System.exit(0);
                }else {
                    List<String> caseIds = l.get("caseIds");
                    Map<String,String> ids = new HashMap<String,String>();
                    List<Map<String, String>> caseName = m.getItemByIds(caseIds, Arrays.asList("Text","Type","id"));//查询文档id包含字段heading
                    if(caseName.size() > 0){
                        for(Map<String, String> s  : caseName){
                            if(s.get("Type").equals("Test Suite")) {
                                List<String> cs = m.getSuiteById(s.get("id"), Arrays.asList("Contains"));
                                for (String s1 : cs) {
                                    Map<String, String> caseinfo = m.getCaseInfoById(s1, Arrays.asList("Text", "id"));
                                    imp.cmb.addItem(imp.textxz(caseinfo.get("Text")));
                                    ids.put(caseinfo.get("id"),caseinfo.get("Text"));
                                }
                            }else if(s.get("Type").equals("Test Case")) {
                                String casename = imp.textxz(s.get("Text"));
                                imp.cmb.addItem(casename);
                                ids.put(s.get("id"),casename);
                            }
                        }
                        imp.sessionid = l.get("tsIds").get(0);
                        imp.caseIds = ids;
                        imp.glasspane.stop();
                    }else {
                        JOptionPane.showMessageDialog(null, "当前Test SessionId无有结果测试项！","错误",0);
                        System.exit(0); //关闭主程序
                    }
                }
            }else {
                log.info("连接失败（根据id查询出错）！");
                JOptionPane.showMessageDialog(null, "连接失败...","错误",0);
                System.exit(0);
            }
        }catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(),"错误",0);
            System.exit(0); //关闭主程序
        }
    }
}
