package com.tasktracker.ui;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class MarkdownEditor {

    private final TextArea editor;
    private final WebView preview;
    private final BorderPane root;

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownEditor() {

        MutableDataSet options = new MutableDataSet();

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        editor = new TextArea();
        editor.setWrapText(true);

        editor.setPromptText(
                "Write your task description using Markdown..."
        );

        preview = new WebView();

        preview.setContextMenuEnabled(true);

        root = new BorderPane();

        createToolbar();

        editor.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        updatePreview()
        );

        updatePreview();
    }

    private void createToolbar() {

        Button boldButton =
                createButton(
                        "B",
                        "Bold"
                );

        Button italicButton =
                createButton(
                        "I",
                        "Italic"
                );

        Button heading1Button =
                createButton(
                        "H1",
                        "Heading 1"
                );

        Button heading2Button =
                createButton(
                        "H2",
                        "Heading 2"
                );

        Button listButton =
                createButton(
                        "• List",
                        "Bullet list"
                );

        Button checklistButton =
                createButton(
                        "☑",
                        "Checklist"
                );

        Button linkButton =
                createButton(
                        "Link",
                        "Insert link"
                );

        Button codeButton =
                createButton(
                        "</>",
                        "Code block"
                );

        boldButton.setOnAction(
                event ->
                        insertAroundSelection(
                                "**",
                                "**"
                        )
        );

        italicButton.setOnAction(
                event ->
                        insertAroundSelection(
                                "*",
                                "*"
                        )
        );

        heading1Button.setOnAction(
                event ->
                        insertLinePrefix("# ")
        );

        heading2Button.setOnAction(
                event ->
                        insertLinePrefix("## ")
        );

        listButton.setOnAction(
                event ->
                        insertLinePrefix("- ")
        );

        checklistButton.setOnAction(
                event ->
                        insertLinePrefix("- [ ] ")
        );

        linkButton.setOnAction(
                event ->
                        insertLink()
        );

        codeButton.setOnAction(
                event ->
                        insertCodeBlock()
        );

        HBox toolbar =
                new HBox(
                        6,
                        boldButton,
                        italicButton,
                        heading1Button,
                        heading2Button,
                        listButton,
                        checklistButton,
                        linkButton,
                        codeButton
                );

        toolbar.setPadding(
                new Insets(8)
        );

        toolbar.setStyle(
                "-fx-background-color: #f5f6f8;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 0 0 1 0;"
        );

        VBox.setVgrow(editor, Priority.ALWAYS);
        VBox.setVgrow(preview, Priority.ALWAYS);

        VBox editorBox =
                new VBox(editor);

        VBox previewBox =
                new VBox(preview);

        VBox.setVgrow(editorBox, Priority.ALWAYS);
        VBox.setVgrow(previewBox, Priority.ALWAYS);

        BorderPane editorPane =
                new BorderPane();

        editorPane.setTop(
                createSectionLabel("Markdown")
        );

        editorPane.setCenter(editor);

        BorderPane previewPane =
                new BorderPane();

        previewPane.setTop(
                createSectionLabel("Preview")
        );

        previewPane.setCenter(preview);

        javafx.scene.control.SplitPane splitPane =
                new javafx.scene.control.SplitPane(
                        editorPane,
                        previewPane
                );

        splitPane.setDividerPositions(0.5);

        root.setTop(toolbar);
        root.setCenter(splitPane);
    }

    private javafx.scene.control.Label createSectionLabel(
            String text
    ) {

        javafx.scene.control.Label label =
                new javafx.scene.control.Label(text);

        label.setPadding(
                new Insets(6, 8, 6, 8)
        );

        label.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-background-color: #eeeeee;"
        );

        return label;
    }

    private Button createButton(
            String text,
            String tooltip
    ) {

        Button button =
                new Button(text);

        button.setTooltip(
                new Tooltip(tooltip)
        );

        return button;
    }

    private void insertAroundSelection(
            String before,
            String after
    ) {

        String selected =
                editor.getSelectedText();

        if (selected == null || selected.isEmpty()) {

            editor.insertText(
                    editor.getCaretPosition(),
                    before + after
            );

            editor.positionCaret(
                    editor.getCaretPosition() - after.length()
            );

            return;
        }

        editor.replaceSelection(
                before + selected + after
        );
    }

    private void insertLinePrefix(
            String prefix
    ) {

        int caret =
                editor.getCaretPosition();

        String text =
                editor.getText();

        int lineStart =
                text.lastIndexOf(
                        '\n',
                        Math.max(0, caret - 1)
                ) + 1;

        editor.insertText(
                lineStart,
                prefix
        );

        editor.positionCaret(
                caret + prefix.length()
        );
    }

    private void insertLink() {

        String selected =
                editor.getSelectedText();

        if (selected == null || selected.isEmpty()) {

            editor.insertText(
                    editor.getCaretPosition(),
                    "[link text](https://example.com)"
            );

            return;
        }

        editor.replaceSelection(
                "[" +
                selected +
                "](https://example.com)"
        );
    }

    private void insertCodeBlock() {

        String selected =
                editor.getSelectedText();

        if (selected == null) {
            selected = "";
        }

        editor.replaceSelection(
                "```\n" +
                selected +
                "\n```"
        );
    }

    private void updatePreview() {

        String markdown =
                editor.getText();

        if (markdown == null) {
            markdown = "";
        }

        String body =
                renderer.render(
                        parser.parse(markdown)
                );

        String html =
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            padding: 15px;
                            line-height: 1.5;
                            color: #222222;
                        }

                        h1 {
                            font-size: 28px;
                        }

                        h2 {
                            font-size: 22px;
                        }

                        code {
                            background: #f1f1f1;
                            padding: 2px 5px;
                            border-radius: 4px;
                        }

                        pre {
                            background: #f5f5f5;
                            padding: 12px;
                            border-radius: 6px;
                            overflow-x: auto;
                        }

                        blockquote {
                            border-left: 4px solid #cccccc;
                            padding-left: 12px;
                            color: #666666;
                        }

                        a {
                            color: #2563eb;
                        }
                    </style>
                </head>
                <body>
                """
                + body +
                """
                </body>
                </html>
                """;

        preview
                .getEngine()
                .loadContent(html);
    }

    public Node getView() {
        return root;
    }

    public String getMarkdown() {
        return editor.getText();
    }

    public void setMarkdown(String markdown) {

        if (markdown == null) {
            markdown = "";
        }

        editor.setText(markdown);
    }
}