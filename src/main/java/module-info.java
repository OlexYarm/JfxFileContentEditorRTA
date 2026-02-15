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
module com.olexyarm.jfxfilecontenteditor {
    requires transitive javafx.graphics;
    requires transitive javafx.fxml;
    requires javafx.controls;
    requires jfx.incubator.richtext;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires java.desktop;

    opens com.olexyarm.jfxfilecontenteditor to javafx.fxml;

    exports com.olexyarm.jfxfilecontenteditor to javafx.graphics;
}
