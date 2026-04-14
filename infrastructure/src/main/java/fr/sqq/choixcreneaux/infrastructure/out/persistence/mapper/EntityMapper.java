package fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper;

import fr.sqq.choixcreneaux.domain.model.*;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface EntityMapper {
    @Mapping(target = "status", expression = "java(fr.sqq.choixcreneaux.domain.model.CampaignStatus.valueOf(entity.status))")
    Campaign toDomain(CampaignEntity entity);

    @Mapping(target = "week", expression = "java(fr.sqq.choixcreneaux.domain.model.Week.valueOf(entity.week))")
    @Mapping(target = "dayOfWeek", expression = "java(java.time.DayOfWeek.valueOf(entity.dayOfWeek))")
    SlotTemplate toDomain(SlotTemplateEntity entity);

    Cooperator toDomain(CooperatorEntity entity);
    SlotRegistration toDomain(SlotRegistrationEntity entity);
}
