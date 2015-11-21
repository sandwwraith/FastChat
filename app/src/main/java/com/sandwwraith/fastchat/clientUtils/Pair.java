package com.sandwwraith.fastchat.clientUtils;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class Pair<K, V> {
    public K first;
    public V second;

    public Pair() {
        first = null;
        second = null;
    }

    public Pair(K fst, V scnd) {
        first = fst;
        second = scnd;
    }
}
