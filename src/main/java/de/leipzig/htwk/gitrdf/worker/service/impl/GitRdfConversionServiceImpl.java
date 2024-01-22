package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.service.GitRdfConversionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitRdfConversionServiceImpl implements GitRdfConversionService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void performGitRepoToRdfConversion(long id) {

        // TODO (ccr)

    }
}
