package forcamente.api.repository;

import forcamente.api.entity.RegistroAuditoriaEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IRegistroAuditoriaRepository extends JpaRepository<RegistroAuditoriaEntity, UUID> {

    Page<RegistroAuditoriaEntity> findByOcorridoEmBetweenOrderByOcorridoEmDesc(
            LocalDateTime inicio, LocalDateTime fim, Pageable paginacao);

    Page<RegistroAuditoriaEntity> findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(
            LocalDateTime inicio, LocalDateTime fim, AcaoAuditoriaEnum acao, Pageable paginacao);

    List<RegistroAuditoriaEntity> findByOcorridoEmBetweenOrderByOcorridoEmDesc(
            LocalDateTime inicio, LocalDateTime fim);

    List<RegistroAuditoriaEntity> findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(
            LocalDateTime inicio, LocalDateTime fim, AcaoAuditoriaEnum acao);
}
