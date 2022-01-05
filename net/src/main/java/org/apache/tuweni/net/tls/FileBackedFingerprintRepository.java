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
package org.apache.tuweni.net.tls;

import static java.nio.file.Files.createDirectories;
import static org.apache.tuweni.io.file.Files.atomicReplace;
import static org.apache.tuweni.io.file.Files.createFileIfMissing;

import org.apache.tuweni.bytes.Bytes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

final class FileBackedFingerprintRepository implements FingerprintRepository {

  private final Path fingerprintFile;
  private volatile Map<String, Bytes> fingerprints;

  FileBackedFingerprintRepository(Path fingerprintFile) {
    try {
      createDirectories(fingerprintFile.toAbsolutePath().getParent());
      createFileIfMissing(fingerprintFile);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Cannot create fingerprint file " + fingerprintFile, e);
    }
    this.fingerprintFile = fingerprintFile;
    this.fingerprints = parseFingerprintFile(fingerprintFile);
  }

  @Override
  public boolean contains(String identifier) {
    return fingerprints.containsKey(identifier);
  }

  @Override
  public boolean contains(String identifier, Bytes fingerprint) {
    return fingerprint.equals(fingerprints.get(identifier));
  }

  @Override
  public void addFingerprint(String identifier, Bytes fingerprint) {
    if (!contains(identifier, fingerprint)) {
      synchronized (this) {
        if (!contains(identifier, fingerprint)) {
          // put into a copy first, then atomically replace
          HashMap<String, Bytes> fingerprintsCopy = new HashMap<>(fingerprints);
          fingerprintsCopy.put(identifier, fingerprint);
          fingerprints = writeFingerprintFile(fingerprintFile, fingerprintsCopy);
        }
      }
    }
  }

  private static Map<String, Bytes> parseFingerprintFile(Path fingerprintFile) {
    List<String> lines;
    try {
      lines = Files.readAllLines(fingerprintFile);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Cannot read fingerprint file " + fingerprintFile, e);
    }

    Map<String, Bytes> fingerprints = new HashMap<>();

    for (int i = 0; i < lines.size(); ++i) {
      String line = lines.get(i).trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      Map.Entry<String, Bytes> entry;
      try {
        entry = parseLine(line);
      } catch (IOException e) {
        throw new TLSEnvironmentException(e.getMessage() + " in " + fingerprintFile + " (line " + (i + 1) + ")");
      }
      fingerprints.put(entry.getKey(), entry.getValue());
    }

    return Collections.unmodifiableMap(fingerprints);
  }

  private static Map<String, Bytes> writeFingerprintFile(Path fingerprintFile, Map<String, Bytes> updatedFingerprints) {
    List<String> lines;
    try {
      lines = Files.readAllLines(fingerprintFile);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Cannot read fingerprint file " + fingerprintFile, e);
    }

    Map<String, Bytes> fingerprints = new HashMap<>();
    HashSet<String> updatedIdentifiers = new HashSet<>(updatedFingerprints.keySet());

    try {
      atomicReplace(fingerprintFile, writer -> {
        // copy lines, replacing any updated fingerprints
        for (int i = 0; i < lines.size(); ++i) {
          String line = lines.get(i).trim();
          if (line.isEmpty() || line.startsWith("#")) {
            writer.write(lines.get(i));
            writer.write(System.lineSeparator());
            continue;
          }

          Map.Entry<String, Bytes> entry;
          try {
            entry = parseLine(line);
          } catch (IOException e) {
            throw new TLSEnvironmentException(e.getMessage() + " in " + fingerprintFile + " (line " + (i + 1) + ")");
          }

          String identifier = entry.getKey();
          Bytes fingerprint = updatedFingerprints.getOrDefault(identifier, entry.getValue());
          fingerprints.put(identifier, fingerprint);
          updatedIdentifiers.remove(identifier);

          writer.write(identifier);
          writer.write(' ');
          writer.write(fingerprint.toHexString().substring(2).toLowerCase());
          writer.write(System.lineSeparator());
        }

        // write any new fingerprints at the end
        for (String identifier : updatedIdentifiers) {
          Bytes fingerprint = updatedFingerprints.get(identifier);
          fingerprints.put(identifier, fingerprint);
          writer.write(identifier);
          writer.write(' ');
          writer.write(fingerprint.toHexString().substring(2).toLowerCase());
          writer.write(System.lineSeparator());
        }
      });

      return Collections.unmodifiableMap(fingerprints);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Cannot write fingerprint file " + fingerprintFile, e);
    }
  }

  private static Map.Entry<String, Bytes> parseLine(String line) throws IOException {
    String[] segments = line.split("\\s+", 2);
    if (segments.length != 2) {
      throw new IOException("Invalid line");
    }
    String identifier = segments[0];
    String fingerprintString = segments[1].trim().replace(":", "");
    Bytes fingerprint;
    try {
      fingerprint = Bytes.fromHexString(fingerprintString);
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid fingerprint", e);
    }
    return new SimpleImmutableEntry<>(identifier, fingerprint);
  }
}
