package com.bcombes.tinypng

/**
 * Create On 16/12/2016
 * @author Wayne
 */
public class TinyPngExtension {
    String apiKey
    ArrayList<String> whiteList;
    ArrayList<String> resourcePattern;

    public TinyPngExtension() {
        apiKey = ""
        whiteList = []
        resourcePattern = []
    }

    @Override
    public String toString() {
        return "TinyPngExtension{" +
                "apiKey='" + apiKey + '\'' +
                ", whiteList=" + whiteList +
                ", resourcePattern=" + resourcePattern +
                '}';
    }
}
