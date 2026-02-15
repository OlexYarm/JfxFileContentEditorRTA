/*
 * Copyright (c) 2024, 2025, 2026 Oleksandr Yarmolenko. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 */
package com.olexyarm.jfxfilecontenteditor;

class FileContentEditorState {

    private boolean booSearchResultVisible = false;
    private boolean booFindVisible = false;
    private boolean booReplaceVisible = false;

    private String strSearchResult;
    private String strFind;
    private String strReplace;

    public boolean isSearchResultVisible() {
        return booSearchResultVisible;
    }

    public void setSearchResultVisible(boolean booSearchResultVisible) {
        this.booSearchResultVisible = booSearchResultVisible;
    }

    public boolean isFindVisible() {
        return booFindVisible;
    }

    public void setFindVisible(boolean booFindVisible) {
        this.booFindVisible = booFindVisible;
    }

    public boolean isReplaceVisible() {
        return booReplaceVisible;
    }

    public void setReplaceVisible(boolean booReplaceVisible) {
        this.booReplaceVisible = booReplaceVisible;
    }

    public String getSearchResult() {
        return strSearchResult;
    }

    public void setSearchResult(String strSearchResult) {
        this.strSearchResult = strSearchResult;
    }

    public String getFind() {
        return strFind;
    }

    public void setFind(String strFind) {
        this.strFind = strFind;
    }

    public String getReplace() {
        return strReplace;
    }

    public void setReplace(String strReplace) {
        this.strReplace = strReplace;
    }

}
