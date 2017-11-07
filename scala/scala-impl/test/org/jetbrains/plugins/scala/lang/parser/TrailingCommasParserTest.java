package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class TrailingCommasParserTest extends AbstractParserTest {

    public TrailingCommasParserTest() {
        super("parser", "data", "trailingCommas");
    }

    public static Test suite() {
        return new TrailingCommasParserTest();
    }
}
