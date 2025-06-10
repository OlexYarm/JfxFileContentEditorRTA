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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import jfx.incubator.scene.control.richtext.LineNumberDecorator;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileContentEditor extends VBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileContentEditor.class);
    private static final byte BYT_CR = 0x0D;
    private static final byte BYT_LF = 0x0A;
    private static final String STR_CR_LF_WIN = "Win CRLF";
    private static final String STR_LF_UNIX = "Unix LF";
    private static final String STR_CR_LF_MIX = "Mix CR LF";
    private static final String STR_NO_CR_LF = "no CR LF)";
    private static final String STR_CR_LF_WIP = "WIP";
    private final StringProperty spLineEnding = new SimpleStringProperty(STR_CR_LF_WIP);

    private enum enuLineEnding {
        No, Win, Unix
    };
    private enuLineEnding enuLineEndType = enuLineEnding.No;

    private Path pathFile;
    private String strFilePath;
    private String strFileName;
    private String strFileExt;
    private String strFileDir;
    private boolean booBinary;
    private String strCharsetName;
    private boolean booFileModified;
    private boolean booTextWrap;

    private final String strId;

    // ---------- Graphics - Begin -----------------------------------------------------
    private final StyledTextModel model = new RichTextModel();
    private final RichTextArea richTextArea = new RichTextArea(model);
    private StyleAttributeMap mapStyleAttrFont;

    private final HBox hboxState;
    private final Label lblFileState;
    private final Label lblFileName;
    private final ProgressBar progressBar;

    private Task<String> taskFileLoad;

    private Service<String> serviceFileSave;
    private static final int INT_PROGRESS_BAR_STEPS = 20;
    private static final int INT_FILE_LEN_SPLIT = 10;
    private int intFileSaveCount = 0;

    private final ReadOnlyBooleanProperty booPropFocusedProperty;
    private final ChangeListener<Boolean> focusedPropertyChangeListener;
    private final StyledTextModel.Listener stmChangeListenerFileContent;
    private final ReadOnlyProperty<TextPos> textPosCaretPositionProperty;
    private final ChangeListener<TextPos> textPosCaretPositionChangeListener;
    private TextPos textPosCaretPosition = TextPos.ZERO;

    private Font font;

    private TabPane tabPane;

    // ---------- Graphics - End -----------------------------------------------------
    // -------------------------------------------------------------------------------------
    // Construstors
    // -------------------------------------------------------------------------------------
    public FileContentEditor(final String strId, final Path pathFile) {

        // TODO: Add CSS
        //getStyleClass().add("fileContentEditor.css");
        this.strId = strId;
        this.pathFile = pathFile;
        this.parseFilePath(strId, pathFile);

        // TODO: Add binary file show/edit
        this.booBinary = false;
        this.font = Settings.getFontDefault();

        this.strCharsetName = Settings.STR_CHARSET_CURRENT;
        // ---------- initGraphics - begin -----------------------------------------------------
        LineNumberDecorator ld = new LineNumberDecorator();
        this.richTextArea.setLeftDecorator(ld);

        this.mapStyleAttrFont = StyleAttributeMap.builder()
                .setFontFamily(Settings.getFontFamilyDefault())
                .setFontSize(Settings.getFontSizeDefault()).build();

        this.richTextArea.setDisplayCaret(true);
        this.richTextArea.setEditable(true);
        this.richTextArea.setManaged(true);
        this.richTextArea.requestFocus();

        VBox.setVgrow(this.richTextArea, Priority.ALWAYS);

        /* switch to richTextArea
        this.textArea.setPromptText("Enter Text here.");
        this.textArea.setFont(this.font);
        VBox.setVgrow(this.textArea, Priority.ALWAYS);
         */
        this.hboxState = new HBox();
        Insets insHboxPadd = new Insets(5, 5, 5, 20);
        this.hboxState.setPadding(insHboxPadd);
        this.hboxState.setSpacing(5);
        this.hboxState.setVisible(false);

        this.lblFileState = new Label("");
        this.lblFileName = new Label(this.strFileName);
        this.progressBar = new ProgressBar(0);

        this.hboxState.getChildren().addAll(this.lblFileName, this.progressBar, this.lblFileState);
        this.hboxState.managedProperty().bind(this.hboxState.visibleProperty());

        this.getChildren().addAll(this.richTextArea, this.hboxState);// switch to richTextArea

        Insets insVboxPadd = new Insets(5, 5, 5, 5);
        this.setPadding(insVboxPadd);

        // ---------- initGraphics - end -------------------------------------------------------
        //
        // ---------- registerListeners - begin ------------------------------------------------
        this.textPosCaretPositionProperty = this.richTextArea.caretPositionProperty();
        this.textPosCaretPositionChangeListener = new ChangeListener<TextPos>() {
            @Override
            public void changed(ObservableValue<? extends TextPos> observable, TextPos oldValue, TextPos newValue) {
                hboxState.setVisible(false);
                textPosCaretPosition = newValue;
                LOGGER.debug("caretPositionChangeListener."
                        + " Id=\"" + strId + "\""
                        + " FileName=\"" + strFileName + "\""
                        + " observable=" + observable
                        + " oldValue=" + oldValue + " newValue=" + newValue);
            }
        };
        this.textPosCaretPositionProperty.addListener(this.textPosCaretPositionChangeListener);

        // -------------------------------------------------------------------------------------
        this.stmChangeListenerFileContent = (ch) -> {
            boolean booIsEdit = ch.isEdit();
            TextPos tpStart = ch.getStart();
            TextPos tpEnd = ch.getEnd();
            int intLinesAdded = ch.getLinesAdded();
            int intCharsAddedTop = ch.getCharsAddedTop();
            int intCharsAddedBottom = ch.getCharsAddedBottom();
            LOGGER.debug("StyledTextModel.Listener."
                    + " Id=\"" + strId + "\""
                    + " FileName=\"" + strFileName + "\""
                    + " booIsEdit=\"" + booIsEdit + "\""
                    + " intLinesAdded=" + intLinesAdded
                    + " intCharsAddedTop=" + intCharsAddedTop
                    + " intCharsAddedBottom=" + intCharsAddedBottom
                    + " tpStart=\"" + tpStart + "\""
                    + " tpEnd=\"" + tpEnd + "\""
            );
            if (booIsEdit) {
                this.booFileModified = true;
            }
        };
        this.model.addListener(this.stmChangeListenerFileContent);

        // -------------------------------------------------------------------------------------
        this.focusedPropertyChangeListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                hboxState.setVisible(false);
                LOGGER.debug("focusedPropertyChangeListener."
                        + " Id=\"" + strId + "\""
                        + " FileName=\"" + strFileName + "\""
                        + " textPosCaretPosition=" + textPosCaretPosition
                        + " observable=" + observable
                        + " oldValue=" + oldValue + " newValue=" + newValue);
            }
        };
        this.booPropFocusedProperty = this.richTextArea.focusedProperty();
        this.booPropFocusedProperty.addListener(this.focusedPropertyChangeListener);

        // ---------- registerListeners - end ------------------------------------------------
        // -------------------------------------------------------------------------------------
        LOGGER.debug("## Created ContentEditor."
                + " Id=\"" + strId + "\""
                + " FilePath=\"" + strFilePath + "\""
        );
    }

    // -------------------------------------------------------------------------------------
    // Methods
    // -------------------------------------------------------------------------------------
    public void newFile() {

        LOGGER.debug("# New File.");
        if (this.pathFile == null) {
            this.hboxState.visibleProperty().set(true);
            LOGGER.error("Could not create file null.");
            return;
        }
        this.hboxState.visibleProperty().set(false);
    }

    // -------------------------------------------------------------------------------------
    public String openFile() {

        this.hboxState.visibleProperty().set(true);
        LOGGER.debug("# openFile."
                + " booBinary=" + this.booBinary
                + " strCharsetName=\"" + this.strCharsetName + "\""
                + " pathFile=\"" + this.pathFile + "\"");
        if (this.pathFile == null) {
            String strMsg = "Could not open file with Path null.";
            LOGGER.error(strMsg);
            lblFileState.textProperty().set(strMsg);
            return strMsg;
        }

        if (!Files.exists(this.pathFile)) {
            String strMsg = "File does not exist."
                    + " pathFile=\"" + pathFile + "\"";
            LOGGER.error(strMsg);
            lblFileState.textProperty().set(strMsg);
            LOGGER.error(strMsg);
            return strMsg;
        }

        if (Files.isDirectory(this.pathFile)) {
            String strMsg = "Could not read file, it's a directory."
                    + " pathFile=\"" + pathFile + "\"";
            LOGGER.error(strMsg);
            lblFileState.textProperty().set(strMsg);
            LOGGER.error(strMsg);
            return strMsg;
        }

        if (!Files.isReadable(this.pathFile)) {
            String strMsg = "File is not readable."
                    + " pathFile=\"" + pathFile + "\"";
            LOGGER.error(strMsg);
            lblFileState.textProperty().set(strMsg);
            LOGGER.error(strMsg);
            return strMsg;
        }

        File file = this.pathFile.toFile();
        long lngFileSize = file.length();
        if (lngFileSize > Integer.MAX_VALUE) {
            String strMsg = "File is too big."
                    + " FileSize=" + lngFileSize
                    + " pathFile=\"" + pathFile + "\"";
            LOGGER.error(strMsg);
            lblFileState.textProperty().set(strMsg);
            return strMsg;
        }
        int intFileSize = (int) lngFileSize;
        int intFileSizeKb = 0;
        int intFileSizeMb = 0;
        String strFileSize = "";
        if (intFileSize > 1024) {
            intFileSizeKb = intFileSize / 1024;
            strFileSize = "" + intFileSizeKb + "KB";
        }
        if (intFileSizeKb > 1024) {
            intFileSizeMb = intFileSizeKb / 1024;
            strFileSize = "" + intFileSizeMb + "MB";
        }
        LOGGER.debug("# openFile."
                + " lngFileSize=" + lngFileSize
                + " intFileSizeKb=" + intFileSizeKb
                + " intFileSizeMb=" + intFileSizeMb
                + " strFileSize=" + strFileSize
                + " pathFile=\"" + this.pathFile + "\""
                + " Binary=" + this.booBinary
                + " strCharsetName=\"" + this.strCharsetName + "\"");

        this.taskFileLoad = new Task<>() {
            @Override
            protected String call() throws Exception {

                updateMessage("Task File loading started.");
                updateProgress(0, intFileSize);

                Charset charset = Charset.forName(strCharsetName);
                CharsetDecoder charsetDecoder = charset.newDecoder();
                charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE);
                charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
                charsetDecoder.replaceWith(Settings.STR_CHARSET_REPLACE_WITH_DEFAULT);

                LOGGER.debug("Using charset."
                        + " Id=\"" + strId + "\""
                        + " Charset=\"" + charset + "\""
                        + " CharsetDecoder=\"" + charsetDecoder + "\""
                        + " charsetDecoder.malformedInputAction()=\"" + charsetDecoder.malformedInputAction() + "\""
                        + " charsetDecoder.unmappableCharacterAction()=\"" + charsetDecoder.unmappableCharacterAction() + "\""
                        + " charsetDecoder.replacement()=\"" + charsetDecoder.replacement() + "\""
                        + " pathFile=\"" + pathFile + "\""
                );

                int intProgressStep = intFileSize / INT_PROGRESS_BAR_STEPS;
                LOGGER.debug("Loading file."
                        + " Id=\"" + strId + "\""
                        + " FileSize=" + intFileSize
                        + " ProgressStep=" + intProgressStep
                        + " pathFile=\"" + pathFile + "\"");

                int intProgressCounter = 1;
                long lngBytesReadTotal = 0;
                int intRemaining = 0;
                int intBytesReadLast = -1;
                long lngLinesLoaded = 0;
                int intErrors = 0;
                int intErrorsMissingLF = 0;
                int intOsWinCrCount = 0;
                int intOsWinLfCount = 0;
                int intOsUnixCount = 0;
                StringBuilder sbFileContent = new StringBuilder();
                long lngTimeStart = System.currentTimeMillis();

                try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(pathFile, EnumSet.of(StandardOpenOption.READ))) {

                    MappedByteBuffer mbb = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, lngFileSize);
                    mbb.mark();
                    int intPosStart = mbb.position();
                    int intPosEnd;
                    boolean booEolFound = false;
                    while (true) {
                        intRemaining = mbb.remaining();
                        if (intRemaining == 0) {
                            intPosEnd = mbb.position();
                            if (intPosStart == intPosEnd) {
                                break;
                            }
                            // get last line without EOL.
                            String strLine = readLine(mbb, charsetDecoder, intPosStart, intPosEnd);
                            sbFileContent.append(strLine);
                            intBytesReadLast = strLine.length();
                            break;
                        }
                        byte b = mbb.get();
                        intRemaining = mbb.remaining();
                        lngBytesReadTotal++;
                        if (b == BYT_CR) {
                            booEolFound = true;
                            lngLinesLoaded++;
                            intOsWinCrCount++;
                            b = mbb.get();
                            intRemaining = mbb.remaining();
                            lngBytesReadTotal++;
                            if (b == BYT_LF) {
                                intOsWinLfCount++;
                            } else {
                                intErrors++;
                                intErrorsMissingLF++;
                            }
                        } else if (b == BYT_LF) {
                            booEolFound = true;
                            lngLinesLoaded++;
                            intOsUnixCount++;
                        }
                        if (booEolFound) {
                            booEolFound = false;
                            intPosEnd = mbb.position();
                            String strLine = readLine(mbb, charsetDecoder, intPosStart, intPosEnd);
                            /*
                            int intBytesInLine = intPosEnd - intPosStart;
                            byte[] abytLine = new byte[intBytesInLine];
                            mbb.reset();
                            mbb.get(abytLine);
                            ByteBuffer byteBufferLine = ByteBuffer.wrap(abytLine);
                            CharBuffer charBufferLine = charsetDecoder.decode(byteBufferLine);
                            String strLine = charBufferLine.toString();
                            //lstLines.add(strLine);
                             */
                            sbFileContent.append(strLine);
                            intBytesReadLast = strLine.length();
                            mbb.mark();
                            intPosStart = intPosEnd;
                        }
// TODO: fix Progressp for file intFileSize < INT_PROGRESS_BAR_STEPS;                    
                        if (intProgressStep == 1
                                || lngBytesReadTotal >= intProgressStep * intProgressCounter) {
                            long lngBytesTotal = lngBytesReadTotal + intRemaining;
                            //int intListSize = lstLines.size();
                            LOGGER.debug("Reading..."
                                    + " intProgressCounter=" + intProgressCounter
                                    + " lngBytesReadTotal=" + lngBytesReadTotal
                                    + " intRemaining=" + intRemaining
                                    + " lngBytesTotal=" + lngBytesTotal
                                    + " lngLinesLoaded=" + lngLinesLoaded
                                    //+ " intListSize=" + intListSize
                                    + " intOsWinCrCount=" + intOsWinCrCount
                                    + " intOsWinLfCount=" + intOsWinLfCount
                                    + " intErrorsMissingLF=" + intErrorsMissingLF
                                    + " intOsUnixCount=" + intOsUnixCount);
                            updateProgress(lngBytesReadTotal, intFileSize);
                            updateMessage("File Loading " + " steps=" + intProgressCounter * INT_PROGRESS_BAR_STEPS + " bytes=" + lngBytesReadTotal);
                            intProgressCounter++;
                        }
                        /*
                        // For testing only !
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException interrupted) {
                            if (isCancelled()) {
                                updateMessage("Cancelled");
                                break;
                            }
                        }
                         */
                        if (isCancelled()) {
                            updateMessage("Cancelled");
                            break;
                        }
                    }
                    fileChannel.close();
                } catch (Throwable t) {
                    updateMessage("Error loading File." + t.getMessage());
                    LOGGER.error("Could not Read File."
                            + " Id=\"" + strId + "\""
                            + " intUpdateProgressCounter=" + intProgressCounter
                            + " lngBytesReadTotal=" + lngBytesReadTotal
                            + " intBytesReadLast=" + intBytesReadLast
                            + " lngLinesLoaded=" + lngLinesLoaded
                            + " intOsWinCrCount=" + intOsWinCrCount
                            + " intOsWinLfCount=" + intOsWinLfCount
                            + " intErrorsMissingLF=" + intErrorsMissingLF
                            + " intOsUnixCount=" + intOsUnixCount
                            + " pathFile=\"" + pathFile + "\""
                            + " Throwable=\"" + t.toString() + "\"");
                    return sbFileContent.toString();
                }
                updateMessage("File Loaded (" + lngBytesReadTotal + " bytes).");

                String strLineEnding;
                if (intOsWinCrCount > 0 && intOsUnixCount > 0) {
                    strLineEnding = STR_CR_LF_MIX;
                    enuLineEndType = enuLineEnding.Win;
                } else if (intOsWinCrCount > 0) {
                    strLineEnding = STR_CR_LF_WIN;
                    enuLineEndType = enuLineEnding.Win;
                } else if (intOsUnixCount > 0) {
                    strLineEnding = STR_LF_UNIX;
                    enuLineEndType = enuLineEnding.Unix;
                } else {
                    strLineEnding = STR_NO_CR_LF;
                    enuLineEndType = enuLineEnding.No;
                }
                spLineEnding.set(strLineEnding);

                long lngTimeFinish = System.currentTimeMillis();
                long lngTimeTaken = lngTimeFinish - lngTimeStart;
                LOGGER.debug("Loaded file."
                        + " Id=\"" + strId + "\""
                        + " lngBytesReadTotal=" + lngBytesReadTotal
                        + " lngLinesLoaded=" + lngLinesLoaded
                        + " intOsWinCrCount=" + intOsWinCrCount
                        + " intOsWinLfCount=" + intOsWinLfCount
                        + " intErrorsMissingLF=" + intErrorsMissingLF
                        + " intOsUnixCount=" + intOsUnixCount
                        + " strLineEnding=\"" + strLineEnding + "\""
                        + " TimeTaken=" + (float) lngTimeTaken / 1000.00 + " sec" + " (" + lngTimeTaken + " ms}"
                        + " pathFile=\"" + pathFile + "\"");

                return sbFileContent.toString();
            }

            private String readLine(MappedByteBuffer mbb, CharsetDecoder charsetDecoder, int intPosStart, int intPosEnd)
                    throws CharacterCodingException {

                if (intPosEnd <= intPosStart) {
                    return "";
                }
                int intBytesInLine = intPosEnd - intPosStart;
                byte[] abytLine = new byte[intBytesInLine];
                mbb.reset();
                mbb.get(abytLine);
                ByteBuffer byteBufferLine = ByteBuffer.wrap(abytLine);
                CharBuffer charBufferLine = charsetDecoder.decode(byteBufferLine);
                String strLine = charBufferLine.toString();
                return strLine;
            }

        };
        this.processTask();

        LOGGER.debug("# openFile-Task starting."
                + " Id=\"" + strId + "\""
                + " task=\"" + taskFileLoad + "\"");
        new Thread(taskFileLoad).start();
        LOGGER.debug("# openFile-Task started."
                + " Id=\"" + strId + "\""
                + " task=\"" + taskFileLoad + "\"");
        return "";
    }

    // -------------------------------------------------------------------------------------
    public void openFileBinary() {

// TODO: implement read and show binary
        this.booBinary = true;
        this.openFile();
    }

    // -------------------------------------------------------------------------------------
    public boolean saveFile(Path pathFileSaveAs) {
        // Parameters:
        // pathFileSaveAs == null for saving new of existing file.
        // pathFileSaveAs != null for saving File As.

        this.hboxState.visibleProperty().set(true);

        if (pathFileSaveAs == null) {
            String strReason = canSaveFile(this.strId, this.pathFile);
            if (strReason != null) {
                lblFileState.textProperty().set(strReason);
                LOGGER.debug("Could not save File."
                        + " pathFile=\"" + pathFile + "\""
                        + " strReason=\"" + strReason + "\"");
                return false;
            }
        } else {
            this.pathFile = pathFileSaveAs;
            this.parseFilePath(this.strId, this.pathFile);
        }

        if (Settings.BOO_BACKUP_FILES_EABLED) {
//Old            renameFileToBackupReverted(strTabID, fileToSave);
            renameFileToBackup(this.strId, this.pathFile);
        }

        if (this.serviceFileSave == null) {
            this.serviceFileSave = new Service<>() {
                @Override
                protected Task<String> createTask() {
                    return new Task<String>() {
                        @Override
                        protected String call() throws InterruptedException {
                            LOGGER.debug("Service: File Save started."
                                    + " pathFile=\"" + pathFile + "\"");
                            updateMessage("File Save started.");

                            String strText;
                            String strComment = "";
                            int intParagraphToSave = 0;
                            int intParagraphCount = richTextArea.getParagraphCount();
                            if (intParagraphCount == 0) {
                                strText = "";
                                strComment = " empty file";
                                LOGGER.debug("Service: Fle saving empty file."
                                        + " pathFile=\"" + pathFile + "\"");
                            } else {
                                strText = richTextArea.getPlainText(intParagraphToSave);
                            }
                            Charset charset = Charset.forName(strCharsetName);
                            LOGGER.info("Service: File saving" + strComment + "."
                                    + " Id=\"" + strId + "\""
                                    + " ParagraphCount=" + intParagraphCount
                                    + " ParagraphToSave=" + intParagraphToSave
                                    + " charset=\"" + charset + "\""
                                    + " pathFile=\"" + pathFile + "\"");
                            int intParagraphsSaved = 0;
                            long lngCharsWroteTotal = 0;
                            int intTextLen;
                            int intParagraphStepDelta = intParagraphCount / INT_PROGRESS_BAR_STEPS;
                            int intParagraphStep = 0;
                            try (BufferedWriter writer = Files.newBufferedWriter(pathFile, charset)) {
                                while (true) {
                                    intTextLen = strText.length();
                                    //SaveParagraph(writer, strText);
                                    writer.write(strText, 0, intTextLen);
                                    lngCharsWroteTotal += intTextLen;
                                    intParagraphsSaved++;
                                    if (enuLineEndType.equals(enuLineEnding.Win)) {
                                        writer.write(BYT_CR);
                                        writer.write(BYT_LF);
                                    } else if (enuLineEndType.equals(enuLineEnding.Unix)) {
                                        writer.write(BYT_LF);
                                    } else if (intParagraphCount > 1 && intParagraphToSave < intParagraphCount - 1) {
                                        writer.write(BYT_CR);
                                        writer.write(BYT_LF);
                                    }
                                    if (intParagraphsSaved > intParagraphStep) {
                                        intParagraphStep += intParagraphStepDelta;
                                        LOGGER.info("Service: File saving Paragraph."
                                                + " ParagraphCountSaved=" + intParagraphsSaved
                                                + " TextLen=" + intTextLen
                                                + " CharsWroteTotal=" + lngCharsWroteTotal);
                                        updateProgress(intParagraphsSaved, intParagraphCount);
                                        updateMessage("File Saving " + " lines=" + intParagraphsSaved + " chars=" + lngCharsWroteTotal);
                                    }
                                    if (intParagraphsSaved >= intParagraphCount) {
                                        updateProgress(intParagraphCount, intParagraphCount);
                                        updateMessage("File Saved " + " lines=" + intParagraphsSaved
                                                + " chars=" + lngCharsWroteTotal);
                                        break;
                                    }
                                    intParagraphToSave++;
                                    strText = richTextArea.getPlainText(intParagraphToSave);
                                }
                            } catch (Throwable t) {
                                String strMsg = "Could not save file."
                                        + " Id=\"" + strId + "\""
                                        + " intParagraphCount=" + intParagraphCount
                                        + " intParagraphsSaved=" + intParagraphsSaved
                                        + " intParagraphToSave=" + intParagraphToSave
                                        + " pathFile=\"" + pathFile + "\""
                                        + " Throwable=\"" + t.toString() + "\"";
                                LOGGER.error(strMsg);
                                return strMsg;
                            }
                            updateProgress(intParagraphsSaved, intParagraphCount);
                            String strMsg = "File Save finished ("
                                    + intParagraphsSaved + " lines, " + lngCharsWroteTotal + " chars" + ").";
                            updateMessage(strMsg);
                            LOGGER.debug(strMsg + " pathFile=\"" + pathFile + "\"");
                            intFileSaveCount++;
                            return "OK";
                        }

                        boolean SaveParagraph(BufferedWriter writer, String strText) throws IOException {
                            // Not in use now, could be used only for very long paragraphs.
                            int intTextLen;
                            if (strText == null) {
                                intTextLen = 0;
                            } else {
                                intTextLen = strText.length();
                            }

                            int intSteps;
                            if (intTextLen < INT_FILE_LEN_SPLIT) {
                                intSteps = 0;
                            } else {
                                intSteps = intTextLen / INT_PROGRESS_BAR_STEPS;
                            }
                            LOGGER.debug("Service: Saving file."
                                    + " Id=\"" + strId + "\""
                                    + " Len=" + intTextLen
                                    + " Steps=" + intSteps
                                    + " pathFile=\"" + pathFile + "\"");
                            int intFrom = 0;
                            //try (BufferedWriter writer = Files.newBufferedWriter(pathFile, charset)) {
                            if (intSteps == 0) {
                                writer.write(strText, 0, intTextLen);
                                LOGGER.debug("Saving whole paragraph."
                                        + " Id=\"" + strId + "\""
                                        + " Len=" + intTextLen
                                        + " From=" + intFrom
                                        + " pathFile=\"" + pathFile + "\"");
                            } else {
                                int intProgressCounter = 0;
                                long lngBytesWriteTotal = 0;
                                while (intFrom <= intTextLen - intSteps) {
                                    intProgressCounter++;
                                    writer.write(strText, intFrom, intSteps);
                                    intFrom += intSteps;
                                    lngBytesWriteTotal += intSteps;
                                    updateProgress(intFrom, intTextLen);
                                    updateMessage("File Saving " + " step=" + intProgressCounter + " bytes=" + lngBytesWriteTotal);

                                    LOGGER.debug("Saving file."
                                            + " Id=\"" + strId + "\""
                                            + " ProgressCounter=" + intProgressCounter
                                            + " From=" + intFrom
                                            + " pathFile=\"" + pathFile + "\"");
                                    /* 
                                        // For testing only !
                                        try {
                                            Thread.sleep(300);
                                        } catch (InterruptedException interrupted) {
                                            if (isCancelled()) {
                                                updateMessage("Cancelled");
                                                break;
                                            }
                                        }
                                     */
                                }
                                int intRest = intTextLen - intFrom;
                                if (intRest != 0) {
                                    writer.write(strText, intFrom, intRest);
                                    intProgressCounter++;
                                    LOGGER.debug("Saving file last step."
                                            + " Id=\"" + strId + "\""
                                            + " pathFile=\"" + pathFile + "\""
                                            + " intProgressCounter=" + intProgressCounter
                                            + " intFrom=" + intFrom
                                            + " intRest=" + intRest);
                                }
                            }
                            /*} catch (Throwable t) {
                                LOGGER.error("Could not save file."
                                        + " Id=\"" + strId + "\""
                                        + " pathFile=\"" + pathFile + "\""
                                        + " Throwable=\"" + t.toString() + "\"");
                                return false;
                            }*/
                            return true;
                        }
                    };
                }
            };
            this.serviceFileSave.onScheduledProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();
                    event.consume();

                    richTextArea.getModel().removeListener(stmChangeListenerFileContent);
                    textPosCaretPositionProperty.removeListener(textPosCaretPositionChangeListener);
                    booPropFocusedProperty.removeListener(focusedPropertyChangeListener);

                    hboxState.visibleProperty().set(true);
                    lblFileState.textProperty().set("");
                    progressBar.progressProperty().bind(serviceFileSave.progressProperty());
                    lblFileState.textProperty().bind(serviceFileSave.messageProperty());
                    LOGGER.debug("onScheduledProperty serviceFileSave."
                            + " Id=\"" + strId + "\""
                            + " intFileSaveCount=" + intFileSaveCount
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\"");
                }
            });

            this.serviceFileSave.onRunningProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();
                    event.consume();
                    LOGGER.debug("onRunningProperty serviceFileSave."
                            + " Id=\"" + strId + "\""
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\"");
                }
            });

            this.serviceFileSave.onFailedProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();

                    richTextArea.getModel().addListener(stmChangeListenerFileContent);
                    textPosCaretPositionProperty.addListener(textPosCaretPositionChangeListener);
                    booPropFocusedProperty.addListener(focusedPropertyChangeListener);

                    booFileModified = false;
                    progressBar.progressProperty().unbind();
                    lblFileState.textProperty().unbind();
                    String strMsg = serviceFileSave.messageProperty().get();
                    lblFileState.textProperty().set(strMsg);
                    LOGGER.debug("onFailedProperty serviceFileSave."
                            + " Id=\"" + strId + "\""
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\"");
                    event.consume();
                }
            });

            this.serviceFileSave.onSucceededProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();
                    event.consume();

                    richTextArea.getModel().addListener(stmChangeListenerFileContent);
                    textPosCaretPositionProperty.addListener(textPosCaretPositionChangeListener);
                    booPropFocusedProperty.addListener(focusedPropertyChangeListener);

                    booFileModified = false;
                    ReadOnlyObjectProperty<Worker.State> stateProperty = serviceFileSave.stateProperty();
                    Worker.State state = stateProperty.getValue();
                    String stateName = state.name();
                    progressBar.progressProperty().unbind();
                    lblFileState.textProperty().unbind();
                    String strResult = (String) serviceFileSave.getValue();
                    String strMsg = serviceFileSave.getMessage();
                    lblFileState.textProperty().set(strMsg);

                    LOGGER.debug("onSucceededProperty serviceFileSave."
                            + " Id=\"" + strId + "\""
                            + " intFileSaveCount=" + intFileSaveCount
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\""
                            + "\nstateProperty=\"" + stateProperty + "\""
                            + "\nstate=\"" + state + "\""
                            + " stateName=\"" + stateName + "\""
                            + "\nstrMsg=\"" + strMsg + "\""
                            + "\nstrResult=\"" + strResult + "\"");
                }
            });
            LOGGER.debug("Created serviceFileSave."
                    + " Id=\"" + this.strId + "\""
                    + " pathFile=\"" + this.pathFile + "\""
                    + " serviceFileSave=\"" + this.serviceFileSave + "\"");
        }

        this.serviceFileSave.reset();
        LOGGER.info("Saving file."
                + " Id=\"" + this.strId + "\""
                + " pathFile=\"" + this.pathFile + "\"");
        this.serviceFileSave.start();

        this.booFileModified = false;
        return true;
    }

    // -------------------------------------------------------------------------------------
    public boolean closeFile() {

        this.richTextArea.clear();
        return true;
    }

    // -------------------------------------------------------------------------------------
    public int find(String strTextFind, String strTextReplace, boolean booAll) {

        if (strTextFind == null || strTextFind.isEmpty()) {
            LOGGER.error("Text for search not set."
                    + " pathFile=\"" + pathFile + "\"");
            return -1;
        }
        int intParagraphCount = richTextArea.getParagraphCount();
        if (intParagraphCount == 0) {
            LOGGER.debug("Could not search in empty file."
                    + " pathFile=\"" + pathFile + "\"");
            return -1;
        }
        int intCount = 0;
        int intTextFindLen = strTextFind.length();
        if (booAll) {
            // Find all occurrences of text in file content.
            TextPos textposCurrent = this.richTextArea.getCaretPosition(); // for debug only
            this.selectionClear();
            for (int intParagraph = 0; intParagraph < intParagraphCount; intParagraph++) {
                String strText = richTextArea.getPlainText(intParagraph);
                int intPosFound = 0;
                while (intPosFound != -1) {
                    intPosFound = strText.indexOf(strTextFind, intPosFound);
                    if (intPosFound != -1) {
                        intCount++;
                        this.selectTextRange(intParagraph, intPosFound, intPosFound + intTextFindLen, strTextReplace);
                        LOGGER.debug("Found Text."
                                + " Id=\"" + this.strId + "\""
                                + " TextFind=\"" + strTextFind + "\""
                                + " TextReplace=\"" + strTextReplace + "\""
                                + " Paragraph=" + intParagraph
                                + " Count=" + intCount
                                + " PosFound=" + intPosFound);
                        intPosFound += intTextFindLen;
                    }
                }
            }
            return intCount;
        } else {
            // Find first occurent of text from Cursor position
            int intParagraphPos = this.textPosCaretPosition.index();
            int intCaretPos = this.textPosCaretPosition.charIndex();
            if (intParagraphPos >= intParagraphCount) {
                LOGGER.error("Could not find Text because Paragraph Index out of range."
                        + " Id=\"" + this.strId + "\""
                        + " TextFind=\"" + strTextFind + "\""
                        + " ParagraphPos=" + intParagraphPos
                        + " ParagraphCount=" + intParagraphCount);
                this.selectionClear();
                return -1;
            }
            int intPosFound;
            for (int intParagraph = intParagraphPos; intParagraph < intParagraphCount; intParagraph++) {
                String strText = richTextArea.getPlainText(intParagraph);
                intPosFound = strText.indexOf(strTextFind, intCaretPos);
                if (intPosFound >= 0) {
                    this.selectTextRange(intParagraph, intPosFound, intPosFound + intTextFindLen, strTextReplace);
                    LOGGER.debug("Found Text after cursor position."
                            + " Id=\"" + this.strId + "\""
                            + " TextFind=\"" + strTextFind + "\""
                            + " textPosCaretPosition=\"" + textPosCaretPosition + "\""
                            + " Paragraph=" + intParagraph
                            + " PosFound=" + intPosFound);
                    return 1;
                }
                intCaretPos = 0;
            }
            for (int intParagraph = 0; intParagraph <= intParagraphPos; intParagraph++) {
                String strText = richTextArea.getPlainText(intParagraph);
                intPosFound = strText.indexOf(strTextFind, intCaretPos);
                if (intPosFound >= 0) {
                    this.selectTextRange(intParagraph, intPosFound, intPosFound + intTextFindLen, strTextReplace);
                    LOGGER.debug("Found Text before cursor position."
                            + " Id=\"" + this.strId + "\""
                            + " TextFind=\"" + strTextFind + "\""
                            + " TextReplace=\"" + strTextReplace + "\""
                            + " Paragraph=" + intParagraph
                            + " textPosCaretPosition=\"" + textPosCaretPosition + "\""
                            + " PosFound=" + intPosFound);
                    return 1;
                }
            }
            this.selectionClear();
            return -1;
        }
    }

    // -------------------------------------------------------------------------------------
    private void selectTextRange(int intParagraph, int intPosStart, int intPosEnd, String strReplace) {

        String strFontFamily = this.font.getFamily();
        double dblFontSize = this.font.getSize();
// TODO: change selection background.
        StyleAttributeMap mapStyleAttrSelected = StyleAttributeMap.builder()
                .setFontFamily(strFontFamily)
                .setFontSize(dblFontSize)
                //.setBackground(Color.ORANGE)
                .setTextColor(Color.RED)
                .build();

        TextPos textPosStart = new TextPos(intParagraph, intPosStart, intPosStart, false);
        TextPos textPosEnd = new TextPos(intParagraph, intPosEnd, intPosEnd, false);

        this.model.applyStyle(textPosStart, textPosEnd, mapStyleAttrSelected, false);
        this.richTextArea.select(textPosStart, textPosEnd);
        this.richTextArea.requestFocus();

        if (strReplace != null) {
            this.richTextArea.replaceText(textPosStart, textPosEnd, strReplace, true);
        }

        LOGGER.debug("selectTextRange."
                + " Id=\"" + this.strId + "\""
                + " Paragraph=" + intParagraph
                + " textPosCaretPositionProperty=\"" + textPosCaretPositionProperty + "\""
                + " textPosCaretPosition=\"" + textPosCaretPosition + "\""
                + " PosStart=" + intPosStart
                + " PosEnd=" + intPosEnd
                + " textPosStart=\"" + textPosStart + "\""
                + " textPosEnd=\"" + textPosEnd + "\"");
    }

    // -------------------------------------------------------------------------------------
    private void selectionClear() {

        TextPos textPosDocEnd = getDocumentEnd();
        this.model.applyStyle(TextPos.ZERO, textPosDocEnd, this.mapStyleAttrFont, false);
    }

    // -------------------------------------------------------------------------------------
    public long getFileSize() {

        if (this.pathFile == null) {
            return 0;
        } else {
            long lngFileSize;
            try {
                lngFileSize = Files.size(pathFile);
            } catch (IOException ex) {
                LOGGER.error("Could not get File size."
                        + " Id=\"" + this.strId + "\""
                        + " pathFile=\"" + this.pathFile + "\""
                        + " IOException=\"" + ex.toString() + "\"");
                return 0;
            }
            return lngFileSize;
        }
    }

    // -------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------
    private void parseFilePath(final String strId, Path pathFile) {

        if (pathFile == null) {
            this.strFilePath = "";
            this.strFileName = "";
            this.strFileExt = "";
            this.strFileDir = "";
            LOGGER.error("File Path is null."
                    + " Id=\"" + strId + "\"");
        } else {
            this.strFilePath = pathFile.toString();
            final int intFileNamePos = this.strFilePath.lastIndexOf(File.separator);
            if (intFileNamePos < 0) {
                this.strFileDir = "";
                this.strFileName = strFilePath;
            } else {
                this.strFileDir = this.strFilePath.substring(0, intFileNamePos);
                this.strFileName = this.strFilePath.substring(intFileNamePos + 1);
            }

            final int intFileNameExtPos = strFileName.lastIndexOf(".");
            final String strFileNameExt;
            if (intFileNameExtPos < 0 || intFileNameExtPos == 0 || intFileNameExtPos == this.strFileName.length()) {
                strFileNameExt = "";
            } else {
                strFileNameExt = this.strFileName.substring(intFileNameExtPos + 1);
            }
            this.strFileExt = strFileNameExt;
            LOGGER.debug("Parsed FilePath."
                    + " Id=\"" + strId + "\""
                    + " FilePath=\"" + this.pathFile + "(" + this.strFilePath + ")\""
                    + " FileNamePos=\"" + intFileNamePos + "\""
                    + " FileName=\"" + this.strFileName + "\""
                    + " FileDir=\"" + this.strFileDir + "\""
                    + " FileNameExtPos=\"" + intFileNameExtPos + "\""
                    + " FileNameExt=\"" + this.strFileExt + "\"");
        }
    }

    // -------------------------------------------------------------------------------------
    private static String canSaveFile(String strId, Path pathFile) {

        if (pathFile == null) {
            LOGGER.error("Cannot save null File."
                    + " TabId=\"" + strId + "\"");
            return "File is null";
        }

        if (!Files.exists(pathFile)) {
            try {
                Path pathNewFile = Files.createFile(pathFile);
                LOGGER.info("Create New File."
                        + " TabId=\"" + strId + "\""
                        + " pathFile=\"" + pathFile + "\""
                        + " pathNewFile=\"" + pathNewFile + "\"");
            } catch (IOException ex) {
                LOGGER.error("Could not create File."
                        + " TabId=\"" + strId + "\""
                        + " pathFile=\"" + pathFile + "\""
                        + " IOException=\"" + ex.toString() + "\"");
                return "Create File exception:" + ex.toString();
            }
        }
        try {
            if (Files.isDirectory(pathFile)) {
                // It should never happen, but ...
                LOGGER.error("File is directory."
                        + " TabId=\"" + strId + "\""
                        + " pathFile=\"" + pathFile + "\"");
                return "File is directory";
            }
            if (Files.isWritable(pathFile)) {
                return null;
            } else {
                if (Files.isReadable(pathFile)) {
                    return "Could not save Read-only File";
                }
                return "Could not save not writable File";
            }
        } catch (Throwable t) {
            LOGGER.error("Could not analize File because of security violation."
                    + " TabId=\"" + strId + "\""
                    + " pathFile=\"" + pathFile + "\""
                    + " Throwable=\"" + t.toString() + "\"");
            return "File save exception:" + t.toString();
        }
    }

    // -------------------------------------------------------------------------------------
    private static void renameFileToBackup(String strTabId, Path pathFile) {

        if (pathFile == null) {
            LOGGER.error("Could not create *bak File for null File."
                    + " TabId=\"" + strTabId + "\"");
            return;
        }

        if (Files.isDirectory(pathFile)) {
            LOGGER.error("Could not create *bak File for directory."
                    + " TabId=\"" + strTabId + "\""
                    + " pathFile=\"" + pathFile + "\"");
            return;
        }
        if (!Files.exists(pathFile)) {
            LOGGER.error("Could not create *bak File because File does not exist."
                    + " TabId=\"" + strTabId + "\""
                    + " pathFile=\"" + pathFile + "\"");
            return;
        }
        if (!Files.isRegularFile(pathFile)) {
            LOGGER.error("Could not create *bak File because it's not a Regular File."
                    + " TabId=\"" + strTabId + "\""
                    + " pathFile=\"" + pathFile + "\"");
            return;
        }
        if (Settings.BOO_BACKUP_FILES_DAILY_ONLY) {
            FileTime ft;
            try {
                ft = Files.getLastModifiedTime(pathFile);
            } catch (IOException ex) {
                LOGGER.error("Could not get getLastModifiedTime."
                        + " TabId=\"" + strTabId + "\""
                        + " pathFile=\"" + pathFile + "\""
                        + " IOException=\"" + ex.toString() + "\"");
                return;
            }
            long lngFileModifiedDays = ft.to(TimeUnit.DAYS);

            LocalDate localDate = LocalDate.now();
            long lngLocalDateEpochDay = localDate.toEpochDay();

            LOGGER.debug("Compare File Modified Days and Current Day."
                    + " TabId=\"" + strTabId + "\""
                    + " pathFile=\"" + pathFile + "\""
                    + " FileTime=\"" + ft + "\""
                    + " lngFileModifiedDays=\"" + lngFileModifiedDays + "\""
                    + " localDate=\"" + localDate + "\""
                    + " lngLocalDateEpochDay=\"" + lngLocalDateEpochDay + "\"");
            if (lngFileModifiedDays - lngLocalDateEpochDay >= 0) {
                LOGGER.debug("Skip updating backup files."
                        + " TabId=\"" + strTabId + "\""
                        + " pathFile=\"" + pathFile + "\""
                        + " lngFileModifiedDays=\"" + lngFileModifiedDays + "\""
                        + " lngLocalDateEpochDay=\"" + lngLocalDateEpochDay + "\"");
                return;
            }
        }

        // Compute FilePath string without file extension.
        String strFilePath = pathFile.toString();
        String strFilePathNoExt;
        int intPos = strFilePath.lastIndexOf(".");
        if (intPos <= 0) {
            strFilePathNoExt = strFilePath;
        } else {
            strFilePathNoExt = strFilePath.substring(0, intPos);
        }

// TODO:  Always backup Favorites file after editing.
        boolean booFileBackupOldest = true;
        Path pathFileBackupOld = null;
        Path pathFileBackup = null;
        for (int i = Settings.INT_BACKUP_FILES_MAX - 1; i >= 0; i--) {
            String strFileNameBackupCount;
            if (i == 0) {
                strFileNameBackupCount = "";
            } else {
                strFileNameBackupCount = "(" + i + ")";
            }
            String strFilenameBackup = strFilePathNoExt + strFileNameBackupCount + "." + Settings.STR_BACKUP_FILES_EXT;
            pathFileBackup = FileSystems.getDefault().getPath(strFilenameBackup);
            if (Files.exists(pathFileBackup)) {
                if (booFileBackupOldest) {
                    try {
                        Files.deleteIfExists(pathFileBackup);
                    } catch (Throwable t) {
                        LOGGER.error("Could not delete oldest *.bak File."
                                + " TabId=\"" + strTabId + "\""
                                + " FilePathBak=\"" + pathFileBackup + "\""
                                + " Throwable=\"" + t.toString() + "\"");
                        return;
                    }
                } else {
                    try {
                        Files.move(pathFileBackup, pathFileBackupOld);
                    } catch (Throwable t) {
                        LOGGER.error("Could not rename *.bak File."
                                + " TabId=\"" + strTabId + "\""
                                + " pathFile=\"" + pathFile + "\""
                                + " pathFileBackup=\"" + pathFileBackup + "\""
                                + " pathFileBackupOld=\"" + pathFileBackupOld + "\""
                                + " Throwable=\"" + t.toString() + "\"");
                        return;
                    }
                    LOGGER.debug("Renamed *.bak File."
                            + " TabId=\"" + strTabId + "\""
                            + " pathFile=\"" + pathFile + "\""
                            + " pathFileBackup=\"" + pathFileBackup + "\""
                            + " pathFileBackupOld=\"" + pathFileBackupOld + "\"");
                }
            }
            booFileBackupOldest = false;
            pathFileBackupOld = pathFileBackup;
        }
        try {
            Files.move(pathFile, pathFileBackup);
        } catch (Throwable t) {
            LOGGER.error("Could not rename File to *.bak File."
                    + " TabId=\"" + strTabId + "\""
                    + " pathFile=\"" + pathFile + "\""
                    + " pathFileBackup=\"" + pathFileBackup + "\""
                    + " Throwable=\"" + t.toString() + "\"");
            return;
        }
        LOGGER.debug("Renamed *.bak File."
                + " TabId=\"" + strTabId + "\""
                + " pathFile=\"" + pathFile + "\""
                + " pathFileBackup=\"" + pathFileBackup + "\"");
    }

    // -------------------------------------------------------------------------------------
    private void processTask() {

        if (this.taskFileLoad == null) {
        } else {
            ReadOnlyObjectProperty<Worker.State> stateProperty = this.taskFileLoad.stateProperty();
            Worker.State state = stateProperty.getValue();
            String stateName = state.name();

            LOGGER.debug("Got Task."
                    + " Id=\"" + this.strId + "\""
                    + " taskFileLoad=\"" + this.taskFileLoad + "\""
                    + " stateProperty=\"" + stateProperty + "\""
                    + " state=\"" + state + "\""
                    + " stateName=\"" + stateName + "\""
            );

            this.taskFileLoad.onScheduledProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();
                    progressBar.progressProperty().bind(taskFileLoad.progressProperty());
                    lblFileState.textProperty().bind(taskFileLoad.messageProperty());

                    richTextArea.getModel().removeListener(stmChangeListenerFileContent);

                    LOGGER.debug("onScheduledProperty."
                            + " Id=\"" + strId + "\""
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\"");
                    event.consume();

                }
            });

            this.taskFileLoad.onRunningProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();
                    LOGGER.debug("onRunningProperty."
                            + " Id=\"" + strId + "\""
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\"");
                    event.consume();

                }
            });

            this.taskFileLoad.onFailedProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    EventType eventType = event.getEventType();
                    String strErrMsg = lblFileState.textProperty().getValue()
                            + "\nFile=" + lblFileName.getText()
                            + "\nTry to Open File with different Charset or Open File Binary.";
                    lblFileState.textProperty().unbind();
                    lblFileState.textProperty().setValue(strErrMsg);

                    richTextArea.getModel().addListener(stmChangeListenerFileContent);

                    richTextArea.insertText(TextPos.ZERO, strErrMsg, mapStyleAttrFont);
                    booFileModified = false;
                    LOGGER.debug("onFailedProperty."
                            + " Id=\"" + strId + "\""
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\""
                            + " ErrMsg=\"" + strErrMsg + "\"");
                    event.consume();
                }
            });

            this.taskFileLoad.onSucceededProperty().set(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    ReadOnlyObjectProperty<Worker.State> stateProperty = taskFileLoad.stateProperty();
                    Worker.State state = stateProperty.getValue();
                    String stateName = state.name();
                    EventType eventType = event.getEventType();
                    String strText;
                    int intTextLen = 0;
                    try {
                        strText = (String) taskFileLoad.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        LOGGER.error("onSucceededProperty."
                                + " Id=\"" + strId + "\""
                                + " Exception=\"" + ex.toString() + "\"");
                        strText = "";
                    }
                    // TODO: filter text !!!???
                    int intTextLogLimit = 100;
                    String strTextPart = "";
                    if (strText != null) {
                        intTextLen = strText.length();
                        int intTrim = Math.min(intTextLen, intTextLogLimit);
                        strTextPart = strText.substring(0, intTrim) + "\n...";
                    }
                    String strLineEnding = spLineEnding.getValue();
                    LOGGER.trace("onSucceededProperty got File content."
                            + " Id=\"" + strId + "\""
                            + " event=\"" + event + "\""
                            + " stateProperty=\"" + stateProperty + "\""
                            + " state=\"" + state + "\""
                            + " stateName=\"" + stateName + "\""
                            + " strLineEnding=\"" + strLineEnding + "\""
                            + "\nintTextLen=\"" + intTextLen + "\""
                            + "\nstrText=\"" + strTextPart + "\""
                    );
                    event.consume();

                    richTextArea.insertText(TextPos.ZERO, strText, mapStyleAttrFont);
                    richTextArea.setWrapText(booTextWrap);

                    richTextArea.getModel().addListener(stmChangeListenerFileContent);

                    lblFileState.textProperty().unbind();
                    String strMsg = taskFileLoad.getMessage();
                    lblFileState.textProperty().set(strMsg);//.unbind();
                    booFileModified = false;
                    LOGGER.debug("onSucceededProperty set text to textArea."
                            + " Id=\"" + strId + "\""
                            + " eventType=\"" + eventType + "\""
                            + " event=\"" + event + "\""
                            + "\nstrLineEnding=\"" + strLineEnding + "\""
                            + "\nstrMsg=\"" + strMsg + "\""
                            + "\nintTextLen=\"" + intTextLen + "\""
                            + "\nstrText=\"" + strTextPart + "\"");

                    if (tabPane == null) {
                        LOGGER.error("onSucceededProperty tabPane is null."
                                + " Id=\"" + strId + "\""
                                + " eventType=\"" + eventType + "\""
                                + " event=\"" + event + "\"");
                    } else {
                        EventTarget eventTarget = tabPane;
                        EventFileRead eventFileRead = new EventFileRead(this, eventTarget, EventFileRead._FILE_READ);
                        eventFileRead.setId(strId);
                        eventFileRead.setLineEnding(strLineEnding);
                        eventFileRead.setCharsetName(strCharsetName);
                        tabPane.fireEvent(eventFileRead);
                    }
                }
            });
        }
    }

    // -------------------------------------------------------------------------------------
    public final TextPos getDocumentEnd() {
        return (model == null) ? TextPos.ZERO : model.getDocumentEnd();
    }

    // -------------------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------------------
    public final RichTextArea getTextArea() {
        return this.richTextArea;
    }

    public final Path getPathFile() {
        return this.pathFile;
    }

    public final String getFilePath() {
        return this.strFilePath;
    }

    public final String getFileName() {
        return this.strFileName;
    }

    public String getFileExt() {
        return this.strFileExt;
    }

    public final String getFileDir() {
        return this.strFileDir;
    }

    public final boolean isFileModified() {
        if (this.pathFile == null) {
            return false;
        } else {
            return this.booFileModified;
        }
    }

    // -------------------------------------------------------------------------------------
    public boolean isTextWrap() {
        return this.booTextWrap;
    }

    public void setTextWrap(boolean booTextWrap) {
        this.booTextWrap = booTextWrap;
        this.richTextArea.setWrapText(booTextWrap);
    }

    // -------------------------------------------------------------------------------------
    public String getLineEnding() {

        return this.spLineEnding.getValue();
    }

    public void setLineEnding(String strLineEnding) {

        this.spLineEnding.setValue(strLineEnding);
    }

    public void setLineEndingWin() {

        this.spLineEnding.setValue(STR_CR_LF_WIN);
    }

    public void setLineEndingUnix() {

        this.spLineEnding.setValue(STR_LF_UNIX);
    }

    // -------------------------------------------------------------------------------------
    public String getCharsetName() {
        return this.strCharsetName;
    }

    public void setCharsetName(String strCharsetName) {
        this.strCharsetName = strCharsetName;
    }

    // -------------------------------------------------------------------------------------
    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
        String strFontFamily = font.getFamily();
        double dblFontSize = font.getSize();
        this.mapStyleAttrFont = StyleAttributeMap.builder()
                .setFontFamily(strFontFamily)
                .setFontSize(dblFontSize).build();
        TextPos textPosDocEnd = getDocumentEnd();
        this.richTextArea.applyStyle(TextPos.ZERO, textPosDocEnd, this.mapStyleAttrFont);
    }

    // -------------------------------------------------------------------------------------
    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
    }

    // -------------------------------------------------------------------------------------
}
