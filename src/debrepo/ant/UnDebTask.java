package debrepo.ant;

import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import debrepo.repo.utils.UnDeb;

public class UnDebTask extends Task {
    private File debFile, dest;
    private boolean verbose = false;

    public void execute() throws BuildException {
        try {
            UnDeb.unDeb(debFile, dest, verbose);
        } catch (IOException | ArchiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setDebFile(File file) {
        debFile = file;
    }

    public void setDest(File file) {
        dest = file;
    }
    
    public void setVerbose(boolean enable) {
        verbose = enable;
    }
    
   
    
}