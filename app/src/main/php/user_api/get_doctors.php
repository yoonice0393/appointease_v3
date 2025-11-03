<?php
error_reporting(0);
header('Content-Type: application/json');

require 'db_connect.php';

$connect = new mysqli($servername, $username, $password, $dbname);

if ($connect->connect_error) {
    echo json_encode(["success" => false, "message" => "Database connection failed"]);
    exit;
}

$specialty = $_GET['specialty'] ?? null;

$sql = "SELECT 
            doctor_id as id,
            name,
            specialty,
            schedule_days,
            availability_type,
            contact_number,
            remarks
        FROM doctors 
        WHERE deleted_at IS NULL";

if ($specialty && $specialty != 'all') {
    $sql .= " AND specialty = ?";
}

$sql .= " ORDER BY name";

$stmt = $connect->prepare($sql);
if ($specialty && $specialty != 'all') {
    $stmt->bind_param("s", $specialty);
}
$stmt->execute();
$result = $stmt->get_result();

$doctors = [];
while ($row = $result->fetch_assoc()) {
    $doctors[] = $row;
}

echo json_encode([
    "success" => true,
    "message" => "Doctors retrieved successfully",
    "data" => $doctors
]);

$connect->close();
?>