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


import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.ByteByReference;
import jnr.ffi.byref.LongLongByReference;
import jnr.ffi.types.ssize_t;
import jnr.ffi.types.u_int32_t;
import jnr.ffi.types.u_int64_t;
import jnr.ffi.types.u_int8_t;
import org.jetbrains.annotations.Nullable;

// Generated with https://gist.github.com/cleishm/39fbad03378f5e1ad82521ad821cd065, then modified
public interface LibSodium {
  // const char * sodium_version_string(void);
  String sodium_version_string();

  // int sodium_library_version_major(void);
  int sodium_library_version_major();

  // int sodium_library_version_minor(void);
  int sodium_library_version_minor();

  // int sodium_library_minimal(void);
  int sodium_library_minimal();

  // int sodium_init(void);
  int sodium_init();

  // int sodium_set_misuse_handler(void * handler);
  int sodium_set_misuse_handler(/*both*/ Pointer handler);

  // void sodium_misuse(void);
  void sodium_misuse();

  // int crypto_aead_aes256gcm_is_available(void);
  int crypto_aead_aes256gcm_is_available();

  // size_t crypto_aead_aes256gcm_keybytes(void);
  @ssize_t
  long crypto_aead_aes256gcm_keybytes();

  // size_t crypto_aead_aes256gcm_nsecbytes(void);
  @ssize_t
  long crypto_aead_aes256gcm_nsecbytes();

  // size_t crypto_aead_aes256gcm_npubbytes(void);
  @ssize_t
  long crypto_aead_aes256gcm_npubbytes();

  // size_t crypto_aead_aes256gcm_abytes(void);
  @ssize_t
  long crypto_aead_aes256gcm_abytes();

  // size_t crypto_aead_aes256gcm_messagebytes_max(void);
  @ssize_t
  long crypto_aead_aes256gcm_messagebytes_max();

  // size_t crypto_aead_aes256gcm_statebytes(void);
  @ssize_t
  long crypto_aead_aes256gcm_statebytes();

  // int crypto_aead_aes256gcm_encrypt(unsigned char * c, unsigned long long * clen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_aes256gcm_encrypt(
      @Out byte[] c,
      @Out LongLongByReference clen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      /*null*/ Pointer nsec,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_aes256gcm_decrypt(unsigned char * m, unsigned long long * mlen_p, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_aes256gcm_decrypt(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      /*null*/ Pointer nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_aes256gcm_encrypt_detached(unsigned char * c, unsigned char * mac, unsigned long long * maclen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_aes256gcm_encrypt_detached(
      @Out byte[] c,
      @Out byte[] mac,
      @Out LongLongByReference maclen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      /*null*/ Pointer nsec,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_aes256gcm_decrypt_detached(unsigned char * m, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * mac, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_aes256gcm_decrypt_detached(
      @Out byte[] m,
      /*null*/ Pointer nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] mac,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_aes256gcm_beforenm(crypto_aead_aes256gcm_state * ctx_, const unsigned char * k);
  int crypto_aead_aes256gcm_beforenm(@Out Pointer ctx_, @In Pointer k);

  // int crypto_aead_aes256gcm_encrypt_afternm(unsigned char * c, unsigned long long * clen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const crypto_aead_aes256gcm_state * ctx_);
  int crypto_aead_aes256gcm_encrypt_afternm(
      @Out byte[] c,
      @Out LongLongByReference clen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      /*null*/ Pointer nsec,
      @In Pointer npub,
      @In Pointer ctx_);

  // int crypto_aead_aes256gcm_decrypt_afternm(unsigned char * m, unsigned long long * mlen_p, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const crypto_aead_aes256gcm_state * ctx_);
  int crypto_aead_aes256gcm_decrypt_afternm(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      /*null*/ Pointer nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In Pointer npub,
      @In Pointer ctx_);

  // int crypto_aead_aes256gcm_encrypt_detached_afternm(unsigned char * c, unsigned char * mac, unsigned long long * maclen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const crypto_aead_aes256gcm_state * ctx_);
  int crypto_aead_aes256gcm_encrypt_detached_afternm(
      @Out byte[] c,
      @Out byte[] mac,
      @Out LongLongByReference maclen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      /*null*/ Pointer nsec,
      @In Pointer npub,
      @In Pointer ctx_);

  // int crypto_aead_aes256gcm_decrypt_detached_afternm(unsigned char * m, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * mac, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const crypto_aead_aes256gcm_state * ctx_);
  int crypto_aead_aes256gcm_decrypt_detached_afternm(
      @Out byte[] m,
      /*null*/ Pointer nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] mac,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In Pointer npub,
      @In Pointer ctx_);

  // void crypto_aead_aes256gcm_keygen(unsigned char[] k);
  void crypto_aead_aes256gcm_keygen(@Out Pointer k);

  // size_t crypto_aead_chacha20poly1305_ietf_keybytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_ietf_keybytes();

  // size_t crypto_aead_chacha20poly1305_ietf_nsecbytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_ietf_nsecbytes();

  // size_t crypto_aead_chacha20poly1305_ietf_npubbytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_ietf_npubbytes();

  // size_t crypto_aead_chacha20poly1305_ietf_abytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_ietf_abytes();

  // size_t crypto_aead_chacha20poly1305_ietf_messagebytes_max(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_ietf_messagebytes_max();

  // int crypto_aead_chacha20poly1305_ietf_encrypt(unsigned char * c, unsigned long long * clen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_ietf_encrypt(
      @Out byte[] c,
      @Out LongLongByReference clen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] nsec,
      @In byte[] npub,
      @In byte[] k);

  // int crypto_aead_chacha20poly1305_ietf_decrypt(unsigned char * m, unsigned long long * mlen_p, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_ietf_decrypt(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      /*null*/ byte[] nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] npub,
      @In byte[] k);

  // int crypto_aead_chacha20poly1305_ietf_encrypt_detached(unsigned char * c, unsigned char * mac, unsigned long long * maclen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_ietf_encrypt_detached(
      @Out byte[] c,
      @Out byte[] mac,
      @Out LongLongByReference maclen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] nsec,
      @In byte[] npub,
      @In byte[] k);

  // int crypto_aead_chacha20poly1305_ietf_decrypt_detached(unsigned char * m, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * mac, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_ietf_decrypt_detached(
      @Out byte[] m,
      /*null*/ byte[] nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] mac,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] npub,
      @In byte[] k);

  // void crypto_aead_chacha20poly1305_ietf_keygen(unsigned char[] k);
  void crypto_aead_chacha20poly1305_ietf_keygen(@Out byte[] k);

  // size_t crypto_aead_chacha20poly1305_keybytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_keybytes();

  // size_t crypto_aead_chacha20poly1305_nsecbytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_nsecbytes();

  // size_t crypto_aead_chacha20poly1305_npubbytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_npubbytes();

  // size_t crypto_aead_chacha20poly1305_abytes(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_abytes();

  // size_t crypto_aead_chacha20poly1305_messagebytes_max(void);
  @ssize_t
  long crypto_aead_chacha20poly1305_messagebytes_max();

  // int crypto_aead_chacha20poly1305_encrypt(unsigned char * c, unsigned long long * clen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_encrypt(
      @Out byte[] c,
      @Out LongLongByReference clen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] nsec,
      @In byte[] npub,
      @In byte[] k);

  // int crypto_aead_chacha20poly1305_decrypt(unsigned char * m, unsigned long long * mlen_p, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_decrypt(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      /*null*/ byte[] nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] npub,
      @In byte[] k);

  // int crypto_aead_chacha20poly1305_encrypt_detached(unsigned char * c, unsigned char * mac, unsigned long long * maclen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_encrypt_detached(
      @Out byte[] c,
      /*null*/ byte[] mac,
      @Out LongLongByReference maclen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] nsec,
      @In byte[] npub,
      @In byte[] k);

  // int crypto_aead_chacha20poly1305_decrypt_detached(unsigned char * m, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * mac, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_chacha20poly1305_decrypt_detached(
      @Out byte[] m,
      /*null*/ byte[] nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] mac,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] npub,
      @In byte[] k);

  // void crypto_aead_chacha20poly1305_keygen(unsigned char[] k);
  void crypto_aead_chacha20poly1305_keygen(@Out byte[] k);

  // size_t crypto_aead_xchacha20poly1305_ietf_keybytes(void);
  @ssize_t
  long crypto_aead_xchacha20poly1305_ietf_keybytes();

  // size_t crypto_aead_xchacha20poly1305_ietf_nsecbytes(void);
  @ssize_t
  long crypto_aead_xchacha20poly1305_ietf_nsecbytes();

  // size_t crypto_aead_xchacha20poly1305_ietf_npubbytes(void);
  @ssize_t
  long crypto_aead_xchacha20poly1305_ietf_npubbytes();

  // size_t crypto_aead_xchacha20poly1305_ietf_abytes(void);
  @ssize_t
  long crypto_aead_xchacha20poly1305_ietf_abytes();

  // size_t crypto_aead_xchacha20poly1305_ietf_messagebytes_max(void);
  @ssize_t
  long crypto_aead_xchacha20poly1305_ietf_messagebytes_max();

  // int crypto_aead_xchacha20poly1305_ietf_encrypt(unsigned char * c, unsigned long long * clen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_xchacha20poly1305_ietf_encrypt(
      @Out byte[] c,
      @Out LongLongByReference clen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] nsec,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_xchacha20poly1305_ietf_decrypt(unsigned char * m, unsigned long long * mlen_p, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_xchacha20poly1305_ietf_decrypt(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      /*null*/ byte[] nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_xchacha20poly1305_ietf_encrypt_detached(unsigned char * c, unsigned char * mac, unsigned long long * maclen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, const unsigned char * nsec, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_xchacha20poly1305_ietf_encrypt_detached(
      @Out byte[] c,
      /*null*/ byte[] mac,
      @Out LongLongByReference maclen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In byte[] nsec,
      @In Pointer npub,
      @In Pointer k);

  // int crypto_aead_xchacha20poly1305_ietf_decrypt_detached(unsigned char * m, unsigned char * nsec, const unsigned char * c, unsigned long long clen, const unsigned char * mac, const unsigned char * ad, unsigned long long adlen, const unsigned char * npub, const unsigned char * k);
  int crypto_aead_xchacha20poly1305_ietf_decrypt_detached(
      @Out byte[] m,
      /*null*/ byte[] nsec,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] mac,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In Pointer npub,
      @In Pointer k);

  // void crypto_aead_xchacha20poly1305_ietf_keygen(unsigned char[] k);
  void crypto_aead_xchacha20poly1305_ietf_keygen(@Out Pointer k);

  // size_t crypto_hash_sha512_statebytes(void);
  @ssize_t
  long crypto_hash_sha512_statebytes();

  // size_t crypto_hash_sha512_bytes(void);
  @ssize_t
  long crypto_hash_sha512_bytes();

  // int crypto_hash_sha512(unsigned char * out, const unsigned char * in, unsigned long long inlen);
  int crypto_hash_sha512(@Out Pointer out, @In Pointer in, @In @u_int64_t long inlen);

  // int crypto_hash_sha512_init(crypto_hash_sha512_state * state);
  int crypto_hash_sha512_init(@Out Pointer state);

  // int crypto_hash_sha512_update(crypto_hash_sha512_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_hash_sha512_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_hash_sha512_final(crypto_hash_sha512_state * state, unsigned char * out);
  int crypto_hash_sha512_final(/*both*/ Pointer state, @Out byte[] out);

  // size_t crypto_auth_hmacsha512_bytes(void);
  @ssize_t
  long crypto_auth_hmacsha512_bytes();

  // size_t crypto_auth_hmacsha512_keybytes(void);
  @ssize_t
  long crypto_auth_hmacsha512_keybytes();

  // int crypto_auth_hmacsha512(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_hmacsha512(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // int crypto_auth_hmacsha512_verify(const unsigned char * h, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_hmacsha512_verify(@In byte[] h, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // size_t crypto_auth_hmacsha512_statebytes(void);
  @ssize_t
  long crypto_auth_hmacsha512_statebytes();

  // int crypto_auth_hmacsha512_init(crypto_auth_hmacsha512_state * state, const unsigned char * key, size_t keylen);
  int crypto_auth_hmacsha512_init(@Out Pointer state, @In byte[] key, @In @ssize_t long keylen);

  // int crypto_auth_hmacsha512_update(crypto_auth_hmacsha512_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_auth_hmacsha512_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_auth_hmacsha512_final(crypto_auth_hmacsha512_state * state, unsigned char * out);
  int crypto_auth_hmacsha512_final(/*both*/ Pointer state, @Out byte[] out);

  // void crypto_auth_hmacsha512_keygen(unsigned char[] k);
  void crypto_auth_hmacsha512_keygen(@Out byte[] k);

  // size_t crypto_auth_hmacsha512256_bytes(void);
  @ssize_t
  long crypto_auth_hmacsha512256_bytes();

  // size_t crypto_auth_hmacsha512256_keybytes(void);
  @ssize_t
  long crypto_auth_hmacsha512256_keybytes();

  // int crypto_auth_hmacsha512256(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_hmacsha512256(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // int crypto_auth_hmacsha512256_verify(const unsigned char * h, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_hmacsha512256_verify(@In byte[] h, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // size_t crypto_auth_hmacsha512256_statebytes(void);
  @ssize_t
  long crypto_auth_hmacsha512256_statebytes();

  // int crypto_auth_hmacsha512256_init(crypto_auth_hmacsha512256_state * state, const unsigned char * key, size_t keylen);
  int crypto_auth_hmacsha512256_init(@Out Pointer state, @In byte[] key, @In @ssize_t long keylen);

  // int crypto_auth_hmacsha512256_update(crypto_auth_hmacsha512256_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_auth_hmacsha512256_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_auth_hmacsha512256_final(crypto_auth_hmacsha512256_state * state, unsigned char * out);
  int crypto_auth_hmacsha512256_final(/*both*/ Pointer state, @Out byte[] out);

  // void crypto_auth_hmacsha512256_keygen(unsigned char[] k);
  void crypto_auth_hmacsha512256_keygen(@Out byte[] k);

  // size_t crypto_auth_bytes(void);
  @ssize_t
  long crypto_auth_bytes();

  // size_t crypto_auth_keybytes(void);
  @ssize_t
  long crypto_auth_keybytes();

  // const char * crypto_auth_primitive(void);
  String crypto_auth_primitive();

  // int crypto_auth(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // int crypto_auth_verify(const unsigned char * h, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_verify(@In byte[] h, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // void crypto_auth_keygen(unsigned char[] k);
  void crypto_auth_keygen(@Out Pointer k);

  // size_t crypto_hash_sha256_statebytes(void);
  @ssize_t
  long crypto_hash_sha256_statebytes();

  // size_t crypto_hash_sha256_bytes(void);
  @ssize_t
  long crypto_hash_sha256_bytes();

  // int crypto_hash_sha256(unsigned char * out, const unsigned char * in, unsigned long long inlen);
  int crypto_hash_sha256(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_hash_sha256(unsigned char * out, const unsigned char * in, unsigned long long inlen);
  int crypto_hash_sha256(@Out Pointer out, @In Pointer in, @In @u_int64_t long inlen);

  // int crypto_hash_sha256_init(crypto_hash_sha256_state * state);
  int crypto_hash_sha256_init(@Out Pointer state);

  // int crypto_hash_sha256_update(crypto_hash_sha256_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_hash_sha256_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_hash_sha256_final(crypto_hash_sha256_state * state, unsigned char * out);
  int crypto_hash_sha256_final(/*both*/ Pointer state, @Out byte[] out);

  // size_t crypto_auth_hmacsha256_bytes(void);
  @ssize_t
  long crypto_auth_hmacsha256_bytes();

  // size_t crypto_auth_hmacsha256_keybytes(void);
  @ssize_t
  long crypto_auth_hmacsha256_keybytes();

  // int crypto_auth_hmacsha256(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_hmacsha256(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // int crypto_auth_hmacsha256_verify(const unsigned char * h, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_auth_hmacsha256_verify(@In byte[] h, @In byte[] in, @In @u_int64_t long inlen, @In Pointer k);

  // size_t crypto_auth_hmacsha256_statebytes(void);
  @ssize_t
  long crypto_auth_hmacsha256_statebytes();

  // int crypto_auth_hmacsha256_init(crypto_auth_hmacsha256_state * state, const unsigned char * key, size_t keylen);
  int crypto_auth_hmacsha256_init(@Out Pointer state, @In byte[] key, @In @ssize_t long keylen);

  // int crypto_auth_hmacsha256_update(crypto_auth_hmacsha256_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_auth_hmacsha256_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_auth_hmacsha256_final(crypto_auth_hmacsha256_state * state, unsigned char * out);
  int crypto_auth_hmacsha256_final(/*both*/ Pointer state, @Out byte[] out);

  // void crypto_auth_hmacsha256_keygen(unsigned char[] k);
  void crypto_auth_hmacsha256_keygen(@Out byte[] k);

  // size_t crypto_stream_xsalsa20_keybytes(void);
  @ssize_t
  long crypto_stream_xsalsa20_keybytes();

  // size_t crypto_stream_xsalsa20_noncebytes(void);
  @ssize_t
  long crypto_stream_xsalsa20_noncebytes();

  // size_t crypto_stream_xsalsa20_messagebytes_max(void);
  @ssize_t
  long crypto_stream_xsalsa20_messagebytes_max();

  // int crypto_stream_xsalsa20(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_xsalsa20(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_xsalsa20_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_xsalsa20_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // int crypto_stream_xsalsa20_xor_ic(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, uint64_t ic, const unsigned char * k);
  int crypto_stream_xsalsa20_xor_ic(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In @u_int64_t long ic,
      @In byte[] k);

  // void crypto_stream_xsalsa20_keygen(unsigned char[] k);
  void crypto_stream_xsalsa20_keygen(@Out byte[] k);

  // size_t crypto_box_curve25519xsalsa20poly1305_seedbytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_seedbytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_publickeybytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_publickeybytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_secretkeybytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_secretkeybytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_beforenmbytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_beforenmbytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_noncebytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_noncebytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_macbytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_macbytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_messagebytes_max(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_messagebytes_max();

  // int crypto_box_curve25519xsalsa20poly1305_seed_keypair(unsigned char * pk, unsigned char * sk, const unsigned char * seed);
  int crypto_box_curve25519xsalsa20poly1305_seed_keypair(@Out byte[] pk, @Out byte[] sk, @In byte[] seed);

  // int crypto_box_curve25519xsalsa20poly1305_keypair(unsigned char * pk, unsigned char * sk);
  int crypto_box_curve25519xsalsa20poly1305_keypair(@Out byte[] pk, @Out byte[] sk);

  // int crypto_box_curve25519xsalsa20poly1305_beforenm(unsigned char * k, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xsalsa20poly1305_beforenm(@Out Pointer k, @In byte[] pk, @In byte[] sk);

  // size_t crypto_box_curve25519xsalsa20poly1305_boxzerobytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_boxzerobytes();

  // size_t crypto_box_curve25519xsalsa20poly1305_zerobytes(void);
  @ssize_t
  long crypto_box_curve25519xsalsa20poly1305_zerobytes();

  // int crypto_box_curve25519xsalsa20poly1305(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xsalsa20poly1305(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_curve25519xsalsa20poly1305_open(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xsalsa20poly1305_open(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_curve25519xsalsa20poly1305_afternm(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_box_curve25519xsalsa20poly1305_afternm(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In Pointer k);

  // int crypto_box_curve25519xsalsa20poly1305_open_afternm(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_box_curve25519xsalsa20poly1305_open_afternm(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In Pointer k);

  // size_t crypto_box_seedbytes(void);
  @ssize_t
  long crypto_box_seedbytes();

  // size_t crypto_box_publickeybytes(void);
  @ssize_t
  long crypto_box_publickeybytes();

  // size_t crypto_box_secretkeybytes(void);
  @ssize_t
  long crypto_box_secretkeybytes();

  // size_t crypto_box_noncebytes(void);
  @ssize_t
  long crypto_box_noncebytes();

  // size_t crypto_box_macbytes(void);
  @ssize_t
  long crypto_box_macbytes();

  // size_t crypto_box_messagebytes_max(void);
  @ssize_t
  long crypto_box_messagebytes_max();

  // const char * crypto_box_primitive(void);
  String crypto_box_primitive();

  // int crypto_box_seed_keypair(unsigned char * pk, unsigned char * sk, const unsigned char * seed);
  int crypto_box_seed_keypair(@Out Pointer pk, @Out Pointer sk, @In Pointer seed);

  // int crypto_box_keypair(unsigned char * pk, unsigned char * sk);
  int crypto_box_keypair(@Out Pointer pk, @Out Pointer sk);

  // int crypto_box_easy(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_easy(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In Pointer n,
      @In Pointer pk,
      @In Pointer sk);

  // int crypto_box_open_easy(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_open_easy(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In Pointer n,
      @In Pointer pk,
      @In Pointer sk);

  // int crypto_box_detached(unsigned char * c, unsigned char * mac, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_detached(
      @Out byte[] c,
      @Out byte[] mac,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In Pointer n,
      @In Pointer pk,
      @In Pointer sk);

  // int crypto_box_open_detached(unsigned char * m, const unsigned char * c, const unsigned char * mac, unsigned long long clen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_open_detached(
      @Out byte[] m,
      @In byte[] c,
      @In byte[] mac,
      @In @u_int64_t long clen,
      @In Pointer n,
      @In Pointer pk,
      @In Pointer sk);

  // size_t crypto_box_beforenmbytes(void);
  @ssize_t
  long crypto_box_beforenmbytes();

  // int crypto_box_beforenm(unsigned char * k, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_beforenm(@Out Pointer k, @In Pointer pk, @In Pointer sk);

  // int crypto_box_easy_afternm(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_box_easy_afternm(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In Pointer n, @In Pointer k);

  // int crypto_box_open_easy_afternm(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_box_open_easy_afternm(@Out byte[] m, @In byte[] c, @In @u_int64_t long clen, @In Pointer n, @In Pointer k);

  // int crypto_box_detached_afternm(unsigned char * c, unsigned char * mac, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_box_detached_afternm(
      @Out byte[] c,
      @Out byte[] mac,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In Pointer n,
      @In Pointer k);

  // int crypto_box_open_detached_afternm(unsigned char * m, const unsigned char * c, const unsigned char * mac, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_box_open_detached_afternm(
      @Out byte[] m,
      @In byte[] c,
      @In byte[] mac,
      @In @u_int64_t long clen,
      @In Pointer n,
      @In Pointer k);

  // size_t crypto_box_sealbytes(void);
  @ssize_t
  long crypto_box_sealbytes();

  // int crypto_box_seal(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * pk);
  int crypto_box_seal(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In Pointer pk);

  // int crypto_box_seal_open(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_seal_open(@Out byte[] m, @In byte[] c, @In @u_int64_t long clen, @In Pointer pk, @In Pointer sk);

  // size_t crypto_box_zerobytes(void);
  @ssize_t
  long crypto_box_zerobytes();

  // size_t crypto_box_boxzerobytes(void);
  @ssize_t
  long crypto_box_boxzerobytes();

  // int crypto_box(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] pk, @In byte[] sk);

  // int crypto_box_open(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_open(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_afternm(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_box_afternm(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In Pointer k);

  // int crypto_box_open_afternm(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_box_open_afternm(@Out byte[] m, @In byte[] c, @In @u_int64_t long clen, @In byte[] n, @In Pointer k);

  // size_t crypto_core_hsalsa20_outputbytes(void);
  @ssize_t
  long crypto_core_hsalsa20_outputbytes();

  // size_t crypto_core_hsalsa20_inputbytes(void);
  @ssize_t
  long crypto_core_hsalsa20_inputbytes();

  // size_t crypto_core_hsalsa20_keybytes(void);
  @ssize_t
  long crypto_core_hsalsa20_keybytes();

  // size_t crypto_core_hsalsa20_constbytes(void);
  @ssize_t
  long crypto_core_hsalsa20_constbytes();

  // int crypto_core_hsalsa20(unsigned char * out, const unsigned char * in, const unsigned char * k, const unsigned char * c);
  int crypto_core_hsalsa20(@Out byte[] out, @In byte[] in, @In byte[] k, @In byte[] c);

  // size_t crypto_core_hchacha20_outputbytes(void);
  @ssize_t
  long crypto_core_hchacha20_outputbytes();

  // size_t crypto_core_hchacha20_inputbytes(void);
  @ssize_t
  long crypto_core_hchacha20_inputbytes();

  // size_t crypto_core_hchacha20_keybytes(void);
  @ssize_t
  long crypto_core_hchacha20_keybytes();

  // size_t crypto_core_hchacha20_constbytes(void);
  @ssize_t
  long crypto_core_hchacha20_constbytes();

  // int crypto_core_hchacha20(unsigned char * out, const unsigned char * in, const unsigned char * k, const unsigned char * c);
  int crypto_core_hchacha20(@Out byte[] out, @In byte[] in, @In byte[] k, @In byte[] c);

  // size_t crypto_core_salsa20_outputbytes(void);
  @ssize_t
  long crypto_core_salsa20_outputbytes();

  // size_t crypto_core_salsa20_inputbytes(void);
  @ssize_t
  long crypto_core_salsa20_inputbytes();

  // size_t crypto_core_salsa20_keybytes(void);
  @ssize_t
  long crypto_core_salsa20_keybytes();

  // size_t crypto_core_salsa20_constbytes(void);
  @ssize_t
  long crypto_core_salsa20_constbytes();

  // int crypto_core_salsa20(unsigned char * out, const unsigned char * in, const unsigned char * k, const unsigned char * c);
  int crypto_core_salsa20(@Out byte[] out, @In byte[] in, @In byte[] k, @In byte[] c);

  // size_t crypto_core_salsa2012_outputbytes(void);
  @ssize_t
  long crypto_core_salsa2012_outputbytes();

  // size_t crypto_core_salsa2012_inputbytes(void);
  @ssize_t
  long crypto_core_salsa2012_inputbytes();

  // size_t crypto_core_salsa2012_keybytes(void);
  @ssize_t
  long crypto_core_salsa2012_keybytes();

  // size_t crypto_core_salsa2012_constbytes(void);
  @ssize_t
  long crypto_core_salsa2012_constbytes();

  // int crypto_core_salsa2012(unsigned char * out, const unsigned char * in, const unsigned char * k, const unsigned char * c);
  int crypto_core_salsa2012(@Out byte[] out, @In byte[] in, @In byte[] k, @In byte[] c);

  // size_t crypto_core_salsa208_outputbytes(void);
  @ssize_t
  long crypto_core_salsa208_outputbytes();

  // size_t crypto_core_salsa208_inputbytes(void);
  @ssize_t
  long crypto_core_salsa208_inputbytes();

  // size_t crypto_core_salsa208_keybytes(void);
  @ssize_t
  long crypto_core_salsa208_keybytes();

  // size_t crypto_core_salsa208_constbytes(void);
  @ssize_t
  long crypto_core_salsa208_constbytes();

  // int crypto_core_salsa208(unsigned char * out, const unsigned char * in, const unsigned char * k, const unsigned char * c);
  int crypto_core_salsa208(@Out byte[] out, @In byte[] in, @In byte[] k, @In byte[] c);

  // size_t crypto_generichash_blake2b_bytes_min(void);
  @ssize_t
  long crypto_generichash_blake2b_bytes_min();

  // size_t crypto_generichash_blake2b_bytes_max(void);
  @ssize_t
  long crypto_generichash_blake2b_bytes_max();

  // size_t crypto_generichash_blake2b_bytes(void);
  @ssize_t
  long crypto_generichash_blake2b_bytes();

  // size_t crypto_generichash_blake2b_keybytes_min(void);
  @ssize_t
  long crypto_generichash_blake2b_keybytes_min();

  // size_t crypto_generichash_blake2b_keybytes_max(void);
  @ssize_t
  long crypto_generichash_blake2b_keybytes_max();

  // size_t crypto_generichash_blake2b_keybytes(void);
  @ssize_t
  long crypto_generichash_blake2b_keybytes();

  // size_t crypto_generichash_blake2b_saltbytes(void);
  @ssize_t
  long crypto_generichash_blake2b_saltbytes();

  // size_t crypto_generichash_blake2b_personalbytes(void);
  @ssize_t
  long crypto_generichash_blake2b_personalbytes();

  // size_t crypto_generichash_blake2b_statebytes(void);
  @ssize_t
  long crypto_generichash_blake2b_statebytes();

  // int crypto_generichash_blake2b(unsigned char * out, size_t outlen, const unsigned char * in, unsigned long long inlen, const unsigned char * key, size_t keylen);
  int crypto_generichash_blake2b(
      @Out byte[] out,
      @In @ssize_t long outlen,
      @In byte[] in,
      @In @u_int64_t long inlen,
      @In byte[] key,
      @In @ssize_t long keylen);

  // int crypto_generichash_blake2b_salt_personal(unsigned char * out, size_t outlen, const unsigned char * in, unsigned long long inlen, const unsigned char * key, size_t keylen, const unsigned char * salt, const unsigned char * personal);
  int crypto_generichash_blake2b_salt_personal(
      @Out byte[] out,
      @In @ssize_t long outlen,
      @In byte[] in,
      @In @u_int64_t long inlen,
      @In byte[] key,
      @In @ssize_t long keylen,
      @In byte[] salt,
      @In byte[] personal);

  // int crypto_generichash_blake2b_init(crypto_generichash_blake2b_state * state, const unsigned char * key, const size_t keylen, const size_t outlen);
  int crypto_generichash_blake2b_init(
      @Out Pointer state,
      @In byte[] key,
      @In @ssize_t long keylen,
      @In @ssize_t long outlen);

  // int crypto_generichash_blake2b_init_salt_personal(crypto_generichash_blake2b_state * state, const unsigned char * key, const size_t keylen, const size_t outlen, const unsigned char * salt, const unsigned char * personal);
  int crypto_generichash_blake2b_init_salt_personal(
      /*both*/ Pointer state,
      @In byte[] key,
      @In @ssize_t long keylen,
      @In @ssize_t long outlen,
      @In byte[] salt,
      @In byte[] personal);

  // int crypto_generichash_blake2b_update(crypto_generichash_blake2b_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_generichash_blake2b_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_generichash_blake2b_final(crypto_generichash_blake2b_state * state, unsigned char * out, const size_t outlen);
  int crypto_generichash_blake2b_final(/*both*/ Pointer state, @Out byte[] out, @In @ssize_t long outlen);

  // void crypto_generichash_blake2b_keygen(unsigned char[] k);
  void crypto_generichash_blake2b_keygen(@Out byte[] k);

  // size_t crypto_generichash_bytes_min(void);
  @ssize_t
  long crypto_generichash_bytes_min();

  // size_t crypto_generichash_bytes_max(void);
  @ssize_t
  long crypto_generichash_bytes_max();

  // size_t crypto_generichash_bytes(void);
  @ssize_t
  long crypto_generichash_bytes();

  // size_t crypto_generichash_keybytes_min(void);
  @ssize_t
  long crypto_generichash_keybytes_min();

  // size_t crypto_generichash_keybytes_max(void);
  @ssize_t
  long crypto_generichash_keybytes_max();

  // size_t crypto_generichash_keybytes(void);
  @ssize_t
  long crypto_generichash_keybytes();

  // const char * crypto_generichash_primitive(void);
  String crypto_generichash_primitive();

  // size_t crypto_generichash_statebytes(void);
  @ssize_t
  long crypto_generichash_statebytes();

  // int crypto_generichash(unsigned char * out, size_t outlen, const unsigned char * in, unsigned long long inlen, const unsigned char * key, size_t keylen);
  int crypto_generichash(
      @Out Pointer out,
      @In @ssize_t long outlen,
      @In Pointer in,
      @In @u_int64_t long inlen,
      @In Pointer key,
      @In @ssize_t long keylen);

  // int crypto_generichash_init(crypto_generichash_state * state, const unsigned char * key, const size_t keylen, const size_t outlen);
  int crypto_generichash_init(@Out Pointer state, @In byte[] key, @In @ssize_t long keylen, @In @ssize_t long outlen);

  // int crypto_generichash_update(crypto_generichash_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_generichash_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_generichash_final(crypto_generichash_state * state, unsigned char * out, const size_t outlen);
  int crypto_generichash_final(/*both*/ Pointer state, @Out byte[] out, @In @ssize_t long outlen);

  // void crypto_generichash_keygen(unsigned char[] k);
  void crypto_generichash_keygen(@Out byte[] k);

  // size_t crypto_hash_bytes(void);
  @ssize_t
  long crypto_hash_bytes();

  // int crypto_hash(unsigned char * out, const unsigned char * in, unsigned long long inlen);
  int crypto_hash(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen);

  // const char * crypto_hash_primitive(void);
  String crypto_hash_primitive();

  // size_t crypto_kdf_blake2b_bytes_min(void);
  @ssize_t
  long crypto_kdf_blake2b_bytes_min();

  // size_t crypto_kdf_blake2b_bytes_max(void);
  @ssize_t
  long crypto_kdf_blake2b_bytes_max();

  // size_t crypto_kdf_blake2b_contextbytes(void);
  @ssize_t
  long crypto_kdf_blake2b_contextbytes();

  // size_t crypto_kdf_blake2b_keybytes(void);
  @ssize_t
  long crypto_kdf_blake2b_keybytes();

  // int crypto_kdf_blake2b_derive_from_key(unsigned char * subkey, size_t subkey_len, uint64_t subkey_id, const char[] ctx, const unsigned char[] key);
  int crypto_kdf_blake2b_derive_from_key(
      @Out byte[] subkey,
      @In @ssize_t long subkey_len,
      @In @u_int64_t long subkey_id,
      @In byte[] ctx,
      @In Pointer key);

  // size_t crypto_kdf_bytes_min(void);
  @ssize_t
  long crypto_kdf_bytes_min();

  // size_t crypto_kdf_bytes_max(void);
  @ssize_t
  long crypto_kdf_bytes_max();

  // size_t crypto_kdf_contextbytes(void);
  @ssize_t
  long crypto_kdf_contextbytes();

  // size_t crypto_kdf_keybytes(void);
  @ssize_t
  long crypto_kdf_keybytes();

  // const char * crypto_kdf_primitive(void);
  String crypto_kdf_primitive();

  // int crypto_kdf_derive_from_key(unsigned char * subkey, size_t subkey_len, uint64_t subkey_id, const char[] ctx, const unsigned char[] key);
  int crypto_kdf_derive_from_key(
      @Out byte[] subkey,
      @In @ssize_t long subkey_len,
      @In @u_int64_t long subkey_id,
      @In byte[] ctx,
      @In Pointer key);

  // void crypto_kdf_keygen(unsigned char[] k);
  void crypto_kdf_keygen(@Out Pointer k);

  // size_t crypto_kx_publickeybytes(void);
  @ssize_t
  long crypto_kx_publickeybytes();

  // size_t crypto_kx_secretkeybytes(void);
  @ssize_t
  long crypto_kx_secretkeybytes();

  // size_t crypto_kx_seedbytes(void);
  @ssize_t
  long crypto_kx_seedbytes();

  // size_t crypto_kx_sessionkeybytes(void);
  @ssize_t
  long crypto_kx_sessionkeybytes();

  // const char * crypto_kx_primitive(void);
  String crypto_kx_primitive();

  // int crypto_kx_seed_keypair(unsigned char[] pk, unsigned char[] sk, const unsigned char[] seed);
  int crypto_kx_seed_keypair(@Out Pointer pk, @Out Pointer sk, @In Pointer seed);

  // int crypto_kx_keypair(unsigned char[] pk, unsigned char[] sk);
  int crypto_kx_keypair(@Out Pointer pk, @Out Pointer sk);

  // int crypto_kx_client_session_keys(unsigned char[] rx, unsigned char[] tx, const unsigned char[] client_pk, const unsigned char[] client_sk, const unsigned char[] server_pk);
  int crypto_kx_client_session_keys(
      @Out Pointer rx,
      @Out Pointer tx,
      @In Pointer client_pk,
      @In Pointer client_sk,
      @In Pointer server_pk);

  // int crypto_kx_server_session_keys(unsigned char[] rx, unsigned char[] tx, const unsigned char[] server_pk, const unsigned char[] server_sk, const unsigned char[] client_pk);
  int crypto_kx_server_session_keys(
      @Out Pointer rx,
      @Out Pointer tx,
      @In Pointer server_pk,
      @In Pointer server_sk,
      @In Pointer client_pk);

  // size_t crypto_onetimeauth_poly1305_statebytes(void);
  @ssize_t
  long crypto_onetimeauth_poly1305_statebytes();

  // size_t crypto_onetimeauth_poly1305_bytes(void);
  @ssize_t
  long crypto_onetimeauth_poly1305_bytes();

  // size_t crypto_onetimeauth_poly1305_keybytes(void);
  @ssize_t
  long crypto_onetimeauth_poly1305_keybytes();

  // int crypto_onetimeauth_poly1305(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_onetimeauth_poly1305(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // int crypto_onetimeauth_poly1305_verify(const unsigned char * h, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_onetimeauth_poly1305_verify(@In byte[] h, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // int crypto_onetimeauth_poly1305_init(crypto_onetimeauth_poly1305_state * state, const unsigned char * key);
  int crypto_onetimeauth_poly1305_init(@Out Pointer state, @In byte[] key);

  // int crypto_onetimeauth_poly1305_update(crypto_onetimeauth_poly1305_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_onetimeauth_poly1305_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_onetimeauth_poly1305_final(crypto_onetimeauth_poly1305_state * state, unsigned char * out);
  int crypto_onetimeauth_poly1305_final(/*both*/ Pointer state, @Out byte[] out);

  // void crypto_onetimeauth_poly1305_keygen(unsigned char[] k);
  void crypto_onetimeauth_poly1305_keygen(@Out byte[] k);

  // size_t crypto_onetimeauth_statebytes(void);
  @ssize_t
  long crypto_onetimeauth_statebytes();

  // size_t crypto_onetimeauth_bytes(void);
  @ssize_t
  long crypto_onetimeauth_bytes();

  // size_t crypto_onetimeauth_keybytes(void);
  @ssize_t
  long crypto_onetimeauth_keybytes();

  // const char * crypto_onetimeauth_primitive(void);
  String crypto_onetimeauth_primitive();

  // int crypto_onetimeauth(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_onetimeauth(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // int crypto_onetimeauth_verify(const unsigned char * h, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_onetimeauth_verify(@In byte[] h, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // int crypto_onetimeauth_init(crypto_onetimeauth_state * state, const unsigned char * key);
  int crypto_onetimeauth_init(@Out Pointer state, @In byte[] key);

  // int crypto_onetimeauth_update(crypto_onetimeauth_state * state, const unsigned char * in, unsigned long long inlen);
  int crypto_onetimeauth_update(/*both*/ Pointer state, @In byte[] in, @In @u_int64_t long inlen);

  // int crypto_onetimeauth_final(crypto_onetimeauth_state * state, unsigned char * out);
  int crypto_onetimeauth_final(/*both*/ Pointer state, @Out byte[] out);

  // void crypto_onetimeauth_keygen(unsigned char[] k);
  void crypto_onetimeauth_keygen(@Out byte[] k);

  // int crypto_pwhash_argon2i_alg_argon2i13(void);
  int crypto_pwhash_argon2i_alg_argon2i13();

  // size_t crypto_pwhash_argon2i_bytes_min(void);
  @ssize_t
  long crypto_pwhash_argon2i_bytes_min();

  // size_t crypto_pwhash_argon2i_bytes_max(void);
  @ssize_t
  long crypto_pwhash_argon2i_bytes_max();

  // size_t crypto_pwhash_argon2i_passwd_min(void);
  @ssize_t
  long crypto_pwhash_argon2i_passwd_min();

  // size_t crypto_pwhash_argon2i_passwd_max(void);
  @ssize_t
  long crypto_pwhash_argon2i_passwd_max();

  // size_t crypto_pwhash_argon2i_saltbytes(void);
  @ssize_t
  long crypto_pwhash_argon2i_saltbytes();

  // size_t crypto_pwhash_argon2i_strbytes(void);
  @ssize_t
  long crypto_pwhash_argon2i_strbytes();

  // const char * crypto_pwhash_argon2i_strprefix(void);
  String crypto_pwhash_argon2i_strprefix();

  // size_t crypto_pwhash_argon2i_opslimit_min(void);
  @ssize_t
  long crypto_pwhash_argon2i_opslimit_min();

  // size_t crypto_pwhash_argon2i_opslimit_max(void);
  @ssize_t
  long crypto_pwhash_argon2i_opslimit_max();

  // size_t crypto_pwhash_argon2i_memlimit_min(void);
  @ssize_t
  long crypto_pwhash_argon2i_memlimit_min();

  // size_t crypto_pwhash_argon2i_memlimit_max(void);
  @ssize_t
  long crypto_pwhash_argon2i_memlimit_max();

  // size_t crypto_pwhash_argon2i_opslimit_interactive(void);
  @ssize_t
  long crypto_pwhash_argon2i_opslimit_interactive();

  // size_t crypto_pwhash_argon2i_memlimit_interactive(void);
  @ssize_t
  long crypto_pwhash_argon2i_memlimit_interactive();

  // size_t crypto_pwhash_argon2i_opslimit_moderate(void);
  @ssize_t
  long crypto_pwhash_argon2i_opslimit_moderate();

  // size_t crypto_pwhash_argon2i_memlimit_moderate(void);
  @ssize_t
  long crypto_pwhash_argon2i_memlimit_moderate();

  // size_t crypto_pwhash_argon2i_opslimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_argon2i_opslimit_sensitive();

  // size_t crypto_pwhash_argon2i_memlimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_argon2i_memlimit_sensitive();

  // int crypto_pwhash_argon2i(unsigned char *const out, unsigned long long outlen, const char *const passwd, unsigned long long passwdlen, const unsigned char *const salt, unsigned long long opslimit, size_t memlimit, int alg);
  int crypto_pwhash_argon2i(
      @Out byte[] out,
      @In @u_int64_t long outlen,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In byte[] salt,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit,
      @In int alg);

  // int crypto_pwhash_argon2i_str(char[] out, const char *const passwd, unsigned long long passwdlen, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_argon2i_str(
      @Out byte[] out,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit);

  // int crypto_pwhash_argon2i_str_verify(const char[] str, const char *const passwd, unsigned long long passwdlen);
  int crypto_pwhash_argon2i_str_verify(@In byte[] str, @In byte[] passwd, @In @u_int64_t long passwdlen);

  // int crypto_pwhash_argon2i_str_needs_rehash(const char[] str, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_argon2i_str_needs_rehash(@In byte[] str, @In @u_int64_t long opslimit, @In @ssize_t long memlimit);

  // int crypto_pwhash_argon2id_alg_argon2id13(void);
  int crypto_pwhash_argon2id_alg_argon2id13();

  // size_t crypto_pwhash_argon2id_bytes_min(void);
  @ssize_t
  long crypto_pwhash_argon2id_bytes_min();

  // size_t crypto_pwhash_argon2id_bytes_max(void);
  @ssize_t
  long crypto_pwhash_argon2id_bytes_max();

  // size_t crypto_pwhash_argon2id_passwd_min(void);
  @ssize_t
  long crypto_pwhash_argon2id_passwd_min();

  // size_t crypto_pwhash_argon2id_passwd_max(void);
  @ssize_t
  long crypto_pwhash_argon2id_passwd_max();

  // size_t crypto_pwhash_argon2id_saltbytes(void);
  @ssize_t
  long crypto_pwhash_argon2id_saltbytes();

  // size_t crypto_pwhash_argon2id_strbytes(void);
  @ssize_t
  long crypto_pwhash_argon2id_strbytes();

  // const char * crypto_pwhash_argon2id_strprefix(void);
  String crypto_pwhash_argon2id_strprefix();

  // size_t crypto_pwhash_argon2id_opslimit_min(void);
  @ssize_t
  long crypto_pwhash_argon2id_opslimit_min();

  // size_t crypto_pwhash_argon2id_opslimit_max(void);
  @ssize_t
  long crypto_pwhash_argon2id_opslimit_max();

  // size_t crypto_pwhash_argon2id_memlimit_min(void);
  @ssize_t
  long crypto_pwhash_argon2id_memlimit_min();

  // size_t crypto_pwhash_argon2id_memlimit_max(void);
  @ssize_t
  long crypto_pwhash_argon2id_memlimit_max();

  // size_t crypto_pwhash_argon2id_opslimit_interactive(void);
  @ssize_t
  long crypto_pwhash_argon2id_opslimit_interactive();

  // size_t crypto_pwhash_argon2id_memlimit_interactive(void);
  @ssize_t
  long crypto_pwhash_argon2id_memlimit_interactive();

  // size_t crypto_pwhash_argon2id_opslimit_moderate(void);
  @ssize_t
  long crypto_pwhash_argon2id_opslimit_moderate();

  // size_t crypto_pwhash_argon2id_memlimit_moderate(void);
  @ssize_t
  long crypto_pwhash_argon2id_memlimit_moderate();

  // size_t crypto_pwhash_argon2id_opslimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_argon2id_opslimit_sensitive();

  // size_t crypto_pwhash_argon2id_memlimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_argon2id_memlimit_sensitive();

  // int crypto_pwhash_argon2id(unsigned char *const out, unsigned long long outlen, const char *const passwd, unsigned long long passwdlen, const unsigned char *const salt, unsigned long long opslimit, size_t memlimit, int alg);
  int crypto_pwhash_argon2id(
      @Out byte[] out,
      @In @u_int64_t long outlen,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In byte[] salt,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit,
      @In int alg);

  // int crypto_pwhash_argon2id_str(char[] out, const char *const passwd, unsigned long long passwdlen, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_argon2id_str(
      @Out byte[] out,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit);

  // int crypto_pwhash_argon2id_str_verify(const char[] str, const char *const passwd, unsigned long long passwdlen);
  int crypto_pwhash_argon2id_str_verify(@In byte[] str, @In byte[] passwd, @In @u_int64_t long passwdlen);

  // int crypto_pwhash_argon2id_str_needs_rehash(const char[] str, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_argon2id_str_needs_rehash(@In byte[] str, @In @u_int64_t long opslimit, @In @ssize_t long memlimit);

  // int crypto_pwhash_alg_argon2i13(void);
  int crypto_pwhash_alg_argon2i13();

  // int crypto_pwhash_alg_argon2id13(void);
  int crypto_pwhash_alg_argon2id13();

  // int crypto_pwhash_alg_default(void);
  int crypto_pwhash_alg_default();

  // size_t crypto_pwhash_bytes_min(void);
  @ssize_t
  long crypto_pwhash_bytes_min();

  // size_t crypto_pwhash_bytes_max(void);
  @ssize_t
  long crypto_pwhash_bytes_max();

  // size_t crypto_pwhash_passwd_min(void);
  @ssize_t
  long crypto_pwhash_passwd_min();

  // size_t crypto_pwhash_passwd_max(void);
  @ssize_t
  long crypto_pwhash_passwd_max();

  // size_t crypto_pwhash_saltbytes(void);
  @ssize_t
  long crypto_pwhash_saltbytes();

  // size_t crypto_pwhash_strbytes(void);
  @ssize_t
  long crypto_pwhash_strbytes();

  // const char * crypto_pwhash_strprefix(void);
  String crypto_pwhash_strprefix();

  // size_t crypto_pwhash_opslimit_min(void);
  @ssize_t
  long crypto_pwhash_opslimit_min();

  // size_t crypto_pwhash_opslimit_max(void);
  @ssize_t
  long crypto_pwhash_opslimit_max();

  // size_t crypto_pwhash_memlimit_min(void);
  @ssize_t
  long crypto_pwhash_memlimit_min();

  // size_t crypto_pwhash_memlimit_max(void);
  @ssize_t
  long crypto_pwhash_memlimit_max();

  // size_t crypto_pwhash_opslimit_interactive(void);
  @ssize_t
  long crypto_pwhash_opslimit_interactive();

  // size_t crypto_pwhash_memlimit_interactive(void);
  @ssize_t
  long crypto_pwhash_memlimit_interactive();

  // size_t crypto_pwhash_opslimit_moderate(void);
  @ssize_t
  long crypto_pwhash_opslimit_moderate();

  // size_t crypto_pwhash_memlimit_moderate(void);
  @ssize_t
  long crypto_pwhash_memlimit_moderate();

  // size_t crypto_pwhash_opslimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_opslimit_sensitive();

  // size_t crypto_pwhash_memlimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_memlimit_sensitive();

  // int crypto_pwhash(unsigned char *const out, unsigned long long outlen, const char *const passwd, unsigned long long passwdlen, const unsigned char *const salt, unsigned long long opslimit, size_t memlimit, int alg);
  int crypto_pwhash(
      @Out byte[] out,
      @In @u_int64_t long outlen,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In Pointer salt,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit,
      @In int alg);

  // int crypto_pwhash_str(char[] out, const char *const passwd, unsigned long long passwdlen, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_str(
      @Out byte[] out,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit);

  // int crypto_pwhash_str_alg(char[] out, const char *const passwd, unsigned long long passwdlen, unsigned long long opslimit, size_t memlimit, int alg);
  int crypto_pwhash_str_alg(
      @Out byte[] out,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit,
      @In int alg);

  // int crypto_pwhash_str_verify(const char[] str, const char *const passwd, unsigned long long passwdlen);
  int crypto_pwhash_str_verify(@In Pointer str, @In byte[] passwd, @In @u_int64_t long passwdlen);

  // int crypto_pwhash_str_needs_rehash(const char[] str, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_str_needs_rehash(@In Pointer str, @In @u_int64_t long opslimit, @In @ssize_t long memlimit);

  // const char * crypto_pwhash_primitive(void);
  String crypto_pwhash_primitive();

  // size_t crypto_scalarmult_curve25519_bytes(void);
  @ssize_t
  long crypto_scalarmult_curve25519_bytes();

  // size_t crypto_scalarmult_curve25519_scalarbytes(void);
  @ssize_t
  long crypto_scalarmult_curve25519_scalarbytes();

  // int crypto_scalarmult_curve25519(unsigned char * q, const unsigned char * n, const unsigned char * p);
  int crypto_scalarmult_curve25519(@Out byte[] q, @In byte[] n, @In byte[] p);

  // int crypto_scalarmult_curve25519_base(unsigned char * q, const unsigned char * n);
  int crypto_scalarmult_curve25519_base(@Out byte[] q, @In byte[] n);

  // size_t crypto_scalarmult_bytes(void);
  @ssize_t
  long crypto_scalarmult_bytes();

  // size_t crypto_scalarmult_scalarbytes(void);
  @ssize_t
  long crypto_scalarmult_scalarbytes();

  // const char * crypto_scalarmult_primitive(void);
  String crypto_scalarmult_primitive();

  // int crypto_scalarmult_base(unsigned char * q, const unsigned char * n);
  int crypto_scalarmult_base(@Out Pointer q, @In Pointer n);

  // int crypto_scalarmult(unsigned char * q, const unsigned char * n, const unsigned char * p);
  int crypto_scalarmult(@Out Pointer q, @In Pointer n, @In Pointer p);

  // size_t crypto_secretbox_xsalsa20poly1305_keybytes(void);
  @ssize_t
  long crypto_secretbox_xsalsa20poly1305_keybytes();

  // size_t crypto_secretbox_xsalsa20poly1305_noncebytes(void);
  @ssize_t
  long crypto_secretbox_xsalsa20poly1305_noncebytes();

  // size_t crypto_secretbox_xsalsa20poly1305_macbytes(void);
  @ssize_t
  long crypto_secretbox_xsalsa20poly1305_macbytes();

  // size_t crypto_secretbox_xsalsa20poly1305_messagebytes_max(void);
  @ssize_t
  long crypto_secretbox_xsalsa20poly1305_messagebytes_max();

  // int crypto_secretbox_xsalsa20poly1305(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_xsalsa20poly1305(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] k);

  // int crypto_secretbox_xsalsa20poly1305_open(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_xsalsa20poly1305_open(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] k);

  // void crypto_secretbox_xsalsa20poly1305_keygen(unsigned char[] k);
  void crypto_secretbox_xsalsa20poly1305_keygen(@Out byte[] k);

  // size_t crypto_secretbox_xsalsa20poly1305_boxzerobytes(void);
  @ssize_t
  long crypto_secretbox_xsalsa20poly1305_boxzerobytes();

  // size_t crypto_secretbox_xsalsa20poly1305_zerobytes(void);
  @ssize_t
  long crypto_secretbox_xsalsa20poly1305_zerobytes();

  // size_t crypto_secretbox_keybytes(void);
  @ssize_t
  long crypto_secretbox_keybytes();

  // size_t crypto_secretbox_noncebytes(void);
  @ssize_t
  long crypto_secretbox_noncebytes();

  // size_t crypto_secretbox_macbytes(void);
  @ssize_t
  long crypto_secretbox_macbytes();

  // const char * crypto_secretbox_primitive(void);
  String crypto_secretbox_primitive();

  // size_t crypto_secretbox_messagebytes_max(void);
  @ssize_t
  long crypto_secretbox_messagebytes_max();

  // int crypto_secretbox_easy(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_easy(@Out Pointer c, @In Pointer m, @In @u_int64_t long mlen, @In Pointer n, @In Pointer k);

  // int crypto_secretbox_easy(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_easy(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In Pointer n, @In Pointer k);

  // int crypto_secretbox_open_easy(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_open_easy(@Out byte[] m, @In byte[] c, @In @u_int64_t long clen, @In Pointer n, @In Pointer k);

  // int crypto_secretbox_open_easy(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_open_easy(@Out Pointer m, @In Pointer c, @In @u_int64_t long clen, @In Pointer n, @In Pointer k);

  // int crypto_secretbox_detached(unsigned char * c, unsigned char * mac, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_detached(
      @Out byte[] c,
      @Out byte[] mac,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In Pointer n,
      @In Pointer k);

  // int crypto_secretbox_open_detached(unsigned char * m, const unsigned char * c, const unsigned char * mac, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_open_detached(
      @Out byte[] m,
      @In byte[] c,
      @In byte[] mac,
      @In @u_int64_t long clen,
      @In Pointer n,
      @In Pointer k);

  // void crypto_secretbox_keygen(unsigned char[] k);
  void crypto_secretbox_keygen(@Out Pointer k);

  // size_t crypto_secretbox_zerobytes(void);
  @ssize_t
  long crypto_secretbox_zerobytes();

  // size_t crypto_secretbox_boxzerobytes(void);
  @ssize_t
  long crypto_secretbox_boxzerobytes();

  // int crypto_secretbox(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // int crypto_secretbox_open(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_open(@Out byte[] m, @In byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // size_t crypto_stream_chacha20_keybytes(void);
  @ssize_t
  long crypto_stream_chacha20_keybytes();

  // size_t crypto_stream_chacha20_noncebytes(void);
  @ssize_t
  long crypto_stream_chacha20_noncebytes();

  // size_t crypto_stream_chacha20_messagebytes_max(void);
  @ssize_t
  long crypto_stream_chacha20_messagebytes_max();

  // int crypto_stream_chacha20(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_chacha20(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_chacha20_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_chacha20_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // int crypto_stream_chacha20_xor_ic(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, uint64_t ic, const unsigned char * k);
  int crypto_stream_chacha20_xor_ic(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In @u_int64_t long ic,
      @In byte[] k);

  // void crypto_stream_chacha20_keygen(unsigned char[] k);
  void crypto_stream_chacha20_keygen(@Out byte[] k);

  // size_t crypto_stream_chacha20_ietf_keybytes(void);
  @ssize_t
  long crypto_stream_chacha20_ietf_keybytes();

  // size_t crypto_stream_chacha20_ietf_noncebytes(void);
  @ssize_t
  long crypto_stream_chacha20_ietf_noncebytes();

  // size_t crypto_stream_chacha20_ietf_messagebytes_max(void);
  @ssize_t
  long crypto_stream_chacha20_ietf_messagebytes_max();

  // int crypto_stream_chacha20_ietf(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_chacha20_ietf(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_chacha20_ietf_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_chacha20_ietf_xor(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] k);

  // int crypto_stream_chacha20_ietf_xor_ic(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, uint32_t ic, const unsigned char * k);
  int crypto_stream_chacha20_ietf_xor_ic(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In @u_int32_t int ic,
      @In byte[] k);

  // void crypto_stream_chacha20_ietf_keygen(unsigned char[] k);
  void crypto_stream_chacha20_ietf_keygen(@Out byte[] k);

  // size_t crypto_secretstream_xchacha20poly1305_abytes(void);
  @ssize_t
  long crypto_secretstream_xchacha20poly1305_abytes();

  // size_t crypto_secretstream_xchacha20poly1305_headerbytes(void);
  @ssize_t
  long crypto_secretstream_xchacha20poly1305_headerbytes();

  // size_t crypto_secretstream_xchacha20poly1305_keybytes(void);
  @ssize_t
  long crypto_secretstream_xchacha20poly1305_keybytes();

  // size_t crypto_secretstream_xchacha20poly1305_messagebytes_max(void);
  @ssize_t
  long crypto_secretstream_xchacha20poly1305_messagebytes_max();

  // unsigned char crypto_secretstream_xchacha20poly1305_tag_message(void);
  @u_int8_t
  char crypto_secretstream_xchacha20poly1305_tag_message();

  // unsigned char crypto_secretstream_xchacha20poly1305_tag_push(void);
  @u_int8_t
  char crypto_secretstream_xchacha20poly1305_tag_push();

  // unsigned char crypto_secretstream_xchacha20poly1305_tag_rekey(void);
  @u_int8_t
  char crypto_secretstream_xchacha20poly1305_tag_rekey();

  // unsigned char crypto_secretstream_xchacha20poly1305_tag_final(void);
  @u_int8_t
  char crypto_secretstream_xchacha20poly1305_tag_final();

  // size_t crypto_secretstream_xchacha20poly1305_statebytes(void);
  @ssize_t
  long crypto_secretstream_xchacha20poly1305_statebytes();

  // void crypto_secretstream_xchacha20poly1305_keygen(unsigned char[] k);
  void crypto_secretstream_xchacha20poly1305_keygen(@Out Pointer k);

  // int crypto_secretstream_xchacha20poly1305_init_push(crypto_secretstream_xchacha20poly1305_state * state, unsigned char[] header, const unsigned char[] k);
  int crypto_secretstream_xchacha20poly1305_init_push(/*both*/ Pointer state, @Out byte[] header, @In Pointer k);

  // int crypto_secretstream_xchacha20poly1305_push(crypto_secretstream_xchacha20poly1305_state * state, unsigned char * c, unsigned long long * clen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * ad, unsigned long long adlen, unsigned char tag);
  int crypto_secretstream_xchacha20poly1305_push(
      /*both*/ Pointer state,
      @Out byte[] c,
      @Out LongLongByReference clen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] ad,
      @In @u_int64_t long adlen,
      @In @u_int8_t byte tag);

  // int crypto_secretstream_xchacha20poly1305_init_pull(crypto_secretstream_xchacha20poly1305_state * state, const unsigned char[] header, const unsigned char[] k);
  int crypto_secretstream_xchacha20poly1305_init_pull(/*both*/ Pointer state, @In byte[] header, @In Pointer k);

  // int crypto_secretstream_xchacha20poly1305_pull(crypto_secretstream_xchacha20poly1305_state * state, unsigned char * m, unsigned long long * mlen_p, unsigned char * tag_p, const unsigned char * c, unsigned long long clen, const unsigned char * ad, unsigned long long adlen);
  int crypto_secretstream_xchacha20poly1305_pull(
      /*both*/ Pointer state,
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      @Out ByteByReference tag_p,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] ad,
      @In @u_int64_t long adlen);

  // void crypto_secretstream_xchacha20poly1305_rekey(crypto_secretstream_xchacha20poly1305_state * state);
  void crypto_secretstream_xchacha20poly1305_rekey(/*both*/ Pointer state);

  // size_t crypto_shorthash_siphash24_bytes(void);
  @ssize_t
  long crypto_shorthash_siphash24_bytes();

  // size_t crypto_shorthash_siphash24_keybytes(void);
  @ssize_t
  long crypto_shorthash_siphash24_keybytes();

  // int crypto_shorthash_siphash24(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_shorthash_siphash24(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // size_t crypto_shorthash_siphashx24_bytes(void);
  @ssize_t
  long crypto_shorthash_siphashx24_bytes();

  // size_t crypto_shorthash_siphashx24_keybytes(void);
  @ssize_t
  long crypto_shorthash_siphashx24_keybytes();

  // int crypto_shorthash_siphashx24(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_shorthash_siphashx24(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // size_t crypto_shorthash_bytes(void);
  @ssize_t
  long crypto_shorthash_bytes();

  // size_t crypto_shorthash_keybytes(void);
  @ssize_t
  long crypto_shorthash_keybytes();

  // const char * crypto_shorthash_primitive(void);
  String crypto_shorthash_primitive();

  // int crypto_shorthash(unsigned char * out, const unsigned char * in, unsigned long long inlen, const unsigned char * k);
  int crypto_shorthash(@Out byte[] out, @In byte[] in, @In @u_int64_t long inlen, @In byte[] k);

  // void crypto_shorthash_keygen(unsigned char[] k);
  void crypto_shorthash_keygen(@Out byte[] k);

  // size_t crypto_sign_ed25519ph_statebytes(void);
  @ssize_t
  long crypto_sign_ed25519ph_statebytes();

  // size_t crypto_sign_ed25519_bytes(void);
  @ssize_t
  long crypto_sign_ed25519_bytes();

  // size_t crypto_sign_ed25519_seedbytes(void);
  @ssize_t
  long crypto_sign_ed25519_seedbytes();

  // size_t crypto_sign_ed25519_publickeybytes(void);
  @ssize_t
  long crypto_sign_ed25519_publickeybytes();

  // size_t crypto_sign_ed25519_secretkeybytes(void);
  @ssize_t
  long crypto_sign_ed25519_secretkeybytes();

  // size_t crypto_sign_ed25519_messagebytes_max(void);
  @ssize_t
  long crypto_sign_ed25519_messagebytes_max();

  // int crypto_sign_ed25519(unsigned char * sm, unsigned long long * smlen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * sk);
  int crypto_sign_ed25519(
      @Out byte[] sm,
      @Out LongLongByReference smlen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] sk);

  // int crypto_sign_ed25519_open(unsigned char * m, unsigned long long * mlen_p, const unsigned char * sm, unsigned long long smlen, const unsigned char * pk);
  int crypto_sign_ed25519_open(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      @In byte[] sm,
      @In @u_int64_t long smlen,
      @In byte[] pk);

  // int crypto_sign_ed25519_detached(unsigned char * sig, unsigned long long * siglen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * sk);
  int crypto_sign_ed25519_detached(
      @Out byte[] sig,
      @Out LongLongByReference siglen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] sk);

  // int crypto_sign_ed25519_verify_detached(const unsigned char * sig, const unsigned char * m, unsigned long long mlen, const unsigned char * pk);
  int crypto_sign_ed25519_verify_detached(@In byte[] sig, @In byte[] m, @In @u_int64_t long mlen, @In byte[] pk);

  // int crypto_sign_ed25519_keypair(unsigned char * pk, unsigned char * sk);
  int crypto_sign_ed25519_keypair(@Out byte[] pk, @Out byte[] sk);

  // int crypto_sign_ed25519_seed_keypair(unsigned char * pk, unsigned char * sk, const unsigned char * seed);
  int crypto_sign_ed25519_seed_keypair(@Out byte[] pk, @Out byte[] sk, @In byte[] seed);

  // int crypto_sign_ed25519_pk_to_curve25519(unsigned char * curve25519_pk, const unsigned char * ed25519_pk);
  int crypto_sign_ed25519_pk_to_curve25519(@Out Pointer curve25519_pk, @In Pointer ed25519_pk);

  // int crypto_sign_ed25519_sk_to_curve25519(unsigned char * curve25519_sk, const unsigned char * ed25519_sk);
  int crypto_sign_ed25519_sk_to_curve25519(@Out Pointer curve25519_sk, @In Pointer ed25519_sk);

  // int crypto_sign_ed25519_sk_to_seed(unsigned char * seed, const unsigned char * sk);
  int crypto_sign_ed25519_sk_to_seed(@Out byte[] seed, @In byte[] sk);

  // int crypto_sign_ed25519_sk_to_pk(unsigned char * pk, const unsigned char * sk);
  int crypto_sign_ed25519_sk_to_pk(@Out Pointer pk, @In Pointer sk);

  // int crypto_sign_ed25519ph_init(crypto_sign_ed25519ph_state * state);
  int crypto_sign_ed25519ph_init(@Out Pointer state);

  // int crypto_sign_ed25519ph_update(crypto_sign_ed25519ph_state * state, const unsigned char * m, unsigned long long mlen);
  int crypto_sign_ed25519ph_update(/*both*/ Pointer state, @In byte[] m, @In @u_int64_t long mlen);

  // int crypto_sign_ed25519ph_final_create(crypto_sign_ed25519ph_state * state, unsigned char * sig, unsigned long long * siglen_p, const unsigned char * sk);
  int crypto_sign_ed25519ph_final_create(
      /*both*/ Pointer state,
      @Out byte[] sig,
      @Out LongLongByReference siglen_p,
      @In byte[] sk);

  // int crypto_sign_ed25519ph_final_verify(crypto_sign_ed25519ph_state * state, unsigned char * sig, const unsigned char * pk);
  int crypto_sign_ed25519ph_final_verify(/*both*/ Pointer state, @In byte[] sig, @In byte[] pk);

  // size_t crypto_sign_statebytes(void);
  @ssize_t
  long crypto_sign_statebytes();

  // size_t crypto_sign_bytes(void);
  @ssize_t
  long crypto_sign_bytes();

  // size_t crypto_sign_seedbytes(void);
  @ssize_t
  long crypto_sign_seedbytes();

  // size_t crypto_sign_publickeybytes(void);
  @ssize_t
  long crypto_sign_publickeybytes();

  // size_t crypto_sign_secretkeybytes(void);
  @ssize_t
  long crypto_sign_secretkeybytes();

  // size_t crypto_sign_messagebytes_max(void);
  @ssize_t
  long crypto_sign_messagebytes_max();

  // const char * crypto_sign_primitive(void);
  String crypto_sign_primitive();

  // int crypto_sign_seed_keypair(unsigned char * pk, unsigned char * sk, const unsigned char * seed);
  int crypto_sign_seed_keypair(@Out Pointer pk, @Out Pointer sk, @In Pointer seed);

  // int crypto_sign_keypair(unsigned char * pk, unsigned char * sk);
  int crypto_sign_keypair(@Out Pointer pk, @Out Pointer sk);

  // int crypto_sign(unsigned char * sm, unsigned long long * smlen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * sk);
  int crypto_sign(
      @Out byte[] sm,
      @Nullable @Out LongLongByReference smlen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In Pointer sk);

  // int crypto_sign_open(unsigned char * m, unsigned long long * mlen_p, const unsigned char * sm, unsigned long long smlen, const unsigned char * pk);
  int crypto_sign_open(
      @Out byte[] m,
      @Out LongLongByReference mlen_p,
      @In byte[] sm,
      @In @u_int64_t long smlen,
      @In Pointer pk);

  // int crypto_sign_detached(unsigned char * sig, unsigned long long * siglen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * sk);
  int crypto_sign_detached(
      @Out byte[] sig,
      @Nullable @Out LongLongByReference siglen_p,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In Pointer sk);

  // int crypto_sign_detached(unsigned char * sig, unsigned long long * siglen_p, const unsigned char * m, unsigned long long mlen, const unsigned char * sk);
  int crypto_sign_detached(
      @Out Pointer sig,
      @Nullable @Out LongLongByReference siglen_p,
      @In Pointer m,
      @In @u_int64_t long mlen,
      @In Pointer sk);

  // int crypto_sign_verify_detached(const unsigned char * sig, const unsigned char * m, unsigned long long mlen, const unsigned char * pk);
  int crypto_sign_verify_detached(@In Pointer sig, @In Pointer m, @In @u_int64_t long mlen, @In Pointer pk);

  // int crypto_sign_verify_detached(const unsigned char * sig, const unsigned char * m, unsigned long long mlen, const unsigned char * pk);
  int crypto_sign_verify_detached(@In byte[] sig, @In byte[] m, @In @u_int64_t long mlen, @In Pointer pk);

  // int crypto_sign_init(crypto_sign_state * state);
  int crypto_sign_init(@Out Pointer state);

  // int crypto_sign_update(crypto_sign_state * state, const unsigned char * m, unsigned long long mlen);
  int crypto_sign_update(/*both*/ Pointer state, @In byte[] m, @In @u_int64_t long mlen);

  // int crypto_sign_final_create(crypto_sign_state * state, unsigned char * sig, unsigned long long * siglen_p, const unsigned char * sk);
  int crypto_sign_final_create(
      /*both*/ Pointer state,
      @Out byte[] sig,
      @Out LongLongByReference siglen_p,
      @In byte[] sk);

  // int crypto_sign_final_verify(crypto_sign_state * state, unsigned char * sig, const unsigned char * pk);
  int crypto_sign_final_verify(/*both*/ Pointer state, @In byte[] sig, @In byte[] pk);

  // size_t crypto_stream_keybytes(void);
  @ssize_t
  long crypto_stream_keybytes();

  // size_t crypto_stream_noncebytes(void);
  @ssize_t
  long crypto_stream_noncebytes();

  // size_t crypto_stream_messagebytes_max(void);
  @ssize_t
  long crypto_stream_messagebytes_max();

  // const char * crypto_stream_primitive(void);
  String crypto_stream_primitive();

  // int crypto_stream(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // void crypto_stream_keygen(unsigned char[] k);
  void crypto_stream_keygen(@Out byte[] k);

  // size_t crypto_stream_salsa20_keybytes(void);
  @ssize_t
  long crypto_stream_salsa20_keybytes();

  // size_t crypto_stream_salsa20_noncebytes(void);
  @ssize_t
  long crypto_stream_salsa20_noncebytes();

  // size_t crypto_stream_salsa20_messagebytes_max(void);
  @ssize_t
  long crypto_stream_salsa20_messagebytes_max();

  // int crypto_stream_salsa20(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_salsa20(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_salsa20_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_salsa20_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // int crypto_stream_salsa20_xor_ic(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, uint64_t ic, const unsigned char * k);
  int crypto_stream_salsa20_xor_ic(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In @u_int64_t long ic,
      @In byte[] k);

  // void crypto_stream_salsa20_keygen(unsigned char[] k);
  void crypto_stream_salsa20_keygen(@Out byte[] k);

  // size_t crypto_verify_16_bytes(void);
  @ssize_t
  long crypto_verify_16_bytes();

  // int crypto_verify_16(const unsigned char * x, const unsigned char * y);
  int crypto_verify_16(@In byte[] x, @In byte[] y);

  // size_t crypto_verify_32_bytes(void);
  @ssize_t
  long crypto_verify_32_bytes();

  // int crypto_verify_32(const unsigned char * x, const unsigned char * y);
  int crypto_verify_32(@In byte[] x, @In byte[] y);

  // size_t crypto_verify_64_bytes(void);
  @ssize_t
  long crypto_verify_64_bytes();

  // int crypto_verify_64(const unsigned char * x, const unsigned char * y);
  int crypto_verify_64(@In byte[] x, @In byte[] y);

  // const char * implementation_name(void);
  String implementation_name();

  // uint32_t random(void);
  @u_int32_t
  int random();

  // void stir(void);
  void stir();

  // uint32_t uniform(const uint32_t upper_bound);
  @u_int32_t
  int uniform(@In @u_int32_t int upper_bound);

  // void buf(void *const buf, const size_t size);
  void buf(/*@In @Out*/ byte[] buf, @In @ssize_t long size);

  // int close(void);
  int close();

  // size_t randombytes_seedbytes(void);
  @ssize_t
  long randombytes_seedbytes();

  // void randombytes_buf(void *const buf, const size_t size);
  void randombytes_buf(@Out Pointer buf, @In @ssize_t long size);

  // void randombytes_buf_deterministic(void *const buf, const size_t size, const unsigned char[] seed);
  void randombytes_buf_deterministic(@Out byte[] buf, @In @ssize_t long size, @In byte[] seed);

  // uint32_t randombytes_random(void);
  @u_int32_t
  int randombytes_random();

  // uint32_t randombytes_uniform(const uint32_t upper_bound);
  @u_int32_t
  int randombytes_uniform(@In @u_int32_t int upper_bound);

  // void randombytes_stir(void);
  void randombytes_stir();

  // int randombytes_close(void);
  int randombytes_close();

  // int randombytes_set_implementation(randombytes_implementation * impl);
  int randombytes_set_implementation(/*@In @Out*/ Pointer impl);

  // const char * randombytes_implementation_name(void);
  String randombytes_implementation_name();

  // void randombytes(unsigned char *const buf, const unsigned long long buf_len);
  void randombytes(@Out byte[] buf, @In @u_int64_t long buf_len);

  // int sodium_runtime_has_neon(void);
  int sodium_runtime_has_neon();

  // int sodium_runtime_has_sse2(void);
  int sodium_runtime_has_sse2();

  // int sodium_runtime_has_sse3(void);
  int sodium_runtime_has_sse3();

  // int sodium_runtime_has_ssse3(void);
  int sodium_runtime_has_ssse3();

  // int sodium_runtime_has_sse41(void);
  int sodium_runtime_has_sse41();

  // int sodium_runtime_has_avx(void);
  int sodium_runtime_has_avx();

  // int sodium_runtime_has_avx2(void);
  int sodium_runtime_has_avx2();

  // int sodium_runtime_has_avx512f(void);
  int sodium_runtime_has_avx512f();

  // int sodium_runtime_has_pclmul(void);
  int sodium_runtime_has_pclmul();

  // int sodium_runtime_has_aesni(void);
  int sodium_runtime_has_aesni();

  // int sodium_runtime_has_rdrand(void);
  int sodium_runtime_has_rdrand();

  // int _sodium_runtime_get_cpu_features(void);
  int _sodium_runtime_get_cpu_features();

  // void sodium_memzero(void *const pnt, const size_t len);
  void sodium_memzero(/*@In @Out*/ Pointer pnt, @In @ssize_t long len);

  // void sodium_stackzero(const size_t len);
  void sodium_stackzero(@In @ssize_t long len);

  // int sodium_memcmp(const void *const b1_, const void *const b2_, size_t len);
  int sodium_memcmp(@In Pointer b1_, @In Pointer b2_, @In @ssize_t long len);

  // int sodium_compare(const unsigned char * b1_, const unsigned char * b2_, size_t len);
  int sodium_compare(@In Pointer b1_, @In Pointer b2_, @In @ssize_t long len);

  // int sodium_is_zero(const unsigned char * n, const size_t nlen);
  int sodium_is_zero(@In Pointer n, @In @ssize_t long nlen);

  // void sodium_increment(unsigned char * n, const size_t nlen);
  void sodium_increment(/*@In @Out*/ Pointer n, @In @ssize_t long nlen);

  // void sodium_add(unsigned char * a, const unsigned char * b, const size_t len);
  void sodium_add(/*@In @Out*/ Pointer a, @In Pointer b, @In @ssize_t long len);

  // void sodium_sub(unsigned char * a, const unsigned char * b, const size_t len);
  void sodium_sub(/*@In @Out*/ byte[] a, @In byte[] b, @In @ssize_t long len);

  // char * sodium_bin2hex(char *const hex, const size_t hex_maxlen, const unsigned char *const bin, const size_t bin_len);
  // FIXME: JNR-FFI code generation fails for this method
  //  byte[] sodium_bin2hex(@Out byte[] hex, @In @ssize_t long hex_maxlen, @In byte[] bin, @In @ssize_t long bin_len);

  // int sodium_hex2bin(unsigned char *const bin, const size_t bin_maxlen, const char *const hex, const size_t hex_len, const char *const ignore, size_t *const bin_len, const char * *const hex_end);
  int sodium_hex2bin(
      @Out byte[] bin,
      @In @ssize_t long bin_maxlen,
      @In byte[] hex,
      @In @ssize_t long hex_len,
      @In byte[] ignore,
      @Out LongLongByReference bin_len,
      /*@In @Out*/ Pointer hex_end);

  // size_t sodium_base64_encoded_len(const size_t bin_len, const int variant);
  @ssize_t
  long sodium_base64_encoded_len(@In @ssize_t long bin_len, @In int variant);

  // char * sodium_bin2base64(char *const b64, const size_t b64_maxlen, const unsigned char *const bin, const size_t bin_len, const int variant);
  // FIXME: JNR-FFI code generation fails for this method
  //  byte[] sodium_bin2base64(
  //      @Out byte[] b64,
  //      @In @ssize_t long b64_maxlen,
  //      @In byte[] bin,
  //      @In @ssize_t long bin_len,
  //      @In int variant);

  // int sodium_base642bin(unsigned char *const bin, const size_t bin_maxlen, const char *const b64, const size_t b64_len, const char *const ignore, size_t *const bin_len, const char * *const b64_end, const int variant);
  int sodium_base642bin(
      @Out byte[] bin,
      @In @ssize_t long bin_maxlen,
      @In byte[] b64,
      @In @ssize_t long b64_len,
      @In byte[] ignore,
      @Out LongLongByReference bin_len,
      /*@In @Out*/ Pointer b64_end,
      @In int variant);

  // int sodium_mlock(void *const addr, const size_t len);
  int sodium_mlock(/*@In @Out*/ Pointer addr, @In @ssize_t long len);

  // int sodium_munlock(void *const addr, const size_t len);
  int sodium_munlock(/*@In @Out*/ Pointer addr, @In @ssize_t long len);

  // void * sodium_malloc(const size_t size);
  Pointer sodium_malloc(@In @ssize_t long size);

  // void * sodium_allocarray(size_t count, size_t size);
  Pointer sodium_allocarray(@In @ssize_t long count, @In @ssize_t long size);

  // void sodium_free(void * ptr);
  void sodium_free(/*@In @Out*/ Pointer ptr);

  // int sodium_mprotect_noaccess(void * ptr);
  int sodium_mprotect_noaccess(/*@In @Out*/ Pointer ptr);

  // int sodium_mprotect_readonly(void * ptr);
  int sodium_mprotect_readonly(/*@In @Out*/ Pointer ptr);

  // int sodium_mprotect_readwrite(void * ptr);
  int sodium_mprotect_readwrite(/*@In @Out*/ Pointer ptr);

  // int sodium_pad(size_t * padded_buflen_p, unsigned char * buf, size_t unpadded_buflen, size_t blocksize, size_t max_buflen);
  int sodium_pad(
      @Out LongLongByReference padded_buflen_p,
      /*@In @Out*/ byte[] buf,
      @In @ssize_t long unpadded_buflen,
      @In @ssize_t long blocksize,
      @In @ssize_t long max_buflen);

  // int sodium_unpad(size_t * unpadded_buflen_p, const unsigned char * buf, size_t padded_buflen, size_t blocksize);
  int sodium_unpad(
      @Out LongLongByReference unpadded_buflen_p,
      @In byte[] buf,
      @In @ssize_t long padded_buflen,
      @In @ssize_t long blocksize);

  // int _sodium_alloc_init(void);
  int _sodium_alloc_init();

  // size_t crypto_stream_xchacha20_keybytes(void);
  @ssize_t
  long crypto_stream_xchacha20_keybytes();

  // size_t crypto_stream_xchacha20_noncebytes(void);
  @ssize_t
  long crypto_stream_xchacha20_noncebytes();

  // size_t crypto_stream_xchacha20_messagebytes_max(void);
  @ssize_t
  long crypto_stream_xchacha20_messagebytes_max();

  // int crypto_stream_xchacha20(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_xchacha20(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_xchacha20_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_xchacha20_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // int crypto_stream_xchacha20_xor_ic(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, uint64_t ic, const unsigned char * k);
  int crypto_stream_xchacha20_xor_ic(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In @u_int64_t long ic,
      @In byte[] k);

  // void crypto_stream_xchacha20_keygen(unsigned char[] k);
  void crypto_stream_xchacha20_keygen(@Out byte[] k);

  // size_t crypto_box_curve25519xchacha20poly1305_seedbytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_seedbytes();

  // size_t crypto_box_curve25519xchacha20poly1305_publickeybytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_publickeybytes();

  // size_t crypto_box_curve25519xchacha20poly1305_secretkeybytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_secretkeybytes();

  // size_t crypto_box_curve25519xchacha20poly1305_beforenmbytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_beforenmbytes();

  // size_t crypto_box_curve25519xchacha20poly1305_noncebytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_noncebytes();

  // size_t crypto_box_curve25519xchacha20poly1305_macbytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_macbytes();

  // size_t crypto_box_curve25519xchacha20poly1305_messagebytes_max(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_messagebytes_max();

  // int crypto_box_curve25519xchacha20poly1305_seed_keypair(unsigned char * pk, unsigned char * sk, const unsigned char * seed);
  int crypto_box_curve25519xchacha20poly1305_seed_keypair(@Out byte[] pk, @Out byte[] sk, @In byte[] seed);

  // int crypto_box_curve25519xchacha20poly1305_keypair(unsigned char * pk, unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_keypair(@Out byte[] pk, @Out byte[] sk);

  // int crypto_box_curve25519xchacha20poly1305_easy(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_easy(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_curve25519xchacha20poly1305_open_easy(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_open_easy(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_curve25519xchacha20poly1305_detached(unsigned char * c, unsigned char * mac, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_detached(
      @Out byte[] c,
      /*@In @Out*/ byte[] mac,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_curve25519xchacha20poly1305_open_detached(unsigned char * m, const unsigned char * c, const unsigned char * mac, unsigned long long clen, const unsigned char * n, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_open_detached(
      @Out byte[] m,
      @In byte[] c,
      @In byte[] mac,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] pk,
      @In byte[] sk);

  // int crypto_box_curve25519xchacha20poly1305_beforenm(unsigned char * k, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_beforenm(@Out Pointer k, @In byte[] pk, @In byte[] sk);

  // int crypto_box_curve25519xchacha20poly1305_easy_afternm(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_box_curve25519xchacha20poly1305_easy_afternm(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In Pointer k);

  // int crypto_box_curve25519xchacha20poly1305_open_easy_afternm(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_box_curve25519xchacha20poly1305_open_easy_afternm(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In Pointer k);

  // int crypto_box_curve25519xchacha20poly1305_detached_afternm(unsigned char * c, unsigned char * mac, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_box_curve25519xchacha20poly1305_detached_afternm(
      @Out byte[] c,
      /*@In @Out*/ byte[] mac,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In Pointer k);

  // int crypto_box_curve25519xchacha20poly1305_open_detached_afternm(unsigned char * m, const unsigned char * c, const unsigned char * mac, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_box_curve25519xchacha20poly1305_open_detached_afternm(
      @Out byte[] m,
      @In byte[] c,
      @In byte[] mac,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In Pointer k);

  // size_t crypto_box_curve25519xchacha20poly1305_sealbytes(void);
  @ssize_t
  long crypto_box_curve25519xchacha20poly1305_sealbytes();

  // int crypto_box_curve25519xchacha20poly1305_seal(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * pk);
  int crypto_box_curve25519xchacha20poly1305_seal(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] pk);

  // int crypto_box_curve25519xchacha20poly1305_seal_open(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * pk, const unsigned char * sk);
  int crypto_box_curve25519xchacha20poly1305_seal_open(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] pk,
      @In byte[] sk);

  // size_t crypto_core_ed25519_bytes(void);
  @ssize_t
  long crypto_core_ed25519_bytes();

  // size_t crypto_core_ed25519_uniformbytes(void);
  @ssize_t
  long crypto_core_ed25519_uniformbytes();

  // size_t crypto_core_ed25519_hashbytes(void);
  @ssize_t
  long crypto_core_ed25519_hashbytes();

  // size_t crypto_core_ed25519_scalarbytes(void);
  @ssize_t
  long crypto_core_ed25519_scalarbytes();

  // size_t crypto_core_ed25519_nonreducedscalarbytes(void);
  @ssize_t
  long crypto_core_ed25519_nonreducedscalarbytes();

  // int crypto_core_ed25519_is_valid_point(const unsigned char * p);
  int crypto_core_ed25519_is_valid_point(@In byte[] p);

  // int crypto_core_ed25519_add(unsigned char * r, const unsigned char * p, const unsigned char * q);
  int crypto_core_ed25519_add(@Out byte[] r, @In byte[] p, @In byte[] q);

  // int crypto_core_ed25519_sub(unsigned char * r, const unsigned char * p, const unsigned char * q);
  int crypto_core_ed25519_sub(@Out byte[] r, @In byte[] p, @In byte[] q);

  // int crypto_core_ed25519_from_uniform(unsigned char * p, const unsigned char * r);
  int crypto_core_ed25519_from_uniform(@Out byte[] p, @In byte[] r);

  // int crypto_core_ed25519_from_hash(unsigned char * p, const unsigned char * h);
  int crypto_core_ed25519_from_hash(@Out byte[] p, @In byte[] h);

  // void crypto_core_ed25519_random(unsigned char * p);
  void crypto_core_ed25519_random(@Out byte[] p);

  // void crypto_core_ed25519_scalar_random(unsigned char * r);
  void crypto_core_ed25519_scalar_random(@Out byte[] r);

  // int crypto_core_ed25519_scalar_invert(unsigned char * recip, const unsigned char * s);
  int crypto_core_ed25519_scalar_invert(@Out byte[] recip, @In byte[] s);

  // void crypto_core_ed25519_scalar_negate(unsigned char * neg, const unsigned char * s);
  void crypto_core_ed25519_scalar_negate(@Out byte[] neg, @In byte[] s);

  // void crypto_core_ed25519_scalar_complement(unsigned char * comp, const unsigned char * s);
  void crypto_core_ed25519_scalar_complement(@Out byte[] comp, @In byte[] s);

  // void crypto_core_ed25519_scalar_add(unsigned char * z, const unsigned char * x, const unsigned char * y);
  void crypto_core_ed25519_scalar_add(@Out byte[] z, @In byte[] x, @In byte[] y);

  // void crypto_core_ed25519_scalar_sub(unsigned char * z, const unsigned char * x, const unsigned char * y);
  void crypto_core_ed25519_scalar_sub(@Out byte[] z, @In byte[] x, @In byte[] y);

  // void crypto_core_ed25519_scalar_mul(unsigned char * z, const unsigned char * x, const unsigned char * y);
  void crypto_core_ed25519_scalar_mul(@Out byte[] z, @In byte[] x, @In byte[] y);

  // void crypto_core_ed25519_scalar_reduce(unsigned char * r, const unsigned char * s);
  void crypto_core_ed25519_scalar_reduce(@Out byte[] r, @In byte[] s);

  // size_t crypto_core_ristretto255_bytes(void);
  @ssize_t
  long crypto_core_ristretto255_bytes();

  // size_t crypto_core_ristretto255_hashbytes(void);
  @ssize_t
  long crypto_core_ristretto255_hashbytes();

  // size_t crypto_core_ristretto255_scalarbytes(void);
  @ssize_t
  long crypto_core_ristretto255_scalarbytes();

  // size_t crypto_core_ristretto255_nonreducedscalarbytes(void);
  @ssize_t
  long crypto_core_ristretto255_nonreducedscalarbytes();

  // int crypto_core_ristretto255_is_valid_point(const unsigned char * p);
  int crypto_core_ristretto255_is_valid_point(@In byte[] p);

  // int crypto_core_ristretto255_add(unsigned char * r, const unsigned char * p, const unsigned char * q);
  int crypto_core_ristretto255_add(@Out byte[] r, @In byte[] p, @In byte[] q);

  // int crypto_core_ristretto255_sub(unsigned char * r, const unsigned char * p, const unsigned char * q);
  int crypto_core_ristretto255_sub(@Out byte[] r, @In byte[] p, @In byte[] q);

  // int crypto_core_ristretto255_from_hash(unsigned char * p, const unsigned char * r);
  int crypto_core_ristretto255_from_hash(@Out byte[] p, @In byte[] r);

  // void crypto_core_ristretto255_random(unsigned char * p);
  void crypto_core_ristretto255_random(@Out byte[] p);

  // void crypto_core_ristretto255_scalar_random(unsigned char * r);
  void crypto_core_ristretto255_scalar_random(@Out byte[] r);

  // int crypto_core_ristretto255_scalar_invert(unsigned char * recip, const unsigned char * s);
  int crypto_core_ristretto255_scalar_invert(@Out byte[] recip, @In byte[] s);

  // void crypto_core_ristretto255_scalar_negate(unsigned char * neg, const unsigned char * s);
  void crypto_core_ristretto255_scalar_negate(@Out byte[] neg, @In byte[] s);

  // void crypto_core_ristretto255_scalar_complement(unsigned char * comp, const unsigned char * s);
  void crypto_core_ristretto255_scalar_complement(@Out byte[] comp, @In byte[] s);

  // void crypto_core_ristretto255_scalar_add(unsigned char * z, const unsigned char * x, const unsigned char * y);
  void crypto_core_ristretto255_scalar_add(@Out byte[] z, @In byte[] x, @In byte[] y);

  // void crypto_core_ristretto255_scalar_sub(unsigned char * z, const unsigned char * x, const unsigned char * y);
  void crypto_core_ristretto255_scalar_sub(@Out byte[] z, @In byte[] x, @In byte[] y);

  // void crypto_core_ristretto255_scalar_mul(unsigned char * z, const unsigned char * x, const unsigned char * y);
  void crypto_core_ristretto255_scalar_mul(@Out byte[] z, @In byte[] x, @In byte[] y);

  // void crypto_core_ristretto255_scalar_reduce(unsigned char * r, const unsigned char * s);
  void crypto_core_ristretto255_scalar_reduce(@Out byte[] r, @In byte[] s);

  // size_t crypto_scalarmult_ed25519_bytes(void);
  @ssize_t
  long crypto_scalarmult_ed25519_bytes();

  // size_t crypto_scalarmult_ed25519_scalarbytes(void);
  @ssize_t
  long crypto_scalarmult_ed25519_scalarbytes();

  // int crypto_scalarmult_ed25519(unsigned char * q, const unsigned char * n, const unsigned char * p);
  int crypto_scalarmult_ed25519(@Out byte[] q, @In byte[] n, @In byte[] p);

  // int crypto_scalarmult_ed25519_noclamp(unsigned char * q, const unsigned char * n, const unsigned char * p);
  int crypto_scalarmult_ed25519_noclamp(@Out byte[] q, @In byte[] n, @In byte[] p);

  // int crypto_scalarmult_ed25519_base(unsigned char * q, const unsigned char * n);
  int crypto_scalarmult_ed25519_base(@Out byte[] q, @In byte[] n);

  // int crypto_scalarmult_ed25519_base_noclamp(unsigned char * q, const unsigned char * n);
  int crypto_scalarmult_ed25519_base_noclamp(@Out byte[] q, @In byte[] n);

  // size_t crypto_scalarmult_ristretto255_bytes(void);
  @ssize_t
  long crypto_scalarmult_ristretto255_bytes();

  // size_t crypto_scalarmult_ristretto255_scalarbytes(void);
  @ssize_t
  long crypto_scalarmult_ristretto255_scalarbytes();

  // int crypto_scalarmult_ristretto255(unsigned char * q, const unsigned char * n, const unsigned char * p);
  int crypto_scalarmult_ristretto255(@Out byte[] q, @In byte[] n, @In byte[] p);

  // int crypto_scalarmult_ristretto255_base(unsigned char * q, const unsigned char * n);
  int crypto_scalarmult_ristretto255_base(@Out byte[] q, @In byte[] n);

  // size_t crypto_secretbox_xchacha20poly1305_keybytes(void);
  @ssize_t
  long crypto_secretbox_xchacha20poly1305_keybytes();

  // size_t crypto_secretbox_xchacha20poly1305_noncebytes(void);
  @ssize_t
  long crypto_secretbox_xchacha20poly1305_noncebytes();

  // size_t crypto_secretbox_xchacha20poly1305_macbytes(void);
  @ssize_t
  long crypto_secretbox_xchacha20poly1305_macbytes();

  // size_t crypto_secretbox_xchacha20poly1305_messagebytes_max(void);
  @ssize_t
  long crypto_secretbox_xchacha20poly1305_messagebytes_max();

  // int crypto_secretbox_xchacha20poly1305_easy(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_xchacha20poly1305_easy(
      @Out byte[] c,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] k);

  // int crypto_secretbox_xchacha20poly1305_open_easy(unsigned char * m, const unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_xchacha20poly1305_open_easy(
      @Out byte[] m,
      @In byte[] c,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] k);

  // int crypto_secretbox_xchacha20poly1305_detached(unsigned char * c, unsigned char * mac, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_xchacha20poly1305_detached(
      @Out byte[] c,
      @Out byte[] mac,
      @In byte[] m,
      @In @u_int64_t long mlen,
      @In byte[] n,
      @In byte[] k);

  // int crypto_secretbox_xchacha20poly1305_open_detached(unsigned char * m, const unsigned char * c, const unsigned char * mac, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_secretbox_xchacha20poly1305_open_detached(
      @Out byte[] m,
      @In byte[] c,
      @In byte[] mac,
      @In @u_int64_t long clen,
      @In byte[] n,
      @In byte[] k);

  // size_t crypto_pwhash_scryptsalsa208sha256_bytes_min(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_bytes_min();

  // size_t crypto_pwhash_scryptsalsa208sha256_bytes_max(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_bytes_max();

  // size_t crypto_pwhash_scryptsalsa208sha256_passwd_min(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_passwd_min();

  // size_t crypto_pwhash_scryptsalsa208sha256_passwd_max(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_passwd_max();

  // size_t crypto_pwhash_scryptsalsa208sha256_saltbytes(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_saltbytes();

  // size_t crypto_pwhash_scryptsalsa208sha256_strbytes(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_strbytes();

  // const char * crypto_pwhash_scryptsalsa208sha256_strprefix(void);
  String crypto_pwhash_scryptsalsa208sha256_strprefix();

  // size_t crypto_pwhash_scryptsalsa208sha256_opslimit_min(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_opslimit_min();

  // size_t crypto_pwhash_scryptsalsa208sha256_opslimit_max(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_opslimit_max();

  // size_t crypto_pwhash_scryptsalsa208sha256_memlimit_min(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_memlimit_min();

  // size_t crypto_pwhash_scryptsalsa208sha256_memlimit_max(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_memlimit_max();

  // size_t crypto_pwhash_scryptsalsa208sha256_opslimit_interactive(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_opslimit_interactive();

  // size_t crypto_pwhash_scryptsalsa208sha256_memlimit_interactive(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_memlimit_interactive();

  // size_t crypto_pwhash_scryptsalsa208sha256_opslimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_opslimit_sensitive();

  // size_t crypto_pwhash_scryptsalsa208sha256_memlimit_sensitive(void);
  @ssize_t
  long crypto_pwhash_scryptsalsa208sha256_memlimit_sensitive();

  // int crypto_pwhash_scryptsalsa208sha256(unsigned char *const out, unsigned long long outlen, const char *const passwd, unsigned long long passwdlen, const unsigned char *const salt, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_scryptsalsa208sha256(
      @Out byte[] out,
      @In @u_int64_t long outlen,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In byte[] salt,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit);

  // int crypto_pwhash_scryptsalsa208sha256_str(char[] out, const char *const passwd, unsigned long long passwdlen, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_scryptsalsa208sha256_str(
      @Out byte[] out,
      @In byte[] passwd,
      @In @u_int64_t long passwdlen,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit);

  // int crypto_pwhash_scryptsalsa208sha256_str_verify(const char[] str, const char *const passwd, unsigned long long passwdlen);
  int crypto_pwhash_scryptsalsa208sha256_str_verify(@In byte[] str, @In byte[] passwd, @In @u_int64_t long passwdlen);

  // int crypto_pwhash_scryptsalsa208sha256_ll(const uint8_t * passwd, size_t passwdlen, const uint8_t * salt, size_t saltlen, uint64_t N, uint32_t r, uint32_t p, uint8_t * buf, size_t buflen);
  int crypto_pwhash_scryptsalsa208sha256_ll(
      @In byte[] passwd,
      @In @ssize_t long passwdlen,
      @In byte[] salt,
      @In @ssize_t long saltlen,
      @In @u_int64_t long N,
      @In @u_int32_t int r,
      @In @u_int32_t int p,
      @Out byte[] buf,
      @In @ssize_t long buflen);

  // int crypto_pwhash_scryptsalsa208sha256_str_needs_rehash(const char[] str, unsigned long long opslimit, size_t memlimit);
  int crypto_pwhash_scryptsalsa208sha256_str_needs_rehash(
      @In byte[] str,
      @In @u_int64_t long opslimit,
      @In @ssize_t long memlimit);

  // size_t crypto_stream_salsa2012_keybytes(void);
  @ssize_t
  long crypto_stream_salsa2012_keybytes();

  // size_t crypto_stream_salsa2012_noncebytes(void);
  @ssize_t
  long crypto_stream_salsa2012_noncebytes();

  // size_t crypto_stream_salsa2012_messagebytes_max(void);
  @ssize_t
  long crypto_stream_salsa2012_messagebytes_max();

  // int crypto_stream_salsa2012(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_salsa2012(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_salsa2012_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_salsa2012_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // void crypto_stream_salsa2012_keygen(unsigned char[] k);
  void crypto_stream_salsa2012_keygen(@Out byte[] k);

  // size_t crypto_stream_salsa208_keybytes(void);
  @ssize_t
  long crypto_stream_salsa208_keybytes();

  // size_t crypto_stream_salsa208_noncebytes(void);
  @ssize_t
  long crypto_stream_salsa208_noncebytes();

  // size_t crypto_stream_salsa208_messagebytes_max(void);
  @ssize_t
  long crypto_stream_salsa208_messagebytes_max();

  // int crypto_stream_salsa208(unsigned char * c, unsigned long long clen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_salsa208(@Out byte[] c, @In @u_int64_t long clen, @In byte[] n, @In byte[] k);

  // int crypto_stream_salsa208_xor(unsigned char * c, const unsigned char * m, unsigned long long mlen, const unsigned char * n, const unsigned char * k);
  int crypto_stream_salsa208_xor(@Out byte[] c, @In byte[] m, @In @u_int64_t long mlen, @In byte[] n, @In byte[] k);

  // void crypto_stream_salsa208_keygen(unsigned char[] k);
  void crypto_stream_salsa208_keygen(@Out byte[] k);
}
