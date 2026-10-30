-- Cinepass PostgreSQL Database Initialization Script
-- Run this script as the 'postgres' superuser (e.g. via psql or pgAdmin query tool)

-- 1. Create User Service Database
SELECT 'CREATE DATABASE cinepass_user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cinepass_user_db')\gexec

-- 2. Create Movie Service Database
SELECT 'CREATE DATABASE cinepass_movie_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cinepass_movie_db')\gexec

-- 3. Create Show Service Database
SELECT 'CREATE DATABASE cinepass_show_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cinepass_show_db')\gexec

-- 4. Create Booking Service Database
SELECT 'CREATE DATABASE cinepass_booking_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cinepass_booking_db')\gexec
