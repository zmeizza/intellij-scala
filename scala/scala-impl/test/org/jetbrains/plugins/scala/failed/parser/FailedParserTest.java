package org.jetbrains.plugins.scala.failed.parser;

/**
 * @author Nikolay.Tropin
 */

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import junit.framework.Test;
import org.jetbrains.plugins.scala.testcases.ScalaFileSetTestCase;
import org.jetbrains.plugins.scala.util.TestUtils;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public abstract class FailedParserTest extends ScalaFileSetTestCase {

    FailedParserTest() {
        super("parser", "failed");
    }

    public String transform(String testName, String[] data) {
        super.transform(testName, data);

        String fileText = data[0];
        PsiFile psiFile = TestUtils.createPseudoPhysicalScalaFile(myProject, fileText);

        return DebugUtil.psiToString(psiFile, false).replace(":" + psiFile.getName(), "");

    }

    public static Test suite() {
        return new ScalaFailedParserTest();
    }
}

