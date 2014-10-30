package debrepo.repo;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import debrepo.repo.packages.PackageEntry;
import debrepo.repo.packages.Packages;
import debrepo.repo.release.Release;
import debrepo.repo.release.ReleaseInfo;
import debrepo.repo.sign.Signer;
import debrepo.repo.utils.ControlHandler;
import debrepo.repo.utils.Hashes;
import debrepo.repo.utils.IOUtil;
import debrepo.repo.utils.Utils;

import org.vafer.jdeb.shaded.bc.openpgp.PGPException;
import org.vafer.jdeb.shaded.compress.compress.archivers.ArchiveException;
import org.vafer.jdeb.shaded.compress.compress.archivers.ArchiveInputStream;
import org.vafer.jdeb.shaded.compress.compress.archivers.ArchiveStreamFactory;
import org.vafer.jdeb.shaded.compress.compress.archivers.ar.ArArchiveEntry;
import org.vafer.jdeb.shaded.compress.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.shaded.compress.compress.utils.IOUtils;

/**
 * creates an apt repository.
 */
public class DebRepo {
    private static final String RELEASE = "Release";
    private static final String RELEASE_GPG = "Release.gpg";
    private static final String PACKAGES = "Packages";
    private static final String PACKAGES_GZ = "Packages.gz";
    private static final String FAILED_TO_CREATE_REPO = "Failed to create repo: ";
    private static final String CONTROL_FILE_NAME = "control";
    private BufferedWriter packagesWriter;
    private boolean signRelease;
    private String key;
    private File secring;
    private String passphrase;
    private File repoDir;
    private File debsDir;
    private boolean verbose;
    // repo metadata
    private String date;
    private String origin;
    private String label;
    private String suite;
    private String version;
    private String codename;
    private String architectures;
    private String components;
    private String description;

    public void execute() throws RuntimeException {
        debsDir = getDebsDir();
        repoDir = getRepoDir();
        Logger log = new Logger(verbose);
        log.info("repo dir: " + repoDir.getPath());
        if (!repoDir.exists()) {
            repoDir.mkdirs();
        }
        if (debsDir != null && debsDir != repoDir) {
            log.info("adding debs from: " + debsDir.getPath() + " to repo: " + repoDir.getAbsolutePath());
            Collection<File> debFiles = Utils.getDebFiles(debsDir);
            try {
                for (File artifact : debFiles) {
                    log.debug("deb: " + artifact);
                    IOUtil.copyFileToDirectory(artifact, repoDir);
                }
            } catch (IOException e) {
                log.error(FAILED_TO_CREATE_REPO, e);
                throw new RuntimeException(FAILED_TO_CREATE_REPO, e);
            }
        }
        Packages packages = new Packages();
        for (File file : Utils.getDebFiles(repoDir)) {
            PackageEntry packageEntry = new PackageEntry();
            packageEntry.setSize(file.length());
            packageEntry.setSha1(Utils.getDigest("SHA-1", file));
            packageEntry.setSha256(Utils.getDigest("SHA-256", file));
            packageEntry.setMd5sum(Utils.getDigest("MD5", file));
            String fileName = file.getName();
            packageEntry.setFilename(fileName);
            log.info("processing: " + fileName);
            try {
                ArchiveInputStream control_tgz;
                ArArchiveEntry entry;
                TarArchiveEntry control_entry;
                ArchiveInputStream debStream = new ArchiveStreamFactory().createArchiveInputStream("ar",
                        new FileInputStream(file));
                while ((entry = (ArArchiveEntry) debStream.getNextEntry()) != null) {
                    if (entry.getName().equals("control.tar.gz")) {
                        ControlHandler controlHandler = new ControlHandler();
                        GZIPInputStream gzipInputStream = new GZIPInputStream(debStream);
                        control_tgz = new ArchiveStreamFactory().createArchiveInputStream("tar", gzipInputStream);
                        while ((control_entry = (TarArchiveEntry) control_tgz.getNextEntry()) != null) {
                            String entryName = control_entry.getName();
                            log.debug("control entry: " + entryName);
                            if (!control_entry.isDirectory() && entryName.replace("./", "").equals(CONTROL_FILE_NAME)) {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                IOUtils.copy(control_tgz, outputStream);
                                String content_string = outputStream.toString("UTF-8");
                                outputStream.close();
                                controlHandler.setControlContent(content_string);
                                log.debug("control cont: " + outputStream.toString("utf-8"));
                                break;
                            }
                        }
                        control_tgz.close();
                        if (controlHandler.hasControlContent()) {
                            controlHandler.handle(packageEntry);
                        } else {
                            throw new RuntimeException("no control content found for: " + file.getName());
                        }
                        break;
                    }
                }
                debStream.close();
                packages.addPackageEntry(packageEntry);
            } catch (RuntimeException e) {
                String msg = FAILED_TO_CREATE_REPO + " " + file.getName();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            } catch (FileNotFoundException e) {
                String msg = FAILED_TO_CREATE_REPO + " " + file.getName();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            } catch (ArchiveException e) {
                String msg = FAILED_TO_CREATE_REPO + " " + file.getName();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            } catch (IOException e) {
                String msg = FAILED_TO_CREATE_REPO + " " + file.getName();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        try {
            Release release = new Release();
            release.setDate(date);
            release.setOrigin(origin);
            release.setLabel(label);
            release.setSuite(suite);
            release.setVersion(version);
            release.setCodename(codename);
            release.setArchitectures(architectures);
            release.setComponents(components);
            release.setDescription(description);

            // first write the plain text Packages file
            File packagesFile = new File(repoDir, PACKAGES);
            IOUtil.fileWrite(packagesFile, packages.toString());
            Hashes hashes = Utils.getDefaultDigests(packagesFile);
            ReleaseInfo pinfo = new ReleaseInfo(PACKAGES, packagesFile.length(), hashes);
            release.addInfo(pinfo);

            // next the Packages.gz file
            packagesFile = new File(repoDir, PACKAGES_GZ);
            packagesWriter = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(
                    packagesFile))));
            packagesWriter.write(packages.toString());
            hashes = Utils.getDefaultDigests(packagesFile);
            pinfo = new ReleaseInfo(PACKAGES_GZ, packagesFile.length(), hashes);
            release.addInfo(pinfo);

            // now add the Release file hash to the Release file itself
            hashes = Utils.getDefaultDigests(release.toString());
            pinfo = new ReleaseInfo("Release", release.toString().length(), hashes);
            release.addInfo(pinfo);
            final File releaseFile = new File(repoDir, RELEASE);
            IOUtil.fileWrite(releaseFile, release.toString());

            // If we should sign the package
            boolean doSign = secring != null && key != null && passphrase != null
                    && secring.length() > 0 && key.length() > 0 && passphrase.length() > 0;
            if (doSign) {
                log.info("Signing Release with key: "+ key);
                if (secring == null || !secring.exists()) {
                    doSign = false;
                    log.warn("Signing requested, but no keyring supplied");
                }
                if (key == null) {
                    doSign = false;
                    log.warn("Signing requested, but no key supplied");
                }
                if (passphrase == null) {
                    doSign = false;
                    log.warn("Signing requested, but no passphrase supplied");
                }
                FileInputStream keyRingInput = new FileInputStream(secring);
                Signer signer = null;
                final FileOutputStream releasePGPFile = new FileOutputStream(new File(repoDir, RELEASE_GPG));
                try {
                    signer = new Signer(new FileInputStream(secring), key, passphrase);
                    signer.signData(release.toString(), releasePGPFile);
                } catch (PGPException e) {
                    e.printStackTrace();
                } finally {
                    keyRingInput.close();
                    releasePGPFile.flush();
                    releasePGPFile.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("writing files failed", e);
        } finally {
            if (packagesWriter != null) {
                try {
                    packagesWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException("writing files failed", e);
                }
            }
        }
        log.info("finished processing repository:" + repoDir.getPath());
    }

    private DebRepo(RepoBuilder builder) {
        this.verbose = builder.verbose;
        this.debsDir = builder.debsDir;
        this.key = builder.key;
        this.secring = builder.secring;
        this.passphrase = builder.passphrase;
        this.repoDir = builder.repoDir;
        // repo metadata
        this.date = builder.date;
        this.origin = builder.origin;
        this.label = builder.label;
        this.suite = builder.suite;
        this.version = builder.version;
        this.codename = builder.codename;
        this.architectures = builder.architectures;
        this.components = builder.components;
        this.description = builder.description;
    }

    public static class RepoBuilder {
        private String key;
        private File secring;
        private String passphrase;
        private File repoDir;
        private File debsDir;
        private boolean verbose;
        private String date;
        private String origin;
        private String label;
        private String suite;
        private String version;
        private String codename;
        private String architectures;
        private String components;
        private String description;

        public RepoBuilder() {
        }

        public RepoBuilder(File repoDir) {
            this.debsDir = repoDir;
            this.repoDir = repoDir;
        }

        public RepoBuilder(File debsDir, File repoDir) {
            this.debsDir = debsDir;
            this.repoDir = repoDir;
        }

        public RepoBuilder key(String key) {
            this.key = key;
            return this;
        }

        public RepoBuilder secring(File keyring) {
            this.secring = keyring;
            return this;
        }

        public RepoBuilder passphrase(String passphrase) {
            this.passphrase = passphrase;
            return this;
        }

        public RepoBuilder repoDir(File repoDir) {
            this.repoDir = repoDir;
            return this;
        }

        public RepoBuilder debsDir(File debsDir) {
            this.debsDir = debsDir;
            return this;
        }

        public RepoBuilder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public RepoBuilder date(String value) {
            this.date = value;
            return this;
        }

        public RepoBuilder origin(String value) {
            this.origin = value;
            return this;
        }

        public RepoBuilder label(String value) {
            this.label = value;
            return this;
        }

        public RepoBuilder suite(String value) {
            this.suite = value;
            return this;
        }

        public RepoBuilder version(String value) {
            this.version = value;
            return this;
        }

        public RepoBuilder codename(String value) {
            this.codename = value;
            return this;
        }

        public RepoBuilder architectures(String value) {
            this.architectures = value;
            return this;
        }

        public RepoBuilder components(String value) {
            this.components = value;
            return this;
        }

        public RepoBuilder description(String value) {
            this.description = value;
            return this;
        }

        public DebRepo build() {
            return new DebRepo(this);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public File getKeyring() {
        return secring;
    }

    public void setSecRing(File keyring) {
        this.secring = keyring;
    }
    
    public File getRepoDir() {
        return repoDir;
    }

    public void setRepoDir(File repoDir) {
        this.repoDir = repoDir;
    }

    public boolean isSignRelease() {
        return signRelease;
    }

    public void setSignRelease(boolean signRelease) {
        this.signRelease = signRelease;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public File getDebsDir() {
        return debsDir;
    }

    public void setDebsDir(File debsDir) {
        this.debsDir = debsDir;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // metadata related
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

    private class Logger {
        private boolean verbose;

        Logger(boolean verbose) {
            this.verbose = verbose;
        }

        public void debug(String message) {
            if (verbose) {
                System.out.println("DEBUG: " + message);
            }
        }

        public void warn(String message) {
            System.out.println("WARN: " + message);
        }

        public void info(String message) {
            System.out.println("INFO: " + message);
        }

        public void error(String message, Exception e) {
            System.err.println(message);
            e.printStackTrace();
        }
    }

}
