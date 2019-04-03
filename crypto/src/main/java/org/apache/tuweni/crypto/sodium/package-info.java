/**
 * Classes and utilities for working with the sodium native library.
 *
 * <p>
 * Classes and utilities in this package provide an interface to the native Sodium crypto library
 * (https://www.libsodium.org/), which must be installed on the same system as the JVM. It will be searched for in
 * common library locations, or it can be loaded explicitly using
 * {@link org.apache.tuweni.crypto.sodium.Sodium#searchLibrary(java.nio.file.Path...)} or
 * {@link org.apache.tuweni.crypto.sodium.Sodium#loadLibrary(java.nio.file.Path)}.
 *
 * <p>
 * Classes in this package also depend upon the JNR-FFI library being available on the classpath, along with its
 * dependencies. See https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency
 * 'com.github.jnr:jnr-ffi'.
 */
@ParametersAreNonnullByDefault
package org.apache.tuweni.crypto.sodium;

import javax.annotation.ParametersAreNonnullByDefault;
