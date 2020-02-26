package com.gw.util;

import com.mks.api.*;
import com.mks.api.response.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MKSCommand {

	public MKSCommand() {

	}

//	private static final Logger logger = Logger.getLogger(MKSCommand.class.getName());
	private static final Log logger = LogFactory.getLog(MKSCommand.class);
	private Session mksSession = null;
	private IntegrationPointFactory mksIpf = null;
	private IntegrationPoint mksIp = null;
	private static CmdRunner mksCmdRunner = null;
	private Command mksCommand = null;
	private Response mksResponse = null;
	private boolean success = false;
	private String currentCommand;
	private String hostname = null;
	private int port = 7001;
	private String user;
	private String password;
	private int APIMajor = 4;
	private int APIMinor = 16;
	private static String errorLog;
	private static final String FIELDS = "fields";
	private static final String CONTAINS = "Contains";
	private static final String PARENT_FIELD = "Contained By";

	public static final Map<String, String> ENVIRONMENTVAR = System.getenv();
	public static MKSCommand cmd;
	public static List<String> tsIds = new ArrayList<String>();
	private static String DOCUMENT_TYPE ;
	private static String documentName ;
	private static List<String> typeList = null;
	private static JComboBox comboBox;

	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public MKSCommand(String _hostname, int _port, String _user, String _password, int _apimajor, int _apiminor) {
		hostname = _hostname;
		port = _port;
		user = _user;
		password = _password;
//		createSession();
		getSession();
	}

	public MKSCommand(String args[]) {
		hostname = args[0];
		port = Integer.parseInt(args[1]);
		user = args[2];
		password = args[3];
		APIMajor = Integer.parseInt(args[4]);
		APIMinor = Integer.parseInt(args[5]);
		createSession();
	}


	public void setCmd(String _type, String _cmd, ArrayList<Option> _ops, String _sel) {
		mksCommand = new Command(_type, _cmd);
		String cmdStrg = (new StringBuilder(String.valueOf(_type))).append(" ").append(_cmd).append(" ").toString();
		if (_ops != null && _ops.size() > 0) {
			for (int i = 0; i < _ops.size(); i++) {
				cmdStrg = (new StringBuilder(String.valueOf(cmdStrg))).append(_ops.get(i).toString()).append(" ")
						.toString();
				// Option o = new Option(_ops.get(i).toString());
				mksCommand.addOption(_ops.get(i));
			}

		}
		if (_sel != null && _sel != "") {
			cmdStrg = (new StringBuilder(String.valueOf(cmdStrg))).append(_sel).toString();
			mksCommand.addSelection(_sel);
		}
		currentCommand = cmdStrg;
		// logger.info((new StringBuilder("Command:
		// ")).append(cmdStrg).toString());
	}

	public String getCommandAsString() {
		return currentCommand;
	}

	public boolean getResultStatus() {
		return success;
	}

	public String getConnectionString() {
		String c = (new StringBuilder(String.valueOf(hostname))).append(" ").append(port).append(" ").append(user)
				.append(" ").append(password).toString();
		return c;
	}

	public void exec() {
		success = false;
		try {
			mksResponse = mksCmdRunner.execute(mksCommand);
			// logger.info((new StringBuilder("Exit Code:
			// ")).append(mksResponse.getExitCode()).toString());
			success = true;
		} catch (APIException ae) {
			logger.error(ae.getMessage());
			success = false;
			errorLog = ae.getMessage();
		} catch (NullPointerException npe) {
			success = false;
			logger.error(npe.getMessage());
			errorLog = npe.getMessage();
		}
	}

	public void release() throws IOException {
		try {
			if (mksSession != null) {
				mksCmdRunner.release();
				mksSession.release();
				mksIp.release();
				mksIpf.removeIntegrationPoint(mksIp);
			}
			success = false;
			currentCommand = "";
		} catch (APIException ae) {
			logger.error(ae.getMessage() );
		}
	}

	public void getSession() {
		try {
			mksIpf = IntegrationPointFactory.getInstance();
			mksIp = mksIpf.createLocalIntegrationPoint(APIMajor, APIMinor);
			mksIp.setAutoStartIntegrityClient(true);
			mksSession = mksIp.getCommonSession();
			mksCmdRunner = mksSession.createCmdRunner();
			mksCmdRunner.setDefaultUsername(user);
			mksCmdRunner.setDefaultPassword(password);
			mksCmdRunner.setDefaultHostname(hostname);
			mksCmdRunner.setDefaultPort(port);
		} catch (APIException ae) {
			logger.error("链接失败！！！！！！！！！！！！！！！！！！！！！！！");
			logger.error(ae.toString());
		}
	}

	@SuppressWarnings("deprecation")
	public void createSession() {
		try {
			mksIpf = IntegrationPointFactory.getInstance();
			mksIp = mksIpf.createIntegrationPoint(hostname, port, APIMajor, APIMinor);
			mksSession = mksIp.createSession(user, password);
			mksCmdRunner = mksSession.createCmdRunner();
			mksCmdRunner.setDefaultHostname(hostname);
			mksCmdRunner.setDefaultPort(port);
			mksCmdRunner.setDefaultUsername(user);
			mksCmdRunner.setDefaultPassword(password);
		} catch (APIException ae) {
			logger.error(ae.getMessage() );
		}
	}

	public String[] getResult() {
		String result[] = null;
		int counter = 0;
		try {
			WorkItemIterator mksWii = mksResponse.getWorkItems();
			result = new String[mksResponse.getWorkItemListSize()];
			while (mksWii.hasNext()) {
				WorkItem mksWi = mksWii.next();
				Field mksField;
				for (Iterator<?> mksFields = mksWi.getFields(); mksFields.hasNext();) {
					mksField = (Field) mksFields.next();
					result[counter] = mksField.getValueAsString();
				}

				counter++;
			}
		} catch (APIException ae) {
			logger.error(ae.toString(),ae);
			JOptionPane.showMessageDialog(null, ae.toString(), "ERROR", 0);
		} catch (NullPointerException npe) {
			logger.error(npe.toString());
			JOptionPane.showMessageDialog(null, npe.toString(), "ERROR", 0);
		}
		return result;
	}

	/**
	 * 根据Ids查询字段的值
	 * 
	 * @param ids
	 * @param fields
	 * @return
	 * @throws APIException
	 */
	public List<Map<String, String>> getItemByIds(List<String> ids, List<String> fields) throws APIException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);

		SelectionList sl = new SelectionList();
		for (String id : ids) { 
			String splitID =null;
			if(id.startsWith("[")&&id.endsWith("]")){
				splitID = id.substring(id.indexOf("[")+1,id.indexOf("]"));
				sl.add(splitID.trim());
			}else if(id.startsWith("[")){
				splitID = id.substring(id.indexOf("[")+1,id.length());
				sl.add(splitID.trim());
			}else if(id.endsWith("]")){
				splitID = id.substring(0,id.indexOf("]"));
				sl.add(splitID.trim());
			}else if(id.startsWith(" ")){
				splitID =id.substring(1,id.length());
				sl.add(splitID.trim());
			}else{
				sl.add(id.trim());
			}
		}
		cmd.setSelectionList(sl);

		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				Map<String, String> map = new HashMap<String, String>();
				for (String field : fields) {
					if (field.contains("::")) {
						field = field.split("::")[0];
					}
					String value = wi.getField(field).getValueAsString(); 
					map.put(field, value);
				}
				list.add(map);
			}
		} catch (APIException e) {
			// success = false;
			logger.error(e.getMessage());
			throw e;
		}
		return list;
	}

	//查询caseid和name
	public Map<String, String> getCaseInfoById(String id, List<String> fields) throws APIException {
		Map<String, String> list = new HashMap<>();
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);

		cmd.addSelection(id);

		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				Map<String, String> map = new HashMap<String, String>();
				for (String field : fields) {
					if (field.contains("::")) {
						field = field.split("::")[0];
					}
					String value = wi.getField(field).getValueAsString();
					list.put(field, value);
				}
			}
		} catch (APIException e) {
			// success = false;
			logger.error(e.getMessage());
			throw e;
		}
		return list;
	}

	//根据id查询单个结果
	public String getTypeById(String id, String field) throws APIException {
		String str = "";
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		mv.add(field);
		Option op = new Option("fields", mv);
		cmd.addOption(op);
		cmd.addSelection(id);

		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				if(wi.getField(field) != null ){
					str = wi.getField(field).getValueAsString();
				}
			}
		} catch (APIException e) {
			// success = false;
			logger.error(e.getMessage());
			throw e;
		}
		return str;
	}

	/**
	 *
	 * @param id
	 * @return
	 * @throws APIException
	 */
	public List<String> getSuiteById(String id, List<String> fields) throws APIException {
		List<String> list = new ArrayList<String>();
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);

		cmd.addSelection(id);

		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			String value = "";
			while (it.hasNext()) {
				WorkItem wi = it.next();
				Map<String, String> map = new HashMap<String, String>();
				for (String field : fields) {
					if (field.contains("::")) {
						field = field.split("::")[0];
					}
					 value = wi.getField(field).getValueAsString();

				}
			}
			String[] ids = {};
			if(!value.equals("") && value != null){
				ids = value.split(",");
				for(int i=0;i<ids.length;i++){
					list.add(shujz(ids[i]));
				}
			}

		} catch (APIException e) {
			// success = false;
			throw e;
		}
		return list;
	}
   //过滤id中的英文 lxg
	public String shujz(String  a){
		String regEx="[^0-9]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(a);
		return  m.replaceAll("").trim();
	}

	/**
	 * 模拟数据 lxg
	 * @return
	 * @throws APIException
	 */
	public List<Map<String, String>> mlsj() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		for(int i=0;i<10;i++){
			Map<String,String> m = new HashMap<String,String>();
			m.put("CaseID","1111");
			m.put("TestType","2222");
			m.put("description","3333");
			m.put("Environment","4444");
			m.put("Precondition","5555");
			m.put("TestSteps","hahahah");
			m.put("ExpectedResults","6666");
			m.put("F/P","f");
			m.put("selecttestcase","7777");
			m.put("ObservedResult","8888");
			m.put("Annotation","9999");
			m.put("ResultServerity","aaa");
			m.put("Reproducilibuty","sss");
			m.put("SWVersion","ddd");
			m.put("HWVersion","ffff");
			m.put("Tester","ggg");
			m.put("TestDate","hhhh");
			list.add(m);
		}
		return list;
	}


	public boolean getResultState() {
		return success;
	}

	public String getErrorLog() {
		return errorLog;
	}

	
	@Deprecated
	public List<Map<String, String>>  getAllChild(List<String> ids, List<String> childs) throws APIException {
		List<Map<String, String>> itemByIds = getItemByIds(ids, Arrays.asList("ID", "Contains"));//查询文档id包含字段heading
		for(Map<String,String> map : itemByIds) { //
			String contains = map.get("Contains");
			String id = map.get("ID");
			map.put("ID", id);
			if(contains!=null && contains.length()>0) {
//				List<String> childIds = Arrays.asList(contains.replaceAll("ay", "").split(","));
				getAllChild(Arrays.asList(id), Arrays.asList(contains));
			}
		}
		return itemByIds;
		
	}
	
	public SelectionList contains(SelectionList documents) throws APIException {
		return relationshipValues(CONTAINS, documents);
	}

	public SelectionList relationshipValues(String fieldName, SelectionList ids) throws APIException {
		if (fieldName == null) {
			throw new APIException("invoke fieldValues() ----- fieldName is null.");
		}
		if (ids == null || ids.size() < 1) {
			throw new APIException("invoke fieldValues() ----- ids is null or empty.");
		}
		Command command = new Command(Command.IM, Constants.ISSUES);
		command.addOption(new Option(Constants.FIELDS, fieldName));
		command.setSelectionList(ids);
		Response res = mksCmdRunner.execute(command);
		WorkItemIterator it = res.getWorkItems();
		SelectionList contents = new SelectionList();
		while (it.hasNext()) {
			WorkItem wi = it.next();
			ItemList il = (ItemList) wi.getField(fieldName).getList();
			if(il != null) {
				for (int i = 0; i < il.size(); i++) {
					Item item = (Item) il.get(i);
					String id = item.getId();
					contents.add(id);
				}
			}
		}
		return contents;
	}


	/**
	 *
	 * @param documentID
	 * @return
	 * @throws APIException
	 */
	public List<String> allContainID(String documentID) throws APIException{
		List<String> allContainID = new ArrayList<>();
		Command command = new Command("im","issues");
		command.addOption(new Option(FIELDS, CONTAINS));
		command.addSelection(documentID);
		Response res = mksCmdRunner.execute(command);
		WorkItemIterator it = res.getWorkItems();
		SelectionList sl = new SelectionList();
		List<String> fields = new ArrayList<>();
		fields.add("ID");
		while (it.hasNext()) {
			WorkItem wi = it.next();
			ItemList il = (ItemList) wi.getField(CONTAINS).getList();
			for (int i = 0; i < il.size(); i++) {
				Item item = (Item) il.get(i);
				String id = item.getId();
				sl.add(id);
			}
		}
		SelectionList contents = null;
		if(sl != null&& sl.size()>=1)
			contents = contains(sl);

		if (contents.size() > 0) {
			SelectionList contains = new SelectionList();
			contains.add(contents);
			while (true) {
				SelectionList conteins = contains(contains);
				if (conteins.size() < 1) {
					break;
				}
				contents.add(conteins);
				contains = new SelectionList();
				contains.add(conteins);
			}
		}
		contents.add(sl);
		for(int i=0; i<contents.size(); i++ ){
			allContainID.add(contents.getSelection(i));
		}
		return allContainID;
	}
	
	public List<Map<String, String>> allContents(String document, List<String> fieldList) throws APIException,Exception {
		List<Map<String, String>> returnResult = new ArrayList<Map<String,String>>();
		Command command = new Command("im","issues");
		command.addOption(new Option(FIELDS, CONTAINS));
		command.addSelection(document);
		Response res = mksCmdRunner.execute(command);
		WorkItemIterator it = res.getWorkItems();
		SelectionList sl = new SelectionList();
		List<String> fields = new ArrayList<String>();
		fields.add("ID");
		if(!fieldList.contains(PARENT_FIELD)){//排序使用
			fieldList.add(PARENT_FIELD);
		}
		if(fieldList != null) {
			fields.addAll(fieldList);
		}
		while (it.hasNext()) {
				WorkItem wi = it.next();
				ItemList il = (ItemList) wi.getField(CONTAINS).getList();
				for (int i = 0; i < il.size(); i++) {
					Item item = (Item) il.get(i);
					String id = item.getId();
					sl.add(id);
				}
		}
		SelectionList contents = null;
		if(sl != null&& sl.size()>=1){
		 contents = contains(sl);
		
		if (contents.size() > 0) {
			SelectionList contains = new SelectionList();
			contains.add(contents);
			while (true) {
				SelectionList conteins = contains(contains);
				if (conteins.size() < 1) {
					break;
				}
				contents.add(conteins);
				contains = new SelectionList();
				contains.add(conteins);
			}
		}
		contents.add(sl);
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		if (contents.size() > 500) {
			List<SelectionList> parallel = new ArrayList<SelectionList>();
			SelectionList ids = new SelectionList();
			for (int i = 0;; i++) {
				if (i % 500 == 0 && ids.size() > 0) {
					parallel.add(ids);
					ids = new SelectionList();
				}
				ids.add(contents.getSelection(i));
				if (i + 1 == contents.size()) {
					parallel.add(ids);
					break;
				}
			}
			for (SelectionList selectionList : parallel) {
				list.addAll(queryIssues(selectionList, fields));
			}
		} else {
			list.addAll(queryIssues(contents, fields));
		}
		String beforeParentId = document;
		Integer startIndex = -1;
		List<String> idRecord = new ArrayList<String> ();
		for(int i=0; i<list.size(); i++) {
			Map<String,String> node = list.get(i);
			String parentId = node.get(PARENT_FIELD);
			if(parentId == null || "".equals(parentId) || parentId.equals(document)) {
				node.put(PARENT_FIELD, document);
				returnResult.add(node);
				idRecord.add(node.get("ID"));
			}
		}
		for(int i=0; i<list.size(); i++) {
			Map<String,String> node = list.get(i);
			String parentId = node.get(PARENT_FIELD);
			if(parentId != null && !"".equals(parentId) && !parentId.equals(document)) {
				if(!beforeParentId.equals(parentId)){
					beforeParentId = parentId;
					startIndex = 1;
				}
				Integer parentIndex = idRecord.indexOf(parentId);
				returnResult.add(parentIndex + startIndex, node);
				idRecord.add(parentIndex + startIndex,node.get("ID"));
				startIndex ++;
			}
		}
		}
		return returnResult;
	}
	//未知错误 String dept = "";String trueType   = " "; lxg
	public List<Map<String, String>> queryIssues(SelectionList selectionList, List<String> fields) throws APIException, Exception {
		List<Map<String, String>> returnResult = new ArrayList<Map<String,String>>();
		String dept = "";
		String trueType   = " ";
		boolean needFilter = false;
		String category = "";
		if(!"Transmission".equals(dept) && trueType.contains("Test Specification")){
			needFilter = true;
			category = trueType.substring(0, trueType.indexOf("Speci")-1);
			fields.add("Test Steps");//测试数据才需要额外查询Test Step数据
			fields.add("Category");
		}
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);
		cmd.setSelectionList(selectionList);
		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				Map<String, String> map = new HashMap<String, String>();
				for (String field : fields) {
					if (field.contains("::")) {
						field = field.split("::")[0];
					}
					Field fieldObj = wi.getField(field);
					String fieldType = fieldObj.getDataType();
					String value = fieldObj.getValueAsString()!=null?fieldObj.getValueAsString().toString():null;
					value = parseDateVal(value, fieldType);
					if(PARENT_FIELD.equals(field) && value!=null
							&& value.contains("[") && value.contains("]")){
						value = value.substring(value.indexOf("[")+1, value.indexOf("]"));
					}
					if("[]".equals(value)){
						value = null;
					}
					map.put(field, value);
				}
				boolean canAdd = true;
				if(needFilter){
					String currentCategory = map.get("Category");
					if(!currentCategory.equals(category))
						canAdd = false;
				}
				if(canAdd)
					returnResult.add(map);
			}
		} catch (APIException e) {
			logger.error(e.getMessage());
			throw e;
		}
		return returnResult;
	}
	
	public String getUserNames(String userId) throws APIException {
		if(userId!=null && !"".equals(userId)){
			List<String> listUser = new ArrayList<String>();
			listUser.add(userId);
			return getUserNames(listUser);
		}else{
			return "";
		}
	}
	
	public static String parseDateVal(String value, String fieldType){
		if("java.util.Date".equals(fieldType)){
			value = FORMAT.format(new Date(value));
		}
		return value;
	}
	
	public String getUserNames(List<String> userIds) throws APIException {
		String user = "";
		if(userIds!=null && userIds.size()>0){
			Command cmd = new Command(Command.IM, "users");
			cmd.addOption(new Option("fields","name,fullname,email,isActive"));
			for(String userId : userIds){
				cmd.addSelection(userId);
			}
			Response res = mksCmdRunner.execute(cmd);
			if (res != null) {
				WorkItemIterator iterator = res.getWorkItems();
				while(iterator.hasNext()) {
					WorkItem item = iterator.next();
					if(item.getField("isActive").getValueAsString().equalsIgnoreCase("true")) {
						user = user + item.getField("fullname").getValueAsString() + ",";
					}
				}
			}
			if(user.length()>0){
				user = user.substring(0, user.length()-1);
			}
		}
		return user;
	}
	
	public List<String> getTestSteps(List<String> realStepFields) throws APIException {
		List<String> fieldList = new ArrayList<String>();
		if (fieldList.isEmpty()) {
			fieldList.add("ID");
			fieldList.add("Test Input");
			fieldList.add("Test Output");
			fieldList.add("Call Depth");
			fieldList.add("Test Procedure");
		}
		return fieldList;
	}
	
	public static List<Map<String, String>> findIssuesByQueryDef(List<String> fields, String query) throws APIException {
		if (query == null || query.isEmpty()) {
			throw new APIException("invoke findIssuesByQueryDef() ----- query is null or empty.");
		}
		if (fields == null) {
			fields = new ArrayList<String>();
		}
		if (fields.size() < 1) {
			fields.add("ID");
			fields.add("Project");
			fields.add("Type");
			fields.add("State");
		}
		MultiValue mv = new MultiValue(",");
		for (String field : fields) {
			mv.add(field);
		}
		Command command = new Command(Command.IM, Constants.ISSUES);
		command.addOption(new Option(Constants.FIELDS, mv));
		command.addOption(new Option(Constants.QUERY_DEFINITION, query));
//		command.addOption(new Option("showTestResults"));
		Response res = mksCmdRunner.execute(command);
		WorkItemIterator it = res.getWorkItems();
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		while (it.hasNext()) {
				WorkItem wi = it.next();
				Iterator<?> iterator = wi.getFields();
				Map<String, String> map = new HashMap<String, String>();
				while (iterator.hasNext()) {
					Field field = (Field) iterator.next();
					String fieldName = field.getName();
					if (Constants.ITEMLIST.equals(field.getDataType())) {
						StringBuilder sb = new StringBuilder();
						ItemList il = (ItemList) field.getList();
						for (int i = 0; i < il.size(); i++) {
							Item item = (Item) il.get(i);
							if (i > 0) {
								sb.append(",");
							}
							sb.append(item.getId());
						}
						map.put(fieldName, sb.toString());
					} else {
						map.put(fieldName, field.getValueAsString());
					}
				}
				list.add(map);
				
			}
		return list;
	}
				
	public void editIssue(String id, Map<String, String> fieldValue, Map<String, String> richFieldValue)
			throws APIException {
		Command cmd = new Command(Command.IM, "editissue");
		if (fieldValue != null) {
			for (Map.Entry<String, String> entrty : fieldValue.entrySet()) {
				cmd.addOption(new Option("field", entrty.getKey() + "=" + entrty.getValue()));
			}
		}
		if (richFieldValue != null) {
			for (Map.Entry<String, String> entrty : richFieldValue.entrySet()) {
				cmd.addOption(new Option("richContentField", entrty.getKey() + "=" + entrty.getValue()));
			}
		}
		cmd.addSelection(id);
		mksCmdRunner.execute(cmd);
	}

	public List<String> viewIssue(String id, boolean showRelationship)
			throws APIException {
		Command cmd = new Command(Command.IM, "viewissue");
		MultiValue mv = new MultiValue(",");
		cmd.addOption(new Option("showTestResults"));
		if(showRelationship){
			cmd.addOption(new Option("showRelationships"));
		}
		cmd.addSelection(id);
		Response res = mksCmdRunner.execute(cmd);
		WorkItemIterator it = res.getWorkItems();
		List<String> relations = new ArrayList<String>();
		while (it.hasNext()) {
			WorkItem wi = it.next();
			Iterator<?> iterator = wi.getFields();
			Map<String, String> map = new HashMap<String, String>();
			while (iterator.hasNext()) {
				Field field = (Field) iterator.next();
				String fieldName = field.getName();
//				if("MKSIssueTestResults".equals(fieldName)){
//					field.getList();
//				}
				if("Test Steps".equals(fieldName)){
					System.out.println("123");
					StringBuilder sb = new StringBuilder();
					ItemList il = (ItemList) field.getList();
					for (int i = 0; i < il.size(); i++) {
						Item item = (Item) il.get(i);
						if (i > 0) {
							sb.append(",");
						}
						sb.append(item.getId());
					}
					map.put(fieldName, sb.toString());
				}
				if("Test Result".equals(fieldName) || "Test Results".equals(fieldName)){
					System.out.println("123");
				}
			}
		}
		return relations;
	}

	/**
	 * 根据sessionid查看测试实例id与测试结果信息 lxg
	 * @param id
	 * @param idType
	 * @return
	 * @throws APIException
	 */
//	public Map<String, String> viewIssueBySessionId(String id, String idType)
//			throws APIException {
//		Command cmd = new Command(Command.IM, "viewissue");
//		MultiValue mv = new MultiValue(",");
//		cmd.addSelection(id);
//		Response res = mksCmdRunner.execute(cmd);
//		WorkItemIterator it = res.getWorkItems();
//		List<Map<String, String>> relations = new ArrayList<Map<String, String>>();
//		Map<String, String> map = new HashMap<String, String>();
//		while (it.hasNext()) {
//			WorkItem wi = it.next();
//			Iterator<?> iterator = wi.getFields();
////			Map<String, String> map = new HashMap<>();
//			while (iterator.hasNext()) {
//				Field field = (Field) iterator.next();
//				String fieldName = field.getName();
//				System.out.println(fieldName);
////				if("MKSIssueTestResults".equals(fieldName)){
////					field.getList();
////				}
//				//如果是sessionid 则查询test session相关信息 lxg
//				if("sessionId".equals(idType)){
//					if("Tests".equals(fieldName)){
//						StringBuilder sb = new StringBuilder();
//						ItemList il = (ItemList) field.getList();
//						for (int i = 0; i < il.size(); i++) {
//							Item item = (Item) il.get(i);
//							if (i > 0) {
//								sb.append(",");
//							}
//							sb.append(item.getId());
//						}
//						map.put(fieldName, sb.toString());
//					}
//					if("Software version".equals(fieldName) || "Hardware version".equals(fieldName)){
//						System.out.println("123");
//					}
//				}
//				//如果是caseId 则查询caseId相关信息 lxg
//				if("caseId".equals(idType)){
//					Result r  =  new AnalysisXML().resultXml();
//					List<Map<String,String>> m  = (List<Map<String,String>>)r.getData();
//					for(Map<String,String> mi: m){
//						for(String key : mi.keySet()){
//							String velue = mi.get(key);
//							logger.info(fieldName +"++++++++"+velue+"=========="+fieldName .equals(velue));
//							if(velue.equals(fieldName)){
//								String il =  field.getValue()==null?"":field.getValue().toString();
//								map.put(key, il);
//							}
//						}
//					}
//				}
//
//			}
////			relations.add(map);
//		}
//		return map;
//	}

	/**
	 * 查询测试结果 lxg
	 * @param SesssionId
	 * @param CaseID
	 * @return
	 * @throws APIException
	 */
	public Result viewresultByCaseID(String SesssionId, String CaseID) throws APIException {
		Result result = new Result(); //最终返回数据
		List<Map<String,String>> map = new ArrayList<Map<String,String>>();
		Map<String,String> resultMap = new HashMap<String,String>();//查询返回数据
		List<String> fields = new ArrayList<String>();      //查询条件list
//		Map<String,String> cstj = new HashMap<>();      //查询条件list
		Result r = new Result();

		Command cmd = new Command("tm", "results");
		cmd.addOption(new Option("caseID", CaseID));

        if(getTypeById(CaseID,"Category").equals("System Qualification Test")){
			r  =  new AnalysisXML().resultXml("System Qualification Test");
		} else if(getTypeById(CaseID,"Category").equals("System Integration Test")){
			r  =  new AnalysisXML().resultXml("System Integration Test");
		} else if(getTypeById(CaseID,"Category").equals("Software Qualification Test")){
			r  =  new AnalysisXML().resultXml("Software Qualification Test");
		} else if(getTypeById(CaseID,"Category").equals("Software Integration Test")){
			r  =  new AnalysisXML().resultXml("Software Integration Test");
		} else {
			JOptionPane.showMessageDialog(null, "当前Test Case无此类型模板！","错误",0);
		}

		List<Map<String,String>> m  = (List<Map<String,String>>)r.getData();
		Map<String,String> sjtype  = (Map<String,String>)r.getMap();
		for(Map<String,String> mi: m){
			for(String key : mi.keySet()){
				String velue = mi.get(key);
				String type = sjtype.get(key);
				if(type.equals("Test Results")){
					fields.add(velue);
				}
//				else {
//					cstj.put(key,getTypeById(CaseID,type));
//				}
			}
		}
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		cmd.addOption(new Option("fields", mv));
		Response res = mksCmdRunner.execute(cmd);
		WorkItemIterator it = res.getWorkItems();

		while (it.hasNext()) {
			WorkItem wi = it.next();
			Iterator<?> iterator = wi.getFields();
			while (iterator.hasNext()) {
				Field field = (Field) iterator.next();
				resultMap.put(field.getName(),field.getValue()==null?"":field.getValue().toString());
			}
		}
		//根据xml内容遍历 （主要是排序）
		for(Map<String,String> mi: m){
			for(String key : mi.keySet()){
				String type = sjtype.get(key);
				if(resultMap.size()>0){
					if(type.equals("Test Results")){
						for(String s : resultMap.keySet()){
							if(mi.get(key).equals(s)){
								Map<String,String> lsm = new HashMap<String,String>();
								lsm.put(key, resultMap.get(s));
								map.add(lsm);
							}
						}
					}else {
						Map<String,String> lsm = new HashMap<String,String>();
						lsm.put(key, getTypeById(CaseID,mi.get(key)));
						map.add(lsm);
					}
				}
			}
		}
		result.setData(map);
		result.setMap1(r.getMap1());
		return result;
	}

	public List<Map<String, Object>> getResult(String sessionID, String suiteID, String type) throws APIException {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		SelectionList list = new SelectionList();
		Command cmd = new Command("tm", "results");
		
		//cmd.addOption(new Option("sessionID", sessionID));
//		if (type.equals("Test Suite")) {
		cmd.addOption(new Option("caseID", suiteID));
//		} else if (type.equals("Test Case")) {
//			cmd.addSelection(sessionID);
//		}
		List<String> fields = new ArrayList<String>();
		fields.add("caseID");
		fields.add("sessionID");
		fields.add("verdict");
		fields.add("Observed Result");
		fields.add("Annotation");
		fields.add("Result Serverity");
		fields.add("Reproducibility");
		
		fields.add("SW Version");
		fields.add("HW Result Version");

		
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);
		Response res = null;
		if (type.equals("Test Suite")) {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator wk = res.getWorkItems();
			while (wk.hasNext()) {
				Map<String, Object> map = new HashMap<String, Object>();
				WorkItem wi = wk.next();
				for (String field : fields) {
					Object value = wi.getField(field).getValue();
					map.put(field, value);
				}
				result.add(map);
			}
		} else if (type.equals("Test Case")) {
			try {
				res = mksCmdRunner.execute(cmd);
				WorkItemIterator wk = res.getWorkItems();
				while (wk.hasNext()) {
					Map<String, Object> map = new HashMap<String, Object>();
					WorkItem wi = wk.next();
					for (String field : fields) {
						Object value = wi.getField(field).getValue();
						if(value instanceof Item){
							Item item = (Item) value;
							value = item.getId();
						}
						if("verdict".equals(field))
							field = "verdictType";
						map.put(field, value);
					}
					result.add(map);
				}
			} catch (Exception e) {
				e.printStackTrace();
				
			}
		}
		return result;
	}
	
	/**
	 * Description 获取所有Field 类型，并把Pick值预先取出
	 * @param fields
	 * @param PICK_FIELD_RECORD
	 * @return
	 * @throws APIException
	 */
	public Map<String,String> getAllFieldType(List<String> fields, Map<String,List<String>> PICK_FIELD_RECORD) throws APIException {
		Map<String,String> fieldTypeMap = new HashMap<String,String>();
		Command cmd = new Command("im", "fields");
		cmd.addOption(new Option("noAsAdmin"));
		cmd.addOption(new Option("fields", "picks,type"));
		for(String field : fields){
			if(field!=null && field.length()>0){
				cmd.addSelection(field);
			}
		}
		Response res=null;
		try {
			res = mksCmdRunner.execute(cmd);
		} catch (APIException e) {
			
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
		if (res != null) {
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				String field = wi.getId();
				String fieldType = wi.getField("Type").getValueAsString();
				if("pick".equals(fieldType) ){
					Field picks = wi.getField("picks");
					ItemList itemList = (ItemList) picks.getList();
					if (itemList != null) {
						List<String> pickVals = new ArrayList<String>();
						for (int i = 0; i < itemList.size(); i++) {
							Item item = (Item) itemList.get(i);
							String visiblePick = item.getId();
							Field attribute = item.getField("active");
							if (attribute != null && attribute.getValueAsString().equalsIgnoreCase("true")
									&& !pickVals.contains(visiblePick)) {
								pickVals.add(visiblePick);
							}
						}
						PICK_FIELD_RECORD.put(field, pickVals);
					}
				}else if("fva".equals(fieldType)){
					
				}
				fieldTypeMap.put(field, fieldType);
			}
		}
		return fieldTypeMap;
	}
	
	/**
	 * Description 查询所有Projects
	 * @return
	 * @throws APIException
	 */
	public List<String> getProjects() throws APIException {
		List<String> projects = new ArrayList<String>();
		Command cmd = new Command("im", "projects");
		
		Response res = mksCmdRunner.execute(cmd);
		if (res != null) {
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				String project = wi.getId();
				projects.add(project);
			}
		}
		return projects;
	}

	/**
	 * 初始化MKSCommand中的参数，并获得连接
	 */
	public static void initMksCommand() {
		try {
			String host = ENVIRONMENTVAR.get(Constants.MKSSI_HOST);
			if(host==null || host.length()==0) {
				host = "192.168.229.133";
			}
			String portStr = ENVIRONMENTVAR.get(Constants.MKSSI_PORT);
			Integer port = portStr!=null && !"".equals(portStr)? Integer.valueOf(portStr) : 7001;
			String defaultUser = ENVIRONMENTVAR.get(Constants.MKSSI_USER);
			String pwd = "";
			if(defaultUser == null || "".equals(defaultUser) ){
				defaultUser = "admin";
				pwd = "admin";
			}
			logger.info("host:" + host+"; defaultUser:"+defaultUser+"; pwd:"+pwd);
			cmd = new MKSCommand(host, 7001, defaultUser, pwd, 4, 16);
//			cmd.getSession();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,"无法连接!","提示",1);
			logger.info("无法连接!");
			System.exit(0);
		}
	}

	/**issues
	 * 获取当前选中id的List集合
	 * @return
	 * @throws Exception
	 */
	public static Map<String,List<String>> getSelectedIdList() throws Exception {
		List<String> caseIds = new ArrayList<String>();
		String issueCount = ENVIRONMENTVAR.get(Constants.MKSSI_NISSUE);
		if (issueCount != null && issueCount.trim().length() > 0) {
			for (int index = 0; index < Integer.parseInt(issueCount); index++) {
				String id =  ENVIRONMENTVAR.get(String.format(Constants.MKSSI_ISSUE_X, index));
				tsIds.add(id);//获取到当前选中的id添加进集合Ids集合
			}
		} else {
			 logger.info("身份验证失败!! :" + issueCount);
		}
//		tsIds.add("21193");
		tsIds.add("11207");
		if (tsIds.size() > 0) {//如果选中的id集合不为空，通过id获取条目简要信息
			List<Map<String, String>> itemByIds = cmd.getItemByIds(tsIds, Arrays.asList("ID", "Type","Summary","Tests"));
			List<String> notTSList = new ArrayList<String>();
			for (Map<String, String> map : itemByIds) {
				DOCUMENT_TYPE = map.get("Type");
				String id = map.get("ID");
				documentName = map.get("Summary");
				if(map.get("Tests")!=null){
					String[] ids = map.get("Tests").toString().split(",");
					for(int i=0;i<ids.length;i++){
						caseIds.add(ids[i]);
					}
				}else{
					JOptionPane.showMessageDialog(null, "当前Test SessionId暂无测试结果！","错误",0);
					System.exit(0);
				}
//				if(typeList.contains(DOCUMENT_TYPE)){
//					comboBox.setSelectedItem(DOCUMENT_TYPE);
//				}

			}
			if (notTSList.size() > 0) {
//				throw new Exception("This item " + notTSList + " is not [ " + documentType + " ]! Please  select the right type!");
			} else {
				 logger.info("get the selection Document : " + tsIds);
			}
		} else {
			throw new Exception("Please select the ID of a Document!");
		}
		Map<String,List<String>> r = new HashMap<String,List<String>>();
		r.put("tsIds",tsIds);
		r.put("caseIds",caseIds);
		return r;
	}
}
