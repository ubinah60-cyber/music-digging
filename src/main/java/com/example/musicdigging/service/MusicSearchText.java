package com.example.musicdigging.service;

import java.text.Normalizer;
import java.util.Locale;

final class MusicSearchText {
    private static final String[] INITIAL = {"g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"};
    private static final String[] VOWEL = {"a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"};
    private static final String[] FINAL = {"", "k", "k", "ks", "n", "nj", "nh", "t", "l", "lk", "lm", "lb", "ls", "lt", "lp", "lh", "m", "p", "ps", "t", "t", "ng", "t", "t", "k", "t", "p", "t"};

    private MusicSearchText() {}

    static String romanizeHangul(String value) {
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint >= 0xAC00 && codePoint <= 0xD7A3) {
                int syllable = codePoint - 0xAC00;
                result.append(INITIAL[syllable / 588]);
                result.append(VOWEL[(syllable % 588) / 28]);
                result.append(FINAL[syllable % 28]);
            } else {
                result.appendCodePoint(codePoint);
            }
        }
        return result.toString();
    }

    static String key(String value) {
        String decomposed = Normalizer.normalize(romanizeHangul(value), Normalizer.Form.NFD);
        return decomposed.toLowerCase(Locale.ROOT)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    static int rank(String name, String query) {
        String title = key(name);
        String term = key(query);
        if (title.isEmpty() || term.isEmpty()) return 3;
        if (title.equals(term)) return 0;
        if (title.startsWith(term)) return 1;
        if (title.contains(term)) return 2;
        return 3;
    }
}
