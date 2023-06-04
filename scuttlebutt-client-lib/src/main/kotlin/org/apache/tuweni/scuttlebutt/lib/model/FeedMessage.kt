// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.util.Optional

/**
 * A scuttlebutt feed message
 *
 * @param key the ID of the message
 * @param type the type of the content (is Empty if unknown because the message is private and not decryptable.)
 * @param value the metadata and contents of the message.
*/
@JsonDeserialize(using = FeedMessageDeserializer::class)
data class FeedMessage(val key: String, val type: Optional<String>, val value: FeedValue)
