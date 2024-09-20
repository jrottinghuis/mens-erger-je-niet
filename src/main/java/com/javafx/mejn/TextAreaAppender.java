/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javafx.mejn;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TextArea;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;

@Plugin(name = "TextAreaAppender", category = "Core", elementType = "appender", printObject = true)
public class TextAreaAppender extends AbstractAppender {

    private static TextArea textArea = null;
    private static BooleanProperty isConsoleEnabled = null;

    private int maxCharacters = 1000;

    private TextAreaAppender(String name, Layout<?> layout, Filter filter, int maxCharacters, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, null);
        this.maxCharacters = maxCharacters;
    }

    public static void setTextArea(TextArea textArea, BooleanProperty isConsoleEnabled) {
        TextAreaAppender.textArea = textArea;
        TextAreaAppender.isConsoleEnabled = isConsoleEnabled;
    }

    @SuppressWarnings("unused")
    @PluginFactory
    public static TextAreaAppender createAppender(@PluginAttribute("name") String name,
                                                   @PluginAttribute("maxCharacters") int maxCharacters,
                                                   @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                   @PluginElement("Layout") Layout<?> layout,
                                                   @PluginElement("Filters") Filter filter)
    {
        if (name == null) {
            LOGGER.error("No name provided for TextAreaAppender");
            return null;
        }
        if (layout == null) {
            layout = createDefaultLayout();
        }
        return new TextAreaAppender(name, layout, filter, maxCharacters, ignoreExceptions);
    }

    @Override
    public void append(org.apache.logging.log4j.core.LogEvent event) {

        if (textArea == null || isConsoleEnabled == null || !isConsoleEnabled.get()) {
            return;
        }

        String message = new String(getLayout().toByteArray(event));
        // message = event.getMessage().getFormattedMessage() + "\n"
        try {
            Platform.runLater(() -> {
                if (textArea.getText().length() >= maxCharacters) {
                    int firstNewLineIndex = textArea.getText().indexOf("\n");
                    if (firstNewLineIndex != -1) {
                        textArea.setText(textArea.getText().substring(firstNewLineIndex + 1));
                    }
                }
                textArea.appendText(message);
            });
        } catch (Throwable trowable) {
            LOGGER.error("Error appending log event to TextArea in TextAreaAppender", trowable);
        }
    }
}
