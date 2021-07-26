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
package org.apache.tuweni.net.ip;

import org.apache.commons.net.util.SubnetUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Checks that an IP is allowed according to IP ranges.
 */
public class IPRangeChecker {

  /**
   * Creates a new IP range checker.
   * 
   * @param allowedRanges list of allowed ranges.
   * @param rejectedRanges list of rejected ranges
   * @return a new IP range checker
   * @throws IllegalArgumentException if a range is invalid.
   */
  public static IPRangeChecker create(List<String> allowedRanges, List<String> rejectedRanges) {
    List<SubnetUtils> allowed = new ArrayList<>();
    List<SubnetUtils> rejected = new ArrayList<>();
    for (String iprange : allowedRanges) {
      allowed.add(new SubnetUtils(iprange));
    }
    for (String iprange : rejectedRanges) {
      rejected.add(new SubnetUtils(iprange));
    }
    return new IPRangeChecker(allowed, rejected);
  }

  /**
   * Creates a checker that allows any IP.
   * 
   * @return a new range checker that allows any IP.
   */
  public static IPRangeChecker allowAll() {
    return create(Collections.singletonList("0.0.0.0/0"), Collections.emptyList());
  }

  private final List<SubnetUtils> allowedRanges;
  private final List<SubnetUtils> rejectedRanges;

  private IPRangeChecker(List<SubnetUtils> allowedRanges, List<SubnetUtils> rejectedRanges) {
    this.allowedRanges = allowedRanges;
    this.rejectedRanges = rejectedRanges;
  }

  /**
   * Checks if an IP address is inside the ranges of this checker
   * 
   * @param ip the IP address to check
   * @return true if it is inside the ranges of the checker.
   */
  public boolean check(String ip) {
    for (SubnetUtils subnetUtils : allowedRanges) {
      if (!subnetUtils.getInfo().isInRange(ip)) {
        return false;
      }
    }
    for (SubnetUtils subnetUtils : rejectedRanges) {
      if (subnetUtils.getInfo().isInRange(ip)) {
        return false;
      }
    }
    return true;
  }


}
