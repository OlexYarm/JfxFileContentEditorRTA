/*
 * Copyright (c) 2024, Oleksandr Yarmolenko. All rights reserved.
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

import java.io.IOException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JfxFileContentEditorBottomController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JfxFileContentEditorBottomController.class);

    // -------------------------------------------------------------------------------------
    private JfxFileContentEditorController jfxEditorController;

    // -------------------------------------------------------------------------------------
    @FXML
    private VBox vboxBottom;

    @FXML
    private HBox hboxBottomSearchResult;

    @FXML
    private HBox hboxBottomFind;

    @FXML
    private TextField tfBottomFind;

    @FXML
    private Button btBottomNext;

    @FXML
    private Button btBottomPrev;

    @FXML
    private Button btBottomAll;

    @FXML
    private Button btBottomCase;

    @FXML
    private HBox hboxBottomReplace;

    @FXML
    private TextField tfBottomReplace;

    @FXML
    private HBox hboxBottomVersion;

    @FXML
    private Label hboxBottomLabelVersion;

    @FXML
    private TextField tfBottomCursorPos;

    @FXML
    private TextField tfBottomLineEnding;

    @FXML
    private TextField tfBottomCharset;

    // -------------------------------------------------------------------------------------
    private BorderPane borderPaneEditor;
    private TabPane tabPane;
    private ObservableList<Tab> lstTabs;

    // -------------------------------------------------------------------------------------
    private Label lblBottomFindResult = null;

    private boolean booCaseSensitive = Settings.BOO_CASE_SENSITIVE;
    private final Paint paintBackgroundSensitive = Color.LIGHTGRAY; //DARKGREY; //DIMGRAY;
    private final Paint paintBackgroundInsensitive = Color.GAINSBORO; //LIGHTGRAY;

    // -------------------------------------------------------------------------------------
    // JFX constructor
    // -------------------------------------------------------------------------------------
    public void initialize() {

        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.hboxBottomSearchResult.toString(), hboxBottomSearchResult);

        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.hboxBottomFind.toString(), hboxBottomFind);
        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.tfBottomFind.toString(), tfBottomFind);

        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.hboxBottomReplace.toString(), hboxBottomReplace);
        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.tfBottomReplace.toString(), tfBottomReplace);

        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.vboxBottom.toString(), vboxBottom);

        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.tfBottomLineEnding.toString(), tfBottomLineEnding);

        Utils.MAP_NODE_REFS.put(
                Utils.NODE_NAMES.tfBottomCharset.toString(), tfBottomCharset);

        this.hboxBottomSearchResult.managedProperty().bind(this.hboxBottomSearchResult.visibleProperty());
        this.hboxBottomFind.managedProperty().bind(this.hboxBottomFind.visibleProperty());
        this.hboxBottomReplace.managedProperty().bind(this.hboxBottomReplace.visibleProperty());
        this.btBottomCase.managedProperty().bind(this.btBottomCase.visibleProperty());

        this.changeSensitivity();

        LOGGER.debug("### Initialize JfxFileContentEditorBottomController."
                + " this=\"" + this + "\""
                + " vboxBottom=\"" +this. vboxBottom + "\""
                + " hboxBottomSearchResult=\"" + this.hboxBottomSearchResult + "\""
                + " hboxBottomFind=\"" + this.hboxBottomFind + "\""
                + " tfBottomFind=\"" + this.tfBottomFind + "\""
                + " hboxBottomReplace=\"" + this.hboxBottomReplace + "\""
                + " tfBottomReplace=\"" + this.tfBottomReplace + "\""
                + " hboxBottomVersion=\"" + this.hboxBottomVersion + "\""
                + " hboxBottomLabelVersion=\"" + this.hboxBottomLabelVersion + "\""
                + " tfBottomLineEnding=\"" + this.tfBottomLineEnding + "\""
                + " tfBottomCharset=\"" + this.tfBottomCharset + "\""
        );
    }

    // -------------------------------------------------------------------------------------
    // Methods
    // -------------------------------------------------------------------------------------
    public void setParentController(JfxFileContentEditorController jfxEditorController) {

        this.jfxEditorController = jfxEditorController;

        this.borderPaneEditor = this.jfxEditorController.borderPaneEditor;

        this.tabPane = this.jfxEditorController.tabPaneEditor;
        this.lstTabs = this.tabPane.getTabs();

        this.hboxBottomLabelVersion.setText(Settings.STR_APP_TITLE + " " + Settings.STR_VERSION);

        LOGGER.debug("### Setting Parent Controller in JfxFileContentEditorBottomController."
                + " this=\"" + this + "\""
                + " jfxEditorController=\"" + this.jfxEditorController + "\""
                + " vboxBottom=\"" + vboxBottom + "\""
                + " hboxBottomSearchResult=\"" + hboxBottomSearchResult + "\""
                + " hboxBottomFind=\"" + hboxBottomFind + "\""
                + " hboxBottomReplace=\"" + hboxBottomReplace + "\""
                + " hboxBottomVersion=\"" + hboxBottomVersion + "\""
                + " hboxBottomLabelVersion=\"" + hboxBottomLabelVersion + "\"");
    }

    // -------------------------------------------------------------------------------------
    // FXML Action Methods
    // -------------------------------------------------------------------------------------
    @FXML
    private void findNext(ActionEvent actionEvent) throws IOException {

        String strTextFind = this.validateFindValue();
        if (strTextFind == null) {
            return;
        }
        Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
        FileContentEditor fileEditor = (FileContentEditor) tab.getContent();
        // TODO: Disable Find Buttons - it does not work !!!
        this.btBottomNext.setDisable(true);
        this.btBottomPrev.setDisable(true);
        this.btBottomAll.setDisable(true);
        LOGGER.debug("Start Find."
                + " actionEvent=\"" + actionEvent + "\""
                + " TextFind=\"" + strTextFind + "\"");

        Platform.runLater(() -> {
            String strFound = fileEditor.find(strTextFind, null, false, false, booCaseSensitive);
            this.lblBottomFindResult.setText(strFound);
            this.btBottomNext.setDisable(false);
            this.btBottomPrev.setDisable(false);
            this.btBottomAll.setDisable(false);
            LOGGER.debug(strFound
                    + " actionEvent=\"" + actionEvent + "\""
                    + " TextFind=\"" + strTextFind + "\"");
        });
        /*
        if (intFoundCount < 1) {
            String strErrMsg = "No occurrence of \"" + strTextFind + "\" found in " + fileEditor.getFilePath();
            LOGGER.debug(strErrMsg
                    + " actionEvent=\"" + actionEvent + "\""
                    + " TextFind=\"" + strTextFind + "\"");
            this.lblBottomFindResult.setText(strErrMsg);
        } else {
            String strMsg = "Found occurrence of \"" + strTextFind + "\" in " + fileEditor.getFilePath();
            LOGGER.debug(strMsg
                    + " actionEvent=\"" + actionEvent + "\""
                    + " TextFind=\"" + strTextFind + "\"");
            this.lblBottomFindResult.setText(strMsg);
        }
         */
    }

    private String validateFindValue() {

        if (lstTabs.isEmpty()) {
            LOGGER.error("No one file open for editing.");
            Utils.showMessage(Alert.AlertType.ERROR, "Find", "", "No one file open for editing.", null, null);
            return null;
        }
        if (!this.findLabelBottomSearchResult()) {
            LOGGER.error("Could not get Label BottomSearchResult");
            Utils.showMessage(Alert.AlertType.ERROR, "Find", "", "Internal error.", null, null);
            return null;
        }

        String strTextFind = this.findTextFieldFindValue();
        if (strTextFind == null) {
            String strErrMsg = "Text to find is not set.";
            this.lblBottomFindResult.setText(strErrMsg);
            return null;
        }

        return strTextFind;
    }

    @FXML
    private void findPrev(ActionEvent actionEvent) throws IOException {

        String strTextFind = this.validateFindValue();
        if (strTextFind == null) {
            return;
        }
        Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
        FileContentEditor fileEditor = (FileContentEditor) tab.getContent();
        String strFound = fileEditor.findPrev(strTextFind, null, booCaseSensitive);
        this.lblBottomFindResult.setText(strFound);
        LOGGER.debug(strFound
                + " actionEvent=\"" + actionEvent + "\""
                + " TextFind=\"" + strTextFind + "\"");
    }

    @FXML
    private void findAll(ActionEvent actionEvent) throws IOException {

        String strTextFind = this.validateFindValue();
        if (strTextFind == null) {
            return;
        }

        Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
        FileContentEditor fileEditor = (FileContentEditor) tab.getContent();
        String strFound = fileEditor.find(strTextFind, null, true, false, booCaseSensitive);
        this.lblBottomFindResult.setText(strFound);
        LOGGER.debug(strFound
                + " actionEvent=\"" + actionEvent + "\""
                + " TextFind=\"" + strTextFind + "\"");

        /*        
        int intFoundCount = fileEditor.find(strTextFind, null, true);
        if (intFoundCount < 1) {
            String strErrMsg = "No occurrence of \"" + strTextFind + "\" found in " + fileEditor.getFilePath();
            LOGGER.debug(strErrMsg
                    + " actionEvent=\"" + actionEvent + "\""
                    + " TextFind=\"" + strTextFind + "\"");
            this.lblBottomFindResult.setText(strErrMsg);
            return;
        }
        String strSuffix;
        if (intFoundCount == 1) {
            strSuffix = "";
        } else {
            strSuffix = "s";
        }
        String strFindResult = "Fount " + intFoundCount + " occurrence" + strSuffix + " of \"" + strTextFind + "\" in " + fileEditor.getFilePath();
        this.lblBottomFindResult.setText(strFindResult);
         */
    }

    // -------------------------------------------------------------------------------------
    @FXML
    private void findCase(ActionEvent actionEvent) throws IOException {

        this.booCaseSensitive = !this.booCaseSensitive;
        this.changeSensitivity();
        Settings.BOO_CASE_SENSITIVE = this.booCaseSensitive;
    }

    // -------------------------------------------------------------------------------------
    @FXML
    private void replace(ActionEvent actionEvent) throws IOException {

        this.replaceText(actionEvent, false);
    }

    // -------------------------------------------------------------------------------------
    @FXML
    private void replaceAll(ActionEvent actionEvent) throws IOException {

        this.replaceText(actionEvent, true);
    }

    // -------------------------------------------------------------------------------------
    @FXML
    private void bottomHide(ActionEvent actionEvent) throws IOException {

        Utils.changeNodeVisibility(this.hboxBottomSearchResult, false);
        Utils.changeNodeVisibility(this.hboxBottomFind, false);
        Utils.changeNodeVisibility(this.hboxBottomReplace, false);
    }

    // -------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------
    private void replaceText(ActionEvent actionEvent, boolean booAll) {

        String strTextFind = this.validateFindValue();
        if (strTextFind == null) {
            return;
        }

        Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
        FileContentEditor fileEditor = (FileContentEditor) tab.getContent();

        String strNodeID = "tfBottomReplace";
        TextField tfTextField = (TextField) Utils.lookupNodeByID(this.borderPaneEditor, TextField.class, strNodeID);
        String strTextReplace = tfTextField.getText();
        LOGGER.debug("Replace Text."
                + " actionEvent=\"" + actionEvent + "\""
                + " TextReplace=\"" + strTextReplace + "\"");
        if (strTextReplace == null) {
            strTextReplace = "";
        }

        String strFound = fileEditor.find(strTextFind, strTextReplace, booAll, false, booCaseSensitive);
        this.lblBottomFindResult.setText(strFound);
        LOGGER.debug(strFound
                + " actionEvent=\"" + actionEvent + "\""
                + " TextFind=\"" + strTextFind + "\"");
        /*
        int intFoundCount;
        intFoundCount = fileEditor.find(strTextFind, strTextReplace, booAll);
        if (intFoundCount < 1) {
            String strErrMsg = "No occurrence of \"" + strTextFind + "\" found in " + fileEditor.getFilePath();
            LOGGER.debug(strErrMsg
                    + " actionEvent=\"" + actionEvent + "\""
                    + " TextFind=\"" + strTextFind + "\""
                    + " TextReplace=\"" + strTextReplace + "\"");
            this.lblBottomFindResult.setText(strErrMsg);
            return;
        }
        String strResult;
        if (intFoundCount == 1) {
            strResult = "";
        } else {
            strResult = "s";
        }
        String strFindResult = intFoundCount + " substring" + strResult + " updated in " + fileEditor.getFilePath();
        this.lblBottomFindResult.setText(strFindResult);

         */
    }

    private boolean findLabelBottomSearchResult() {

        if (this.lblBottomFindResult == null) {
            String strNodeID = "lblBottomSearchResult";
            String strNodeClassName = Label.class.getName();
            this.lblBottomFindResult = (Label) Utils.lookupNodeByID(this.borderPaneEditor, Label.class, strNodeID);
            if (this.lblBottomFindResult == null) {
                String strErrMsg = "Could not find Node."
                        + " NodeID=\"" + strNodeID + "\" "
                        + " NodeClassName=\"" + strNodeClassName + "\"";
                LOGGER.info(strErrMsg);
                // TODO: add error message on screen
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------------------
    private String findTextFieldFindValue() {

        if (this.tfBottomFind == null) {
            String strNodeID = "tfBottomFind";
            String strNodeClassName = TextField.class.getName();
            this.tfBottomFind = (TextField) Utils.lookupNodeByID(this.borderPaneEditor, TextField.class, strNodeID);
            if (this.tfBottomFind == null) {
                String strErrMsg = "Could not find Node."
                        + " NodeID=\"" + strNodeID + "\" "
                        + " NodeClassName=\"" + strNodeClassName + "\"";
                LOGGER.info(strErrMsg);
                // TODO: add error message on screen
                return null;
            }
        }
        String strTextFind = this.tfBottomFind.getText();
        if (strTextFind == null || strTextFind.isEmpty()) {
            String strErrMsg = "Find string not set (is null or empty).";
            LOGGER.error(strErrMsg
                    + " TextFind=\"" + strTextFind + "\"");
            if (this.findLabelBottomSearchResult()) {
                this.lblBottomFindResult.setText(strErrMsg);
            }
            return null;
        }
        return strTextFind;
    }

    // -------------------------------------------------------------------------------------
    private void changeSensitivity() {

        if (this.booCaseSensitive) {
            this.btBottomCase.setText("Sensitive");
            this.btBottomCase.setBackground(Background.fill(paintBackgroundSensitive));
        } else {
            this.btBottomCase.setText("Insensitive");
            this.btBottomCase.setBackground(Background.fill(paintBackgroundInsensitive));
        }
    }
    // -------------------------------------------------------------------------------------
}
