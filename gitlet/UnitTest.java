package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import java.util.Map;
import java.util.HashMap;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    @Test
    public void addTest() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "i want to drop this class ;(");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        Stage currStage = Utils.readObject(Main.STAGE, Stage.class);
        System.out.println(currStage.getStageAdd());
        clearDirectory(new File(".gitlet"));
        assertTrue(toAdd.delete());
    }

    @Test
    public void duplicateAdds() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "adding");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        String[] commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        Stage stage = Utils.readObject(Main.STAGE, Stage.class);
        System.out.println(stage.getStageAdd());
        Main.main(args);
        stage = Utils.readObject(Main.STAGE, Stage.class);
        System.out.println(stage.getStageAdd());
        clearDirectory(new File(".gitlet"));
        assertTrue(toAdd.delete());
    }

    @Test
    public void testLog() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "adding");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        String[] commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        File toAdd2 = new File("./toAdd.txt");
        toAdd2.createNewFile();
        Utils.writeContents(toAdd, "adding2");
        args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        commitargs = new String[]{"commit", "second committing"};
        Main.main(commitargs);
        String[] logargs = new String[]{"log"};
        Main.main(logargs);
        clearDirectory(new File(".gitlet"));
        assertTrue(toAdd.delete());
        //assertTrue(toAdd2.delete());
    }

    @Test
    public void testglobalLog() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "adding");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        String[] commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        File toAdd2 = new File("./toAdd2.txt");
        toAdd2.createNewFile();
        Utils.writeContents(toAdd2, "adding2");
        args = new String[]{"add", "toAdd2.txt"};
        Main.main(args);
        commitargs = new String[]{"commit", "second committing"};
        Main.main(commitargs);
        String[] logargs = new String[]{"global-log"};
        Main.main(logargs);
        clearDirectory(new File(".gitlet"));
        assertTrue(toAdd.delete());
        assertTrue(toAdd2.delete());
    }

    @Test
    public void testBranch() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "adding");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        String[] commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        File toAdd2 = new File("./toAdd2.txt");
        toAdd2.createNewFile();
        Utils.writeContents(toAdd2, "adding2");
        args = new String[]{"add", "toAdd2.txt"};
        Main.main(args);
        commitargs = new String[]{"commit", "second committing"};
        Main.main(commitargs);
        String[] branchargs = new String[]{"branch", "branchtest1"};
        Main.main(branchargs);
        MetaData currMetaData = Utils.readObject(Main.METADATA, MetaData.class);
        HashMap<String, String> currBranches = currMetaData.getBranches();
        String branchTestId = currMetaData.getHead().getId();
        assertTrue(currBranches.containsKey(branchargs[1]));
        assertEquals(currBranches.get(branchargs[1]), branchTestId);
        clearDirectory(new File(".gitlet"));
        assertTrue(toAdd.delete());
        assertTrue(toAdd2.delete());
    }

    @Test
    public void testRm() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "adding");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        String[] rmArgs = new String[]{"rm", "toAdd.txt"};
        Main.main(rmArgs);
        MetaData currMetaData = Utils.readObject(Main.METADATA, MetaData.class);
        HashMap<String,String> currStageAdd = currMetaData.getStage().getStageAdd();
        assertTrue(!currStageAdd.containsValue(rmArgs[1]));
        Utils.writeObject(Main.METADATA, currMetaData);
        Main.main(args);
        String[] commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        Main.main(rmArgs);
        MetaData currMetaDataI = Utils.readObject(Main.METADATA, MetaData.class);
        HashMap<String,String> currStageRemove = currMetaDataI.getStage().getStageRemove();
        System.out.println(currStageRemove);
        Main.main("status");
        assertTrue(!toAdd.exists());
        clearDirectory(new File(".gitlet"));
    }

    @Test
    public void testFind() throws IOException {
        Main.main("init");
        File toAdd = new File("./toAdd.txt");
        toAdd.createNewFile();
        Utils.writeContents(toAdd, "adding");
        String[] args = new String[]{"add", "toAdd.txt"};
        Main.main(args);
        String[] commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        File toAdd2 = new File("./toAdd2.txt");
        toAdd2.createNewFile();
        Utils.writeContents(toAdd2, "adding2");
        args = new String[]{"add", "toAdd2.txt"};
        Main.main(args);
        commitargs = new String[]{"commit", "committing"};
        Main.main(commitargs);
        String[] findArgs = new String[]{"find", "committing"};
        Main.main(findArgs);
        clearDirectory(new File(".gitlet"));
        assertTrue(toAdd.delete());
        assertTrue(toAdd2.delete());
    }


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