package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatalogoSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CatalogoSnapshotRepository extends MongoRepository<CatalogoSnapshot, String> {

    Optional<CatalogoSnapshot> findFirstByOrderByFechaSincronizacionDesc();

    long countByFechaSincronizacionBetween(Instant desde, Instant hasta);
}