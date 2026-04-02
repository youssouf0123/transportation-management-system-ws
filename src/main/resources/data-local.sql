-- Local-only sample data for development with the "local" Spring profile.
-- This file is not loaded by production because only application-local.properties points to it.

INSERT INTO organization (id, name, status) VALUES
  (100, 'Local Demo Logistics', 'APPROVED'),
  (99, 'YD Groupe', 'APPROVED');

INSERT INTO app_user (id, full_name, email, role, status, password_hash, auth_token, organization_id) VALUES
  (99, 'Youssouf Diarra', 'dyoussouf12@gmail.com', 'OWNER', 'ACTIVE', '$2a$10$dzg6ik4XJVjBnW0cItr1Oeo10BZSXzvEaewFpUurbWx82l64Xo4D.', NULL, 100),
  (100, 'Local Owner', 'local.owner@example.com', 'OWNER', 'ACTIVE', '$2a$10$dzg6ik4XJVjBnW0cItr1Oeo10BZSXzvEaewFpUurbWx82l64Xo4D.', NULL, 100),
  (101, 'Fatoumata Traore', 'fatoumata@example.com', 'MANAGER', 'ACTIVE', '$2a$10$dzg6ik4XJVjBnW0cItr1Oeo10BZSXzvEaewFpUurbWx82l64Xo4D.', NULL, 100),
  (102, 'Moussa Diallo', 'moussa@example.com', 'VIEWER', 'ACTIVE', '$2a$10$dzg6ik4XJVjBnW0cItr1Oeo10BZSXzvEaewFpUurbWx82l64Xo4D.', NULL, 100);

INSERT INTO driver (id, name, license_number, status, phone_number, organization_id) VALUES
  (100, 'Amadou Keita', 'ML-DRV-1001', 'AVAILABLE', '+22370000001', 100),
  (101, 'Aissata Coulibaly', 'ML-DRV-1002', 'ON_TRIP', '+22370000002', 100),
  (102, 'Ibrahim Sangare', 'ML-DRV-1003', 'OFF_DUTY', '+22370000003', 100);

INSERT INTO vehicle (id, make, model, plate_number, status, current_mileage, organization_id, driver_id) VALUES
  (100, 'Toyota', 'Hilux', 'ML-TRK-100', 'ACTIVE', 128450, 100, 100),
  (101, 'Mercedes', 'Sprinter', 'ML-VAN-210', 'IN_SERVICE', 86420, 100, 101),
  (102, 'Renault', 'Duster', 'ML-SUV-315', 'ACTIVE', 54210, 100, 102);

INSERT INTO trip (id, origin, destination, status, scheduled_date, cargo_description, notes, organization_id, driver_id, vehicle_id) VALUES
  (100, 'Bamako', 'Sikasso', 'PLANNED', DATE '2026-04-02', 'Retail stock replenishment', 'Morning departure', 100, 100, 100),
  (101, 'Bamako', 'Segou', 'IN_PROGRESS', DATE '2026-04-01', 'Construction materials', 'Customer called on departure', 100, 101, 101),
  (102, 'Kayes', 'Bamako', 'COMPLETED', DATE '2026-03-30', 'Produce return load', 'Closed and invoiced', 100, 102, 102);

INSERT INTO finance_record (id, type, record_scope, category, description, description_en, description_fr, amount, date, organization_id, vehicle_id) VALUES
  (100, 'EARNING', 'VEHICLE', 'OPERATIONS', 'Livraison Bamako Sikasso', 'Bamako Sikasso delivery', 'Livraison Bamako Sikasso', 1850.00, DATE '2026-04-01', 100, 100),
  (101, 'EXPENSE', 'VEHICLE', 'FUEL', 'Carburant Mercedes Sprinter', 'Mercedes Sprinter fuel', 'Carburant Mercedes Sprinter', 245.50, DATE '2026-04-01', 100, 101),
  (102, 'EXPENSE', 'VEHICLE', 'MAINTENANCE', 'Pneus Toyota Hilux', 'Toyota Hilux tires', 'Pneus Toyota Hilux', 520.00, DATE '2026-03-29', 100, 100),
  (103, 'EXPENSE', 'MISC', 'OTHER', 'Fournitures de bureau', 'Office supplies', 'Fournitures de bureau', 90.00, DATE '2026-03-28', 100, NULL);

INSERT INTO maintenance_record (id, service_type, status, service_date, mileage, cost, notes, organization_id, vehicle_id) VALUES
  (100, 'Oil Change', 'SCHEDULED', DATE '2026-04-03', 128500, 95.00, 'Scheduled before next intercity trip', 100, 100),
  (101, 'Brake Inspection', 'COMPLETED', DATE '2026-03-27', 86200, 140.00, 'Pads replaced on front axle', 100, 101);

INSERT INTO document_record (
  id, title, document_type, entity_type, entity_id, status, file_name, file_content_type, file_size, file_url, storage_path, notes, expiry_date, created_at, organization_id, uploaded_by_id
) VALUES
  (100, 'Toyota Hilux Insurance', 'INSURANCE', 'VEHICLE', 100, 'ACTIVE', 'toyota-insurance.pdf', 'application/pdf', 120400, '/documents/100/file', 'local-seed/toyota-insurance.pdf', 'Demo metadata only', DATE '2026-12-31', TIMESTAMP '2026-03-28 09:15:00', 100, 100),
  (101, 'Amadou License Copy', 'LICENSE', 'DRIVER', 100, 'ACTIVE', 'amadou-license.jpg', 'image/jpeg', 80450, '/documents/101/file', 'local-seed/amadou-license.jpg', 'Demo metadata only', DATE '2027-05-30', TIMESTAMP '2026-03-28 09:25:00', 100, 101);

ALTER TABLE organization ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE app_user ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE driver ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE vehicle ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE trip ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE finance_record ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE maintenance_record ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE document_record ALTER COLUMN id RESTART WITH 1000;
