package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.gitdatatypes.RdfGitDataType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.eclipse.jgit.diff.DiffEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfUtils {

    private static final PrefixMap prefixMap = PrefixMapFactory.create(new HashMap<>(Map.of(// instead of Map.of
            GIT_NAMESPACE, GIT_URI,
            PLATFORM_NAMESPACE,PLATFORM_URI,
            PLATFORM_GITHUB_NAMESPACE,PLATFORM_GITHUB_URI,
            XSD_SCHEMA_NAMESPACE,XSD_SCHEMA_URI,
            RDF_SCHEMA_NAMESPACE,RDF_SCHEMA_URI
    )));

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

    public static Node changeTypeLiteral(DiffEntry.ChangeType changeType) {
        return NodeFactory.createLiteral(changeType.toString(), RdfGitDataType.DiffEntryChangeType);
    }

    public static Node nonNegativeIntegerLiteral(long value) {
        return NodeFactory.createLiteralByValue(value, XSDDatatype.XSDnonNegativeInteger);
    }

    public static Node uri(String value) {
        var expandedValue = prefixMap.expand(value);

        if( expandedValue != null )
            value = expandedValue;

        return NodeFactory.createURI(value);
    }

}
