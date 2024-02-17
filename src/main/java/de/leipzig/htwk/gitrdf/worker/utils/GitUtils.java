package de.leipzig.htwk.gitrdf.worker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitUtils {

    public static File getDotGitFileFromParentDirectoryFileAndThrowExceptionIfNoOrMoreThanOneExists(File parentDirectoryFile) {

        File[] files = parentDirectoryFile.listFiles((dir, name) -> name.equals(".git"));

        if (files.length < 1) {
            throw new RuntimeException("Expected git repository parent directory file doesn't contain '.git' directory");
        }

        if (files.length > 1) {
            throw new RuntimeException("Expected git repository parent directory file contains more than a single '.git' directory");
        }

        return files[0];
    }

}
