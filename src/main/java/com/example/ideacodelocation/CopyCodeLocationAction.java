package com.example.ideacodelocation;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 复制当前 Java 代码位置，便于在代码评审、飞书、Jira、PR 中引用。
 *
 * @author 孟祥宇
 */
public class CopyCodeLocationAction extends AnAction implements IntentionAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (project == null || editor == null || psiFile == null) {
            return;
        }
        performCopy(project, editor, psiFile);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        event.getPresentation().setEnabledAndVisible(editor != null && isSupportedFile(psiFile));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public @Nls @NotNull String getText() {
        return "Copy Class Location";
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
        return "Copy Class Location";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiFile psiFile) {
        return editor != null && isSupportedFile(psiFile);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
        performCopy(project, editor, psiFile);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private static void performCopy(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
        LineRange lineRange = resolveLineRange(editor);
        int targetOffset = resolveTargetOffset(editor);
        String qualifiedClassName = resolveQualifiedClassName(psiFile, targetOffset);
        if (qualifiedClassName == null) {
            Messages.showWarningDialog(project, "当前文件没有找到 Java 类。", "Copy Class Location");
            return;
        }

        String location = formatLocation(qualifiedClassName, lineRange);
        CopyPasteManager.getInstance().setContents(new StringSelection(location));
        StatusBar.Info.set("Copied: " + location, project);
    }

    /**
     * 从选区计算 1-based 行号；无选区时取当前光标行。
     *
     * @author 孟祥宇
     */
    private static LineRange resolveLineRange(Editor editor) {
        Document document = editor.getDocument();
        int startOffset = resolveTargetOffset(editor);
        int endOffset = startOffset;
        if (editor.getSelectionModel().hasSelection()) {
            startOffset = editor.getSelectionModel().getSelectionStart();
            endOffset = editor.getSelectionModel().getSelectionEnd();
            // IDEA 整行选择时 endOffset 常落在下一行开头，减 1 避免多复制一行。
            if (endOffset > startOffset) {
                endOffset--;
            }
        }

        int startLine = document.getLineNumber(startOffset) + 1;
        int endLine = document.getLineNumber(endOffset) + 1;
        return new LineRange(startLine, endLine);
    }

    /**
     * 取选区起点或光标位置，用于定位当前 PSI 类。
     *
     * @author 孟祥宇
     */
    private static int resolveTargetOffset(Editor editor) {
        if (editor.getSelectionModel().hasSelection()) {
            return editor.getSelectionModel().getSelectionStart();
        }
        return editor.getCaretModel().getOffset();
    }

    /**
     * 优先使用 PSI 获取包名和类名，不读取源码文本。
     *
     * @author 孟祥宇
     */
    private static String resolveQualifiedClassName(PsiFile psiFile, int offset) {
        String packageName = resolvePackageName(psiFile);
        String className = resolveClassName(psiFile, offset);
        if (className == null || className.isEmpty()) {
            return null;
        }
        if (packageName == null || packageName.isEmpty()) {
            return className;
        }
        return packageName + "." + className;
    }

    /**
     * 使用 Java PSI 文件对象读取包名。
     *
     * @author 孟祥宇
     */
    private static String resolvePackageName(PsiFile psiFile) {
        if (psiFile instanceof PsiJavaFile) {
            return ((PsiJavaFile) psiFile).getPackageName();
        }
        return null;
    }

    /**
     * 优先取光标/选区所在类；不在类体内时，回退到文件中的第一个类。
     *
     * @author 孟祥宇
     */
    private static String resolveClassName(PsiFile psiFile, int offset) {
        PsiElement element = psiFile.findElementAt(Math.max(0, Math.min(offset, psiFile.getTextLength())));
        if (psiFile instanceof PsiJavaFile) {
            PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
            if (psiClass == null) {
                PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
                psiClass = classes.length > 0 ? classes[0] : null;
            }
            return buildJavaClassName(psiClass);
        }

        return null;
    }

    /**
     * 保留嵌套类层级，避免引用 Inner 时丢失 Outer 背景。
     *
     * @author 孟祥宇
     */
    private static String buildJavaClassName(PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }

        Deque<String> names = new ArrayDeque<>();
        PsiClass current = psiClass;
        while (current != null) {
            if (current.getName() != null) {
                names.addFirst(current.getName());
            }
            current = current.getContainingClass();
        }
        return String.join(".", names);
    }

    /**
     * 单行输出 package.Class:128，多行输出 package.Class:128-147。
     *
     * @author 孟祥宇
     */
    private static String formatLocation(String qualifiedClassName, LineRange lineRange) {
        if (lineRange.startLine == lineRange.endLine) {
            return qualifiedClassName + ":" + lineRange.startLine;
        }
        return qualifiedClassName + ":" + lineRange.startLine + "-" + lineRange.endLine;
    }

    /**
     * 仅在 Java 文件中启用 Action。
     *
     * @author 孟祥宇
     */
    private static boolean isSupportedFile(PsiFile psiFile) {
        return psiFile instanceof PsiJavaFile;
    }

    /**
     * 选区行号范围。
     *
     * @author 孟祥宇
     */
    private static class LineRange {
        private final int startLine;
        private final int endLine;

        private LineRange(int startLine, int endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}
