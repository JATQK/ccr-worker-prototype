package de.leipzig.htwk.gitrdf.worker.utils.rdf.core;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.GIT_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.GIT_URI;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_URI;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_URI;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.RDF_SCHEMA_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.RDF_SCHEMA_URI;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.XSD_SCHEMA_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.XSD_SCHEMA_URI;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.OWL_SCHEMA_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.OWL_SCHEMA_URI;
// REMOVED: SPDX imports no longer needed in v2.1

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.eclipse.jgit.diff.DiffEntry;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.git.gitdatatypes.RdfGitDataType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfUtils {

    private static final PrefixMap prefixMap = PrefixMapFactory.create(new HashMap<>(Map.of(// instead of Map.of
            GIT_NAMESPACE, GIT_URI,
            PLATFORM_NAMESPACE, PLATFORM_URI,
            PLATFORM_GITHUB_NAMESPACE, PLATFORM_GITHUB_URI,
            XSD_SCHEMA_NAMESPACE, XSD_SCHEMA_URI,
            RDF_SCHEMA_NAMESPACE, RDF_SCHEMA_URI,
            OWL_SCHEMA_NAMESPACE, OWL_SCHEMA_URI
            // REMOVED: SPDX_NAMESPACE, SPDX_URI - no longer needed in v2.1
    )));

    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static Node stringLiteral(String value) {
        return NodeFactory.createLiteral(value, XSDDatatype.XSDstring);
    }

    public static Node longLiteral(long value) {
        // Use createLiteralByValue to avoid unnecessary quotes in serialization
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

    public static Node booleanLiteral(boolean value) {
        return NodeFactory.createLiteralByValue(value, XSDDatatype.XSDboolean);
    }

    public static Node integerLiteral(int value) {
        return NodeFactory.createLiteralByValue(value, XSDDatatype.XSDinteger);
    }

    public static Node uri(String value) {
        var expandedValue = prefixMap.expand(value);

        if( expandedValue != null )
            value = expandedValue;

        return NodeFactory.createURI(value);
    }

    // REMOVED: createSpdxCheckSumNode - no longer needed in v2.1

    /**
     * Creates a properly typed xsd:anyURI literal.
     */
    public static Node anyUriLiteral(String uriValue) {
        return NodeFactory.createLiteral(uriValue, XSDDatatype.XSDanyURI);
    }

    /**
     * Creates a properly typed xsd:duration literal from milliseconds.
     */
    public static Node durationLiteral(long durationMillis) {
        // Convert milliseconds to ISO 8601 duration format (PT{seconds}S)
        double seconds = durationMillis / 1000.0;
        String durationString = String.format("PT%.3fS", seconds);
        return NodeFactory.createLiteral(durationString, XSDDatatype.XSDduration);
    }

}
