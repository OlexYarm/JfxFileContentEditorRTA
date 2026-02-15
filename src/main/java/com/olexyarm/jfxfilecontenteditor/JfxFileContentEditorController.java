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

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JfxFileContentEditorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JfxFileContentEditorController.class);

    // -------------------------------------------------------------------------------------
    @FXML
    public JfxFileContentEditorMenuController jfxEditorMenuController;

    @FXML
    public JfxFileContentEditorBottomController jfxEditorBottomController;

    // -------------------------------------------------------------------------------------
    @FXML
    BorderPane borderPaneEditor;

    @FXML
    HBox jfxEditorMenu;

    @FXML
    TabPane tabPaneEditor;

    @FXML
    VBox jfxEditorBottom;

    // -------------------------------------------------------------------------------------
    // JFX constructor
    // -------------------------------------------------------------------------------------
    @FXML
    public void initialize() {

        this.tabPaneEditor.setTabDragPolicy(TabDragPolicy.REORDER);
        TabDragPolicy tabDragPolicy = this.tabPaneEditor.getTabDragPolicy();

        LOGGER.debug("### Initialize JfxFileContentEditorController."
                + " borderPaneEditor=\"" + this.borderPaneEditor + "\""
                + " jfxEditorMenu=\"" + this.jfxEditorMenu + "\""
                + " tabPaneEditor=\"" + this.tabPaneEditor + "\""
                + " tabDragPolicy=\"" + tabDragPolicy + "\""
                + " jfxEditorBottom=\"" + this.jfxEditorBottom + "\""
                + " jfxEditorMenuController=\"" + this.jfxEditorMenuController + "\""
                + " jfxEditorBottomController=\"" + this.jfxEditorBottomController + "\""
        );
        this.jfxEditorMenuController.setParentController(this);
        this.jfxEditorBottomController.setParentController(this);
    }
    // -------------------------------------------------------------------------------------
}
