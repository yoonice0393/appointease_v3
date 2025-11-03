<?php
include 'db_connect.php';
header('Content-Type: application/json');

$email = $_POST['email'] ?? '';
$code = $_POST['code'] ?? '';

$sql = $connect->prepare("SELECT token FROM email_verification WHERE email = ?");
$sql->bind_param("s", $email);
$sql->execute();
$result = $sql->get_result();

if ($row = $result->fetch_assoc()) {
    if ($row['token'] == $code) {
        $update = $connect->prepare("UPDATE email_verification SET verified = 1 WHERE email = ?");
        $update->bind_param("s", $email);
        $update->execute();

        echo json_encode(["success" => true, "message" => "Account verified"]);
    } else {
        echo json_encode(["success" => false, "message" => "Invalid verification code"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Account not found"]);
}

$connect->close();
?>