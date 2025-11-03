<?php
error_reporting(0);
ini_set('display_errors', 0);
include 'db_connect.php';
header('Content-Type: application/json');

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'smtp_mailer/PHPMailer/src/Exception.php';
require 'smtp_mailer/PHPMailer/src/PHPMailer.php';
require 'smtp_mailer/PHPMailer/src/SMTP.php';

$email = $_POST['email'] ?? '';

// Validate email
if (empty($email)) {
    echo json_encode(["success" => false, "message" => "Email is required"]);
    exit;
}

// Check if email exists in user_account
$check_user = $connect->prepare("SELECT user_id FROM user_account WHERE email = ?");
$check_user->bind_param("s", $email);
$check_user->execute();
$check_user->store_result();

if ($check_user->num_rows === 0) {
    echo json_encode(["success" => false, "message" => "Email not found"]);
    exit;
}
$check_user->close();

// Check if already verified
$check_verified = $connect->prepare("SELECT verified FROM email_verification WHERE email = ? AND verified = 1");
$check_verified->bind_param("s", $email);
$check_verified->execute();
$check_verified->store_result();

if ($check_verified->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Email already verified"]);
    exit;
}
$check_verified->close();

// Get user's name
$get_name = $connect->prepare("
    SELECT p.first_name, p.last_name 
    FROM patient p 
    JOIN user_account u ON p.user_id = u.user_id 
    WHERE u.email = ?
");
$get_name->bind_param("s", $email);
$get_name->execute();
$name_result = $get_name->get_result();
$name_row = $name_result->fetch_assoc();
$first_name = $name_row['first_name'] ?? 'User';
$last_name = $name_row['last_name'] ?? '';
$get_name->close();

// Generate new verification code
$verification_code = rand(100000, 999999);

// Delete old verification codes for this email
$delete_old = $connect->prepare("DELETE FROM email_verification WHERE email = ?");
$delete_old->bind_param("s", $email);
$delete_old->execute();
$delete_old->close();

// Insert new verification code
$expires_at = date('Y-m-d H:i:s', strtotime('+15 minutes'));
$token = bin2hex(random_bytes(32));

$sql_verify = $connect->prepare("INSERT INTO email_verification (email, token, expires_at) VALUES (?, ?, ?)");
$sql_verify->bind_param("sss", $email, $token, $expires_at);

if (!$sql_verify->execute()) {
    echo json_encode(["success" => false, "message" => "Failed to generate new code"]);
    exit;
}

$verification_id = $connect->insert_id;
$sql_verify->close();

// Update with the 6-digit code
$update_code = $connect->prepare("UPDATE email_verification SET token = ? WHERE id = ?");
$code_string = (string) $verification_code;
$update_code->bind_param("si", $code_string, $verification_id);
$update_code->execute();
$update_code->close();

// Send verification email
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
    $mail->Subject = 'New Verification Code';
    $mail->Body = "
        <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
            <h2 style='color: #2c3e50;'>New Verification Code</h2>
            <p>Dear <strong>$first_name</strong>,</p>
            <p>You requested a new verification code. Here it is:</p>
            <div style='background: #f8f9fa; padding: 20px; text-align: center; margin: 20px 0; border-radius: 10px;'>
                <h1 style='color: #c85a54; font-size: 42px; letter-spacing: 8px; margin: 0;'>$verification_code</h1>
            </div>
            <p style='color: #7f8c8d; font-size: 14px;'>This code will expire in <strong>15 minutes</strong>.</p>
            <p style='color: #7f8c8d; font-size: 14px;'>If you didn't request this, please ignore this email.</p>
            <hr style='border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;'>
            <p style='color: #95a5a6; font-size: 12px; text-align: center;'>St. Therese Multi-Specialty Services, Inc<br>Your Health, Our Priority</p>
        </div>
    ";

    $mail->send();

    echo json_encode([
        "success" => true,
        "message" => "A new verification code has been sent to your email."
    ]);

} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "Failed to send email: " . $mail->ErrorInfo
    ]);
}

$connect->close();
?>