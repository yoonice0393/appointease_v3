<?php
include 'db_connect.php';

header('Content-Type: application/json');

// Get the user ID from the app
$user_id = $_POST['user_id'] ?? '';

if (empty($user_id)) {
    echo json_encode(['success' => false, 'message' => 'User ID missing']);
    exit;
}

$query = $connect->prepare("SELECT first_name, last_name FROM patient WHERE user_id = ?");
$query->bind_param("i", $user_id);
$query->execute();
$result = $query->get_result();

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();
    echo json_encode([
        'success' => true,
        'first_name' => $row['first_name'],
        'last_name' => $row['last_name']
    ]);
} else {
    echo json_encode(['success' => false, 'message' => 'User not found']);
}

$connect->close();
?>