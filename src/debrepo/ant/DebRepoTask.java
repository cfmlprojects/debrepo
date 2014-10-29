package debrepo.ant;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import debrepo.repo.DebRepo;

public class DebRepoTask extends Task {
    private DebRepo.RepoBuilder repoBuilder = new DebRepo.RepoBuilder();
    private Vector<FileSet> filesets = new Vector<FileSet>();

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    public void execute() throws BuildException {
        repoBuilder.build().execute();
    }

    public void setRepoDir(File file) {
        repoBuilder.repoDir(file);
    }

    public void setDebsDir(File file) {
        repoBuilder.debsDir(file);
    }
    
    public void setVerbose(Boolean value) {
        repoBuilder.verbose(value);
    }
    
    public void setKey(String key) {
        repoBuilder.key(key);
    }
    
    public void setKeyring(File keyring) {
        repoBuilder.secring(keyring);
    }
    
    public void setSecRing(File keyring) {
        repoBuilder.secring(keyring);
    }
    
    public void setPassphrase(String passphrase) {
        repoBuilder.passphrase(passphrase);
    }

    public void setDate(String date) {
        repoBuilder.date(date);
    }

    public void setOrigin(String origin) {
        repoBuilder.origin(origin);
    }

    public void setLabel(String label) {
        repoBuilder.label(label);
    }

    public void setSuite(String suite) {
        repoBuilder.suite(suite);
    }

    public void setVersion(String version) {
        repoBuilder.version(version);
    }

    public void setCodename(String codename) {
        repoBuilder.codename(codename);
    }

    public void setArchitectures(String architectures) {
        repoBuilder.architectures(architectures);
    }

    public void setComponents(String components) {
        repoBuilder.components(components);
    }

    public void setDescription(String description) {
        repoBuilder.description(description);
    }
    
    
}