
<?php
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\SMTP;
use PHPMailer\PHPMailer\Exception;

require 'vendor/autoload.php';

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$message = '';  // Default message

// Initialize variables
$name = '';
$time = '';
$appointment_type = '';
$expiration_date = '';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $name = $_POST['name'] ?? '';
    $time = $_POST['time'] ?? '';
    $appointment_type = $_POST['appointment_type'] ?? '';
    $expiration_date = $_POST['expiration_date'] ?? ''; // Get the expiration date
    $action = $_POST['action'] ?? 'submit'; // Default action is to submit

    if ($action === 'submit') {
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
            $mail->addAddress('minyoonice93@gmail.com', 'User');


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
                $message = "Appointment has been successfully scheduled. A confirmation email has been sent to you.";
            } else {
                $message = "Failed to send the email. Please check your configuration.";
            }
        } catch (Exception $e) {
            $message = "There was an error sending your appointment email: " . $e->getMessage();
        }
    }
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>New Lower Bicutan Portal</title>
<style>
body {
    font-family: Arial, sans-serif;
    background: linear-gradient(to bottom, #fce4ec, #ffffff);
    margin: 0;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
}
.container {
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
    padding: 30px;
    width: 400px;
    text-align: center;
}
h1 {
    color: #e74c3c;
    margin-bottom: 20px;
    font-size: 2rem;
}
.message, .confirmation {
    color: #27ae60;
    font-size: 16px;
    margin: 10px 0;
}
.error-message {
    color: #e74c3c;
    font-size: 16px;
    margin: 10px 0;
}
form {
    display: flex;
    flex-direction: column;
    align-items: center;
}
input, select, button {
    width: 80%;
    padding: 12px;
    margin-bottom: 15px;
    border-radius: 4px;
    border: 1px solid #ddd;
    font-size: 16px;
}
button {
    background-color: #f0e4d7;
    cursor: pointer;
    transition: background-color 0.3s ease;
}
button:hover {
    background-color: #e1d0a1;
}
.confirmation {
    background-color: #d4edda;
    padding: 15px;
    border-radius: 5px;
    border: 1px solid #28a745;
    text-align: left;
    margin-top: 20px;
}
</style>
</head>
<body>
<div class="container">
    <h1>NLB Center Portal</h1>
    <?php if (!empty($message)): ?>
        <div class="confirmation"><?php echo $message; ?></div>
    <?php endif; ?>
    <form method="POST">
        <label for="name">Name</label>
        <input type="text" id="name" name="name" value="<?php echo htmlspecialchars($name); ?>" required>
        
        <label for="appointment_type">Type of Appointment</label>
        <select id="appointment_type" name="appointment_type" required>
            <option value="">--Select Appointment Type--</option>
            <option value="Dental" <?php echo ($appointment_type == 'Dental') ? 'selected' : ''; ?>>Dental</option>
            <option value="Medical Check-up" <?php echo ($appointment_type == 'Medical Check-up') ? 'selected' : ''; ?>>Medical Check-up</option>
            <option value="Eye Consultation" <?php echo ($appointment_type == 'Eye Consultation') ? 'selected' : ''; ?>>Eye Consultation</option>
            <option value="Pre-Natal" <?php echo ($appointment_type == 'Pre-Natal') ? 'selected' : ''; ?>>Pre-Natal</option>
            <option value="TB-Dots" <?php echo ($appointment_type == 'TB-Dots') ? 'selected' : ''; ?>>TB-Dots</option>
        </select>

        <label for="time">Appointment Time</label>
        <input type="datetime-local" id="time" name="time" value="<?php echo htmlspecialchars($time); ?>" required>

        <label for="expiration_date">Expiration Date</label>
        <input type="datetime-local" id="expiration_date" name="expiration_date" value="<?php echo htmlspecialchars($expiration_date); ?>" required>

        <button type="submit" name="action" value="submit">Submit Appointment</button>
    </form>
</div>
</body>
</html>
