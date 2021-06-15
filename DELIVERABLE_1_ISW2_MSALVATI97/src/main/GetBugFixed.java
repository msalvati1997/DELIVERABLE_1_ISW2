package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GetBugFixed  {
	private static final String COMMITS_LOG = "CommitsLog";

	private static final String PROJNAME="S2GRAPH";

	private static final Logger LOGGER = Logger.getLogger( GetBugFixed.class.getName() );
	
	public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
		      InputStream is = new URL(url).openStream();
		         try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					String jsonText = readAll(rd);
					 return new JSONArray(jsonText);
				}
		   }
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	      InputStream is = new URL(url).openStream();
	         try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String jsonText = readAll(rd);
				 return new JSONObject(jsonText);
			}
	   }
	   
	private static String readAll(Reader rd) throws IOException {
		      StringBuilder sb = new StringBuilder();
		      int cp;
		      while ((cp = rd.read()) != -1) {
		         sb.append((char) cp);
		      }
		      return sb.toString();
		   }
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
	        String content = new String(Files.readAllBytes(Paths.get(filename)));
	        return new JSONObject(content);
	    }
	
	   
	  public static List<Object> checkLinkage(String projname) throws JSONException, IOException {
		  String filename = "CommitLog"+projname+".json";
		  float nolinked=0;
		  float linked=0;
		  ArrayList <String> tickets = new ArrayList<>();
	      JSONObject jsonObject = parseJSONFile(filename);
	      JSONArray  arr = jsonObject.getJSONArray(COMMITS_LOG);
	      for (int i=0;i<arr.length();i++) {
	    	    JSONObject commit = arr.getJSONObject(i);
	    	    if (commit.get("Linked").toString().equals("No")) {
	    	    	nolinked=nolinked+1;
	    	    }
	    	    if (commit.get("Linked").toString().equals("Yes")) {
	    	    	linked=linked+1;
	    	    	tickets.add(commit.get("Ticket").toString());
	    	    }
	    	}
	     int tot= (int) (nolinked+linked);
	     float link;
	     float linkage;
	     if (tot!=0) {
	       linkage = (linked / (tot))*100;
	       link= Math.round(linkage); // per eccesso
	     } 
	     else {
	          link= 0; 
	        } 
	    
	     ArrayList<Object> result = new ArrayList<>();
	     result.add(link);
	     result.add(tickets);
	     result.add(nolinked);
	     result.add(linked);
	     result.add(tot);
	     return result;
	  }
	    
  public static List<String> validDate() throws JSONException, IOException {
	  String filename = "CommitLogS2GRAPH.json";
      JSONObject jsonObject = parseJSONFile(filename);
      ArrayList<String> dateslist = new ArrayList<>();
      JSONArray ja = jsonObject.getJSONArray(COMMITS_LOG);
	  for (int i=0;i<ja.length();i++) {
  	    JSONObject commit = ja.getJSONObject(i);
  	    String dates =  commit.getString("CommitTime");
  	    String yearsmonth = dates.substring(0,7);
  	    dateslist.add(yearsmonth);
	  }
	  return dateslist;
  }
  public static Map<String, Integer> countFrequencies(List<String> list)
  {
      // hashmap to store the frequency of element
      Map<String, Integer> hm = new HashMap<>();
      for (String i : list) {
          Integer j = hm.get(i);
          hm.put(i, (j == null) ? 1 : j + 1);
      }
      return  hm;
  }
  public static List<String> getJiraBugFixed(String projName) throws IOException, JSONException {
       ArrayList<String> tickets = new ArrayList<>();
	   Integer j = 0;
	   Integer i = 0;
	   Integer total = 1;
   //Get JSON API for closed bugs w/ AV in the project
   do {
      //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
      j = i + 1000;
      String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
              + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22done%22OR"
              + "%22status%22=%22resolved%22)&fields=key,resolutiondate,versions,created&startAt="
              + i.toString() + "&maxResults=" + j.toString();
      JSONObject json = readJsonFromUrl(url);
      JSONArray issues = json.getJSONArray("issues");
      total = json.getInt("total");
      JSONArray nwa = new JSONArray();
	try (FileWriter fileWriter = new FileWriter(projName + "Bug_Fixed.csv")) {
          fileWriter.append("Key,id,Created_date,Resolution_date");
          fileWriter.append("\n");
          for (; i < total && i < j; i++) {
             JSONObject jo = new JSONObject();
             String key = issues.getJSONObject(i%1000).get("key").toString();
             fileWriter.append(key);
             fileWriter.append(",");
             JSONObject fields= (JSONObject) issues.getJSONObject(i%1000).get("fields");
             String createddate = (String) fields.get("created");
             fileWriter.append(createddate);
             String msg="["+issues.getJSONObject(i%1000).get("key").toString()+"]";
             msg= msg.split("\\[")[1];
             msg= msg.split("\\]")[0];
             tickets.add(msg);
             fileWriter.append("\n");
             jo.put("Key", key);
             jo.put("Created_date", createddate);
             nwa.put(jo);
          }
        JSONObject info = new JSONObject();
        info.put("INFO", nwa);
        try (FileWriter file = new
				  FileWriter("Info.json")) { file.write(info.toString(1)); }
       } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error in csv writer");
       }
   } while (i < total);
   return tickets;
}
  public static List<Object> compareArrayList(List<String> arr1,List<String> arr2 ) {
		float match=0; 
		ArrayList<String> missingtickets = new ArrayList<>();
	    for (int i=0;i<arr2.size();i++) {
			  if (arr1.contains(arr2.get(i))) {
				  match=match+1;
			  }	  
			  else {
				  missingtickets.add(arr2.get(i));
			  }
		  }
	    float per = match / arr2.size();
	    ArrayList<Object> result= new ArrayList<>();
	    result.add(Math.round(per*100));
	    result.add(missingtickets);
		return result;
	  }
  public static void getFixedDate() throws JSONException, IOException {
	  JSONObject bugsfixed = parseJSONFile("Info.json");
	  JSONArray ja = bugsfixed.getJSONArray("INFO");
	  String filename = "CommitLogS2GRAPH.json";
      JSONObject commitlog = parseJSONFile(filename);
      JSONArray try1 = commitlog.getJSONArray(COMMITS_LOG);
      JSONObject read = new JSONObject();
      read.put(COMMITS_LOG, try1);
	  for(int p=0;p<ja.length();p++) {
		  JSONObject bug = ja.getJSONObject(p);
		  String ticketname = bug.getString("Key");
  	      String query = ("$.CommitsLog[?(@.Ticket=='"+ticketname+"')].['CommitTime']");
  		  ReadContext ctx = JsonPath.parse(read.toString());
	      List <String> commitdates = ctx.read(query);
	      if (!commitdates.isEmpty()) {
	      String commitdate = commitdates.get(0); //è la più vecchia
	      bug.put("CommitDate", commitdate.substring(0,7));
	      }
	  }
	  try (FileWriter file = new FileWriter("S2GRAPH_BUGS.json")) { 
		  file.write(bugsfixed.toString(1)); 
		  }
  }
  public static void getMeasures(String projname,List<String> ticketsjira) throws JSONException, IOException {
	  ArrayList<Object> result = (ArrayList<Object>) checkLinkage(projname); 
	  ArrayList <String> ticketsgit = (ArrayList<String>) result.get(1);
	  ArrayList<Object> result2 = (ArrayList<Object>) compareArrayList(ticketsgit,ticketsjira);
	  JSONObject jsonObject = new JSONObject(); 
	  jsonObject.put("TicketsGit", ticketsgit); 
	  jsonObject.put("TicketsJira", ticketsjira);
	  jsonObject.put("MissingTicket",result2.get(1));
	  jsonObject.put("#CommitNotLinked", result.get(2));
	  jsonObject.put("#CommitLinked", result.get(3)); 
	  jsonObject.put("#Commit",  result.get(4));
	  jsonObject.put("Linkage #CommitLinked/#Commit", result.get(0).toString()+" %");
	  jsonObject.put("Linkage (Bug Fixed Ticket Jira/Total Ticket Git)",  result2.get(0).toString()+" %"); 	
	  jsonObject.put("MW_SIZE", ticketsjira.size()*0.1);
	  try (FileWriter file = new FileWriter("LinkageResult"+projname+".json")) { 
		  file.write(jsonObject.toString(1)); 
		  }
  }
  @SuppressWarnings("deprecation")
public static  void json2csv(JSONArray array) throws IOException {         
  	File file=new File("Asse_X"+".csv");
      String csv = CDL.toString(array);
      FileUtils.writeStringToFile(file, csv);
  }
  
  public static void main(String[] args) throws IOException, JSONException { 
      ArrayList<String> ticketsjira1= (ArrayList<String>) getJiraBugFixed(PROJNAME);
      getMeasures(PROJNAME,ticketsjira1);
	  getFixedDate();
	  Map<String, Integer> hm = countFrequencies(validDate());
	  try (FileWriter writer = new FileWriter("Asse_y.csv")) {  // i mesi in cui sono presenti commit
		  writer.write("Anno-mese");
		  writer.write(",");
		  writer.write("Ticket");
		  writer.write("\n");
		  for (Map.Entry<String, Integer> val : hm.entrySet()) {
			  writer.write(val.getKey());
			  writer.write(",");
			  writer.write(val.getValue().toString());
			  writer.write("\n");
		  }
	  }
	  JSONObject bugsfixed = parseJSONFile("S2GRAPH_BUGS.json");
	  json2csv(bugsfixed.getJSONArray("INFO"));
}  }
