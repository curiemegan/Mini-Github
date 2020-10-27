package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.io.File;



public class Stage implements Serializable {
    private HashMap<String, String> stageAdd;
    private HashMap<String, String> stageRemove;

    public Stage() {
        stageAdd = new HashMap<>();
        stageRemove = new HashMap<>();
    }

    public void staging(String fileName, String blobHash) {
        stageAdd.put(blobHash, fileName);
    }

    public void AddToRemove(String fileName, String blobHash){
        stageRemove.put(blobHash, fileName);
    }

    public HashMap<String, String> StageAdded() {
        return stageAdd;
    }

    public HashMap<String, String> StageRemoved() {
        return stageRemove;
    }

    public boolean isEmpty() {
        return (stageAdd.isEmpty() && stageRemove.isEmpty());
    }

    public HashMap<String, String> getStageAdd() {
        return stageAdd;
    }
    public HashMap<String, String> getStageRemove() {
        return stageRemove;
    }

    public void removeFromAdd(String key) {
        stageAdd.remove(key);
    }

    public void removeFromRemove(String key){stageRemove.remove(key);}

    public void clearStage() {
        if (stageAdd != null) {
            stageAdd = new HashMap<String, String>();
        }

        if (stageRemove != null) {
            stageRemove = new HashMap<String, String>();
        }
    }
}