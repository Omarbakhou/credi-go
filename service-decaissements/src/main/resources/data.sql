INSERT INTO treasury_account (libelle, solde_disponible) VALUES ('Enveloppe crédit conso Casablanca', 500000.00);
INSERT INTO treasury_account (libelle, solde_disponible) VALUES ('Enveloppe crédit conso Rabat', 250000.00);
INSERT INTO treasury_account (libelle, solde_disponible) VALUES ('Enveloppe test (épuisée)', 1000.00);

-- Versement déjà effectué pour le dossier 3 (DEBLOQUE) du service-credit
INSERT INTO disbursement (loan_application_id, treasury_account_id, montant, date_versement, statut) VALUES (3, 1, 30000.00, '2026-10-05 10:30:00', 'VERSE');
