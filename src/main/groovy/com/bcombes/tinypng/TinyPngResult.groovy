package com.bcombes.tinypng

public class TinyPngResult {
    private long beforeSize
    private long afterSize
    private boolean error
    private ArrayList<TinyPngInfo> results

    TinyPngResult() {
        this.results = new ArrayList<TinyPngInfo>()
    }

    TinyPngResult(long beforeSize, long afterSize, boolean error, ArrayList<TinyPngInfo> results) {
        this.beforeSize = beforeSize
        this.afterSize = afterSize
        this.error = error
        this.results = results
    }

    long getBeforeSize() {
        return beforeSize
    }

    long getAfterSize() {
        return afterSize
    }

    boolean getError() {
        return error
    }

    void addInfo(TinyPngInfo tinyInfo) {
        if(tinyInfo != null) {
            beforeSize += tinyInfo.preSize
            afterSize += tinyInfo.postSize
            results.add(tinyInfo);
        }
    }

    void addResult(TinyPngResult tinyResult) {
        if(tinyResult != null) {
            this.beforeSize += tinyResult.beforeSize
            this.afterSize += tinyResult.afterSize
            results.addAll(tinyResult.getResults())
        }
    }

    ArrayList<TinyPngInfo> getResults() {
        return results
    }
}