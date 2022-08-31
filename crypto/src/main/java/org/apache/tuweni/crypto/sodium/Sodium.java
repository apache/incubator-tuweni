/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.crypto.sodium;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.byref.ByteByReference;
import jnr.ffi.byref.LongLongByReference;
import org.jetbrains.annotations.Nullable;

/**
 * Access to the sodium native library.
 *
 * <p>
 * This class provides static methods for checking or loading the sodium native library.
 */
public final class Sodium {
  private Sodium() {}

  static final SodiumVersion VERSION_10_0_11 = new SodiumVersion(9, 3, "10.0.11");
  static final SodiumVersion VERSION_10_0_12 = new SodiumVersion(9, 4, "10.0.12");
  static final SodiumVersion VERSION_10_0_13 = new SodiumVersion(9, 5, "10.0.13");
  static final SodiumVersion VERSION_10_0_14 = new SodiumVersion(9, 6, "10.0.14");
  static final SodiumVersion VERSION_10_0_15 = new SodiumVersion(10, 0, "10.0.15");
  static final SodiumVersion VERSION_10_0_16 = new SodiumVersion(10, 1, "10.0.16");
  static final SodiumVersion VERSION_10_0_17 = new SodiumVersion(10, 1, "10.0.17");
  static final SodiumVersion VERSION_10_0_18 = new SodiumVersion(10, 1, "10.0.18");

  /**
   * The minimum version of the sodium native library that this binding supports.
   *
   * @return The minimum version of the sodium native library that this binding supports.
   */
  public static SodiumVersion minSupportedVersion() {
    return VERSION_10_0_11;
  }

  /**
   * The version of the loaded sodium native library.
   *
   * @return The version of the loaded sodium library.
   */
  public static SodiumVersion version() {
    return version(libSodium());
  }

  private static SodiumVersion version(LibSodium lib) {
    return new SodiumVersion(
        lib.sodium_library_version_major(),
        lib.sodium_library_version_minor(),
        lib.sodium_version_string());
  }

  /**
   * Check if the loaded sodium native library is the same or later than the specified version.
   *
   * @param requiredVersion The version to compare to.
   * @return {@code true} if the loaded sodium native library is the same or a later version.
   */
  public static boolean supportsVersion(SodiumVersion requiredVersion) {
    return supportsVersion(requiredVersion, libSodium());
  }

  private static boolean supportsVersion(SodiumVersion requiredVersion, LibSodium lib) {
    return version(lib).compareTo(requiredVersion) >= 0;
  }

  private static final String LIBRARY_NAME;
  static {
    try {
      Class.forName("jnr.ffi.Platform");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("JNR-FFI is not available on the classpath, see https://github.com/jnr/jnr-ffi");
    }
    switch (Platform.getNativePlatform().getOS()) {
      case WINDOWS:
        LIBRARY_NAME = "libsodium";
        break;
      default:
        LIBRARY_NAME = "sodium";
        break;
    }
  }

  private static volatile LibSodium libSodium = null;

  /**
   * Load and initialize the native libsodium shared library.
   *
   * <p>
   * If this method returns successfully (without throwing a {@link LinkageError}), then all future calls to methods
   * provided by this class will use the loaded library.
   *
   * @param path The path to the shared library.
   * @throws LinkageError If the library cannot be found, dependent libraries are missing, or cannot be initialized.
   */
  public static void loadLibrary(Path path) {
    requireNonNull(path);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("Non-existent path");
    }

    Path dir = path.getParent();
    Path library = path.getFileName();

    LibSodium lib =
        LibraryLoader.create(LibSodium.class).search(dir.toFile().getAbsolutePath()).load(library.toString());
    initializeLibrary(lib);

    synchronized (LibSodium.class) {
      Sodium.libSodium = lib;
    }
  }

  /**
   * Search for, then load and initialize the native libsodium shared library.
   *
   * <p>
   * The library will be searched for in all the provided locations, using the library name {@code "sodium"}. If this
   * method returns successfully (without throwing a {@link LinkageError}), then all future calls to methods provided by
   * this class will use the loaded library.
   *
   * @param paths A set of directories to search for the library in.
   * @throws LinkageError If the library cannot be found, dependent libraries are missing, or cannot be initialized.
   */
  public static void searchLibrary(Path... paths) {
    searchLibrary(LIBRARY_NAME, paths);
  }

  /**
   * Search for, then load and initialize the native libsodium shared library.
   *
   * <p>
   * The library will be searched for in all the provided locations, using the provided library name. If this method
   * returns successfully (without throwing a {@link LinkageError}), then all future calls to methods provided by this
   * class will use the loaded library.
   *
   * @param libraryName The name of the library (e.g. {@code "sodium"}).
   * @param paths A set of directories to search for the library in.
   * @throws LinkageError If the library cannot be found, dependent libraries are missing, or cannot be initialized.
   */
  public static void searchLibrary(String libraryName, Path... paths) {
    LibraryLoader<LibSodium> loader = LibraryLoader.create(LibSodium.class);
    for (Path path : paths) {
      loader = loader.search(path.toFile().getAbsolutePath());
    }
    LibSodium lib = loader.load(libraryName);
    initializeLibrary(lib);

    synchronized (LibSodium.class) {
      Sodium.libSodium = lib;
    }
  }

  private static LibSodium libSodium() {
    if (libSodium == null) {
      synchronized (LibSodium.class) {
        if (libSodium == null) {
          LibSodium lib = LibraryLoader
              .create(LibSodium.class)
              .search("/usr/local/lib")
              .search("/opt/local/lib")
              .search("/usr/lib")
              .search("/lib")
              .load(LIBRARY_NAME);
          libSodium = initializeLibrary(lib);
        }
      }
    }
    return libSodium;
  }

  private static LibSodium initializeLibrary(LibSodium lib) {
    if (!supportsVersion(minSupportedVersion(), lib)) {
      throw new LinkageError(
          String
              .format(
                  "Unsupported libsodium version %s (%s:%s)",
                  lib.sodium_version_string(),
                  lib.sodium_library_version_major(),
                  lib.sodium_library_version_minor()));
    }
    int result = lib.sodium_init();
    if (result == -1) {
      throw new LinkageError("Failed to initialize libsodium: sodium_init returned " + result);
    }
    return lib;
  }

  /**
   * Check if the sodium library is available.
   *
   * <p>
   * If the sodium library has not already been loaded, this will attempt to load and initialize it before returning.
   *
   * @return {@code true} if the library is loaded and available.
   */
  public static boolean isAvailable() {
    try {
      libSodium();
    } catch (LinkageError e) {
      return false;
    }
    return true;
  }

  static Pointer malloc(long length) {
    Pointer ptr = sodium_malloc(length);
    if (ptr == null) {
      throw new OutOfMemoryError("Sodium.sodium_malloc failed allocating " + length);
    }
    return ptr;
  }

  static Pointer dup(Pointer src, int length) {
    Pointer ptr = malloc(length);
    try {
      ptr.transferFrom(0, src, 0, length);
      return ptr;
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static Pointer dupAndIncrement(Pointer src, int length) {
    Pointer ptr = dup(src, length);
    try {
      sodium_increment(ptr, length);
      return ptr;
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static <T> T dupAndIncrement(Pointer src, int length, BiFunction<Pointer, Integer, T> ctr) {
    Pointer ptr = Sodium.dupAndIncrement(src, length);
    try {
      return ctr.apply(ptr, length);
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static Pointer dup(byte[] bytes) {
    Pointer ptr = malloc(bytes.length);
    try {
      ptr.put(0, bytes, 0, bytes.length);
      return ptr;
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static <T> T dup(byte[] bytes, BiFunction<Pointer, Integer, T> ctr) {
    Pointer ptr = Sodium.dup(bytes);
    try {
      return ctr.apply(ptr, bytes.length);
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static byte[] reify(Pointer ptr, int length) {
    byte[] bytes = new byte[length];
    ptr.get(0, bytes, 0, bytes.length);
    return bytes;
  }

  static Pointer randomBytes(int length) {
    Pointer ptr = malloc(length);
    try {
      randombytes_buf(ptr, length);
      return ptr;
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static <T> T randomBytes(int length, BiFunction<Pointer, Integer, T> ctr) {
    Pointer ptr = Sodium.randomBytes(length);
    try {
      return ctr.apply(ptr, length);
    } catch (Throwable e) {
      sodium_free(ptr);
      throw e;
    }
  }

  static int hashCode(Pointer ptr, int length) {
    int result = 1;
    for (int i = 0; i < length; ++i) {
      result = 31 * result + ((int) ptr.getByte(i));
    }
    return result;
  }

  static <T> T scalarMultBase(Pointer n, long nlen, BiFunction<Pointer, Long, T> ctr) {
    if (nlen != Sodium.crypto_scalarmult_scalarbytes()) {
      throw new IllegalArgumentException(
          "secret key length is " + nlen + " but required " + Sodium.crypto_scalarmult_scalarbytes());
    }
    long qbytes = Sodium.crypto_scalarmult_bytes();
    Pointer dst = malloc(qbytes);
    try {
      int rc = Sodium.crypto_scalarmult_base(dst, n);
      if (rc != 0) {
        throw new SodiumException("crypto_scalarmult_base: failed with result " + rc);
      }
      return ctr.apply(dst, qbytes);
    } catch (Throwable e) {
      sodium_free(dst);
      throw e;
    }
  }

  static <T> T scalarMult(Pointer n, long nlen, Pointer p, long plen, BiFunction<Pointer, Long, T> ctr) {
    if (nlen != Sodium.crypto_scalarmult_scalarbytes()) {
      throw new IllegalArgumentException(
          "secret key length is " + nlen + " but required " + Sodium.crypto_scalarmult_scalarbytes());
    }
    if (plen != Sodium.crypto_scalarmult_bytes()) {
      throw new IllegalArgumentException(
          "public key length is " + plen + " but required " + Sodium.crypto_scalarmult_bytes());
    }
    long qbytes = Sodium.crypto_scalarmult_bytes();
    Pointer dst = malloc(qbytes);
    try {
      int rc = Sodium.crypto_scalarmult(dst, n, p);
      if (rc != 0) {
        throw new SodiumException("crypto_scalarmult_base: failed with result " + rc);
      }
      return ctr.apply(dst, qbytes);
    } catch (Throwable e) {
      sodium_free(dst);
      throw e;
    }
  }

  /////////
  // Generated with https://gist.github.com/cleishm/39fbad03378f5e1ad82521ad821cd065, then modified

  static String sodium_version_string() {
    return libSodium().sodium_version_string();
  }

  static int sodium_library_version_major() {
    return libSodium().sodium_library_version_major();
  }

  static int sodium_library_version_minor() {
    return libSodium().sodium_library_version_minor();
  }

  static int sodium_library_minimal() {
    return libSodium().sodium_library_minimal();
  }

  static int sodium_set_misuse_handler(Pointer handler) {
    return libSodium().sodium_set_misuse_handler(handler);
  }

  static void sodium_misuse() {
    libSodium().sodium_misuse();
  }

  static int crypto_aead_aes256gcm_is_available() {
    return libSodium().crypto_aead_aes256gcm_is_available();
  }

  static long crypto_aead_aes256gcm_keybytes() {
    return libSodium().crypto_aead_aes256gcm_keybytes();
  }

  static long crypto_aead_aes256gcm_nsecbytes() {
    return libSodium().crypto_aead_aes256gcm_nsecbytes();
  }

  static long crypto_aead_aes256gcm_npubbytes() {
    return libSodium().crypto_aead_aes256gcm_npubbytes();
  }

  static long crypto_aead_aes256gcm_abytes() {
    return libSodium().crypto_aead_aes256gcm_abytes();
  }

  static long crypto_aead_aes256gcm_messagebytes_max() {
    return libSodium().crypto_aead_aes256gcm_messagebytes_max();
  }

  static long crypto_aead_aes256gcm_statebytes() {
    return libSodium().crypto_aead_aes256gcm_statebytes();
  }

  static int crypto_aead_aes256gcm_encrypt(
      byte[] c,
      LongLongByReference clen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      @Nullable Pointer nsec,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_aes256gcm_encrypt(c, clen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_aes256gcm_decrypt(
      byte[] m,
      LongLongByReference mlen_p,
      @Nullable Pointer nsec,
      byte[] c,
      long clen,
      byte[] ad,
      long adlen,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_aes256gcm_decrypt(m, mlen_p, nsec, c, clen, ad, adlen, npub, k);
  }

  static int crypto_aead_aes256gcm_encrypt_detached(
      byte[] c,
      byte[] mac,
      LongLongByReference maclen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      @Nullable Pointer nsec,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_aes256gcm_encrypt_detached(c, mac, maclen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_aes256gcm_decrypt_detached(
      byte[] m,
      @Nullable Pointer nsec,
      byte[] c,
      long clen,
      byte[] mac,
      byte[] ad,
      long adlen,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_aes256gcm_decrypt_detached(m, nsec, c, clen, mac, ad, adlen, npub, k);
  }

  static int crypto_aead_aes256gcm_beforenm(Pointer ctx_, Pointer k) {
    return libSodium().crypto_aead_aes256gcm_beforenm(ctx_, k);
  }

  static int crypto_aead_aes256gcm_encrypt_afternm(
      byte[] c,
      LongLongByReference clen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      @Nullable Pointer nsec,
      Pointer npub,
      Pointer ctx_) {
    return libSodium().crypto_aead_aes256gcm_encrypt_afternm(c, clen_p, m, mlen, ad, adlen, nsec, npub, ctx_);
  }

  static int crypto_aead_aes256gcm_decrypt_afternm(
      byte[] m,
      LongLongByReference mlen_p,
      @Nullable Pointer nsec,
      byte[] c,
      long clen,
      byte[] ad,
      long adlen,
      Pointer npub,
      Pointer ctx_) {
    return libSodium().crypto_aead_aes256gcm_decrypt_afternm(m, mlen_p, nsec, c, clen, ad, adlen, npub, ctx_);
  }

  static int crypto_aead_aes256gcm_encrypt_detached_afternm(
      byte[] c,
      byte[] mac,
      LongLongByReference maclen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      @Nullable Pointer nsec,
      Pointer npub,
      Pointer ctx_) {
    return libSodium()
        .crypto_aead_aes256gcm_encrypt_detached_afternm(c, mac, maclen_p, m, mlen, ad, adlen, nsec, npub, ctx_);
  }

  static int crypto_aead_aes256gcm_decrypt_detached_afternm(
      byte[] m,
      @Nullable Pointer nsec,
      byte[] c,
      long clen,
      byte[] mac,
      byte[] ad,
      long adlen,
      Pointer npub,
      Pointer ctx_) {
    return libSodium().crypto_aead_aes256gcm_decrypt_detached_afternm(m, nsec, c, clen, mac, ad, adlen, npub, ctx_);
  }

  static void crypto_aead_aes256gcm_keygen(Pointer k) {
    libSodium().crypto_aead_aes256gcm_keygen(k);
  }

  static long crypto_aead_chacha20poly1305_ietf_keybytes() {
    return libSodium().crypto_aead_chacha20poly1305_ietf_keybytes();
  }

  static long crypto_aead_chacha20poly1305_ietf_nsecbytes() {
    return libSodium().crypto_aead_chacha20poly1305_ietf_nsecbytes();
  }

  static long crypto_aead_chacha20poly1305_ietf_npubbytes() {
    return libSodium().crypto_aead_chacha20poly1305_ietf_npubbytes();
  }

  static long crypto_aead_chacha20poly1305_ietf_abytes() {
    return libSodium().crypto_aead_chacha20poly1305_ietf_abytes();
  }

  static long crypto_aead_chacha20poly1305_ietf_messagebytes_max() {
    return libSodium().crypto_aead_chacha20poly1305_ietf_messagebytes_max();
  }

  static int crypto_aead_chacha20poly1305_ietf_encrypt(
      byte[] c,
      LongLongByReference clen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      byte[] nsec,
      byte[] npub,
      byte[] k) {
    return libSodium().crypto_aead_chacha20poly1305_ietf_encrypt(c, clen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_chacha20poly1305_ietf_decrypt(
      byte[] m,
      LongLongByReference mlen_p,
      byte[] nsec,
      byte[] c,
      long clen,
      byte[] ad,
      long adlen,
      byte[] npub,
      byte[] k) {
    return libSodium().crypto_aead_chacha20poly1305_ietf_decrypt(m, mlen_p, nsec, c, clen, ad, adlen, npub, k);
  }

  static int crypto_aead_chacha20poly1305_ietf_encrypt_detached(
      byte[] c,
      byte[] mac,
      LongLongByReference maclen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      byte[] nsec,
      byte[] npub,
      byte[] k) {
    return libSodium()
        .crypto_aead_chacha20poly1305_ietf_encrypt_detached(c, mac, maclen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_chacha20poly1305_ietf_decrypt_detached(
      byte[] m,
      byte[] nsec,
      byte[] c,
      long clen,
      byte[] mac,
      byte[] ad,
      long adlen,
      byte[] npub,
      byte[] k) {
    return libSodium().crypto_aead_chacha20poly1305_ietf_decrypt_detached(m, nsec, c, clen, mac, ad, adlen, npub, k);
  }

  static void crypto_aead_chacha20poly1305_ietf_keygen(byte[] k) {
    libSodium().crypto_aead_chacha20poly1305_ietf_keygen(k);
  }

  static long crypto_aead_chacha20poly1305_keybytes() {
    return libSodium().crypto_aead_chacha20poly1305_keybytes();
  }

  static long crypto_aead_chacha20poly1305_nsecbytes() {
    return libSodium().crypto_aead_chacha20poly1305_nsecbytes();
  }

  static long crypto_aead_chacha20poly1305_npubbytes() {
    return libSodium().crypto_aead_chacha20poly1305_npubbytes();
  }

  static long crypto_aead_chacha20poly1305_abytes() {
    return libSodium().crypto_aead_chacha20poly1305_abytes();
  }

  static long crypto_aead_chacha20poly1305_messagebytes_max() {
    return libSodium().crypto_aead_chacha20poly1305_messagebytes_max();
  }

  static int crypto_aead_chacha20poly1305_encrypt(
      byte[] c,
      LongLongByReference clen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      byte[] nsec,
      byte[] npub,
      byte[] k) {
    return libSodium().crypto_aead_chacha20poly1305_encrypt(c, clen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_chacha20poly1305_decrypt(
      byte[] m,
      LongLongByReference mlen_p,
      byte[] nsec,
      byte[] c,
      long clen,
      byte[] ad,
      long adlen,
      byte[] npub,
      byte[] k) {
    return libSodium().crypto_aead_chacha20poly1305_decrypt(m, mlen_p, nsec, c, clen, ad, adlen, npub, k);
  }

  static int crypto_aead_chacha20poly1305_encrypt_detached(
      byte[] c,
      byte[] mac,
      LongLongByReference maclen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      byte[] nsec,
      byte[] npub,
      byte[] k) {
    return libSodium()
        .crypto_aead_chacha20poly1305_encrypt_detached(c, mac, maclen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_chacha20poly1305_decrypt_detached(
      byte[] m,
      byte[] nsec,
      byte[] c,
      long clen,
      byte[] mac,
      byte[] ad,
      long adlen,
      byte[] npub,
      byte[] k) {
    return libSodium().crypto_aead_chacha20poly1305_decrypt_detached(m, nsec, c, clen, mac, ad, adlen, npub, k);
  }

  static void crypto_aead_chacha20poly1305_keygen(byte[] k) {
    libSodium().crypto_aead_chacha20poly1305_keygen(k);
  }

  static long crypto_aead_xchacha20poly1305_ietf_keybytes() {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_keybytes();
  }

  static long crypto_aead_xchacha20poly1305_ietf_nsecbytes() {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_nsecbytes();
  }

  static long crypto_aead_xchacha20poly1305_ietf_npubbytes() {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_npubbytes();
  }

  static long crypto_aead_xchacha20poly1305_ietf_abytes() {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_abytes();
  }

  static long crypto_aead_xchacha20poly1305_ietf_messagebytes_max() {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_messagebytes_max();
  }

  static int crypto_aead_xchacha20poly1305_ietf_encrypt(
      byte[] c,
      LongLongByReference clen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      @Nullable byte[] nsec,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_encrypt(c, clen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_xchacha20poly1305_ietf_decrypt(
      byte[] m,
      LongLongByReference mlen_p,
      @Nullable byte[] nsec,
      byte[] c,
      long clen,
      byte[] ad,
      long adlen,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_decrypt(m, mlen_p, nsec, c, clen, ad, adlen, npub, k);
  }

  static int crypto_aead_xchacha20poly1305_ietf_encrypt_detached(
      byte[] c,
      byte[] mac,
      LongLongByReference maclen_p,
      byte[] m,
      long mlen,
      byte[] ad,
      long adlen,
      @Nullable byte[] nsec,
      Pointer npub,
      Pointer k) {
    return libSodium()
        .crypto_aead_xchacha20poly1305_ietf_encrypt_detached(c, mac, maclen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  static int crypto_aead_xchacha20poly1305_ietf_decrypt_detached(
      byte[] m,
      @Nullable byte[] nsec,
      byte[] c,
      long clen,
      byte[] mac,
      byte[] ad,
      long adlen,
      Pointer npub,
      Pointer k) {
    return libSodium().crypto_aead_xchacha20poly1305_ietf_decrypt_detached(m, nsec, c, clen, mac, ad, adlen, npub, k);
  }

  static void crypto_aead_xchacha20poly1305_ietf_keygen(Pointer k) {
    libSodium().crypto_aead_xchacha20poly1305_ietf_keygen(k);
  }

  static long crypto_hash_sha512_statebytes() {
    return libSodium().crypto_hash_sha512_statebytes();
  }

  static long crypto_hash_sha512_bytes() {
    return libSodium().crypto_hash_sha512_bytes();
  }

  static int crypto_hash_sha512(Pointer out, Pointer in, long inlen) {
    return libSodium().crypto_hash_sha512(out, in, inlen);
  }

  static int crypto_hash_sha512_init(Pointer state) {
    return libSodium().crypto_hash_sha512_init(state);
  }

  static int crypto_hash_sha512_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_hash_sha512_update(state, in, inlen);
  }

  static int crypto_hash_sha512_final(Pointer state, byte[] out) {
    return libSodium().crypto_hash_sha512_final(state, out);
  }

  static long crypto_auth_hmacsha512_bytes() {
    return libSodium().crypto_auth_hmacsha512_bytes();
  }

  static long crypto_auth_hmacsha512_keybytes() {
    return libSodium().crypto_auth_hmacsha512_keybytes();
  }

  static int crypto_auth_hmacsha512(byte[] out, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_hmacsha512(out, in, inlen, k);
  }

  static int crypto_auth_hmacsha512_verify(byte[] h, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_hmacsha512_verify(h, in, inlen, k);
  }

  static long crypto_auth_hmacsha512_statebytes() {
    return libSodium().crypto_auth_hmacsha512_statebytes();
  }

  static int crypto_auth_hmacsha512_init(Pointer state, byte[] key, long keylen) {
    return libSodium().crypto_auth_hmacsha512_init(state, key, keylen);
  }

  static int crypto_auth_hmacsha512_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_auth_hmacsha512_update(state, in, inlen);
  }

  static int crypto_auth_hmacsha512_final(Pointer state, byte[] out) {
    return libSodium().crypto_auth_hmacsha512_final(state, out);
  }

  static void crypto_auth_hmacsha512_keygen(byte[] k) {
    libSodium().crypto_auth_hmacsha512_keygen(k);
  }

  static long crypto_auth_hmacsha512256_bytes() {
    return libSodium().crypto_auth_hmacsha512256_bytes();
  }

  static long crypto_auth_hmacsha512256_keybytes() {
    return libSodium().crypto_auth_hmacsha512256_keybytes();
  }

  static int crypto_auth_hmacsha512256(byte[] out, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_hmacsha512256(out, in, inlen, k);
  }

  static int crypto_auth_hmacsha512256_verify(byte[] h, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_hmacsha512256_verify(h, in, inlen, k);
  }

  static long crypto_auth_hmacsha512256_statebytes() {
    return libSodium().crypto_auth_hmacsha512256_statebytes();
  }

  static int crypto_auth_hmacsha512256_init(Pointer state, byte[] key, long keylen) {
    return libSodium().crypto_auth_hmacsha512256_init(state, key, keylen);
  }

  static int crypto_auth_hmacsha512256_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_auth_hmacsha512256_update(state, in, inlen);
  }

  static int crypto_auth_hmacsha512256_final(Pointer state, byte[] out) {
    return libSodium().crypto_auth_hmacsha512256_final(state, out);
  }

  static void crypto_auth_hmacsha512256_keygen(byte[] k) {
    libSodium().crypto_auth_hmacsha512256_keygen(k);
  }

  static long crypto_auth_bytes() {
    return libSodium().crypto_auth_bytes();
  }

  static long crypto_auth_keybytes() {
    return libSodium().crypto_auth_keybytes();
  }

  static String crypto_auth_primitive() {
    return libSodium().crypto_auth_primitive();
  }

  static int crypto_auth(byte[] out, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth(out, in, inlen, k);
  }

  static int crypto_auth_verify(byte[] h, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_verify(h, in, inlen, k);
  }

  static void crypto_auth_keygen(Pointer k) {
    libSodium().crypto_auth_keygen(k);
  }

  static long crypto_hash_sha256_statebytes() {
    return libSodium().crypto_hash_sha256_statebytes();
  }

  static long crypto_hash_sha256_bytes() {
    return libSodium().crypto_hash_sha256_bytes();
  }

  static int crypto_hash_sha256(byte[] out, byte[] in, long inlen) {
    return libSodium().crypto_hash_sha256(out, in, inlen);
  }

  static int crypto_hash_sha256(Pointer out, Pointer in, long inlen) {
    return libSodium().crypto_hash_sha256(out, in, inlen);
  }

  static int crypto_hash_sha256_init(Pointer state) {
    return libSodium().crypto_hash_sha256_init(state);
  }

  static int crypto_hash_sha256_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_hash_sha256_update(state, in, inlen);
  }

  static int crypto_hash_sha256_final(Pointer state, byte[] out) {
    return libSodium().crypto_hash_sha256_final(state, out);
  }

  static long crypto_auth_hmacsha256_bytes() {
    return libSodium().crypto_auth_hmacsha256_bytes();
  }

  static long crypto_auth_hmacsha256_keybytes() {
    return libSodium().crypto_auth_hmacsha256_keybytes();
  }

  static int crypto_auth_hmacsha256(byte[] out, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_hmacsha256(out, in, inlen, k);
  }

  static int crypto_auth_hmacsha256_verify(byte[] h, byte[] in, long inlen, Pointer k) {
    return libSodium().crypto_auth_hmacsha256_verify(h, in, inlen, k);
  }

  static long crypto_auth_hmacsha256_statebytes() {
    return libSodium().crypto_auth_hmacsha256_statebytes();
  }

  static int crypto_auth_hmacsha256_init(Pointer state, byte[] key, long keylen) {
    return libSodium().crypto_auth_hmacsha256_init(state, key, keylen);
  }

  static int crypto_auth_hmacsha256_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_auth_hmacsha256_update(state, in, inlen);
  }

  static int crypto_auth_hmacsha256_final(Pointer state, byte[] out) {
    return libSodium().crypto_auth_hmacsha256_final(state, out);
  }

  static void crypto_auth_hmacsha256_keygen(byte[] k) {
    libSodium().crypto_auth_hmacsha256_keygen(k);
  }

  static long crypto_stream_xsalsa20_keybytes() {
    return libSodium().crypto_stream_xsalsa20_keybytes();
  }

  static long crypto_stream_xsalsa20_noncebytes() {
    return libSodium().crypto_stream_xsalsa20_noncebytes();
  }

  static long crypto_stream_xsalsa20_messagebytes_max() {
    return libSodium().crypto_stream_xsalsa20_messagebytes_max();
  }

  static int crypto_stream_xsalsa20(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_xsalsa20(c, clen, n, k);
  }

  static int crypto_stream_xsalsa20_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_xsalsa20_xor(c, m, mlen, n, k);
  }

  static int crypto_stream_xsalsa20_xor_ic(byte[] c, byte[] m, long mlen, byte[] n, long ic, byte[] k) {
    return libSodium().crypto_stream_xsalsa20_xor_ic(c, m, mlen, n, ic, k);
  }

  static void crypto_stream_xsalsa20_keygen(byte[] k) {
    libSodium().crypto_stream_xsalsa20_keygen(k);
  }

  static long crypto_box_curve25519xsalsa20poly1305_seedbytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_seedbytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_publickeybytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_publickeybytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_secretkeybytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_secretkeybytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_beforenmbytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_beforenmbytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_noncebytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_noncebytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_macbytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_macbytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_messagebytes_max() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_messagebytes_max();
  }

  static int crypto_box_curve25519xsalsa20poly1305_seed_keypair(byte[] pk, byte[] sk, byte[] seed) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_seed_keypair(pk, sk, seed);
  }

  static int crypto_box_curve25519xsalsa20poly1305_keypair(byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_keypair(pk, sk);
  }

  static int crypto_box_curve25519xsalsa20poly1305_beforenm(Pointer k, byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_beforenm(k, pk, sk);
  }

  static long crypto_box_curve25519xsalsa20poly1305_boxzerobytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_boxzerobytes();
  }

  static long crypto_box_curve25519xsalsa20poly1305_zerobytes() {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_zerobytes();
  }

  static int crypto_box_curve25519xsalsa20poly1305(byte[] c, byte[] m, long mlen, byte[] n, byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305(c, m, mlen, n, pk, sk);
  }

  static int crypto_box_curve25519xsalsa20poly1305_open(byte[] m, byte[] c, long clen, byte[] n, byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_open(m, c, clen, n, pk, sk);
  }

  static int crypto_box_curve25519xsalsa20poly1305_afternm(byte[] c, byte[] m, long mlen, byte[] n, Pointer k) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_afternm(c, m, mlen, n, k);
  }

  static int crypto_box_curve25519xsalsa20poly1305_open_afternm(byte[] m, byte[] c, long clen, byte[] n, Pointer k) {
    return libSodium().crypto_box_curve25519xsalsa20poly1305_open_afternm(m, c, clen, n, k);
  }

  static long crypto_box_seedbytes() {
    return libSodium().crypto_box_seedbytes();
  }

  static long crypto_box_publickeybytes() {
    return libSodium().crypto_box_publickeybytes();
  }

  static long crypto_box_secretkeybytes() {
    return libSodium().crypto_box_secretkeybytes();
  }

  static long crypto_box_noncebytes() {
    return libSodium().crypto_box_noncebytes();
  }

  static long crypto_box_macbytes() {
    return libSodium().crypto_box_macbytes();
  }

  static long crypto_box_messagebytes_max() {
    return libSodium().crypto_box_messagebytes_max();
  }

  static String crypto_box_primitive() {
    return libSodium().crypto_box_primitive();
  }

  static int crypto_box_seed_keypair(Pointer pk, Pointer sk, Pointer seed) {
    return libSodium().crypto_box_seed_keypair(pk, sk, seed);
  }

  static int crypto_box_keypair(Pointer pk, Pointer sk) {
    return libSodium().crypto_box_keypair(pk, sk);
  }

  static int crypto_box_easy(byte[] c, byte[] m, long mlen, Pointer n, Pointer pk, Pointer sk) {
    return libSodium().crypto_box_easy(c, m, mlen, n, pk, sk);
  }

  static int crypto_box_open_easy(byte[] m, byte[] c, long clen, Pointer n, Pointer pk, Pointer sk) {
    return libSodium().crypto_box_open_easy(m, c, clen, n, pk, sk);
  }

  static int crypto_box_detached(byte[] c, byte[] mac, byte[] m, long mlen, Pointer n, Pointer pk, Pointer sk) {
    return libSodium().crypto_box_detached(c, mac, m, mlen, n, pk, sk);
  }

  static int crypto_box_open_detached(byte[] m, byte[] c, byte[] mac, long clen, Pointer n, Pointer pk, Pointer sk) {
    return libSodium().crypto_box_open_detached(m, c, mac, clen, n, pk, sk);
  }

  static long crypto_box_beforenmbytes() {
    return libSodium().crypto_box_beforenmbytes();
  }

  static int crypto_box_beforenm(Pointer k, Pointer pk, Pointer sk) {
    return libSodium().crypto_box_beforenm(k, pk, sk);
  }

  static int crypto_box_easy_afternm(byte[] c, byte[] m, long mlen, Pointer n, Pointer k) {
    return libSodium().crypto_box_easy_afternm(c, m, mlen, n, k);
  }

  static int crypto_box_open_easy_afternm(byte[] m, byte[] c, long clen, Pointer n, Pointer k) {
    return libSodium().crypto_box_open_easy_afternm(m, c, clen, n, k);
  }

  static int crypto_box_detached_afternm(byte[] c, byte[] mac, byte[] m, long mlen, Pointer n, Pointer k) {
    return libSodium().crypto_box_detached_afternm(c, mac, m, mlen, n, k);
  }

  static int crypto_box_open_detached_afternm(byte[] m, byte[] c, byte[] mac, long clen, Pointer n, Pointer k) {
    return libSodium().crypto_box_open_detached_afternm(m, c, mac, clen, n, k);
  }

  static long crypto_box_sealbytes() {
    return libSodium().crypto_box_sealbytes();
  }

  static int crypto_box_seal(byte[] c, byte[] m, long mlen, Pointer pk) {
    return libSodium().crypto_box_seal(c, m, mlen, pk);
  }

  static int crypto_box_seal_open(byte[] m, byte[] c, long clen, Pointer pk, Pointer sk) {
    return libSodium().crypto_box_seal_open(m, c, clen, pk, sk);
  }

  static long crypto_box_zerobytes() {
    return libSodium().crypto_box_zerobytes();
  }

  static long crypto_box_boxzerobytes() {
    return libSodium().crypto_box_boxzerobytes();
  }

  static int crypto_box(byte[] c, byte[] m, long mlen, byte[] n, byte[] pk, byte[] sk) {
    return libSodium().crypto_box(c, m, mlen, n, pk, sk);
  }

  static int crypto_box_open(byte[] m, byte[] c, long clen, byte[] n, byte[] pk, byte[] sk) {
    return libSodium().crypto_box_open(m, c, clen, n, pk, sk);
  }

  static int crypto_box_afternm(byte[] c, byte[] m, long mlen, byte[] n, Pointer k) {
    return libSodium().crypto_box_afternm(c, m, mlen, n, k);
  }

  static int crypto_box_open_afternm(byte[] m, byte[] c, long clen, byte[] n, Pointer k) {
    return libSodium().crypto_box_open_afternm(m, c, clen, n, k);
  }

  static long crypto_core_hsalsa20_outputbytes() {
    return libSodium().crypto_core_hsalsa20_outputbytes();
  }

  static long crypto_core_hsalsa20_inputbytes() {
    return libSodium().crypto_core_hsalsa20_inputbytes();
  }

  static long crypto_core_hsalsa20_keybytes() {
    return libSodium().crypto_core_hsalsa20_keybytes();
  }

  static long crypto_core_hsalsa20_constbytes() {
    return libSodium().crypto_core_hsalsa20_constbytes();
  }

  static int crypto_core_hsalsa20(byte[] out, byte[] in, byte[] k, byte[] c) {
    return libSodium().crypto_core_hsalsa20(out, in, k, c);
  }

  static long crypto_core_hchacha20_outputbytes() {
    return libSodium().crypto_core_hchacha20_outputbytes();
  }

  static long crypto_core_hchacha20_inputbytes() {
    return libSodium().crypto_core_hchacha20_inputbytes();
  }

  static long crypto_core_hchacha20_keybytes() {
    return libSodium().crypto_core_hchacha20_keybytes();
  }

  static long crypto_core_hchacha20_constbytes() {
    return libSodium().crypto_core_hchacha20_constbytes();
  }

  static int crypto_core_hchacha20(byte[] out, byte[] in, byte[] k, byte[] c) {
    return libSodium().crypto_core_hchacha20(out, in, k, c);
  }

  static long crypto_core_salsa20_outputbytes() {
    return libSodium().crypto_core_salsa20_outputbytes();
  }

  static long crypto_core_salsa20_inputbytes() {
    return libSodium().crypto_core_salsa20_inputbytes();
  }

  static long crypto_core_salsa20_keybytes() {
    return libSodium().crypto_core_salsa20_keybytes();
  }

  static long crypto_core_salsa20_constbytes() {
    return libSodium().crypto_core_salsa20_constbytes();
  }

  static int crypto_core_salsa20(byte[] out, byte[] in, byte[] k, byte[] c) {
    return libSodium().crypto_core_salsa20(out, in, k, c);
  }

  static long crypto_core_salsa2012_outputbytes() {
    return libSodium().crypto_core_salsa2012_outputbytes();
  }

  static long crypto_core_salsa2012_inputbytes() {
    return libSodium().crypto_core_salsa2012_inputbytes();
  }

  static long crypto_core_salsa2012_keybytes() {
    return libSodium().crypto_core_salsa2012_keybytes();
  }

  static long crypto_core_salsa2012_constbytes() {
    return libSodium().crypto_core_salsa2012_constbytes();
  }

  static int crypto_core_salsa2012(byte[] out, byte[] in, byte[] k, byte[] c) {
    return libSodium().crypto_core_salsa2012(out, in, k, c);
  }

  static long crypto_core_salsa208_outputbytes() {
    return libSodium().crypto_core_salsa208_outputbytes();
  }

  static long crypto_core_salsa208_inputbytes() {
    return libSodium().crypto_core_salsa208_inputbytes();
  }

  static long crypto_core_salsa208_keybytes() {
    return libSodium().crypto_core_salsa208_keybytes();
  }

  static long crypto_core_salsa208_constbytes() {
    return libSodium().crypto_core_salsa208_constbytes();
  }

  static int crypto_core_salsa208(byte[] out, byte[] in, byte[] k, byte[] c) {
    return libSodium().crypto_core_salsa208(out, in, k, c);
  }

  static long crypto_generichash_blake2b_bytes_min() {
    return libSodium().crypto_generichash_blake2b_bytes_min();
  }

  static long crypto_generichash_blake2b_bytes_max() {
    return libSodium().crypto_generichash_blake2b_bytes_max();
  }

  static long crypto_generichash_blake2b_bytes() {
    return libSodium().crypto_generichash_blake2b_bytes();
  }

  static long crypto_generichash_blake2b_keybytes_min() {
    return libSodium().crypto_generichash_blake2b_keybytes_min();
  }

  static long crypto_generichash_blake2b_keybytes_max() {
    return libSodium().crypto_generichash_blake2b_keybytes_max();
  }

  static long crypto_generichash_blake2b_keybytes() {
    return libSodium().crypto_generichash_blake2b_keybytes();
  }

  static long crypto_generichash_blake2b_saltbytes() {
    return libSodium().crypto_generichash_blake2b_saltbytes();
  }

  static long crypto_generichash_blake2b_personalbytes() {
    return libSodium().crypto_generichash_blake2b_personalbytes();
  }

  static long crypto_generichash_blake2b_statebytes() {
    return libSodium().crypto_generichash_blake2b_statebytes();
  }

  static int crypto_generichash_blake2b(byte[] out, long outlen, byte[] in, long inlen, byte[] key, long keylen) {
    return libSodium().crypto_generichash_blake2b(out, outlen, in, inlen, key, keylen);
  }

  static int crypto_generichash_blake2b_salt_personal(
      byte[] out,
      long outlen,
      byte[] in,
      long inlen,
      byte[] key,
      long keylen,
      byte[] salt,
      byte[] personal) {
    return libSodium().crypto_generichash_blake2b_salt_personal(out, outlen, in, inlen, key, keylen, salt, personal);
  }

  static int crypto_generichash_blake2b_init(Pointer state, byte[] key, long keylen, long outlen) {
    return libSodium().crypto_generichash_blake2b_init(state, key, keylen, outlen);
  }

  static int crypto_generichash_blake2b_init_salt_personal(
      Pointer state,
      byte[] key,
      long keylen,
      long outlen,
      byte[] salt,
      byte[] personal) {
    return libSodium().crypto_generichash_blake2b_init_salt_personal(state, key, keylen, outlen, salt, personal);
  }

  static int crypto_generichash_blake2b_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_generichash_blake2b_update(state, in, inlen);
  }

  static int crypto_generichash_blake2b_final(Pointer state, byte[] out, long outlen) {
    return libSodium().crypto_generichash_blake2b_final(state, out, outlen);
  }

  static void crypto_generichash_blake2b_keygen(byte[] k) {
    libSodium().crypto_generichash_blake2b_keygen(k);
  }

  static long crypto_generichash_bytes_min() {
    return libSodium().crypto_generichash_bytes_min();
  }

  static long crypto_generichash_bytes_max() {
    return libSodium().crypto_generichash_bytes_max();
  }

  static long crypto_generichash_bytes() {
    return libSodium().crypto_generichash_bytes();
  }

  static long crypto_generichash_keybytes_min() {
    return libSodium().crypto_generichash_keybytes_min();
  }

  static long crypto_generichash_keybytes_max() {
    return libSodium().crypto_generichash_keybytes_max();
  }

  static long crypto_generichash_keybytes() {
    return libSodium().crypto_generichash_keybytes();
  }

  static String crypto_generichash_primitive() {
    return libSodium().crypto_generichash_primitive();
  }

  static long crypto_generichash_statebytes() {
    return libSodium().crypto_generichash_statebytes();
  }

  static int crypto_generichash(Pointer out, long outlen, Pointer in, long inlen, Pointer key, long keylen) {
    return libSodium().crypto_generichash(out, outlen, in, inlen, key, keylen);
  }

  static int crypto_generichash_init(Pointer state, byte[] key, long keylen, long outlen) {
    return libSodium().crypto_generichash_init(state, key, keylen, outlen);
  }

  static int crypto_generichash_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_generichash_update(state, in, inlen);
  }

  static int crypto_generichash_final(Pointer state, byte[] out, long outlen) {
    return libSodium().crypto_generichash_final(state, out, outlen);
  }

  static void crypto_generichash_keygen(byte[] k) {
    libSodium().crypto_generichash_keygen(k);
  }

  static long crypto_hash_bytes() {
    return libSodium().crypto_hash_bytes();
  }

  static int crypto_hash(byte[] out, byte[] in, long inlen) {
    return libSodium().crypto_hash(out, in, inlen);
  }

  static String crypto_hash_primitive() {
    return libSodium().crypto_hash_primitive();
  }

  static long crypto_kdf_blake2b_bytes_min() {
    return libSodium().crypto_kdf_blake2b_bytes_min();
  }

  static long crypto_kdf_blake2b_bytes_max() {
    return libSodium().crypto_kdf_blake2b_bytes_max();
  }

  static long crypto_kdf_blake2b_contextbytes() {
    return libSodium().crypto_kdf_blake2b_contextbytes();
  }

  static long crypto_kdf_blake2b_keybytes() {
    return libSodium().crypto_kdf_blake2b_keybytes();
  }

  static int crypto_kdf_blake2b_derive_from_key(
      byte[] subkey,
      long subkey_len,
      long subkey_id,
      byte[] ctx,
      Pointer key) {
    return libSodium().crypto_kdf_blake2b_derive_from_key(subkey, subkey_len, subkey_id, ctx, key);
  }

  static long crypto_kdf_bytes_min() {
    return libSodium().crypto_kdf_bytes_min();
  }

  static long crypto_kdf_bytes_max() {
    return libSodium().crypto_kdf_bytes_max();
  }

  static long crypto_kdf_contextbytes() {
    return libSodium().crypto_kdf_contextbytes();
  }

  static long crypto_kdf_keybytes() {
    return libSodium().crypto_kdf_keybytes();
  }

  static String crypto_kdf_primitive() {
    return libSodium().crypto_kdf_primitive();
  }

  static int crypto_kdf_derive_from_key(byte[] subkey, long subkey_len, long subkey_id, byte[] ctx, Pointer key) {
    return libSodium().crypto_kdf_derive_from_key(subkey, subkey_len, subkey_id, ctx, key);
  }

  static void crypto_kdf_keygen(Pointer k) {
    libSodium().crypto_kdf_keygen(k);
  }

  static long crypto_kx_publickeybytes() {
    return libSodium().crypto_kx_publickeybytes();
  }

  static long crypto_kx_secretkeybytes() {
    return libSodium().crypto_kx_secretkeybytes();
  }

  static long crypto_kx_seedbytes() {
    return libSodium().crypto_kx_seedbytes();
  }

  static long crypto_kx_sessionkeybytes() {
    return libSodium().crypto_kx_sessionkeybytes();
  }

  static String crypto_kx_primitive() {
    return libSodium().crypto_kx_primitive();
  }

  static int crypto_kx_seed_keypair(Pointer pk, Pointer sk, Pointer seed) {
    return libSodium().crypto_kx_seed_keypair(pk, sk, seed);
  }

  static int crypto_kx_keypair(Pointer pk, Pointer sk) {
    return libSodium().crypto_kx_keypair(pk, sk);
  }

  static int crypto_kx_client_session_keys(
      Pointer rx,
      Pointer tx,
      Pointer client_pk,
      Pointer client_sk,
      Pointer server_pk) {
    return libSodium().crypto_kx_client_session_keys(rx, tx, client_pk, client_sk, server_pk);
  }

  static int crypto_kx_server_session_keys(
      Pointer rx,
      Pointer tx,
      Pointer server_pk,
      Pointer server_sk,
      Pointer client_pk) {
    return libSodium().crypto_kx_server_session_keys(rx, tx, server_pk, server_sk, client_pk);
  }

  static long crypto_onetimeauth_poly1305_statebytes() {
    return libSodium().crypto_onetimeauth_poly1305_statebytes();
  }

  static long crypto_onetimeauth_poly1305_bytes() {
    return libSodium().crypto_onetimeauth_poly1305_bytes();
  }

  static long crypto_onetimeauth_poly1305_keybytes() {
    return libSodium().crypto_onetimeauth_poly1305_keybytes();
  }

  static int crypto_onetimeauth_poly1305(byte[] out, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_onetimeauth_poly1305(out, in, inlen, k);
  }

  static int crypto_onetimeauth_poly1305_verify(byte[] h, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_onetimeauth_poly1305_verify(h, in, inlen, k);
  }

  static int crypto_onetimeauth_poly1305_init(Pointer state, byte[] key) {
    return libSodium().crypto_onetimeauth_poly1305_init(state, key);
  }

  static int crypto_onetimeauth_poly1305_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_onetimeauth_poly1305_update(state, in, inlen);
  }

  static int crypto_onetimeauth_poly1305_final(Pointer state, byte[] out) {
    return libSodium().crypto_onetimeauth_poly1305_final(state, out);
  }

  static void crypto_onetimeauth_poly1305_keygen(byte[] k) {
    libSodium().crypto_onetimeauth_poly1305_keygen(k);
  }

  static long crypto_onetimeauth_statebytes() {
    return libSodium().crypto_onetimeauth_statebytes();
  }

  static long crypto_onetimeauth_bytes() {
    return libSodium().crypto_onetimeauth_bytes();
  }

  static long crypto_onetimeauth_keybytes() {
    return libSodium().crypto_onetimeauth_keybytes();
  }

  static String crypto_onetimeauth_primitive() {
    return libSodium().crypto_onetimeauth_primitive();
  }

  static int crypto_onetimeauth(byte[] out, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_onetimeauth(out, in, inlen, k);
  }

  static int crypto_onetimeauth_verify(byte[] h, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_onetimeauth_verify(h, in, inlen, k);
  }

  static int crypto_onetimeauth_init(Pointer state, byte[] key) {
    return libSodium().crypto_onetimeauth_init(state, key);
  }

  static int crypto_onetimeauth_update(Pointer state, byte[] in, long inlen) {
    return libSodium().crypto_onetimeauth_update(state, in, inlen);
  }

  static int crypto_onetimeauth_final(Pointer state, byte[] out) {
    return libSodium().crypto_onetimeauth_final(state, out);
  }

  static void crypto_onetimeauth_keygen(byte[] k) {
    libSodium().crypto_onetimeauth_keygen(k);
  }

  static int crypto_pwhash_argon2i_alg_argon2i13() {
    return libSodium().crypto_pwhash_argon2i_alg_argon2i13();
  }

  static long crypto_pwhash_argon2i_bytes_min() {
    return libSodium().crypto_pwhash_argon2i_bytes_min();
  }

  static long crypto_pwhash_argon2i_bytes_max() {
    return libSodium().crypto_pwhash_argon2i_bytes_max();
  }

  static long crypto_pwhash_argon2i_passwd_min() {
    return libSodium().crypto_pwhash_argon2i_passwd_min();
  }

  static long crypto_pwhash_argon2i_passwd_max() {
    return libSodium().crypto_pwhash_argon2i_passwd_max();
  }

  static long crypto_pwhash_argon2i_saltbytes() {
    return libSodium().crypto_pwhash_argon2i_saltbytes();
  }

  static long crypto_pwhash_argon2i_strbytes() {
    return libSodium().crypto_pwhash_argon2i_strbytes();
  }

  static String crypto_pwhash_argon2i_strprefix() {
    return libSodium().crypto_pwhash_argon2i_strprefix();
  }

  static long crypto_pwhash_argon2i_opslimit_min() {
    return libSodium().crypto_pwhash_argon2i_opslimit_min();
  }

  static long crypto_pwhash_argon2i_opslimit_max() {
    return libSodium().crypto_pwhash_argon2i_opslimit_max();
  }

  static long crypto_pwhash_argon2i_memlimit_min() {
    return libSodium().crypto_pwhash_argon2i_memlimit_min();
  }

  static long crypto_pwhash_argon2i_memlimit_max() {
    return libSodium().crypto_pwhash_argon2i_memlimit_max();
  }

  static long crypto_pwhash_argon2i_opslimit_interactive() {
    return libSodium().crypto_pwhash_argon2i_opslimit_interactive();
  }

  static long crypto_pwhash_argon2i_memlimit_interactive() {
    return libSodium().crypto_pwhash_argon2i_memlimit_interactive();
  }

  static long crypto_pwhash_argon2i_opslimit_moderate() {
    return libSodium().crypto_pwhash_argon2i_opslimit_moderate();
  }

  static long crypto_pwhash_argon2i_memlimit_moderate() {
    return libSodium().crypto_pwhash_argon2i_memlimit_moderate();
  }

  static long crypto_pwhash_argon2i_opslimit_sensitive() {
    return libSodium().crypto_pwhash_argon2i_opslimit_sensitive();
  }

  static long crypto_pwhash_argon2i_memlimit_sensitive() {
    return libSodium().crypto_pwhash_argon2i_memlimit_sensitive();
  }

  static int crypto_pwhash_argon2i(
      byte[] out,
      long outlen,
      byte[] passwd,
      long passwdlen,
      byte[] salt,
      long opslimit,
      long memlimit,
      int alg) {
    return libSodium().crypto_pwhash_argon2i(out, outlen, passwd, passwdlen, salt, opslimit, memlimit, alg);
  }

  static int crypto_pwhash_argon2i_str(byte[] out, byte[] passwd, long passwdlen, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_argon2i_str(out, passwd, passwdlen, opslimit, memlimit);
  }

  static int crypto_pwhash_argon2i_str_verify(byte[] str, byte[] passwd, long passwdlen) {
    return libSodium().crypto_pwhash_argon2i_str_verify(str, passwd, passwdlen);
  }

  static int crypto_pwhash_argon2i_str_needs_rehash(byte[] str, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_argon2i_str_needs_rehash(str, opslimit, memlimit);
  }

  static int crypto_pwhash_argon2id_alg_argon2id13() {
    return libSodium().crypto_pwhash_argon2id_alg_argon2id13();
  }

  static long crypto_pwhash_argon2id_bytes_min() {
    return libSodium().crypto_pwhash_argon2id_bytes_min();
  }

  static long crypto_pwhash_argon2id_bytes_max() {
    return libSodium().crypto_pwhash_argon2id_bytes_max();
  }

  static long crypto_pwhash_argon2id_passwd_min() {
    return libSodium().crypto_pwhash_argon2id_passwd_min();
  }

  static long crypto_pwhash_argon2id_passwd_max() {
    return libSodium().crypto_pwhash_argon2id_passwd_max();
  }

  static long crypto_pwhash_argon2id_saltbytes() {
    return libSodium().crypto_pwhash_argon2id_saltbytes();
  }

  static long crypto_pwhash_argon2id_strbytes() {
    return libSodium().crypto_pwhash_argon2id_strbytes();
  }

  static String crypto_pwhash_argon2id_strprefix() {
    return libSodium().crypto_pwhash_argon2id_strprefix();
  }

  static long crypto_pwhash_argon2id_opslimit_min() {
    return libSodium().crypto_pwhash_argon2id_opslimit_min();
  }

  static long crypto_pwhash_argon2id_opslimit_max() {
    return libSodium().crypto_pwhash_argon2id_opslimit_max();
  }

  static long crypto_pwhash_argon2id_memlimit_min() {
    return libSodium().crypto_pwhash_argon2id_memlimit_min();
  }

  static long crypto_pwhash_argon2id_memlimit_max() {
    return libSodium().crypto_pwhash_argon2id_memlimit_max();
  }

  static long crypto_pwhash_argon2id_opslimit_interactive() {
    return libSodium().crypto_pwhash_argon2id_opslimit_interactive();
  }

  static long crypto_pwhash_argon2id_memlimit_interactive() {
    return libSodium().crypto_pwhash_argon2id_memlimit_interactive();
  }

  static long crypto_pwhash_argon2id_opslimit_moderate() {
    return libSodium().crypto_pwhash_argon2id_opslimit_moderate();
  }

  static long crypto_pwhash_argon2id_memlimit_moderate() {
    return libSodium().crypto_pwhash_argon2id_memlimit_moderate();
  }

  static long crypto_pwhash_argon2id_opslimit_sensitive() {
    return libSodium().crypto_pwhash_argon2id_opslimit_sensitive();
  }

  static long crypto_pwhash_argon2id_memlimit_sensitive() {
    return libSodium().crypto_pwhash_argon2id_memlimit_sensitive();
  }

  static int crypto_pwhash_argon2id(
      byte[] out,
      long outlen,
      byte[] passwd,
      long passwdlen,
      byte[] salt,
      long opslimit,
      long memlimit,
      int alg) {
    return libSodium().crypto_pwhash_argon2id(out, outlen, passwd, passwdlen, salt, opslimit, memlimit, alg);
  }

  static int crypto_pwhash_argon2id_str(byte[] out, byte[] passwd, long passwdlen, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_argon2id_str(out, passwd, passwdlen, opslimit, memlimit);
  }

  static int crypto_pwhash_argon2id_str_verify(byte[] str, byte[] passwd, long passwdlen) {
    return libSodium().crypto_pwhash_argon2id_str_verify(str, passwd, passwdlen);
  }

  static int crypto_pwhash_argon2id_str_needs_rehash(byte[] str, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_argon2id_str_needs_rehash(str, opslimit, memlimit);
  }

  static int crypto_pwhash_alg_argon2i13() {
    return libSodium().crypto_pwhash_alg_argon2i13();
  }

  static int crypto_pwhash_alg_argon2id13() {
    return libSodium().crypto_pwhash_alg_argon2id13();
  }

  static int crypto_pwhash_alg_default() {
    return libSodium().crypto_pwhash_alg_default();
  }

  static long crypto_pwhash_bytes_min() {
    return libSodium().crypto_pwhash_bytes_min();
  }

  static long crypto_pwhash_bytes_max() {
    return libSodium().crypto_pwhash_bytes_max();
  }

  static long crypto_pwhash_passwd_min() {
    return libSodium().crypto_pwhash_passwd_min();
  }

  static long crypto_pwhash_passwd_max() {
    return libSodium().crypto_pwhash_passwd_max();
  }

  static long crypto_pwhash_saltbytes() {
    return libSodium().crypto_pwhash_saltbytes();
  }

  static long crypto_pwhash_strbytes() {
    return libSodium().crypto_pwhash_strbytes();
  }

  static String crypto_pwhash_strprefix() {
    return libSodium().crypto_pwhash_strprefix();
  }

  static long crypto_pwhash_opslimit_min() {
    return libSodium().crypto_pwhash_opslimit_min();
  }

  static long crypto_pwhash_opslimit_max() {
    return libSodium().crypto_pwhash_opslimit_max();
  }

  static long crypto_pwhash_memlimit_min() {
    return libSodium().crypto_pwhash_memlimit_min();
  }

  static long crypto_pwhash_memlimit_max() {
    return libSodium().crypto_pwhash_memlimit_max();
  }

  static long crypto_pwhash_opslimit_interactive() {
    return libSodium().crypto_pwhash_opslimit_interactive();
  }

  static long crypto_pwhash_memlimit_interactive() {
    return libSodium().crypto_pwhash_memlimit_interactive();
  }

  static long crypto_pwhash_opslimit_moderate() {
    return libSodium().crypto_pwhash_opslimit_moderate();
  }

  static long crypto_pwhash_memlimit_moderate() {
    return libSodium().crypto_pwhash_memlimit_moderate();
  }

  static long crypto_pwhash_opslimit_sensitive() {
    return libSodium().crypto_pwhash_opslimit_sensitive();
  }

  static long crypto_pwhash_memlimit_sensitive() {
    return libSodium().crypto_pwhash_memlimit_sensitive();
  }

  static int crypto_pwhash(
      byte[] out,
      long outlen,
      byte[] passwd,
      long passwdlen,
      Pointer salt,
      long opslimit,
      long memlimit,
      int alg) {
    return libSodium().crypto_pwhash(out, outlen, passwd, passwdlen, salt, opslimit, memlimit, alg);
  }

  static int crypto_pwhash_str(byte[] out, byte[] passwd, long passwdlen, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_str(out, passwd, passwdlen, opslimit, memlimit);
  }

  static int crypto_pwhash_str_alg(byte[] out, byte[] passwd, long passwdlen, long opslimit, long memlimit, int alg) {
    return libSodium().crypto_pwhash_str_alg(out, passwd, passwdlen, opslimit, memlimit, alg);
  }

  static int crypto_pwhash_str_verify(Pointer str, byte[] passwd, long passwdlen) {
    return libSodium().crypto_pwhash_str_verify(str, passwd, passwdlen);
  }

  static int crypto_pwhash_str_needs_rehash(Pointer str, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_str_needs_rehash(str, opslimit, memlimit);
  }

  static String crypto_pwhash_primitive() {
    return libSodium().crypto_pwhash_primitive();
  }

  static long crypto_scalarmult_curve25519_bytes() {
    return libSodium().crypto_scalarmult_curve25519_bytes();
  }

  static long crypto_scalarmult_curve25519_scalarbytes() {
    return libSodium().crypto_scalarmult_curve25519_scalarbytes();
  }

  static int crypto_scalarmult_curve25519(byte[] q, byte[] n, byte[] p) {
    return libSodium().crypto_scalarmult_curve25519(q, n, p);
  }

  static int crypto_scalarmult_curve25519_base(byte[] q, byte[] n) {
    return libSodium().crypto_scalarmult_curve25519_base(q, n);
  }

  static long crypto_scalarmult_bytes() {
    return libSodium().crypto_scalarmult_bytes();
  }

  static long crypto_scalarmult_scalarbytes() {
    return libSodium().crypto_scalarmult_scalarbytes();
  }

  static String crypto_scalarmult_primitive() {
    return libSodium().crypto_scalarmult_primitive();
  }

  static int crypto_scalarmult_base(Pointer q, Pointer n) {
    return libSodium().crypto_scalarmult_base(q, n);
  }

  static int crypto_scalarmult(Pointer q, Pointer n, Pointer p) {
    return libSodium().crypto_scalarmult(q, n, p);
  }

  static long crypto_secretbox_xsalsa20poly1305_keybytes() {
    return libSodium().crypto_secretbox_xsalsa20poly1305_keybytes();
  }

  static long crypto_secretbox_xsalsa20poly1305_noncebytes() {
    return libSodium().crypto_secretbox_xsalsa20poly1305_noncebytes();
  }

  static long crypto_secretbox_xsalsa20poly1305_macbytes() {
    return libSodium().crypto_secretbox_xsalsa20poly1305_macbytes();
  }

  static long crypto_secretbox_xsalsa20poly1305_messagebytes_max() {
    return libSodium().crypto_secretbox_xsalsa20poly1305_messagebytes_max();
  }

  static int crypto_secretbox_xsalsa20poly1305(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_secretbox_xsalsa20poly1305(c, m, mlen, n, k);
  }

  static int crypto_secretbox_xsalsa20poly1305_open(byte[] m, byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_secretbox_xsalsa20poly1305_open(m, c, clen, n, k);
  }

  static void crypto_secretbox_xsalsa20poly1305_keygen(byte[] k) {
    libSodium().crypto_secretbox_xsalsa20poly1305_keygen(k);
  }

  static long crypto_secretbox_xsalsa20poly1305_boxzerobytes() {
    return libSodium().crypto_secretbox_xsalsa20poly1305_boxzerobytes();
  }

  static long crypto_secretbox_xsalsa20poly1305_zerobytes() {
    return libSodium().crypto_secretbox_xsalsa20poly1305_zerobytes();
  }

  static long crypto_secretbox_keybytes() {
    return libSodium().crypto_secretbox_keybytes();
  }

  static long crypto_secretbox_noncebytes() {
    return libSodium().crypto_secretbox_noncebytes();
  }

  static long crypto_secretbox_macbytes() {
    return libSodium().crypto_secretbox_macbytes();
  }

  static String crypto_secretbox_primitive() {
    return libSodium().crypto_secretbox_primitive();
  }

  static long crypto_secretbox_messagebytes_max() {
    return libSodium().crypto_secretbox_messagebytes_max();
  }

  static int crypto_secretbox_easy(byte[] c, byte[] m, long mlen, Pointer n, Pointer k) {
    return libSodium().crypto_secretbox_easy(c, m, mlen, n, k);
  }

  static int crypto_secretbox_easy(Pointer c, Pointer m, long mlen, Pointer n, Pointer k) {
    return libSodium().crypto_secretbox_easy(c, m, mlen, n, k);
  }

  static int crypto_secretbox_open_easy(byte[] m, byte[] c, long clen, Pointer n, Pointer k) {
    return libSodium().crypto_secretbox_open_easy(m, c, clen, n, k);
  }

  static int crypto_secretbox_open_easy(Pointer m, Pointer c, long clen, Pointer n, Pointer k) {
    return libSodium().crypto_secretbox_open_easy(m, c, clen, n, k);
  }

  static int crypto_secretbox_detached(byte[] c, byte[] mac, byte[] m, long mlen, Pointer n, Pointer k) {
    return libSodium().crypto_secretbox_detached(c, mac, m, mlen, n, k);
  }

  static int crypto_secretbox_open_detached(byte[] m, byte[] c, byte[] mac, long clen, Pointer n, Pointer k) {
    return libSodium().crypto_secretbox_open_detached(m, c, mac, clen, n, k);
  }

  static void crypto_secretbox_keygen(Pointer k) {
    libSodium().crypto_secretbox_keygen(k);
  }

  static long crypto_secretbox_zerobytes() {
    return libSodium().crypto_secretbox_zerobytes();
  }

  static long crypto_secretbox_boxzerobytes() {
    return libSodium().crypto_secretbox_boxzerobytes();
  }

  static int crypto_secretbox(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_secretbox(c, m, mlen, n, k);
  }

  static int crypto_secretbox_open(byte[] m, byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_secretbox_open(m, c, clen, n, k);
  }

  static long crypto_stream_chacha20_keybytes() {
    return libSodium().crypto_stream_chacha20_keybytes();
  }

  static long crypto_stream_chacha20_noncebytes() {
    return libSodium().crypto_stream_chacha20_noncebytes();
  }

  static long crypto_stream_chacha20_messagebytes_max() {
    return libSodium().crypto_stream_chacha20_messagebytes_max();
  }

  static int crypto_stream_chacha20(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_chacha20(c, clen, n, k);
  }

  static int crypto_stream_chacha20_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_chacha20_xor(c, m, mlen, n, k);
  }

  static int crypto_stream_chacha20_xor_ic(byte[] c, byte[] m, long mlen, byte[] n, long ic, byte[] k) {
    return libSodium().crypto_stream_chacha20_xor_ic(c, m, mlen, n, ic, k);
  }

  static void crypto_stream_chacha20_keygen(byte[] k) {
    libSodium().crypto_stream_chacha20_keygen(k);
  }

  static long crypto_stream_chacha20_ietf_keybytes() {
    return libSodium().crypto_stream_chacha20_ietf_keybytes();
  }

  static long crypto_stream_chacha20_ietf_noncebytes() {
    return libSodium().crypto_stream_chacha20_ietf_noncebytes();
  }

  static long crypto_stream_chacha20_ietf_messagebytes_max() {
    return libSodium().crypto_stream_chacha20_ietf_messagebytes_max();
  }

  static int crypto_stream_chacha20_ietf(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_chacha20_ietf(c, clen, n, k);
  }

  static int crypto_stream_chacha20_ietf_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_chacha20_ietf_xor(c, m, mlen, n, k);
  }

  static int crypto_stream_chacha20_ietf_xor_ic(byte[] c, byte[] m, long mlen, byte[] n, int ic, byte[] k) {
    return libSodium().crypto_stream_chacha20_ietf_xor_ic(c, m, mlen, n, ic, k);
  }

  static void crypto_stream_chacha20_ietf_keygen(byte[] k) {
    libSodium().crypto_stream_chacha20_ietf_keygen(k);
  }

  static long crypto_secretstream_xchacha20poly1305_abytes() {
    return libSodium().crypto_secretstream_xchacha20poly1305_abytes();
  }

  static long crypto_secretstream_xchacha20poly1305_headerbytes() {
    return libSodium().crypto_secretstream_xchacha20poly1305_headerbytes();
  }

  static long crypto_secretstream_xchacha20poly1305_keybytes() {
    return libSodium().crypto_secretstream_xchacha20poly1305_keybytes();
  }

  static long crypto_secretstream_xchacha20poly1305_messagebytes_max() {
    return libSodium().crypto_secretstream_xchacha20poly1305_messagebytes_max();
  }

  static char crypto_secretstream_xchacha20poly1305_tag_message() {
    return libSodium().crypto_secretstream_xchacha20poly1305_tag_message();
  }

  static char crypto_secretstream_xchacha20poly1305_tag_push() {
    return libSodium().crypto_secretstream_xchacha20poly1305_tag_push();
  }

  static char crypto_secretstream_xchacha20poly1305_tag_rekey() {
    return libSodium().crypto_secretstream_xchacha20poly1305_tag_rekey();
  }

  static char crypto_secretstream_xchacha20poly1305_tag_final() {
    return libSodium().crypto_secretstream_xchacha20poly1305_tag_final();
  }

  static long crypto_secretstream_xchacha20poly1305_statebytes() {
    return libSodium().crypto_secretstream_xchacha20poly1305_statebytes();
  }

  static void crypto_secretstream_xchacha20poly1305_keygen(Pointer k) {
    libSodium().crypto_secretstream_xchacha20poly1305_keygen(k);
  }

  static int crypto_secretstream_xchacha20poly1305_init_push(Pointer state, byte[] header, Pointer k) {
    return libSodium().crypto_secretstream_xchacha20poly1305_init_push(state, header, k);
  }

  static int crypto_secretstream_xchacha20poly1305_push(
      Pointer state,
      byte[] c,
      @Nullable LongLongByReference clen_p,
      byte[] m,
      long mlen,
      @Nullable byte[] ad,
      long adlen,
      byte tag) {
    return libSodium().crypto_secretstream_xchacha20poly1305_push(state, c, clen_p, m, mlen, ad, adlen, tag);
  }

  static int crypto_secretstream_xchacha20poly1305_init_pull(Pointer state, byte[] header, Pointer k) {
    return libSodium().crypto_secretstream_xchacha20poly1305_init_pull(state, header, k);
  }

  static int crypto_secretstream_xchacha20poly1305_pull(
      Pointer state,
      byte[] m,
      @Nullable LongLongByReference mlen_p,
      ByteByReference tag_p,
      byte[] c,
      long clen,
      @Nullable byte[] ad,
      long adlen) {
    return libSodium().crypto_secretstream_xchacha20poly1305_pull(state, m, mlen_p, tag_p, c, clen, ad, adlen);
  }

  static void crypto_secretstream_xchacha20poly1305_rekey(Pointer state) {
    libSodium().crypto_secretstream_xchacha20poly1305_rekey(state);
  }

  static long crypto_shorthash_siphash24_bytes() {
    return libSodium().crypto_shorthash_siphash24_bytes();
  }

  static long crypto_shorthash_siphash24_keybytes() {
    return libSodium().crypto_shorthash_siphash24_keybytes();
  }

  static int crypto_shorthash_siphash24(byte[] out, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_shorthash_siphash24(out, in, inlen, k);
  }

  static long crypto_shorthash_siphashx24_bytes() {
    return libSodium().crypto_shorthash_siphashx24_bytes();
  }

  static long crypto_shorthash_siphashx24_keybytes() {
    return libSodium().crypto_shorthash_siphashx24_keybytes();
  }

  static int crypto_shorthash_siphashx24(byte[] out, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_shorthash_siphashx24(out, in, inlen, k);
  }

  static long crypto_shorthash_bytes() {
    return libSodium().crypto_shorthash_bytes();
  }

  static long crypto_shorthash_keybytes() {
    return libSodium().crypto_shorthash_keybytes();
  }

  static String crypto_shorthash_primitive() {
    return libSodium().crypto_shorthash_primitive();
  }

  static int crypto_shorthash(byte[] out, byte[] in, long inlen, byte[] k) {
    return libSodium().crypto_shorthash(out, in, inlen, k);
  }

  static void crypto_shorthash_keygen(byte[] k) {
    libSodium().crypto_shorthash_keygen(k);
  }

  static long crypto_sign_ed25519ph_statebytes() {
    return libSodium().crypto_sign_ed25519ph_statebytes();
  }

  static long crypto_sign_ed25519_bytes() {
    return libSodium().crypto_sign_ed25519_bytes();
  }

  static long crypto_sign_ed25519_seedbytes() {
    return libSodium().crypto_sign_ed25519_seedbytes();
  }

  static long crypto_sign_ed25519_publickeybytes() {
    return libSodium().crypto_sign_ed25519_publickeybytes();
  }

  static long crypto_sign_ed25519_secretkeybytes() {
    return libSodium().crypto_sign_ed25519_secretkeybytes();
  }

  static long crypto_sign_ed25519_messagebytes_max() {
    return libSodium().crypto_sign_ed25519_messagebytes_max();
  }

  static int crypto_sign_ed25519(byte[] sm, LongLongByReference smlen_p, byte[] m, long mlen, byte[] sk) {
    return libSodium().crypto_sign_ed25519(sm, smlen_p, m, mlen, sk);
  }

  static int crypto_sign_ed25519_open(byte[] m, LongLongByReference mlen_p, byte[] sm, long smlen, byte[] pk) {
    return libSodium().crypto_sign_ed25519_open(m, mlen_p, sm, smlen, pk);
  }

  static int crypto_sign_ed25519_detached(byte[] sig, LongLongByReference siglen_p, byte[] m, long mlen, byte[] sk) {
    return libSodium().crypto_sign_ed25519_detached(sig, siglen_p, m, mlen, sk);
  }

  static int crypto_sign_ed25519_verify_detached(byte[] sig, byte[] m, long mlen, byte[] pk) {
    return libSodium().crypto_sign_ed25519_verify_detached(sig, m, mlen, pk);
  }

  static int crypto_sign_ed25519_keypair(byte[] pk, byte[] sk) {
    return libSodium().crypto_sign_ed25519_keypair(pk, sk);
  }

  static int crypto_sign_ed25519_seed_keypair(byte[] pk, byte[] sk, byte[] seed) {
    return libSodium().crypto_sign_ed25519_seed_keypair(pk, sk, seed);
  }

  static int crypto_sign_ed25519_pk_to_curve25519(Pointer curve25519_pk, Pointer ed25519_pk) {
    return libSodium().crypto_sign_ed25519_pk_to_curve25519(curve25519_pk, ed25519_pk);
  }

  static int crypto_sign_ed25519_sk_to_curve25519(Pointer curve25519_sk, Pointer ed25519_sk) {
    return libSodium().crypto_sign_ed25519_sk_to_curve25519(curve25519_sk, ed25519_sk);
  }

  static int crypto_sign_ed25519_sk_to_seed(byte[] seed, byte[] sk) {
    return libSodium().crypto_sign_ed25519_sk_to_seed(seed, sk);
  }

  static int crypto_sign_ed25519_sk_to_pk(Pointer pk, Pointer sk) {
    return libSodium().crypto_sign_ed25519_sk_to_pk(pk, sk);
  }

  static int crypto_sign_ed25519ph_init(Pointer state) {
    return libSodium().crypto_sign_ed25519ph_init(state);
  }

  static int crypto_sign_ed25519ph_update(Pointer state, byte[] m, long mlen) {
    return libSodium().crypto_sign_ed25519ph_update(state, m, mlen);
  }

  static int crypto_sign_ed25519ph_final_create(Pointer state, byte[] sig, LongLongByReference siglen_p, byte[] sk) {
    return libSodium().crypto_sign_ed25519ph_final_create(state, sig, siglen_p, sk);
  }

  static int crypto_sign_ed25519ph_final_verify(Pointer state, byte[] sig, byte[] pk) {
    return libSodium().crypto_sign_ed25519ph_final_verify(state, sig, pk);
  }

  static long crypto_sign_statebytes() {
    return libSodium().crypto_sign_statebytes();
  }

  static long crypto_sign_bytes() {
    return libSodium().crypto_sign_bytes();
  }

  static long crypto_sign_seedbytes() {
    return libSodium().crypto_sign_seedbytes();
  }

  static long crypto_sign_publickeybytes() {
    return libSodium().crypto_sign_publickeybytes();
  }

  static long crypto_sign_secretkeybytes() {
    return libSodium().crypto_sign_secretkeybytes();
  }

  static long crypto_sign_messagebytes_max() {
    return libSodium().crypto_sign_messagebytes_max();
  }

  static String crypto_sign_primitive() {
    return libSodium().crypto_sign_primitive();
  }

  static int crypto_sign_seed_keypair(Pointer pk, Pointer sk, Pointer seed) {
    return libSodium().crypto_sign_seed_keypair(pk, sk, seed);
  }

  static int crypto_sign_keypair(Pointer pk, Pointer sk) {
    return libSodium().crypto_sign_keypair(pk, sk);
  }

  static int crypto_sign(byte[] sm, @Nullable LongLongByReference smlen_p, byte[] m, long mlen, Pointer sk) {
    return libSodium().crypto_sign(sm, smlen_p, m, mlen, sk);
  }

  static int crypto_sign_open(byte[] m, LongLongByReference mlen_p, byte[] sm, long smlen, Pointer pk) {
    return libSodium().crypto_sign_open(m, mlen_p, sm, smlen, pk);
  }

  static int crypto_sign_detached(byte[] sig, @Nullable LongLongByReference siglen_p, byte[] m, long mlen, Pointer sk) {
    return libSodium().crypto_sign_detached(sig, siglen_p, m, mlen, sk);
  }

  static int crypto_sign_detached(
      Pointer sig,
      @Nullable LongLongByReference siglen_p,
      Pointer m,
      long mlen,
      Pointer sk) {
    return libSodium().crypto_sign_detached(sig, siglen_p, m, mlen, sk);
  }

  static int crypto_sign_verify_detached(Pointer sig, Pointer m, long mlen, Pointer pk) {
    return libSodium().crypto_sign_verify_detached(sig, m, mlen, pk);
  }

  static int crypto_sign_verify_detached(byte[] sig, byte[] m, long mlen, Pointer pk) {
    return libSodium().crypto_sign_verify_detached(sig, m, mlen, pk);
  }

  static int crypto_sign_init(Pointer state) {
    return libSodium().crypto_sign_init(state);
  }

  static int crypto_sign_update(Pointer state, byte[] m, long mlen) {
    return libSodium().crypto_sign_update(state, m, mlen);
  }

  static int crypto_sign_final_create(Pointer state, byte[] sig, LongLongByReference siglen_p, byte[] sk) {
    return libSodium().crypto_sign_final_create(state, sig, siglen_p, sk);
  }

  static int crypto_sign_final_verify(Pointer state, byte[] sig, byte[] pk) {
    return libSodium().crypto_sign_final_verify(state, sig, pk);
  }

  static long crypto_stream_keybytes() {
    return libSodium().crypto_stream_keybytes();
  }

  static long crypto_stream_noncebytes() {
    return libSodium().crypto_stream_noncebytes();
  }

  static long crypto_stream_messagebytes_max() {
    return libSodium().crypto_stream_messagebytes_max();
  }

  static String crypto_stream_primitive() {
    return libSodium().crypto_stream_primitive();
  }

  static int crypto_stream(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream(c, clen, n, k);
  }

  static int crypto_stream_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_xor(c, m, mlen, n, k);
  }

  static void crypto_stream_keygen(byte[] k) {
    libSodium().crypto_stream_keygen(k);
  }

  static long crypto_stream_salsa20_keybytes() {
    return libSodium().crypto_stream_salsa20_keybytes();
  }

  static long crypto_stream_salsa20_noncebytes() {
    return libSodium().crypto_stream_salsa20_noncebytes();
  }

  static long crypto_stream_salsa20_messagebytes_max() {
    return libSodium().crypto_stream_salsa20_messagebytes_max();
  }

  static int crypto_stream_salsa20(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_salsa20(c, clen, n, k);
  }

  static int crypto_stream_salsa20_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_salsa20_xor(c, m, mlen, n, k);
  }

  static int crypto_stream_salsa20_xor_ic(byte[] c, byte[] m, long mlen, byte[] n, long ic, byte[] k) {
    return libSodium().crypto_stream_salsa20_xor_ic(c, m, mlen, n, ic, k);
  }

  static void crypto_stream_salsa20_keygen(byte[] k) {
    libSodium().crypto_stream_salsa20_keygen(k);
  }

  static long crypto_verify_16_bytes() {
    return libSodium().crypto_verify_16_bytes();
  }

  static int crypto_verify_16(byte[] x, byte[] y) {
    return libSodium().crypto_verify_16(x, y);
  }

  static long crypto_verify_32_bytes() {
    return libSodium().crypto_verify_32_bytes();
  }

  static int crypto_verify_32(byte[] x, byte[] y) {
    return libSodium().crypto_verify_32(x, y);
  }

  static long crypto_verify_64_bytes() {
    return libSodium().crypto_verify_64_bytes();
  }

  static int crypto_verify_64(byte[] x, byte[] y) {
    return libSodium().crypto_verify_64(x, y);
  }

  static String implementation_name() {
    return libSodium().implementation_name();
  }

  static int random() {
    return libSodium().random();
  }

  static void stir() {
    libSodium().stir();
  }

  static int uniform(int upper_bound) {
    return libSodium().uniform(upper_bound);
  }

  static void buf(byte[] buf, long size) {
    libSodium().buf(buf, size);
  }

  static int close() {
    return libSodium().close();
  }

  static long randombytes_seedbytes() {
    return libSodium().randombytes_seedbytes();
  }

  static void randombytes_buf(Pointer buf, long size) {
    libSodium().randombytes_buf(buf, size);
  }

  static void randombytes_buf_deterministic(byte[] buf, long size, byte[] seed) {
    libSodium().randombytes_buf_deterministic(buf, size, seed);
  }

  static int randombytes_random() {
    return libSodium().randombytes_random();
  }

  static int randombytes_uniform(int upper_bound) {
    return libSodium().randombytes_uniform(upper_bound);
  }

  static void randombytes_stir() {
    libSodium().randombytes_stir();
  }

  static int randombytes_close() {
    return libSodium().randombytes_close();
  }

  static int randombytes_set_implementation(Pointer impl) {
    return libSodium().randombytes_set_implementation(impl);
  }

  static String randombytes_implementation_name() {
    return libSodium().randombytes_implementation_name();
  }

  static void randombytes(byte[] buf, long buf_len) {
    libSodium().randombytes(buf, buf_len);
  }

  static int sodium_runtime_has_neon() {
    return libSodium().sodium_runtime_has_neon();
  }

  static int sodium_runtime_has_sse2() {
    return libSodium().sodium_runtime_has_sse2();
  }

  static int sodium_runtime_has_sse3() {
    return libSodium().sodium_runtime_has_sse3();
  }

  static int sodium_runtime_has_ssse3() {
    return libSodium().sodium_runtime_has_ssse3();
  }

  static int sodium_runtime_has_sse41() {
    return libSodium().sodium_runtime_has_sse41();
  }

  static int sodium_runtime_has_avx() {
    return libSodium().sodium_runtime_has_avx();
  }

  static int sodium_runtime_has_avx2() {
    return libSodium().sodium_runtime_has_avx2();
  }

  static int sodium_runtime_has_avx512f() {
    return libSodium().sodium_runtime_has_avx512f();
  }

  static int sodium_runtime_has_pclmul() {
    return libSodium().sodium_runtime_has_pclmul();
  }

  static int sodium_runtime_has_aesni() {
    return libSodium().sodium_runtime_has_aesni();
  }

  static int sodium_runtime_has_rdrand() {
    return libSodium().sodium_runtime_has_rdrand();
  }

  static int _sodium_runtime_get_cpu_features() {
    return libSodium()._sodium_runtime_get_cpu_features();
  }

  static void sodium_memzero(Pointer pnt, long len) {
    libSodium().sodium_memzero(pnt, len);
  }

  //  static void sodium_stackzero(long len) {
  //    libSodium().sodium_stackzero(len);
  //  }

  static int sodium_memcmp(Pointer b1_, Pointer b2_, long len) {
    return libSodium().sodium_memcmp(b1_, b2_, len);
  }

  static int sodium_compare(Pointer b1_, Pointer b2_, long len) {
    return libSodium().sodium_compare(b1_, b2_, len);
  }

  static int sodium_is_zero(Pointer n, long nlen) {
    return libSodium().sodium_is_zero(n, nlen);
  }

  static void sodium_increment(Pointer n, long nlen) {
    libSodium().sodium_increment(n, nlen);
  }

  static void sodium_add(Pointer a, Pointer b, long len) {
    libSodium().sodium_add(a, b, len);
  }

  // FIXME: not available due to issue with LibSodium#sodium_bin2hex
  //  static byte[] sodium_bin2hex(byte[] hex, long hex_maxlen, byte[] bin, long bin_len) {
  //    return libSodium().sodium_bin2hex(hex, hex_maxlen, bin, bin_len);
  //  }

  static int sodium_hex2bin(
      byte[] bin,
      long bin_maxlen,
      byte[] hex,
      long hex_len,
      byte[] ignore,
      LongLongByReference bin_len,
      Pointer hex_end) {
    return libSodium().sodium_hex2bin(bin, bin_maxlen, hex, hex_len, ignore, bin_len, hex_end);
  }

  static long sodium_base64_encoded_len(long bin_len, int variant) {
    return libSodium().sodium_base64_encoded_len(bin_len, variant);
  }

  // FIXME: not available due to issue with LibSodium#sodium_bin2base64
  //  static byte[] sodium_bin2base64(byte[] b64, long b64_maxlen, byte[] bin, long bin_len, int variant) {
  //    return libSodium().sodium_bin2base64(b64, b64_maxlen, bin, bin_len, variant);
  //  }

  static int sodium_base642bin(
      byte[] bin,
      long bin_maxlen,
      byte[] b64,
      long b64_len,
      byte[] ignore,
      LongLongByReference bin_len,
      Pointer b64_end,
      int variant) {
    return libSodium().sodium_base642bin(bin, bin_maxlen, b64, b64_len, ignore, bin_len, b64_end, variant);
  }

  static int sodium_mlock(Pointer addr, long len) {
    return libSodium().sodium_mlock(addr, len);
  }

  static int sodium_munlock(Pointer addr, long len) {
    return libSodium().sodium_munlock(addr, len);
  }

  static Pointer sodium_malloc(long size) {
    return libSodium().sodium_malloc(size);
  }

  static Pointer sodium_allocarray(long count, long size) {
    return libSodium().sodium_allocarray(count, size);
  }

  static void sodium_free(Pointer ptr) {
    libSodium().sodium_free(ptr);
  }

  static int sodium_mprotect_noaccess(Pointer ptr) {
    return libSodium().sodium_mprotect_noaccess(ptr);
  }

  static int sodium_mprotect_readonly(Pointer ptr) {
    return libSodium().sodium_mprotect_readonly(ptr);
  }

  static int sodium_mprotect_readwrite(Pointer ptr) {
    return libSodium().sodium_mprotect_readwrite(ptr);
  }

  static int sodium_pad(
      LongLongByReference padded_buflen_p,
      byte[] buf,
      long unpadded_buflen,
      long blocksize,
      long max_buflen) {
    return libSodium().sodium_pad(padded_buflen_p, buf, unpadded_buflen, blocksize, max_buflen);
  }

  static int sodium_unpad(LongLongByReference unpadded_buflen_p, byte[] buf, long padded_buflen, long blocksize) {
    return libSodium().sodium_unpad(unpadded_buflen_p, buf, padded_buflen, blocksize);
  }

  static long crypto_stream_xchacha20_keybytes() {
    return libSodium().crypto_stream_xchacha20_keybytes();
  }

  static long crypto_stream_xchacha20_noncebytes() {
    return libSodium().crypto_stream_xchacha20_noncebytes();
  }

  static long crypto_stream_xchacha20_messagebytes_max() {
    return libSodium().crypto_stream_xchacha20_messagebytes_max();
  }

  static int crypto_stream_xchacha20(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_xchacha20(c, clen, n, k);
  }

  static int crypto_stream_xchacha20_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_xchacha20_xor(c, m, mlen, n, k);
  }

  static int crypto_stream_xchacha20_xor_ic(byte[] c, byte[] m, long mlen, byte[] n, long ic, byte[] k) {
    return libSodium().crypto_stream_xchacha20_xor_ic(c, m, mlen, n, ic, k);
  }

  static void crypto_stream_xchacha20_keygen(byte[] k) {
    libSodium().crypto_stream_xchacha20_keygen(k);
  }

  static long crypto_box_curve25519xchacha20poly1305_seedbytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_seedbytes();
  }

  static long crypto_box_curve25519xchacha20poly1305_publickeybytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_publickeybytes();
  }

  static long crypto_box_curve25519xchacha20poly1305_secretkeybytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_secretkeybytes();
  }

  static long crypto_box_curve25519xchacha20poly1305_beforenmbytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_beforenmbytes();
  }

  static long crypto_box_curve25519xchacha20poly1305_noncebytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_noncebytes();
  }

  static long crypto_box_curve25519xchacha20poly1305_macbytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_macbytes();
  }

  static long crypto_box_curve25519xchacha20poly1305_messagebytes_max() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_messagebytes_max();
  }

  static int crypto_box_curve25519xchacha20poly1305_seed_keypair(byte[] pk, byte[] sk, byte[] seed) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_seed_keypair(pk, sk, seed);
  }

  static int crypto_box_curve25519xchacha20poly1305_keypair(byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_keypair(pk, sk);
  }

  static int crypto_box_curve25519xchacha20poly1305_easy(
      byte[] c,
      byte[] m,
      long mlen,
      byte[] n,
      byte[] pk,
      byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_easy(c, m, mlen, n, pk, sk);
  }

  static int crypto_box_curve25519xchacha20poly1305_open_easy(
      byte[] m,
      byte[] c,
      long clen,
      byte[] n,
      byte[] pk,
      byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_open_easy(m, c, clen, n, pk, sk);
  }

  static int crypto_box_curve25519xchacha20poly1305_detached(
      byte[] c,
      byte[] mac,
      byte[] m,
      long mlen,
      byte[] n,
      byte[] pk,
      byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_detached(c, mac, m, mlen, n, pk, sk);
  }

  static int crypto_box_curve25519xchacha20poly1305_open_detached(
      byte[] m,
      byte[] c,
      byte[] mac,
      long clen,
      byte[] n,
      byte[] pk,
      byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_open_detached(m, c, mac, clen, n, pk, sk);
  }

  static int crypto_box_curve25519xchacha20poly1305_beforenm(Pointer k, byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_beforenm(k, pk, sk);
  }

  static int crypto_box_curve25519xchacha20poly1305_easy_afternm(byte[] c, byte[] m, long mlen, byte[] n, Pointer k) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_easy_afternm(c, m, mlen, n, k);
  }

  static int crypto_box_curve25519xchacha20poly1305_open_easy_afternm(
      byte[] m,
      byte[] c,
      long clen,
      byte[] n,
      Pointer k) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_open_easy_afternm(m, c, clen, n, k);
  }

  static int crypto_box_curve25519xchacha20poly1305_detached_afternm(
      byte[] c,
      byte[] mac,
      byte[] m,
      long mlen,
      byte[] n,
      Pointer k) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_detached_afternm(c, mac, m, mlen, n, k);
  }

  static int crypto_box_curve25519xchacha20poly1305_open_detached_afternm(
      byte[] m,
      byte[] c,
      byte[] mac,
      long clen,
      byte[] n,
      Pointer k) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_open_detached_afternm(m, c, mac, clen, n, k);
  }

  static long crypto_box_curve25519xchacha20poly1305_sealbytes() {
    return libSodium().crypto_box_curve25519xchacha20poly1305_sealbytes();
  }

  static int crypto_box_curve25519xchacha20poly1305_seal(byte[] c, byte[] m, long mlen, byte[] pk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_seal(c, m, mlen, pk);
  }

  static int crypto_box_curve25519xchacha20poly1305_seal_open(byte[] m, byte[] c, long clen, byte[] pk, byte[] sk) {
    return libSodium().crypto_box_curve25519xchacha20poly1305_seal_open(m, c, clen, pk, sk);
  }

  static long crypto_core_ed25519_bytes() {
    return libSodium().crypto_core_ed25519_bytes();
  }

  static long crypto_core_ed25519_uniformbytes() {
    return libSodium().crypto_core_ed25519_uniformbytes();
  }

  static long crypto_core_ed25519_hashbytes() {
    return libSodium().crypto_core_ed25519_hashbytes();
  }

  static long crypto_core_ed25519_scalarbytes() {
    return libSodium().crypto_core_ed25519_scalarbytes();
  }

  static long crypto_core_ed25519_nonreducedscalarbytes() {
    return libSodium().crypto_core_ed25519_nonreducedscalarbytes();
  }

  static int crypto_core_ed25519_is_valid_point(byte[] p) {
    return libSodium().crypto_core_ed25519_is_valid_point(p);
  }

  static int crypto_core_ed25519_add(byte[] r, byte[] p, byte[] q) {
    return libSodium().crypto_core_ed25519_add(r, p, q);
  }

  static int crypto_core_ed25519_sub(byte[] r, byte[] p, byte[] q) {
    return libSodium().crypto_core_ed25519_sub(r, p, q);
  }

  static int crypto_core_ed25519_from_uniform(byte[] p, byte[] r) {
    return libSodium().crypto_core_ed25519_from_uniform(p, r);
  }

  static int crypto_core_ed25519_from_hash(byte[] p, byte[] h) {
    return libSodium().crypto_core_ed25519_from_hash(p, h);
  }

  static void crypto_core_ed25519_random(byte[] p) {
    libSodium().crypto_core_ed25519_random(p);
  }

  static void crypto_core_ed25519_scalar_random(byte[] r) {
    libSodium().crypto_core_ed25519_scalar_random(r);
  }

  static int crypto_core_ed25519_scalar_invert(byte[] recip, byte[] s) {
    return libSodium().crypto_core_ed25519_scalar_invert(recip, s);
  }

  static void crypto_core_ed25519_scalar_negate(byte[] neg, byte[] s) {
    libSodium().crypto_core_ed25519_scalar_negate(neg, s);
  }

  static void crypto_core_ed25519_scalar_complement(byte[] comp, byte[] s) {
    libSodium().crypto_core_ed25519_scalar_complement(comp, s);
  }

  static void crypto_core_ed25519_scalar_add(byte[] z, byte[] x, byte[] y) {
    libSodium().crypto_core_ed25519_scalar_add(z, x, y);
  }

  static void crypto_core_ed25519_scalar_sub(byte[] z, byte[] x, byte[] y) {
    libSodium().crypto_core_ed25519_scalar_sub(z, x, y);
  }

  static void crypto_core_ed25519_scalar_mul(byte[] z, byte[] x, byte[] y) {
    libSodium().crypto_core_ed25519_scalar_mul(z, x, y);
  }

  static void crypto_core_ed25519_scalar_reduce(byte[] r, byte[] s) {
    libSodium().crypto_core_ed25519_scalar_reduce(r, s);
  }

  static long crypto_core_ristretto255_bytes() {
    return libSodium().crypto_core_ristretto255_bytes();
  }

  static long crypto_core_ristretto255_hashbytes() {
    return libSodium().crypto_core_ristretto255_hashbytes();
  }

  static long crypto_core_ristretto255_scalarbytes() {
    return libSodium().crypto_core_ristretto255_scalarbytes();
  }

  static long crypto_core_ristretto255_nonreducedscalarbytes() {
    return libSodium().crypto_core_ristretto255_nonreducedscalarbytes();
  }

  static int crypto_core_ristretto255_is_valid_point(byte[] p) {
    return libSodium().crypto_core_ristretto255_is_valid_point(p);
  }

  static int crypto_core_ristretto255_add(byte[] r, byte[] p, byte[] q) {
    return libSodium().crypto_core_ristretto255_add(r, p, q);
  }

  static int crypto_core_ristretto255_sub(byte[] r, byte[] p, byte[] q) {
    return libSodium().crypto_core_ristretto255_sub(r, p, q);
  }

  static int crypto_core_ristretto255_from_hash(byte[] p, byte[] r) {
    return libSodium().crypto_core_ristretto255_from_hash(p, r);
  }

  static void crypto_core_ristretto255_random(byte[] p) {
    libSodium().crypto_core_ristretto255_random(p);
  }

  static void crypto_core_ristretto255_scalar_random(byte[] r) {
    libSodium().crypto_core_ristretto255_scalar_random(r);
  }

  static int crypto_core_ristretto255_scalar_invert(byte[] recip, byte[] s) {
    return libSodium().crypto_core_ristretto255_scalar_invert(recip, s);
  }

  static void crypto_core_ristretto255_scalar_negate(byte[] neg, byte[] s) {
    libSodium().crypto_core_ristretto255_scalar_negate(neg, s);
  }

  static void crypto_core_ristretto255_scalar_complement(byte[] comp, byte[] s) {
    libSodium().crypto_core_ristretto255_scalar_complement(comp, s);
  }

  static void crypto_core_ristretto255_scalar_add(byte[] z, byte[] x, byte[] y) {
    libSodium().crypto_core_ristretto255_scalar_add(z, x, y);
  }

  static void crypto_core_ristretto255_scalar_sub(byte[] z, byte[] x, byte[] y) {
    libSodium().crypto_core_ristretto255_scalar_sub(z, x, y);
  }

  static void crypto_core_ristretto255_scalar_mul(byte[] z, byte[] x, byte[] y) {
    libSodium().crypto_core_ristretto255_scalar_mul(z, x, y);
  }

  static void crypto_core_ristretto255_scalar_reduce(byte[] r, byte[] s) {
    libSodium().crypto_core_ristretto255_scalar_reduce(r, s);
  }

  static long crypto_scalarmult_ed25519_bytes() {
    return libSodium().crypto_scalarmult_ed25519_bytes();
  }

  static long crypto_scalarmult_ed25519_scalarbytes() {
    return libSodium().crypto_scalarmult_ed25519_scalarbytes();
  }

  static int crypto_scalarmult_ed25519(byte[] q, byte[] n, byte[] p) {
    return libSodium().crypto_scalarmult_ed25519(q, n, p);
  }

  static int crypto_scalarmult_ed25519_noclamp(byte[] q, byte[] n, byte[] p) {
    return libSodium().crypto_scalarmult_ed25519_noclamp(q, n, p);
  }

  static int crypto_scalarmult_ed25519_base(byte[] q, byte[] n) {
    return libSodium().crypto_scalarmult_ed25519_base(q, n);
  }

  static int crypto_scalarmult_ed25519_base_noclamp(byte[] q, byte[] n) {
    return libSodium().crypto_scalarmult_ed25519_base_noclamp(q, n);
  }

  static long crypto_scalarmult_ristretto255_bytes() {
    return libSodium().crypto_scalarmult_ristretto255_bytes();
  }

  static long crypto_scalarmult_ristretto255_scalarbytes() {
    return libSodium().crypto_scalarmult_ristretto255_scalarbytes();
  }

  static int crypto_scalarmult_ristretto255(byte[] q, byte[] n, byte[] p) {
    return libSodium().crypto_scalarmult_ristretto255(q, n, p);
  }

  static int crypto_scalarmult_ristretto255_base(byte[] q, byte[] n) {
    return libSodium().crypto_scalarmult_ristretto255_base(q, n);
  }

  static long crypto_secretbox_xchacha20poly1305_keybytes() {
    return libSodium().crypto_secretbox_xchacha20poly1305_keybytes();
  }

  static long crypto_secretbox_xchacha20poly1305_noncebytes() {
    return libSodium().crypto_secretbox_xchacha20poly1305_noncebytes();
  }

  static long crypto_secretbox_xchacha20poly1305_macbytes() {
    return libSodium().crypto_secretbox_xchacha20poly1305_macbytes();
  }

  static long crypto_secretbox_xchacha20poly1305_messagebytes_max() {
    return libSodium().crypto_secretbox_xchacha20poly1305_messagebytes_max();
  }

  static int crypto_secretbox_xchacha20poly1305_easy(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_secretbox_xchacha20poly1305_easy(c, m, mlen, n, k);
  }

  static int crypto_secretbox_xchacha20poly1305_open_easy(byte[] m, byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_secretbox_xchacha20poly1305_open_easy(m, c, clen, n, k);
  }

  static int crypto_secretbox_xchacha20poly1305_detached(
      byte[] c,
      byte[] mac,
      byte[] m,
      long mlen,
      byte[] n,
      byte[] k) {
    return libSodium().crypto_secretbox_xchacha20poly1305_detached(c, mac, m, mlen, n, k);
  }

  static int crypto_secretbox_xchacha20poly1305_open_detached(
      byte[] m,
      byte[] c,
      byte[] mac,
      long clen,
      byte[] n,
      byte[] k) {
    return libSodium().crypto_secretbox_xchacha20poly1305_open_detached(m, c, mac, clen, n, k);
  }

  static long crypto_pwhash_scryptsalsa208sha256_bytes_min() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_bytes_min();
  }

  static long crypto_pwhash_scryptsalsa208sha256_bytes_max() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_bytes_max();
  }

  static long crypto_pwhash_scryptsalsa208sha256_passwd_min() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_passwd_min();
  }

  static long crypto_pwhash_scryptsalsa208sha256_passwd_max() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_passwd_max();
  }

  static long crypto_pwhash_scryptsalsa208sha256_saltbytes() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_saltbytes();
  }

  static long crypto_pwhash_scryptsalsa208sha256_strbytes() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_strbytes();
  }

  static String crypto_pwhash_scryptsalsa208sha256_strprefix() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_strprefix();
  }

  static long crypto_pwhash_scryptsalsa208sha256_opslimit_min() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_opslimit_min();
  }

  static long crypto_pwhash_scryptsalsa208sha256_opslimit_max() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_opslimit_max();
  }

  static long crypto_pwhash_scryptsalsa208sha256_memlimit_min() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_memlimit_min();
  }

  static long crypto_pwhash_scryptsalsa208sha256_memlimit_max() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_memlimit_max();
  }

  static long crypto_pwhash_scryptsalsa208sha256_opslimit_interactive() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_opslimit_interactive();
  }

  static long crypto_pwhash_scryptsalsa208sha256_memlimit_interactive() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_memlimit_interactive();
  }

  static long crypto_pwhash_scryptsalsa208sha256_opslimit_sensitive() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_opslimit_sensitive();
  }

  static long crypto_pwhash_scryptsalsa208sha256_memlimit_sensitive() {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_memlimit_sensitive();
  }

  static int crypto_pwhash_scryptsalsa208sha256(
      byte[] out,
      long outlen,
      byte[] passwd,
      long passwdlen,
      byte[] salt,
      long opslimit,
      long memlimit) {
    return libSodium().crypto_pwhash_scryptsalsa208sha256(out, outlen, passwd, passwdlen, salt, opslimit, memlimit);
  }

  static int crypto_pwhash_scryptsalsa208sha256_str(
      byte[] out,
      byte[] passwd,
      long passwdlen,
      long opslimit,
      long memlimit) {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_str(out, passwd, passwdlen, opslimit, memlimit);
  }

  static int crypto_pwhash_scryptsalsa208sha256_str_verify(byte[] str, byte[] passwd, long passwdlen) {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_str_verify(str, passwd, passwdlen);
  }

  static int crypto_pwhash_scryptsalsa208sha256_ll(
      byte[] passwd,
      long passwdlen,
      byte[] salt,
      long saltlen,
      long N,
      int r,
      int p,
      byte[] buf,
      long buflen) {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_ll(passwd, passwdlen, salt, saltlen, N, r, p, buf, buflen);
  }

  static int crypto_pwhash_scryptsalsa208sha256_str_needs_rehash(byte[] str, long opslimit, long memlimit) {
    return libSodium().crypto_pwhash_scryptsalsa208sha256_str_needs_rehash(str, opslimit, memlimit);
  }

  static long crypto_stream_salsa2012_keybytes() {
    return libSodium().crypto_stream_salsa2012_keybytes();
  }

  static long crypto_stream_salsa2012_noncebytes() {
    return libSodium().crypto_stream_salsa2012_noncebytes();
  }

  static long crypto_stream_salsa2012_messagebytes_max() {
    return libSodium().crypto_stream_salsa2012_messagebytes_max();
  }

  static int crypto_stream_salsa2012(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_salsa2012(c, clen, n, k);
  }

  static int crypto_stream_salsa2012_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_salsa2012_xor(c, m, mlen, n, k);
  }

  static void crypto_stream_salsa2012_keygen(byte[] k) {
    libSodium().crypto_stream_salsa2012_keygen(k);
  }

  static long crypto_stream_salsa208_keybytes() {
    return libSodium().crypto_stream_salsa208_keybytes();
  }

  static long crypto_stream_salsa208_noncebytes() {
    return libSodium().crypto_stream_salsa208_noncebytes();
  }

  static long crypto_stream_salsa208_messagebytes_max() {
    return libSodium().crypto_stream_salsa208_messagebytes_max();
  }

  static int crypto_stream_salsa208(byte[] c, long clen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_salsa208(c, clen, n, k);
  }

  static int crypto_stream_salsa208_xor(byte[] c, byte[] m, long mlen, byte[] n, byte[] k) {
    return libSodium().crypto_stream_salsa208_xor(c, m, mlen, n, k);
  }

  static void crypto_stream_salsa208_keygen(byte[] k) {
    libSodium().crypto_stream_salsa208_keygen(k);
  }
}
