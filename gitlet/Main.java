package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    private static HashMap<Integer, Commit> splitpoints = new HashMap<>();
    static final File GITLETFOLDER = new File (".gitlet");
    static final File COMMITS = new File(".gitlet/commits");
    static final File BLOBS = new File(".gitlet/blobs");
    static final File METADATA = Utils.join(GITLETFOLDER, "metadata");
    static final File STAGE = Utils.join(GITLETFOLDER, "stage");
    private static HashMap<String, Remote> remotes = new HashMap<>();

    public static void main(String... args) throws IOException {
        // FILL THIS IN
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        switch (args[0]) {
            case "init":
                validateNumArgs("init", args, 1);
                init();
                break;
            case "add":
                validateNumArgs("add", args, 2);
                add(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                commit(args[1], null);
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                rm(args[1]);
                break;
            case "log":
                validateNumArgs("log", args, 1);
                log();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                globallog();
                break;
            case "find":
                validateNumArgs("find", args, 2);
                find(args[1]);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                status();
                break;
            case "checkout":
                if (args.length <= 1 || args.length > 4) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    checkoutOne(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    checkoutTwo(args[1], args[3]);
                } else if (args.length == 2) {
                    checkoutThree(args[1]);
                } else {
                    exitWithError("Incorrect operands.");
                }
                break;
            case "branch":
                validateNumArgs("branch", args, 2);
                branch(args[1]);
                break;
            case "merge":
                validateNumArgs("merge", args, 2);
                merge(args[1]);
                break;
            case "add-remote":
                validateNumArgs("add-remote", args, 3);
                addremote(args[1], args[2]);
                break;
            case "rm-remote":
                validateNumArgs("rm-remote", args, 2);
                rmremote(args[1]);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
        return;
    }

    public static void init() throws IOException {
        if (GITLETFOLDER.exists()){
            System.out.println("A gitlet version-control system already exists in the current directory.");
            return;
        } else {
            setupPersistence();
            MetaData initial = new MetaData();
            Utils.writeObject(METADATA, initial);
        }
    }

    public static void setupPersistence() throws IOException {
        GITLETFOLDER.mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
    }

    public static void add(String filename) throws IOException {
        File fileToAdd = new File("./" + filename);
        if(!(GITLETFOLDER.exists())){
            System.out.println("Gitlet system has not been initialized");
            return;
        } else if(!(fileToAdd.exists())){
            System.out.println("File does not exist");
            return;
        } else {
            MetaData currmetadata = Utils.readObject(METADATA, MetaData.class);
            Commit head = currmetadata.getHead();
            HashMap<String, String> currblobs = head.getBlobs();
            String fileId = Utils.sha1(filename, Utils.readContents(fileToAdd));
            if (currblobs.containsKey(fileId)) {
                Stage currStage = currmetadata.getStage();
                HashMap<String,String> stageadd = currStage.getStageAdd();
                HashMap<String, String> stageremove = currStage.getStageRemove();
                if (stageadd.containsKey(fileId)) {
                    currStage.removeFromAdd(fileId);
                    Utils.writeObject(STAGE, currStage);
                    Utils.writeObject(METADATA, currmetadata);
                } else if (stageremove.containsKey(fileId)) {
                    if (!fileToAdd.exists()) {
                        fileToAdd.createNewFile();
                        File savedBlob = new File(".gitlet/blobs/" + fileId + ".txt");
                        Utils.writeContents(fileToAdd, Utils.readContents(savedBlob));
                    }
                    currStage.removeFromRemove(fileId);
                    Utils.writeObject(STAGE, currStage);
                    currmetadata.setStage(currStage);
                    Utils.writeObject(METADATA, currmetadata);
                }
                return;
            }
            HashMap <String, String> blob = (HashMap<String, String>) new HashMap<>().put(fileId, filename);
            File savedBlob = new File(".gitlet/blobs/" + fileId + ".txt");
            savedBlob.createNewFile();
            Utils.writeContents(savedBlob, Utils.readContents(fileToAdd));
            MetaData currMetaData = Utils.readObject(METADATA,MetaData.class);
            Stage stage = Utils.readObject(Main.STAGE, Stage.class);
            if (stage.getStageRemove().containsKey(fileId)) {
                stage.removeFromRemove(fileId);
            }
            stage.staging(filename, fileId);
            Utils.writeObject(STAGE, stage);
            currMetaData.setStage(stage);
            Utils.writeObject(METADATA, currMetaData);
        }

    }

    public static void commit(String msg, Commit mergep) throws IOException {
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        Stage currStage = currMetaData.getStage();
        if (currStage.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        HashMap<String, String> bl = currStage.getStageAdd();
        Commit parent = currMetaData.getHead();
        if (!parent.getBlobs().isEmpty()){
            Map<String, String> prevBlobs = swapper(parent.getBlobs());
         for (String k : prevBlobs.keySet()) {
                if (!bl.containsValue(k)) {
                    bl.put(prevBlobs.get(k), k);
                }
                if (currStage.getStageRemove().containsValue(k)) {
                    bl.remove(prevBlobs.get(k));
                }
            }
    }
        Commit newCommit = new Commit(parent, msg, bl);
        if (mergep != null) {
            newCommit = new Commit(parent, mergep, msg, bl);
        }
        currMetaData.updateMetaData(newCommit);
        Utils.writeObject(METADATA, currMetaData);
    }


    public static void checkoutOne(String filename) throws IOException {
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        Commit currHead = currMetaData.getHead();
        HashMap<String, String> currBlobs = currHead.getBlobs();
        Map<String,String> swapped = swapper(currBlobs);
        if (swapped.containsKey(filename)) {
            String currID = swapped.get(filename);
            File checkOutFile = new File(".gitlet/blobs/" + currID + ".txt");
            File directoryFile = new File("./" + filename);
            if (!directoryFile.exists()) {
                directoryFile.createNewFile();
            }
            Utils.writeContents(directoryFile, Utils.readContentsAsString(checkOutFile));
        }else{
            System.out.println("File does not exists in that commit");
        }
        return;
    }

    private static Map<String, String> swapper(HashMap<String, String> hm){
        Map<String, String> map = hm;
        Map<String, String> swapped = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return swapped;
    }

    public static void checkoutTwo(String commit, String filename) throws IOException {
        MetaData currmetadata = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, Commit> allcommits = currmetadata.getAllcommits();
        String fileid = null;
        for (String id : allcommits.keySet()) {
            if (id.indexOf(commit) == 0) {
                fileid = id;
            }
        }
        if (fileid == null) {
            System.out.println("No commit with that id exists");
            return;
        }
        File cmt = new File(".gitlet/commits/" + fileid + ".txt");
        Commit currCommit = Utils.readObject(cmt,Commit.class);
        HashMap<String, String> currBlobs = currCommit.getBlobs();
        Map<String,String> swapped = swapper(currBlobs);
        if (swapped.containsKey(filename)) {
            String currID = swapped.get(filename);
            File checkOutFile = new File(".gitlet/blobs/" + currID + ".txt");
            File directoryFile = new File("./" + filename);
            if (!directoryFile.exists()) {
                directoryFile.createNewFile();
            }
            Utils.writeContents(directoryFile, Utils.readContents(checkOutFile));
        }else {
            System.out.println("File does not exists in that commit");
            return;
        }
    }

    public static void checkoutThree(String branchName) throws IOException {
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, String> branches = currMetaData.getBranches();
        HashMap<String, Commit> allcommits = currMetaData.getAllcommits();
        String currBranch = currMetaData.getCurrBranch();
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists");
            return;
        }
        if (branchName.equals(currBranch)) {
            System.out.println("No need to checkout the current branch");
            return;
        }
        String commitId = branches.get(branchName);
        Commit currCmt = currMetaData.getHead();
        Commit newCmt = allcommits.get(commitId);

        HashMap<String, String> currBlobs = currCmt.getBlobs();
        Map<String, String> currSwapped = swapper(currBlobs);

        HashMap<String, String> newBlobs = newCmt.getBlobs();
        Map<String,String> newSwapped = swapper(newBlobs);

        for (String k : newSwapped.keySet()){
            File a = new File("./" + k);
            if ((!currSwapped.containsKey(k) && newSwapped.containsKey(k)) && a.exists()){
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }

        for (String k : newSwapped.keySet()){
            File checkOutFile = new File(".gitlet/blobs/" + newSwapped.get(k) + ".txt");
            File directoryFile = new File("./" + k);
            if (!directoryFile.exists()) {
                directoryFile.createNewFile();
            }
            Utils.writeContents(directoryFile, Utils.readContents(checkOutFile));
        }

        List<String> workingDirectoryFiles = Utils.plainFilenamesIn("./");
        for (String filename : workingDirectoryFiles) {
            if (!newSwapped.containsKey(filename)) {
                File t = new File("./" + filename);
                t.delete();
            }
        }

        currMetaData.setCurrBranch(branchName);
        currMetaData.setHead(allcommits.get(branches.get(branchName)));
        Stage currStage = currMetaData.getStage();
        currStage.clearStage();
        Utils.writeObject(STAGE, currStage);
        currMetaData.setStage(currStage);
        Utils.writeObject(METADATA, currMetaData);
    }

    public static void rm(String filename){
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        File toRemove = new File("./" + filename);
        Stage currStage = currMetaData.getStage();
        HashMap<String, String> currCommitBlobs = currMetaData.getHead().getBlobs();
        Map<String,String> swappedTwo = swapper(currCommitBlobs);
        Map<String,String> swappedOne = swapper(currStage.getStageAdd());
        if (swappedOne.containsKey(filename)){
            String removeId = swappedOne.get(filename);
            currStage.removeFromAdd(removeId);
            Utils.writeObject(STAGE, currStage);
            currMetaData.setStage(currStage);
            Utils.writeObject(METADATA, currMetaData);
        }else if (swappedTwo.containsKey(filename)) {
            String removeToStage = swappedTwo.get(filename);
            currStage.AddToRemove(filename, removeToStage);
            toRemove.delete();
            Utils.writeObject(STAGE, currStage);
            currMetaData.setStage(currStage);
            Utils.writeObject(METADATA, currMetaData);
        } else {
            System.out.println("No reason to remove the File");
            return;
        }
    }

    public static void log() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        MetaData currMetadata = Utils.readObject(METADATA, MetaData.class);
        Commit currentCommit = currMetadata.getHead();
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommit.getId());
            if (currentCommit.getMergeparent() != null) {
                System.out.println("Merge: " + currentCommit.getParent().getId().substring(0,7)
                        + " " + currentCommit.getMergeparent().getId().substring(0,7));
            }
            System.out.println("Date: " + sdf.format(currentCommit.getTimestamp()));
            System.out.println(currentCommit.getMessage());
            System.out.println();
            currentCommit = currentCommit.getParent();
        }
    }

    public static void globallog() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        MetaData currMetadata = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, Commit> allcommits = currMetadata.getAllcommits();
        for (String k : allcommits.keySet()) {
            Commit a = allcommits.get(k);
            System.out.println("===");
            System.out.println("commit " + a.getId());
            System.out.println("Date: " + sdf.format(a.getTimestamp()));
            System.out.println(a.getMessage());
            System.out.println();
        }
    }

    public static void find(String commitMsg){
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, Commit> ac = currMetaData.getAllcommits();
        ArrayList<String> temp = new ArrayList<>();
        for (String k : ac.keySet()){
            Commit currCommit = ac.get(k);
            if (currCommit.getMessage().equals(commitMsg)){
                temp.add(currCommit.getId());
            }
        }
        if (temp.isEmpty()){
            System.out.println("Found no commit with that message.");
            return;
        } else {
            for(int i = 0; i < temp.size(); i++){
                System.out.println(temp.get(i));
            }
        }
    }

    public static void status() {
        if(!(GITLETFOLDER.exists())){
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, String> savedBranches = currMetaData.getBranches();
        String currbranch = currMetaData.getCurrBranch();
        System.out.println("=== Branches ===");
        System.out.print("*");
        System.out.println(currbranch);
        for (String branchname : savedBranches.keySet()) {
            if (!branchname.equals(currbranch)) {
                System.out.println(branchname);
            }
        }
        System.out.println();

        Stage currstage = Utils.readObject(STAGE, Stage.class);
        HashMap<String, String> stageadd = currstage.getStageAdd();
        HashMap<String, String> stageremove = currstage.getStageRemove();
        System.out.println("=== Staged Files ===");
        for (String fileid : stageadd.keySet()) {
            System.out.println(stageadd.get(fileid));
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String fileid : stageremove.keySet()) {
            System.out.println(stageremove.get(fileid));
        }
        System.out.println();
        extracreditstatus();
    }

    public static void extracreditstatus() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        MetaData currmetadata = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, String> currblobs = currmetadata.getHead().getBlobs();
        Map<String, String> currblobswapped = swapper(currblobs);
        Stage currstage = currmetadata.getStage();
        HashMap<String, String> stageadd = currstage.getStageAdd();
        Map<String, String> stageaddswapped = swapper(stageadd);
        HashMap<String, String> stageremove = currstage.getStageRemove();
        Map<String, String> stageremoveswapped = swapper(stageremove);
        for (String filename : stageaddswapped.keySet()) {
            File t = new File("./" + filename);
            if (!t.exists()) {
                System.out.println(filename + " (deleted)");
            } else {
                String fileid = Utils.sha1(filename, Utils.readContents(t));
                if (!stageaddswapped.get(filename).equals(fileid)) {
                    System.out.println(filename + " (modified");
                }
            }
        }
        for (String filename : currblobswapped.keySet()) {
            File t = new File("./" + filename);
            if (!t.exists() && !stageremoveswapped.containsKey(filename)) {
                System.out.println(filename + " (deleted)");
            } else if (t.exists()) {
                String fileid = Utils.sha1(filename, Utils.readContents(t));
                if (!currblobswapped.get(filename).equals(fileid) && !stageaddswapped.containsKey(filename)) {
                    System.out.println(filename + " (modified)");
                }
            }
        }
        System.out.println();

        List<String> workingDirectoryFiles = Utils.plainFilenamesIn("./");
        System.out.println("=== Untracked Files ===");
        for (String filename : workingDirectoryFiles) {
            if (!currblobswapped.containsKey(filename) && !stageaddswapped.containsKey(filename)) {
                System.out.println(filename);
            }
            else if (stageremoveswapped.containsKey(filename)) {
                System.out.println(filename);
            }
        }
        System.out.println();
    }

    public static void branch(String branchName){
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, String> savedBranches = currMetaData.getBranches();
        Commit currCommit = currMetaData.getHead();
        if (savedBranches.containsKey(branchName)){
            System.out.println("A branch with that name already exists");
            return;
        }
        savedBranches.put(branchName, currCommit.getId());
        Utils.writeObject(METADATA, currMetaData);
    }

    public static void rmBranch(String branchName){
        MetaData currMetaData = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, String> savedBranches = currMetaData.getBranches();
        if (!savedBranches.containsKey(branchName)){
            System.out.println("A branch with that name does not exist");
            return;
        }else if (currMetaData.getCurrBranch().equals(branchName)){
            System.out.println("Cannot remove the current branch");
            return;
        } else {
            savedBranches.remove(branchName);
        }
        Utils.writeObject(METADATA, currMetaData);
    }

    public static void reset(String commitId) throws IOException {
        MetaData currmetadata = Utils.readObject(METADATA, MetaData.class);
        HashMap<String, Commit> allcommits = currmetadata.getAllcommits();
        String fileid = null;
        for (String id : allcommits.keySet()) {
            if (id.indexOf(commitId) == 0) {
                fileid = id;
            }
        }
        if (fileid == null) {
            System.out.println("No commit with that id exists");
            return;
        }
        File cmt = new File(".gitlet/commits/" + fileid + ".txt");
        Commit currCommit = Utils.readObject(cmt,Commit.class);
        HashMap<String, String> currBlobs = currCommit.getBlobs();
        Map<String,String> swappedOne = swapper(currBlobs);

        String currBranchHeadId = currmetadata.getBranches().get(currmetadata.getCurrBranch());
        File headCmt = new File(".gitlet/commits/" + currBranchHeadId + ".txt");
        Commit headCommit = Utils.readObject(headCmt,Commit.class);
        Map<String, String> swappedTwo = swapper(headCommit.getBlobs());

        for (String k : swappedOne.keySet()){
            File a = new File("./" + k);
            if ((!swappedTwo.containsKey(k) && swappedOne.containsKey(k)) && a.exists()){
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }

        for (String k : swappedOne.keySet()){
            File a = new File("./" + k);
            if ((swappedOne.containsKey(k) && !swappedTwo.containsKey(k)) && a.exists()){
                a.delete();
            }
        }
        Stage currstage = currmetadata.getStage();
        HashMap<String, String> stageadd = currstage.getStageAdd();

        for (String id : stageadd.keySet()) {
            if (!currBlobs.containsKey(id)) {
                currstage.removeFromAdd(id);
            }
        }

        for (String k : swappedOne.keySet()){
            checkoutTwo(commitId, k);
        }

        currmetadata.setHead(currCommit);
        Utils.writeObject(STAGE, currstage);
        currmetadata.setStage(currstage);
        Utils.writeObject(METADATA, currmetadata);
    }

    public static void merge(String branchName) throws IOException{
        MetaData currmetadata = Utils.readObject(METADATA, MetaData.class);
        String currbranch = currmetadata.getCurrBranch();
        HashMap<String, String> branches = currmetadata.getBranches();
        Stage currstage = currmetadata.getStage();
        HashMap<String, String> stageadd = currstage.getStageAdd();
        HashMap<String, String> stageremove = currstage.getStageRemove();
        
        if (!stageadd.isEmpty() || !stageremove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(currbranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        
        String id = branches.get(branchName);
        Commit givencommit = currmetadata.getAllcommits().get(id);
        Commit currcommit = currmetadata.getHead();
        findSplitPoint(currcommit, givencommit, 0);
        Commit splitpoint = null;
        int min = Integer.MAX_VALUE;
        for (int a : splitpoints.keySet()) {
            if (a < min) {
                min = a;
                splitpoint = splitpoints.get(min);
            }
        }
        
        HashMap<String, String> givenblobs = givencommit.getBlobs();
        HashMap<String, String> currblobs = currcommit.getBlobs();
        HashMap<String, String> splitblobs = splitpoint.getBlobs();
        Map<String, String> currblobswapped = swapper(currblobs);
        Map<String, String> givenblobswapped = swapper(givenblobs);
        Map<String, String> splitblobswapped = swapper(splitblobs);
        
        for (String k : givenblobswapped.keySet()){
            File a = new File("./" + k);
            if ((!currblobswapped.containsKey(k) && givenblobswapped.containsKey(k)) && a.exists()){
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }

        if (splitpoint.getId().equals(givencommit.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (splitpoint.getId().equals(currcommit.getId())) {
            checkoutThree(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        boolean conflict = false;
        for (String filename : splitblobswapped.keySet()) {
            File t = new File("./" + filename);
            File splitfile = new File(".gitlet/blobs/" + splitblobswapped.get(filename) + ".txt");
            File currfile = new File(".gitlet/blobs/" + currblobswapped.get(filename) + ".txt");
            File givenfile = new File(".gitlet/blobs/" + givenblobswapped.get(filename) + ".txt");
            if (givenblobswapped.containsKey(filename) && currblobswapped.containsKey(filename)) {
                if ((!givenblobswapped.get(filename).equals(splitblobswapped.get(filename))) && currblobswapped.get(filename).equals(splitblobswapped.get(filename))) {
                    checkoutTwo(givencommit.getId(), filename);
                    add(filename);
                } else if ((!givenblobswapped.get(filename).equals(splitblobswapped.get(filename))) && (!currblobswapped.get(filename).equals(splitblobswapped.get(filename)))) {
                    if ((!givenblobswapped.get(filename).equals(currblobswapped.get(filename)))) {
                        conflict = true;
                        Utils.writeContents(t, "<<<<<<< HEAD\n" +
                                Utils.readContentsAsString(currfile) +
                                "=======\n" +
                                Utils.readContentsAsString(givenfile)  +
                                ">>>>>>>\n");
                        add(filename);
                    }
                }
            } else if (!givenblobswapped.containsKey(filename) && currblobswapped.containsKey(filename)) {
                if (currblobswapped.get(filename).equals(splitblobswapped.get(filename))) {
                    rm(filename);
                } else {
                    conflict = true;
                    Utils.writeContents(t, "<<<<<<< HEAD\n" +
                            Utils.readContentsAsString(currfile) +
                            "=======\n" +
                            ""  +
                            ">>>>>>>\n");
                    add(filename);
                }
            } else if (!currblobswapped.containsKey(filename) && givenblobswapped.containsKey(filename)) {
                if (!givenblobswapped.get(filename).equals(splitblobswapped.get(filename))) {
                    conflict = true;
                    Utils.writeContents(t, "<<<<<<< HEAD\n" +
                            "" +
                            "=======\n" +
                            Utils.readContentsAsString(givenfile)  +
                            ">>>>>>>\n");
                    add(filename);
                }
            }
        }
        for (String filename : givenblobswapped.keySet()) {
            File t = new File("./" + filename);
            File currfile = new File(".gitlet/blobs/" + currblobswapped.get(filename) + ".txt");
            File givenfile = new File(".gitlet/blobs/" + givenblobswapped.get(filename) + ".txt");
            if (!currblobswapped.containsKey(filename) && !splitblobswapped.containsKey(filename)) {
                checkoutTwo(givencommit.getId(), filename);
                add(filename);
            } else if (currblobswapped.containsKey(filename) && !splitblobswapped.containsKey(filename)) {
                if (!givenblobswapped.get(filename).equals(currblobswapped.get(filename))) {
                    conflict = true;
                    if (!t.exists())  {
                        t.createNewFile();
                    }
                    Utils.writeContents(t, "<<<<<<< HEAD\n" +
                            Utils.readContentsAsString(currfile) +
                            "=======\n" +
                            Utils.readContentsAsString(givenfile)  +
                            ">>>>>>>\n");
                    add(filename);
                }
            }
        }
        commit("Merged " + branchName + " into " + currbranch+".", givencommit);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static void findSplitPoint(Commit curr, Commit branch, int step) {
        if (curr == null || branch == null) {
            return;
        }
        if (curr.getId().equals(branch.getId())) {
            splitpoints.put(step, curr);
            return;
        }
        else {
            findSplitPoint(curr.getParent(), branch, step+1);
            findSplitPoint(curr, branch.getParent(), step+1);
            if (curr.getMergeparent() != null) {
                findSplitPoint(curr.getMergeparent(), branch, step + 1);
            }
            if (branch.getMergeparent() != null) {
                findSplitPoint(curr, branch.getMergeparent(), step+1);
            }
        }
    }

    public static void exitWithError(String message){
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            exitWithError("Incorrect operands.");
        }
    }

    public static boolean notimportantfile(File file) {
        for (int i = 0; i < importantfiles.length; i++) {
            if (importantfiles[i].equals(file.getName())) {
                return false;
            }
        }
        return true;
    }

    private static String [] importantfiles = {".gitlet",
            ".idea", "gitlet", ".gitignore",
            "testing", "proj2.iml",
            "out"};

    private static void clearDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                clearDirectory(file);
            }
            file.delete();
        }
        dir.delete();
    }

}
