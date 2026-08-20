-- =====================================================================
--  Creation des 6 bases de la plateforme.
--  Une base par microservice : chacun est seul proprietaire de ses
--  donnees, aucun service ne lit la base d'un autre.
--
--  Ce script n'est execute qu'au TOUT PREMIER demarrage du conteneur,
--  quand le volume mysql-data est vide. Pour le rejouer :
--      docker compose down -v && docker compose up -d
-- =====================================================================

CREATE DATABASE IF NOT EXISTS auth_db          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS internship_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS evaluation_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS document_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

FLUSH PRIVILEGES;
