package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.io.File;

public class MetaData implements Serializable {
    private HashMap<String, String> treeMap;
    private HashMap<String, String> branches;
    private HashMap<String, Commit> allcommits;
    private String currBranch;
    private Stage stage;
    private Commit prevCommit;
    private Commit head;


    public MetaData() throws IOException {
        treeMap = new HashMap<>();
        branches = new HashMap<>();
        allcommits = new HashMap<>();
        currBranch = "master";
        stage = new Stage();
        Commit initCommit = new Commit();
        head = initCommit;
        String id = initCommit.getId();
        File initFile = new File(".gitlet/commits/" + id + ".txt");
        initFile.createNewFile();
        Utils.writeObject(initFile, initCommit);
        Utils.writeObject(Main.STAGE, stage);
        treeMap.put(id, null);
        branches.put(currBranch, id);
        allcommits.put(id, initCommit);
        prevCommit = initCommit;
    }

    public void updateMetaData(Commit c) throws IOException {
        stage = Utils.readObject(Main.STAGE, Stage.class);
        String commitId = c.getId();
        String commitParentId = c.getParent().getId();
        treeMap.put(commitId,commitParentId);
        branches.put(currBranch,commitId);
        allcommits.put(commitId, c);
        head = c;
        stage.clearStage();
        Utils.writeObject(Main.STAGE, stage);
        File commitFile = new File(".gitlet/commits/" + commitId + ".txt");
        commitFile.createNewFile();
        Utils.writeObject(commitFile, c);
        prevCommit = c;
    }

    public Commit getHead(){
        return head;
    }

    public Commit getPrevCommit(){
        return prevCommit;
    }

    public Stage getStage(){
        return stage;
    }

    public HashMap<String, Commit> getAllcommits() {
        return allcommits;
    }

    public String getCurrBranch() {
        return currBranch;
    }

    public HashMap<String, String> getBranches() {
        return branches;
    }

    public void setCurrBranch(String BranchName){
        currBranch = BranchName;
        String commitid = branches.get(BranchName);
        head = allcommits.get(commitid);
    }

    public void setStage(Stage s){
        Utils.writeObject(Main.STAGE, s);
        stage = s;
    }

    public void setHead(Commit c) {
        head = c;
        branches.put(currBranch, c.getId());
    }


}