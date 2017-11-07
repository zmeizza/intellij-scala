package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class StatementsParserTest extends AbstractParserTest {

    public StatementsParserTest() {
        super("parser", "data", "statements");
    }

    public static Test suite() {
        return new StatementsParserTest();
    }
}
