-- Dev seed data
DO $$
DECLARE
    v_week VARCHAR(1);
    weeks VARCHAR(1)[] := ARRAY['A', 'B', 'C', 'D'];

    -- Fixed cooperator IDs for registrations
    coop_ids UUID[] := ARRAY[
        'c0000000-0000-0000-0000-000000000001'::UUID,  -- alice (logged-in user, no registration)
        'c0000000-0000-0000-0000-000000000002'::UUID,
        'c0000000-0000-0000-0000-000000000003'::UUID,
        'c0000000-0000-0000-0000-000000000004'::UUID,
        'c0000000-0000-0000-0000-000000000005'::UUID,
        'c0000000-0000-0000-0000-000000000006'::UUID,
        'c0000000-0000-0000-0000-000000000007'::UUID,
        'c0000000-0000-0000-0000-000000000008'::UUID,
        'c0000000-0000-0000-0000-000000000009'::UUID,
        'c0000000-0000-0000-0000-000000000010'::UUID
    ];

    -- Fixed slot template IDs (week A) for targeted registrations
    -- Mon 15:45
    slot_a_mon1 UUID := 'a0000000-0000-0000-0001-000000000001';
    -- Mon 18:15
    slot_a_mon2 UUID := 'a0000000-0000-0000-0001-000000000002';
    -- Wed 08:15
    slot_a_wed1 UUID := 'a0000000-0000-0000-0003-000000000001';
    -- Wed 10:45
    slot_a_wed2 UUID := 'a0000000-0000-0000-0003-000000000002';
    -- Sat 08:15
    slot_a_sat1 UUID := 'a0000000-0000-0000-0006-000000000001';
    -- Sat 10:45
    slot_a_sat2 UUID := 'a0000000-0000-0000-0006-000000000002';

    -- Fixed slot template IDs (week B) for some registrations
    slot_b_thu1 UUID := 'b0000000-0000-0000-0004-000000000001';
    slot_b_fri1 UUID := 'b0000000-0000-0000-0005-000000000001';
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

    -- =============================================
    -- WEEK A: varied fill levels for demo
    -- =============================================

    -- Monday
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (slot_a_mon1, 'A', 'MONDAY', '15:45', '18:30', 4, 5),  -- will be FULL (5/5)
        (slot_a_mon2, 'A', 'MONDAY', '18:15', '21:00', 4, 5);   -- will have 1/5 (needs people)

    -- Tuesday
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (gen_random_uuid(), 'A', 'TUESDAY', '15:45', '18:30', 4, 5),  -- empty (needs people)
        (gen_random_uuid(), 'A', 'TUESDAY', '18:15', '21:00', 4, 5);  -- empty (needs people)

    -- Wednesday
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (slot_a_wed1, 'A', 'WEDNESDAY', '08:15', '11:00', 5, 6),   -- will be FULL (6/6)
        (slot_a_wed2, 'A', 'WEDNESDAY', '10:45', '13:30', 4, 5),   -- will have 4/5 (min reached)
        (gen_random_uuid(), 'A', 'WEDNESDAY', '15:45', '18:30', 4, 5),  -- empty
        (gen_random_uuid(), 'A', 'WEDNESDAY', '18:15', '21:00', 4, 5);  -- empty

    -- Thursday
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (gen_random_uuid(), 'A', 'THURSDAY', '08:15', '11:00', 5, 6),
        (gen_random_uuid(), 'A', 'THURSDAY', '10:45', '13:30', 4, 5),
        (gen_random_uuid(), 'A', 'THURSDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'A', 'THURSDAY', '18:15', '21:00', 4, 5);

    -- Friday
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (gen_random_uuid(), 'A', 'FRIDAY', '08:15', '11:00', 5, 6),
        (gen_random_uuid(), 'A', 'FRIDAY', '10:45', '13:30', 4, 5),
        (gen_random_uuid(), 'A', 'FRIDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'A', 'FRIDAY', '18:15', '21:00', 4, 5);

    -- Saturday
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (slot_a_sat1, 'A', 'SATURDAY', '08:15', '11:00', 5, 6),    -- will have 3/6 (needs people)
        (slot_a_sat2, 'A', 'SATURDAY', '10:45', '13:30', 5, 6),    -- will have 5/6 (min reached)
        (gen_random_uuid(), 'A', 'SATURDAY', '15:45', '18:30', 5, 6),
        (gen_random_uuid(), 'A', 'SATURDAY', '18:15', '20:15', 5, 6);

    -- =============================================
    -- WEEKS B, C, D: generated with random IDs (except targeted B slots)
    -- =============================================

    -- Week B
    INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
        (gen_random_uuid(), 'B', 'MONDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'B', 'MONDAY', '18:15', '21:00', 4, 5),
        (gen_random_uuid(), 'B', 'TUESDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'B', 'TUESDAY', '18:15', '21:00', 4, 5),
        (gen_random_uuid(), 'B', 'WEDNESDAY', '08:15', '11:00', 5, 6),
        (gen_random_uuid(), 'B', 'WEDNESDAY', '10:45', '13:30', 4, 5),
        (gen_random_uuid(), 'B', 'WEDNESDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'B', 'WEDNESDAY', '18:15', '21:00', 4, 5),
        (slot_b_thu1, 'B', 'THURSDAY', '08:15', '11:00', 5, 6),     -- will have 2/6 (needs people)
        (gen_random_uuid(), 'B', 'THURSDAY', '10:45', '13:30', 4, 5),
        (gen_random_uuid(), 'B', 'THURSDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'B', 'THURSDAY', '18:15', '21:00', 4, 5),
        (slot_b_fri1, 'B', 'FRIDAY', '08:15', '11:00', 5, 6),       -- will be FULL (6/6)
        (gen_random_uuid(), 'B', 'FRIDAY', '10:45', '13:30', 4, 5),
        (gen_random_uuid(), 'B', 'FRIDAY', '15:45', '18:30', 4, 5),
        (gen_random_uuid(), 'B', 'FRIDAY', '18:15', '21:00', 4, 5),
        (gen_random_uuid(), 'B', 'SATURDAY', '08:15', '11:00', 5, 6),
        (gen_random_uuid(), 'B', 'SATURDAY', '10:45', '13:30', 5, 6),
        (gen_random_uuid(), 'B', 'SATURDAY', '15:45', '18:30', 5, 6),
        (gen_random_uuid(), 'B', 'SATURDAY', '18:15', '20:15', 5, 6);

    -- Weeks C and D: all empty (needs people everywhere)
    FOREACH v_week IN ARRAY ARRAY['C', 'D'] LOOP
        INSERT INTO slot_template (id, week, day_of_week, start_time, end_time, min_capacity, max_capacity) VALUES
            (gen_random_uuid(), v_week, 'MONDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'MONDAY', '18:15', '21:00', 4, 5),
            (gen_random_uuid(), v_week, 'TUESDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'TUESDAY', '18:15', '21:00', 4, 5),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '10:45', '13:30', 4, 5),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'WEDNESDAY', '18:15', '21:00', 4, 5),
            (gen_random_uuid(), v_week, 'THURSDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'THURSDAY', '10:45', '13:30', 4, 5),
            (gen_random_uuid(), v_week, 'THURSDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'THURSDAY', '18:15', '21:00', 4, 5),
            (gen_random_uuid(), v_week, 'FRIDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'FRIDAY', '10:45', '13:30', 4, 5),
            (gen_random_uuid(), v_week, 'FRIDAY', '15:45', '18:30', 4, 5),
            (gen_random_uuid(), v_week, 'FRIDAY', '18:15', '21:00', 4, 5),
            (gen_random_uuid(), v_week, 'SATURDAY', '08:15', '11:00', 5, 6),
            (gen_random_uuid(), v_week, 'SATURDAY', '10:45', '13:30', 5, 6),
            (gen_random_uuid(), v_week, 'SATURDAY', '15:45', '18:30', 5, 6),
            (gen_random_uuid(), v_week, 'SATURDAY', '18:15', '20:15', 5, 6);
    END LOOP;

    -- =============================================
    -- COOPERATORS: fixed IDs for registration references
    -- =============================================
    DELETE FROM cooperator WHERE email LIKE 'test%@example.com' OR email = 'alice@alice.com';
    INSERT INTO cooperator (id, email, first_name, last_name, keycloak_subject) VALUES
        (coop_ids[1],  'alice@alice.com',      'Alice',   'Coopératrice', 'alice'),
        (coop_ids[2],  'test2@example.com',    'Bob',     'Martin',       'test-subject-2'),
        (coop_ids[3],  'test3@example.com',    'Claire',  'Dupont',       'test-subject-3'),
        (coop_ids[4],  'test4@example.com',    'David',   'Lefebvre',     'test-subject-4'),
        (coop_ids[5],  'test5@example.com',    'Emma',    'Moreau',       'test-subject-5'),
        (coop_ids[6],  'test6@example.com',    'François','Garcia',       'test-subject-6'),
        (coop_ids[7],  'test7@example.com',    'Gaëlle',  'Petit',        'test-subject-7'),
        (coop_ids[8],  'test8@example.com',    'Hugo',    'Roux',         'test-subject-8'),
        (coop_ids[9],  'test9@example.com',    'Inès',    'Bernard',      'test-subject-9'),
        (coop_ids[10], 'test10@example.com',   'Jules',   'Thomas',       'test-subject-10');

    -- =============================================
    -- REGISTRATIONS: create varied fill levels
    -- =============================================

    -- Week A, Mon 15:45 → FULL (5/5)
    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_a_mon1, coop_ids[2]),
        (gen_random_uuid(), slot_a_mon1, coop_ids[3]),
        (gen_random_uuid(), slot_a_mon1, coop_ids[4]),
        (gen_random_uuid(), slot_a_mon1, coop_ids[5]),
        (gen_random_uuid(), slot_a_mon1, coop_ids[6]);

    -- Week A, Mon 18:15 → 1/5 (needs people, low fill)
    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_a_mon2, coop_ids[7]);

    -- Week A, Wed 08:15 → FULL (6/6) — need extra cooperators
    -- We reuse some coops across different slots (cooperator UNIQUE constraint is per cooperator, not per slot)
    -- Actually: cooperator_id is UNIQUE in slot_registration, so one coop = one slot only.
    -- We need more cooperators for this. Let's add extra demo cooperators.
    DELETE FROM cooperator WHERE email LIKE 'demo%@example.com';
    INSERT INTO cooperator (id, email, first_name, last_name, keycloak_subject) VALUES
        ('d0000000-0000-0000-0000-000000000001', 'demo1@example.com', 'Léa',      'Nguyen',    'demo-subject-1'),
        ('d0000000-0000-0000-0000-000000000002', 'demo2@example.com', 'Mathis',   'Fournier',  'demo-subject-2'),
        ('d0000000-0000-0000-0000-000000000003', 'demo3@example.com', 'Nina',     'Girard',    'demo-subject-3'),
        ('d0000000-0000-0000-0000-000000000004', 'demo4@example.com', 'Oscar',    'André',     'demo-subject-4'),
        ('d0000000-0000-0000-0000-000000000005', 'demo5@example.com', 'Pauline',  'Leroy',     'demo-subject-5'),
        ('d0000000-0000-0000-0000-000000000006', 'demo6@example.com', 'Quentin',  'Lambert',   'demo-subject-6'),
        ('d0000000-0000-0000-0000-000000000007', 'demo7@example.com', 'Rose',     'Bonnet',    'demo-subject-7'),
        ('d0000000-0000-0000-0000-000000000008', 'demo8@example.com', 'Samuel',   'Faure',     'demo-subject-8'),
        ('d0000000-0000-0000-0000-000000000009', 'demo9@example.com', 'Théo',     'Mercier',   'demo-subject-9'),
        ('d0000000-0000-0000-0000-000000000010', 'demo10@example.com','Victoire', 'Duval',     'demo-subject-10');

    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_a_wed1, coop_ids[8]),
        (gen_random_uuid(), slot_a_wed1, coop_ids[9]),
        (gen_random_uuid(), slot_a_wed1, coop_ids[10]),
        (gen_random_uuid(), slot_a_wed1, 'd0000000-0000-0000-0000-000000000001'),
        (gen_random_uuid(), slot_a_wed1, 'd0000000-0000-0000-0000-000000000002'),
        (gen_random_uuid(), slot_a_wed1, 'd0000000-0000-0000-0000-000000000003');

    -- Week A, Wed 10:45 → 4/5 (min reached, one spot left)
    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_a_wed2, 'd0000000-0000-0000-0000-000000000004'),
        (gen_random_uuid(), slot_a_wed2, 'd0000000-0000-0000-0000-000000000005'),
        (gen_random_uuid(), slot_a_wed2, 'd0000000-0000-0000-0000-000000000006'),
        (gen_random_uuid(), slot_a_wed2, 'd0000000-0000-0000-0000-000000000007');

    -- Week A, Sat 08:15 → 3/6 (needs people, half-filled)
    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_a_sat1, 'd0000000-0000-0000-0000-000000000008'),
        (gen_random_uuid(), slot_a_sat1, 'd0000000-0000-0000-0000-000000000009'),
        (gen_random_uuid(), slot_a_sat1, 'd0000000-0000-0000-0000-000000000010');

    -- Week A, Sat 10:45 → 5/6 (min reached, one spot left)
    -- Need a few more cooperators
    DELETE FROM cooperator WHERE email LIKE 'extra%@example.com';
    INSERT INTO cooperator (id, email, first_name, last_name, keycloak_subject) VALUES
        ('e0000000-0000-0000-0000-000000000001', 'extra1@example.com', 'Amina',  'Diallo',  'extra-subject-1'),
        ('e0000000-0000-0000-0000-000000000002', 'extra2@example.com', 'Basile', 'Perrin',  'extra-subject-2'),
        ('e0000000-0000-0000-0000-000000000003', 'extra3@example.com', 'Camille','Robert',  'extra-subject-3'),
        ('e0000000-0000-0000-0000-000000000004', 'extra4@example.com', 'Diane',  'Richard', 'extra-subject-4'),
        ('e0000000-0000-0000-0000-000000000005', 'extra5@example.com', 'Ethan',  'Simon',   'extra-subject-5'),
        ('e0000000-0000-0000-0000-000000000006', 'extra6@example.com', 'Fiona',  'Laurent', 'extra-subject-6'),
        ('e0000000-0000-0000-0000-000000000007', 'extra7@example.com', 'Gabriel','Michel',  'extra-subject-7'),
        ('e0000000-0000-0000-0000-000000000008', 'extra8@example.com', 'Hélène', 'Blanc',   'extra-subject-8');

    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_a_sat2, 'e0000000-0000-0000-0000-000000000001'),
        (gen_random_uuid(), slot_a_sat2, 'e0000000-0000-0000-0000-000000000002'),
        (gen_random_uuid(), slot_a_sat2, 'e0000000-0000-0000-0000-000000000003'),
        (gen_random_uuid(), slot_a_sat2, 'e0000000-0000-0000-0000-000000000004'),
        (gen_random_uuid(), slot_a_sat2, 'e0000000-0000-0000-0000-000000000005');

    -- Week B, Thu 08:15 → 2/6 (needs people)
    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_b_thu1, 'e0000000-0000-0000-0000-000000000006'),
        (gen_random_uuid(), slot_b_thu1, 'e0000000-0000-0000-0000-000000000007');

    -- Week B, Fri 08:15 → FULL (6/6)
    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_b_fri1, 'e0000000-0000-0000-0000-000000000008');
    -- Need 5 more for this slot
    DELETE FROM cooperator WHERE email LIKE 'fill%@example.com';
    INSERT INTO cooperator (id, email, first_name, last_name, keycloak_subject) VALUES
        ('f0000000-0000-0000-0000-000000000001', 'fill1@example.com', 'Ivan',    'Morel',   'fill-subject-1'),
        ('f0000000-0000-0000-0000-000000000002', 'fill2@example.com', 'Julia',   'Guérin',  'fill-subject-2'),
        ('f0000000-0000-0000-0000-000000000003', 'fill3@example.com', 'Karim',   'Boyer',   'fill-subject-3'),
        ('f0000000-0000-0000-0000-000000000004', 'fill4@example.com', 'Lucie',   'Garnier', 'fill-subject-4'),
        ('f0000000-0000-0000-0000-000000000005', 'fill5@example.com', 'Maxime',  'Chevalier','fill-subject-5');

    INSERT INTO slot_registration (id, slot_template_id, cooperator_id) VALUES
        (gen_random_uuid(), slot_b_fri1, 'f0000000-0000-0000-0000-000000000001'),
        (gen_random_uuid(), slot_b_fri1, 'f0000000-0000-0000-0000-000000000002'),
        (gen_random_uuid(), slot_b_fri1, 'f0000000-0000-0000-0000-000000000003'),
        (gen_random_uuid(), slot_b_fri1, 'f0000000-0000-0000-0000-000000000004'),
        (gen_random_uuid(), slot_b_fri1, 'f0000000-0000-0000-0000-000000000005');

END $$;
