package debrepo.repo.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;

import org.vafer.jdeb.shaded.bc.util.encoders.Hex;

import debrepo.repo.utils.Hashes.Hash;

public class Utils {

  /**
   * Compute the given message digest for a file.
   * 
   * @param hashType algorithm to be used (as {@code String})
   * @param file File to compute the digest for (as {@code File}).
   * @return A {@code String} for the hex encoded digest.
   * @throws RuntimeException
   */
  public static String getDigest(String hashType, File file) throws RuntimeException {
    try {
      FileInputStream fis = new FileInputStream(file);
      BufferedInputStream bis = new BufferedInputStream(fis);
      MessageDigest digest = MessageDigest.getInstance(hashType);
      DigestInputStream dis = new DigestInputStream(bis, digest);
      @SuppressWarnings("unused")
      int ch;
      while ((ch = dis.read()) != -1);
      String hex = new String(Hex.encode(digest.digest()));
      fis.close();
      bis.close();
      dis.close();
      return hex;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("could not create digest", e);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("could not create digest", e);
    } catch (IOException e) {
      throw new RuntimeException("could not create digest", e);
    }
  }

  /**
   * Compute md5, sha1, sha256 message digest for a file.
   * 
   * @param file File to compute the digest for (as {@code File}).
   * @return {@link Hashes} with the computed digests.
   * @throws RuntimeException
   */
  public static Hashes getDefaultDigests(File file) throws RuntimeException {
    Hashes h = new Hashes();
      for (Hash hash : Hashes.Hash.values()) {
        String hex = getDigest(hash.toString(), file);
        switch (Hash.values()[hash.ordinal()]) {
          case MD5:
            h.setMd5(hex);
            break;
          case SHA1:
            h.setSha1(hex);
            break;
          case SHA256:
            h.setSha256(hex);
            break;
          default:
            throw new RuntimeException("unknown hash type: " + hash.toString());
        }
      }
      return h;
  }

    public static Collection<File> getDebFiles(File debsDir) {
        Collection<File> debFiles = new ArrayList<File>();
        for(File debFile : debsDir.listFiles(new IOUtil.ExtFilter(".deb"))){
            debFiles.add(debFile);   
        }
        return debFiles;
    }

    public static Hashes getDefaultDigests(String string) {
        try {
            final File tmpFile = File.createTempFile("hash", "er");
            IOUtil.fileWrite(tmpFile, string);
            return getDefaultDigests(tmpFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
