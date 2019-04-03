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
package org.apache.tuweni.io;

import static org.apache.tuweni.io.Streams.enumerationStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.MustBeClosed;

/**
 * Methods for resolving resources.
 *
 * Supports recursive discovery and glob matching on the filesystem and in jar archives.
 */
public final class Resources {

  private Resources() {}

  // Lazily initialized on access
  private static final class Defaults {
    static final ClassLoader CLASSLOADER;

    static {
      ClassLoader cl;
      try {
        cl = Thread.currentThread().getContextClassLoader();
      } catch (Throwable e) {
        // can't load thread context classloader
        cl = Resources.class.getClassLoader();
        if (cl == null) {
          cl = ClassLoader.getSystemClassLoader();
        }
      }
      CLASSLOADER = cl;
    }
  }

  /**
   * Resolve resources using the default class loader.
   *
   * @param glob The glob pattern for matching resource paths against.
   * @return A stream of URLs to all resources.
   * @throws IOException If an I/O error occurs.
   */
  @MustBeClosed
  public static Stream<URL> find(String glob) throws IOException {
    return find(Defaults.CLASSLOADER, glob);
  }

  /**
   * Resolve resources using the default class loader.
   *
   * @param classLoader The class loader to use for resolving resources.
   * @param glob The glob pattern for matching resource paths against.
   * @return A stream of URLs to all resources.
   * @throws IOException If an I/O error occurs.
   */
  @MustBeClosed
  @SuppressWarnings("MustBeClosedChecker")
  public static Stream<URL> find(@Nullable ClassLoader classLoader, String glob) throws IOException {
    if (glob.isEmpty()) {
      return Stream.empty();
    }
    String[] globParts = globRoot(glob);
    String root = globParts[0];

    while (!root.isEmpty() && root.charAt(0) == '/') {
      root = root.substring(1);
    }

    Enumeration<URL> resources =
        (classLoader != null) ? classLoader.getResources(root) : ClassLoader.getSystemResources(root);
    Stream<URL> stream = enumerationStream(resources);

    if ("".equals(root)) {
      // stream will only contain file system references - will need to add all jar roots as well.
      stream = Stream.concat(stream, classLoaderJarRoots(classLoader));
    }

    if (globParts.length == 1) {
      return stream;
    }

    String rest = globParts[1];
    try {
      return stream.flatMap(url -> {
        try {
          // the mapped stream is closed after its contents have been placed into this stream
          return find(url, rest);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static Stream<URL> classLoaderJarRoots(@Nullable ClassLoader classLoader) {
    return classLoaderJarRoots(classLoader, new HashMap<>()).stream().map(uri -> {
      try {
        return uri.toURL();
      } catch (MalformedURLException e) {
        // Should not happen
        throw new RuntimeException(e);
      }
    });
  }

  private static Collection<URI> classLoaderJarRoots(@Nullable ClassLoader classLoader, Map<String, URI> results) {
    if (classLoader instanceof URLClassLoader) {
      URL[] urls = ((URLClassLoader) classLoader).getURLs();
      for (URL url : urls) {
        String urlPath;
        try {
          urlPath = Paths.get(url.toURI()).toString();
        } catch (URISyntaxException e) {
          // Should not happen
          throw new RuntimeException(e);
        }
        if (!Files.isRegularFile(Paths.get(urlPath))
            || !(urlPath.endsWith(".jar") || urlPath.endsWith(".war") || urlPath.endsWith(".zip"))) {
          continue;
        }
        URI jarUri;
        try {
          jarUri = new URI("jar:" + url.toString() + "!/");
        } catch (URISyntaxException e) {
          // Should not happen
          throw new RuntimeException(e);
        }
        results.put(urlPath, jarUri);
      }
    }

    if (classLoader == ClassLoader.getSystemClassLoader()) {
      // "java.class.path" manifest evaluation...
      classPathManifestEntries(results);
    }

    if (classLoader == null) {
      return results.values();
    }
    return classLoaderJarRoots(classLoader.getParent(), results);
  }

  private static void classPathManifestEntries(Map<String, URI> results) {
    String classPath = System.getProperty("java.class.path");
    if (classPath == null) {
      return;
    }

    StringTokenizer st = new StringTokenizer(classPath, System.getProperty("path.separator"));
    while (st.hasMoreTokens()) {
      String entry = st.nextToken().trim();
      if (entry.isEmpty()) {
        continue;
      }
      Path path = Paths.get(entry);
      if (!Files.isRegularFile(path)) {
        continue;
      }
      URI jarUri;
      try {
        jarUri = new URI("jar:" + path.toUri().toString() + "!/");
      } catch (URISyntaxException e) {
        // Should not happen
        throw new RuntimeException(e);
      }
      results.put(path.toString(), jarUri);
    }
  }

  @MustBeClosed
  private static Stream<URL> find(URL baseUrl, String glob) throws IOException {
    if (!isJarURL(baseUrl)) {
      return findFileResources(baseUrl, glob);
    } else {
      return findJarResources(baseUrl, glob);
    }
  }

  private static boolean isJarURL(URL url) {
    String protocol = url.getProtocol();
    return ("jar".equals(protocol) || "war".equals(protocol) || "zip".equals(protocol));
  }

  @MustBeClosed
  private static Stream<URL> findFileResources(URL rootDirUrl, String glob) throws IOException {
    Path rootDir;
    try {
      rootDir = Paths.get(rootDirUrl.toURI());
    } catch (URISyntaxException e) {
      // Should not happen
      throw new RuntimeException(e);
    }
    if (!Files.isDirectory(rootDir) || !Files.isReadable(rootDir)) {
      return Stream.empty();
    }

    PathMatcher pathMatcher = rootDir.getFileSystem().getPathMatcher("glob:" + glob);
    return Files
        .walk(rootDir, FileVisitOption.FOLLOW_LINKS)
        .filter(path -> pathMatcher.matches(rootDir.relativize(path)))
        .map(path -> {
          try {
            return path.toUri().toURL();
          } catch (MalformedURLException e) {
            // should not happen
            throw new RuntimeException(e);
          }
        });
  }

  private static Stream<URL> findJarResources(URL baseUrl, String glob) throws IOException {
    URLConnection connection = baseUrl.openConnection();
    if (!(connection instanceof JarURLConnection)) {
      // Not a jar file
      return Stream.empty();
    }
    JarURLConnection jarConnection = (JarURLConnection) connection;
    jarConnection.setUseCaches(false);
    JarFile jarFile = jarConnection.getJarFile();
    JarEntry jarEntry = jarConnection.getJarEntry();
    String rootEntryPath = (jarEntry == null) ? "" : jarEntry.getName();
    int rootEntryLength = rootEntryPath.length();

    // Use a PathMatcher for the default filesystem, which should be ok for the path segments from the resource URIs
    FileSystem fileSystem = FileSystems.getDefault();
    PathMatcher pathMatcher = fileSystem.getPathMatcher("glob:" + glob);

    return enumerationStream(jarFile.entries()).flatMap(entry -> {
      String entryPath = entry.getName();
      if (!entryPath.startsWith(rootEntryPath) || entryPath.length() == rootEntryLength) {
        return Stream.empty();
      }
      String relativePath = entryPath.substring(rootEntryLength);
      if (!pathMatcher.matches(fileSystem.getPath(relativePath))) {
        return Stream.empty();
      }
      URL entryURL;
      try {
        entryURL = new URL(baseUrl, relativePath);
      } catch (MalformedURLException e) {
        // should not happen
        throw new RuntimeException(e);
      }
      return Stream.of(entryURL);
    });
  }

  @VisibleForTesting
  static String[] globRoot(String glob) {
    int length = glob.length();
    int j = 0;
    for (int i = 0; i < length; ++i) {
      char c = glob.charAt(i);
      switch (c) {
        case '/':
          j = i;
          break;
        case '\\':
          i++;
          if (i >= length) {
            throw new PatternSyntaxException("Invalid escape character at end of glob pattern", glob, length - 1);
          }
          break;
        case '*':
        case '?':
        case '[':
        case '{':
          if (i == 0) {
            return new String[] {"", glob};
          } else {
            return new String[] {unescape(glob.substring(0, j)), glob.substring(j + 1)};
          }
        default:
          // move on to next char
      }
    }
    return new String[] {unescape(glob)};
  }

  private static String unescape(String glob) {
    int length = glob.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; ++i) {
      char c = glob.charAt(i);
      if (c == '\\') {
        ++i;
        assert (i < length);
      }
      builder.append(glob.charAt(i));
    }
    return builder.toString();
  }
}
