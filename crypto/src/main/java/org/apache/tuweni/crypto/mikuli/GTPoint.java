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
package org.apache.tuweni.crypto.mikuli;

import org.apache.milagro.amcl.BLS381.FP12;

import java.util.Objects;

/**
 * GT is the object that holds the result of the pairing operation. Points in GT are elements of Fq12.
 */
final class GTPoint {

  private final FP12 point;

  GTPoint(FP12 point) {
    this.point = point;
  }

  @Override
  public int hashCode() {
    return Objects.hash(point);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GTPoint gtPoint = (GTPoint) o;
    return (point != null && gtPoint.point == null) || point.equals(gtPoint.point);
  }
}
