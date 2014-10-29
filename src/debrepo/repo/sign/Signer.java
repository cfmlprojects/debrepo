package debrepo.repo.sign;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Iterator;

import org.vafer.jdeb.shaded.compress.io.LineIterator;
import org.vafer.jdeb.shaded.bc.bcpg.ArmoredOutputStream;
import org.vafer.jdeb.shaded.bc.bcpg.BCPGOutputStream;
import org.vafer.jdeb.shaded.bc.openpgp.PGPException;
import org.vafer.jdeb.shaded.bc.openpgp.PGPPrivateKey;
import org.vafer.jdeb.shaded.bc.openpgp.PGPSecretKey;
import org.vafer.jdeb.shaded.bc.openpgp.PGPSecretKeyRing;
import org.vafer.jdeb.shaded.bc.openpgp.PGPSecretKeyRingCollection;
import org.vafer.jdeb.shaded.bc.openpgp.PGPSignature;
import org.vafer.jdeb.shaded.bc.openpgp.PGPSignatureGenerator;
import org.vafer.jdeb.shaded.bc.openpgp.PGPUtil;
import org.vafer.jdeb.shaded.bc.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.vafer.jdeb.shaded.bc.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.vafer.jdeb.shaded.bc.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.vafer.jdeb.utils.PGPSignatureOutputStream;

public class Signer {

    private static final byte[] EOL = "\n".getBytes(Charset.forName("UTF-8"));

    private PGPSecretKey secretKey;
    private PGPPrivateKey privateKey;

    public Signer(InputStream keyring, String keyId, String passphrase) throws IOException, PGPException {
        secretKey = getSecretKey(keyring, keyId);
        if (secretKey == null) {
            throw new PGPException(String.format("Specified key %s does not exist in key ring %s", keyId, keyring));
        }
        privateKey = secretKey
                .extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                        .build(passphrase.toCharArray()));
    }

    /**
     * Creates a clear sign signature over the input data. (Not detached)
     *
     * @param input
     *            the content to be signed
     * @param output
     *            the output destination of the signature
     */
    public void clearSign(String input, OutputStream output) throws IOException, PGPException, GeneralSecurityException {
        clearSign(new ByteArrayInputStream(input.getBytes("UTF-8")), output);
    }

    /**
     * Creates a clear sign signature over the input data. (Not detached)
     *
     * @param input
     *            the content to be signed
     * @param output
     *            the output destination of the signature
     */
    public void clearSign(InputStream input, OutputStream output) throws IOException, PGPException,
            GeneralSecurityException {
        int digest = PGPUtil.SHA1;

        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(privateKey
                .getPublicKeyPacket().getAlgorithm(), digest));
        signatureGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);

        ArmoredOutputStream armoredOutput = new ArmoredOutputStream(output);
        armoredOutput.beginClearText(digest);

        LineIterator iterator = new LineIterator(new InputStreamReader(input));

        while (iterator.hasNext()) {
            String line = iterator.nextLine();

            // trailing spaces must be removed for signature calculation (see
            // http://tools.ietf.org/html/rfc4880#section-7.1)
            byte[] data = trim(line).getBytes("UTF-8");

            armoredOutput.write(data);
            armoredOutput.write(EOL);

            signatureGenerator.update(data);
            if (iterator.hasNext()) {
                signatureGenerator.update(EOL);
            }
        }

        armoredOutput.endClearText();

        PGPSignature signature = signatureGenerator.generate();
        signature.encode(new BCPGOutputStream(armoredOutput));

        armoredOutput.close();
    }

    /**
     * Returns the secret key.
     */
    public PGPSecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * Returns the private key.
     */
    public PGPPrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Returns the secret key matching the specified identifier.
     * 
     * @param input
     *            the input stream containing the keyring collection
     * @param keyId
     *            the 4 bytes identifier of the key
     */
    @SuppressWarnings("rawtypes")
    private PGPSecretKey getSecretKey(InputStream input, String keyId) throws IOException, PGPException {
        PGPSecretKeyRingCollection keyrings = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(input));

        Iterator rIt = keyrings.getKeyRings();

        while (rIt.hasNext()) {
            PGPSecretKeyRing kRing = (PGPSecretKeyRing) rIt.next();
            Iterator kIt = kRing.getSecretKeys();

            while (kIt.hasNext()) {
                PGPSecretKey key = (PGPSecretKey) kIt.next();

                if (key.isSigningKey()
                        && String.format("%08x", key.getKeyID() & 0xFFFFFFFFL).equals(keyId.toLowerCase())) {
                    return key;
                }
            }
        }

        return null;
    }

    public String signData(String data) {
        int digest = PGPUtil.SHA1;
        String signed = null;
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(
                getSecretKey().getPublicKey().getAlgorithm(), digest));
        try {
            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, getPrivateKey());
            PGPSignatureOutputStream sigStream = new PGPSignatureOutputStream(signatureGenerator);
            sigStream.write(data.getBytes());
            signed = sigStream.generateASCIISignature();
            sigStream.close();
        } catch (PGPException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return signed;
    }

    public void signData(String string, OutputStream out) {
        try {
            out.write(signData(string).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Trim the trailing spaces.
     * 
     * @param line
     */
    private String trim(String line) {
        char[] chars = line.toCharArray();
        int len = chars.length;

        while (len > 0) {
            if (!Character.isWhitespace(chars[len - 1])) {
                break;
            }
            len--;
        }

        return line.substring(0, len);
    }

}
