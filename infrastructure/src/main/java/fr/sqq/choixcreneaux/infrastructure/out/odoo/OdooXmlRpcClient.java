package fr.sqq.choixcreneaux.infrastructure.out.odoo;

import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import fr.sqq.choixcreneaux.domain.model.Week;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

@ApplicationScoped
public class OdooXmlRpcClient implements OdooSyncPort {

    private static final Logger LOG = Logger.getLogger(OdooXmlRpcClient.class);

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
                    "worker_nb_min", "worker_nb_max"});
            kwargs.put("domain", new Object[]{new Object[]{"active", "=", true}});
            Object[] result = (Object[]) execute(uid, "shift.template", "search_read",
                    new Object[]{new Object[]{}}, kwargs);

            List<SlotTemplate> templates = new ArrayList<>();
            for (Object item : result) {
                @SuppressWarnings("unchecked")
                Map<String, Object> record = (Map<String, Object>) item;
                try {
                    templates.add(mapSlotTemplate(record));
                } catch (Exception e) {
                    LOG.warnf("Failed to map slot template %s: %s", record.get("id"), e.getMessage());
                }
            }
            return templates;
        } catch (Exception e) {
            LOG.errorf("Failed to pull slot templates from Odoo: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    private SlotTemplate mapSlotTemplate(Map<String, Object> record) {
        long odooId = ((Number) record.get("id")).longValue();
        String weekName = (String) record.get("week_name");
        String weekList = (String) record.get("week_list");
        String startDatetime = (String) record.get("start_datetime");
        String endDatetime = (String) record.get("end_datetime");
        int minCapacity = record.get("worker_nb_min") instanceof Number n ? n.intValue() : 1;
        int maxCapacity = record.get("worker_nb_max") instanceof Number n ? n.intValue() : 10;

        Week week = mapWeek(weekName);
        DayOfWeek dayOfWeek = mapDayOfWeek(weekList);
        LocalTime startTime = extractTime(startDatetime);
        LocalTime endTime = extractTime(endDatetime);

        return new SlotTemplate(UUID.randomUUID(), week, dayOfWeek, startTime, endTime, minCapacity, maxCapacity, odooId);
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
        return switch (weekList.trim().toLowerCase()) {
            case "0", "monday", "lundi" -> DayOfWeek.MONDAY;
            case "1", "tuesday", "mardi" -> DayOfWeek.TUESDAY;
            case "2", "wednesday", "mercredi" -> DayOfWeek.WEDNESDAY;
            case "3", "thursday", "jeudi" -> DayOfWeek.THURSDAY;
            case "4", "friday", "vendredi" -> DayOfWeek.FRIDAY;
            case "5", "saturday", "samedi" -> DayOfWeek.SATURDAY;
            case "6", "sunday", "dimanche" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.MONDAY;
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
            Map<String, Object> kwargs = new HashMap<>();
            kwargs.put("fields", new Object[]{"id", "email", "name", "working_state"});
            kwargs.put("domain", new Object[]{new Object[]{"working_state", "!=", "blocked"}});
            Object[] result = (Object[]) execute(uid, "res.partner", "search_read",
                    new Object[]{new Object[]{}}, kwargs);

            List<Cooperator> cooperators = new ArrayList<>();
            for (Object item : result) {
                @SuppressWarnings("unchecked")
                Map<String, Object> record = (Map<String, Object>) item;
                try {
                    cooperators.add(mapCooperator(record));
                } catch (Exception e) {
                    LOG.warnf("Failed to map cooperator %s: %s", record.get("id"), e.getMessage());
                }
            }
            return cooperators;
        } catch (Exception e) {
            LOG.errorf("Failed to pull cooperators from Odoo: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Cooperator mapCooperator(Map<String, Object> record) {
        long odooId = ((Number) record.get("id")).longValue();
        String email = record.get("email") instanceof String s ? s : "";
        String fullName = record.get("name") instanceof String s ? s : "";
        String[] nameParts = fullName.trim().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return new Cooperator(UUID.randomUUID(), email, firstName, lastName, odooId, null);
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
