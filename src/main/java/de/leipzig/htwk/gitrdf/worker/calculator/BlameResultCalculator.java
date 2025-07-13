package de.leipzig.htwk.gitrdf.worker.calculator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jgit.blame.BlameGenerator;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.io.EolCanonicalizingInputStream;

/**
 * The implementation logic of this class closely resembles the logic of
 * {@link org.eclipse.jgit.api.BlameCommand}, targeting the call() method.
 *
 * The optimization in this class is, that the directory cache over the git
 * repository is only build once, and not for every 'get blame result' call.
 */
public class BlameResultCalculator {

    private final DirCache dirCache;

    private final Repository repository;

    public BlameResultCalculator(Repository repo) throws IOException {
        this.dirCache = repo.readDirCache();
        this.repository = repo;
    }

    public BlameResult getBlameResult(String path) throws IOException {
        return getBlameResult(path, RawTextComparator.WS_IGNORE_ALL);
    }

    /**
     * See the call method of {@link org.eclipse.jgit.api.BlameCommand}.
     */
    public BlameResult getBlameResult(String path, RawTextComparator rawTextComparator) throws IOException {

        BlameGenerator gen = new BlameGenerator(repository, path);

        try {

            gen.setTextComparator(rawTextComparator);

            gen.push(null, repository.resolve(Constants.HEAD));

            if (!repository.isBare()) {

                int entry = dirCache.findEntry(path);

                if (0 <= entry) {
                    gen.push(null, dirCache.getEntry(entry).getObjectId());
                }

                File inTree = new File(repository.getWorkTree(), path);

                if (repository.getFS().isFile(inTree)) {
                    RawText rawText = getRawText(inTree);
                    gen.push(null, rawText);
                }

            }

            return gen.computeBlameResult();

        } finally {

            gen.release();

        }

    }

    private RawText getRawText(File inTree) throws IOException,
            FileNotFoundException {
        RawText rawText;

        WorkingTreeOptions workingTreeOptions = repository.getConfig()
                .get(WorkingTreeOptions.KEY);
        CoreConfig.AutoCRLF autoCRLF = workingTreeOptions.getAutoCRLF();
        switch (autoCRLF) {
            case FALSE:
            case INPUT:
                // Git used the repo format on checkout, but other tools
                // may change the format to CRLF. We ignore that here.
                rawText = new RawText(inTree);
                break;
            case TRUE:
                EolCanonicalizingInputStream in = new EolCanonicalizingInputStream(
                        new FileInputStream(inTree), true);
                // Canonicalization should lead to same or shorter length
                // (CRLF to LF), so the file size on disk is an upper size bound
                rawText = new RawText(toByteArray(in, (int) inTree.length()));
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown autocrlf option " + autoCRLF); //$NON-NLS-1$
        }
        return rawText;
    }

    private static byte[] toByteArray(InputStream source, int upperSizeLimit)
            throws IOException {
        byte[] buffer = new byte[upperSizeLimit];
        try {
            int read = IO.readFully(source, buffer, 0);
            if (read == upperSizeLimit)
                return buffer;
            else {
                byte[] copy = new byte[read];
                System.arraycopy(buffer, 0, copy, 0, read);
                return copy;
            }
        } finally {
            source.close();
        }
    }

}