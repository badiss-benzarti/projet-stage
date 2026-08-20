-- =====================================================================
--  Remise a zero des donnees de demonstration.
--
--  Vide les donnees METIER uniquement. Les comptes (auth_db.users) et
--  les profils (user_db) sont conserves : l'interface n'a pas encore
--  d'ecran d'inscription ni de creation de profil, les supprimer
--  rendrait la plateforme inutilisable.
--
--      docker exec -i gs-mysql mysql -uroot -proot < docker/reset-demo.sql
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE internship_db.status_history;
TRUNCATE TABLE internship_db.document_requests;
TRUNCATE TABLE internship_db.internships;

TRUNCATE TABLE evaluation_db.claim_messages;
TRUNCATE TABLE evaluation_db.claims;
TRUNCATE TABLE evaluation_db.tasks;
TRUNCATE TABLE evaluation_db.evaluations;

TRUNCATE TABLE document_db.documents;
TRUNCATE TABLE notification_db.notifications;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Donnees metier videes. Comptes et profils conserves.' AS resultat;
