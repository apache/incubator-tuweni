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
package org.apache.tuweni.config;

import java.util.List;

/**
 * A validator for a configuration.
 *
 * <p>
 * Validators of this type are invoked during verification after all property validators. However, errors returned by
 * property validators do not prevent this validator being evaluated, so properties of the configuration may be missing
 * or invalid.
 */
public interface ConfigurationValidator {

  /**
   * Validate a configuration.
   *
   * @param configuration The value associated with the configuration entry.
   * @return A list of error messages. If no errors are found, an empty list should be returned.
   */
  List<ConfigurationError> validate(Configuration configuration);
}
