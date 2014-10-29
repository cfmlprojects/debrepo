package debrepo.repo.release;

import debrepo.repo.utils.Hashes;
import debrepo.repo.utils.Hashes.Hash;

public class ReleaseInfo {
    private String md5hash;
    private String sha1hash;
    private String sha256hash;
    private Long size;
    private String name;

    public ReleaseInfo(String name, long size, Hashes hashes) throws RuntimeException {
        this.name = name;
        this.size = size;
        for (Hash hash : Hashes.Hash.values()) {
            switch (Hash.values()[hash.ordinal()]) {
            case MD5:
                this.setMd5hash(hashes.getMd5());
                break;
            case SHA1:
                this.setSha1hash(hashes.getSha1());
                break;
            case SHA256:
                this.setSha256hash(hashes.getSha256());
                break;
            default:
                throw new RuntimeException("unknown hash type: " + hash.toString());
            }
        }
    }

    public void setMd5hash(String md5hash) {
        this.md5hash = md5hash;
    }

    public void setSha1hash(String sha1hash) {
        this.sha1hash = sha1hash;
    }

    public void setSha256hash(String sha256hash) {
        this.sha256hash = sha256hash;
    }

    public String getMd5hash() {
        return md5hash;
    }

    public String getSha1hash() {
        return sha1hash;
    }

    public String getSha256hash() {
        return sha256hash;
    }

    public Long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }
}
