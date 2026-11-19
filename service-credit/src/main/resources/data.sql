-- Dossiers dans différents états du cycle de vie
INSERT INTO loan_application (client_id, montant_demande, montant_accorde, score_etude, duree_mois, taux_propose, motif, statut) VALUES (1, 50000.00, NULL, NULL, 36, NULL, 'Achat véhicule', 'DEPOSE');
INSERT INTO loan_application (client_id, montant_demande, montant_accorde, score_etude, duree_mois, taux_propose, motif, statut) VALUES (2, 40000.00, NULL, 650, 24, NULL, 'Travaux maison', 'EN_ETUDE');
INSERT INTO loan_application (client_id, montant_demande, montant_accorde, score_etude, duree_mois, taux_propose, motif, statut) VALUES (1, 30000.00, 30000.00, 720, 12, 6.50, 'Équipement maison', 'DEBLOQUE');
INSERT INTO loan_application (client_id, montant_demande, montant_accorde, score_etude, duree_mois, taux_propose, motif, statut) VALUES (3, 120000.00, NULL, 480, 60, NULL, 'Achat terrain', 'REFUSE');
INSERT INTO loan_application (client_id, montant_demande, montant_accorde, score_etude, duree_mois, taux_propose, motif, statut) VALUES (2, 60000.00, 55000.00, 650, 48, 7.00, 'Études enfants', 'ACCORDE');

-- Échéancier du dossier 3 (DEBLOQUE) : 30 000 MAD sur 12 mois à 6,50 %, amortissement constant
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 1, '2026-11-05', 2500.00, 162.50, 'PAYEE');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 2, '2026-12-05', 2500.00, 148.96, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 3, '2027-01-05', 2500.00, 135.42, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 4, '2027-02-05', 2500.00, 121.88, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 5, '2027-03-05', 2500.00, 108.33, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 6, '2027-04-05', 2500.00, 94.79, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 7, '2027-05-05', 2500.00, 81.25, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 8, '2027-06-05', 2500.00, 67.71, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 9, '2027-07-05', 2500.00, 54.17, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 10, '2027-08-05', 2500.00, 40.62, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 11, '2027-09-05', 2500.00, 27.08, 'A_PAYER');
INSERT INTO repayment_schedule (loan_application_id, numero_echeance, date_echeance, montant_capital, montant_interets, statut) VALUES (3, 12, '2027-10-05', 2500.00, 13.54, 'A_PAYER');
