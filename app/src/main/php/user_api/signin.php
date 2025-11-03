<?php
include 'db_connect.php';
header('Content-Type: application/json');

// Config
define('MAX_ATTEMPTS', 5);
define('LOCKOUT_TIME', 120); // 5 minutes in seconds

// Helper Functions
function is_locked($email, $connect)
{
    // Use TIMESTAMPDIFF to calculate seconds directly in MySQL
    $stmt = $connect->prepare("
        SELECT failed_attempts, 
               TIMESTAMPDIFF(SECOND, attempt_time, NOW()) as seconds_passed 
        FROM login_attempts 
        WHERE email = ? 
        LIMIT 1
    ");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();
    $attempt = $result->fetch_assoc();
    $stmt->close();

    if ($attempt) {
        $seconds_passed = $attempt['seconds_passed'];

        // If enough time has passed, not locked anymore
        if ($seconds_passed >= LOCKOUT_TIME) {
            return false;
        }

        return $attempt['failed_attempts'] >= MAX_ATTEMPTS;
    }

    return false;
}

function record_failed_attempt($email, $connect)
{
    // Check if record exists and calculate time difference in MySQL
    $stmtCheck = $connect->prepare("
        SELECT failed_attempts, 
               TIMESTAMPDIFF(SECOND, attempt_time, NOW()) as seconds_passed 
        FROM login_attempts 
        WHERE email = ? 
        LIMIT 1
    ");
    $stmtCheck->bind_param("s", $email);
    $stmtCheck->execute();
    $result = $stmtCheck->get_result();
    $attempt = $result->fetch_assoc();
    $stmtCheck->close();

    if ($attempt) {
        if ($attempt['seconds_passed'] >= LOCKOUT_TIME) {
            // Reset counter if last attempt was long ago
            $stmtReset = $connect->prepare("UPDATE login_attempts SET failed_attempts = 1, attempt_time = NOW() WHERE email = ?");
            $stmtReset->bind_param("s", $email);
            $stmtReset->execute();
            $stmtReset->close();
        } else {
            // Increment failed attempts
            $stmtUpdate = $connect->prepare("UPDATE login_attempts SET failed_attempts = failed_attempts + 1, attempt_time = NOW() WHERE email = ?");
            $stmtUpdate->bind_param("s", $email);
            $stmtUpdate->execute();
            $stmtUpdate->close();
        }
    } else {
        // Insert new record
        $stmtInsert = $connect->prepare("INSERT INTO login_attempts (email, failed_attempts, attempt_time) VALUES (?, 1, NOW())");
        $stmtInsert->bind_param("s", $email);
        $stmtInsert->execute();
        $stmtInsert->close();
    }
}

function reset_failed_attempts($email, $connect)
{
    $stmt = $connect->prepare("DELETE FROM login_attempts WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $affected = $stmt->affected_rows;
    $stmt->close();

    return $affected > 0;
}

// Main Login Logic
$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(["success" => false, "message" => "All fields are required."]);
    exit;
}

// Check lockout status
if (is_locked($email, $connect)) {
    echo json_encode([
        "success" => false,
        "message" => "Your account is temporarily locked due to multiple failed attempts. Please try again after 5 minutes."
    ]);
    exit;
}

// Fetch user
$sql = $connect->prepare("
    SELECT ua.user_id, ua.password, p.first_name, p.last_name
    FROM user_account ua
    LEFT JOIN patient p ON ua.user_id = p.user_id
    WHERE ua.email = ?
");
$sql->bind_param("s", $email);
$sql->execute();
$result = $sql->get_result();

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();

    if (password_verify($password, $row['password'])) {
        reset_failed_attempts($email, $connect);

        echo json_encode([
            "success" => true,
            "message" => "Login successful",
            "user_id" => $row['user_id'],
            "first_name" => $row['first_name'] ?? '',
            "last_name" => $row['last_name'] ?? ''
        ]);
    } else {
        record_failed_attempt($email, $connect);
        echo json_encode(["success" => false, "message" => "Invalid password."]);
    }
} else {
    record_failed_attempt($email, $connect);
    echo json_encode(["success" => false, "message" => "No user found with that email."]);
}

$sql->close();
$connect->close();
?>