package org.jetbrains.plugins.scala.lang.actions.editor.backspace;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.IncorrectOperationException;
import junit.framework.Test;
import org.jetbrains.plugins.scala.lang.actions.ActionTestBase;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import static com.intellij.testFramework.EditorTestUtil.CARET_TAG;
import static org.jetbrains.plugins.scala.util.TestUtils.removeCaretMarker;

/**
 * User: Alexander Podkhalyuzin
 * Date: 03.07.2008
 */
@RunWith(AllTests.class)
public class BackspaceActionTest extends ActionTestBase {

  protected Editor myEditor;
  protected FileEditorManager fileEditorManager;
  protected PsiFile myFile;

  public BackspaceActionTest() {
    super("actions", "editor", "backspace", "data");
  }


  protected EditorActionHandler getMyHandler() {
    return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE);
  }


  private String processFile(final PsiFile file) throws IncorrectOperationException, InvalidDataException {
    String result;
    String fileText = file.getText();
    int offset = fileText.indexOf(CARET_TAG);
    fileText = removeCaretMarker(fileText, offset);
    myFile = createScalaFileFromText(fileText);
    fileEditorManager = FileEditorManager.getInstance(LightPlatformTestCase.getProject());
    myEditor = fileEditorManager.openTextEditor(new OpenFileDescriptor(myProject, myFile.getVirtualFile(), 0), false);
    assert myEditor != null;
    myEditor.getCaretModel().moveToOffset(offset);

    final myDataContext dataContext = getDataContext(myFile);
    final EditorActionHandler handler = getMyHandler();

    try {
      performAction(myProject, new Runnable() {
        public void run() {
          handler.execute(myEditor, myEditor.getCaretModel().getCurrentCaret(), dataContext);
        }
      });
      offset = myEditor.getCaretModel().getOffset();
      result = myEditor.getDocument().getText();
      result = result.substring(0, offset) + CARET_TAG + result.substring(offset);
    } finally {
      fileEditorManager.closeFile(myFile.getVirtualFile());
      myEditor = null;
    }

    return result;
  }

  public String transform(String testName, String[] data) {
    super.transform(testName, data);

    PsiFile psiFile = createScalaFileFrom(data);
    return processFile(psiFile);
  }


  public static Test suite() {
    return new BackspaceActionTest();
  }
}
