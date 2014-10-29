package debrepo.repo.release;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Release {
    String date;
    String origin;
    String label;
    String suite;
    String version;
    String codename;
    String architectures;
    String components;
    String description;
    List<ReleaseInfo> infos = new ArrayList<ReleaseInfo>();

    public Release() {
    }

    public void setDate(String date) {
        this.date = date;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public void setArchitectures(String architectures) {
        this.architectures = architectures;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addInfo(ReleaseInfo info) {
        infos.add(info);
    }

    @Override
    public String toString() {
        if(this.date == null) {
            SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH);
            dateFormat.applyPattern("EEE, d MMM yyyy HH:mm:ss z");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.date = dateFormat.format(new Date());
        }
        StringBuffer b = new StringBuffer();
        b.append("Date: " + date + "\n");
        if(origin != null) {
            b.append("Origin: " + origin + "\n");
        }
        if(label != null) {
            b.append("Label: " + label + "\n");
        }
        if(suite != null) {
            b.append("Suite: " + suite + "\n");
        }
        if(version != null) {
            b.append("Version: " + version + "\n");
        }
        if(codename != null) {
            b.append("Codename: " + codename + "\n");
        }
        if(architectures != null) {
            b.append("Architectures: " + architectures + "\n");
        }
        if(components != null) {
            b.append("Components: " + components + "\n");
        }
        if(description != null) {
            b.append("Description: " + description + "\n");
        }
        b.append("MD5Sum:\n");
        for (ReleaseInfo info : infos) {
            b.append(String.format(" %s  %s %s\n", info.getMd5hash(), info.getSize(), info.getName()));
        }
        b.append("SHA1:\n");
        for (ReleaseInfo info : infos) {
            b.append(String.format(" %s  %s %s\n", info.getSha1hash(), info.getSize(), info.getName()));
        }
        b.append("SHA256:\n");
        for (ReleaseInfo info : infos) {
            b.append(String.format(" %s  %s %s\n", info.getSha256hash(), info.getSize(), info.getName()));
        }
        return b.toString();
    }

}
