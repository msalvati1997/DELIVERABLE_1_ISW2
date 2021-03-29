package main;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class RepositoryClass {
	private Repository repo;
	private Git git;
	private String path;
	
	public RepositoryClass(String path) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        this.repo=builder.setGitDir(new File(path)).setMustExist(true).build();
        this.git=new Git(repo);
        this.setPath(path);
	}
	 public Repository getRepo() {
		return repo;
	}
	public void setRepo(Repository repo) {
		this.repo = repo;
	}
	public Git getGit() {
		return git;
	}
	public void setGit(Git git) {
		this.git = git;
	}
	public static void cloneRepository(String repositorylink,String directory) throws  GitAPIException {
	  		Git.cloneRepository()
	           .setURI(repositorylink)
	           .setDirectory(new File(directory))
	           .call();
	}
    //this class return commit info with the particular id 
	public JSONObject returnCommitsFromString(Git git,String s) throws IOException,  GitAPIException, JSONException{ 
   	        JSONObject jsonObject = new JSONObject();
	    	Iterable<RevCommit> log = git.log().call();
	 	    RevCommit previousCommit = null;
	   	    for (RevCommit commit : log) {
	   	        if (previousCommit != null) {
	   	            AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( previousCommit, git );
	   	            AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( commit, git );
	   	            OutputStream outputStream = new ByteArrayOutputStream();
	   	            try( DiffFormatter formatter = new DiffFormatter(outputStream)) {
	   	              formatter.setRepository( git.getRepository() );
	   	              formatter.format( oldTreeIterator, newTreeIterator );
	   	            }
	   	            }
	   	        String logMessage = commit.getShortMessage();
 	   		    Long temp = Long.parseLong(commit.getCommitTime()+"") * 1000; 
 	            Date date = new Date(temp);
	   	        if (logMessage.startsWith(s))  {
	   		    JSONArray array = new JSONArray();
	   		    array.put("CommitShortMEssage:"+ logMessage);
	   		    array.put("CommitTime:"+ date);
                jsonObject.put(commit.getId().toString(), array);
	   	         }
	   	        previousCommit = commit;
	   	        }
	   	    git.close();
			return jsonObject;
	   	}
	
	private static AbstractTreeIterator getCanonicalTreeParser( ObjectId commitId ,Git git) throws IOException {
			try( RevWalk walk = new RevWalk( git.getRepository() ) ) {
		      RevCommit commit = walk.parseCommit( commitId );
		      ObjectId treeId = commit.getTree().getId();
		      try( ObjectReader reader = git.getRepository().newObjectReader() ) {
		        return new CanonicalTreeParser( null, reader, treeId );
		      }
		    }
		  }
	public  JSONObject getcommitlogs(Git git) throws IOException, GitAPIException, JSONException {
	    	    JSONObject jsonObject = new JSONObject();
	    	    Iterable<RevCommit> log = git.log().call();
	            Pattern p = Pattern.compile("S2GRAPH-\\d{2,}");
	            RevCommit previousCommit = null;
	    	    for (RevCommit commit : log) {
	    	        if (previousCommit != null) {
	    	            AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( previousCommit, git );
	    	            AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( commit, git );
	    	            OutputStream outputStream = new ByteArrayOutputStream();
	    	            try( DiffFormatter formatter = new DiffFormatter( outputStream ) ) {
	    	              formatter.setRepository( git.getRepository() );
	    	              formatter.format( oldTreeIterator, newTreeIterator );
	    	            }
	    	        }
	    	        JSONArray array = new JSONArray();
	    	        String logMessage = commit.getShortMessage();
	    	   		Long temp = Long.parseLong(commit.getCommitTime()+"") * 1000; 
	    	        Date date = new Date(temp);
	    	        @SuppressWarnings("deprecation")
					String data = date.toLocaleString();
	    	        String meseannoformat = data.split(" ")[1]+" "+data.split(" ")[2];
	    	        meseannoformat=meseannoformat.split(",")[0];
		   		    array.put("CommitShortMEssage:"+ logMessage);
		   		    array.put("CommitTime:"+ meseannoformat);
		   	   	    array.put("CommitLongMEssage:"+ commit.getFullMessage());
		   	        array.put("Tree:"+ commit.getTree());
		 	        Matcher m = p.matcher(logMessage);
		 	        if (m.find()) {
		 	             	array.put("Linked: Yes");
		 	            	array.put("Ticket:" + m.group(0));
		 	               }
		   	        
		   	        else {
		   	        	array.put("Linked: No");
		   	        	array.put("Ticket: No");
		   	        }
	                jsonObject.put(commit.getName(), array);
	    	        previousCommit = commit;
	    	    }
	    	    git.close();
				return jsonObject;
	    	}
	    
	public static List<String> fetchGitBranches(Git git) throws GitAPIException {
              ArrayList <String> branches = new ArrayList<>();          
              List<Ref> call = git.branchList().setListMode(ListMode.ALL).call();
              for (Ref ref : call) {
            	   String[] name = ref.getName().split("/");
                   branches.add(name[name.length-1]);
        }
              return branches;
	    }
    
	public String getPath() {
				return path;
			}
	public void setPath(String path) {
				this.path = path;
			}
	
	public static void main(String[] args) throws IOException, GitAPIException, JSONException {
	    	RepositoryClass rep = new RepositoryClass("C:\\Users\\salva\\git\\incubator-s2graph\\.git");	
	    	JSONObject json = rep.getcommitlogs(rep.getGit());
	    	try (FileWriter file = new FileWriter("CommitLog.json")) {
				file.write(json.toString(1));
			}
	    	}
	    	
	    }
	    
	