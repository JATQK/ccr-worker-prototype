package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfTurtleTidier {

    public static void tidyFile(File file) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = new FileInputStream(file)) {
            RDFDataMgr.read(model, in, Lang.TURTLE);
        }
        try (OutputStream out = new FileOutputStream(file)) {
            RDFDataMgr.write(out, model, RDFFormat.TURTLE_BLOCKS);
        }
    }
}
