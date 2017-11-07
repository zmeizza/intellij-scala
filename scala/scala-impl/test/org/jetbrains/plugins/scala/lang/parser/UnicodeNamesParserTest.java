package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class UnicodeNamesParserTest extends AbstractParserTest {

    public UnicodeNamesParserTest() {
        super("parser", "data", "unicodeNames");
    }

    public static Test suite() {
        return new UnicodeNamesParserTest();
    }
}
