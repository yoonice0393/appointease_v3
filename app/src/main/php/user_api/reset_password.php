<?php
error_reporting(0);
ini_set('display_errors', 0);
include 'db_connect.php';
header('Content-Type: application/json');

$user_id = $_POST['user_id'] ?? '';
$new_password = $_POST['new_password'] ?? '';

if (empty($user_id) || empty($new_password)) {
    echo json_encode(["success" => false, "message" => "Missing user ID or new password."]);
    exit;
}

// Securely hash the new password
$hashed_password = password_hash($new_password, PASSWORD_BCRYPT);

// Update password in the database
$update = $connect->prepare("UPDATE user_account SET password = ?, updated_at = NOW() WHERE user_id = ?");
$update->bind_param("si", $hashed_password, $user_id);
$update->execute();

if ($update->affected_rows > 0) {
    echo json_encode(["success" => true, "message" => "Password reset successfully."]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to reset password. Try again."]);
}
?>