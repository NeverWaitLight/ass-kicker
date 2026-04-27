package com.github.waitlight.asskicker.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.GlobalVariableEntity;
import com.github.waitlight.asskicker.repository.GlobalVariableRepository;

import reactor.test.StepVerifier;

@SpringBootTest(classes = {
        AssKickerTestApplication.class,
        MongoTestConfiguration.class
}, properties = {
        "spring.main.web-application-type=none",
        "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class GlobalVariableServiceTest {

    @Autowired
    private GlobalVariableService globalVariableService;

    @Autowired
    private GlobalVariableRepository globalVariableRepository;

    @BeforeEach
    void clearVariables() {
        StepVerifier.create(globalVariableRepository.deleteAll()).verifyComplete();
    }

    @Test
    void create_findById_and_findByKey_returnsFullPayload() {
        GlobalVariableEntity input = variable("brandName", "品牌名", "Ass Kicker", true);

        StepVerifier.create(globalVariableService.create(input)
                .flatMap(saved -> globalVariableService.findById(saved.getId())
                        .zipWith(globalVariableService.findByKey(saved.getKey()))))
                .assertNext(tuple -> {
                    GlobalVariableEntity byId = tuple.getT1();
                    GlobalVariableEntity byKey = tuple.getT2();
                    assertThat(byId.getId()).isNotBlank();
                    assertThat(byId.getCreatedAt()).isNotNull();
                    assertThat(byId.getUpdatedAt()).isNotNull();
                    assertThat(byId.getKey()).isEqualTo("brandName");
                    assertThat(byId.getName()).isEqualTo("品牌名");
                    assertThat(byId.getValue()).isEqualTo("Ass Kicker");
                    assertThat(byId.getEnabled()).isTrue();
                    assertThat(byKey.getId()).isEqualTo(byId.getId());
                })
                .verifyComplete();
    }

    @Test
    void create_sameKey_conflicts() {
        GlobalVariableEntity first = variable("brandName", "品牌名", "Ass Kicker", true);
        GlobalVariableEntity second = variable("brandName", "品牌名", "Other", true);

        StepVerifier.create(globalVariableService.create(first)
                .then(globalVariableService.create(second)))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ConflictException.class))
                .verify();
    }

    @Test
    void update_changesFields_andInvalidatesEnabledVariablesCache() {
        GlobalVariableEntity input = variable("brandName", "品牌名", "Ass Kicker", true);

        StepVerifier.create(globalVariableService.create(input)
                .flatMap(saved -> globalVariableService.findEnabledVariablesMap()
                        .then(MonoPatch.updateValue(globalVariableService, saved.getId(), "Updated Brand"))
                        .then(globalVariableService.findEnabledVariablesMap())))
                .assertNext(values -> assertThat(values).containsEntry("brandName", "Updated Brand"))
                .verifyComplete();
    }

    @Test
    void findEnabledVariablesMap_onlyIncludesEnabledVariables() {
        GlobalVariableEntity enabled = variable("brandName", "品牌名", "Ass Kicker", true);
        GlobalVariableEntity disabled = variable("smsSignature", "短信签名", "签名", false);

        StepVerifier.create(globalVariableService.create(enabled)
                .then(globalVariableService.create(disabled))
                .then(globalVariableService.findEnabledVariablesMap()))
                .assertNext(values -> {
                    assertThat(values).containsEntry("brandName", "Ass Kicker");
                    assertThat(values).doesNotContainKey("smsSignature");
                })
                .verifyComplete();
    }

    @Test
    void list_keywordSearch_matchesKeyOrName() {
        GlobalVariableEntity brand = variable("brandName", "品牌名", "Ass Kicker", true);
        GlobalVariableEntity team = variable("teamName", "团队名", "Ops", true);

        StepVerifier.create(globalVariableService.create(brand)
                .then(globalVariableService.create(team))
                .then(globalVariableService.count("团队")
                        .zipWith(globalVariableService.list("团队", 10, 0).collectList())))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo(1);
                    assertThat(tuple.getT2()).hasSize(1);
                    assertThat(tuple.getT2().get(0).getKey()).isEqualTo("teamName");
                })
                .verifyComplete();
    }

    @Test
    void delete_removesDocument() {
        GlobalVariableEntity input = variable("brandName", "品牌名", "Ass Kicker", true);

        StepVerifier.create(globalVariableService.create(input)
                .flatMap(saved -> globalVariableService.delete(saved.getId())
                        .then(globalVariableService.findById(saved.getId()))))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(NotFoundException.class))
                .verify();
    }

    private static GlobalVariableEntity variable(String key, String name, String value, boolean enabled) {
        GlobalVariableEntity entity = new GlobalVariableEntity();
        entity.setKey(key);
        entity.setName(name);
        entity.setValue(value);
        entity.setDescription("desc");
        entity.setEnabled(enabled);
        return entity;
    }

    private static final class MonoPatch {
        private static reactor.core.publisher.Mono<GlobalVariableEntity> updateValue(
                GlobalVariableService service, String id, String value) {
            GlobalVariableEntity patch = new GlobalVariableEntity();
            patch.setValue(value);
            return service.update(id, patch);
        }
    }
}
