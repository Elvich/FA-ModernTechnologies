-- Runs once when an empty MySQL volume is initialized.
CREATE DATABASE IF NOT EXISTS fleet CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS cinema CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS warehouse CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON fleet.* TO 'fa'@'%';
GRANT ALL PRIVILEGES ON cinema.* TO 'fa'@'%';
GRANT ALL PRIVILEGES ON warehouse.* TO 'fa'@'%';
