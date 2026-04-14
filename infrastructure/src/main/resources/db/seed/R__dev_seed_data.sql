-- Dev seed data
DO $$
DECLARE
    v_week VARCHAR(1);
    weeks VARCHAR(1)[] := ARRAY['A', 'B', 'C', 'D'];
BEGIN
    -- Insert campaign (upsert)
    INSERT INTO campaign (id, status, start_date, end_date, store_opening, week_a_reference)
    VALUES (
        '00000000-0000-0000-0000-000000000001',
        'OPEN',
        NOW(),
        NULL,
        '2026-05-18',
        '2015-12-28'
    )
    ON CONFLICT (id) DO UPDATE
        SET status = 'OPEN',
            store_opening = '2026-05-18',
            week_a_reference = '2015-12-28';

    -- Remove existing slot registrations and templates
    DELETE FROM slot_registration;
    DELETE FROM slot_template;

    -- Insert slot templates for each week
    FOREACH v_week IN ARRAY weeks LOOP
        -- Monday: 15:45-18:30 (min 4, max 5), 18:15-21:00 (min 4, max 5)
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity)
        VALUES
            (gen_random_uuid(), v_week, 'MONDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'MONDAY', '18:15', '21:00', 4, 5);

        -- Tuesday: same as Monday
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity)
        VALUES
            (gen_random_uuid(), v_week, 'TUESDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'TUESDAY', '18:15', '21:00', 4, 5);

        -- Wednesday: 4 slots
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity)
        VALUES
            (gen_random_uuid(), v_week, 'WEDNESDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '10:45', '13:30', 4, 5),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '18:15', '21:00', 4, 5);

        -- Thursday: same as Wednesday
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity)
        VALUES
            (gen_random_uuid(), v_week, 'THURSDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'THURSDAY', '10:45', '13:30', 4, 5),
            (gen_random_uuid(), v_week, 'THURSDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'THURSDAY', '18:15', '21:00', 4, 5);

        -- Friday: same as Wednesday
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity)
        VALUES
            (gen_random_uuid(), v_week, 'FRIDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'FRIDAY', '10:45', '13:30', 4, 5),
            (gen_random_uuid(), v_week, 'FRIDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'FRIDAY', '18:15', '21:00', 4, 5);

        -- Saturday: 4 slots (all min 5, max 6; last ends at 20:15)
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity)
        VALUES
            (gen_random_uuid(), v_week, 'SATURDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'SATURDAY', '10:45', '13:30', 5, 6),
            (gen_random_uuid(), v_week, 'SATURDAY', '15:45', '18:30', 5, 6),
            (gen_random_uuid(), v_week, 'SATURDAY', '18:15', '20:15', 5, 6);
    END LOOP;

    -- Delete and re-insert 10 test cooperators
    -- First cooperator uses Keycloak dev services default user (subject = 'alice')
    DELETE FROM cooperator WHERE email LIKE 'test%@example.com' OR email = 'alice@alice.com';
    INSERT INTO cooperator (id, email, first_name, last_name, keycloak_subject)
    VALUES
        (gen_random_uuid(), 'alice@alice.com', 'Alice', 'Coopératrice', 'alice'),
        (gen_random_uuid(), 'test2@example.com', 'Test', 'User2', 'test-subject-2'),
        (gen_random_uuid(), 'test3@example.com', 'Test', 'User3', 'test-subject-3'),
        (gen_random_uuid(), 'test4@example.com', 'Test', 'User4', 'test-subject-4'),
        (gen_random_uuid(), 'test5@example.com', 'Test', 'User5', 'test-subject-5'),
        (gen_random_uuid(), 'test6@example.com', 'Test', 'User6', 'test-subject-6'),
        (gen_random_uuid(), 'test7@example.com', 'Test', 'User7', 'test-subject-7'),
        (gen_random_uuid(), 'test8@example.com', 'Test', 'User8', 'test-subject-8'),
        (gen_random_uuid(), 'test9@example.com', 'Test', 'User9', 'test-subject-9'),
        (gen_random_uuid(), 'test10@example.com', 'Test', 'User10', 'test-subject-10');
END $$;
