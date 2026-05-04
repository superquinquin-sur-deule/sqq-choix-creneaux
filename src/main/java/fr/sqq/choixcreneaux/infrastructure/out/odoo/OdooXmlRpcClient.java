package fr.sqq.choixcreneaux.infrastructure.out.odoo;

import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
import fr.sqq.choixcreneaux.application.port.out.PushOutcome;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import fr.sqq.choixcreneaux.domain.model.Week;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class OdooXmlRpcClient implements OdooSyncPort {

    @ConfigProperty(name = "odoo.url", defaultValue = "http://localhost:8069")
    String odooUrl;

    @ConfigProperty(name = "odoo.db", defaultValue = "odoo")
    String odooDb;

    @ConfigProperty(name = "odoo.username", defaultValue = "admin")
    String odooUsername;

    @ConfigProperty(name = "odoo.password", defaultValue = "admin")
    String odooPassword;

    private Integer authenticate() throws Exception {
        XmlRpcClient client = new XmlRpcClient();
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(odooUrl + "/xmlrpc/2/common"));
        client.setConfig(config);
        Object result = client.execute("authenticate", new Object[]{
                odooDb, odooUsername, odooPassword, Collections.emptyMap()
        });
        if (result instanceof Integer uid) {
            return uid;
        }
        throw new RuntimeException("Odoo authentication failed");
    }

    private Object execute(int uid, String model, String method, Object[] args, Map<String, Object> kwargs) throws Exception {
        XmlRpcClient client = new XmlRpcClient();
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(odooUrl + "/xmlrpc/2/object"));
        client.setConfig(config);
        return client.execute("execute_kw", new Object[]{
                odooDb, uid, odooPassword, model, method, args, kwargs
        });
    }

    @Override
    public List<SlotTemplate> pullSlotTemplates() {
        try {
            int uid = authenticate();
            Map<String, Object> kwargs = new HashMap<>();
            kwargs.put("fields", new Object[]{"id", "week_name", "week_list", "start_datetime", "end_datetime",
                    "worker_nb_min", "worker_nb_max", "seats_min", "seats_max"});
            Object[] domain = new Object[]{new Object[]{"active", "=", true}};
            Object[] result = (Object[]) execute(uid, "shift.template", "search_read",
                    new Object[]{domain}, kwargs);

            List<SlotTemplate> templates = new ArrayList<>();
            for (Object item : result) {
                @SuppressWarnings("unchecked")
                Map<String, Object> record = (Map<String, Object>) item;
                try {
                    templates.add(mapSlotTemplate(record));
                } catch (Exception e) {
                    Log.warnf("Failed to map slot template %s: %s", record.get("id"), e.getMessage());
                }
            }
            return templates;
        } catch (Exception e) {
            Log.errorf("Failed to pull slot templates from Odoo: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    private SlotTemplate mapSlotTemplate(Map<String, Object> record) {
        long odooId = ((Number) record.get("id")).longValue();
        String weekName = (String) record.get("week_name");
        String weekList = (String) record.get("week_list");
        String startDatetime = (String) record.get("start_datetime");
        String endDatetime = (String) record.get("end_datetime");
        int minCapacity = firstPositiveInt(record, "worker_nb_min", "seats_min", 1);
        int maxCapacity = firstPositiveInt(record, "worker_nb_max", "seats_max", 10);

        Week week = mapWeek(weekName);
        DayOfWeek dayOfWeek = mapDayOfWeek(weekList);
        LocalTime startTime = extractTime(startDatetime);
        LocalTime endTime = extractTime(endDatetime);

        return new SlotTemplate(UUID.randomUUID(), week, dayOfWeek, startTime, endTime, minCapacity, maxCapacity, odooId);
    }

    private int firstPositiveInt(Map<String, Object> record, String primaryKey, String fallbackKey, int defaultValue) {
        if (record.get(primaryKey) instanceof Number n && n.intValue() > 0) return n.intValue();
        if (record.get(fallbackKey) instanceof Number n && n.intValue() > 0) return n.intValue();
        return defaultValue;
    }

    private Week mapWeek(String weekName) {
        if (weekName == null) return Week.A;
        return switch (weekName.trim().toUpperCase()) {
            case "A", "SEMAINE A" -> Week.A;
            case "B", "SEMAINE B" -> Week.B;
            case "C", "SEMAINE C" -> Week.C;
            case "D", "SEMAINE D" -> Week.D;
            default -> Week.A;
        };
    }

    private DayOfWeek mapDayOfWeek(String weekList) {
        if (weekList == null) return DayOfWeek.MONDAY;
        return switch (weekList.trim().toUpperCase()) {
            case "MO" -> DayOfWeek.MONDAY;
            case "TU" -> DayOfWeek.TUESDAY;
            case "WE" -> DayOfWeek.WEDNESDAY;
            case "TH" -> DayOfWeek.THURSDAY;
            case "FR" -> DayOfWeek.FRIDAY;
            case "SA" -> DayOfWeek.SATURDAY;
            case "SU" -> DayOfWeek.SUNDAY;
            default -> {
                Log.warnf("Unknown week_list value from Odoo: '%s' — defaulting to MONDAY", weekList);
                yield DayOfWeek.MONDAY;
            }
        };
    }

    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter ODOO_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalTime extractTime(String datetime) {
        if (datetime == null || datetime.isBlank()) return LocalTime.of(8, 0);
        // Odoo Datetime fields are stored and returned as naive UTC strings
        // ("YYYY-MM-DD HH:MM:SS"). Convert to Europe/Paris before extracting
        // the wall-clock time so the app shows local hours (handles DST).
        LocalDateTime utc = LocalDateTime.parse(datetime, ODOO_DT_FMT);
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(PARIS_ZONE).toLocalTime();
    }

    @Override
    public List<Cooperator> pullCooperators() {
        try {
            int uid = authenticate();

            // Binômes are duplicated as child "contact" entries (type='contact', parent_id != false)
            // on the main coop's partner record, with the binôme's name. We collect those names
            // to exclude the binômes' own top-level partner records below.
            Set<String> binomeNames = pullBinomeNames(uid);

            Map<String, Object> kwargs = new HashMap<>();
            kwargs.put("fields", new Object[]{"id", "email", "name", "barcode_base"});
            Object[] domain = new Object[]{
                    new Object[]{"is_member", "=", true},
                    new Object[]{"user_ids", "!=", false}
            };
            Object[] result = (Object[]) execute(uid, "res.partner", "search_read",
                    new Object[]{domain}, kwargs);

            List<Cooperator> cooperators = new ArrayList<>();
            for (Object item : result) {
                @SuppressWarnings("unchecked")
                Map<String, Object> record = (Map<String, Object>) item;
                String name = record.get("name") instanceof String s ? s.trim() : "";
                if (!name.isBlank() && binomeNames.contains(normalize(name))) {
                    continue;
                }
                try {
                    cooperators.add(mapCooperator(record));
                } catch (Exception e) {
                    Log.warnf("Failed to map cooperator %s: %s", record.get("id"), e.getMessage());
                }
            }
            return cooperators;
        } catch (Exception e) {
            Log.errorf("Failed to pull cooperators from Odoo: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String normalize(String s) {
        String stripped = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private Set<String> pullBinomeNames(int uid) throws Exception {
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("fields", new Object[]{"name"});
        Object[] domain = new Object[]{
                new Object[]{"parent_id", "!=", false},
                new Object[]{"type", "=", "contact"}
        };
        Object[] result = (Object[]) execute(uid, "res.partner", "search_read",
                new Object[]{domain}, kwargs);
        Set<String> names = new HashSet<>();
        for (Object item : result) {
            @SuppressWarnings("unchecked")
            Map<String, Object> record = (Map<String, Object>) item;
            if (record.get("name") instanceof String s && !s.isBlank()) {
                names.add(normalize(s));
            }
        }
        return names;
    }

    private Cooperator mapCooperator(Map<String, Object> record) {
        long odooId = ((Number) record.get("id")).longValue();
        String email = record.get("email") instanceof String s ? s : "";
        String fullName = record.get("name") instanceof String s ? s : "";
        if (!(record.get("barcode_base") instanceof Number barcodeNumber) || barcodeNumber.longValue() == 0L) {
            throw new IllegalStateException("Cooperator " + odooId + " has no barcode_base");
        }
        String barcodeBase = String.valueOf(barcodeNumber.longValue());
        String[] nameParts = fullName.trim().split(",", 2);
        String lastName = nameParts[0].trim();
        String firstName = nameParts.length > 1 ? nameParts[1].trim() : "";

        return new Cooperator(UUID.randomUUID(), email, firstName, lastName, odooId, barcodeBase);
    }

    @Override
    public PushOutcome pushRegistration(long odooPartnerId, long odooTemplateId) {
        try {
            int uid = authenticate();

            List<Map<String, Object>> existing = findRegistrationsForPartner(uid, odooPartnerId);
            List<Integer> toRemove = new ArrayList<>();
            boolean alreadyOnTarget = false;
            for (Map<String, Object> reg : existing) {
                long templateId = extractRelationId(reg.get("shift_template_id"));
                int regId = ((Number) reg.get("id")).intValue();
                if (templateId == odooTemplateId) {
                    alreadyOnTarget = true;
                } else {
                    toRemove.add(regId);
                }
            }

            if (alreadyOnTarget && toRemove.isEmpty()) {
                return PushOutcome.UNCHANGED;
            }

            if (!toRemove.isEmpty()) {
                execute(uid, "shift.template.registration", "unlink",
                        new Object[]{toRemove.toArray()}, Collections.emptyMap());
            }

            if (alreadyOnTarget) {
                return PushOutcome.MOVED;
            }

            int ticketId = findShiftTicketId(uid, odooTemplateId);
            Map<String, Object> values = new HashMap<>();
            values.put("partner_id", (int) odooPartnerId);
            values.put("shift_template_id", (int) odooTemplateId);
            values.put("shift_ticket_id", ticketId);
            execute(uid, "shift.template.registration", "create",
                    new Object[]{values}, Collections.emptyMap());
            return toRemove.isEmpty() ? PushOutcome.CREATED : PushOutcome.MOVED;
        } catch (Exception e) {
            throw new RuntimeException("Failed to push registration to Odoo: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> findRegistrationsForPartner(int uid, long odooPartnerId) throws Exception {
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("fields", new Object[]{"id", "shift_template_id"});
        Object[] domain = new Object[]{
                new Object[]{"partner_id", "=", (int) odooPartnerId}
        };
        Object[] result = (Object[]) execute(uid, "shift.template.registration", "search_read",
                new Object[]{domain}, kwargs);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : result) {
            @SuppressWarnings("unchecked")
            Map<String, Object> record = (Map<String, Object>) item;
            out.add(record);
        }
        return out;
    }

    private long extractRelationId(Object many2one) {
        // Odoo returns many2one as [id, "display name"] or false
        if (many2one instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private int findShiftTicketId(int uid, long odooTemplateId) throws Exception {
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("fields", new Object[]{"id"});
        kwargs.put("limit", 1);
        Object[] domain = new Object[]{
                new Object[]{"shift_template_id", "=", (int) odooTemplateId}
        };
        Object[] result = (Object[]) execute(uid, "shift.template.ticket", "search_read",
                new Object[]{domain}, kwargs);
        if (result.length == 0) {
            throw new RuntimeException("No shift.template.ticket found for shift template " + odooTemplateId);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> record = (Map<String, Object>) result[0];
        return ((Number) record.get("id")).intValue();
    }
}
