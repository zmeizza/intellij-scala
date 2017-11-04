package org.jetbrains.plugins.scala.lang.formatter.tests;

import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import junit.framework.Test;
import org.jetbrains.plugins.scala.lang.formatter.FormatterTest;
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * User: Dmitry Naydanov
 * Date: 4/16/12
 */
@RunWith(AllTests.class)
public class MultiLineStringFormatterTest extends FormatterTest {

  public MultiLineStringFormatterTest() {
      super("formatter", "multiLineStringData");
  }

  @Override
  protected void withSettings(CommonCodeStyleSettings settings) {
    super.withSettings(settings);

    ScalaCodeStyleSettings scalaSettings = settings.getRootSettings().getCustomSettings(ScalaCodeStyleSettings.class);

    scalaSettings.MULTILINE_STRING_SUPORT = ScalaCodeStyleSettings.MULTILINE_STRING_ALL;
    scalaSettings.KEEP_MULTI_LINE_QUOTES = false;
    scalaSettings.MULTI_LINE_QUOTES_ON_NEW_LINE = true;
    settings.ALIGN_MULTILINE_BINARY_OPERATION = true;
    scalaSettings.MULTI_LINE_STRING_MARGIN_INDENT = 3;
  }

  public static Test suite() {
    return new MultiLineStringFormatterTest();
  }
}
