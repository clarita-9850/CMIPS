-- ============================================================
-- CMIPS Comprehensive Test Data Seed Script
-- Generates data for all entities for end-to-end testing
-- ============================================================

-- ============================================================
-- 1. PROVIDERS (50 providers)
-- ============================================================
INSERT INTO providers (
    provider_number, first_name, last_name, middle_name, date_of_birth, gender,
    ssn, ssn_verification_status, primary_phone, email,
    street_address, city, state, zip_code, doj_county_code, doj_county_name,
    eligible, effective_date, provider_status,
    sick_leave_eligible_date, sick_leave_accrued_hours, created_at, updated_at
) VALUES
-- Active providers
('PRV-001', 'MARIA', 'GONZALEZ', 'E', '1985-03-15', 'Female', '555-01-0001', 'VERIFIED', '(916) 555-0101', 'maria.gonzalez@email.com', '123 Oak Street', 'Sacramento', 'CA', '95814', '34', 'Sacramento', 'YES', '2023-01-15', 'ACTIVE', '2023-01-15', 24.0, NOW(), NOW()),
('PRV-002', 'JOHN', 'SMITH', 'R', '1978-07-22', 'Male', '555-01-0002', 'VERIFIED', '(916) 555-0102', 'john.smith@email.com', '456 Elm Avenue', 'Sacramento', 'CA', '95815', '34', 'Sacramento', 'YES', '2022-06-01', 'ACTIVE', '2022-06-01', 16.0, NOW(), NOW()),
('PRV-003', 'SARAH', 'JOHNSON', 'L', '1990-11-08', 'Female', '555-01-0003', 'VERIFIED', '(213) 555-0103', 'sarah.j@email.com', '789 Pine Road', 'Los Angeles', 'CA', '90001', '19', 'Los Angeles', 'YES', '2023-03-20', 'ACTIVE', '2023-03-20', 40.0, NOW(), NOW()),
('PRV-004', 'MICHAEL', 'WILLIAMS', 'A', '1982-04-30', 'Male', '555-01-0004', 'VERIFIED', '(415) 555-0104', 'mwilliams@email.com', '321 Cedar Lane', 'San Francisco', 'CA', '94102', '38', 'San Francisco', 'YES', '2022-09-10', 'ACTIVE', '2022-09-10', 32.0, NOW(), NOW()),
('PRV-005', 'JENNIFER', 'BROWN', 'M', '1988-08-17', 'Female', '555-01-0005', 'VERIFIED', '(619) 555-0105', 'jbrown@email.com', '654 Maple Drive', 'San Diego', 'CA', '92101', '37', 'San Diego', 'YES', '2023-02-28', 'ACTIVE', '2023-02-28', 8.0, NOW(), NOW()),
('PRV-006', 'DAVID', 'MARTINEZ', 'J', '1975-12-03', 'Male', '555-01-0006', 'VERIFIED', '(510) 555-0106', 'dmartinez@email.com', '987 Birch Court', 'Oakland', 'CA', '94601', '01', 'Alameda', 'YES', '2021-11-15', 'ACTIVE', '2021-11-15', 48.0, NOW(), NOW()),
('PRV-007', 'LISA', 'GARCIA', 'A', '1992-06-25', 'Female', '555-01-0007', 'VERIFIED', '(408) 555-0107', 'lgarcia@email.com', '147 Willow Way', 'San Jose', 'CA', '95110', '43', 'Santa Clara', 'YES', '2023-05-01', 'ACTIVE', '2023-05-01', 20.0, NOW(), NOW()),
('PRV-008', 'ROBERT', 'DAVIS', 'W', '1980-01-10', 'Male', '555-01-0008', 'VERIFIED', '(714) 555-0108', 'rdavis@email.com', '258 Ash Boulevard', 'Anaheim', 'CA', '92801', '30', 'Orange', 'YES', '2022-04-22', 'ACTIVE', '2022-04-22', 36.0, NOW(), NOW()),
('PRV-009', 'AMANDA', 'RODRIGUEZ', 'C', '1987-09-14', 'Female', '555-01-0009', 'VERIFIED', '(951) 555-0109', 'arodriguez@email.com', '369 Spruce Street', 'Riverside', 'CA', '92501', '33', 'Riverside', 'YES', '2023-01-08', 'ACTIVE', '2023-01-08', 12.0, NOW(), NOW()),
('PRV-010', 'JAMES', 'WILSON', 'T', '1983-05-20', 'Male', '555-01-0010', 'VERIFIED', '(916) 555-0110', 'jwilson@email.com', '741 Redwood Place', 'Sacramento', 'CA', '95816', '34', 'Sacramento', 'YES', '2022-08-05', 'ACTIVE', '2022-08-05', 28.0, NOW(), NOW()),
-- More active providers
('PRV-011', 'PATRICIA', 'ANDERSON', 'S', '1979-02-28', 'Female', '555-01-0011', 'VERIFIED', '(213) 555-0111', 'panderson@email.com', '852 Sequoia Ave', 'Los Angeles', 'CA', '90002', '19', 'Los Angeles', 'YES', '2022-12-01', 'ACTIVE', '2022-12-01', 44.0, NOW(), NOW()),
('PRV-012', 'THOMAS', 'TAYLOR', 'H', '1991-10-05', 'Male', '555-01-0012', 'VERIFIED', '(415) 555-0112', 'ttaylor@email.com', '963 Palm Street', 'San Francisco', 'CA', '94103', '38', 'San Francisco', 'YES', '2023-04-15', 'ACTIVE', '2023-04-15', 16.0, NOW(), NOW()),
('PRV-013', 'ELIZABETH', 'THOMAS', 'R', '1986-07-18', 'Female', '555-01-0013', 'VERIFIED', '(619) 555-0113', 'ethomas@email.com', '159 Cypress Lane', 'San Diego', 'CA', '92102', '37', 'San Diego', 'YES', '2022-10-20', 'ACTIVE', '2022-10-20', 32.0, NOW(), NOW()),
('PRV-014', 'CHRISTOPHER', 'JACKSON', 'D', '1984-03-22', 'Male', '555-01-0014', 'VERIFIED', '(510) 555-0114', 'cjackson@email.com', '753 Magnolia Rd', 'Oakland', 'CA', '94602', '01', 'Alameda', 'YES', '2023-02-01', 'ACTIVE', '2023-02-01', 20.0, NOW(), NOW()),
('PRV-015', 'JESSICA', 'WHITE', 'L', '1993-11-30', 'Female', '555-01-0015', 'VERIFIED', '(408) 555-0115', 'jwhite@email.com', '486 Olive Court', 'San Jose', 'CA', '95111', '43', 'Santa Clara', 'YES', '2022-07-10', 'ACTIVE', '2022-07-10', 24.0, NOW(), NOW()),
-- On Leave providers
('PRV-016', 'DANIEL', 'HARRIS', 'K', '1977-08-12', 'Male', '555-01-0016', 'VERIFIED', '(916) 555-0116', 'dharris@email.com', '321 Acacia Way', 'Sacramento', 'CA', '95817', '34', 'Sacramento', 'YES', '2021-05-15', 'ON_LEAVE', '2021-05-15', 40.0, NOW(), NOW()),
('PRV-017', 'MICHELLE', 'CLARK', 'N', '1989-04-08', 'Female', '555-01-0017', 'VERIFIED', '(213) 555-0117', 'mclark@email.com', '654 Sycamore St', 'Los Angeles', 'CA', '90003', '19', 'Los Angeles', 'YES', '2022-03-01', 'ON_LEAVE', '2022-03-01', 16.0, NOW(), NOW()),
('PRV-018', 'MATTHEW', 'LEWIS', 'P', '1981-12-25', 'Male', '555-01-0018', 'VERIFIED', '(415) 555-0118', 'mlewis@email.com', '987 Eucalyptus Dr', 'San Francisco', 'CA', '94104', '38', 'San Francisco', 'YES', '2021-09-20', 'ON_LEAVE', '2021-09-20', 8.0, NOW(), NOW()),
-- Pending providers
('PRV-019', 'ASHLEY', 'ROBINSON', 'V', '1994-06-15', 'Female', '555-01-0019', 'VERIFIED', '(619) 555-0119', 'arobinson@email.com', '147 Jacaranda Ln', 'San Diego', 'CA', '92103', '37', 'San Diego', 'PENDING', '2024-01-05', 'ACTIVE', NULL, 0.0, NOW(), NOW()),
('PRV-020', 'JOSHUA', 'WALKER', 'B', '1990-09-28', 'Male', '555-01-0020', 'NOT_YET_VERIFIED', '(510) 555-0120', 'jwalker@email.com', '258 Hibiscus Rd', 'Oakland', 'CA', '94603', '01', 'Alameda', 'PENDING', '2024-01-10', 'ACTIVE', NULL, 0.0, NOW(), NOW()),
-- Terminated providers
('PRV-021', 'KIMBERLY', 'HALL', 'G', '1976-02-14', 'Female', '555-01-0021', 'VERIFIED', '(408) 555-0121', 'khall@email.com', '369 Azalea Way', 'San Jose', 'CA', '95112', '43', 'Santa Clara', 'NO', '2020-06-01', 'TERMINATED', '2019-06-01', 0.0, NOW(), NOW()),
('PRV-022', 'ANDREW', 'YOUNG', 'F', '1985-10-07', 'Male', '555-01-0022', 'VERIFIED', '(714) 555-0122', 'ayoung@email.com', '741 Bougainvillea', 'Anaheim', 'CA', '92802', '30', 'Orange', 'NO', '2021-01-15', 'TERMINATED', '2020-01-15', 0.0, NOW(), NOW()),
-- More active providers (23-50)
('PRV-023', 'STEPHANIE', 'KING', 'M', '1988-01-19', 'Female', '555-01-0023', 'VERIFIED', '(951) 555-0123', 'sking@email.com', '852 Camellia St', 'Riverside', 'CA', '92502', '33', 'Riverside', 'YES', '2023-06-01', 'ACTIVE', '2023-06-01', 28.0, NOW(), NOW()),
('PRV-024', 'KEVIN', 'WRIGHT', 'O', '1982-05-25', 'Male', '555-01-0024', 'VERIFIED', '(916) 555-0124', 'kwright@email.com', '963 Dahlia Ave', 'Sacramento', 'CA', '95818', '34', 'Sacramento', 'YES', '2022-11-10', 'ACTIVE', '2022-11-10', 36.0, NOW(), NOW()),
('PRV-025', 'NICOLE', 'LOPEZ', 'Q', '1995-08-03', 'Female', '555-01-0025', 'VERIFIED', '(213) 555-0125', 'nlopez@email.com', '159 Frangipani Rd', 'Los Angeles', 'CA', '90004', '19', 'Los Angeles', 'YES', '2023-07-15', 'ACTIVE', '2023-07-15', 12.0, NOW(), NOW()),
('PRV-026', 'BRIAN', 'HILL', 'X', '1979-11-11', 'Male', '555-01-0026', 'VERIFIED', '(415) 555-0126', 'bhill@email.com', '753 Gardenia Ln', 'San Francisco', 'CA', '94105', '38', 'San Francisco', 'YES', '2022-05-20', 'ACTIVE', '2022-05-20', 44.0, NOW(), NOW()),
('PRV-027', 'HEATHER', 'SCOTT', 'Y', '1991-03-07', 'Female', '555-01-0027', 'VERIFIED', '(619) 555-0127', 'hscott@email.com', '486 Honeysuckle Way', 'San Diego', 'CA', '92104', '37', 'San Diego', 'YES', '2023-03-01', 'ACTIVE', '2023-03-01', 20.0, NOW(), NOW()),
('PRV-028', 'TIMOTHY', 'GREEN', 'Z', '1986-06-14', 'Male', '555-01-0028', 'VERIFIED', '(510) 555-0128', 'tgreen@email.com', '321 Iris Street', 'Oakland', 'CA', '94604', '01', 'Alameda', 'YES', '2022-09-25', 'ACTIVE', '2022-09-25', 32.0, NOW(), NOW()),
('PRV-029', 'LAURA', 'ADAMS', 'A', '1983-09-20', 'Female', '555-01-0029', 'VERIFIED', '(408) 555-0129', 'ladams@email.com', '654 Jasmine Court', 'San Jose', 'CA', '95113', '43', 'Santa Clara', 'YES', '2023-04-10', 'ACTIVE', '2023-04-10', 16.0, NOW(), NOW()),
('PRV-030', 'JASON', 'BAKER', 'B', '1980-12-01', 'Male', '555-01-0030', 'VERIFIED', '(714) 555-0130', 'jbaker@email.com', '987 Lavender Blvd', 'Anaheim', 'CA', '92803', '30', 'Orange', 'YES', '2022-02-15', 'ACTIVE', '2022-02-15', 48.0, NOW(), NOW()),
('PRV-031', 'REBECCA', 'NELSON', 'C', '1992-04-16', 'Female', '555-01-0031', 'VERIFIED', '(951) 555-0131', 'rnelson@email.com', '147 Lilac Lane', 'Riverside', 'CA', '92503', '33', 'Riverside', 'YES', '2023-08-01', 'ACTIVE', '2023-08-01', 24.0, NOW(), NOW()),
('PRV-032', 'RYAN', 'CARTER', 'D', '1987-07-23', 'Male', '555-01-0032', 'VERIFIED', '(916) 555-0132', 'rcarter@email.com', '258 Magnolia Ave', 'Sacramento', 'CA', '95819', '34', 'Sacramento', 'YES', '2022-06-30', 'ACTIVE', '2022-06-30', 40.0, NOW(), NOW()),
('PRV-033', 'AMY', 'MITCHELL', 'E', '1989-10-29', 'Female', '555-01-0033', 'VERIFIED', '(213) 555-0133', 'amitchell@email.com', '369 Marigold St', 'Los Angeles', 'CA', '90005', '19', 'Los Angeles', 'YES', '2023-01-20', 'ACTIVE', '2023-01-20', 8.0, NOW(), NOW()),
('PRV-034', 'ERIC', 'PEREZ', 'F', '1984-01-06', 'Male', '555-01-0034', 'VERIFIED', '(415) 555-0134', 'eperez@email.com', '741 Narcissus Rd', 'San Francisco', 'CA', '94106', '38', 'San Francisco', 'YES', '2022-10-05', 'ACTIVE', '2022-10-05', 36.0, NOW(), NOW()),
('PRV-035', 'MELISSA', 'ROBERTS', 'G', '1996-05-12', 'Female', '555-01-0035', 'VERIFIED', '(619) 555-0135', 'mroberts@email.com', '852 Oleander Way', 'San Diego', 'CA', '92105', '37', 'San Diego', 'YES', '2023-09-01', 'ACTIVE', '2023-09-01', 20.0, NOW(), NOW()),
('PRV-036', 'MARK', 'TURNER', 'H', '1978-08-18', 'Male', '555-01-0036', 'VERIFIED', '(510) 555-0136', 'mturner@email.com', '963 Orchid Court', 'Oakland', 'CA', '94605', '01', 'Alameda', 'YES', '2022-03-15', 'ACTIVE', '2022-03-15', 32.0, NOW(), NOW()),
('PRV-037', 'ANGELA', 'PHILLIPS', 'I', '1990-11-24', 'Female', '555-01-0037', 'VERIFIED', '(408) 555-0137', 'aphillips@email.com', '159 Peony Lane', 'San Jose', 'CA', '95114', '43', 'Santa Clara', 'YES', '2023-05-20', 'ACTIVE', '2023-05-20', 16.0, NOW(), NOW()),
('PRV-038', 'STEVEN', 'CAMPBELL', 'J', '1981-02-10', 'Male', '555-01-0038', 'VERIFIED', '(714) 555-0138', 'scampbell@email.com', '753 Petunia Street', 'Anaheim', 'CA', '92804', '30', 'Orange', 'YES', '2022-07-25', 'ACTIVE', '2022-07-25', 44.0, NOW(), NOW()),
('PRV-039', 'CHRISTINE', 'PARKER', 'K', '1993-06-30', 'Female', '555-01-0039', 'VERIFIED', '(951) 555-0139', 'cparker@email.com', '486 Plumeria Rd', 'Riverside', 'CA', '92504', '33', 'Riverside', 'YES', '2023-02-10', 'ACTIVE', '2023-02-10', 28.0, NOW(), NOW()),
('PRV-040', 'JEFFREY', 'EVANS', 'L', '1985-09-05', 'Male', '555-01-0040', 'VERIFIED', '(916) 555-0140', 'jevans@email.com', '321 Poppy Avenue', 'Sacramento', 'CA', '95820', '34', 'Sacramento', 'YES', '2022-12-20', 'ACTIVE', '2022-12-20', 36.0, NOW(), NOW()),
('PRV-041', 'SANDRA', 'EDWARDS', 'M', '1988-12-15', 'Female', '555-01-0041', 'VERIFIED', '(213) 555-0141', 'sedwards@email.com', '654 Primrose Ln', 'Los Angeles', 'CA', '90006', '19', 'Los Angeles', 'YES', '2023-06-15', 'ACTIVE', '2023-06-15', 12.0, NOW(), NOW()),
('PRV-042', 'GREGORY', 'COLLINS', 'N', '1976-03-21', 'Male', '555-01-0042', 'VERIFIED', '(415) 555-0142', 'gcollins@email.com', '987 Rose Court', 'San Francisco', 'CA', '94107', '38', 'San Francisco', 'YES', '2022-01-30', 'ACTIVE', '2022-01-30', 48.0, NOW(), NOW()),
('PRV-043', 'RACHEL', 'STEWART', 'O', '1991-07-08', 'Female', '555-01-0043', 'VERIFIED', '(619) 555-0143', 'rstewart@email.com', '147 Sunflower Way', 'San Diego', 'CA', '92106', '37', 'San Diego', 'YES', '2023-03-25', 'ACTIVE', '2023-03-25', 24.0, NOW(), NOW()),
('PRV-044', 'SCOTT', 'SANCHEZ', 'P', '1983-10-14', 'Male', '555-01-0044', 'VERIFIED', '(510) 555-0144', 'ssanchez@email.com', '258 Tulip Street', 'Oakland', 'CA', '94606', '01', 'Alameda', 'YES', '2022-08-10', 'ACTIVE', '2022-08-10', 40.0, NOW(), NOW()),
('PRV-045', 'KATHERINE', 'MORRIS', 'Q', '1994-01-27', 'Female', '555-01-0045', 'VERIFIED', '(408) 555-0145', 'kmorris@email.com', '369 Violet Lane', 'San Jose', 'CA', '95115', '43', 'Santa Clara', 'YES', '2023-07-05', 'ACTIVE', '2023-07-05', 8.0, NOW(), NOW()),
('PRV-046', 'PATRICK', 'ROGERS', 'R', '1980-04-03', 'Male', '555-01-0046', 'VERIFIED', '(714) 555-0146', 'progers@email.com', '741 Wisteria Blvd', 'Anaheim', 'CA', '92805', '30', 'Orange', 'YES', '2022-04-15', 'ACTIVE', '2022-04-15', 32.0, NOW(), NOW()),
('PRV-047', 'CAROLYN', 'REED', 'S', '1986-07-19', 'Female', '555-01-0047', 'VERIFIED', '(951) 555-0147', 'creed@email.com', '852 Zinnia Road', 'Riverside', 'CA', '92505', '33', 'Riverside', 'YES', '2023-08-20', 'ACTIVE', '2023-08-20', 20.0, NOW(), NOW()),
('PRV-048', 'DENNIS', 'COOK', 'T', '1977-11-08', 'Male', '555-01-0048', 'VERIFIED', '(916) 555-0148', 'dcook@email.com', '963 Aster Avenue', 'Sacramento', 'CA', '95821', '34', 'Sacramento', 'YES', '2022-11-25', 'ACTIVE', '2022-11-25', 44.0, NOW(), NOW()),
('PRV-049', 'JANET', 'MORGAN', 'U', '1989-02-22', 'Female', '555-01-0049', 'VERIFIED', '(213) 555-0149', 'jmorgan@email.com', '159 Begonia Court', 'Los Angeles', 'CA', '90007', '19', 'Los Angeles', 'YES', '2023-04-30', 'ACTIVE', '2023-04-30', 16.0, NOW(), NOW()),
('PRV-050', 'LARRY', 'BELL', 'V', '1982-06-11', 'Male', '555-01-0050', 'VERIFIED', '(415) 555-0150', 'lbell@email.com', '753 Carnation Ln', 'San Francisco', 'CA', '94108', '38', 'San Francisco', 'YES', '2022-09-05', 'ACTIVE', '2022-09-05', 36.0, NOW(), NOW())
ON CONFLICT (provider_number) DO NOTHING;

-- ============================================================
-- 2. RECIPIENTS (50 recipients)
-- ============================================================
INSERT INTO recipients (
    person_type, first_name, last_name, middle_name, date_of_birth, gender,
    ssn, ssn_type, cin, primary_phone, email,
    residence_street_number, residence_street_name, residence_city, residence_state, residence_zip, residence_county,
    county_code, county_name, referral_date, created_at, updated_at
) VALUES
-- Recipients
('RECIPIENT', 'DOROTHY', 'MURPHY', 'A', '1940-05-15', 'Female', '555-02-0001', 'VERIFIED', 'CIN0001', '(916) 555-1001', 'dmurphy@email.com', '100', 'Main Street', 'Sacramento', 'CA', '95814', 'Sacramento', '34', 'Sacramento', '2023-01-01', NOW(), NOW()),
('RECIPIENT', 'WALTER', 'BAILEY', 'B', '1938-08-22', 'Male', '555-02-0002', 'VERIFIED', 'CIN0002', '(916) 555-1002', 'wbailey@email.com', '200', 'Oak Avenue', 'Sacramento', 'CA', '95815', 'Sacramento', '34', 'Sacramento', '2023-01-15', NOW(), NOW()),
('RECIPIENT', 'HELEN', 'RIVERA', 'C', '1945-03-10', 'Female', '555-02-0003', 'VERIFIED', 'CIN0003', '(213) 555-1003', 'hrivera@email.com', '300', 'Sunset Blvd', 'Los Angeles', 'CA', '90001', 'Los Angeles', '19', 'Los Angeles', '2023-02-01', NOW(), NOW()),
('RECIPIENT', 'HAROLD', 'COOPER', 'D', '1935-11-28', 'Male', '555-02-0004', 'VERIFIED', 'CIN0004', '(415) 555-1004', 'hcooper@email.com', '400', 'Market Street', 'San Francisco', 'CA', '94102', 'San Francisco', '38', 'San Francisco', '2023-02-15', NOW(), NOW()),
('RECIPIENT', 'RUTH', 'RICHARDSON', 'E', '1942-07-05', 'Female', '555-02-0005', 'VERIFIED', 'CIN0005', '(619) 555-1005', 'rrichardson@email.com', '500', 'Harbor Drive', 'San Diego', 'CA', '92101', 'San Diego', '37', 'San Diego', '2023-03-01', NOW(), NOW()),
('RECIPIENT', 'HENRY', 'COX', 'F', '1939-01-18', 'Male', '555-02-0006', 'VERIFIED', 'CIN0006', '(510) 555-1006', 'hcox@email.com', '600', 'Broadway', 'Oakland', 'CA', '94601', 'Alameda', '01', 'Alameda', '2023-03-15', NOW(), NOW()),
('RECIPIENT', 'EVELYN', 'HOWARD', 'G', '1948-09-12', 'Female', '555-02-0007', 'VERIFIED', 'CIN0007', '(408) 555-1007', 'ehoward@email.com', '700', 'First Street', 'San Jose', 'CA', '95110', 'Santa Clara', '43', 'Santa Clara', '2023-04-01', NOW(), NOW()),
('RECIPIENT', 'ARTHUR', 'WARD', 'H', '1936-04-30', 'Male', '555-02-0008', 'VERIFIED', 'CIN0008', '(714) 555-1008', 'award@email.com', '800', 'Lincoln Ave', 'Anaheim', 'CA', '92801', 'Orange', '30', 'Orange', '2023-04-15', NOW(), NOW()),
('RECIPIENT', 'MARIE', 'TORRES', 'I', '1944-12-20', 'Female', '555-02-0009', 'VERIFIED', 'CIN0009', '(951) 555-1009', 'mtorres@email.com', '900', 'University Ave', 'Riverside', 'CA', '92501', 'Riverside', '33', 'Riverside', '2023-05-01', NOW(), NOW()),
('RECIPIENT', 'RAYMOND', 'PETERSON', 'J', '1941-06-08', 'Male', '555-02-0010', 'VERIFIED', 'CIN0010', '(916) 555-1010', 'rpeterson@email.com', '1000', 'Capitol Mall', 'Sacramento', 'CA', '95816', 'Sacramento', '34', 'Sacramento', '2023-05-15', NOW(), NOW()),
-- Applicants
('APPLICANT', 'MARGARET', 'GRAY', 'K', '1950-02-14', 'Female', '555-02-0011', 'VERIFIED', 'CIN0011', '(213) 555-1011', 'mgray@email.com', '1100', 'Wilshire Blvd', 'Los Angeles', 'CA', '90002', 'Los Angeles', '19', 'Los Angeles', '2024-01-05', NOW(), NOW()),
('APPLICANT', 'EUGENE', 'RAMIREZ', 'L', '1947-10-25', 'Male', '555-02-0012', 'VERIFIED', 'CIN0012', '(415) 555-1012', 'eramirez@email.com', '1200', 'Van Ness Ave', 'San Francisco', 'CA', '94103', 'San Francisco', '38', 'San Francisco', '2024-01-10', NOW(), NOW()),
('APPLICANT', 'FRANCES', 'JAMES', 'M', '1952-05-03', 'Female', '555-02-0013', 'NOT_YET_VERIFIED', 'CIN0013', '(619) 555-1013', 'fjames@email.com', '1300', 'Pacific Highway', 'San Diego', 'CA', '92102', 'San Diego', '37', 'San Diego', '2024-01-15', NOW(), NOW()),
('APPLICANT', 'CARL', 'WATSON', 'N', '1949-08-17', 'Male', '555-02-0014', 'NOT_YET_VERIFIED', 'CIN0014', '(510) 555-1014', 'cwatson@email.com', '1400', 'International Blvd', 'Oakland', 'CA', '94602', 'Alameda', '01', 'Alameda', '2024-01-08', NOW(), NOW()),
('CLOSED_REFERRAL', 'ALICE', 'BROOKS', 'O', '1955-11-30', 'Female', '555-02-0015', 'VERIFIED', 'CIN0015', '(408) 555-1015', 'abrooks@email.com', '1500', 'Santa Clara St', 'San Jose', 'CA', '95111', 'Santa Clara', '43', 'Santa Clara', '2023-12-01', NOW(), NOW()),
-- More recipients (16-50)
('RECIPIENT', 'LOUIS', 'KELLY', 'P', '1943-03-22', 'Male', '555-02-0016', 'VERIFIED', 'CIN0016', '(714) 555-1016', 'lkelly@email.com', '1600', 'Chapman Ave', 'Anaheim', 'CA', '92802', 'Orange', '30', 'Orange', '2023-06-01', NOW(), NOW()),
('RECIPIENT', 'SHIRLEY', 'SANDERS', 'Q', '1946-07-09', 'Female', '555-02-0017', 'VERIFIED', 'CIN0017', '(951) 555-1017', 'ssanders@email.com', '1700', 'Market Street', 'Riverside', 'CA', '92502', 'Riverside', '33', 'Riverside', '2023-06-15', NOW(), NOW()),
('RECIPIENT', 'JACK', 'PRICE', 'R', '1937-12-05', 'Male', '555-02-0018', 'VERIFIED', 'CIN0018', '(916) 555-1018', 'jprice@email.com', '1800', 'J Street', 'Sacramento', 'CA', '95817', 'Sacramento', '34', 'Sacramento', '2023-07-01', NOW(), NOW()),
('RECIPIENT', 'BETTY', 'BENNETT', 'S', '1940-09-18', 'Female', '555-02-0019', 'VERIFIED', 'CIN0019', '(213) 555-1019', 'bbennett@email.com', '1900', 'Figueroa St', 'Los Angeles', 'CA', '90003', 'Los Angeles', '19', 'Los Angeles', '2023-07-15', NOW(), NOW()),
('RECIPIENT', 'GEORGE', 'WOOD', 'T', '1934-04-27', 'Male', '555-02-0020', 'VERIFIED', 'CIN0020', '(415) 555-1020', 'gwood@email.com', '2000', 'Mission Street', 'San Francisco', 'CA', '94104', 'San Francisco', '38', 'San Francisco', '2023-08-01', NOW(), NOW()),
('RECIPIENT', 'ANNA', 'BARNES', 'U', '1947-01-11', 'Female', '555-02-0021', 'VERIFIED', 'CIN0021', '(619) 555-1021', 'abarnes@email.com', '2100', 'El Cajon Blvd', 'San Diego', 'CA', '92103', 'San Diego', '37', 'San Diego', '2023-08-15', NOW(), NOW()),
('RECIPIENT', 'FRANK', 'ROSS', 'V', '1941-06-24', 'Male', '555-02-0022', 'VERIFIED', 'CIN0022', '(510) 555-1022', 'fross@email.com', '2200', 'MacArthur Blvd', 'Oakland', 'CA', '94603', 'Alameda', '01', 'Alameda', '2023-09-01', NOW(), NOW()),
('RECIPIENT', 'VIRGINIA', 'HENDERSON', 'W', '1945-10-08', 'Female', '555-02-0023', 'VERIFIED', 'CIN0023', '(408) 555-1023', 'vhenderson@email.com', '2300', 'Almaden Blvd', 'San Jose', 'CA', '95112', 'Santa Clara', '43', 'Santa Clara', '2023-09-15', NOW(), NOW()),
('RECIPIENT', 'ROY', 'COLEMAN', 'X', '1938-03-15', 'Male', '555-02-0024', 'VERIFIED', 'CIN0024', '(714) 555-1024', 'rcoleman@email.com', '2400', 'Katella Ave', 'Anaheim', 'CA', '92803', 'Orange', '30', 'Orange', '2023-10-01', NOW(), NOW()),
('RECIPIENT', 'IRENE', 'JENKINS', 'Y', '1943-08-20', 'Female', '555-02-0025', 'VERIFIED', 'CIN0025', '(951) 555-1025', 'ijenkins@email.com', '2500', 'Iowa Ave', 'Riverside', 'CA', '92503', 'Riverside', '33', 'Riverside', '2023-10-15', NOW(), NOW()),
('RECIPIENT', 'ALBERT', 'PERRY', 'Z', '1936-11-02', 'Male', '555-02-0026', 'VERIFIED', 'CIN0026', '(916) 555-1026', 'aperry@email.com', '2600', 'Folsom Blvd', 'Sacramento', 'CA', '95818', 'Sacramento', '34', 'Sacramento', '2023-11-01', NOW(), NOW()),
('RECIPIENT', 'LILLIAN', 'POWELL', 'A', '1948-02-28', 'Female', '555-02-0027', 'VERIFIED', 'CIN0027', '(213) 555-1027', 'lpowell@email.com', '2700', 'Vermont Ave', 'Los Angeles', 'CA', '90004', 'Los Angeles', '19', 'Los Angeles', '2023-11-15', NOW(), NOW()),
('RECIPIENT', 'CLARENCE', 'LONG', 'B', '1939-05-16', 'Male', '555-02-0028', 'VERIFIED', 'CIN0028', '(415) 555-1028', 'clong@email.com', '2800', 'Geary Blvd', 'San Francisco', 'CA', '94105', 'San Francisco', '38', 'San Francisco', '2023-12-01', NOW(), NOW()),
('RECIPIENT', 'MILDRED', 'PATTERSON', 'C', '1944-09-07', 'Female', '555-02-0029', 'VERIFIED', 'CIN0029', '(619) 555-1029', 'mpatterson@email.com', '2900', 'University Ave', 'San Diego', 'CA', '92104', 'San Diego', '37', 'San Diego', '2023-12-15', NOW(), NOW()),
('RECIPIENT', 'HOWARD', 'HUGHES', 'D', '1935-12-12', 'Male', '555-02-0030', 'VERIFIED', 'CIN0030', '(510) 555-1030', 'hhughes@email.com', '3000', 'Telegraph Ave', 'Oakland', 'CA', '94604', 'Alameda', '01', 'Alameda', '2023-01-20', NOW(), NOW()),
('RECIPIENT', 'EDNA', 'FLORES', 'E', '1946-04-23', 'Female', '555-02-0031', 'VERIFIED', 'CIN0031', '(408) 555-1031', 'eflores@email.com', '3100', 'Stevens Creek', 'San Jose', 'CA', '95113', 'Santa Clara', '43', 'Santa Clara', '2023-02-05', NOW(), NOW()),
('RECIPIENT', 'EARL', 'WASHINGTON', 'F', '1940-07-30', 'Male', '555-02-0032', 'VERIFIED', 'CIN0032', '(714) 555-1032', 'ewashington@email.com', '3200', 'Harbor Blvd', 'Anaheim', 'CA', '92804', 'Orange', '30', 'Orange', '2023-02-20', NOW(), NOW()),
('RECIPIENT', 'GLADYS', 'BUTLER', 'G', '1942-11-18', 'Female', '555-02-0033', 'VERIFIED', 'CIN0033', '(951) 555-1033', 'gbutler@email.com', '3300', 'Central Ave', 'Riverside', 'CA', '92504', 'Riverside', '33', 'Riverside', '2023-03-05', NOW(), NOW()),
('RECIPIENT', 'LEONARD', 'SIMMONS', 'H', '1937-02-09', 'Male', '555-02-0034', 'VERIFIED', 'CIN0034', '(916) 555-1034', 'lsimmons@email.com', '3400', 'Stockton Blvd', 'Sacramento', 'CA', '95819', 'Sacramento', '34', 'Sacramento', '2023-03-20', NOW(), NOW()),
('RECIPIENT', 'HAZEL', 'FOSTER', 'I', '1949-06-05', 'Female', '555-02-0035', 'VERIFIED', 'CIN0035', '(213) 555-1035', 'hfoster@email.com', '3500', 'Olympic Blvd', 'Los Angeles', 'CA', '90005', 'Los Angeles', '19', 'Los Angeles', '2023-04-05', NOW(), NOW()),
('RECIPIENT', 'RUSSELL', 'GONZALES', 'J', '1941-09-28', 'Male', '555-02-0036', 'VERIFIED', 'CIN0036', '(415) 555-1036', 'rgonzales@email.com', '3600', 'Divisadero St', 'San Francisco', 'CA', '94106', 'San Francisco', '38', 'San Francisco', '2023-04-20', NOW(), NOW()),
('RECIPIENT', 'FLORENCE', 'BRYANT', 'K', '1945-01-14', 'Female', '555-02-0037', 'VERIFIED', 'CIN0037', '(619) 555-1037', 'fbryant@email.com', '3700', 'Broadway', 'San Diego', 'CA', '92105', 'San Diego', '37', 'San Diego', '2023-05-05', NOW(), NOW()),
('RECIPIENT', 'CLIFFORD', 'ALEXANDER', 'L', '1938-04-21', 'Male', '555-02-0038', 'VERIFIED', 'CIN0038', '(510) 555-1038', 'calexander@email.com', '3800', 'Grand Ave', 'Oakland', 'CA', '94605', 'Alameda', '01', 'Alameda', '2023-05-20', NOW(), NOW()),
('RECIPIENT', 'PAULINE', 'RUSSELL', 'M', '1943-08-03', 'Female', '555-02-0039', 'VERIFIED', 'CIN0039', '(408) 555-1039', 'prussell@email.com', '3900', 'San Carlos St', 'San Jose', 'CA', '95114', 'Santa Clara', '43', 'Santa Clara', '2023-06-05', NOW(), NOW()),
('RECIPIENT', 'NORMAN', 'GRIFFIN', 'N', '1936-12-26', 'Male', '555-02-0040', 'VERIFIED', 'CIN0040', '(714) 555-1040', 'ngriffin@email.com', '4000', 'Ball Road', 'Anaheim', 'CA', '92805', 'Orange', '30', 'Orange', '2023-06-20', NOW(), NOW()),
('RECIPIENT', 'THELMA', 'DIAZ', 'O', '1947-05-19', 'Female', '555-02-0041', 'VERIFIED', 'CIN0041', '(951) 555-1041', 'tdiaz@email.com', '4100', 'Magnolia Ave', 'Riverside', 'CA', '92505', 'Riverside', '33', 'Riverside', '2023-07-05', NOW(), NOW()),
('RECIPIENT', 'CHESTER', 'HAYES', 'P', '1939-10-11', 'Male', '555-02-0042', 'VERIFIED', 'CIN0042', '(916) 555-1042', 'chayes@email.com', '4200', 'Broadway', 'Sacramento', 'CA', '95820', 'Sacramento', '34', 'Sacramento', '2023-07-20', NOW(), NOW()),
('RECIPIENT', 'VERA', 'MYERS', 'Q', '1944-02-07', 'Female', '555-02-0043', 'VERIFIED', 'CIN0043', '(213) 555-1043', 'vmyers@email.com', '4300', 'Western Ave', 'Los Angeles', 'CA', '90006', 'Los Angeles', '19', 'Los Angeles', '2023-08-05', NOW(), NOW()),
('RECIPIENT', 'MARVIN', 'FORD', 'R', '1937-07-24', 'Male', '555-02-0044', 'VERIFIED', 'CIN0044', '(415) 555-1044', 'mford@email.com', '4400', 'Columbus Ave', 'San Francisco', 'CA', '94107', 'San Francisco', '38', 'San Francisco', '2023-08-20', NOW(), NOW()),
('RECIPIENT', 'LUCILLE', 'HAMILTON', 'S', '1942-11-30', 'Female', '555-02-0045', 'VERIFIED', 'CIN0045', '(619) 555-1045', 'lhamilton@email.com', '4500', 'Fifth Ave', 'San Diego', 'CA', '92106', 'San Diego', '37', 'San Diego', '2023-09-05', NOW(), NOW()),
('RECIPIENT', 'MELVIN', 'GRAHAM', 'T', '1935-04-05', 'Male', '555-02-0046', 'VERIFIED', 'CIN0046', '(510) 555-1046', 'mgraham@email.com', '4600', 'Piedmont Ave', 'Oakland', 'CA', '94606', 'Alameda', '01', 'Alameda', '2023-09-20', NOW(), NOW()),
('RECIPIENT', 'JOSEPHINE', 'SULLIVAN', 'U', '1946-09-17', 'Female', '555-02-0047', 'VERIFIED', 'CIN0047', '(408) 555-1047', 'jsullivan@email.com', '4700', 'Bascom Ave', 'San Jose', 'CA', '95115', 'Santa Clara', '43', 'Santa Clara', '2023-10-05', NOW(), NOW()),
('RECIPIENT', 'LEROY', 'WALLACE', 'V', '1940-01-28', 'Male', '555-02-0048', 'VERIFIED', 'CIN0048', '(714) 555-1048', 'lwallace@email.com', '4800', 'State College', 'Anaheim', 'CA', '92806', 'Orange', '30', 'Orange', '2023-10-20', NOW(), NOW()),
('RECIPIENT', 'GERTRUDE', 'WEST', 'W', '1943-06-12', 'Female', '555-02-0049', 'VERIFIED', 'CIN0049', '(951) 555-1049', 'gwest@email.com', '4900', 'Arlington Ave', 'Riverside', 'CA', '92506', 'Riverside', '33', 'Riverside', '2023-11-05', NOW(), NOW()),
('RECIPIENT', 'BERNARD', 'COLE', 'X', '1938-08-25', 'Male', '555-02-0050', 'VERIFIED', 'CIN0050', '(916) 555-1050', 'bcole@email.com', '5000', 'Alhambra Blvd', 'Sacramento', 'CA', '95821', 'Sacramento', '34', 'Sacramento', '2023-11-20', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. CASES (linked to recipients)
-- ============================================================
INSERT INTO cases (
    case_number, recipient_id, case_status, case_type, county_code, county_name,
    case_owner_id, case_owner_name, supervisor_id, cin,
    funding_source, application_date, eligibility_date,
    authorized_hours_monthly, authorization_start_date, authorization_end_date,
    created_at, updated_at
)
SELECT
    'CASE-' || LPAD(r.id::TEXT, 5, '0'),
    r.id,
    CASE
        WHEN r.person_type = 'RECIPIENT' THEN 'ELIGIBLE'
        WHEN r.person_type = 'APPLICANT' THEN 'PENDING'
        WHEN r.person_type = 'CLOSED_REFERRAL' THEN 'DENIED'
        ELSE 'PENDING'
    END,
    CASE (r.id % 3)
        WHEN 0 THEN 'IHSS'
        WHEN 1 THEN 'WPCS'
        ELSE 'IHSS_WPCS'
    END,
    r.county_code,
    r.county_name,
    'caseworker1',
    'John Caseworker',
    'supervisor1',
    r.cin,
    'PCSP',
    r.referral_date - INTERVAL '30 days',
    CASE WHEN r.person_type = 'RECIPIENT' THEN r.referral_date ELSE NULL END,
    CASE WHEN r.person_type = 'RECIPIENT' THEN (150 + (r.id % 100) * 2)::double precision ELSE 0 END,
    CASE WHEN r.person_type = 'RECIPIENT' THEN r.referral_date ELSE NULL END,
    CASE WHEN r.person_type = 'RECIPIENT' THEN r.referral_date + INTERVAL '1 year' ELSE NULL END,
    NOW(),
    NOW()
FROM recipients r
ON CONFLICT (case_number) DO NOTHING;

-- ============================================================
-- 4. TASKS / WORK QUEUE (100 tasks)
-- ============================================================
INSERT INTO tasks (
    title, description, status, priority, assigned_to, created_by,
    due_date, related_entity_type, related_entity_id, work_queue,
    created_at, updated_at
) VALUES
-- Open tasks
('Review Timesheet - Maria Gonzalez', 'Review and approve submitted timesheet for provider PRV-001', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '2 days', 'TIMESHEET', 1, 'Timesheet Review', NOW(), NOW()),
('Review Timesheet - John Smith', 'Review and approve submitted timesheet for provider PRV-002', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '1 day', 'TIMESHEET', 2, 'Timesheet Review', NOW(), NOW()),
('Process New Application - Frances James', 'Process new IHSS application for applicant CIN0013', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'APPLICATION', 13, 'New Applications', NOW(), NOW()),
('Process New Application - Carl Watson', 'Process new IHSS application for applicant CIN0014', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'APPLICATION', 14, 'New Applications', NOW(), NOW()),
('Annual Reassessment - Dorothy Murphy', 'Complete annual reassessment for recipient CIN0001', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '30 days', 'CASE', 1, 'Reassessments', NOW(), NOW()),
('Annual Reassessment - Walter Bailey', 'Complete annual reassessment for recipient CIN0002', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '25 days', 'CASE', 2, 'Reassessments', NOW(), NOW()),
('EVV Violation Review - Provider PRV-003', 'Review EVV violation for provider Sarah Johnson', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '3 days', 'PROVIDER', 3, 'EVV Violations', NOW(), NOW()),
('Provider Certification Expiring - PRV-021', 'Provider certification expires in 30 days', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '30 days', 'PROVIDER', 21, 'Certifications', NOW(), NOW()),
('Provider Certification Expiring - PRV-022', 'Provider certification expires in 30 days', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '30 days', 'PROVIDER', 22, 'Certifications', NOW(), NOW()),
('Address Change Verification - Case CASE-00003', 'Verify address change for case', 'OPEN', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '7 days', 'CASE', 3, 'Address Changes', NOW(), NOW()),
-- In Progress tasks
('Review Timesheet - Sarah Johnson', 'Review and approve submitted timesheet for provider PRV-003', 'IN_PROGRESS', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '1 day', 'TIMESHEET', 3, 'Timesheet Review', NOW(), NOW()),
('Process New Application - Eugene Ramirez', 'Process presumptive eligibility for applicant CIN0012', 'IN_PROGRESS', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '3 days', 'APPLICATION', 12, 'New Applications', NOW(), NOW()),
('Annual Reassessment - Helen Rivera', 'Complete annual reassessment for recipient CIN0003', 'IN_PROGRESS', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '20 days', 'CASE', 3, 'Reassessments', NOW(), NOW()),
('Provider Background Check - PRV-019', 'Complete background check for new provider', 'IN_PROGRESS', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'PROVIDER', 19, 'Provider Enrollment', NOW(), NOW()),
('Provider Background Check - PRV-020', 'Complete background check for new provider', 'IN_PROGRESS', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'PROVIDER', 20, 'Provider Enrollment', NOW(), NOW()),
-- Closed tasks
('Review Timesheet - Michael Williams', 'Reviewed and approved timesheet', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '2 days', 'TIMESHEET', 4, 'Timesheet Review', NOW() - INTERVAL '5 days', NOW()),
('Review Timesheet - Jennifer Brown', 'Reviewed and approved timesheet', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '3 days', 'TIMESHEET', 5, 'Timesheet Review', NOW() - INTERVAL '6 days', NOW()),
('Annual Reassessment - Harold Cooper', 'Completed annual reassessment', 'CLOSED', 'MEDIUM', 'caseworker1', 'system', NOW() - INTERVAL '10 days', 'CASE', 4, 'Reassessments', NOW() - INTERVAL '30 days', NOW()),
('Provider Enrollment - PRV-023', 'Completed provider enrollment', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '5 days', 'PROVIDER', 23, 'Provider Enrollment', NOW() - INTERVAL '20 days', NOW()),
('Address Change - Case CASE-00005', 'Address change verified', 'CLOSED', 'LOW', 'caseworker1', 'system', NOW() - INTERVAL '7 days', 'CASE', 5, 'Address Changes', NOW() - INTERVAL '14 days', NOW()),
-- More open tasks for variety
('Overtime Violation Review - PRV-006', 'Review overtime violation for provider', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '2 days', 'PROVIDER', 6, 'Overtime Violations', NOW(), NOW()),
('Sick Leave Request - PRV-007', 'Process sick leave request', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '1 day', 'PROVIDER', 7, 'Sick Leave', NOW(), NOW()),
('Hours Adjustment - Case CASE-00008', 'Review hours adjustment request', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'CASE', 8, 'Hours Adjustments', NOW(), NOW()),
('Provider Transfer Request - PRV-009', 'Process provider county transfer', 'OPEN', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '10 days', 'PROVIDER', 9, 'Transfers', NOW(), NOW()),
('Recipient Transfer Request - CIN0010', 'Process recipient county transfer', 'OPEN', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '10 days', 'RECIPIENT', 10, 'Transfers', NOW(), NOW()),
-- Tasks assigned to supervisor
('Approve Denied Application - CIN0015', 'Review denial decision appeal', 'OPEN', 'HIGH', 'supervisor1', 'caseworker1', NOW() + INTERVAL '3 days', 'APPLICATION', 15, 'Appeals', NOW(), NOW()),
('Review Provider Termination - PRV-021', 'Review provider termination request', 'OPEN', 'HIGH', 'supervisor1', 'caseworker1', NOW() + INTERVAL '2 days', 'PROVIDER', 21, 'Terminations', NOW(), NOW()),
('Quality Review - Case CASE-00020', 'Quality assurance review', 'OPEN', 'LOW', 'supervisor1', 'system', NOW() + INTERVAL '14 days', 'CASE', 20, 'Quality Reviews', NOW(), NOW()),
-- More tasks
('Review Timesheet - Provider PRV-010', 'Review pending timesheet', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '2 days', 'TIMESHEET', 10, 'Timesheet Review', NOW(), NOW()),
('Review Timesheet - Provider PRV-011', 'Review pending timesheet', 'OPEN', 'HIGH', 'caseworker1', 'system', NOW() + INTERVAL '2 days', 'TIMESHEET', 11, 'Timesheet Review', NOW(), NOW()),
('Review Timesheet - Provider PRV-012', 'Review pending timesheet', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '3 days', 'TIMESHEET', 12, 'Timesheet Review', NOW(), NOW()),
('Annual Reassessment - CIN0016', 'Complete annual reassessment', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '45 days', 'CASE', 16, 'Reassessments', NOW(), NOW()),
('Annual Reassessment - CIN0017', 'Complete annual reassessment', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '40 days', 'CASE', 17, 'Reassessments', NOW(), NOW()),
('Annual Reassessment - CIN0018', 'Complete annual reassessment', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '35 days', 'CASE', 18, 'Reassessments', NOW(), NOW()),
('Provider Document Review - PRV-024', 'Review submitted documents', 'OPEN', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '7 days', 'PROVIDER', 24, 'Documents', NOW(), NOW()),
('Provider Document Review - PRV-025', 'Review submitted documents', 'OPEN', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '7 days', 'PROVIDER', 25, 'Documents', NOW(), NOW()),
('Case Note Follow-up - CASE-00019', 'Follow up on case note', 'IN_PROGRESS', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'CASE', 19, 'Follow-ups', NOW(), NOW()),
('Case Note Follow-up - CASE-00021', 'Follow up on case note', 'IN_PROGRESS', 'LOW', 'caseworker1', 'system', NOW() + INTERVAL '5 days', 'CASE', 21, 'Follow-ups', NOW(), NOW()),
('EVV Compliance Check - PRV-026', 'Verify EVV compliance', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '7 days', 'PROVIDER', 26, 'EVV Compliance', NOW(), NOW()),
('EVV Compliance Check - PRV-027', 'Verify EVV compliance', 'OPEN', 'MEDIUM', 'caseworker1', 'system', NOW() + INTERVAL '7 days', 'PROVIDER', 27, 'EVV Compliance', NOW(), NOW()),
('Review Timesheet - Provider PRV-028', 'Review pending timesheet', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '1 day', 'TIMESHEET', 28, 'Timesheet Review', NOW() - INTERVAL '4 days', NOW()),
('Review Timesheet - Provider PRV-029', 'Review pending timesheet', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '1 day', 'TIMESHEET', 29, 'Timesheet Review', NOW() - INTERVAL '4 days', NOW()),
('Review Timesheet - Provider PRV-030', 'Review pending timesheet', 'CLOSED', 'MEDIUM', 'caseworker1', 'system', NOW() - INTERVAL '2 days', 'TIMESHEET', 30, 'Timesheet Review', NOW() - INTERVAL '5 days', NOW()),
('Annual Reassessment - CIN0022', 'Completed annual reassessment', 'CLOSED', 'MEDIUM', 'caseworker1', 'system', NOW() - INTERVAL '15 days', 'CASE', 22, 'Reassessments', NOW() - INTERVAL '45 days', NOW()),
('Annual Reassessment - CIN0023', 'Completed annual reassessment', 'CLOSED', 'MEDIUM', 'caseworker1', 'system', NOW() - INTERVAL '20 days', 'CASE', 23, 'Reassessments', NOW() - INTERVAL '50 days', NOW()),
('Provider Enrollment - PRV-031', 'Completed provider enrollment', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '10 days', 'PROVIDER', 31, 'Provider Enrollment', NOW() - INTERVAL '25 days', NOW()),
('Provider Enrollment - PRV-032', 'Completed provider enrollment', 'CLOSED', 'HIGH', 'caseworker1', 'system', NOW() - INTERVAL '12 days', 'PROVIDER', 32, 'Provider Enrollment', NOW() - INTERVAL '27 days', NOW()),
('Hours Adjustment - Case CASE-00024', 'Completed hours adjustment', 'CLOSED', 'MEDIUM', 'caseworker1', 'system', NOW() - INTERVAL '5 days', 'CASE', 24, 'Hours Adjustments', NOW() - INTERVAL '12 days', NOW())
ON CONFLICT DO NOTHING;

-- Add more tasks to reach approximately 100
INSERT INTO tasks (title, description, status, priority, assigned_to, created_by, due_date, related_entity_type, related_entity_id, work_queue, created_at, updated_at)
SELECT
    'Task ' || gs || ' - Review Item',
    'Auto-generated task for testing purposes',
    CASE WHEN gs % 3 = 0 THEN 'CLOSED' WHEN gs % 3 = 1 THEN 'IN_PROGRESS' ELSE 'OPEN' END,
    CASE WHEN gs % 3 = 0 THEN 'LOW' WHEN gs % 3 = 1 THEN 'MEDIUM' ELSE 'HIGH' END,
    CASE WHEN gs % 2 = 0 THEN 'caseworker1' ELSE 'supervisor1' END,
    'system',
    NOW() + (gs || ' days')::INTERVAL,
    CASE WHEN gs % 4 = 0 THEN 'CASE' WHEN gs % 4 = 1 THEN 'PROVIDER' WHEN gs % 4 = 2 THEN 'RECIPIENT' ELSE 'TIMESHEET' END,
    (gs % 50) + 1,
    CASE WHEN gs % 5 = 0 THEN 'Timesheet Review' WHEN gs % 5 = 1 THEN 'Reassessments' WHEN gs % 5 = 2 THEN 'Provider Enrollment' WHEN gs % 5 = 3 THEN 'EVV Compliance' ELSE 'General' END,
    NOW(),
    NOW()
FROM generate_series(51, 100) gs;

-- ============================================================
-- 5. PROVIDER-RECIPIENT RELATIONSHIPS
-- ============================================================
INSERT INTO provider_recipient_relationships (
    provider_id, provider_name, recipient_id, recipient_name, case_number,
    authorized_hours_per_month, status, relationship, county, start_date,
    created_at, updated_at
)
SELECT
    'PRV-' || LPAD(((r.id - 1) % 40 + 1)::TEXT, 3, '0'),
    (SELECT first_name || ' ' || last_name FROM providers WHERE provider_number = 'PRV-' || LPAD(((r.id - 1) % 40 + 1)::TEXT, 3, '0') LIMIT 1),
    r.cin,
    r.first_name || ' ' || r.last_name,
    'CASE-' || LPAD(r.id::TEXT, 5, '0'),
    (150 + (r.id % 100) * 2),
    'ACTIVE',
    'PRIMARY_PROVIDER',
    r.residence_county,
    r.referral_date,
    NOW(),
    NOW()
FROM recipients r
WHERE r.person_type = 'RECIPIENT'
AND r.id <= 40
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. NOTIFICATIONS (50 notifications)
-- ============================================================
INSERT INTO notifications (
    user_id, message, notification_type, action_link, related_entity_type, related_entity_id, read_status, created_at
) VALUES
-- Notifications for caseworker1
('caseworker1', 'New timesheet submitted by Maria Gonzalez requires review', 'INFO', '/timesheets/1', 'TIMESHEET', 1, false, NOW()),
('caseworker1', 'New timesheet submitted by John Smith requires review', 'INFO', '/timesheets/2', 'TIMESHEET', 2, false, NOW() - INTERVAL '1 hour'),
('caseworker1', 'New application received for Frances James', 'INFO', '/applications/13', 'APPLICATION', 13, false, NOW() - INTERVAL '2 hours'),
('caseworker1', 'Annual reassessment due in 30 days for Dorothy Murphy', 'WARNING', '/cases/1', 'CASE', 1, false, NOW() - INTERVAL '3 hours'),
('caseworker1', 'EVV violation detected for provider Sarah Johnson', 'ALERT', '/providers/3', 'PROVIDER', 3, false, NOW() - INTERVAL '4 hours'),
('caseworker1', 'Provider certification expiring for PRV-021', 'WARNING', '/providers/21', 'PROVIDER', 21, false, NOW() - INTERVAL '5 hours'),
('caseworker1', 'Overtime violation requires immediate attention', 'ALERT', '/providers/6', 'PROVIDER', 6, false, NOW() - INTERVAL '6 hours'),
('caseworker1', 'Task completed: Timesheet review for Michael Williams', 'SUCCESS', '/timesheets/4', 'TIMESHEET', 4, true, NOW() - INTERVAL '1 day'),
('caseworker1', 'Task completed: Annual reassessment for Harold Cooper', 'SUCCESS', '/cases/4', 'CASE', 4, true, NOW() - INTERVAL '2 days'),
('caseworker1', 'Application approved for Eugene Ramirez', 'SUCCESS', '/applications/12', 'APPLICATION', 12, true, NOW() - INTERVAL '3 days'),
-- Notifications for supervisor1
('supervisor1', 'Appeal submitted for denied application CIN0015', 'INFO', '/applications/15', 'APPLICATION', 15, false, NOW()),
('supervisor1', 'Provider termination request for PRV-021 requires review', 'WARNING', '/providers/21', 'PROVIDER', 21, false, NOW() - INTERVAL '1 hour'),
('supervisor1', 'Quality review scheduled for Case CASE-00020', 'INFO', '/cases/20', 'CASE', 20, false, NOW() - INTERVAL '2 hours'),
('supervisor1', 'Caseworker completed 10 tasks today', 'SUCCESS', '/dashboard', 'SYSTEM', NULL, true, NOW() - INTERVAL '1 day'),
('supervisor1', 'Weekly report generated successfully', 'INFO', '/reports', 'SYSTEM', NULL, true, NOW() - INTERVAL '2 days'),
-- Notifications for providers
('provider1', 'Your timesheet for January has been approved', 'SUCCESS', '/provider/timesheets', 'TIMESHEET', 1, false, NOW()),
('provider1', 'Reminder: Complete EVV check-in for today', 'INFO', '/provider/evv', 'EVV', NULL, false, NOW() - INTERVAL '6 hours'),
('provider1', 'Your certification expires in 60 days', 'WARNING', '/provider/certification', 'PROVIDER', 1, false, NOW() - INTERVAL '1 day'),
-- Notifications for recipients
('recipient1', 'Your care provider Maria Gonzalez has submitted January timesheet', 'INFO', '/recipient/timesheets', 'TIMESHEET', 1, false, NOW()),
('recipient1', 'Annual reassessment scheduled for next month', 'INFO', '/recipient/assessments', 'CASE', 1, false, NOW() - INTERVAL '1 day'),
('recipient1', 'Your authorized hours have been updated', 'INFO', '/recipient/case', 'CASE', 1, true, NOW() - INTERVAL '5 days')
ON CONFLICT DO NOTHING;

-- Add more notifications programmatically
INSERT INTO notifications (user_id, message, notification_type, action_link, related_entity_type, related_entity_id, read_status, created_at)
SELECT
    CASE WHEN gs % 3 = 0 THEN 'caseworker1' WHEN gs % 3 = 1 THEN 'supervisor1' ELSE 'provider1' END,
    'System notification #' || gs,
    CASE WHEN gs % 4 = 0 THEN 'INFO' WHEN gs % 4 = 1 THEN 'WARNING' WHEN gs % 4 = 2 THEN 'SUCCESS' ELSE 'ALERT' END,
    '/notifications/' || gs,
    CASE WHEN gs % 3 = 0 THEN 'CASE' WHEN gs % 3 = 1 THEN 'PROVIDER' ELSE 'TIMESHEET' END,
    (gs % 50) + 1,
    gs % 2 = 0,
    NOW() - (gs || ' hours')::INTERVAL
FROM generate_series(22, 50) gs;

-- ============================================================
-- 7. WORK QUEUE SUBSCRIPTIONS
-- ============================================================
INSERT INTO work_queue_subscriptions (
    username, work_queue, subscribed_by, created_at, updated_at
) VALUES
('caseworker1', 'Timesheet Review', 'caseworker1', NOW(), NOW()),
('caseworker1', 'New Applications', 'caseworker1', NOW(), NOW()),
('caseworker1', 'Reassessments', 'caseworker1', NOW(), NOW()),
('caseworker1', 'EVV Violations', 'caseworker1', NOW(), NOW()),
('caseworker1', 'Provider Enrollment', 'caseworker1', NOW(), NOW()),
('caseworker1', 'General', 'caseworker1', NOW(), NOW()),
('supervisor1', 'Appeals', 'supervisor1', NOW(), NOW()),
('supervisor1', 'Terminations', 'supervisor1', NOW(), NOW()),
('supervisor1', 'Quality Reviews', 'supervisor1', NOW(), NOW()),
('supervisor1', 'General', 'supervisor1', NOW(), NOW())
ON CONFLICT (username, work_queue) DO NOTHING;

-- ============================================================
-- VERIFICATION QUERIES
-- ============================================================
SELECT 'PROVIDERS' as entity, COUNT(*) as count FROM providers;
SELECT 'RECIPIENTS' as entity, COUNT(*) as count FROM recipients;
SELECT 'CASES' as entity, COUNT(*) as count FROM cases;
SELECT 'TASKS' as entity, COUNT(*) as count FROM tasks;
SELECT 'NOTIFICATIONS' as entity, COUNT(*) as count FROM notifications;
SELECT 'WORK_QUEUE_SUBS' as entity, COUNT(*) as count FROM work_queue_subscriptions;
SELECT 'RELATIONSHIPS' as entity, COUNT(*) as count FROM provider_recipient_relationships;

-- Task summary by status
SELECT 'TASKS BY STATUS:' as info;
SELECT status, COUNT(*) as count FROM tasks GROUP BY status ORDER BY status;

-- Provider summary by status
SELECT 'PROVIDERS BY STATUS:' as info;
SELECT provider_status, COUNT(*) as count FROM providers GROUP BY provider_status ORDER BY provider_status;

-- Recipient summary by person_type
SELECT 'RECIPIENTS BY TYPE:' as info;
SELECT person_type, COUNT(*) as count FROM recipients GROUP BY person_type ORDER BY person_type;
