package org.jetbrains.plugins.scala.lang.actions.editor.enter;

import com.intellij.codeInsight.CodeInsightSettings;
import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * @author Alexander Podkhalyuzin
 */
@RunWith(AllTests.class)
public class EnterActionTest extends AbstractEnterActionTestBase {

  public EnterActionTest() {
    super("actions", "editor", "enter", "data");
  }

  @Override
  protected void setUp() {
    super.setUp();

    CodeInsightSettings.getInstance().JAVADOC_STUB_ON_ENTER = false; //No, we don't need it.
  }

  @Override
  protected void tearDown() {
    CodeInsightSettings.getInstance().JAVADOC_STUB_ON_ENTER = true;

    super.tearDown();
  }

  public static Test suite() {
    return new EnterActionTest();
  }
}
