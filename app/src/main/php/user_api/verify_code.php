<?php
error_reporting(0);
ini_set('display_errors', 0);
include 'db_connect.php';
header('Content-Type: application/json');

$email = $_POST['email'] ?? '';
$code = $_POST['code'] ?? '';

if (empty($email) || empty($code)) {
    echo json_encode(["success" => false, "message" => "Missing email or verification code."]);
    exit;
}

// Check if code matches and is still valid
$stmt = $connect->prepare("
    SELECT user_id, password_reset_token, token_expiry 
    FROM user_account 
    WHERE email = ? AND password_reset_token = ?
");
$stmt->bind_param("ss", $email, $code);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["success" => false, "message" => "Invalid or expired verification code."]);
    exit;
}

$user = $result->fetch_assoc();
$current_time = date("Y-m-d H:i:s");

if ($current_time > $user['token_expiry']) {
    echo json_encode(["success" => false, "message" => "The verification code has expired."]);
    exit;
}

// Success — clear the token after verification
$clear = $connect->prepare("UPDATE user_account SET password_reset_token = NULL, token_expiry = NULL WHERE email = ?");
$clear->bind_param("s", $email);
$clear->execute();

echo json_encode([
    "success" => true,
    "message" => "Code verified successfully. You may now reset your password.",
    "user_id" => $user['user_id']
]);
?>