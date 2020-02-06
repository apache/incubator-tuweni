package org.apache.tuweni.ethstats;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.apache.tuweni.crypto.Hash;

final class TxStats {

    private final Hash hash;

    TxStats(Hash hash) {
        this.hash = hash;
    }

    @JsonGetter("hash")
    public Hash getHash() {
        return hash;
    }
}
