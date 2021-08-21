package se.magnus.microservices.core.nationalteam.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.core.nationalteam.NationalTeam;
import se.magnus.api.core.nationalteam.NationalTeamService;
import se.magnus.microservices.core.nationalteam.persistence.NationalTeamEntity;
import se.magnus.microservices.core.nationalteam.persistence.NationalTeamRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import static reactor.core.publisher.Mono.error;

@SuppressWarnings("ALL")
@RestController
class NationalTeamServiceImpl implements NationalTeamService {
    private static final Logger LOG = LoggerFactory.getLogger(NationalTeamServiceImpl.class);
    private final NationalTeamRepository repository;
    private final NationalTeamMapper mapper;
    private final ServiceUtil serviceUtil;

    @Autowired
    public NationalTeamServiceImpl(NationalTeamRepository repository, NationalTeamMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public NationalTeam createNationalTeam(NationalTeam body) {
        if (body.getNationalTeamId() < 1)
            throw new InvalidInputException("Invalid nationalteamId: " + body.getNationalTeamId());

        NationalTeamEntity entity = mapper.apiToEntity(body);

        Mono<NationalTeam> newEntity = repository.save(entity)
                .log()
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, National Team Id: " + body.getNationalTeamId()))
                .map(e -> mapper.entityToApi(e));

        return newEntity.block();
    }

    @Override
    public Mono<NationalTeam> getNationalTeam(int nationalteamId) {
        if (nationalteamId < 1) throw new InvalidInputException("Invalid nationalteamId: " + nationalteamId);

        return repository.findByNationalteamId(nationalteamId)
                .switchIfEmpty(error(new NotFoundException("No national team found for nationalteamId: " + nationalteamId)))
                .log()
                .map(mapper::entityToApi)
                .map(e -> {
                    e.setServiceAddress(serviceUtil.getServiceAddress());
                    return e;
                });
    }

    @Override
    public void deleteNationalTeam(int nationalteamId) {
        if (nationalteamId < 1) throw new InvalidInputException("Invalid nationalteamId: " + nationalteamId);
        LOG.debug("deleteNationalTeam: tries to delete national team with nationalteamId: {}", nationalteamId);
        repository.findByNationalteamId(nationalteamId).log().map(e -> repository.delete(e)).flatMap(e -> e).block();
    }
}