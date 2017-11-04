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

package org.jetbrains.plugins.scala.testcases;

import com.intellij.FileSetTestCase;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.plugins.scala.ScalaLanguage;
import org.jetbrains.plugins.scala.ScalaLoader;
import org.jetbrains.plugins.scala.util.TestUtils;

import java.nio.file.Paths;

public abstract class ScalaFileSetTestCase extends FileSetTestCase {

    protected ScalaFileSetTestCase(String... pathSegments) {
        super(getPath(pathSegments));
    }

    @Override
    protected void setUp() {
        super.setUp();
        ScalaLoader.loadScala();
    }

    @Override
    public String transform(String s, String[] strings) {
        CommonCodeStyleSettings settings = CodeStyleSettingsManager.getSettings(myProject)
                .getCommonSettings(ScalaLanguage.INSTANCE);
        withSettings(settings);

        return null;
    }

    protected void withSettings(CommonCodeStyleSettings settings) {
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions();
        indentOptions.INDENT_SIZE = 2;
        indentOptions.CONTINUATION_INDENT_SIZE = 2;
        indentOptions.TAB_SIZE = 2;
    }

    private static String getPath(String... pathSegments) {
        String pathProperty = System.getProperty("path");
        return pathProperty != null ?
                pathProperty :
                Paths.get(TestUtils.getTestDataPath(), pathSegments).toString();
    }
}
