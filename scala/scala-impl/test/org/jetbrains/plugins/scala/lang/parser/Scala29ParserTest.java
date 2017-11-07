package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class Scala29ParserTest extends AbstractParserTest {

    public Scala29ParserTest() {
        super("parser", "data", "scala29");
    }

    public static Test suite() {
        return new Scala29ParserTest();
    }
}
