package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class ExpressionsParserTest extends AbstractParserTest {

    public ExpressionsParserTest() {
        super("parser", "data", "expressions");
    }

    public static Test suite() {
        return new ExpressionsParserTest();
    }
}
