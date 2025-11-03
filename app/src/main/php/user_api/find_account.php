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

// Get user input
$email = $_POST['email'] ?? '';

if (empty($email)) {
    echo json_encode(["success" => false, "message" => "Please enter your email or phone number."]);
    exit;
}

// Check if email exists
$stmt = $connect->prepare("SELECT user_id, email FROM user_account WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["success" => false, "message" => "Account not found."]);
    exit;
}

$user = $result->fetch_assoc();
$verification_code = rand(100000, 999999);

// Save token and expiry in database
date_default_timezone_set('Asia/Manila');
$expiry = date("Y-m-d H:i:s", strtotime("+15 minutes"));
$update = $connect->prepare("UPDATE user_account SET password_reset_token = ?, token_expiry = ? WHERE email = ?");
$update->bind_param("sss", $verification_code, $expiry, $email);
$update->execute();

// Setup mailer
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
    $mail->addAddress($user['email']);
    $mail->isHTML(true);
    $mail->Subject = 'Password Reset Request';
    $mail->Body = "
        <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
            <h2 style='color: #2c3e50;'>Password Reset Request</h2>
            <p>We received a request to reset your password. Please use the verification code below:</p>
            <div style='background: #f8f9fa; padding: 20px; text-align: center; margin: 20px 0; border-radius: 10px;'>
                <h1 style='color: #c85a54; font-size: 42px; letter-spacing: 8px; margin: 0;'>$verification_code</h1>
            </div>
            <p>This code will expire in <strong>15 minutes</strong>.</p>
            <p>If you didn't request this, you can safely ignore this message.</p>
            <hr style='border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;'>
            <p style='color: #95a5a6; font-size: 12px; text-align: center;'>St. Therese Multi-Specialty Services, Inc<br>Your Health, Our Priority</p>
        </div>
    ";

    $mail->send();
    echo json_encode(["success" => true, "message" => "Verification email sent successfully."]);

} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => "Mailer Error: {$mail->ErrorInfo}"]);
}
?>