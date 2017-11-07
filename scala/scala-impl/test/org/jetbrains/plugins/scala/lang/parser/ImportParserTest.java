package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class ImportParserTest extends AbstractParserTest {

    public ImportParserTest() {
        super("parser", "data", "import");
    }

    public static Test suite() {
        return new ImportParserTest();
    }
}
