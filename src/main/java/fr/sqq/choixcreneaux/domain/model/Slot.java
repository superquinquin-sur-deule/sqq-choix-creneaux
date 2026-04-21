package fr.sqq.choixcreneaux.domain.model;

import fr.sqq.choixcreneaux.domain.exception.AlreadyRegisteredException;
import fr.sqq.choixcreneaux.domain.exception.CampaignNotOpenException;
import fr.sqq.choixcreneaux.domain.exception.SlotFullException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Slot {

    private final UUID id;
    private final Week week;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int minCapacity;
    private final int maxCapacity;
    private final Long odooTemplateId;
    private final int version;
    private final Set<SlotRegistration> registrations;
    private SlotStatus status;

    private Slot(UUID id, Week week, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                 int minCapacity, int maxCapacity, Long odooTemplateId, int version,
                 Set<SlotRegistration> registrations, SlotStatus status) {
        this.id = id;
        this.week = week;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;
        this.odooTemplateId = odooTemplateId;
        this.version = version;
        this.registrations = registrations;
        this.status = status;
    }

    public static Slot create(UUID id, Week week, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                              int minCapacity, int maxCapacity, Long odooTemplateId) {
        var slot = new Slot(id, week, dayOfWeek, startTime, endTime, minCapacity, maxCapacity, odooTemplateId,
                0, new LinkedHashSet<>(), SlotStatus.NEEDS_PEOPLE);
        slot.refreshStatus();
        return slot;
    }

    public static Slot rehydrate(UUID id, Week week, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                 int minCapacity, int maxCapacity, Long odooTemplateId, int version,
                                 Collection<SlotRegistration> registrations, SlotStatus status) {
        return new Slot(id, week, dayOfWeek, startTime, endTime, minCapacity, maxCapacity, odooTemplateId,
                version, new LinkedHashSet<>(registrations), status);
    }

    public void register(Cooperator cooperator, Campaign campaign) {
        if (!campaign.isOpen()) throw new CampaignNotOpenException();
        if (hasCooperator(cooperator.id())) throw new AlreadyRegisteredException();
        if (isFull()) throw new SlotFullException();
        registrations.add(new SlotRegistration(UUID.randomUUID(), id, cooperator.id(), Instant.now()));
        refreshStatus();
    }

    public void adminAssign(Cooperator cooperator) {
        if (hasCooperator(cooperator.id())) return;
        if (isFull()) throw new SlotFullException();
        registrations.add(new SlotRegistration(UUID.randomUUID(), id, cooperator.id(), Instant.now()));
        refreshStatus();
    }

    public boolean unregister(UUID cooperatorId) {
        boolean removed = registrations.removeIf(r -> r.cooperatorId().equals(cooperatorId));
        if (removed) refreshStatus();
        return removed;
    }

    public boolean hasCooperator(UUID cooperatorId) {
        return registrations.stream().anyMatch(r -> r.cooperatorId().equals(cooperatorId));
    }

    public boolean isFull() {
        return registrations.size() >= maxCapacity;
    }

    public boolean isUnderMinimum() {
        return registrations.size() < minCapacity;
    }

    public int registrationCount() {
        return registrations.size();
    }

    private void refreshStatus() {
        int count = registrations.size();
        if (count >= maxCapacity) status = SlotStatus.FULL;
        else if (count < minCapacity) status = SlotStatus.NEEDS_PEOPLE;
        else status = SlotStatus.OPEN;
    }

    public UUID id() { return id; }
    public Week week() { return week; }
    public DayOfWeek dayOfWeek() { return dayOfWeek; }
    public LocalTime startTime() { return startTime; }
    public LocalTime endTime() { return endTime; }
    public int minCapacity() { return minCapacity; }
    public int maxCapacity() { return maxCapacity; }
    public Long odooTemplateId() { return odooTemplateId; }
    public int version() { return version; }
    public SlotStatus status() { return status; }

    public Set<SlotRegistration> registrations() {
        return Collections.unmodifiableSet(registrations);
    }

    public SlotTemplate asTemplateView() {
        return new SlotTemplate(id, week, dayOfWeek, startTime, endTime, minCapacity, maxCapacity, odooTemplateId);
    }
}
