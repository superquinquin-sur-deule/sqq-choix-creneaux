package fr.sqq.choixcreneaux.infrastructure.out.odoo;

import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
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
import java.time.LocalTime;
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

    private LocalTime extractTime(String datetime) {
        if (datetime == null || datetime.isBlank()) return LocalTime.of(8, 0);
        // Odoo datetimes are "YYYY-MM-DD HH:MM:SS"
        String[] parts = datetime.split(" ");
        if (parts.length < 2) return LocalTime.of(8, 0);
        String timePart = parts[1];
        String[] timeParts = timePart.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
        return LocalTime.of(hour, minute);
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
                if (matchesBinome(name, binomeNames)) {
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

    // Tolerates light typos (case, accents, small edits) between the binôme child contact
    // name and the binôme partner's own name. Threshold scales with the shorter string.
    private static boolean matchesBinome(String name, Set<String> binomeNames) {
        if (name == null || name.isBlank()) return false;
        String n = normalize(name);
        for (String b : binomeNames) {
            String bn = normalize(b);
            int threshold = Math.min(3, Math.max(1, Math.min(n.length(), bn.length()) / 5));
            if (levenshtein(n, bn, threshold) <= threshold) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        String stripped = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static int levenshtein(String a, String b, int threshold) {
        int la = a.length();
        int lb = b.length();
        if (Math.abs(la - lb) > threshold) return threshold + 1;
        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            int rowMin = curr[0];
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                if (curr[j] < rowMin) rowMin = curr[j];
            }
            if (rowMin > threshold) return threshold + 1;
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[lb];
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
            if (record.get("name") instanceof String s) {
                names.add(s.trim());
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
        String[] nameParts = fullName.trim().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return new Cooperator(UUID.randomUUID(), email, firstName, lastName, odooId, barcodeBase);
    }

    @Override
    public void pushRegistration(long odooPartnerId, long odooTemplateId) {
        try {
            int uid = authenticate();
            Map<String, Object> values = new HashMap<>();
            values.put("partner_id", (int) odooPartnerId);
            values.put("shift_template_id", (int) odooTemplateId);
            execute(uid, "shift.template.registration", "create",
                    new Object[]{values}, Collections.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to push registration to Odoo: " + e.getMessage(), e);
        }
    }
}
