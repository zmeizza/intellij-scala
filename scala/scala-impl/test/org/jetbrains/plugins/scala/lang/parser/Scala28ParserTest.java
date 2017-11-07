package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class Scala28ParserTest extends AbstractParserTest {

    public Scala28ParserTest() {
        super("parser", "data", "scala28");
    }

    public static Test suite() {
        return new Scala28ParserTest();
    }
}
