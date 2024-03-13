package de.leipzig.htwk.gitrdf.worker.utils.rdf.gitdatatypes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.shared.JenaException;
import org.eclipse.jgit.diff.DiffEntry;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.GIT_NAMESPACE;

public class RdfGitDiffEntryChangeType extends BaseDatatype {
    private static final String URI = GIT_NAMESPACE + ":changeType";

    RdfGitDiffEntryChangeType() {
        super(URI);
    }

    @Override
    public Object parse(String lexicalForm) throws JenaException {
        return DiffEntry.ChangeType.valueOf(lexicalForm);
    }

    @Override
    public String unparse(Object value) {
        return ((DiffEntry.ChangeType) value).name();
    }
}
