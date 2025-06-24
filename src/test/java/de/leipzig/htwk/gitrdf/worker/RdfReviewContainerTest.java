package de.leipzig.htwk.gitrdf.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.Test;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils;

public class RdfReviewContainerTest {

    @Test
    void reviewsAreContainedInBag() {
        Graph graph = Factory.createDefaultGraph();

        String issueUri = "https://example.com/pull/1";
        String reviewsUri = issueUri + "/reviews";
        String review1 = reviewsUri + "/10";
        String review2 = reviewsUri + "/20";

        graph.add(RdfGithubIssueUtils.createIssueReviewsProperty(issueUri, reviewsUri));
        graph.add(RdfGithubIssueUtils.createReviewContainerTypeProperty(reviewsUri));
        graph.add(Triple.create(RdfUtils.uri(reviewsUri), RdfGithubIssueUtils.rdfTypeProperty(), RdfUtils.uri("rdf:Bag")));
        graph.add(Triple.create(RdfUtils.uri(reviewsUri), RdfUtils.uri("rdf:_1"), RdfUtils.uri(review1)));
        graph.add(Triple.create(RdfUtils.uri(reviewsUri), RdfUtils.uri("rdf:_2"), RdfUtils.uri(review2)));

        assertEquals(1, graph.find(RdfUtils.uri(issueUri), RdfGithubIssueUtils.reviewsProperty(), Node.ANY).toList().size());
        assertTrue(graph.contains(RdfUtils.uri(reviewsUri), RdfUtils.uri("rdf:_1"), RdfUtils.uri(review1)));
        assertTrue(graph.contains(RdfUtils.uri(reviewsUri), RdfUtils.uri("rdf:_2"), RdfUtils.uri(review2)));
    }
}

