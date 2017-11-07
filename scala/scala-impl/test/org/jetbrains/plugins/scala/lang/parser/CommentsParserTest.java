package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class CommentsParserTest extends AbstractParserTest {

    public CommentsParserTest() {
        super("parser", "data", "comments");
    }

    public static Test suite() {
        return new CommentsParserTest();
    }
}
