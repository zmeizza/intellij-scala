package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class ScalaDocParserTest extends AbstractParserTest {

    public ScalaDocParserTest() {
        super("parser", "data", "scaladoc");
    }

    public static Test suite() {
        return new ScalaDocParserTest();
    }
}
