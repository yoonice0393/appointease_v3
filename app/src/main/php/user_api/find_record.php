<?php
// This must be the VERY FIRST line - no whitespace, no BOM, nothing before this
header("Content-Type: application/json; charset=UTF-8");

// For debugging only - remove in production
ini_set('display_errors', 0); // Changed to 0 to prevent HTML output
error_reporting(E_ALL);

// Start output buffering to catch any unexpected output
ob_start();

include 'db_connect.php';

try {
    $full_name = $_POST['full_name'] ?? '';
    $contact = $_POST['contact'] ?? '';

    // Clean any output buffer
    ob_clean();

    if (empty($full_name) || empty($contact)) {
        echo json_encode(["success" => false, "message" => "Incomplete input."]);
        exit;
    }

    $name_parts = explode(' ', trim($full_name));
    $first_name = $name_parts[0];
    $last_name = end($name_parts);

    $sql = "SELECT * FROM existing_patients WHERE LOWER(first_name)=LOWER(?) AND LOWER(last_name)=LOWER(?) AND contact=?";
    $stmt = $connect->prepare($sql);

    if (!$stmt) {
        echo json_encode(["success" => false, "message" => "SQL error: " . $connect->error]);
        exit;
    }

    $stmt->bind_param("sss", $first_name, $last_name, $contact);

    if (!$stmt->execute()) {
        echo json_encode(["success" => false, "message" => "Execution error: " . $stmt->error]);
        exit;
    }

    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $patient = $result->fetch_assoc();
        echo json_encode(["success" => true, "data" => $patient]);
    } else {
        echo json_encode(["success" => false, "message" => "No records found."]);
    }

    $stmt->close();
    $connect->close();

} catch (Exception $e) {
    // Clean buffer before error response
    ob_clean();
    echo json_encode(["success" => false, "message" => "Exception: " . $e->getMessage()]);
}

// End and clean output buffer
ob_end_flush();
?>