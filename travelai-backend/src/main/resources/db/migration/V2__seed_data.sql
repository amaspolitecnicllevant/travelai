-- Admin: Admin1234!
INSERT INTO users (username, email, password, name, role, age_verified)
VALUES ('admin','admin@travelai.local',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RnUqRCBOu',
        'Admin TravelAI','ADMIN', TRUE);

-- Demo: Demo1234!
INSERT INTO users (username, email, password, name, bio, age_verified)
VALUES ('demo','demo@travelai.local',
        '$2a$12$9RW/LD5QYSvpbS6lgWpHyOHvn8eRpBqDGkajUdBLOTVOiQjxFKKMm',
        'Usuari Demo','Viatger apassionat', TRUE);

-- Documents legals inicials (plantilles — cal substituir pel text real)
INSERT INTO legal_documents (type, version, content) VALUES
('PRIVACY_POLICY', '1.0', 'PLANTILLA: Cal redactar la Política de Privacitat real amb un advocat.'),
('TERMS',          '1.0', 'PLANTILLA: Cal redactar els Termes dÚs reals amb un advocat.'),
('COOKIES',        '1.0', 'PLANTILLA: Cal redactar la Política de Cookies real amb un advocat.');
