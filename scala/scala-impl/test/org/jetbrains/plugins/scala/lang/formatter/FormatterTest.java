/*
 * Copyright 2000-2008 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.scala.lang.formatter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import junit.framework.Test;
import org.jetbrains.plugins.scala.testcases.ScalaFileSetTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Created by IntelliJ IDEA.
 * User: Ilya.Sergey
 */

@RunWith(AllTests.class)
public class FormatterTest extends ScalaFileSetTestCase {

  protected FormatterTest(String... pathSegments) {
    super(pathSegments);
  }

  public FormatterTest() {
    this("formatter", "data");
  }

  protected void performFormatting(final Project project, final PsiFile file) throws IncorrectOperationException {
    TextRange myTextRange = file.getTextRange();
    CodeStyleManager.getInstance(project).reformatText(file, myTextRange.getStartOffset(), myTextRange.getEndOffset());
  }

  public String transform(String testName, String[] data) {
    super.transform(testName, data);

    PsiFile psiFile = createScalaFileFrom(data);
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            try {
              performFormatting(myProject, psiFile);
            } catch (IncorrectOperationException e) {
              e.printStackTrace();
            }
          }
        });
      }
    }, null, null);
    return psiFile.getText();
  }

  public static Test suite() {
    return new FormatterTest();
  }

}



