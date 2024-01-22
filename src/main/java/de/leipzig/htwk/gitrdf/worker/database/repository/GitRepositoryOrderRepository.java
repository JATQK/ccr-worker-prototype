package de.leipzig.htwk.gitrdf.worker.database.repository;

import de.leipzig.htwk.gitrdf.worker.database.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GitRepositoryOrderRepository extends JpaRepository<GitRepositoryOrderEntity, Long> {

    List<GitRepositoryOrderEntity> findAllByStatus(GitRepositoryOrderStatus status);

}
