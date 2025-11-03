<?php
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'PHPMailer/src/Exception.php';
require 'PHPMailer/src/PHPMailer.php';
require 'PHPMailer/src/SMTP.php';

$mail = new PHPMailer(true);

try {
    $mail->isSMTP();
    $mail->Host = 'smtp.gmail.com';
    $mail->SMTPAuth = true;
    $mail->Username = 'youremail@gmail.com'; // change this
    $mail->Password = 'your_app_password'; // use Gmail app password (not your Gmail password)
    $mail->SMTPSecure = 'tls';
    $mail->Port = 587;

    $mail->setFrom('youremail@gmail.com', 'St. Therese App');
    $mail->addAddress($email);
    $mail->isHTML(true);
    $mail->Subject = 'Your Verification Code';
    $mail->Body = "
<h3>Welcome to St. Therese App!</h3>
<p>Your verification code is:</p>
<h2>$verification_code</h2>
<p>Please enter this code in the app to verify your account.</p>
";

    $mail->send();
    echo json_encode(["success" => true, "message" => "Verification email sent"]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => "Mailer Error: {$mail->ErrorInfo}"]);
}
?>