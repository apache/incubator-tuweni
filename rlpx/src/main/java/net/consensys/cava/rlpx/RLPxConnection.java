/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.rlpx;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.bytes.MutableBytes;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlpx.wire.HelloMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.xerial.snappy.Snappy;

/**
 * Connection between 2 peers over the RLPx protocol.
 * <p>
 * The RLPx protocol creates a exchange of unique secrets during an initial handshake. The peers proceed to communicate
 * using the shared secrets.
 * <p>
 * This connection allows encrypting and decrypting messages with a remote peer.
 */
public final class RLPxConnection {

  /**
   * Checks if two RLPx connections represent both ends of a connection.
   * <p>
   * Used for testing.
   * 
   * @param one one RLPx connection
   * @param other an other RLPx connection
   * @return true if both connections refer to the same peers, on each side of the connection
   */
  public static boolean isComplementedBy(RLPxConnection one, RLPxConnection other) {
    return Objects.equals(one.aesSecret, other.aesSecret)
        && Objects.equals(one.macSecret, other.macSecret)
        && Objects.equals(one.token, other.token)
        && Objects.equals(snapshot(one.egressMac), snapshot(other.ingressMac))
        && Objects.equals(snapshot(one.ingressMac), snapshot(other.egressMac));
  }

  private static Bytes32 snapshot(KeccakDigest digest) {
    byte[] out = new byte[32];
    new KeccakDigest(digest).doFinal(out, 0);
    return Bytes32.wrap(out);
  }

  private final Bytes32 aesSecret;
  private final Bytes32 macSecret;
  private final Bytes32 token;
  private final KeccakDigest egressMac = new KeccakDigest(Bytes32.SIZE * 8);
  private final KeccakDigest ingressMac = new KeccakDigest(Bytes32.SIZE * 8);
  private final SECP256K1.PublicKey publicKey;
  private final SECP256K1.PublicKey peerPublicKey;
  private final AESEngine macEncryptionEngine;

  private boolean applySnappyCompression = false;
  private Bytes buffer = Bytes.EMPTY;

  RLPxConnection(
      Bytes32 aesSecret,
      Bytes32 macSecret,
      Bytes32 token,
      Bytes egressMac,
      Bytes ingressMac,
      SECP256K1.PublicKey publicKey,
      SECP256K1.PublicKey peerPublicKey) {
    this.aesSecret = aesSecret;
    this.macSecret = macSecret;
    this.token = token;

    KeyParameter macKey = new KeyParameter(macSecret.toArrayUnsafe());
    macEncryptionEngine = new AESEngine();
    macEncryptionEngine.init(true, macKey);

    updateEgress(egressMac);
    updateIngress(ingressMac);
    this.publicKey = publicKey;
    this.peerPublicKey = peerPublicKey;
  }

  /**
   *
   * @return our public key associated with this connection
   */
  public SECP256K1.PublicKey publicKey() {
    return publicKey;
  }

  /**
   *
   * @return the public key of the peer associated with this connection
   */
  public SECP256K1.PublicKey peerPublicKey() {
    return peerPublicKey;
  }

  public void configureAfterHandshake(HelloMessage helloMessage) {
    this.applySnappyCompression = helloMessage.p2pVersion() >= 5;
  }

  public synchronized void stream(Bytes newBytes, Consumer<RLPxMessage> messageConsumer) {
    buffer = Bytes.concatenate(buffer, newBytes);
    RLPxMessage message = null;
    do {
      message = readFrame(buffer);
      if (message != null) {
        buffer = buffer.slice(message.bytesLength());
        messageConsumer.accept(message);
      }
    } while (buffer.size() != 0 && message != null);
  }

  public RLPxMessage readFrame(Bytes messageFrame) {
    if (messageFrame.size() < 32) {
      return null;
    }

    KeyParameter aesKey = new KeyParameter(aesSecret.toArrayUnsafe());

    byte[] IV = new byte[16];
    Arrays.fill(IV, (byte) 0);

    SICBlockCipher decryptionCipher = new SICBlockCipher(new AESEngine());
    decryptionCipher.init(false, new ParametersWithIV(aesKey, IV));

    Bytes macBytes = messageFrame.slice(16, 16);
    Bytes headerBytes = messageFrame.slice(0, 16);

    Bytes decryptedHeader = Bytes.wrap(new byte[16]);
    decryptionCipher.processBytes(headerBytes.toArrayUnsafe(), 0, 16, decryptedHeader.toArrayUnsafe(), 0);
    int frameSize = decryptedHeader.get(0) & 0xff;
    frameSize = (frameSize << 8) + (decryptedHeader.get(1) & 0xff);
    frameSize = (frameSize << 8) + (decryptedHeader.get(2) & 0xff);
    int pad = frameSize % 16 == 0 ? 0 : 16 - frameSize % 16;

    if (messageFrame.size() < 32 + frameSize + pad + 16) {
      return null;
    }

    Bytes expectedMac = calculateMac(headerBytes, true);

    if (!macBytes.equals(expectedMac)) {
      throw new InvalidMACException(
          String.format(
              "Header MAC did not match expected MAC; expected: %s, received: %s",
              expectedMac.toHexString(),
              macBytes.toHexString()));
    }

    Bytes frameData = messageFrame.slice(32, frameSize);
    Bytes frameMac = messageFrame.slice(32 + frameSize + pad, 16);

    Bytes newFrameMac = Bytes.wrap(new byte[16]);
    Bytes frameMacSeed = updateIngress(messageFrame.slice(32, frameSize + pad));
    macEncryptionEngine.processBlock(frameMacSeed.toArrayUnsafe(), 0, newFrameMac.toArrayUnsafe(), 0);
    Bytes expectedFrameMac = updateIngress(newFrameMac.xor(frameMacSeed.slice(0, 16))).slice(0, 16);
    if (!expectedFrameMac.equals(frameMac)) {
      throw new InvalidMACException(
          String.format(
              "Frame MAC did not match expected MAC; expected: %s, received: %s",
              expectedFrameMac.toHexString(),
              frameMac.toHexString()));
    }

    Bytes decryptedFrameData = Bytes.wrap(new byte[frameData.size()]);
    decryptionCipher
        .processBytes(frameData.toArrayUnsafe(), 0, frameData.size(), decryptedFrameData.toArrayUnsafe(), 0);

    int messageType = RLP.decodeInt(decryptedFrameData.slice(0, 1));

    Bytes messageData = decryptedFrameData.slice(1);
    if (applySnappyCompression) {
      try {
        messageData = Bytes.wrap(Snappy.uncompress(messageData.toArrayUnsafe()));
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    return new RLPxMessage(messageType, messageData, 32 + frameSize + pad + 16);
  }

  /**
   * Frames a message for sending to an RLPx peer, encrypting it and calculating the appropriate MACs.
   *
   * @param message The message to frame.
   * @return The framed message, as byte buffer.
   */
  public Bytes write(RLPxMessage message) {
    KeyParameter aesKey = new KeyParameter(aesSecret.toArrayUnsafe());

    byte[] IV = new byte[16];
    Arrays.fill(IV, (byte) 0);

    SICBlockCipher encryptionCipher = new SICBlockCipher(new AESEngine());
    encryptionCipher.init(true, new ParametersWithIV(aesKey, IV));

    // Compress message
    Bytes messageData = message.content();
    if (applySnappyCompression) {
      try {
        messageData = Bytes.wrap(Snappy.compress(messageData.toArrayUnsafe()));
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    int frameSize = messageData.size() + 1;
    int pad = frameSize % 16 == 0 ? 0 : 16 - frameSize % 16;

    // Generate the header data.
    MutableBytes frameSizeBytes = MutableBytes.create(3);
    frameSizeBytes.set(0, (byte) ((frameSize >> 16) & 0xff));
    frameSizeBytes.set(1, (byte) ((frameSize >> 8) & 0xff));
    frameSizeBytes.set(2, (byte) (frameSize & 0xff));
    Bytes protocolHeader = RLP.encodeList(writer -> {
      writer.writeValue(Bytes.EMPTY);
      writer.writeValue(Bytes.EMPTY);
    });
    byte[] zeros = new byte[16 - frameSizeBytes.size() - protocolHeader.size()];
    Arrays.fill(zeros, (byte) 0x00);
    Bytes headerBytes = Bytes.concatenate(frameSizeBytes, protocolHeader, Bytes.wrap(zeros));
    Bytes encryptedHeaderBytes = Bytes.wrap(new byte[16]);
    encryptionCipher.processBytes(headerBytes.toArrayUnsafe(), 0, 16, encryptedHeaderBytes.toArrayUnsafe(), 0);
    Bytes headerMac = calculateMac(encryptedHeaderBytes, false);

    Bytes idBytes = RLP.encodeInt(message.messageId());
    assert idBytes.size() == 1;

    Bytes encryptedPayload = Bytes.wrap(new byte[idBytes.size() + messageData.size() + pad]);
    encryptionCipher.processBytes(
        Bytes.concatenate(idBytes, messageData, Bytes.wrap(new byte[pad])).toArrayUnsafe(),
        0,
        encryptedPayload.size(),
        encryptedPayload.toArrayUnsafe(),
        0);

    // Calculate the frame MAC.
    Bytes payloadMacSeed = updateEgress(encryptedPayload).slice(0, 16);
    Bytes payloadMac = Bytes.wrap(new byte[16]);
    macEncryptionEngine.processBlock(payloadMacSeed.toArrayUnsafe(), 0, payloadMac.toArrayUnsafe(), 0);
    payloadMac = updateEgress(payloadMacSeed.xor(payloadMac)).slice(0, 16);

    Bytes finalBytes = Bytes.concatenate(encryptedHeaderBytes, headerMac, encryptedPayload, payloadMac);
    return finalBytes;
  }

  private Bytes calculateMac(Bytes input, boolean ingress) {
    Bytes mac = Bytes.wrap(new byte[16]);
    macEncryptionEngine.processBlock(
        snapshot(ingress ? ingressMac : egressMac).slice(0, 16).toArrayUnsafe(),
        0,
        mac.toArrayUnsafe(),
        0);
    mac = mac.xor(input);
    if (ingress) {
      mac = updateIngress(mac).slice(0, 16);
    } else {
      mac = updateEgress(mac).slice(0, 16);
    }
    return mac.slice(0, 16);
  }

  private Bytes32 updateEgress(Bytes bytes) {
    egressMac.update(bytes.toArrayUnsafe(), 0, bytes.size());
    return snapshot(egressMac);
  }

  private Bytes32 updateIngress(Bytes bytes) {
    ingressMac.update(bytes.toArrayUnsafe(), 0, bytes.size());
    return snapshot(ingressMac);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RLPxConnection that = (RLPxConnection) obj;
    return Objects.equals(aesSecret, that.aesSecret)
        && Objects.equals(macSecret, that.macSecret)
        && Objects.equals(token, that.token)
        && Objects.equals(snapshot(egressMac), snapshot(that.egressMac))
        && Objects.equals(snapshot(ingressMac), snapshot(that.ingressMac));
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(Objects.hashCode(aesSecret), Objects.hashCode(macSecret), Objects.hashCode(token), egressMac, ingressMac);
  }
}
