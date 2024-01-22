package de.leipzig.htwk.gitrdf.worker.database.entity;

import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gitrepositoryorder")
@Data
@NoArgsConstructor
public class GitRepositoryOrderEntity {

    public static GitRepositoryOrderEntity newOrder(String fileName) {

        GitRepositoryOrderEntity gitRepositoryOrderEntity = new GitRepositoryOrderEntity();

        gitRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.RECEIVED);
        gitRepositoryOrderEntity.setNumberOfTries(0);
        gitRepositoryOrderEntity.setFileName(fileName);

        return gitRepositoryOrderEntity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    private GitRepositoryOrderStatus status;

    @Column(nullable = false)
    private int numberOfTries;


}
