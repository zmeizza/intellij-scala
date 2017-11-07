package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class ParametersParserTest extends AbstractParserTest {

    public ParametersParserTest() {
        super("parser", "data", "parameters");
    }

    public static Test suite() {
        return new ParametersParserTest();
    }
}
