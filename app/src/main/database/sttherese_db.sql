-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 23, 2025 at 02:40 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `sttherese_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `appointments`
--

CREATE TABLE `appointments` (
  `appointment_id` int(11) NOT NULL,
  `transaction_number` varchar(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `doctor_name` varchar(100) NOT NULL,
  `specialty` varchar(100) NOT NULL,
  `appointment_date` date NOT NULL,
  `appointment_time` time NOT NULL,
  `status` enum('Upcoming','Completed','Cancelled') DEFAULT 'Upcoming',
  `cancel_reason` varchar(255) DEFAULT NULL,
  `cancel_notes` text DEFAULT NULL,
  `cancelled_by` int(11) DEFAULT NULL,
  `remarks` text DEFAULT NULL,
  `clinic_staff_id` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `doctors`
--

CREATE TABLE `doctors` (
  `doctor_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `specialty` varchar(100) NOT NULL,
  `schedule_days` varchar(150) NOT NULL,
  `availability_type` varchar(50) DEFAULT 'Regular',
  `contact_number` varchar(15) DEFAULT NULL,
  `remarks` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `doctors`
--

INSERT INTO `doctors` (`doctor_id`, `name`, `specialty`, `schedule_days`, `availability_type`, `contact_number`, `remarks`) VALUES
(1, 'Dr. Maria Santos', 'Ob-Gyne', 'Monday to Saturday', 'Regular', NULL, NULL),
(2, 'Dr. Ana Reyes', 'Pediatrician', 'Monday to Saturday', 'Regular', NULL, NULL),
(3, 'Dr. Jose Cruz', 'Internal Medicine', 'Monday, Thursday & Saturday', 'Regular', NULL, NULL),
(4, 'Dr. Carlo Dela Peña', 'Orthopedic', 'Friday', 'Regular', NULL, NULL),
(5, 'Dr. Liza Ramos', 'Family Medicine', 'Monday, Tuesday, Wednesday & Friday', 'Regular', NULL, NULL),
(6, 'Dr. Ben Castillo', 'Surgeon', 'By Appointment', 'By Appointment', NULL, NULL),
(7, 'Dr. Paolo Lim', 'ENT', 'Monday, Wednesday & Friday', 'Regular', NULL, NULL),
(8, 'Dr. Rhea Navarro', 'Radiologist', 'Monday to Saturday', 'Regular', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `email_verification`
--

CREATE TABLE `email_verification` (
  `id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `token` varchar(64) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `expires_at` datetime NOT NULL,
  `verified` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `email_verification`
--

INSERT INTO `email_verification` (`id`, `email`, `token`, `created_at`, `expires_at`, `verified`) VALUES
(5, 'minyoonice93@gmail.com', '793768', '2025-10-21 07:13:07', '2025-10-21 09:28:07', 1),
(6, 'princesskyriefamini@gmail.com', '426720', '2025-10-21 10:09:08', '2025-10-21 12:24:08', 1),
(9, 'dfamini.k12044438@umak.edu.ph', '257996', '2025-10-23 12:20:23', '2025-10-23 14:35:23', 1);

-- --------------------------------------------------------

--
-- Table structure for table `existing_patients`
--

CREATE TABLE `existing_patients` (
  `existing_id` int(11) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `contact` varchar(20) NOT NULL,
  `date_added` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `existing_patients`
--

INSERT INTO `existing_patients` (`existing_id`, `first_name`, `last_name`, `contact`, `date_added`) VALUES
(1, 'jimmy', 'famini', '09604177314', '2025-10-23 10:52:39');

-- --------------------------------------------------------

--
-- Table structure for table `patient`
--

CREATE TABLE `patient` (
  `patient_id` int(11) NOT NULL,
  `existing_id` int(11) DEFAULT NULL,
  `first_name` varchar(100) NOT NULL,
  `middle_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) NOT NULL,
  `dob` date NOT NULL,
  `age` int(11) NOT NULL,
  `gender` varchar(20) NOT NULL,
  `contact` varchar(20) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `patient`
--

INSERT INTO `patient` (`patient_id`, `existing_id`, `first_name`, `middle_name`, `last_name`, `dob`, `age`, `gender`, `contact`, `address`, `created_at`, `updated_at`, `deleted_at`, `user_id`) VALUES
(6, NULL, 'denice', 'albesa', 'famini', '2003-08-28', 22, 'Female', '09690396748', 'james st.', '2025-10-21 07:13:07', '2025-10-23 09:45:02', NULL, 9),
(7, NULL, 'Princess Kyrie', '', 'Famini', '2012-02-16', 13, 'Female', '09615199142', 'James St', '2025-10-21 10:09:08', '2025-10-21 10:09:08', NULL, 10),
(12, 1, 'jimmy', 'albesa', 'famini', '2025-10-13', 0, 'Male', '09604177314', 'jamws', '2025-10-23 12:20:23', '2025-10-23 12:20:23', NULL, 13);

-- --------------------------------------------------------

--
-- Table structure for table `services`
--

CREATE TABLE `services` (
  `service_id` int(11) NOT NULL,
  `service_name` varchar(150) NOT NULL,
  `description` text DEFAULT NULL,
  `schedule_info` varchar(150) DEFAULT NULL,
  `assigned_doctor_id` int(11) DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `services`
--

INSERT INTO `services` (`service_id`, `service_name`, `description`, `schedule_info`, `assigned_doctor_id`, `category`) VALUES
(1, 'Adult & Pedia Consultation', NULL, NULL, NULL, 'Medical'),
(2, 'Prenatal & Postnatal Check-up', NULL, NULL, NULL, 'Medical'),
(3, 'Vaccinations / Immunizations', NULL, NULL, NULL, 'Medical'),
(4, 'Family Planning & Counselling', NULL, NULL, NULL, 'Medical'),
(5, 'Minor Surgical Procedure', NULL, NULL, NULL, 'Surgical'),
(6, 'Medical Certificate Issuance', NULL, NULL, NULL, 'Administrative'),
(7, 'Annual PE / Pre-employment Check-up', NULL, NULL, NULL, 'Medical'),
(8, 'Ears, Nose & Throat Services', NULL, NULL, NULL, 'ENT'),
(9, 'Orthopedic Consultation', NULL, NULL, NULL, 'Orthopedic'),
(10, 'Ultrasound (OB/RADIO)', NULL, NULL, NULL, 'Diagnostic'),
(11, '24/7 Lying-in (Paanakan)', NULL, NULL, NULL, 'Lying-in'),
(12, 'Free Prenatal Check-up (by Midwife)', NULL, NULL, NULL, 'Medical');

-- --------------------------------------------------------

--
-- Table structure for table `user_account`
--

CREATE TABLE `user_account` (
  `user_id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(60) NOT NULL,
  `user_role_id` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL,
  `password_reset_token` varchar(100) DEFAULT NULL,
  `token_expiry` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_account`
--

INSERT INTO `user_account` (`user_id`, `email`, `password`, `user_role_id`, `created_at`, `updated_at`, `deleted_at`, `password_reset_token`, `token_expiry`) VALUES
(9, 'minyoonice93@gmail.com', '$2y$10$F6frnI9alSToBd6VN/04t.LOgIkoP3v2LNL7zR0QSgJpnSJYeFx4e', 4, '2025-10-21 07:13:07', '2025-10-23 12:25:58', NULL, NULL, NULL),
(10, 'princesskyriefamini@gmail.com', '$2y$10$nRdFkco/WoOEg2kQMvyzduvsuqDi/3bJ8oyhD9JycP7RMwrIaWgLy', 4, '2025-10-21 10:09:08', '2025-10-21 10:09:08', NULL, NULL, NULL),
(13, 'dfamini.k12044438@umak.edu.ph', '$2y$10$E2SKbwXOmOuewbzZYneTgeYFjdQzmgUaypjdOQHZMdN/AFHAdeLkK', 4, '2025-10-23 12:20:23', '2025-10-23 12:20:23', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `user_role`
--

CREATE TABLE `user_role` (
  `user_role_id` int(11) NOT NULL,
  `user_role_type` varchar(50) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_role`
--

INSERT INTO `user_role` (`user_role_id`, `user_role_type`, `created_at`, `updated_at`, `deleted_at`) VALUES
(1, 'clinic_manager', '2025-10-21 02:41:10', '2025-10-21 02:41:10', NULL),
(2, 'clinic_staff', '2025-10-21 02:41:32', '2025-10-21 02:41:32', NULL),
(3, 'doctor', '2025-10-21 02:41:41', '2025-10-21 02:41:41', NULL),
(4, 'patient', '2025-10-21 02:41:53', '2025-10-21 02:41:53', NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `appointments`
--
ALTER TABLE `appointments`
  ADD PRIMARY KEY (`appointment_id`);

--
-- Indexes for table `doctors`
--
ALTER TABLE `doctors`
  ADD PRIMARY KEY (`doctor_id`);

--
-- Indexes for table `email_verification`
--
ALTER TABLE `email_verification`
  ADD PRIMARY KEY (`id`),
  ADD KEY `email` (`email`),
  ADD KEY `token` (`token`);

--
-- Indexes for table `existing_patients`
--
ALTER TABLE `existing_patients`
  ADD PRIMARY KEY (`existing_id`);

--
-- Indexes for table `patient`
--
ALTER TABLE `patient`
  ADD PRIMARY KEY (`patient_id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `existing_id` (`existing_id`);

--
-- Indexes for table `services`
--
ALTER TABLE `services`
  ADD PRIMARY KEY (`service_id`),
  ADD KEY `assigned_doctor_id` (`assigned_doctor_id`);

--
-- Indexes for table `user_account`
--
ALTER TABLE `user_account`
  ADD PRIMARY KEY (`user_id`),
  ADD KEY `fk_user_role` (`user_role_id`);

--
-- Indexes for table `user_role`
--
ALTER TABLE `user_role`
  ADD PRIMARY KEY (`user_role_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `appointments`
--
ALTER TABLE `appointments`
  MODIFY `appointment_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `doctors`
--
ALTER TABLE `doctors`
  MODIFY `doctor_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `email_verification`
--
ALTER TABLE `email_verification`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `existing_patients`
--
ALTER TABLE `existing_patients`
  MODIFY `existing_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `patient`
--
ALTER TABLE `patient`
  MODIFY `patient_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `services`
--
ALTER TABLE `services`
  MODIFY `service_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `user_account`
--
ALTER TABLE `user_account`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `patient`
--
ALTER TABLE `patient`
  ADD CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `patient_ibfk_1` FOREIGN KEY (`existing_id`) REFERENCES `existing_patients` (`existing_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `services`
--
ALTER TABLE `services`
  ADD CONSTRAINT `services_ibfk_1` FOREIGN KEY (`assigned_doctor_id`) REFERENCES `doctors` (`doctor_id`);

--
-- Constraints for table `user_account`
--
ALTER TABLE `user_account`
  ADD CONSTRAINT `fk_user_role` FOREIGN KEY (`user_role_id`) REFERENCES `user_role` (`user_role_id`) ON DELETE SET NULL ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
