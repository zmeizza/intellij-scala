package org.jetbrains.plugins.scala.lang.parser.stress;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.Test;
import org.jetbrains.plugins.scala.ScalaFileType;
import org.jetbrains.plugins.scala.lang.parser.ScalaParserDefinition;
import org.jetbrains.plugins.scala.testcases.ScalaFileSetTestCase;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author ilyas
 */
@RunWith(AllTests.class)
public class DragSearchTest extends ScalaFileSetTestCase {

    private static final int MAX_ROLLBACKS = 30;

    public DragSearchTest() {
        super("parser", "stress", "data");
    }

    public String transform(String s, String[] strings) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(myProject);
        PsiElementFactory psiElementFactory = facade.getElementFactory();
        assertNotNull(psiElementFactory);

        String fileText = strings[0];
        assertNotNull(fileText);

        ScalaParserDefinition parserDefinition = new ScalaParserDefinition();
        PsiBuilder psiBuilder = PsiBuilderFactory.getInstance()
                .createBuilder(parserDefinition, parserDefinition.createLexer(myProject), fileText);
        DragBuilderWrapper dragBuilder = new DragBuilderWrapper(myProject, psiBuilder);
        parserDefinition.createParser(myProject).parse(parserDefinition.getFileNodeType(), dragBuilder);

        Pair<TextRange, Integer>[] dragInfo = dragBuilder.getDragInfo();
        exploreForDrags(dragInfo);

        PsiFile psiFile = PsiFileFactory.getInstance(myProject)
                .createFileFromText("temp.scala", ScalaFileType.INSTANCE, fileText);
        return DebugUtil.psiToString(psiFile, false);
    }

    private static void exploreForDrags(Pair<TextRange, Integer>[] dragInfo) {
        int ourMaximum = max(dragInfo);
        List<Pair<TextRange, Integer>> penals = ContainerUtil.findAll(dragInfo, new Condition<Pair<TextRange, Integer>>() {
            public boolean value(Pair<TextRange, Integer> pair) {
                return pair.getSecond() >= MAX_ROLLBACKS;
            }
        });

        if (penals.size() > 0) {
            Assert.assertTrue("Too much rollbacks: " + ourMaximum, ourMaximum < MAX_ROLLBACKS);
        }

    }

    private static int max(Pair<TextRange, Integer>[] dragInfo) {
        int max = 0;
        for (Pair<TextRange, Integer> pair : dragInfo) {
            if (pair.getSecond() > max) {
                max = pair.getSecond();
            }
        }
        return max;
    }

    public static Test suite() {
        return new DragSearchTest();
    }
}
