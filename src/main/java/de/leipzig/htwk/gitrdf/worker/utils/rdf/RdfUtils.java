package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfUtils {

    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss");

    public static Node stringLiteral(String value) {
        return NodeFactory.createLiteral(value, XSDDatatype.XSDstring);
    }

    public static Node longLiteral(long value) {
        //return NodeFactory.createLiteral(String.valueOf(value), XSDDatatype.XSDlong);
        return NodeFactory.createLiteralByValue(value, XSDDatatype.XSDlong);
    }

    public static Node dateTimeLiteral(LocalDateTime dateTime) {
        //return NodeFactory.createLiteral(dateTime.toString(), XSDDatatype.XSDdateTime);
        return NodeFactory.createLiteral(dateTime.format(dateTimeFormatter), XSDDatatype.XSDdateTime);
    }

    public static Node uri(String value) {
        return NodeFactory.createURI(value);
    }

}
