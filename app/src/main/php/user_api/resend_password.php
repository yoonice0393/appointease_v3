<?php
include 'db_connect.php';
header('Content-Type: application/json');

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'smtp_mailer/PHPMailer/src/Exception.php';
require 'smtp_mailer/PHPMailer/src/PHPMailer.php';
require 'smtp_mailer/PHPMailer/src/SMTP.php';


$email = $_POST['email'] ?? '';

if (empty($email)) {
    echo json_encode(["success" => false, "message" => "Email is required."]);
    exit;
}

// Check user
$stmt = $connect->prepare("SELECT user_id, email FROM user_account WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["success" => false, "message" => "Email not found."]);
    exit;
}

$user = $result->fetch_assoc();

// Generate new code
$verification_code = rand(100000, 999999);
$expiry = date("Y-m-d H:i:s", strtotime("+15 minutes"));

// Save code
$update = $connect->prepare("UPDATE user_account SET password_reset_token = ?, token_expiry = ? WHERE email = ?");
$update->bind_param("sss", $verification_code, $expiry, $email);
$update->execute();

// Send email
$mail = new PHPMailer(true);
try {
    $mail->isSMTP();
    $mail->Host = 'smtp.gmail.com';
    $mail->SMTPAuth = true;
    $mail->Username = 'faminidenice@gmail.com';
    $mail->Password = 'lmbx xggb iqem ufmg';
    $mail->SMTPSecure = 'tls';
    $mail->Port = 587;

    $mail->setFrom('faminidenice@gmail.com', 'St. Therese Health Center');
    $mail->addAddress($email);
    $mail->isHTML(true);
    $mail->Subject = 'Your New Verification Code';
    $mail->Body = "<h3>Your new 6-digit code is:</h3>
                   <div style='font-size:32px; font-weight:bold;'>$verification_code</div>";

    $mail->send();
    echo json_encode(["success" => true, "message" => "New code sent to your email."]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => "Mailer error: {$mail->ErrorInfo}"]);
}
?>