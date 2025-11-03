<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
include 'db_connect.php';
header('Content-Type: application/json');

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'smtp_mailer/PHPMailer/src/Exception.php';
require 'smtp_mailer/PHPMailer/src/PHPMailer.php';
require 'smtp_mailer/PHPMailer/src/SMTP.php';

// Get form data from Account Credentials page
$first_name = $_POST['first_name'] ?? '';
$middle_name = $_POST['middle_name'] ?? '';
$last_name = $_POST['last_name'] ?? '';
$dob = $_POST['dob'] ?? '';
$gender = $_POST['gender'] ?? '';
$contact = $_POST['contact'] ?? '';
$address = $_POST['address'] ?? '';
$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

// Generate 6-digit verification code
$verification_code = rand(100000, 999999);

// Validate date format
if (!empty($dob) && !preg_match('/^\d{4}-\d{2}-\d{2}$/', $dob)) {
    echo json_encode(["success" => false, "message" => "Invalid date format"]);
    exit;
}

// Validate required fields
if (
    empty($first_name) || empty($last_name) || empty($dob) ||
    empty($gender) || empty($contact) || empty($address) ||
    empty($email) || empty($password)
) {
    echo json_encode(["success" => false, "message" => "All required fields must be filled"]);
    exit;
}

// Step 1: Check if the patient exists in existing_patients
$check_existing = $connect->prepare("
    SELECT existing_id FROM existing_patients 
    WHERE first_name = ? AND last_name = ? AND contact = ?
");
$check_existing->bind_param("sss", $first_name, $last_name, $contact);
$check_existing->execute();
$result = $check_existing->get_result();

if ($row = $result->fetch_assoc()) {
    $existing_id = $row['existing_id'];
} else {
    echo json_encode(["success" => false, "message" => "No matching record found in clinic records. Please visit the clinic first."]);
    exit;
}
$check_existing->close();

// Step 2: Check if email already exists
$check = $connect->prepare("SELECT user_id FROM user_account WHERE email = ?");
$check->bind_param("s", $email);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Email already registered"]);
    exit;
}
$check->close();

// Step 3: Hash password
$hashed = password_hash($password, PASSWORD_DEFAULT);

// Step 4: Insert into user_account
$sql_user = $connect->prepare("INSERT INTO user_account (email, password, user_role_id) VALUES (?, ?, ?)");
$user_role_id = 4; // Patient role
$sql_user->bind_param("ssi", $email, $hashed, $user_role_id);

if (!$sql_user->execute()) {
    echo json_encode(["success" => false, "message" => "Failed to create user account"]);
    exit;
}

$user_id = $connect->insert_id;
$sql_user->close();

// Step 5: Compute age from dob
$dob_date = new DateTime($dob);
$today = new DateTime();
$age = $today->diff($dob_date)->y;

// Step 6: Insert into patient table (linked to both user_account and existing_patients)
$sql_patient = $connect->prepare("
    INSERT INTO patient (first_name, middle_name, last_name, dob, age, gender, contact, address, user_id, existing_id)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
");
$sql_patient->bind_param("ssssisssii", $first_name, $middle_name, $last_name, $dob, $age, $gender, $contact, $address, $user_id, $existing_id);

if (!$sql_patient->execute()) {
    // Rollback user_account if patient insert fails
    $connect->query("DELETE FROM user_account WHERE user_id = $user_id");
    echo json_encode(["success" => false, "message" => "Failed to save patient information"]);
    exit;
}
$sql_patient->close();

// Step 7: Delete any old verification codes for this email
$delete_old = $connect->prepare("DELETE FROM email_verification WHERE email = ?");
$delete_old->bind_param("s", $email);
$delete_old->execute();
$delete_old->close();

// Step 8: Store verification code in database
$expires_at = date('Y-m-d H:i:s', strtotime('+15 minutes'));
$token = bin2hex(random_bytes(32)); // Unique token

$sql_verify = $connect->prepare("INSERT INTO email_verification (email, token, expires_at) VALUES (?, ?, ?)");
$sql_verify->bind_param("sss", $email, $token, $expires_at);

if (!$sql_verify->execute()) {
    echo json_encode(["success" => false, "message" => "Failed to create verification record"]);
    exit;
}

$verification_id = $connect->insert_id;
$sql_verify->close();

// Step 9: Update with the 6-digit code
$update_code = $connect->prepare("UPDATE email_verification SET token = ? WHERE id = ?");
$code_string = (string) $verification_code;
$update_code->bind_param("si", $code_string, $verification_id);
$update_code->execute();
$update_code->close();

// Step 10: Send verification email
$mail = new PHPMailer(true);
try {
    $mail->isSMTP();
    $mail->Host = 'smtp.gmail.com';
    $mail->SMTPAuth = true;
    $mail->Username = 'faminidenice@gmail.com';
    $mail->Password = 'lmbx xggb iqem ufmg';
    $mail->SMTPSecure = 'tls';
    $mail->Port = 587;

    $mail->setFrom('faminidenice@gmail.com', 'St. Therese Multi-Specialty Services, Inc');
    $mail->addAddress($email, $first_name . ' ' . $last_name);
    $mail->isHTML(true);
    $mail->Subject = 'Email Verification Code';
    $mail->Body = "
        <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
            <h2 style='color: #2c3e50;'>Welcome to AppointEase!</h2>
            <p>Dear <strong>$first_name</strong>,</p>
            <p>Thank you for creating an account. To complete your registration, please verify your email address using the code below:</p>
            <div style='background: #f8f9fa; padding: 20px; text-align: center; margin: 20px 0; border-radius: 10px;'>
                <h1 style='color: #c85a54; font-size: 42px; letter-spacing: 8px; margin: 0;'>$verification_code</h1>
            </div>
            <p style='color: #7f8c8d; font-size: 14px;'>This code will expire in <strong>15 minutes</strong>.</p>
            <p style='color: #7f8c8d; font-size: 14px;'>If you didn't create this account, please ignore this email.</p>
            <hr style='border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;'>
            <p style='color: #95a5a6; font-size: 12px; text-align: center;'>St. Therese Multi-Specialty Services, Inc<br>Your Health, Our Priority</p>
        </div>
    ";

    $mail->send();

    echo json_encode([
        "success" => true,
        "message" => "Registration successful! Please check your email for the verification code.",
        "email" => $email
    ]);

} catch (Exception $e) {
    echo json_encode([
        "success" => true,
        "message" => "Account created, but failed to send verification email. Please contact support.",
        "email" => $email,
        "email_error" => true
    ]);
}

$connect->close();
?>