package de.leipzig.htwk.gitrdf.worker.database.repository;

import de.leipzig.htwk.gitrdf.worker.database.entity.GithubRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GithubRepositoryOrderRepository extends JpaRepository<GithubRepositoryOrderEntity, Long> {

    List<GithubRepositoryOrderEntity> findAllByStatus(GitRepositoryOrderStatus status);

}
