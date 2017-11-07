package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class PatternsParserTest extends AbstractParserTest {

    public PatternsParserTest() {
        super("parser", "data", "patterns");
    }

    public static Test suite() {
        return new PatternsParserTest();
    }
}
