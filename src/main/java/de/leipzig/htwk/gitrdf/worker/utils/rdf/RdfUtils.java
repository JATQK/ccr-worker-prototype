package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfUtils {

    public static Node literal(String value) {
        //NodeFactory.createLiteral("fda", XSDDatatype.)
        return NodeFactory.createLiteral(value);
    }

    public static Node stringLiteral(String value) {
        return NodeFactory.createLiteral(value, XSDDatatype.XSDstring);
    }

    public static Node dateTimeLiteral(LocalDateTime dateTime) {
        return NodeFactory.createLiteral(dateTime.toString(), XSDDatatype.XSDdateTime);
    }

    public static Node uri(String value) {
        return NodeFactory.createURI(value);
    }

}
