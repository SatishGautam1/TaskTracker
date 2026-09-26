package com.tasktracker.ui;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;

/**
 * A small Markdown editor with a toolbar and a live HTML preview.
 * <p>
 * Security note: task descriptions are user-supplied text. Flexmark, by
 * default, passes raw HTML embedded in the Markdown straight through to the
 * rendered output, and the preview WebView executes JavaScript. Together
 * that means a task body such as {@code <img src=x onerror=alert(1)>} would
 * have run script inside the app. Both are switched off below.
 */
public class MarkdownEditor {

    private final TextArea editor;
    private final WebView preview;
    private final BorderPane root;

    private final Parser parser;
    private final HtmlRenderer renderer;

    // Reloading the WebView content on every keystroke is wasteful and can
    // make typing feel laggy in longer descriptions, so preview updates are
    // coalesced with a short pause instead of firing on every character.
    private final PauseTransition previewDebounce =
            new PauseTransition(Duration.millis(180));

    public MarkdownEditor() {

        MutableDataSet options = new MutableDataSet();

        // Do not render raw HTML/script embedded in the markdown source.
        options.set(HtmlRenderer.SUPPRESS_HTML_BLOCKS, true);
        options.set(HtmlRenderer.SUPPRESS_INLINE_HTML, true);

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        editor = new TextArea();
        editor.setWrapText(true);
        editor.setPromptText("Write your task description using Markdown...");
        editor.getStyleClass().add("markdown-input");

        preview = new WebView();
        preview.setContextMenuEnabled(true);
        preview.getEngine().setJavaScriptEnabled(false);

        root = new BorderPane();
        root.getStyleClass().add("markdown-editor");

        previewDebounce.setOnFinished(event -> renderPreview());

        createToolbar();

        editor.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    previewDebounce.stop();
                    previewDebounce.playFromStart();
                }
        );

        renderPreview();
    }

    private void createToolbar() {

        Button boldButton = createButton("B", "Bold");
        Button italicButton = createButton("I", "Italic");
        Button heading1Button = createButton("H1", "Heading 1");
        Button heading2Button = createButton("H2", "Heading 2");
        Button listButton = createButton("• List", "Bullet list");
        Button checklistButton = createButton("☑", "Checklist");
        Button linkButton = createButton("Link", "Insert link");
        Button codeButton = createButton("</>", "Code block");

        boldButton.setOnAction(event -> insertAroundSelection("**", "**"));
        italicButton.setOnAction(event -> insertAroundSelection("*", "*"));
        heading1Button.setOnAction(event -> insertLinePrefix("# "));
        heading2Button.setOnAction(event -> insertLinePrefix("## "));
        listButton.setOnAction(event -> insertLinePrefix("- "));
        checklistButton.setOnAction(event -> insertLinePrefix("- [ ] "));
        linkButton.setOnAction(event -> insertLink());
        codeButton.setOnAction(event -> insertCodeBlock());

        HBox toolbar = new HBox(
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

        toolbar.setPadding(new Insets(8));
        toolbar.getStyleClass().add("markdown-toolbar");

        VBox.setVgrow(editor, Priority.ALWAYS);
        VBox.setVgrow(preview, Priority.ALWAYS);

        BorderPane editorPane = new BorderPane();
        editorPane.setTop(createSectionLabel("Markdown"));
        editorPane.setCenter(editor);

        BorderPane previewPane = new BorderPane();
        previewPane.setTop(createSectionLabel("Preview"));
        previewPane.setCenter(preview);

        SplitPane splitPane = new SplitPane(editorPane, previewPane);
        splitPane.setDividerPositions(0.5);

        root.setTop(toolbar);
        root.setCenter(splitPane);
    }

    private Label createSectionLabel(String text) {

        Label label = new Label(text);
        label.setPadding(new Insets(6, 8, 6, 8));
        label.getStyleClass().add("markdown-section-label");

        return label;
    }

    private Button createButton(String text, String tooltip) {

        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add("toolbar-button");

        return button;
    }

    private void insertAroundSelection(String before, String after) {

        String selected = editor.getSelectedText();

        if (selected == null || selected.isEmpty()) {

            int caret = editor.getCaretPosition();

            editor.insertText(caret, before + after);
            editor.positionCaret(caret + before.length());

            return;
        }

        int start = editor.getSelection().getStart();

        editor.replaceSelection(before + selected + after);
        editor.selectRange(start + before.length(), start + before.length() + selected.length());
    }

    private void insertLinePrefix(String prefix) {

        int caret = editor.getCaretPosition();
        String text = editor.getText();

        int lineStart = text.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;

        editor.insertText(lineStart, prefix);
        editor.positionCaret(caret + prefix.length());
    }

    private void insertLink() {

        String selected = editor.getSelectedText();

        if (selected == null || selected.isEmpty()) {
            editor.insertText(editor.getCaretPosition(), "[link text](https://example.com)");
            return;
        }

        editor.replaceSelection("[" + selected + "](https://example.com)");
    }

    private void insertCodeBlock() {

        String selected = editor.getSelectedText();

        if (selected == null) {
            selected = "";
        }

        editor.replaceSelection("```\n" + selected + "\n```");
    }

    private void renderPreview() {

        String markdown = editor.getText();

        if (markdown == null) {
            markdown = "";
        }

        String body = renderer.render(parser.parse(markdown));

        String html =
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: -apple-system, Segoe UI, Arial, sans-serif;
                            padding: 15px;
                            line-height: 1.55;
                            color: #1f2430;
                        }
                        h1 { font-size: 26px; }
                        h2 { font-size: 20px; }
                        code {
                            background: #f1f2f6;
                            padding: 2px 5px;
                            border-radius: 4px;
                            font-size: 13px;
                        }
                        pre {
                            background: #f5f6f8;
                            padding: 12px;
                            border-radius: 8px;
                            overflow-x: auto;
                        }
                        blockquote {
                            border-left: 4px solid #cbd2e0;
                            margin-left: 0;
                            padding-left: 12px;
                            color: #5b6472;
                        }
                        a { color: #2563eb; }
                    </style>
                </head>
                <body>
                """
                + body +
                """
                </body>
                </html>
                """;

        preview.getEngine().loadContent(html);
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
        renderPreview();
    }
}
