package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class MembersParserTest extends AbstractParserTest {

    public MembersParserTest() {
        super("parser", "data", "members");
    }

    public static Test suite() {
        return new MembersParserTest();
    }
}
