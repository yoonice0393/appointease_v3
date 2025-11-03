<?php
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\SMTP;
use PHPMailer\PHPMailer\Exception;

require 'vendor/autoload.php';

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$message = '';  // Default message

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $name = $_POST['name'] ?? '';
    $time = $_POST['time'] ?? '';
    $appointment_type = $_POST['appointment_type'] ?? '';
    $expiration_date = $_POST['expiration_date'] ?? ''; // Get the expiration date
    $action = $_POST['action'] ?? 'submit'; // Default action is to submit

    if ($action === 'cancel') {
        $message = "Appointment has been canceled successfully.";
    } elseif ($action === 'move') {
        $message = "Appointment has been rescheduled. Please check your email for details.";
    } else {
        $mail = new PHPMailer(true);

        try {
            // Disable verbose debug output
            $mail->SMTPDebug = SMTP::DEBUG_OFF;

            // Use SMTP
            $mail->isSMTP();

            // Set the SMTP server to send through
            $mail->Host       = 'smtp.gmail.com';

            // Enable SMTP authentication
            $mail->SMTPAuth   = true;

            // Your Gmail address and app password
            $mail->Username   = 'nlbhealthcenter01@gmail.com';
            $mail->Password   = 'megitfdqgqhjufqn'; // Use an app password here

            // Use SSL encryption (port 465)
            $mail->SMTPSecure = PHPMailer::ENCRYPTION_SMTPS;
            $mail->Port       = 465;

            // Optional: Disable SSL certificate verification (not recommended for production)
            $mail->SMTPOptions = [
                'ssl' => [
                    'verify_peer' => false,
                    'verify_peer_name' => false,
                    'allow_self_signed' => true,
                ],
            ];

            // Set sender and recipient
            $mail->setFrom('nlbhealthcenter01@gmail.com', 'Appointment Portal NLB');
            $mail->addAddress('nicoleborden2003@gmail.com', 'User');

            // Set the email format to HTML
            $mail->isHTML(true);
            $mail->Subject = 'Appointment Confirmation';

            // Email Body
            $mail->Body    = "
                <html>
                    <body>
                        <h2>Appointment Confirmation</h2>
                        <p>Hello <strong>$name</strong>,</p>
                        <p>Your $appointment_type appointment has been successfully scheduled at:</p>
                        <p><strong>$time</strong></p>
                        <p>This appointment will expire on: <strong>$expiration_date</strong></p>
                        <p>If you do not attend your appointment by this time, it will automatically expire. You will receive a notification if this happens.</p>
                        <p>Thank you for choosing NLB Center!</p>
                    </body>
                </html>";

            // Send the email
            if ($mail->send()) {
                $message = "Your appointment has been successfully scheduled. A confirmation email has been sent to you.";
            } else {
                $message = "Failed to send the email. Please check your configuration.";
            }
        } catch (Exception $e) {
            $message = "There was an error sending your appointment email: " . $e->getMessage();
        }
    }
}
?>
