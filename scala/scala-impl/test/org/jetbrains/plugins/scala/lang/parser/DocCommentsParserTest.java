package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class DocCommentsParserTest extends AbstractParserTest {

    public DocCommentsParserTest() {
        super("parser", "data", "doccomments");
    }

    public static Test suite() {
        return new DocCommentsParserTest();
    }
}
