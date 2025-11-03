<?php
error_reporting(0);
header('Content-Type: application/json');

require 'db_connect.php';

$connect = new mysqli($servername, $username, $password, $dbname);

if ($connect->connect_error) {
    echo json_encode(["success" => false, "message" => "Database connection failed"]);
    exit;
}

$user_id = $_POST['user_id'] ?? null;

if (!$user_id) {
    echo json_encode(["success" => false, "message" => "User ID required"]);
    exit;
}

$sql = "SELECT 
            appointment_id as id,
            doctor_name,
            specialty,
            appointment_date as date,
            TIME_FORMAT(appointment_time, '%h:%i %p') as time,
            status
        FROM appointments 
        WHERE user_id = ? AND status = 'Upcoming'
        ORDER BY appointment_date, appointment_time";

$stmt = $connect->prepare($sql);
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

$appointments = [];
while ($row = $result->fetch_assoc()) {
    $appointments[] = $row;
}

echo json_encode([
    "success" => true,
    "message" => "Appointments retrieved successfully",
    "data" => $appointments
]);

$connect->close();
?>