package de.leipzig.htwk.gitrdf.worker.database.entity.lob;

import de.leipzig.htwk.gitrdf.worker.database.entity.GitRepositoryOrderEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Blob;
import java.sql.Clob;

@Entity
@Data
@NoArgsConstructor
public class GitRepositoryOrderEntityLobs {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    private GitRepositoryOrderEntity orderEntity;

    @Lob
    //@Column(columnDefinition = "BLOB")
    private Blob gitZipFile;

    @Lob
    //@Column(columnDefinition = "CLOB")
    private Clob rdfFile;
}
