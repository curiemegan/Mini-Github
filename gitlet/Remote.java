package gitlet;

import java.io.File;
import java.io.IOException;

public class Remote {
    private String remotename;
    private File directory;
    private MetaData remotemetadata;

    public Remote(String remotename, File directory) throws IOException {
        this.remotename = remotename;
        this.directory = directory;
        remotemetadata = new MetaData();
    }

    public MetaData getRemotemetadata() {
        return remotemetadata;
    }


}
