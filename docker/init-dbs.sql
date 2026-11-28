-- Cinepass Multi-Database Provisioning Script for PostgreSQL
-- Mounted in /docker-entrypoint-initdb.d/ to auto-initialize on first container start

CREATE DATABASE cinepass_user_db;
CREATE DATABASE cinepass_movie_db;
CREATE DATABASE cinepass_show_db;
CREATE DATABASE cinepass_booking_db;

-- Grant all privileges to the default user
GRANT ALL PRIVILEGES ON DATABASE cinepass_user_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE cinepass_movie_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE cinepass_show_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE cinepass_booking_db TO CURRENT_USER;
