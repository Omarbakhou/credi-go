INSERT INTO client (nom, email, revenus_mensuels, score_interne, statut) VALUES ('Youssef Alaoui', 'youssef.alaoui@mail.ma', 12000.00, 720, 'ACTIF');
INSERT INTO client (nom, email, revenus_mensuels, score_interne, statut) VALUES ('Salma Bennani', 'salma.bennani@mail.ma', 8500.00, 650, 'ACTIF');
INSERT INTO client (nom, email, revenus_mensuels, score_interne, statut) VALUES ('Karim Tazi', 'karim.tazi@mail.ma', 4200.00, 480, 'ACTIF');
INSERT INTO client (nom, email, revenus_mensuels, score_interne, statut) VALUES ('Nadia Chraibi', 'nadia.chraibi@mail.ma', 15000.00, 810, 'INACTIF');

INSERT INTO collateral (client_id, type, valeur, description) VALUES (1, 'VEHICULE', 95000.00, 'Dacia Duster 2022');
INSERT INTO collateral (client_id, type, valeur, description) VALUES (1, 'EPARGNE', 30000.00, 'Compte épargne nanti');
INSERT INTO collateral (client_id, type, valeur, description) VALUES (2, 'CAUTION', 50000.00, 'Caution personnelle d''un parent');
