/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2014-2016 the libsecp256k1 contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.crypto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.StandardOpenOption.READ;
import static org.apache.tuweni.crypto.Hash.keccak256;
import static org.apache.tuweni.crypto.SECP256K1.Parameters.CURVE;
import static org.apache.tuweni.io.file.Files.atomicReplace;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.security.auth.Destroyable;

import com.google.common.base.Objects;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;

/*
 * Adapted from the BitcoinJ ECKey (Apache 2 License) implementation:
 * https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/core/ECKey.java
 *
 */

/**
 * An Elliptic Curve Digital Signature using parameters as used by Bitcoin, and defined in Standards for Efficient
 * Cryptography (SEC) (Certicom Research, http://www.secg.org/sec2-v2.pdf).
 *
 * <p>
 * This class depends upon the BouncyCastle library being available and added as a {@link java.security.Provider}. See
 * https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation.
 *
 * <p>
 * BouncyCastle can be included using the gradle dependency 'org.bouncycastle:bcprov-jdk15on'.
 */
public final class SECP256K1 {
  private SECP256K1() {}

  private static final String ALGORITHM = "ECDSA";
  private static final String CURVE_NAME = "secp256k1";
  private static final String PROVIDER = "BC";

  // Lazily initialize parameters by using java initialization on demand
  public static final class Parameters {
    public static final ECDomainParameters CURVE;
    static final BigInteger CURVE_ORDER;
    static final BigInteger HALF_CURVE_ORDER;
    static final KeyPairGenerator KEY_PAIR_GENERATOR;
    static final X9IntegerConverter X_9_INTEGER_CONVERTER;

    static {
      try {
        Class.forName("org.bouncycastle.asn1.sec.SECNamedCurves");
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            "BouncyCastle is not available on the classpath, see https://www.bouncycastle.org/latest_releases.html");
      }
      X9ECParameters params = SECNamedCurves.getByName(CURVE_NAME);
      CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
      CURVE_ORDER = CURVE.getN();
      HALF_CURVE_ORDER = CURVE_ORDER.shiftRight(1);
      if (CURVE_ORDER.compareTo(SecP256K1Curve.q) >= 0) {
        throw new IllegalStateException("secp256k1.n should be smaller than secp256k1.q, but is not");
      }
      try {
        KEY_PAIR_GENERATOR = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
      } catch (NoSuchProviderException e) {
        throw new IllegalStateException(
            "BouncyCastleProvider is not available, see https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation",
            e);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("Algorithm should be available but was not", e);
      }
      ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(CURVE_NAME);
      try {
        KEY_PAIR_GENERATOR.initialize(ecGenParameterSpec, new SecureRandom());
      } catch (InvalidAlgorithmParameterException e) {
        throw new IllegalStateException("Algorithm parameter should be available but was not", e);
      }

      X_9_INTEGER_CONVERTER = new X9IntegerConverter();
    }
  }

  // Decompress a compressed public key (x co-ord and low-bit of y-coord).
  @Nullable
  private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
    byte[] compEnc = Parameters.X_9_INTEGER_CONVERTER
        .integerToBytes(xBN, 1 + Parameters.X_9_INTEGER_CONVERTER.getByteLength(Parameters.CURVE.getCurve()));
    compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
    try {
      return Parameters.CURVE.getCurve().decodePoint(compEnc);
    } catch (IllegalArgumentException e) {
      // the compressed key was invalid
      return null;
    }
  }

  /**
   * Given the components of a signature and a selector value, recover and return the public key that generated the
   * signature according to the algorithm in SEC1v2 section 4.1.6.
   *
   * <p>
   * The recovery id is an index from 0 to 3 which indicates which of the 4 possible keys is the correct one. Because
   * the key recovery operation yields multiple potential keys, the correct key must either be stored alongside the
   * signature, or you must be willing to try each recovery id in turn until you find one that outputs the key you are
   * expecting.
   *
   * <p>
   * If this method returns null it means recovery was not possible and recovery id should be iterated.
   *
   * <p>
   * Given the above two points, a correct usage of this method is inside a for loop from 0 to 3, and if the output is
   * null OR a key that is not the one you expect, you try again with the next recovery id.
   *
   * @param v Which possible key to recover.
   * @param r The R component of the signature.
   * @param s The S component of the signature.
   * @param messageHash Hash of the data that was signed.
   * @return A ECKey containing only the public part, or {@code null} if recovery wasn't possible.
   */
  @Nullable
  private static BigInteger recoverFromSignature(int v, BigInteger r, BigInteger s, Bytes32 messageHash) {
    assert (v == 0 || v == 1);
    assert (r.signum() >= 0);
    assert (s.signum() >= 0);
    assert (messageHash != null);

    // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
    // So it's encoded in the recovery id (v).
    ECPoint R = decompressKey(r, (v & 1) == 1);
    // 1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
    if (R == null || !R.multiply(Parameters.CURVE_ORDER).isInfinity()) {
      return null;
    }

    // 1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
    BigInteger e = messageHash.toUnsignedBigInteger();
    // 1.6. For k from 1 to 2 do the following. (loop is outside this function via iterating v)
    // 1.6.1. Compute a candidate public key as:
    //   Q = mi(r) * (sR - eG)
    //
    // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
    //   Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
    // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
    // In the above equation ** is point multiplication and + is point addition (the EC group
    // operator).
    //
    // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
    // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
    BigInteger eInv = BigInteger.ZERO.subtract(e).mod(Parameters.CURVE_ORDER);
    BigInteger rInv = r.modInverse(Parameters.CURVE_ORDER);
    BigInteger srInv = rInv.multiply(s).mod(Parameters.CURVE_ORDER);
    BigInteger eInvrInv = rInv.multiply(eInv).mod(Parameters.CURVE_ORDER);
    ECPoint q = ECAlgorithms.sumOfTwoMultiplies(Parameters.CURVE.getG(), eInvrInv, R, srInv);

    if (q.isInfinity()) {
      return null;
    }

    byte[] qBytes = q.getEncoded(false);
    // We remove the prefix
    return new BigInteger(1, Arrays.copyOfRange(qBytes, 1, qBytes.length));
  }

  /**
   * Generates an ECDSA signature.
   *
   * @param data The data to sign.
   * @param keyPair The keypair to sign using.
   * @return The signature.
   */
  public static Signature sign(byte[] data, KeyPair keyPair) {
    return signHashed(keccak256(data), keyPair);
  }

  /**
   * Generates an ECDSA signature.
   *
   * @param data The data to sign.
   * @param keyPair The keypair to sign using.
   * @return The signature.
   */
  public static Signature sign(Bytes data, KeyPair keyPair) {
    return signHashed(keccak256(data), keyPair);
  }

  /**
   * Generates an ECDSA signature.
   *
   * @param hash The keccak256 hash of the data to sign.
   * @param keyPair The keypair to sign using.
   * @return The signature.
   */
  public static Signature signHashed(byte[] hash, KeyPair keyPair) {
    return signHashed(Bytes32.wrap(hash), keyPair);
  }

  /**
   * Generates an ECDSA signature.
   *
   * @param hash The keccak256 hash of the data to sign.
   * @param keyPair The keypair to sign using.
   * @return The signature.
   */
  public static Signature signHashed(Bytes32 hash, KeyPair keyPair) {
    ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));

    ECPrivateKeyParameters privKey =
        new ECPrivateKeyParameters(keyPair.secretKey().bytes().toUnsignedBigInteger(), Parameters.CURVE);
    signer.init(true, privKey);

    BigInteger[] components = signer.generateSignature(hash.toArrayUnsafe());
    BigInteger r = components[0];
    BigInteger s = components[1];

    // Automatically adjust the S component to be less than or equal to half the curve
    // order, if necessary. This is required because for every signature (r,s) the signature
    // (r, -s (mod N)) is a valid signature of the same message. However, we dislike the
    // ability to modify the bits of a Bitcoin transaction after it's been signed, as that
    // violates various assumed invariants. Thus in future only one of those forms will be
    // considered legal and the other will be banned.
    if (s.compareTo(Parameters.HALF_CURVE_ORDER) > 0) {
      // The order of the curve is the number of valid points that exist on that curve.
      // If S is in the upper half of the number of valid points, then bring it back to
      // the lower half. Otherwise, imagine that:
      //   N = 10
      //   s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
      //   10 - 8 == 2, giving us always the latter solution, which is canonical.
      s = Parameters.CURVE_ORDER.subtract(s);
    }

    // Now we have to work backwards to figure out the recovery id needed to recover the signature.
    // On this curve, there are only two possible values for the recovery id.
    int recId = -1;
    BigInteger publicKeyBI = keyPair.publicKey().bytes().toUnsignedBigInteger();
    for (int i = 0; i < 2; i++) {
      BigInteger k = recoverFromSignature(i, r, s, hash);
      if (k != null && k.equals(publicKeyBI)) {
        recId = i;
        break;
      }
    }
    if (recId == -1) {
      // this should never happen
      throw new RuntimeException("Unexpected error - could not construct a recoverable key.");
    }

    byte v = (byte) recId;
    return new Signature(v, r, s);
  }

  /**
   * Verifies the given ECDSA signature against the message bytes using the public key bytes.
   *
   * @param data The data to verify.
   * @param signature The signature.
   * @param publicKey The public key.
   * @return True if the verification is successful.
   */
  public static boolean verify(byte[] data, Signature signature, PublicKey publicKey) {
    return verifyHashed(keccak256(data), signature, publicKey);
  }

  /**
   * Verifies the given ECDSA signature against the message bytes using the public key bytes.
   *
   * @param data The data to verify.
   * @param signature The signature.
   * @param publicKey The public key.
   * @return True if the verification is successful.
   */
  public static boolean verify(Bytes data, Signature signature, PublicKey publicKey) {
    return verifyHashed(keccak256(data), signature, publicKey);
  }

  /**
   * Verifies the given ECDSA signature against the message bytes using the public key bytes.
   *
   * @param hash The keccak256 hash of the data to verify.
   * @param signature The signature.
   * @param publicKey The public key.
   * @return True if the verification is successful.
   */
  public static boolean verifyHashed(Bytes32 hash, Signature signature, PublicKey publicKey) {
    return verifyHashed(hash.toArrayUnsafe(), signature, publicKey);
  }

  /**
   * Verifies the given ECDSA signature against the message bytes using the public key bytes.
   *
   * @param hash The keccak256 hash of the data to verify.
   * @param signature The signature.
   * @param publicKey The public key.
   * @return True if the verification is successful.
   */
  public static boolean verifyHashed(byte[] hash, Signature signature, PublicKey publicKey) {
    ECDSASigner signer = new ECDSASigner();
    Bytes toDecode = Bytes.wrap(Bytes.of((byte) 4), publicKey.bytes());
    ECPublicKeyParameters params =
        new ECPublicKeyParameters(Parameters.CURVE.getCurve().decodePoint(toDecode.toArray()), Parameters.CURVE);
    signer.init(false, params);
    try {
      return signer.verifySignature(hash, signature.r, signature.s);
    } catch (NullPointerException e) {
      // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those signatures
      // are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
      return false;
    }
  }

  /**
   * Calculates an ECDH key agreement between the private and the public key of another party, formatted as a 32 bytes
   * array.
   *
   * @param privKey the private key
   * @param theirPubKey the public key
   * @return shared secret as 32 bytes
   */
  public static Bytes32 calculateKeyAgreement(SecretKey privKey, PublicKey theirPubKey) {
    checkArgument(privKey != null, "missing private key");
    checkArgument(theirPubKey != null, "missing remote public key");

    ECPrivateKeyParameters privKeyP =
        new ECPrivateKeyParameters(privKey.bytes().toUnsignedBigInteger(), Parameters.CURVE);
    ECPublicKeyParameters pubKeyP = new ECPublicKeyParameters(theirPubKey.asEcPoint(), Parameters.CURVE);

    ECDHBasicAgreement agreement = new ECDHBasicAgreement();
    agreement.init(privKeyP);
    return UInt256.valueOf(agreement.calculateAgreement(pubKeyP)).toBytes();
  }

  /**
   * A SECP256K1 private key.
   */
  public static class SecretKey implements Destroyable {

    private Bytes32 keyBytes;

    @Override
    protected void finalize() {
      destroy();
    }

    @Override
    public void destroy() {
      if (keyBytes != null) {
        byte[] b = keyBytes.toArrayUnsafe();
        keyBytes = null;
        Arrays.fill(b, (byte) 0);
      }
    }

    /**
     * Create the private key from a {@link BigInteger}.
     *
     * @param key The integer describing the key.
     * @return The private key.
     * @throws IllegalArgumentException If the integer would overflow 32 bytes.
     */
    public static SecretKey fromInteger(BigInteger key) {
      checkNotNull(key);
      byte[] bytes = key.toByteArray();
      int offset = 0;
      while (bytes[offset] == 0) {
        ++offset;
      }
      if ((bytes.length - offset) > Bytes32.SIZE) {
        throw new IllegalArgumentException("key integer is too large");
      }
      return fromBytes(Bytes32.leftPad(Bytes.wrap(bytes, offset, bytes.length - offset)));
    }

    /**
     * Create the private key from bytes.
     *
     * @param bytes The key bytes.
     * @return The private key.
     */
    public static SecretKey fromBytes(Bytes32 bytes) {
      return new SecretKey(bytes.copy());
    }

    /**
     * Load a private key from a file.
     *
     * @param file The file to read the key from.
     * @return The private key.
     * @throws IOException On a filesystem error.
     * @throws InvalidSEC256K1SecretKeyStoreException If the file does not contain a valid key.
     */
    public static SecretKey load(Path file) throws IOException, InvalidSEC256K1SecretKeyStoreException {
      // use buffers for all secret key data transfer, so they can be overwritten on completion
      ByteBuffer byteBuffer = ByteBuffer.allocate(65);
      CharBuffer charBuffer = CharBuffer.allocate(64);
      try {
        FileChannel channel = FileChannel.open(file, READ);
        while (byteBuffer.hasRemaining() && channel.read(byteBuffer) > 0) {
          // no body
        }
        channel.close();
        if (byteBuffer.remaining() > 1) {
          throw new InvalidSEC256K1SecretKeyStoreException();
        }
        byteBuffer.flip();
        for (int i = 0; i < 64; ++i) {
          charBuffer.put((char) byteBuffer.get());
        }
        if (byteBuffer.limit() == 65 && byteBuffer.get(64) != '\n' && byteBuffer.get(64) != '\r') {
          throw new InvalidSEC256K1SecretKeyStoreException();
        }
        charBuffer.flip();
        return SecretKey.fromBytes(Bytes32.fromHexString(charBuffer));
      } catch (IllegalArgumentException ex) {
        throw new InvalidSEC256K1SecretKeyStoreException();
      } finally {
        Arrays.fill(byteBuffer.array(), (byte) 0);
        Arrays.fill(charBuffer.array(), (char) 0);
      }
    }

    private SecretKey(Bytes32 bytes) {
      checkNotNull(bytes);
      this.keyBytes = bytes;
    }

    /**
     * Write the secret key to a file.
     *
     * @param file The file to write to.
     * @throws IOException On a filesystem error.
     */
    public void store(Path file) throws IOException {
      checkState(keyBytes != null, "SecretKey has been destroyed");
      // use buffers for all secret key data transfer, so they can be overwritten on completion
      byte[] bytes = new byte[64];
      CharBuffer hexChars = keyBytes.appendHexTo(CharBuffer.allocate(64));
      try {
        hexChars.flip();
        for (int i = 0; i < 64; ++i) {
          bytes[i] = (byte) hexChars.get();
        }
        atomicReplace(file, bytes);
      } finally {
        Arrays.fill(bytes, (byte) 0);
        Arrays.fill(hexChars.array(), (char) 0);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SecretKey)) {
        return false;
      }
      checkState(keyBytes != null, "SecretKey has been destroyed");
      SecretKey other = (SecretKey) obj;
      return this.keyBytes.equals(other.keyBytes);
    }

    @Override
    public int hashCode() {
      checkState(keyBytes != null, "SecretKey has been destroyed");
      return keyBytes.hashCode();
    }

    /**
     * @return The bytes of the key.
     */
    public Bytes32 bytes() {
      checkState(keyBytes != null, "SecretKey has been destroyed");
      return keyBytes;
    }

    /**
     * @return The bytes of the key.
     */
    public byte[] bytesArray() {
      checkState(keyBytes != null, "SecretKey has been destroyed");
      return keyBytes.toArrayUnsafe();
    }
  }

  /**
   * A SECP256K1 public key.
   */
  public static class PublicKey {

    private static final int BYTE_LENGTH = 64;

    private final Bytes keyBytes;

    /**
     * Create the public key from a secret key.
     *
     * @param secretKey The secret key.
     * @return The associated public key.
     */
    public static PublicKey fromSecretKey(SecretKey secretKey) {
      BigInteger privKey = secretKey.bytes().toUnsignedBigInteger();

      /*
       * TODO: FixedPointCombMultiplier currently doesn't support scalars longer than the group
       * order, but that could change in future versions.
       */
      if (privKey.bitLength() > Parameters.CURVE_ORDER.bitLength()) {
        privKey = privKey.mod(Parameters.CURVE_ORDER);
      }

      ECPoint point = new FixedPointCombMultiplier().multiply(Parameters.CURVE.getG(), privKey);
      return PublicKey.fromBytes(Bytes.wrap(Arrays.copyOfRange(point.getEncoded(false), 1, 65)));
    }

    private static Bytes toBytes64(byte[] backing) {
      if (backing.length == BYTE_LENGTH) {
        return Bytes.wrap(backing);
      } else if (backing.length > BYTE_LENGTH) {
        return Bytes.wrap(backing, backing.length - BYTE_LENGTH, BYTE_LENGTH);
      } else {
        MutableBytes res = MutableBytes.create(BYTE_LENGTH);
        Bytes.wrap(backing).copyTo(res, BYTE_LENGTH - backing.length);
        return res;
      }
    }

    /**
     * Create the public key from a secret key.
     *
     * @param privateKey The secret key.
     * @return The associated public key.
     */
    public static PublicKey fromInteger(BigInteger privateKey) {
      checkNotNull(privateKey);
      return fromBytes(toBytes64(privateKey.toByteArray()));
    }

    /**
     * Create the public key from bytes.
     *
     * @param bytes The key bytes.
     * @return The public key.
     */
    public static PublicKey fromBytes(Bytes bytes) {
      return new PublicKey(bytes);
    }

    /**
     * Create the public key from a hex string.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @return The public key.
     */
    public static PublicKey fromHexString(CharSequence str) {
      return new PublicKey(Bytes.fromHexString(str));
    }

    /**
     * Recover a public key using a digital signature and the data it signs.
     *
     * @param data The signed data.
     * @param signature The digital signature.
     * @return The associated public key, or {@code null} if recovery wasn't possible.
     */
    @Nullable
    public static PublicKey recoverFromSignature(byte[] data, Signature signature) {
      return recoverFromHashAndSignature(keccak256(data), signature);
    }

    /**
     * Recover a public key using a digital signature and the data it signs.
     *
     * @param data The signed data.
     * @param signature The digital signature.
     * @return The associated public key, or {@code null} if recovery wasn't possible.
     */
    @Nullable
    public static PublicKey recoverFromSignature(Bytes data, Signature signature) {
      return recoverFromHashAndSignature(keccak256(data), signature);
    }

    /**
     * Recover a public key using a digital signature and a keccak256 hash of the data it signs.
     *
     * @param hash The keccak256 hash of the signed data.
     * @param signature The digital signature.
     * @return The associated public key, or {@code null} if recovery wasn't possible.
     */
    @Nullable
    public static PublicKey recoverFromHashAndSignature(byte[] hash, Signature signature) {
      return recoverFromHashAndSignature(Bytes32.wrap(hash), signature);
    }

    /**
     * Recover a public key using a digital signature and a keccak256 hash of the data it signs.
     *
     * @param hash The keccak256 hash of the signed data.
     * @param signature The digital signature.
     * @return The associated public key, or {@code null} if recovery wasn't possible.
     */
    @Nullable
    public static PublicKey recoverFromHashAndSignature(Bytes32 hash, Signature signature) {
      BigInteger publicKeyBI = SECP256K1.recoverFromSignature(signature.v(), signature.r(), signature.s(), hash);
      return (publicKeyBI != null) ? fromInteger(publicKeyBI) : null;
    }

    private PublicKey(Bytes bytes) {
      checkNotNull(bytes);
      checkArgument(bytes.size() == BYTE_LENGTH, "Key must be %s bytes long, got %s", BYTE_LENGTH, bytes.size());
      this.keyBytes = bytes;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof PublicKey)) {
        return false;
      }

      PublicKey that = (PublicKey) other;
      return this.keyBytes.equals(that.keyBytes);
    }

    @Override
    public int hashCode() {
      return keyBytes.hashCode();
    }

    /**
     * @return The bytes of the key.
     */
    public Bytes bytes() {
      return keyBytes;
    }

    /**
     * @return The bytes of the key.
     */
    public byte[] bytesArray() {
      return keyBytes.toArrayUnsafe();
    }

    /**
     * Computes the public key as a point on the elliptic curve.
     *
     * @return the public key as a BouncyCastle elliptic curve point
     */
    public ECPoint asEcPoint() {
      // 0x04 is the prefix for uncompressed keys.
      Bytes val = Bytes.concatenate(Bytes.of(0x04), keyBytes);
      return CURVE.getCurve().decodePoint(val.toArrayUnsafe());
    }

    @Override
    public String toString() {
      return keyBytes.toString();
    }

    /**
     * @return This key represented as hexadecimal, starting with "0x".
     */
    public String toHexString() {
      return keyBytes.toHexString();
    }
  }

  /**
   * A SECP256K1 key pair.
   */
  public static class KeyPair {

    private final SecretKey secretKey;
    private final PublicKey publicKey;

    /**
     * Create a keypair from a private and public key.
     *
     * @param secretKey The private key.
     * @param publicKey The public key.
     * @return The key pair.
     */
    public static KeyPair create(SecretKey secretKey, PublicKey publicKey) {
      return new KeyPair(secretKey, publicKey);
    }

    /**
     * Create a keypair using only a private key.
     *
     * @param secretKey The private key.
     * @return The key pair.
     */
    public static KeyPair fromSecretKey(SecretKey secretKey) {
      return new KeyPair(secretKey, PublicKey.fromSecretKey(secretKey));
    }

    /**
     * Generate a new keypair.
     *
     * Entropy for the generation is drawn from {@link SecureRandom}.
     *
     * @return A new keypair.
     */
    public static KeyPair random() {
      java.security.KeyPair rawKeyPair = Parameters.KEY_PAIR_GENERATOR.generateKeyPair();
      BCECPrivateKey privateKey = (BCECPrivateKey) rawKeyPair.getPrivate();
      BCECPublicKey publicKey = (BCECPublicKey) rawKeyPair.getPublic();

      BigInteger privateKeyValue = privateKey.getD();

      // Ethereum does not use encoded public keys like bitcoin - see
      // https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm for details
      // Additionally, as the first bit is a constant prefix (0x04) we ignore this value
      byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
      BigInteger publicKeyValue = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));

      return new KeyPair(SecretKey.fromInteger(privateKeyValue), PublicKey.fromInteger(publicKeyValue));
    }

    /**
     * Load a key pair from a path.
     *
     * @param file The file containing a private key.
     * @return The key pair.
     * @throws IOException On a filesystem error.
     * @throws InvalidSEC256K1SecretKeyStoreException If the file does not contain a valid key.
     */
    public static KeyPair load(Path file) throws IOException, InvalidSEC256K1SecretKeyStoreException {
      return fromSecretKey(SecretKey.load(file));
    }

    private KeyPair(SecretKey secretKey, PublicKey publicKey) {
      checkNotNull(secretKey);
      checkNotNull(publicKey);
      this.secretKey = secretKey;
      this.publicKey = publicKey;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(secretKey, publicKey);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof KeyPair)) {
        return false;
      }

      KeyPair that = (KeyPair) other;
      return this.secretKey.equals(that.secretKey) && this.publicKey.equals(that.publicKey);
    }

    /**
     * @return The secret key.
     */
    public SecretKey secretKey() {
      return secretKey;
    }

    /**
     * @return The public key.
     */
    public PublicKey publicKey() {
      return publicKey;
    }

    /**
     * Write the key pair to a file.
     *
     * @param file The file to write to.
     * @throws IOException On a filesystem error.
     */
    public void store(Path file) throws IOException {
      secretKey.store(file);
    }
  }

  /**
   * A SECP256K1 digital signature.
   */
  public static class Signature {
    /*
     * Parameter v is the recovery id to reconstruct the public key used to create the signature. It must be in
     * the range 0 to 3 and indicates which of the 4 possible keys is the correct one. Because the key recovery
     * operation yields multiple potential keys, the correct key must either be stored alongside the signature,
     * or you must be willing to try each recovery id in turn until you find one that outputs the key you are
     * expecting.
     */
    private final byte v;
    private final BigInteger r;
    private final BigInteger s;

    /**
     * Create a signature from bytes.
     *
     * @param bytes The signature bytes.
     * @return The signature.
     */
    public static Signature fromBytes(Bytes bytes) {
      checkNotNull(bytes);
      checkArgument(bytes.size() == 65, "Signature must be 65 bytes, but got %s instead", bytes.size());
      BigInteger r = bytes.slice(0, 32).toUnsignedBigInteger();
      BigInteger s = bytes.slice(32, 32).toUnsignedBigInteger();
      return new Signature(bytes.get(64), r, s);
    }

    /**
     * Create a signature from parameters.
     *
     * @param v The v-value (recovery id).
     * @param r The r-value.
     * @param s The s-value.
     * @return The signature.
     * @throws IllegalArgumentException If any argument has an invalid range.
     */
    public static Signature create(byte v, BigInteger r, BigInteger s) {
      return new Signature(v, r, s);
    }

    Signature(byte v, BigInteger r, BigInteger s) {
      checkArgument(v == 0 || v == 1, "Invalid v-value, should be 0 or 1, got %s", v);
      checkNotNull(r);
      checkNotNull(s);
      checkArgument(
          r.compareTo(BigInteger.ONE) >= 0 && r.compareTo(Parameters.CURVE_ORDER) < 0,
          "Invalid r-value, should be >= 1 and < %s, got %s",
          Parameters.CURVE_ORDER,
          r);
      checkArgument(
          s.compareTo(BigInteger.ONE) >= 0 && s.compareTo(Parameters.CURVE_ORDER) < 0,
          "Invalid s-value, should be >= 1 and < %s, got %s",
          Parameters.CURVE_ORDER,
          s);
      this.v = v;
      this.r = r;
      this.s = s;
    }

    /**
     * @return The v-value (recovery id) of the signature.
     */
    public byte v() {
      return v;
    }

    /**
     * @return The r-value of the signature.
     */
    public BigInteger r() {
      return r;
    }

    /**
     * @return The s-value of the signature.
     */
    public BigInteger s() {
      return s;
    }

    /**
     * Check if the signature is canonical.
     *
     * Every signature (r,s) has an equivalent signature (r, -s (mod N)) that is also valid for the same message. The
     * canonical signature is considered the signature with the s-value less than or equal to half the curve order.
     *
     * @return {@code true} if this is the canonical form of the signature, and {@code false} otherwise.
     */
    public boolean isCanonical() {
      return s.compareTo(Parameters.HALF_CURVE_ORDER) <= 0;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Signature)) {
        return false;
      }

      Signature that = (Signature) other;
      return this.r.equals(that.r) && this.s.equals(that.s) && this.v == that.v;
    }

    /**
     * @return The bytes of the signature.
     */
    public Bytes bytes() {
      MutableBytes signature = MutableBytes.create(65);
      UInt256.valueOf(r).toBytes().copyTo(signature, 0);
      UInt256.valueOf(s).toBytes().copyTo(signature, 32);
      signature.set(64, v);
      return signature;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(r, s, v);
    }

    @Override
    public String toString() {
      return "Signature{" + "r=" + r + ", s=" + s + ", v=" + v + '}';
    }
  }
}
