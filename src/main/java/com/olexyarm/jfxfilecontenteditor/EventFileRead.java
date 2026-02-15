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

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventFileRead extends Event { 

    private static final Logger LOGGER = LoggerFactory.getLogger(EventFileRead.class);
    public static final EventType<EventFileRead> _FILE_READ = new EventType<>(ANY, "_FILE_READ");
    private static final long serialVersionUID = 1L;

    private String strId;
    private String strLineEnding;
    private String strCharsetName;

    public EventFileRead(Object obj, EventTarget eventTarget, EventType<? extends Event> eventType) {

        super(obj, eventTarget, eventType);

        if (obj == null) {
            LOGGER.error("EventFileRead Constructor."
                    + " eventType=\"" + eventType + "\""
                    + " eventTarget=\"" + eventTarget + "\""
                    + " obj=\"" + obj + "\"");
        } else {
            LOGGER.debug("EventFileRead Constructor."
                    + " eventType=\"" + eventType + "\""
                    + " eventTarget=\"" + eventTarget + "\""
                    + " obj.getClass=\"" + obj.getClass() + "\""
                    + " obj=\"" + obj + "\""
            );
        }
    }

    // -------------------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------------------
    public String getId() {
        return this.strId;
    }

    public void setId(String strId) {
        this.strId = strId;
    }

    public String getLineEnding() {
        return this.strLineEnding;
    }

    public void setLineEnding(String strLineEnding) {
        this.strLineEnding = strLineEnding;
    }

    public String getCharsetName() {
        return this.strCharsetName;
    }

    public void setCharsetName(String strCharsetName) {
        this.strCharsetName = strCharsetName;
    }
    // -------------------------------------------------------------------------------------
}
