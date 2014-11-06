package debrepo.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Tar.TarFileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.DebMaker;
import org.vafer.jdeb.PackagingException;
import org.vafer.jdeb.ant.Data;
import org.vafer.jdeb.ant.DebAntTask;
import org.vafer.jdeb.producers.DataProducerFileSet;

/**
 * Task that creates a Debian package.
 *
 * @antTaskName deb
 */
public class DebTask extends Task {
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9+\\-.]+");

    public static class Description extends ProjectComponent {
        private String _synopsis;
        private String _extended = "";

        public String getSynopsis() {
            return _synopsis;
        }

        public void setSynopsis(String synopsis) {
            _synopsis = synopsis.trim();
        }

        public void addText(String text) {
            _extended += getProject().replaceProperties(text);
        }

        public String getExtended() {
            return _extended;
        }

        public String getExtendedFormatted() {
            StringBuffer buffer = new StringBuffer(_extended.length());

            String lines[] = _extended.split("\n");

            int start = 0;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                if (line.length() > 0)
                    break;

                start++;
            }

            int end = lines.length;

            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();

                if (line.length() > 0)
                    break;

                end--;
            }

            for (int i = start; i < end; i++) {
                String line = lines[i].trim();

                buffer.append(' ');
                buffer.append(line.length() == 0 ? "." : line);
                buffer.append('\n');
            }

            buffer.deleteCharAt(buffer.length() - 1);

            return buffer.toString();
        }
    }

    public static class Version extends ProjectComponent {
        private static final Pattern UPSTREAM_VERSION_PATTERN = Pattern.compile("[0-9][A-Za-z0-9.+\\-:~]*");
        private static final Pattern DEBIAN_VERSION_PATTERN = Pattern.compile("[A-Za-z0-9+.~]+");

        private int _epoch = 0;
        private String _upstream;
        private String _debian = "1";

        public void setEpoch(int epoch) {
            _epoch = epoch;
        }

        public void setUpstream(String upstream) {
            _upstream = upstream.trim();

            if (!UPSTREAM_VERSION_PATTERN.matcher(_upstream).matches())
                throw new BuildException("Invalid upstream version number!");
        }

        public void setDebian(String debian) {
            _debian = debian.trim();

            if (_debian.length() > 0 && !DEBIAN_VERSION_PATTERN.matcher(_debian).matches())
                throw new BuildException("Invalid debian version number!");
        }

        public String toString() {
            StringBuffer version = new StringBuffer();

            if (_epoch > 0) {
                version.append(_epoch);
                version.append(':');
            } else if (_upstream.indexOf(':') > -1)
                throw new BuildException("Upstream version can contain colons only if epoch is specified!");

            version.append(_upstream);

            if (_debian.length() > 0) {
                version.append('-');
                version.append(_debian);
            } else if (_upstream.indexOf('-') > -1)
                throw new BuildException("Upstream version can contain hyphens only if debian version is specified!");

            return version.toString();
        }
    }

    public static class Maintainer extends ProjectComponent {
        private String _name;
        private String _email;

        public void setName(String name) {
            _name = name.trim();
        }

        public void setEmail(String email) {
            _email = email.trim();
        }

        public String toString() {
            if (_name == null || _name.length() == 0)
                return _email;

            StringBuffer buffer = new StringBuffer(_name);

            buffer.append(" <");
            buffer.append(_email);
            buffer.append(">");

            return buffer.toString();
        }
    }

    public static class Changelog extends ProjectComponent {
        public static class Format extends EnumeratedAttribute {
            public String[] getValues() {
                // XML format will be added when supported
                return new String[] { "plain" /* , "xml" */};
            }
        }

        private static final String STANDARD_FILENAME = "changelog.gz";
        private static final String DEBIAN_FILENAME = "changelog.Debian.gz";

        private String _file;
        private Changelog.Format _format;
        private boolean _debian;

        public Changelog() {
            _debian = false;
            _format = new Changelog.Format();
            _format.setValue("plain");
        }

        public void setFile(String file) {
            _file = file.trim();
        }

        public String getFile() {
            return _file;
        }

        public void setFormat(Changelog.Format format) {
            _format = format;
        }

        public Changelog.Format getFormat() {
            return _format;
        }

        public void setDebian(boolean debian) {
            _debian = debian;
        }

        public boolean isDebian() {
            return _debian;
        }

        public String getTargetFilename() {
            return _debian ? DEBIAN_FILENAME : STANDARD_FILENAME;
        }
    }

    public static class Section extends EnumeratedAttribute {
        private static final String[] PREFIXES = new String[] { "", "contrib/", "non-free/" };
        private static final String[] BASIC_SECTIONS = new String[] { "admin", "base", "comm", "devel", "doc",
                "editors", "electronics", "embedded", "games", "gnome", "graphics", "hamradio", "interpreters", "kde",
                "libs", "libdevel", "mail", "math", "misc", "net", "news", "oldlibs", "otherosfs", "perl", "python",
                "science", "shells", "sound", "tex", "text", "utils", "web", "x11" };

        private List sections = new ArrayList(PREFIXES.length * BASIC_SECTIONS.length);

        public Section() {
            for (int i = 0; i < PREFIXES.length; i++) {
                String prefix = PREFIXES[i];

                for (int j = 0; j < BASIC_SECTIONS.length; j++) {
                    String basicSection = BASIC_SECTIONS[j];

                    sections.add(prefix + basicSection);
                }
            }
        }

        public String[] getValues() {
            return (String[]) sections.toArray(new String[sections.size()]);
        }
    }

    public static class Priority extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] { "required", "important", "standard", "optional", "extra" };
        }
    }

    private File _toDir;

    private String _debFilenameProperty = "";

    private String _package;
    private String _version;
    private DebTask.Version _versionObj;
    private String _section;
    private String _priority = "extra";
    private String _architecture = "all";
    private String _depends;
    private String _preDepends;
    private String _recommends;
    private String _suggests;
    private String _enhances;
    private String _conflicts;
    private String _provides;
    private String _replaces;
    private String _maintainer;
    private URL _homepage;
    private DebTask.Maintainer _maintainerObj;
    private DebTask.Description _description;

    private List<TarFileSet> _conffiles = new ArrayList<TarFileSet>();
    private Set _changelogs = new HashSet();
    private List<TarFileSet> _data = new ArrayList<TarFileSet>();

    private File _changesIn;
    private File _changesOut;
    private File _changesSave;
    private String _compression = "gzip";
    private boolean _verbose;

    private File _tempFolder;

    private String _key;
    private File _keyring;
    private String _passphrase;
    private String _signmethod = "debsig-verify";
    private String _signrole = "origin";
    private Boolean _signpackage;

    private static final Tar.TarCompressionMethod GZIP_COMPRESSION_METHOD = new Tar.TarCompressionMethod();
    private static final Tar.TarLongFileMode GNU_LONGFILE_MODE = new Tar.TarLongFileMode();
    private final Task task = this;


    static {
        GZIP_COMPRESSION_METHOD.setValue("gzip");
        GNU_LONGFILE_MODE.setValue(Tar.TarLongFileMode.GNU);
    }

    public void setToDir(File toDir) {
        _toDir = toDir;
    }

    public void setDebFilenameProperty(String debFilenameProperty) {
        _debFilenameProperty = debFilenameProperty.trim();
    }

    public void setPackage(String packageName) {
        if (!PACKAGE_NAME_PATTERN.matcher(packageName).matches())
            throw new BuildException("Invalid package name!");

        _package = packageName;
    }

    public void setVersion(String version) {
        _version = version;
    }

    public void setVerbose(Boolean verbose) {
        _verbose = verbose;
    }
    
    public void setSection(Section section) {
        _section = section.getValue();
    }

    public void setPriority(Priority priority) {
        _priority = priority.getValue();
    }

    public void setArchitecture(String architecture) {
        _architecture = sanitize(architecture, _architecture);
    }

    public void setDepends(String depends) {
        _depends = sanitize(depends);
    }

    public void setPreDepends(String preDepends) {
        _preDepends = sanitize(preDepends);
    }

    public void setRecommends(String recommends) {
        _recommends = sanitize(recommends);
    }

    public void setSuggests(String suggests) {
        _suggests = sanitize(suggests);
    }

    public void setEnhances(String enhances) {
        _enhances = sanitize(enhances);
    }

    public void setConflicts(String conflicts) {
        _conflicts = sanitize(conflicts);
    }

    public void setProvides(String provides) {
        _provides = sanitize(provides);
    }

    public void setReplaces(String replaces) {
        _replaces = sanitize(replaces);
    }

    public void setMaintainer(String maintainer) {
        _maintainer = sanitize(maintainer);
    }

    public void setHomepage(String homepage) {
        try {
            _homepage = new URL(homepage);
        } catch (MalformedURLException e) {
            throw new BuildException("Invalid homepage, must be a URL: " + homepage, e);
        }
    }

    public void setKey(String key) {
        _key = key;
    }

    public void setKeyring(File keyring) {
        _keyring = keyring;
    }

    public void setPassphrase(String passphrase) {
        _passphrase = passphrase;
    }

    public void setSignPackage(Boolean value) {
        _signpackage = value;
    }
    
    public void setSignMethod(String method) {
        _signmethod = method;
    }
    
    public void setSignRole(String role) {
        _signrole = role;
    }
    
    public void setChangesIn( File changes ) {
        _changesIn = changes;
    }

    public void setChangesOut( File changes ) {
        _changesOut = changes;
    }

    public void setChangesSave( File changes ) {
        _changesSave = changes;
    }

    public void addConfFiles(TarFileSet conffiles) {
        _conffiles.add(conffiles);
        _data.add(conffiles);
    }

    public void addChangelog(DebTask.Changelog changelog) {
        _changelogs.add(changelog);
    }

    public void addDescription(DebTask.Description description) {
        _description = description;
    }

    public void addTarFileSet(Tar.TarFileSet fileset) {
        _data.add(fileset);
    }

    public void add(TarFileSet resourceCollection) {
        _data.add(resourceCollection);
    }

    public void addVersion(DebTask.Version version) {
        _versionObj = version;
    }

    public void addMaintainer(DebTask.Maintainer maintainer) {
        _maintainerObj = maintainer;
    }

    private void writeControlFile(File controlFile) throws FileNotFoundException {
        log("Generating control file to: " + controlFile.getAbsolutePath(), Project.MSG_VERBOSE);

        PrintWriter control = new UnixPrintWriter(controlFile);

        control.print("Package: ");
        control.println(_package);

        control.print("Version: ");
        control.println(_version);

        if (_section != null) {
            control.print("Section: ");
            control.println(_section);
        }

        if (_priority != null) {
            control.print("Priority: ");
            control.println(_priority);
        }

        control.print("Architecture: ");
        control.println(_architecture);

        if (_depends != null) {
            control.print("Depends: ");
            control.println(_depends);
        }

        if (_preDepends != null) {
            control.print("Pre-Depends: ");
            control.println(_preDepends);
        }

        if (_recommends != null) {
            control.print("Recommends: ");
            control.println(_recommends);
        }

        if (_suggests != null) {
            control.print("Suggests: ");
            control.println(_suggests);
        }

        if (_enhances != null) {
            control.print("Enhances: ");
            control.println(_enhances);
        }

        if (_conflicts != null) {
            control.print("Conflicts: ");
            control.println(_conflicts);
        }

        if (_provides != null) {
            control.print("Provides: ");
            control.println(_provides);
        }

        if (_replaces != null) {
            control.print("Replaces: ");
            control.println(_replaces);
        }

        control.print("Maintainer: ");
        control.println(_maintainer);

        if (_homepage != null) {
            control.print("Homepage: ");
            control.println(_homepage.toExternalForm());
        }

        control.print("Description: ");
        control.println(_description.getSynopsis());
        control.println(_description.getExtendedFormatted());

        control.close();
    }

    public void execute() throws BuildException {
        try {
            Console console = new Console() {
                @Override
                public void debug(String message) {
                    if (_verbose) {
                        task.log(message);
                    }
                }

                @Override
                public void info(String message) {
                    task.log(message);
                }

                @Override
                public void warn(String message) {
                    task.log(message, Project.MSG_WARN);
                }
            };

            if (_versionObj != null)
                _version = _versionObj.toString();

            if (_maintainerObj != null)
                _maintainer = _maintainerObj.toString();

            _tempFolder = createTempFolder();

            String debFileName = _package + "_" + _version + "_" + _architecture + ".deb";
            File debFile = new File(_toDir, debFileName);

            File controlFile = new File(_tempFolder, "control");
            writeControlFile(controlFile);

            log("Writing deb file to: " + debFile.getAbsolutePath());

            boolean doSign = _keyring != null && _key != null && _passphrase != null
                    && _keyring.length() > 0 && _key.length() > 0 && _passphrase.length() > 0;
            for(String key : getRuntimeConfigurableWrapper().getAttributeMap().keySet()){
                if(key.toLowerCase() == "signpackage"){
                    doSign = Boolean.valueOf(getRuntimeConfigurableWrapper().getAttributeMap().get(key).toString());
                }
            }
            if(doSign){
                log("signing - key: "+ _key + " role: " + _signrole + " method:"+_signmethod);
            }
            
            Collection<DataProducer> dataProducers = new ArrayList<DataProducer>();
            Collection<DataProducer> conffilesProducers = new ArrayList<DataProducer>();
            Iterator<?> filesets = _data.iterator();
            while (filesets.hasNext()) {
                dataProducers.add(new DataProducerFileSet((TarFileSet) filesets.next()));
            }
            // validate the type of the <data> elements
            for (DataProducer dataProducer : dataProducers) {
                if (dataProducer instanceof Data) {
                    Data data = (Data) dataProducer;
                    if (data.getType() == null) {
                        throw new BuildException(
                                "The type of the data element wasn't specified (expected 'file', 'directory' or 'archive')");
                    } else if (!Arrays.asList("file", "directory", "archive").contains(data.getType().toLowerCase())) {
                        throw new BuildException("The type '" + data.getType()
                                + "' of the data element is unknown (expected 'file', 'directory' or 'archive')");
                    }
                    if (data.getConffile() != null && data.getConffile()) {
                        conffilesProducers.add(dataProducer);
                    }
                }
            }
            DebMaker debMaker = new DebMaker(console, dataProducers, conffilesProducers);
            debMaker.setDeb(debFile);
            debMaker.setControl(_tempFolder);
            debMaker.setChangesIn(_changesIn);
            debMaker.setChangesOut(_changesOut);
            debMaker.setChangesSave(_changesSave);
            debMaker.setCompression(_compression);
            if(doSign) {
                debMaker.setKeyring(_keyring);
                debMaker.setKey(_key);
                debMaker.setPassphrase(_passphrase);
                debMaker.setSignPackage(doSign);
                debMaker.setSignMethod(_signmethod);
                debMaker.setSignRole(_signrole);
            }

            try {
                debMaker.validate();
                debMaker.makeDeb();

            } catch (PackagingException e) {
                log("Failed to create the Debian package " + debFile, e, Project.MSG_ERR);
                throw new BuildException("Failed to create the Debian package " + debFile, e);
            }

            if (_debFilenameProperty.length() > 0)
                getProject().setProperty(_debFilenameProperty, debFile.getAbsolutePath());

            deleteFileCheck(controlFile);
            deleteFolderCheck(_tempFolder);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private File createTempFolder() throws IOException {
        File tempFile = File.createTempFile("deb", ".dir");
        String tempFolderName = tempFile.getAbsolutePath();
        deleteFileCheck(tempFile);

        tempFile = new File(tempFolderName, "removeme");

        if (!tempFile.mkdirs())
            throw new IOException("Cannot create folder(s): " + tempFile.getAbsolutePath());

        deleteFileCheck(tempFile);

        log("Temp folder: " + tempFolderName, Project.MSG_VERBOSE);

        return new File(tempFolderName);
    }

    private boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] children = folder.listFiles();
            for (int i = 0; i < children.length; i++) {
                if (!deleteFolder(children[i]))
                    return false;
            }
        }

        return folder.delete();
    }

    private void deleteFolderCheck(File folder) throws IOException {
        if (!deleteFolder(folder))
            throw new IOException("Cannot delete file: " + folder.getAbsolutePath());
    }

    private void deleteFileCheck(File file) throws IOException {
        if (!file.delete())
            throw new IOException("Cannot delete file: " + file.getAbsolutePath());
    }

    private String sanitize(String value) {
        return sanitize(value, null);
    }

    private String sanitize(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        value = value.trim();

        return value.length() == 0 ? defaultValue : value;
    }

    public class UnixPrintWriter extends PrintWriter {
        public UnixPrintWriter(File file) throws FileNotFoundException {
            super(new FileOutputStream(file));
        }

        public UnixPrintWriter(Writer writer) {
            super(writer);
        }

        public UnixPrintWriter(Writer writer, boolean b) {
            super(writer, b);
        }

        public UnixPrintWriter(OutputStream outputStream) {
            super(outputStream);
        }

        public UnixPrintWriter(OutputStream outputStream, boolean b) {
            super(outputStream, b);
        }

        public void println() {
            print('\n');
        }
    }
}