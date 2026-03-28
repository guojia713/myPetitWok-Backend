-- Promote the first registered user (lowest id) to ROLE_ADMIN
UPDATE users SET role = 'ROLE_ADMIN' WHERE id = (SELECT MIN(id) FROM users);
