package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.Test;

public class RdfCommitUtilsTest {

    @Test
    void parseCommitMessageCreatesIssueTriple() {
        String message = "Fixes #123";
        Set<String> numbers = RdfCommitUtils.extractIssueNumbers(message);
        assertTrue(numbers.contains("123"));

        Triple t = RdfCommitUtils.createCommitIssueProperty("http://example.com/c1", "https://github.com/user/repo/issues/123");
        assertEquals(NodeFactory.createURI("http://example.com/c1"), t.getSubject());
        assertEquals(RdfCommitUtils.commitIssueProperty(), t.getPredicate());
        assertEquals(NodeFactory.createURI("https://github.com/user/repo/issues/123"), t.getObject());
    }
}
