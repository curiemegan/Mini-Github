package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Date;
import java.io.Serializable;


public class Commit implements Serializable {
    private String id;
    private Commit parent;
    private Commit mergeparent = null;
    private Date timestamp;
    private String message;
    private HashMap<String,String> blobs;

    public Commit(){
        message = "initial commit";
        id = Utils.sha1(message);
        parent = null;
        timestamp = new Date(0);
        blobs = new HashMap<>();
    }

    public Commit(Commit p, String m, HashMap<String,String> b) {
        parent = p;
        String parentId = p.getId();
        timestamp = new Date();
        message = m;
        id = Utils.sha1(timestamp.toString(), message, parentId);
        blobs = new HashMap<>();
        for(String k : b.keySet()){
            blobs.put(k, b.get(k));
        }

    }

    public Commit(Commit p, Commit mergep, String m, HashMap<String, String> b) {
        parent = p;
        mergeparent = mergep;
        String parentId = p.getId();
        timestamp = new Date();
        message = m;
        id = Utils.sha1(timestamp.toString(), message, parentId);
        blobs = new HashMap<>();
        for(String k : b.keySet()){
            blobs.put(k, b.get(k));
        }
    }

    public void setParent(Commit prnt){
        parent = prnt;
    }

    public void setMessage(String msg){
        message = msg;
    }

    public String getId() {
        return id;
    }

    public Date getTimestamp(){
        return timestamp;
    }

    public Commit getParent(){
        return parent;
    }

    public String getMessage(){
        return message;
    }

    public HashMap<String,String> getBlobs(){
        return blobs;
    }

    public Commit getMergeparent() {return mergeparent;}

}