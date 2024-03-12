package de.leipzig.htwk.gitrdf.worker.utils.rdf.gitdatatypes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.xsd.impl.*;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.xerces.impl.dv.*;
import org.apache.xerces.impl.validation.ValidationState;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.GIT_NAMESPACE;


public class RdfGitDataType extends BaseDatatype {
    public static final String GIT = GIT_NAMESPACE;
    public static final BaseDatatype DiffEntryChangeType = new RdfGitDiffEntryChangeType();

    private final String typeName;

    public RdfGitDataType(String typeName) {
        super("");
        this.typeName = typeName;
        this.uri = GIT + "#" + this.typeName;
    }
}

