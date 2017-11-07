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

package org.jetbrains.plugins.scala.lang.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testcases.ScalaFileSetTestCase;

/**
 * @author Ilya.Sergey
 */
public abstract class ActionTestBase extends ScalaFileSetTestCase {

  protected int myOffset;

  protected ActionTestBase(String... pathSegments) {
    super(pathSegments);
  }

  /**
   * Runs editor action
   */
  public static void runAsWriteAction(final Runnable runnable) {
    ApplicationManager.getApplication().runWriteAction(runnable);
  }

  /**
   * Returns context for action performing
   */
  protected myDataContext getDataContext(PsiFile file) throws InvalidDataException {
    return new myDataContext(file);
  }

  /**
   * Performs specified action
   */
  public static void performAction(final Project project, final Runnable action) {
    runAsWriteAction(new Runnable() {
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, action, "Execution", null);
      }
    });
  }

  /**
   * Implements current DataContext logic
   */
  public class myDataContext implements DataContext, DataProvider {

    PsiFile myFile;

    public myDataContext(PsiFile file) {
      myFile = file;
    }
    @Nullable
    public Object getData(@NonNls String dataId) {
      if (LangDataKeys.LANGUAGE.is(dataId)) return myFile.getLanguage();
      if (PlatformDataKeys.PROJECT.is(dataId)) return myFile.getProject();
      return null;
    }
  }

}
