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
package org.apache.tuweni.ssz;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SSZFixedSizeTypeList<T extends SSZReadable & SSZWritable> implements SSZReadable, SSZWritable {

  private final int elementSize;
  private final Supplier<T> supplier;

  private final List<T> elements = new ArrayList<>();

  public SSZFixedSizeTypeList(int elementSize, Supplier<T> supplier) {
    this.elementSize = elementSize;
    this.supplier = supplier;
  }

  // The elements might be fixed, but the overall type is variable
  @Override
  public boolean isFixed() {
    return false;
  }

  @Override
  public void populateFromReader(SSZReader reader) {
    elements.addAll(reader.readFixedTypedList(elementSize, supplier));
  }

  @Override
  public void writeTo(SSZWriter writer) {
    writer.writeTypedList(elements);
  }

  public List<T> getElements() {
    return elements;
  }
}
